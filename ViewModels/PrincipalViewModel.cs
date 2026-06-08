using CadernoReceitas.Data;
using CadernoReceitas.Models;
using CadernoReceitas.Services;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using System.Collections.ObjectModel;

namespace CadernoReceitas.ViewModels;

public sealed partial class PrincipalViewModel : BaseViewModel
{
    private readonly AppDatabase database;
    private readonly UpdateService updateService;

    [ObservableProperty]
    private string status = string.Empty;

    public ObservableCollection<Caderno> Cadernos { get; } = new();

    public PrincipalViewModel(AppDatabase database, UpdateService updateService)
    {
        this.database = database;
        this.updateService = updateService;
        Title = "Principal";
    }

    [RelayCommand]
    private async Task LoadAsync()
    {
        await database.SeedAsync();
        Cadernos.Clear();
        foreach (var item in await database.GetCadernosAsync())
        {
            Cadernos.Add(item);
        }
    }

    [RelayCommand]
    private async Task NovoCadernoAsync()
    {
        var nome = await Shell.Current.DisplayPromptAsync("Novo caderno", "Nome do caderno de receitas:");
        if (string.IsNullOrWhiteSpace(nome)) return;
        var descricao = await Shell.Current.DisplayPromptAsync("Novo caderno", "Descricao curta:", "Salvar", "Pular");
        await database.SaveAsync(new Caderno
        {
            Nome = nome.Trim(),
            Descricao = descricao?.Trim() ?? string.Empty,
            CriadoEm = DateTime.Now
        });
        await LoadAsync();
    }

    [RelayCommand]
    private Task AbrirCadernoAsync(Caderno caderno)
    {
        return Shell.Current.GoToAsync($"caderno?cadernoId={caderno.Id}");
    }

    [RelayCommand]
    private Task TestesAsync() => Shell.Current.GoToAsync("quiz");

    [RelayCommand]
    private async Task CheckUpdatesAsync()
    {
        Status = "Verificando atualizacao...";
        var manifest = await updateService.CheckAsync();
        if (manifest is null)
        {
            Status = "Sem atualizacao disponivel.";
            return;
        }

        Status = $"Versao {manifest.VersionName} disponivel.";
        if (!string.IsNullOrWhiteSpace(manifest.PageUrl))
        {
            await Launcher.Default.OpenAsync(manifest.PageUrl);
        }
    }
}
