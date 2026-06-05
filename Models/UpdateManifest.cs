namespace CadernoReceitas.Models;

public sealed class UpdateManifest
{
    public int VersionCode { get; set; }

    public string VersionName { get; set; } = string.Empty;

    public bool Critical { get; set; }

    public string PageUrl { get; set; } = string.Empty;

    public string ApkUrl { get; set; } = string.Empty;

    public string Sha256 { get; set; } = string.Empty;

    public string SignatureSha256 { get; set; } = string.Empty;

    public string Origin { get; set; } = string.Empty;

    public string Changelog { get; set; } = string.Empty;
}
