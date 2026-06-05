using SQLite;

namespace CadernoReceitas.Models;

public sealed class QuizHistory
{
    [PrimaryKey, AutoIncrement]
    public int Id { get; set; }

    [Indexed]
    public DateTime RealizadoEm { get; set; } = DateTime.Now;

    public string Modo { get; set; } = "Quiz";

    public int TotalPerguntas { get; set; }

    public int Acertos { get; set; }

    public int Erros { get; set; }

    public double Pontuacao { get; set; }
}
