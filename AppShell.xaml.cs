namespace CadernoReceitas;

public partial class AppShell : Shell
{
    public AppShell()
    {
        InitializeComponent();
        Routing.RegisterRoute("caderno", typeof(Views.CadernoPage));
        Routing.RegisterRoute("grupo", typeof(Views.GrupoPage));
        Routing.RegisterRoute("receitaDetalhe", typeof(Views.ReceitaDetalhePage));
        Routing.RegisterRoute("quiz", typeof(Views.QuizPage));
        Routing.RegisterRoute("resultado", typeof(Views.ResultadoQuizPage));
    }
}
