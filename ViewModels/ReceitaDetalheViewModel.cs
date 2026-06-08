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

    public ObservableCollection<Ingrediente> Ingredientes { get; } = new();

    public ObservableCollection<string> Categorias { get; } = new();

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

        Ingredientes.Clear();
        foreach (var item in await database.GetIngredientesAsync(ReceitaId))
        {
            Ingredientes.Add(item);
        }

        Categorias.Clear();
        foreach (var item in await database.GetCategoriasIngredientesAsync())
        {
            Categorias.Add(item);
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
}
