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
    private readonly List<GrupoReceitas> todosGrupos = new();

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

    [ObservableProperty]
    private string searchText = string.Empty;

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
        todosGrupos.Clear();
        todosGrupos.AddRange(await database.GetGruposDoCadernoAsync(CadernoId));
        ApplySearch();
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

    partial void OnSearchTextChanged(string value) => ApplySearch();

    private void ApplySearch()
    {
        Grupos.Clear();
        foreach (var item in todosGrupos.Where(item =>
            MatchesSearch(item.Nome, SearchText) ||
            MatchesSearch(item.Descricao, SearchText)))
        {
            Grupos.Add(item);
        }
    }

    [RelayCommand]
    private Task AbrirGrupoAsync(GrupoReceitas grupo)
    {
        return Shell.Current.GoToAsync($"grupo?grupoId={grupo.Id}");
    }

    [RelayCommand]
    private async Task MenuGrupoAsync(GrupoReceitas grupo)
    {
        var action = await Shell.Current.DisplayActionSheet(grupo.Nome, "Cancelar", null, "Editar", "Excluir");
        if (action == "Editar")
        {
            var nome = await Shell.Current.DisplayPromptAsync("Editar categoria", "Nome:", "Salvar", "Cancelar", initialValue: grupo.Nome);
            if (string.IsNullOrWhiteSpace(nome)) return;
            var descricao = await Shell.Current.DisplayPromptAsync("Editar categoria", "Descricao:", "Salvar", "Manter", initialValue: grupo.Descricao);
            grupo.Nome = nome.Trim();
            if (descricao is not null)
            {
                grupo.Descricao = descricao.Trim();
            }

            await database.SaveAsync(grupo);
            await LoadAsync();
            return;
        }

        if (action == "Excluir")
        {
            var confirm = await Shell.Current.DisplayAlert("Excluir categoria", $"Excluir \"{grupo.Nome}\" e todas as receitas dentro dela?", "Excluir", "Cancelar");
            if (!confirm) return;
            await database.DeleteGrupoAsync(grupo);
            await LoadAsync();
        }
    }

    [RelayCommand]
    private Task VoltarAsync() => Shell.Current.GoToAsync("//principal");
}
