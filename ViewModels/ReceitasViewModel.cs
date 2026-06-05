using CadernoReceitas.Data;
using CadernoReceitas.Models;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using System.Collections.ObjectModel;

namespace CadernoReceitas.ViewModels;

public sealed partial class ReceitasViewModel : BaseViewModel
{
    private readonly AppDatabase database;

    [ObservableProperty]
    private Prato? selectedPrato;

    [ObservableProperty]
    private Receita? selectedReceita;

    [ObservableProperty]
    private string modoPreparo = string.Empty;

    [ObservableProperty]
    private string ingredienteNome = string.Empty;

    [ObservableProperty]
    private string ingredienteQuantidade = string.Empty;

    public ObservableCollection<Prato> Pratos { get; } = new();

    public ObservableCollection<Receita> Items { get; } = new();

    public ObservableCollection<Ingrediente> Ingredientes { get; } = new();

    public ReceitasViewModel(AppDatabase database)
    {
        this.database = database;
        Title = "Receitas";
    }

    [RelayCommand]
    private async Task LoadAsync()
    {
        Pratos.Clear();
        foreach (var item in await database.GetPratosAsync()) Pratos.Add(item);
        Items.Clear();
        foreach (var item in await database.GetReceitasAsync()) Items.Add(item);
    }

    [RelayCommand]
    private async Task SelectAsync(Receita item)
    {
        SelectedReceita = item;
        SelectedPrato = Pratos.FirstOrDefault(prato => prato.Id == item.PratoId);
        ModoPreparo = item.ModoPreparo;
        await LoadIngredientesAsync();
    }

    [RelayCommand]
    private async Task SaveAsync()
    {
        if (SelectedPrato is null || string.IsNullOrWhiteSpace(ModoPreparo)) return;
        var item = SelectedReceita ?? new Receita();
        item.PratoId = SelectedPrato.Id;
        item.ModoPreparo = ModoPreparo.Trim();
        await database.SaveAsync(item);
        SelectedReceita = item;
        await LoadAsync();
        await LoadIngredientesAsync();
    }

    [RelayCommand]
    private async Task DeleteAsync()
    {
        if (SelectedReceita is null) return;
        await database.DeleteReceitaAsync(SelectedReceita);
        Clear();
        await LoadAsync();
    }

    [RelayCommand]
    private async Task AddIngredienteAsync()
    {
        if (SelectedReceita is null || string.IsNullOrWhiteSpace(IngredienteNome)) return;
        await database.SaveAsync(new Ingrediente
        {
            ReceitaId = SelectedReceita.Id,
            Nome = IngredienteNome.Trim(),
            Quantidade = IngredienteQuantidade.Trim()
        });
        IngredienteNome = string.Empty;
        IngredienteQuantidade = string.Empty;
        await LoadIngredientesAsync();
    }

    [RelayCommand]
    private async Task DeleteIngredienteAsync(Ingrediente item)
    {
        await database.DeleteAsync(item);
        await LoadIngredientesAsync();
    }

    [RelayCommand]
    private void Clear()
    {
        SelectedReceita = null;
        SelectedPrato = null;
        ModoPreparo = string.Empty;
        IngredienteNome = string.Empty;
        IngredienteQuantidade = string.Empty;
        Ingredientes.Clear();
    }

    private async Task LoadIngredientesAsync()
    {
        Ingredientes.Clear();
        if (SelectedReceita is null) return;
        foreach (var item in await database.GetIngredientesAsync(SelectedReceita.Id))
        {
            Ingredientes.Add(item);
        }
    }
}
