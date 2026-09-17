namespace Wayside.Api.Models.Api;

/// <summary>
/// Categories the app colours pins and chips by. Kept in sync with
/// <c>PlaceCategory</c> in the Android app — changing a name here changes both sides.
/// </summary>
public enum PlaceCategory
{
    Food,
    Nature,
    Culture,
    Viewpoints,
    HiddenGem,
}

/// <summary>A recurring theme across a place's reviews, with how many reviews mention it.</summary>
public sealed record ReviewTheme(string Label, int Count);

/// <summary>A place as it appears in a suggestion list — enough to draw a pin and a card.</summary>
public sealed record PlaceSummary(
    string Id,
    string Name,
    PlaceCategory Category,
    double Rating,
    int RatingCount,
    int DetourMinutes,
    double DistanceKm,
    double Latitude,
    double Longitude,
    /// <summary>How far along the drive this place sits, 0..1. Drives stop ordering.</summary>
    double RouteProgress,
    /// <summary>
    /// How many photos this place has, so the suggestion card can show one when there is one
    /// rather than always falling back to the drawn placeholder.
    /// </summary>
    int PhotoCount = 0);

/// <summary>Everything the detail screen shows, including the generated summary.</summary>
public sealed record PlaceDetail(
    string Id,
    string Name,
    PlaceCategory Category,
    double Rating,
    int RatingCount,
    int DetourMinutes,
    double DistanceKm,
    double Latitude,
    double Longitude,
    double RouteProgress,
    string? OpenUntil,
    string Highlight,
    /// <summary>Generated once when the place enters the database, then served from storage.</summary>
    string AiSummary,
    IReadOnlyList<string> Tags,
    IReadOnlyList<ReviewTheme> ReviewThemes,
    /// <summary>
    /// How many photos this place has. The client asks for them by index at
    /// <c>/places/{id}/photos/{index}</c> — the URLs need the API key, so they are not sent.
    /// </summary>
    int PhotoCount = 0);

/// <summary>Suggestions along one origin → destination drive, within a detour budget.</summary>
public sealed record SuggestionsResponse(
    string Origin,
    string Destination,
    int DriveMinutes,
    double DistanceKm,
    int BudgetMinutes,
    IReadOnlyList<PlaceSummary> Places,
    /// <summary>
    /// The drive itself, as Google's encoded polyline. The client draws this rather than a
    /// straight line between places — a road is not a straight line, and a route that cuts
    /// across a river tells the driver nothing.
    /// </summary>
    string? RoutePolyline = null,
    GeoPointDto? OriginPoint = null,
    GeoPointDto? DestinationPoint = null);

/// <summary>A point, for the ends of the drive.</summary>
public sealed record GeoPointDto(double Latitude, double Longitude);

/// <summary>
/// One drive, with no suggestions attached — what the app draws when it reroutes to a stop
/// the user chose to visit, and again when they are done and heading for the destination.
/// </summary>
public sealed record DirectionsResponse(
    int DriveMinutes,
    double DistanceKm,
    string? Polyline,
    GeoPointDto? OriginPoint,
    GeoPointDto? DestinationPoint);

/// <summary>
/// One row of the search dropdown. Picking a suggestion hands back a place ID, which is what
/// routing should use — free text is re-interpreted on every call and can resolve elsewhere.
/// </summary>
public sealed record PlaceSuggestion(
    string PlaceId,
    string PrimaryText,
    string SecondaryText);
