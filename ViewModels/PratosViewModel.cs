using CadernoReceitas.Data;
using CadernoReceitas.Models;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using System.Collections.ObjectModel;

namespace CadernoReceitas.ViewModels;

public sealed partial class PratosViewModel : BaseViewModel
{
    private readonly AppDatabase database;

    [ObservableProperty]
    private string nome = string.Empty;

    [ObservableProperty]
    private Praca? selectedPraca;

    [ObservableProperty]
    private Prato? selectedPrato;

    public ObservableCollection<Praca> Pracas { get; } = new();

    public ObservableCollection<Prato> Items { get; } = new();

    public PratosViewModel(AppDatabase database)
    {
        this.database = database;
        Title = "Pratos";
    }

    [RelayCommand]
    private async Task LoadAsync()
    {
        Pracas.Clear();
        foreach (var item in await database.GetPracasAsync()) Pracas.Add(item);
        Items.Clear();
        foreach (var item in await database.GetPratosAsync()) Items.Add(item);
    }

    [RelayCommand]
    private void Select(Prato item)
    {
        SelectedPrato = item;
        Nome = item.Nome;
        SelectedPraca = Pracas.FirstOrDefault(praca => praca.Id == item.PracaId);
    }

    [RelayCommand]
    private async Task SaveAsync()
    {
        if (string.IsNullOrWhiteSpace(Nome) || SelectedPraca is null) return;
        var item = SelectedPrato ?? new Prato();
        item.Nome = Nome.Trim();
        item.PracaId = SelectedPraca.Id;
        await database.SaveAsync(item);
        Clear();
        await LoadAsync();
    }

    [RelayCommand]
    private async Task DeleteAsync()
    {
        if (SelectedPrato is null) return;
        await database.DeleteAsync(SelectedPrato);
        Clear();
        await LoadAsync();
    }

    [RelayCommand]
    private void Clear()
    {
        SelectedPrato = null;
        SelectedPraca = null;
        Nome = string.Empty;
    }
}
