using SQLite;

namespace CadernoReceitas.Models;

public sealed class Caderno
{
    [PrimaryKey, AutoIncrement]
    public int Id { get; set; }

    [Indexed]
    public string Nome { get; set; } = string.Empty;

    public string Descricao { get; set; } = string.Empty;

    public DateTime CriadoEm { get; set; } = DateTime.Now;

    [Ignore]
    public int TotalReceitas { get; set; }
}
