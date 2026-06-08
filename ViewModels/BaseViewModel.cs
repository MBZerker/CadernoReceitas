using CommunityToolkit.Mvvm.ComponentModel;
using System.Globalization;
using System.Text;

namespace CadernoReceitas.ViewModels;

public abstract partial class BaseViewModel : ObservableObject
{
    [ObservableProperty]
    private bool isBusy;

    [ObservableProperty]
    private string title = string.Empty;

    protected static bool MatchesSearch(string text, string search)
    {
        if (string.IsNullOrWhiteSpace(search))
        {
            return true;
        }

        return NormalizeSearch(text).Contains(NormalizeSearch(search), StringComparison.OrdinalIgnoreCase);
    }

    protected static string NormalizeSearch(string value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return string.Empty;
        }

        var normalized = value.Normalize(NormalizationForm.FormD);
        var builder = new StringBuilder(normalized.Length);
        foreach (var c in normalized)
        {
            if (CharUnicodeInfo.GetUnicodeCategory(c) != UnicodeCategory.NonSpacingMark)
            {
                builder.Append(char.ToLowerInvariant(c));
            }
        }

        return builder.ToString().Normalize(NormalizationForm.FormC);
    }
}
