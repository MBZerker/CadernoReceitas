using CadernoReceitas.Data;
using CadernoReceitas.Models;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using System.Collections.ObjectModel;

namespace CadernoReceitas.ViewModels;

public sealed partial class RestaurantesViewModel : BaseViewModel
{
    private readonly AppDatabase database;

    [ObservableProperty]
    private string nome = string.Empty;

    [ObservableProperty]
    private Restaurante? selectedRestaurante;

    public ObservableCollection<Restaurante> Items { get; } = new();

    public RestaurantesViewModel(AppDatabase database)
    {
        this.database = database;
        Title = "Restaurantes";
    }

    [RelayCommand]
    private async Task LoadAsync()
    {
        Items.Clear();
        foreach (var item in await database.GetRestaurantesAsync())
        {
            Items.Add(item);
        }
    }

    [RelayCommand]
    private void Select(Restaurante item)
    {
        SelectedRestaurante = item;
        Nome = item.Nome;
    }

    [RelayCommand]
    private async Task SaveAsync()
    {
        if (string.IsNullOrWhiteSpace(Nome)) return;
        var item = SelectedRestaurante ?? new Restaurante();
        item.Nome = Nome.Trim();
        await database.SaveAsync(item);
        Clear();
        await LoadAsync();
    }

    [RelayCommand]
    private async Task DeleteAsync()
    {
        if (SelectedRestaurante is null) return;
        await database.DeleteAsync(SelectedRestaurante);
        Clear();
        await LoadAsync();
    }

    [RelayCommand]
    private void Clear()
    {
        SelectedRestaurante = null;
        Nome = string.Empty;
    }
}
