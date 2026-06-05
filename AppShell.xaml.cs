namespace CadernoReceitas;

public partial class AppShell : Shell
{
    public AppShell()
    {
        InitializeComponent();
        Routing.RegisterRoute("resultado", typeof(Views.ResultadoQuizPage));
    }
}
