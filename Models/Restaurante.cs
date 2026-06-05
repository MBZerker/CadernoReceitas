using SQLite;

namespace CadernoReceitas.Models;

public sealed class Restaurante
{
    [PrimaryKey, AutoIncrement]
    public int Id { get; set; }

    [Indexed]
    public string Nome { get; set; } = string.Empty;
}
