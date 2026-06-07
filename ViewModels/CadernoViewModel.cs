using CadernoReceitas.Data;
using CadernoReceitas.Models;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using System.Collections.ObjectModel;

namespace CadernoReceitas.ViewModels;

[QueryProperty(nameof(CadernoId), "cadernoId")]
public sealed partial class CadernoViewModel : BaseViewModel
{
    private readonly AppDatabase database;

    [ObservableProperty]
    private int cadernoId;

    [ObservableProperty]
    private string nomeCaderno = string.Empty;

    [ObservableProperty]
    private string descricaoCaderno = string.Empty;

    [ObservableProperty]
    private string novaReceitaNome = string.Empty;

    [ObservableProperty]
    private string novaReceitaPreparo = string.Empty;

    public ObservableCollection<Receita> Receitas { get; } = new();

    public CadernoViewModel(AppDatabase database)
    {
        this.database = database;
        Title = "Caderno";
    }

    partial void OnCadernoIdChanged(int value)
    {
        _ = LoadAsync();
    }

    [RelayCommand]
    private async Task LoadAsync()
    {
        if (CadernoId <= 0) return;
        var caderno = await database.GetCadernoAsync(CadernoId);
        NomeCaderno = caderno?.Nome ?? "Caderno";
        DescricaoCaderno = caderno?.Descricao ?? string.Empty;
        Receitas.Clear();
        foreach (var item in await database.GetReceitasDoCadernoAsync(CadernoId))
        {
            Receitas.Add(item);
        }
    }

    [RelayCommand]
    private async Task NovaReceitaAsync()
    {
        if (CadernoId <= 0 || string.IsNullOrWhiteSpace(NovaReceitaNome)) return;
        await database.SaveAsync(new Receita
        {
            CadernoId = CadernoId,
            Nome = NovaReceitaNome.Trim(),
            ModoPreparo = NovaReceitaPreparo.Trim()
        });
        NovaReceitaNome = string.Empty;
        NovaReceitaPreparo = string.Empty;
        await LoadAsync();
    }

    [RelayCommand]
    private Task AbrirReceitaAsync(Receita receita)
    {
        return Shell.Current.GoToAsync($"receitaDetalhe?receitaId={receita.Id}");
    }

    [RelayCommand]
    private Task VoltarAsync() => Shell.Current.GoToAsync("//principal");
}
