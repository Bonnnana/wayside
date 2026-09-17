using System.Net.Http.Json;
using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.Extensions.Options;
using Wayside.Api.Configuration;
using Wayside.Api.Models.Api;
using Wayside.Api.Services.Interfaces;

namespace Wayside.Api.Services.Ai;

/// <inheritdoc />
/// <remarks>
/// Talks to the REST endpoint directly rather than through an SDK: the call is one POST, and
/// a dependency that has to be kept in step with a fast-moving API earns its place only if it
/// does more than this.
/// </remarks>
public sealed class GeminiSummaryService : ISummaryService
{
    private const string SystemPrompt =
        """
        You summarise reviews of places a driver might stop at on a road trip.

        Write two or three sentences describing what reviewers consistently say. Report only
        what the reviews support — if they disagree, say so; if something is mentioned once,
        leave it out. Prefer the concrete detail a traveller can act on (when the pastries come
        out of the oven, that the last stretch of road is single-lane) over adjectives.

        Do not open with the place name, do not recommend, and do not use marketing language.

        Also pick up to three recurring themes. A theme is two or three words naming what the
        reviews keep coming back to ("Fresh pastries", "Hard to park"), with a count of how many
        of the supplied reviews mention it. Count only the reviews you were given. A theme
        mentioned in one review is not recurring — leave it out.

        Return JSON only, in this exact shape:
        {"summary": "...", "themes": [{"label": "...", "count": 0}]}
        """;

    private readonly HttpClient _httpClient;
    private readonly GeminiOptions _options;
    private readonly ILogger<GeminiSummaryService> _logger;

    public GeminiSummaryService(
        HttpClient httpClient,
        IOptions<GeminiOptions> options,
        ILogger<GeminiSummaryService> logger)
    {
        _httpClient = httpClient;
        _options = options.Value;
        _logger = logger;
    }

    public async Task<PlaceInsights> SummariseReviewsAsync(
        string placeName,
        IReadOnlyList<string> reviews,
        CancellationToken cancellationToken = default)
    {
        if (reviews.Count == 0)
        {
            return PlaceInsights.None;
        }

        if (!_options.IsConfigured)
        {
            // Not an error: the app is expected to run without an AI key, serving whatever
            // summary text is already stored.
            _logger.LogInformation("No Gemini key configured; skipping summary for {Place}.", placeName);
            return PlaceInsights.None;
        }

        var request = new GenerateContentRequest(
            SystemInstruction: new Content([new Part(SystemPrompt)]),
            Contents:
            [
                new Content(
                    [
                        new Part(
                            $"""
                            Place: {placeName}

                            Reviews:
                            {string.Join("\n\n", reviews.Select(review => $"- {review}"))}
                            """),
                    ],
                    Role: "user"),
            ],
            GenerationConfig: new GenerationConfig(
                MaxOutputTokens: _options.MaxOutputTokens,
                // Low, because the job is to report what the reviews say, not to write prose.
                Temperature: 0.2,
                // Asking for JSON in the prompt is a request; this makes it a guarantee.
                ResponseMimeType: "application/json"));

        var url = $"{_options.BaseUrl}/models/{_options.Model}:generateContent";

        using var message = new HttpRequestMessage(HttpMethod.Post, url)
        {
            Content = JsonContent.Create(request),
        };
        // Header rather than a query parameter, so the key never lands in a proxy access log.
        message.Headers.Add("x-goog-api-key", _options.ApiKey);

        using var response = await _httpClient.SendAsync(message, cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            var body = await response.Content.ReadAsStringAsync(cancellationToken);
            _logger.LogWarning(
                "Gemini returned {Status} for {Place}: {Body}",
                (int)response.StatusCode,
                placeName,
                body);
            return PlaceInsights.None;
        }

        var payload = await response.Content.ReadFromJsonAsync<GenerateContentResponse>(
            cancellationToken);

        var candidate = payload?.Candidates?.FirstOrDefault();

        // A blocked or truncated answer comes back as HTTP 200 with a finish reason that is
        // not STOP, and often with no parts at all — so check before reading the text.
        if (candidate is null || (candidate.FinishReason is not null and not "STOP"))
        {
            _logger.LogWarning(
                "No usable summary for {Place}. Finish reason: {Reason}",
                placeName,
                candidate?.FinishReason ?? "none");
            return PlaceInsights.None;
        }

        var text = candidate.Content?.Parts?
            .Select(part => part.Text)
            .Where(part => !string.IsNullOrWhiteSpace(part));

        var json = text is null ? string.Empty : string.Join("\n", text).Trim();
        return Parse(json, placeName);
    }

    private PlaceInsights Parse(string json, string placeName)
    {
        if (string.IsNullOrWhiteSpace(json))
        {
            return PlaceInsights.None;
        }

        try
        {
            var parsed = JsonSerializer.Deserialize<InsightsPayload>(json);
            if (parsed is null)
            {
                return PlaceInsights.None;
            }

            var themes = (parsed.Themes ?? [])
                .Where(theme => !string.IsNullOrWhiteSpace(theme.Label) && theme.Count > 0)
                .Select(theme => new ReviewTheme(theme.Label!.Trim(), theme.Count))
                .Take(3)
                .ToList();

            return new PlaceInsights(parsed.Summary?.Trim() ?? string.Empty, themes);
        }
        catch (JsonException ex)
        {
            // The model ignored the schema. Losing the summary is better than storing
            // something that will render as a wall of JSON on the detail screen.
            _logger.LogWarning(ex, "Unparseable insights JSON for {Place}.", placeName);
            return PlaceInsights.None;
        }
    }

    // Wire shapes, kept private: nothing outside this class should know Gemini exists.
    private sealed record GenerateContentRequest(
        [property: JsonPropertyName("system_instruction")] Content SystemInstruction,
        [property: JsonPropertyName("contents")] IReadOnlyList<Content> Contents,
        [property: JsonPropertyName("generationConfig")] GenerationConfig GenerationConfig);

    private sealed record Content(
        [property: JsonPropertyName("parts")] IReadOnlyList<Part> Parts,
        [property: JsonPropertyName("role")] string? Role = null);

    private sealed record Part([property: JsonPropertyName("text")] string Text);

    private sealed record GenerationConfig(
        [property: JsonPropertyName("maxOutputTokens")] int MaxOutputTokens,
        [property: JsonPropertyName("temperature")] double Temperature,
        [property: JsonPropertyName("responseMimeType")] string ResponseMimeType);

    private sealed record GenerateContentResponse(
        [property: JsonPropertyName("candidates")] IReadOnlyList<Candidate>? Candidates);

    private sealed record Candidate(
        [property: JsonPropertyName("content")] Content? Content,
        [property: JsonPropertyName("finishReason")] string? FinishReason);

    private sealed record InsightsPayload(
        [property: JsonPropertyName("summary")] string? Summary,
        [property: JsonPropertyName("themes")] IReadOnlyList<ThemePayload>? Themes);

    private sealed record ThemePayload(
        [property: JsonPropertyName("label")] string? Label,
        [property: JsonPropertyName("count")] int Count);
}
