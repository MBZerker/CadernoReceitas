using CadernoReceitas.Data;
using CadernoReceitas.Models;
using CadernoReceitas.Services;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using System.Collections.ObjectModel;

namespace CadernoReceitas.ViewModels;

public sealed partial class HomeViewModel : BaseViewModel
{
    private readonly AppDatabase database;
    private readonly UpdateService updateService;

    [ObservableProperty]
    private int restaurantes;

    [ObservableProperty]
    private int pracas;

    [ObservableProperty]
    private int pratos;

    [ObservableProperty]
    private int receitas;

    [ObservableProperty]
    private int ingredientes;

    [ObservableProperty]
    private string updateStatus = "Toque para verificar atualizacoes.";

    public ObservableCollection<QuizHistory> Historico { get; } = new();

    public HomeViewModel(AppDatabase database, UpdateService updateService)
    {
        this.database = database;
        this.updateService = updateService;
        Title = "Caderno de Receitas";
    }

    [RelayCommand]
    private async Task LoadAsync()
    {
        await database.SeedAsync();
        var stats = await database.GetStatsAsync();
        Restaurantes = stats["Restaurantes"];
        Pracas = stats["Pracas"];
        Pratos = stats["Pratos"];
        Receitas = stats["Receitas"];
        Ingredientes = stats["Ingredientes"];

        Historico.Clear();
        foreach (var item in (await database.GetHistoricoAsync()).Take(5))
        {
            Historico.Add(item);
        }
    }

    [RelayCommand]
    private Task GoToQuizAsync() => Shell.Current.GoToAsync("//quiz");

    [RelayCommand]
    private async Task CheckUpdatesAsync()
    {
        UpdateStatus = "Verificando...";
        var manifest = await updateService.CheckAsync();
        if (manifest is null)
        {
            UpdateStatus = "Voce esta na versao mais recente ou sem conexao.";
            return;
        }

        UpdateStatus = $"Versao {manifest.VersionName} disponivel.";
        if (!string.IsNullOrWhiteSpace(manifest.PageUrl))
        {
            await Launcher.Default.OpenAsync(manifest.PageUrl);
        }
    }
}
