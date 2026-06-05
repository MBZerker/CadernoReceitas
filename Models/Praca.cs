using SQLite;

namespace CadernoReceitas.Models;

public sealed class Praca
{
    [PrimaryKey, AutoIncrement]
    public int Id { get; set; }

    [Indexed]
    public string Nome { get; set; } = string.Empty;

    [Indexed]
    public int RestauranteId { get; set; }

    [Ignore]
    public string RestauranteNome { get; set; } = string.Empty;
}
