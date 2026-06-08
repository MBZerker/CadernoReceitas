using System.Net.Http.Json;
using System.Security.Cryptography;
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

    public async Task<string> DownloadAsync(UpdateManifest manifest, IProgress<double>? progress = null)
    {
        if (string.IsNullOrWhiteSpace(manifest.ApkUrl))
        {
            throw new InvalidOperationException("Manifesto sem link do APK.");
        }

        using var response = await httpClient.GetAsync(manifest.ApkUrl, HttpCompletionOption.ResponseHeadersRead);
        response.EnsureSuccessStatusCode();

        var total = response.Content.Headers.ContentLength ?? 0;
        var fileName = $"CadernoReceitas-{manifest.VersionName}.apk";
        var filePath = Path.Combine(FileSystem.CacheDirectory, fileName);

        await using var source = await response.Content.ReadAsStreamAsync();
        await using var destination = File.Create(filePath);
        using var sha = SHA256.Create();
        var buffer = new byte[1024 * 96];
        long readTotal = 0;
        int read;
        while ((read = await source.ReadAsync(buffer)) > 0)
        {
            await destination.WriteAsync(buffer.AsMemory(0, read));
            sha.TransformBlock(buffer, 0, read, null, 0);
            readTotal += read;
            if (total > 0)
            {
                progress?.Report((double)readTotal / total);
            }
        }

        sha.TransformFinalBlock(Array.Empty<byte>(), 0, 0);
        var hash = Convert.ToHexString(sha.Hash ?? Array.Empty<byte>());
        if (!string.IsNullOrWhiteSpace(manifest.Sha256) && !string.Equals(hash, manifest.Sha256, StringComparison.OrdinalIgnoreCase))
        {
            File.Delete(filePath);
            throw new InvalidOperationException("O APK baixado nao confere com o SHA-256 publicado.");
        }

        progress?.Report(1);
        return filePath;
    }

    public Task OpenDownloadedApkAsync(string filePath)
    {
#if ANDROID
        var context = Android.App.Application.Context;
        var file = new Java.IO.File(filePath);
        var uri = AndroidX.Core.Content.FileProvider.GetUriForFile(context, $"{context.PackageName}.fileprovider", file);
        var intent = new Android.Content.Intent(Android.Content.Intent.ActionView);
        intent.SetDataAndType(uri, "application/vnd.android.package-archive");
        intent.AddFlags(Android.Content.ActivityFlags.GrantReadUriPermission | Android.Content.ActivityFlags.NewTask);
        context.StartActivity(intent);
        return Task.CompletedTask;
#else
        return Launcher.Default.OpenAsync(new OpenFileRequest("Abrir APK", new ReadOnlyFile(filePath)));
#endif
    }

    private static int CurrentBuildNumber()
    {
        return int.TryParse(AppInfo.Current.BuildString, out var value) ? value : 0;
    }
}
