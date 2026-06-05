using CadernoReceitas.ViewModels;

namespace CadernoReceitas.Views;

public partial class ResultadoQuizPage : ContentPage
{
    public ResultadoQuizPage(ResultadoQuizViewModel viewModel)
    {
        InitializeComponent();
        BindingContext = viewModel;
    }
}
