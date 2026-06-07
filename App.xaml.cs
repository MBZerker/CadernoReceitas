namespace CadernoReceitas;

public partial class App : Application
{
    public App(Services.ThemeService themeService)
    {
        InitializeComponent();
        themeService.ApplySavedTheme();
    }

    protected override Window CreateWindow(IActivationState? activationState)
    {
        return new Window(new AppShell());
    }
}
