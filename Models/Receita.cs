using SQLite;

namespace CadernoReceitas.Models;

public sealed class Receita
{
    [PrimaryKey, AutoIncrement]
    public int Id { get; set; }

    [Indexed]
    public int PratoId { get; set; }

    public string ModoPreparo { get; set; } = string.Empty;

    [Ignore]
    public string PratoNome { get; set; } = string.Empty;
}
