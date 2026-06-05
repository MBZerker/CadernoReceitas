using SQLite;

namespace CadernoReceitas.Models;

public sealed class Prato
{
    [PrimaryKey, AutoIncrement]
    public int Id { get; set; }

    [Indexed]
    public string Nome { get; set; } = string.Empty;

    [Indexed]
    public int PracaId { get; set; }

    [Ignore]
    public string PracaNome { get; set; } = string.Empty;
}
