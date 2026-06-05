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
}
