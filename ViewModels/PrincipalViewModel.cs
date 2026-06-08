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
    private readonly List<Caderno> todosCadernos = new();

    [ObservableProperty]
    private string status = string.Empty;

    [ObservableProperty]
    private string searchText = string.Empty;

    [ObservableProperty]
    private bool isDownloadingUpdate;

    [ObservableProperty]
    private double updateProgress;

    public ObservableCollection<Caderno> Cadernos { get; } = new();

    public ObservableCollection<string> SugestoesCadernos { get; } = new();

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
        todosCadernos.Clear();
        todosCadernos.AddRange(await database.GetCadernosAsync());

        SugestoesCadernos.Clear();
        foreach (var item in todosCadernos.Select(item => item.Nome).Where(item => !string.IsNullOrWhiteSpace(item)).Distinct(StringComparer.OrdinalIgnoreCase))
        {
            SugestoesCadernos.Add(item);
        }

        ApplySearch();
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

    partial void OnSearchTextChanged(string value) => ApplySearch();

    private void ApplySearch()
    {
        Cadernos.Clear();
        foreach (var item in todosCadernos.Where(item =>
            MatchesSearch(item.Nome, SearchText) ||
            MatchesSearch(item.Descricao, SearchText)))
        {
            Cadernos.Add(item);
        }
    }

    [RelayCommand]
    private Task AbrirCadernoAsync(Caderno caderno)
    {
        return Shell.Current.GoToAsync($"caderno?cadernoId={caderno.Id}");
    }

    [RelayCommand]
    private async Task MenuCadernoAsync(Caderno caderno)
    {
        var action = await Shell.Current.DisplayActionSheet(caderno.Nome, "Cancelar", null, "Editar", "Excluir");
        if (action == "Editar")
        {
            var nome = await Shell.Current.DisplayPromptAsync("Editar caderno", "Nome:", "Salvar", "Cancelar", initialValue: caderno.Nome);
            if (string.IsNullOrWhiteSpace(nome)) return;
            var descricao = await Shell.Current.DisplayPromptAsync("Editar caderno", "Descricao:", "Salvar", "Manter", initialValue: caderno.Descricao);
            caderno.Nome = nome.Trim();
            if (descricao is not null)
            {
                caderno.Descricao = descricao.Trim();
            }

            await database.SaveAsync(caderno);
            await LoadAsync();
            return;
        }

        if (action == "Excluir")
        {
            var confirm = await Shell.Current.DisplayAlert("Excluir caderno", $"Excluir \"{caderno.Nome}\" e todo o conteudo dentro dele?", "Excluir", "Cancelar");
            if (!confirm) return;
            await database.DeleteCadernoAsync(caderno);
            await LoadAsync();
        }
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
        var confirm = await Shell.Current.DisplayAlert("Atualizacao disponivel", $"Versao {manifest.VersionName} disponivel. Deseja baixar e instalar agora?", "Baixar", "Depois");
        if (!confirm)
        {
            return;
        }

        IsDownloadingUpdate = true;
        UpdateProgress = 0;
        try
        {
            var file = await updateService.DownloadAsync(manifest, new Progress<double>(value => UpdateProgress = value));
            Status = "Download concluido. Abrindo instalador...";
            await updateService.OpenDownloadedApkAsync(file);
        }
        catch (Exception ex)
        {
            Status = "Nao foi possivel baixar a atualizacao.";
            await Shell.Current.DisplayAlert("Atualizacao", $"Falha ao baixar a atualizacao: {ex.Message}", "OK");
            if (!string.IsNullOrWhiteSpace(manifest.PageUrl))
            {
                await Launcher.Default.OpenAsync(manifest.PageUrl);
            }
        }
        finally
        {
            IsDownloadingUpdate = false;
        }
    }
}
