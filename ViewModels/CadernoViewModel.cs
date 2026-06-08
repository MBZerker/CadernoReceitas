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
    private string novoGrupoNome = string.Empty;

    [ObservableProperty]
    private string novoGrupoDescricao = string.Empty;

    public ObservableCollection<GrupoReceitas> Grupos { get; } = new();

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
    public async Task LoadAsync()
    {
        if (CadernoId <= 0) return;
        var caderno = await database.GetCadernoAsync(CadernoId);
        NomeCaderno = caderno?.Nome ?? "Caderno";
        DescricaoCaderno = caderno?.Descricao ?? string.Empty;
        Grupos.Clear();
        foreach (var item in await database.GetGruposDoCadernoAsync(CadernoId))
        {
            Grupos.Add(item);
        }
    }

    [RelayCommand]
    private async Task NovoGrupoAsync()
    {
        if (CadernoId <= 0 || string.IsNullOrWhiteSpace(NovoGrupoNome)) return;
        await database.SaveAsync(new GrupoReceitas
        {
            CadernoId = CadernoId,
            Nome = NovoGrupoNome.Trim(),
            Descricao = NovoGrupoDescricao.Trim(),
            CriadoEm = DateTime.Now
        });

        NovoGrupoNome = string.Empty;
        NovoGrupoDescricao = string.Empty;
        await LoadAsync();
    }

    [RelayCommand]
    private Task AbrirGrupoAsync(GrupoReceitas grupo)
    {
        return Shell.Current.GoToAsync($"grupo?grupoId={grupo.Id}");
    }

    [RelayCommand]
    private Task VoltarAsync() => Shell.Current.GoToAsync("//principal");
}
