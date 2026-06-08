using CadernoReceitas.Data;
using CadernoReceitas.Models;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using System.Collections.ObjectModel;

namespace CadernoReceitas.ViewModels;

[QueryProperty(nameof(GrupoId), "grupoId")]
public sealed partial class GrupoViewModel : BaseViewModel
{
    private readonly AppDatabase database;

    [ObservableProperty]
    private int grupoId;

    [ObservableProperty]
    private int cadernoId;

    [ObservableProperty]
    private string nomeGrupo = string.Empty;

    [ObservableProperty]
    private string descricaoGrupo = string.Empty;

    [ObservableProperty]
    private string novaReceitaNome = string.Empty;

    [ObservableProperty]
    private string novaReceitaPreparo = string.Empty;

    public ObservableCollection<Receita> Receitas { get; } = new();

    public GrupoViewModel(AppDatabase database)
    {
        this.database = database;
        Title = "Grupo";
    }

    partial void OnGrupoIdChanged(int value)
    {
        _ = LoadAsync();
    }

    [RelayCommand]
    public async Task LoadAsync()
    {
        if (GrupoId <= 0) return;
        var grupo = await database.GetGrupoAsync(GrupoId);
        CadernoId = grupo?.CadernoId ?? 0;
        NomeGrupo = grupo?.Nome ?? "Grupo";
        DescricaoGrupo = grupo?.Descricao ?? string.Empty;
        Receitas.Clear();
        foreach (var item in await database.GetReceitasDoGrupoAsync(GrupoId))
        {
            Receitas.Add(item);
        }
    }

    [RelayCommand]
    private async Task NovaReceitaAsync()
    {
        if (GrupoId <= 0 || string.IsNullOrWhiteSpace(NovaReceitaNome)) return;
        await database.SaveAsync(new Receita
        {
            CadernoId = CadernoId,
            GrupoId = GrupoId,
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
    private Task VoltarAsync() => Shell.Current.GoToAsync("..");
}
