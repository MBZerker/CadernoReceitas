using SQLite;

namespace CadernoReceitas.Models;

public sealed class Receita
{
    [PrimaryKey, AutoIncrement]
    public int Id { get; set; }

    [Indexed]
    public int PratoId { get; set; }

    [Indexed]
    public int CadernoId { get; set; }

    public string Nome { get; set; } = string.Empty;

    public string ModoPreparo { get; set; } = string.Empty;

    [Ignore]
    public string PratoNome { get; set; } = string.Empty;

    [Ignore]
    public int TotalIngredientes { get; set; }
}
