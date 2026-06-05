namespace CadernoReceitas.Models;

public sealed class QuizQuestion
{
    public string Enunciado { get; init; } = string.Empty;

    public string RespostaCorreta { get; init; } = string.Empty;

    public IReadOnlyList<string> Alternativas { get; init; } = Array.Empty<string>();
}
