using CadernoReceitas.ViewModels;

namespace CadernoReceitas.Views;

public partial class QuizPage : ContentPage
{
    private readonly QuizViewModel viewModel;

    public QuizPage(QuizViewModel viewModel)
    {
        InitializeComponent();
        BindingContext = this.viewModel = viewModel;
    }

    protected override async void OnAppearing()
    {
        base.OnAppearing();
        if (viewModel.Alternativas.Count == 0)
        {
            await viewModel.StartCommand.ExecuteAsync("Quiz");
        }
    }
}
