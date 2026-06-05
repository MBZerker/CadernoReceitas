using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace CadernoReceitas.ViewModels;

[QueryProperty(nameof(Score), "score")]
[QueryProperty(nameof(Acertos), "acertos")]
[QueryProperty(nameof(Erros), "erros")]
[QueryProperty(nameof(Total), "total")]
[QueryProperty(nameof(Modo), "modo")]
public sealed partial class ResultadoQuizViewModel : BaseViewModel
{
    [ObservableProperty]
    private string score = "0";

    [ObservableProperty]
    private string acertos = "0";

    [ObservableProperty]
    private string erros = "0";

    [ObservableProperty]
    private string total = "0";

    [ObservableProperty]
    private string modo = "Quiz";

    public ResultadoQuizViewModel()
    {
        Title = "Resultado";
    }

    [RelayCommand]
    private Task BackHomeAsync() => Shell.Current.GoToAsync("//home");

    [RelayCommand]
    private Task AgainAsync() => Shell.Current.GoToAsync("//quiz");
}
