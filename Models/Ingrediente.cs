using SQLite;

namespace CadernoReceitas.Models;

public sealed class Ingrediente
{
    [PrimaryKey, AutoIncrement]
    public int Id { get; set; }

    [Indexed]
    public int ReceitaId { get; set; }

    public string Nome { get; set; } = string.Empty;

    public string Quantidade { get; set; } = string.Empty;

    [Indexed]
    public string Categoria { get; set; } = string.Empty;

    [Indexed]
    public int ReceitaIngredienteId { get; set; }

    [Ignore]
    public string ReceitaIngredienteNome { get; set; } = string.Empty;

    [Ignore]
    public bool TemReceitaVinculada => ReceitaIngredienteId > 0;

    [Ignore]
    public string TipoIngrediente => TemReceitaVinculada ? "Receita preparada" : "Materia-prima";
}
