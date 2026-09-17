using System.Text.Json;
using Wayside.Api.Models.Api;

namespace Wayside.Api.Entities;

/// <summary>
/// A place as it is kept between requests.
/// </summary>
/// <remarks>
/// Only what belongs to the place itself lives here. Detour minutes, distance off-route and
/// route progress are properties of a *drive*, not of a place — the same palace is a 14-minute
/// detour from one route and unreachable from another — so they are computed per request and
/// never stored.
///
/// Google's terms allow place IDs to be kept indefinitely but limit caching of most other
/// Places content; derived data of our own, including the generated summary, is not subject
/// to that. <see cref="RefreshedUtc"/> is what a cache-expiry sweep would work from.
/// </remarks>
public sealed class StoredPlace
{
    /// <summary>The Google place ID. Stable, and the one field that may be kept forever.</summary>
    public required string Id { get; set; }

    public required string Name { get; set; }

    public PlaceCategory Category { get; set; }

    public double Rating { get; set; }

    public int RatingCount { get; set; }

    public double Latitude { get; set; }

    public double Longitude { get; set; }

    public string? OpenUntil { get; set; }

    public string Highlight { get; set; } = string.Empty;

    /// <summary>Generated once, from reviews, when the place is first stored.</summary>
    public string AiSummary { get; set; } = string.Empty;

    public DateTime? SummaryGeneratedUtc { get; set; }

    /// <summary>When the upstream place data was last written.</summary>
    public DateTime RefreshedUtc { get; set; }

    // Stored as JSON rather than as child tables: both are short, read as a whole, and never
    // queried across places.
    public string TagsJson { get; set; } = "[]";

    public string ReviewThemesJson { get; set; } = "[]";

    /// <summary>
    /// Google's photo resource names, not URLs. Fetching the image needs the API key, so the
    /// app asks this API for a photo by index and gets redirected to a signed URL.
    /// </summary>
    public string PhotosJson { get; set; } = "[]";

    public IReadOnlyList<string> Tags
    {
        get => Deserialise<string>(TagsJson);
        set => TagsJson = JsonSerializer.Serialize(value);
    }

    public IReadOnlyList<ReviewTheme> ReviewThemes
    {
        get => Deserialise<ReviewTheme>(ReviewThemesJson);
        set => ReviewThemesJson = JsonSerializer.Serialize(value);
    }

    public IReadOnlyList<string> Photos
    {
        get => Deserialise<string>(PhotosJson);
        set => PhotosJson = JsonSerializer.Serialize(value);
    }

    private static IReadOnlyList<T> Deserialise<T>(string json) =>
        string.IsNullOrWhiteSpace(json)
            ? []
            : JsonSerializer.Deserialize<List<T>>(json) ?? [];
}
