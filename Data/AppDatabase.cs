using CadernoReceitas.Models;
using SQLite;

namespace CadernoReceitas.Data;

public sealed class AppDatabase
{
    private readonly SQLiteAsyncConnection database;
    private bool initialized;

    public AppDatabase()
    {
        var path = Path.Combine(FileSystem.AppDataDirectory, "caderno-receitas.db3");
        database = new SQLiteAsyncConnection(path, SQLiteOpenFlags.Create | SQLiteOpenFlags.ReadWrite | SQLiteOpenFlags.SharedCache);
    }

    public async Task InitializeAsync()
    {
        if (initialized)
        {
            return;
        }

        await database.CreateTableAsync<Caderno>();
        await database.CreateTableAsync<Restaurante>();
        await database.CreateTableAsync<Praca>();
        await database.CreateTableAsync<Prato>();
        await database.CreateTableAsync<Receita>();
        await database.CreateTableAsync<Ingrediente>();
        await database.CreateTableAsync<QuizHistory>();
        await EnsureRecipeNotebookAsync();
        initialized = true;
    }

    public async Task<List<Caderno>> GetCadernosAsync()
    {
        await InitializeAsync();
        var cadernos = await database.Table<Caderno>().OrderBy(item => item.Nome).ToListAsync();
        foreach (var caderno in cadernos)
        {
            caderno.TotalReceitas = await database.Table<Receita>().Where(item => item.CadernoId == caderno.Id).CountAsync();
        }

        return cadernos;
    }

    public async Task<List<Receita>> GetReceitasDoCadernoAsync(int cadernoId)
    {
        await InitializeAsync();
        var receitas = await database.Table<Receita>().Where(item => item.CadernoId == cadernoId).OrderBy(item => item.Nome).ToListAsync();
        foreach (var receita in receitas)
        {
            receita.TotalIngredientes = await database.Table<Ingrediente>().Where(item => item.ReceitaId == receita.Id).CountAsync();
        }

        return receitas;
    }

    public async Task<Caderno?> GetCadernoAsync(int id)
    {
        await InitializeAsync();
        return await database.Table<Caderno>().FirstOrDefaultAsync(item => item.Id == id);
    }

    public async Task<Receita?> GetReceitaAsync(int id)
    {
        await InitializeAsync();
        var receita = await database.Table<Receita>().FirstOrDefaultAsync(item => item.Id == id);
        if (receita is not null)
        {
            receita.TotalIngredientes = await database.Table<Ingrediente>().Where(item => item.ReceitaId == receita.Id).CountAsync();
        }

        return receita;
    }

    public async Task<List<Ingrediente>> GetCatalogoIngredientesAsync()
    {
        await InitializeAsync();
        return await database.Table<Ingrediente>().OrderBy(item => item.Categoria).ThenBy(item => item.Nome).ToListAsync();
    }

    public async Task<List<string>> GetCategoriasIngredientesAsync()
    {
        await InitializeAsync();
        return (await database.Table<Ingrediente>().ToListAsync())
            .Select(item => item.Categoria)
            .Where(item => !string.IsNullOrWhiteSpace(item))
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .OrderBy(item => item)
            .ToList();
    }

    public async Task<List<Restaurante>> GetRestaurantesAsync()
    {
        await InitializeAsync();
        return await database.Table<Restaurante>().OrderBy(item => item.Nome).ToListAsync();
    }

    public async Task<List<Praca>> GetPracasAsync()
    {
        await InitializeAsync();
        var restaurantes = await GetRestaurantesAsync();
        var items = await database.Table<Praca>().OrderBy(item => item.Nome).ToListAsync();
        foreach (var item in items)
        {
            item.RestauranteNome = restaurantes.FirstOrDefault(restaurante => restaurante.Id == item.RestauranteId)?.Nome ?? "Sem restaurante";
        }

        return items;
    }

    public async Task<List<Prato>> GetPratosAsync()
    {
        await InitializeAsync();
        var pracas = await GetPracasAsync();
        var items = await database.Table<Prato>().OrderBy(item => item.Nome).ToListAsync();
        foreach (var item in items)
        {
            item.PracaNome = pracas.FirstOrDefault(praca => praca.Id == item.PracaId)?.Nome ?? "Sem praca";
        }

        return items;
    }

    public async Task<List<Receita>> GetReceitasAsync()
    {
        await InitializeAsync();
        var pratos = await GetPratosAsync();
        var items = await database.Table<Receita>().OrderBy(item => item.Nome).ToListAsync();
        foreach (var item in items)
        {
            item.PratoNome = pratos.FirstOrDefault(prato => prato.Id == item.PratoId)?.Nome ?? "Sem prato";
            item.TotalIngredientes = await database.Table<Ingrediente>().Where(ingrediente => ingrediente.ReceitaId == item.Id).CountAsync();
        }

        return items;
    }

    public async Task<List<Ingrediente>> GetIngredientesAsync(int receitaId)
    {
        await InitializeAsync();
        return await database.Table<Ingrediente>().Where(item => item.ReceitaId == receitaId).OrderBy(item => item.Categoria).ThenBy(item => item.Nome).ToListAsync();
    }

    public async Task<List<QuizHistory>> GetHistoricoAsync()
    {
        await InitializeAsync();
        return await database.Table<QuizHistory>().OrderByDescending(item => item.RealizadoEm).ToListAsync();
    }

    public async Task SaveAsync<T>(T item) where T : new()
    {
        await InitializeAsync();
        var idProperty = typeof(T).GetProperty("Id");
        var id = (int)(idProperty?.GetValue(item) ?? 0);
        if (id == 0)
        {
            await database.InsertAsync(item);
        }
        else
        {
            await database.UpdateAsync(item);
        }
    }

    public async Task DeleteAsync<T>(T item) where T : new()
    {
        await InitializeAsync();
        await database.DeleteAsync(item);
    }

    public async Task DeleteCadernoAsync(Caderno caderno)
    {
        await InitializeAsync();
        var receitas = await GetReceitasDoCadernoAsync(caderno.Id);
        foreach (var receita in receitas)
        {
            await DeleteReceitaAsync(receita);
        }

        await database.DeleteAsync(caderno);
    }

    public async Task DeleteReceitaAsync(Receita receita)
    {
        await InitializeAsync();
        var ingredientes = await GetIngredientesAsync(receita.Id);
        foreach (var item in ingredientes)
        {
            await database.DeleteAsync(item);
        }

        await database.DeleteAsync(receita);
    }

    public async Task<Dictionary<string, int>> GetStatsAsync()
    {
        await InitializeAsync();
        return new Dictionary<string, int>
        {
            ["Cadernos"] = await database.Table<Caderno>().CountAsync(),
            ["Restaurantes"] = await database.Table<Restaurante>().CountAsync(),
            ["Pracas"] = await database.Table<Praca>().CountAsync(),
            ["Pratos"] = await database.Table<Prato>().CountAsync(),
            ["Receitas"] = await database.Table<Receita>().CountAsync(),
            ["Ingredientes"] = await database.Table<Ingrediente>().CountAsync()
        };
    }

    public async Task SeedAsync()
    {
        await InitializeAsync();
        if (await database.Table<Caderno>().CountAsync() > 0)
        {
            return;
        }

        var caderno = new Caderno { Nome = "Caderno Modelo", Descricao = "Receitas base para treinamento da equipe." };
        await database.InsertAsync(caderno);

        var restaurante = new Restaurante { Nome = "Restaurante Modelo" };
        await database.InsertAsync(restaurante);
        var praca = new Praca { Nome = "Cozinha Quente", RestauranteId = restaurante.Id };
        await database.InsertAsync(praca);
        var prato = new Prato { Nome = "Risoto da Casa", PracaId = praca.Id };
        await database.InsertAsync(prato);
        var receita = new Receita
        {
            CadernoId = caderno.Id,
            PratoId = prato.Id,
            Nome = "Risoto da Casa",
            ModoPreparo = "Refogue a base, adicione arroz, caldo aos poucos e finalize cremoso."
        };
        await database.InsertAsync(receita);
        await database.InsertAllAsync(new[]
        {
            new Ingrediente { ReceitaId = receita.Id, Categoria = "Grao", Nome = "Arroz arboreo", Quantidade = "120 g" },
            new Ingrediente { ReceitaId = receita.Id, Categoria = "Base liquida", Nome = "Caldo", Quantidade = "400 ml" },
            new Ingrediente { ReceitaId = receita.Id, Categoria = "Laticinio", Nome = "Queijo", Quantidade = "40 g" }
        });
    }

    private async Task EnsureRecipeNotebookAsync()
    {
        if (await database.Table<Caderno>().CountAsync() > 0)
        {
            return;
        }

        var receitas = await database.Table<Receita>().ToListAsync();
        if (receitas.Count == 0)
        {
            return;
        }

        var caderno = new Caderno { Nome = "Caderno Geral", Descricao = "Receitas migradas da versao inicial." };
        await database.InsertAsync(caderno);
        foreach (var receita in receitas.Where(item => item.CadernoId == 0))
        {
            receita.CadernoId = caderno.Id;
            if (string.IsNullOrWhiteSpace(receita.Nome))
            {
                receita.Nome = string.IsNullOrWhiteSpace(receita.PratoNome) ? $"Receita {receita.Id}" : receita.PratoNome;
            }

            await database.UpdateAsync(receita);
        }
    }
}
