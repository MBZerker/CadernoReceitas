using CadernoReceitas.Data;
using CadernoReceitas.Services;
using CadernoReceitas.ViewModels;
using CadernoReceitas.Views;
using Microsoft.Extensions.Logging;

namespace CadernoReceitas;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();
        builder
            .UseMauiApp<App>()
            .ConfigureFonts(fonts =>
            {
                fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
                fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
            });

#if DEBUG
        builder.Logging.AddDebug();
#endif

        builder.Services.AddSingleton<AppDatabase>();
        builder.Services.AddSingleton<QuizService>();
        builder.Services.AddSingleton<UpdateService>();
        builder.Services.AddSingleton<ThemeService>();

        builder.Services.AddTransient<PrincipalViewModel>();
        builder.Services.AddTransient<CadernoViewModel>();
        builder.Services.AddTransient<ReceitaDetalheViewModel>();
        builder.Services.AddTransient<HomeViewModel>();
        builder.Services.AddTransient<RestaurantesViewModel>();
        builder.Services.AddTransient<PracasViewModel>();
        builder.Services.AddTransient<PratosViewModel>();
        builder.Services.AddTransient<ReceitasViewModel>();
        builder.Services.AddTransient<QuizViewModel>();
        builder.Services.AddTransient<ResultadoQuizViewModel>();

        builder.Services.AddTransient<PrincipalPage>();
        builder.Services.AddTransient<CadernoPage>();
        builder.Services.AddTransient<ReceitaDetalhePage>();
        builder.Services.AddTransient<HomePage>();
        builder.Services.AddTransient<RestaurantesPage>();
        builder.Services.AddTransient<PracasPage>();
        builder.Services.AddTransient<PratosPage>();
        builder.Services.AddTransient<ReceitasPage>();
        builder.Services.AddTransient<QuizPage>();
        builder.Services.AddTransient<ResultadoQuizPage>();

        return builder.Build();
    }
}
