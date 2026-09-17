namespace Wayside.Api.Configuration;

/// <summary>
/// Gemini API settings. The key stays server-side: summaries are generated here and stored,
/// so the mobile client only ever sees finished text.
/// </summary>
/// <remarks>
/// A Google AI Studio key has a free tier, which is why summaries run on Gemini rather than
/// on a provider that bills from the first call. The key is unrelated to
/// <see cref="GoogleMapsOptions"/> — that one is a Maps Platform key and cannot call this API.
/// </remarks>
public sealed class GeminiOptions
{
    public const string SectionName = "Gemini";

    /// <summary>Read from user-secrets in development, from the host's secret store in production.</summary>
    public string ApiKey { get; init; } = string.Empty;

    /// <summary>
    /// Summarising supplied review text is not reasoning-heavy, so the cheapest, fastest model
    /// is the right one. Configured rather than hardcoded so it can be raised without a deploy.
    /// </summary>
    public string Model { get; init; } = "gemini-3.5-flash-lite";

    public string BaseUrl { get; init; } = "https://generativelanguage.googleapis.com/v1beta";

    /// <summary>Ceiling per summary. A place summary is two or three sentences.</summary>
    public int MaxOutputTokens { get; init; } = 512;

    /// <summary>False when no key is configured — the service then returns no summary.</summary>
    public bool IsConfigured => !string.IsNullOrWhiteSpace(ApiKey);
}
