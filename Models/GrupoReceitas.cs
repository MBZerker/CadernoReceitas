using SQLite;

namespace CadernoReceitas.Models;

public sealed class GrupoReceitas
{
    [PrimaryKey, AutoIncrement]
    public int Id { get; set; }

    [Indexed]
    public int CadernoId { get; set; }

    public string Nome { get; set; } = string.Empty;

    public string Descricao { get; set; } = string.Empty;

    public DateTime CriadoEm { get; set; } = DateTime.Now;

    [Ignore]
    public int TotalReceitas { get; set; }
}
