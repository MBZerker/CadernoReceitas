using System.Net.Http.Json;
using CadernoReceitas.Models;

namespace CadernoReceitas.Services;

public sealed class UpdateService
{
    public const string ManifestUrl = "https://raw.githubusercontent.com/MBZerker/CadernoReceitas/main/docs/update.json";

    private readonly HttpClient httpClient = new();

    public async Task<UpdateManifest?> CheckAsync()
    {
        try
        {
            var manifest = await httpClient.GetFromJsonAsync<UpdateManifest>(ManifestUrl);
            if (manifest is null || manifest.VersionCode <= CurrentBuildNumber())
            {
                return null;
            }

            return manifest;
        }
        catch
        {
            return null;
        }
    }
    private static int CurrentBuildNumber()
    {
        return int.TryParse(AppInfo.Current.BuildString, out var value) ? value : 0;
    }
}
