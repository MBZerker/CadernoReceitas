using CadernoReceitas.Data;
using CadernoReceitas.Models;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using System.Collections.ObjectModel;

namespace CadernoReceitas.ViewModels;

public sealed partial class PracasViewModel : BaseViewModel
{
    private readonly AppDatabase database;

    [ObservableProperty]
    private string nome = string.Empty;

    [ObservableProperty]
    private Restaurante? selectedRestaurante;

    [ObservableProperty]
    private Praca? selectedPraca;

    public ObservableCollection<Restaurante> Restaurantes { get; } = new();

    public ObservableCollection<Praca> Items { get; } = new();

    public PracasViewModel(AppDatabase database)
    {
        this.database = database;
        Title = "Pracas";
    }

    [RelayCommand]
    private async Task LoadAsync()
    {
        Restaurantes.Clear();
        foreach (var item in await database.GetRestaurantesAsync()) Restaurantes.Add(item);
        Items.Clear();
        foreach (var item in await database.GetPracasAsync()) Items.Add(item);
    }

    [RelayCommand]
    private void Select(Praca item)
    {
        SelectedPraca = item;
        Nome = item.Nome;
        SelectedRestaurante = Restaurantes.FirstOrDefault(restaurante => restaurante.Id == item.RestauranteId);
    }

    [RelayCommand]
    private async Task SaveAsync()
    {
        if (string.IsNullOrWhiteSpace(Nome) || SelectedRestaurante is null) return;
        var item = SelectedPraca ?? new Praca();
        item.Nome = Nome.Trim();
        item.RestauranteId = SelectedRestaurante.Id;
        await database.SaveAsync(item);
        Clear();
        await LoadAsync();
    }

    [RelayCommand]
    private async Task DeleteAsync()
    {
        if (SelectedPraca is null) return;
        await database.DeleteAsync(SelectedPraca);
        Clear();
        await LoadAsync();
    }

    [RelayCommand]
    private void Clear()
    {
        SelectedPraca = null;
        SelectedRestaurante = null;
        Nome = string.Empty;
    }
}
