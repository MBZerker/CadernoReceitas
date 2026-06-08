using CadernoReceitas.Models;
using SQLite;

namespace CadernoReceitas.Data;

public sealed class AppDatabase
{
    private readonly SQLiteAsyncConnection database;
    private bool initialized;

    public AppDatabase()
    {
        var path = Path.Combine(FileSystem.AppDataDirectory, "caderno-receitas.db3");
        database = new SQLiteAsyncConnection(path, SQLiteOpenFlags.Create | SQLiteOpenFlags.ReadWrite | SQLiteOpenFlags.SharedCache);
    }

    public async Task InitializeAsync()
    {
        if (initialized)
        {
            return;
        }

        await database.CreateTableAsync<Caderno>();
        await database.CreateTableAsync<GrupoReceitas>();
        await database.CreateTableAsync<Restaurante>();
        await database.CreateTableAsync<Praca>();
        await database.CreateTableAsync<Prato>();
        await database.CreateTableAsync<Receita>();
        await database.CreateTableAsync<Ingrediente>();
        await database.CreateTableAsync<QuizHistory>();
        await EnsureColumnAsync("Receita", "GrupoId", "GrupoId INTEGER NOT NULL DEFAULT 0");
        await EnsureColumnAsync("Ingrediente", "ReceitaIngredienteId", "ReceitaIngredienteId INTEGER NOT NULL DEFAULT 0");
        await EnsureRecipeNotebookAsync();
        await EnsureRecipeGroupsAsync();
        initialized = true;
    }

    public async Task<List<Caderno>> GetCadernosAsync()
    {
        await InitializeAsync();
        var cadernos = await database.Table<Caderno>().OrderBy(item => item.Nome).ToListAsync();
        foreach (var caderno in cadernos)
        {
            caderno.TotalReceitas = await database.Table<Receita>().Where(item => item.CadernoId == caderno.Id).CountAsync();
        }

        return cadernos;
    }

    public async Task<List<GrupoReceitas>> GetGruposDoCadernoAsync(int cadernoId)
    {
        await InitializeAsync();
        var grupos = await database.Table<GrupoReceitas>().Where(item => item.CadernoId == cadernoId).OrderBy(item => item.Nome).ToListAsync();
        foreach (var grupo in grupos)
        {
            grupo.TotalReceitas = await database.Table<Receita>().Where(item => item.GrupoId == grupo.Id).CountAsync();
        }

        return grupos;
    }

    public async Task<List<Receita>> GetReceitasDoGrupoAsync(int grupoId)
    {
        await InitializeAsync();
        var receitas = await database.Table<Receita>().Where(item => item.GrupoId == grupoId).OrderBy(item => item.Nome).ToListAsync();
        foreach (var receita in receitas)
        {
            receita.TotalIngredientes = await database.Table<Ingrediente>().Where(item => item.ReceitaId == receita.Id).CountAsync();
        }

        return receitas;
    }

    public async Task<List<Receita>> GetReceitasDoCadernoAsync(int cadernoId)
    {
        await InitializeAsync();
        var receitas = await database.Table<Receita>().Where(item => item.CadernoId == cadernoId).OrderBy(item => item.Nome).ToListAsync();
        foreach (var receita in receitas)
        {
            receita.TotalIngredientes = await database.Table<Ingrediente>().Where(item => item.ReceitaId == receita.Id).CountAsync();
        }

        return receitas;
    }

    public async Task<Caderno?> GetCadernoAsync(int id)
    {
        await InitializeAsync();
        return await database.Table<Caderno>().FirstOrDefaultAsync(item => item.Id == id);
    }

    public async Task<GrupoReceitas?> GetGrupoAsync(int id)
    {
        await InitializeAsync();
        var grupo = await database.Table<GrupoReceitas>().FirstOrDefaultAsync(item => item.Id == id);
        if (grupo is not null)
        {
            grupo.TotalReceitas = await database.Table<Receita>().Where(item => item.GrupoId == grupo.Id).CountAsync();
        }

        return grupo;
    }

    public async Task<Receita?> GetReceitaAsync(int id)
    {
        await InitializeAsync();
        var receita = await database.Table<Receita>().FirstOrDefaultAsync(item => item.Id == id);
        if (receita is not null)
        {
            receita.TotalIngredientes = await database.Table<Ingrediente>().Where(item => item.ReceitaId == receita.Id).CountAsync();
            receita.GrupoNome = (await database.Table<GrupoReceitas>().FirstOrDefaultAsync(item => item.Id == receita.GrupoId))?.Nome ?? string.Empty;
        }

        return receita;
    }

    public async Task<List<Receita>> GetReceitasParaIngredienteAsync(int receitaAtualId)
    {
        await InitializeAsync();
        return await database.Table<Receita>()
            .Where(item => item.Id != receitaAtualId)
            .OrderBy(item => item.Nome)
            .ToListAsync();
    }

    public async Task<List<Ingrediente>> GetCatalogoIngredientesAsync()
    {
        await InitializeAsync();
        var ingredientes = await database.Table<Ingrediente>().OrderBy(item => item.Categoria).ThenBy(item => item.Nome).ToListAsync();
        await HydrateIngredientesAsync(ingredientes);
        return ingredientes;
    }

    public async Task<List<string>> GetCategoriasIngredientesAsync()
    {
        await InitializeAsync();
        return (await database.Table<Ingrediente>().ToListAsync())
            .Select(item => item.Categoria)
            .Where(item => !string.IsNullOrWhiteSpace(item))
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .OrderBy(item => item)
            .ToList();
    }

    public async Task<List<Restaurante>> GetRestaurantesAsync()
    {
        await InitializeAsync();
        return await database.Table<Restaurante>().OrderBy(item => item.Nome).ToListAsync();
    }

    public async Task<List<Praca>> GetPracasAsync()
    {
        await InitializeAsync();
        var restaurantes = await GetRestaurantesAsync();
        var items = await database.Table<Praca>().OrderBy(item => item.Nome).ToListAsync();
        foreach (var item in items)
        {
            item.RestauranteNome = restaurantes.FirstOrDefault(restaurante => restaurante.Id == item.RestauranteId)?.Nome ?? "Sem restaurante";
        }

        return items;
    }

    public async Task<List<Prato>> GetPratosAsync()
    {
        await InitializeAsync();
        var pracas = await GetPracasAsync();
        var items = await database.Table<Prato>().OrderBy(item => item.Nome).ToListAsync();
        foreach (var item in items)
        {
            item.PracaNome = pracas.FirstOrDefault(praca => praca.Id == item.PracaId)?.Nome ?? "Sem praca";
        }

        return items;
    }

    public async Task<List<Receita>> GetReceitasAsync()
    {
        await InitializeAsync();
        var pratos = await GetPratosAsync();
        var grupos = await database.Table<GrupoReceitas>().ToListAsync();
        var items = await database.Table<Receita>().OrderBy(item => item.Nome).ToListAsync();
        foreach (var item in items)
        {
            var pratoNome = pratos.FirstOrDefault(prato => prato.Id == item.PratoId)?.Nome;
            item.PratoNome = string.IsNullOrWhiteSpace(pratoNome) ? item.Nome : pratoNome;
            item.GrupoNome = grupos.FirstOrDefault(grupo => grupo.Id == item.GrupoId)?.Nome ?? string.Empty;
            item.TotalIngredientes = await database.Table<Ingrediente>().Where(ingrediente => ingrediente.ReceitaId == item.Id).CountAsync();
        }

        return items;
    }

    public async Task<List<Ingrediente>> GetIngredientesAsync(int receitaId)
    {
        await InitializeAsync();
        var ingredientes = await database.Table<Ingrediente>()
            .Where(item => item.ReceitaId == receitaId)
            .OrderBy(item => item.Categoria)
            .ThenBy(item => item.Nome)
            .ToListAsync();
        await HydrateIngredientesAsync(ingredientes);
        return ingredientes;
    }

    public async Task<List<QuizHistory>> GetHistoricoAsync()
    {
        await InitializeAsync();
        return await database.Table<QuizHistory>().OrderByDescending(item => item.RealizadoEm).ToListAsync();
    }

    public async Task SaveAsync<T>(T item) where T : new()
    {
        await InitializeAsync();
        var idProperty = typeof(T).GetProperty("Id");
        var id = (int)(idProperty?.GetValue(item) ?? 0);
        if (id == 0)
        {
            await database.InsertAsync(item);
        }
        else
        {
            await database.UpdateAsync(item);
        }
    }

    public async Task DeleteAsync<T>(T item) where T : new()
    {
        await InitializeAsync();
        await database.DeleteAsync(item);
    }

    public async Task DeleteCadernoAsync(Caderno caderno)
    {
        await InitializeAsync();
        var grupos = await GetGruposDoCadernoAsync(caderno.Id);
        foreach (var grupo in grupos)
        {
            await DeleteGrupoAsync(grupo);
        }

        var receitasSemGrupo = (await GetReceitasDoCadernoAsync(caderno.Id)).Where(item => item.GrupoId == 0).ToList();
        foreach (var receita in receitasSemGrupo)
        {
            await DeleteReceitaAsync(receita);
        }

        await database.DeleteAsync(caderno);
    }

    public async Task DeleteGrupoAsync(GrupoReceitas grupo)
    {
        await InitializeAsync();
        var receitas = await GetReceitasDoGrupoAsync(grupo.Id);
        foreach (var receita in receitas)
        {
            await DeleteReceitaAsync(receita);
        }

        await database.DeleteAsync(grupo);
    }

    public async Task DeleteReceitaAsync(Receita receita)
    {
        await InitializeAsync();
        var ingredientes = await GetIngredientesAsync(receita.Id);
        foreach (var item in ingredientes)
        {
            await database.DeleteAsync(item);
        }

        await database.DeleteAsync(receita);
    }

    public async Task<Dictionary<string, int>> GetStatsAsync()
    {
        await InitializeAsync();
        return new Dictionary<string, int>
        {
            ["Cadernos"] = await database.Table<Caderno>().CountAsync(),
            ["Grupos"] = await database.Table<GrupoReceitas>().CountAsync(),
            ["Restaurantes"] = await database.Table<Restaurante>().CountAsync(),
            ["Pracas"] = await database.Table<Praca>().CountAsync(),
            ["Pratos"] = await database.Table<Prato>().CountAsync(),
            ["Receitas"] = await database.Table<Receita>().CountAsync(),
            ["Ingredientes"] = await database.Table<Ingrediente>().CountAsync()
        };
    }

    public async Task SeedAsync()
    {
        await InitializeAsync();
        if (await database.Table<Caderno>().CountAsync() > 0)
        {
            return;
        }

        var caderno = new Caderno { Nome = "Caderno Modelo", Descricao = "Receitas base para organizar e treinar a equipe." };
        await database.InsertAsync(caderno);

        var grupoBases = new GrupoReceitas { CadernoId = caderno.Id, Nome = "Bases e molhos", Descricao = "Preparos que tambem podem entrar como ingredientes." };
        await database.InsertAsync(grupoBases);
        var grupoPratos = new GrupoReceitas { CadernoId = caderno.Id, Nome = "Pratos principais", Descricao = "Receitas finais prontas para preparo." };
        await database.InsertAsync(grupoPratos);

        var restaurante = new Restaurante { Nome = "Restaurante Modelo" };
        await database.InsertAsync(restaurante);
        var praca = new Praca { Nome = "Cozinha Quente", RestauranteId = restaurante.Id };
        await database.InsertAsync(praca);
        var prato = new Prato { Nome = "Lasanha cremosa", PracaId = praca.Id };
        await database.InsertAsync(prato);

        var molho = new Receita
        {
            CadernoId = caderno.Id,
            GrupoId = grupoBases.Id,
            Nome = "Molho branco",
            ModoPreparo = "Derreta a manteiga, misture farinha, acrescente leite aos poucos e cozinhe ate engrossar."
        };
        await database.InsertAsync(molho);
        await database.InsertAllAsync(new[]
        {
            new Ingrediente { ReceitaId = molho.Id, Categoria = "Gordura", Nome = "Manteiga", Quantidade = "40 g" },
            new Ingrediente { ReceitaId = molho.Id, Categoria = "Seco", Nome = "Farinha de trigo", Quantidade = "40 g" },
            new Ingrediente { ReceitaId = molho.Id, Categoria = "Laticinio", Nome = "Leite", Quantidade = "500 ml" }
        });

        var receita = new Receita
        {
            CadernoId = caderno.Id,
            GrupoId = grupoPratos.Id,
            PratoId = prato.Id,
            Nome = "Lasanha cremosa",
            ModoPreparo = "Monte camadas de massa, recheio e molho branco. Finalize com queijo e leve ao forno."
        };
        await database.InsertAsync(receita);
        await database.InsertAllAsync(new[]
        {
            new Ingrediente { ReceitaId = receita.Id, Categoria = "Massa", Nome = "Massa de lasanha", Quantidade = "300 g" },
            new Ingrediente { ReceitaId = receita.Id, Categoria = "Receita preparada", Nome = "Molho branco", Quantidade = "500 ml", ReceitaIngredienteId = molho.Id },
            new Ingrediente { ReceitaId = receita.Id, Categoria = "Laticinio", Nome = "Queijo", Quantidade = "120 g" }
        });
    }

    private async Task EnsureRecipeNotebookAsync()
    {
        if (await database.Table<Caderno>().CountAsync() > 0)
        {
            return;
        }

        var receitas = await database.Table<Receita>().ToListAsync();
        if (receitas.Count == 0)
        {
            return;
        }

        var caderno = new Caderno { Nome = "Caderno Geral", Descricao = "Receitas migradas da versao inicial." };
        await database.InsertAsync(caderno);
        foreach (var receita in receitas.Where(item => item.CadernoId == 0))
        {
            receita.CadernoId = caderno.Id;
            if (string.IsNullOrWhiteSpace(receita.Nome))
            {
                receita.Nome = string.IsNullOrWhiteSpace(receita.PratoNome) ? $"Receita {receita.Id}" : receita.PratoNome;
            }

            await database.UpdateAsync(receita);
        }
    }

    private async Task EnsureRecipeGroupsAsync()
    {
        var receitasSemGrupo = (await database.Table<Receita>().ToListAsync()).Where(item => item.GrupoId == 0).ToList();
        if (receitasSemGrupo.Count == 0)
        {
            return;
        }

        var grupos = await database.Table<GrupoReceitas>().ToListAsync();
        foreach (var cadernoId in receitasSemGrupo.Select(item => item.CadernoId).Distinct())
        {
            var grupo = grupos.FirstOrDefault(item => item.CadernoId == cadernoId && string.Equals(item.Nome, "Geral", StringComparison.OrdinalIgnoreCase));
            if (grupo is null)
            {
                grupo = new GrupoReceitas { CadernoId = cadernoId, Nome = "Geral", Descricao = "Receitas migradas para um grupo inicial." };
                await database.InsertAsync(grupo);
                grupos.Add(grupo);
            }

            foreach (var receita in receitasSemGrupo.Where(item => item.CadernoId == cadernoId))
            {
                receita.GrupoId = grupo.Id;
                await database.UpdateAsync(receita);
            }
        }
    }

    private async Task HydrateIngredientesAsync(IEnumerable<Ingrediente> ingredientes)
    {
        foreach (var ingrediente in ingredientes.Where(item => item.ReceitaIngredienteId > 0))
        {
            ingrediente.ReceitaIngredienteNome = (await database.Table<Receita>().FirstOrDefaultAsync(item => item.Id == ingrediente.ReceitaIngredienteId))?.Nome ?? string.Empty;
        }
    }

    private async Task EnsureColumnAsync(string tableName, string columnName, string columnDefinition)
    {
        var columns = await database.QueryAsync<TableColumnInfo>($"PRAGMA table_info({tableName})");
        if (columns.Any(item => string.Equals(item.name, columnName, StringComparison.OrdinalIgnoreCase)))
        {
            return;
        }

        await database.ExecuteAsync($"ALTER TABLE {tableName} ADD COLUMN {columnDefinition}");
    }

    private sealed class TableColumnInfo
    {
        public string name { get; set; } = string.Empty;
    }
}
