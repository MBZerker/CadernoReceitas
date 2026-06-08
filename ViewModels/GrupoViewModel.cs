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
    private readonly List<Receita> todasReceitas = new();

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

    [ObservableProperty]
    private string searchText = string.Empty;

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
        todasReceitas.Clear();
        todasReceitas.AddRange(await database.GetReceitasDoGrupoAsync(GrupoId));
        ApplySearch();
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

    partial void OnSearchTextChanged(string value) => ApplySearch();

    private void ApplySearch()
    {
        Receitas.Clear();
        foreach (var item in todasReceitas.Where(item =>
            MatchesSearch(item.Nome, SearchText) ||
            MatchesSearch(item.ModoPreparo, SearchText)))
        {
            Receitas.Add(item);
        }
    }

    [RelayCommand]
    private Task AbrirReceitaAsync(Receita receita)
    {
        return Shell.Current.GoToAsync($"receitaDetalhe?receitaId={receita.Id}");
    }

    [RelayCommand]
    private async Task MenuReceitaAsync(Receita receita)
    {
        var action = await Shell.Current.DisplayActionSheet(receita.Nome, "Cancelar", null, "Editar", "Excluir");
        if (action == "Editar")
        {
            var nome = await Shell.Current.DisplayPromptAsync("Editar receita", "Nome:", "Salvar", "Cancelar", initialValue: receita.Nome);
            if (string.IsNullOrWhiteSpace(nome)) return;
            var preparo = await Shell.Current.DisplayPromptAsync("Editar receita", "Modo de preparo:", "Salvar", "Manter", initialValue: receita.ModoPreparo);
            receita.Nome = nome.Trim();
            if (preparo is not null)
            {
                receita.ModoPreparo = preparo.Trim();
            }

            await database.SaveAsync(receita);
            await LoadAsync();
            return;
        }

        if (action == "Excluir")
        {
            var confirm = await Shell.Current.DisplayAlert("Excluir receita", $"Excluir \"{receita.Nome}\" e seus ingredientes?", "Excluir", "Cancelar");
            if (!confirm) return;
            await database.DeleteReceitaAsync(receita);
            await LoadAsync();
        }
    }

    [RelayCommand]
    private Task VoltarAsync() => Shell.Current.GoToAsync("..");
}
