namespace Wayside.Api.Configuration;

/// <summary>
/// Anthropic API settings. The key is never sent to the mobile client — summaries are
/// generated here and stored, so the app only ever sees the finished text.
/// </summary>
public sealed class AnthropicOptions
{
    public const string SectionName = "Anthropic";

    /// <summary>Read from user-secrets in development, from the host's secret store in production.</summary>
    public string ApiKey { get; init; } = string.Empty;

    public string Model { get; init; } = "claude-opus-5";

    /// <summary>Ceiling per summary. A place summary is two or three sentences.</summary>
    public int MaxTokens { get; init; } = 1024;
}
