using Wayside.Api.Models.Api;
using Wayside.Api.Services.Interfaces;

namespace Wayside.Api.Services;

/// <summary>
/// Serves a fixed set of places so the API answers real requests before Google Places is
/// wired up. Swap the DI registration in <c>Program.cs</c> for <c>GooglePlacesService</c>
/// to go live; the endpoints and the mobile client don't change.
/// </summary>
public sealed class SamplePlacesService : IPlacesService
{
    private static readonly PlaceDetail[] Places =
    [
        new(
            Id: "sacolinha",
            Name: "Pastelaria Sacolinha",
            Category: PlaceCategory.Food,
            Rating: 4.6,
            RatingCount: 312,
            DetourMinutes: 6,
            DistanceKm: 1.8,
            Latitude: 38.7570,
            Longitude: -9.2200,
            RouteProgress: 0.14,
            OpenUntil: "19:00",
            Highlight: "The custard tarts come out of the oven at 10:00 and sell out by noon.",
            AiSummary: "Reviewers keep coming back for the pastéis de nata straight from the "
                + "oven, and note the counter staff will tell you when the next tray is due.",
            Tags: ["Pastéis de nata", "Quick stop", "Cash preferred"],
            ReviewThemes:
            [
                new("Fresh pastries", 184),
                new("Fast service", 96),
                new("Small seating area", 41),
            ]),
        new(
            Id: "poetas",
            Name: "Parque dos Poetas",
            Category: PlaceCategory.Nature,
            Rating: 4.5,
            RatingCount: 2204,
            DetourMinutes: 9,
            DistanceKm: 3.1,
            Latitude: 38.7075,
            Longitude: -9.2822,
            RouteProgress: 0.38,
            OpenUntil: "20:00",
            Highlight: "Twenty sculptures of Portuguese poets scattered across open lawns.",
            AiSummary: "People treat this as a leg-stretch stop: flat paths, shade in the upper "
                + "terrace, and sculptures signposted with the poems they belong to.",
            Tags: ["Sculpture walk", "Shade", "Free entry"],
            ReviewThemes:
            [
                new("Sculptures", 908),
                new("Good for walking", 615),
                new("Limited parking", 142),
            ]),
        new(
            Id: "agualva",
            Name: "Mercado de Agualva",
            Category: PlaceCategory.Food,
            Rating: 4.3,
            RatingCount: 428,
            DetourMinutes: 12,
            DistanceKm: 2.2,
            Latitude: 38.7700,
            Longitude: -9.3050,
            RouteProgress: 0.46,
            OpenUntil: "14:00",
            Highlight: "A working produce market where the fish stalls close first, around 13:00.",
            AiSummary: "Regulars say the cheese and cured meat counters at the back are the "
                + "reason to come, and that most stalls have packed up by early afternoon.",
            Tags: ["Produce market", "Local cheese", "Mornings only"],
            ReviewThemes:
            [
                new("Cheese counter", 201),
                new("Fair prices", 133),
                new("Closes early", 78),
            ]),
        new(
            Id: "sabuga",
            Name: "Fonte da Sabuga",
            Category: PlaceCategory.HiddenGem,
            Rating: 4.4,
            RatingCount: 96,
            DetourMinutes: 21,
            DistanceKm: 5.0,
            Latitude: 38.7930,
            Longitude: -9.3830,
            RouteProgress: 0.62,
            OpenUntil: "22:00",
            Highlight: "A tiled spring fountain on a lane most people drive straight past.",
            AiSummary: "The few reviews agree on the same thing: it takes five minutes, and the "
                + "lane is narrow enough that parking means walking the last 200 metres.",
            Tags: ["Tiled fountain", "Five-minute stop", "Narrow lane"],
            ReviewThemes:
            [
                new("Azulejo tilework", 48),
                new("Quiet", 31),
                new("Hard to park", 22),
            ]),
        new(
            Id: "queluz",
            Name: "Palácio de Queluz",
            Category: PlaceCategory.Culture,
            Rating: 4.7,
            RatingCount: 1841,
            DetourMinutes: 14,
            DistanceKm: 2.6,
            Latitude: 38.7503,
            Longitude: -9.2586,
            RouteProgress: 0.30,
            OpenUntil: "18:00",
            Highlight: "A rococo summer palace with formal gardens you can walk in 30 minutes.",
            AiSummary: "Visitors describe the tiled canal and the throne room as the two things "
                + "worth the ticket, and most finish the gardens in about half an hour.",
            Tags: ["Rococo palace", "Formal gardens", "Ticketed entry"],
            ReviewThemes:
            [
                new("Gardens", 742),
                new("Tiled canal", 410),
                new("Queues at midday", 188),
            ]),
        new(
            Id: "capuchos",
            Name: "Convento dos Capuchos",
            Category: PlaceCategory.Culture,
            Rating: 4.8,
            RatingCount: 1207,
            DetourMinutes: 34,
            DistanceKm: 9.2,
            Latitude: 38.7847,
            Longitude: -9.4278,
            RouteProgress: 0.74,
            OpenUntil: "18:30",
            Highlight: "Cork-lined monk cells built into the rock of the Sintra hills.",
            AiSummary: "Almost every review mentions the cork-lined cells and how low the "
                + "doorways are; the walk between them is uneven and damp after rain.",
            Tags: ["Cork cells", "Forest setting", "Uneven paths"],
            ReviewThemes:
            [
                new("Cork architecture", 561),
                new("Peaceful", 344),
                new("Slippery when wet", 129),
            ]),
        new(
            Id: "peninha",
            Name: "Miradouro da Peninha",
            Category: PlaceCategory.Viewpoints,
            Rating: 4.8,
            RatingCount: 486,
            DetourMinutes: 43,
            DistanceKm: 12.6,
            Latitude: 38.7822,
            Longitude: -9.4561,
            RouteProgress: 0.86,
            OpenUntil: "20:30",
            Highlight: "On a clear afternoon you can see Cabo da Roca and the whole coastline.",
            AiSummary: "Reviewers say the view reaches Cabo da Roca when the sea fog lifts, and "
                + "that the last stretch of road is single-lane with passing places.",
            Tags: ["Coastal view", "Sunset spot", "Single-lane road"],
            ReviewThemes:
            [
                new("Coastline view", 298),
                new("Windy", 121),
                new("Rough final road", 74),
            ]),
    ];

    public Task<SuggestionsResponse?> GetSuggestionsAsync(
        string origin,
        string destination,
        int budgetMinutes,
        string? originPlaceId = null,
        string? destinationPlaceId = null,
        CancellationToken cancellationToken = default)
    {
        var withinBudget = Places
            .Where(p => p.DetourMinutes <= budgetMinutes)
            .OrderBy(p => p.RouteProgress)
            .Select(p => new PlaceSummary(
                p.Id, p.Name, p.Category, p.Rating, p.RatingCount, p.DetourMinutes,
                p.DistanceKm, p.Latitude, p.Longitude, p.RouteProgress))
            .ToList();

        return Task.FromResult<SuggestionsResponse?>(new SuggestionsResponse(
            Origin: origin,
            Destination: destination,
            DriveMinutes: 52,
            DistanceKm: 28,
            BudgetMinutes: budgetMinutes,
            Places: withinBudget));
    }

    /// <summary>
    /// Matches on the fixed set, so the dropdown works without a key. There are no place IDs
    /// here, so the sample id stands in — routing against it would fail, which is correct:
    /// without a Maps key there is no routing either.
    /// </summary>
    public Task<IReadOnlyList<PlaceSuggestion>> AutocompleteAsync(
        string query,
        double? latitude = null,
        double? longitude = null,
        CancellationToken cancellationToken = default)
    {
        var matches = Places
            .Where(place => place.Name.Contains(query, StringComparison.OrdinalIgnoreCase))
            .Select(place => new PlaceSuggestion(place.Id, place.Name, "Sample data"))
            .ToList();

        return Task.FromResult<IReadOnlyList<PlaceSuggestion>>(matches);
    }

    /// <summary>Sample places have no photos — there is no upstream to fetch them from.</summary>
    public Task<string?> GetPhotoUrlAsync(
        string placeId,
        int index,
        int maxWidthPx,
        CancellationToken cancellationToken = default) => Task.FromResult<string?>(null);

    public Task<PlaceDetail?> GetPlaceAsync(
        string placeId,
        CancellationToken cancellationToken = default)
        => Task.FromResult(Places.FirstOrDefault(p => p.Id == placeId));
}
