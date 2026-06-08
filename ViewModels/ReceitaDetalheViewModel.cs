using CadernoReceitas.Data;
using CadernoReceitas.Models;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using System.Collections.ObjectModel;

namespace CadernoReceitas.ViewModels;

[QueryProperty(nameof(ReceitaId), "receitaId")]
public sealed partial class ReceitaDetalheViewModel : BaseViewModel
{
    private readonly AppDatabase database;
    private readonly List<Ingrediente> todosIngredientes = new();

    [ObservableProperty]
    private int receitaId;

    [ObservableProperty]
    private Receita? receita;

    [ObservableProperty]
    private string nome = string.Empty;

    [ObservableProperty]
    private string modoPreparo = string.Empty;

    [ObservableProperty]
    private string ingredienteNome = string.Empty;

    [ObservableProperty]
    private string ingredienteQuantidade = string.Empty;

    [ObservableProperty]
    private string ingredienteCategoria = string.Empty;

    [ObservableProperty]
    private Receita? receitaIngredienteSelecionada;

    [ObservableProperty]
    private string ingredienteVinculoResumo = "Opcional: selecione uma receita cadastrada para usar como ingrediente preparado.";

    [ObservableProperty]
    private string searchText = string.Empty;

    public ObservableCollection<Ingrediente> Ingredientes { get; } = new();

    public ObservableCollection<string> Categorias { get; } = new();

    public ObservableCollection<string> SugestoesIngredientes { get; } = new();

    public ObservableCollection<Receita> ReceitasParaVincular { get; } = new();

    public ReceitaDetalheViewModel(AppDatabase database)
    {
        this.database = database;
        Title = "Receita";
    }

    partial void OnReceitaIdChanged(int value)
    {
        _ = LoadAsync();
    }

    partial void OnReceitaIngredienteSelecionadaChanged(Receita? value)
    {
        if (value is null)
        {
            IngredienteVinculoResumo = "Opcional: selecione uma receita cadastrada para usar como ingrediente preparado.";
            return;
        }

        IngredienteVinculoResumo = $"Vinculado a receita: {value.Nome}";
        if (string.IsNullOrWhiteSpace(IngredienteNome))
        {
            IngredienteNome = value.Nome;
        }

        if (string.IsNullOrWhiteSpace(IngredienteCategoria))
        {
            IngredienteCategoria = "Receita preparada";
        }
    }

    [RelayCommand]
    private async Task LoadAsync()
    {
        if (ReceitaId <= 0) return;
        Receita = await database.GetReceitaAsync(ReceitaId);
        Nome = Receita?.Nome ?? "Receita";
        ModoPreparo = Receita?.ModoPreparo ?? string.Empty;

        todosIngredientes.Clear();
        todosIngredientes.AddRange(await database.GetIngredientesAsync(ReceitaId));
        ApplySearch();

        Categorias.Clear();
        foreach (var item in await database.GetCategoriasIngredientesAsync())
        {
            Categorias.Add(item);
        }

        SugestoesIngredientes.Clear();
        foreach (var item in await database.GetNomesIngredientesAsync())
        {
            SugestoesIngredientes.Add(item);
        }

        ReceitasParaVincular.Clear();
        foreach (var item in await database.GetReceitasParaIngredienteAsync(ReceitaId))
        {
            ReceitasParaVincular.Add(item);
        }
    }

    [RelayCommand]
    private async Task SalvarReceitaAsync()
    {
        if (Receita is null || string.IsNullOrWhiteSpace(Nome)) return;
        Receita.Nome = Nome.Trim();
        Receita.ModoPreparo = ModoPreparo.Trim();
        await database.SaveAsync(Receita);
        await LoadAsync();
    }

    partial void OnSearchTextChanged(string value) => ApplySearch();

    private void ApplySearch()
    {
        Ingredientes.Clear();
        foreach (var item in todosIngredientes.Where(item =>
            MatchesSearch(item.Nome, SearchText) ||
            MatchesSearch(item.Quantidade, SearchText) ||
            MatchesSearch(item.Categoria, SearchText) ||
            MatchesSearch(item.ReceitaIngredienteNome, SearchText)))
        {
            Ingredientes.Add(item);
        }
    }

    [RelayCommand]
    private async Task AdicionarIngredienteAsync()
    {
        if (ReceitaId <= 0 || (string.IsNullOrWhiteSpace(IngredienteNome) && ReceitaIngredienteSelecionada is null)) return;

        var receitaVinculada = ReceitaIngredienteSelecionada;
        await database.SaveAsync(new Ingrediente
        {
            ReceitaId = ReceitaId,
            Nome = string.IsNullOrWhiteSpace(IngredienteNome) ? receitaVinculada?.Nome ?? string.Empty : IngredienteNome.Trim(),
            Quantidade = IngredienteQuantidade.Trim(),
            Categoria = string.IsNullOrWhiteSpace(IngredienteCategoria) && receitaVinculada is not null ? "Receita preparada" : IngredienteCategoria.Trim(),
            ReceitaIngredienteId = receitaVinculada?.Id ?? 0
        });

        IngredienteNome = string.Empty;
        IngredienteQuantidade = string.Empty;
        IngredienteCategoria = string.Empty;
        ReceitaIngredienteSelecionada = null;
        await LoadAsync();
    }

    [RelayCommand]
    private void LimparReceitaIngrediente()
    {
        ReceitaIngredienteSelecionada = null;
    }

    [RelayCommand]
    private Task AbrirReceitaVinculadaAsync(Ingrediente ingrediente)
    {
        if (ingrediente.ReceitaIngredienteId <= 0)
        {
            return Task.CompletedTask;
        }

        return Shell.Current.GoToAsync($"receitaDetalhe?receitaId={ingrediente.ReceitaIngredienteId}");
    }

    [RelayCommand]
    private async Task ExcluirIngredienteAsync(Ingrediente ingrediente)
    {
        await database.DeleteAsync(ingrediente);
        await LoadAsync();
    }

    [RelayCommand]
    private async Task MenuIngredienteAsync(Ingrediente ingrediente)
    {
        var action = await Shell.Current.DisplayActionSheet(ingrediente.Nome, "Cancelar", null, "Editar", "Excluir");
        if (action == "Editar")
        {
            var nome = await Shell.Current.DisplayPromptAsync("Editar ingrediente", "Nome:", "Salvar", "Cancelar", initialValue: ingrediente.Nome);
            if (string.IsNullOrWhiteSpace(nome)) return;
            var quantidade = await Shell.Current.DisplayPromptAsync("Editar ingrediente", "Quantidade:", "Salvar", "Manter", initialValue: ingrediente.Quantidade);
            var categoria = await Shell.Current.DisplayPromptAsync("Editar ingrediente", "Categoria:", "Salvar", "Manter", initialValue: ingrediente.Categoria);
            ingrediente.Nome = nome.Trim();
            if (quantidade is not null)
            {
                ingrediente.Quantidade = quantidade.Trim();
            }

            if (categoria is not null)
            {
                ingrediente.Categoria = categoria.Trim();
            }

            await database.SaveAsync(ingrediente);
            await LoadAsync();
            return;
        }

        if (action == "Excluir")
        {
            var confirm = await Shell.Current.DisplayAlert("Excluir ingrediente", $"Excluir \"{ingrediente.Nome}\"?", "Excluir", "Cancelar");
            if (!confirm) return;
            await database.DeleteAsync(ingrediente);
            await LoadAsync();
        }
    }
}
