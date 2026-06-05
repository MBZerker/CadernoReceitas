using CadernoReceitas.Data;
using CadernoReceitas.Models;
using CadernoReceitas.Services;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using System.Collections.ObjectModel;

namespace CadernoReceitas.ViewModels;

public sealed partial class QuizViewModel : BaseViewModel
{
    private readonly QuizService quizService;
    private readonly AppDatabase database;
    private List<QuizQuestion> questions = new();
    private int currentIndex;
    private int acertos;

    [ObservableProperty]
    private string mode = "Quiz";

    [ObservableProperty]
    private string questionText = "Cadastre receitas para iniciar.";

    [ObservableProperty]
    private string progressText = "0/0";

    [ObservableProperty]
    private string feedback = string.Empty;

    [ObservableProperty]
    private bool isFinished;

    public ObservableCollection<string> Alternativas { get; } = new();

    public QuizViewModel(QuizService quizService, AppDatabase database)
    {
        this.quizService = quizService;
        this.database = database;
        Title = "Quiz";
    }

    [RelayCommand]
    private async Task StartAsync(string? mode)
    {
        Mode = string.IsNullOrWhiteSpace(mode) ? "Quiz" : mode;
        questions = await quizService.CreateQuizAsync(Mode == "Prova" ? 10 : 5);
        currentIndex = 0;
        acertos = 0;
        IsFinished = false;
        Feedback = string.Empty;
        ShowCurrent();
    }

    [RelayCommand]
    private async Task AnswerAsync(string resposta)
    {
        if (IsFinished || currentIndex >= questions.Count) return;
        var correct = string.Equals(resposta, questions[currentIndex].RespostaCorreta, StringComparison.OrdinalIgnoreCase);
        if (correct) acertos++;
        Feedback = correct ? "Resposta correta." : $"Resposta certa: {questions[currentIndex].RespostaCorreta}";
        currentIndex++;
        await Task.Delay(550);
        if (currentIndex >= questions.Count)
        {
            await FinishAsync();
            return;
        }

        ShowCurrent();
    }

    private void ShowCurrent()
    {
        Alternativas.Clear();
        if (questions.Count == 0)
        {
            QuestionText = "Cadastre pratos, receitas e ingredientes para liberar o quiz.";
            ProgressText = "0/0";
            return;
        }

        var question = questions[currentIndex];
        QuestionText = question.Enunciado;
        ProgressText = $"{currentIndex + 1}/{questions.Count}";
        Feedback = string.Empty;
        foreach (var item in question.Alternativas)
        {
            Alternativas.Add(item);
        }
    }

    private async Task FinishAsync()
    {
        var total = questions.Count;
        var erros = total - acertos;
        var score = total == 0 ? 0 : Math.Round(acertos * 100d / total, 1);
        await database.SaveAsync(new QuizHistory
        {
            Modo = Mode,
            TotalPerguntas = total,
            Acertos = acertos,
            Erros = erros,
            Pontuacao = score,
            RealizadoEm = DateTime.Now
        });
        IsFinished = true;
        QuestionText = $"{Mode} finalizado";
        ProgressText = $"{score:0.#}%";
        Feedback = $"Acertos: {acertos} | Erros: {erros}";
        await Shell.Current.GoToAsync($"resultado?score={score}&acertos={acertos}&erros={erros}&total={total}&modo={Uri.EscapeDataString(Mode)}");
    }
}
