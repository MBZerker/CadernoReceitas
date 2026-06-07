namespace CadernoReceitas.Services;

public sealed class ThemeService
{
    private const string ThemeKey = "theme_mode";

    public void ApplySavedTheme()
    {
        Application.Current!.UserAppTheme = Preferences.Default.Get(ThemeKey, "system") switch
        {
            "light" => AppTheme.Light,
            "dark" => AppTheme.Dark,
            _ => AppTheme.Unspecified
        };
    }

    public void ToggleTheme()
    {
        var next = Application.Current!.RequestedTheme == AppTheme.Dark ? AppTheme.Light : AppTheme.Dark;
        Application.Current.UserAppTheme = next;
        Preferences.Default.Set(ThemeKey, next == AppTheme.Dark ? "dark" : "light");
    }
}
