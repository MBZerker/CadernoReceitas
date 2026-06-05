using CadernoReceitas.Data;
using CadernoReceitas.Models;

namespace CadernoReceitas.Services;

public sealed class QuizService
{
    private readonly AppDatabase database;
    private readonly Random random = new();

    public QuizService(AppDatabase database)
    {
        this.database = database;
    }

    public async Task<List<QuizQuestion>> CreateQuizAsync(int amount = 5)
    {
        var receitas = await database.GetReceitasAsync();
        var perguntas = new List<QuizQuestion>();
        var modos = receitas.Where(item => !string.IsNullOrWhiteSpace(item.ModoPreparo)).ToList();
        foreach (var receita in modos.OrderBy(_ => random.Next()).Take(amount))
        {
            perguntas.Add(new QuizQuestion
            {
                Enunciado = $"Qual preparo pertence ao prato {receita.PratoNome}?",
                RespostaCorreta = receita.ModoPreparo,
                Alternativas = BuildOptions(receita.ModoPreparo, modos.Select(item => item.ModoPreparo))
            });
        }

        foreach (var receita in receitas.OrderBy(_ => random.Next()).Take(amount))
        {
            var ingredientes = await database.GetIngredientesAsync(receita.Id);
            var allNames = new List<string>();
            foreach (var item in receitas)
            {
                allNames.AddRange((await database.GetIngredientesAsync(item.Id)).Select(ingrediente => ingrediente.Nome));
            }

            var ingrediente = ingredientes.OrderBy(_ => random.Next()).FirstOrDefault();
            if (ingrediente is null)
            {
                continue;
            }

            perguntas.Add(new QuizQuestion
            {
                Enunciado = $"Qual ingrediente faz parte de {receita.PratoNome}?",
                RespostaCorreta = ingrediente.Nome,
                Alternativas = BuildOptions(ingrediente.Nome, allNames)
            });
        }

        return perguntas.OrderBy(_ => random.Next()).Take(amount).ToList();
    }

    private IReadOnlyList<string> BuildOptions(string correct, IEnumerable<string> source)
    {
        var options = source
            .Where(item => !string.IsNullOrWhiteSpace(item) && !string.Equals(item, correct, StringComparison.OrdinalIgnoreCase))
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .OrderBy(_ => random.Next())
            .Take(3)
            .ToList();

        options.Add(correct);
        return options.OrderBy(_ => random.Next()).ToList();
    }
}
