using System.Net.Http.Json;
using System.Text.Json.Serialization;
using Microsoft.Extensions.Options;
using Wayside.Api.Configuration;
using Wayside.Api.Entities;
using Wayside.Api.Models.Api;
using Wayside.Api.Services.Interfaces;

namespace Wayside.Api.Services.Google;

/// <summary>
/// Real places along a real drive.
/// </summary>
/// <remarks>
/// The shape of the work: ask Routes for the drive, sample points along its polyline, ask
/// Places what is near each sample, then keep whatever fits the detour budget. Everything
/// upstream stays inside this class — endpoints and the mobile client see only
/// <c>Models.Api</c> types.
/// </remarks>
public sealed class GooglePlacesService(
    HttpClient httpClient,
    IRouteDirectionsService routes,
    IPlaceStore store,
    IOptions<GoogleMapsOptions> options,
    ILogger<GooglePlacesService> logger) : IPlacesService
{
    /// <summary>Types worth stopping for. Anything not on this list is not a detour.</summary>
    private static readonly string[] IncludedTypes =
    [
        "bakery", "cafe", "restaurant",
        "park", "national_park", "hiking_area", "garden",
        "museum", "art_gallery", "historical_landmark", "tourist_attraction",
    ];

    /// <summary>
    /// Types that slip in through a broader one — a shopping centre is a "restaurant" to
    /// Places because it contains them — and that nobody drives off a motorway to see.
    /// </summary>
    private static readonly string[] ExcludedTypes =
    [
        "shopping_mall", "supermarket", "grocery_store", "convenience_store",
        "department_store", "gas_station", "fast_food_restaurant",
    ];

    /// <summary>How far apart the searches are centred along the route.</summary>
    private const double SampleSpacingKm = 8;

    /// <summary>Ceiling on searches per request — each one is a billed Places call.</summary>
    private const int MaxSamples = 4;

    /// <summary>
    /// Deliberately generous. The client asks once at the widest budget and filters locally as
    /// the slider moves, so returning more here costs nothing extra — the searches have already
    /// happened — and stops a narrow budget from leaving two suggestions on screen.
    /// </summary>
    private const int MaxSuggestions = 18;

    /// <summary>Photos are billed per fetch, so a place carries a handful, not a gallery.</summary>
    private const int MaxPhotos = 6;

    /// <summary>How much of each end of the route counts as "where you were going anyway".</summary>
    private const double EdgeMargin = 0.04;

    /// <summary>Assumed speed on the roads leading off the route.</summary>
    private const double DetourSpeedKmh = 45;

    /// <summary>Turning off and rejoining costs time even when the place is on the road.</summary>
    private const int DetourOverheadMinutes = 4;

    private readonly GoogleMapsOptions _options = options.Value;

    public async Task<SuggestionsResponse?> GetSuggestionsAsync(
        string origin,
        string destination,
        int budgetMinutes,
        string? originPlaceId = null,
        string? destinationPlaceId = null,
        CancellationToken cancellationToken = default)
    {
        var route = await routes.ComputeAsync(
            origin, destination, originPlaceId, destinationPlaceId, null, cancellationToken);

        if (route is null || route.Points.Count == 0)
        {
            logger.LogWarning("No route for {Origin} → {Destination}.", origin, destination);
            return null;
        }

        var radiusKm = MaxOffRouteKm(budgetMinutes);
        // Spacing has to grow with the route: a fixed 8km on a 300km drive would spend the
        // whole sample budget in the first 50km and never look at the rest.
        var spacingKm = Math.Max(SampleSpacingKm, route.DistanceKm / MaxSamples);
        var samples = GeoMath.SampleAlong(route.Points, spacingKm).Take(MaxSamples + 1);

        var found = new Dictionary<string, PlaceSummary>();

        foreach (var sample in samples)
        {
            foreach (var place in await SearchNearbyAsync(sample, radiusKm, cancellationToken))
            {
                var (offRouteKm, progress) = GeoMath.NearestOnRoute(
                    route.Points,
                    new GeoPoint(place.Location?.Latitude ?? 0, place.Location?.Longitude ?? 0));

                var detourMinutes = EstimateDetourMinutes(offRouteKm);
                if (detourMinutes > budgetMinutes || place.Id is null)
                {
                    continue;
                }

                // No reviews at this field mask, so this never spends a summary call — it only
                // ever writes name/photos/etc., same fields GetPlaceAsync would refresh later.
                await store.UpsertAsync(ToStored(place), reviews: [], cancellationToken);
                var photoCount = Math.Min(place.Photos?.Count ?? 0, MaxPhotos);

                found[place.Id] = new PlaceSummary(
                    Id: place.Id,
                    Name: place.DisplayName?.Text ?? "Unnamed place",
                    Category: CategoryFor(place),
                    Rating: place.Rating,
                    RatingCount: place.UserRatingCount,
                    DetourMinutes: detourMinutes,
                    DistanceKm: Math.Round(offRouteKm, 1),
                    Latitude: place.Location?.Latitude ?? 0,
                    Longitude: place.Location?.Longitude ?? 0,
                    RouteProgress: Math.Round(progress, 3),
                    PhotoCount: photoCount);
            }
        }

        var suggestions = SpreadAlongRoute(found.Values);

        return new SuggestionsResponse(
            Origin: origin,
            Destination: destination,
            DriveMinutes: route.DriveMinutes,
            DistanceKm: Math.Round(route.DistanceKm, 1),
            BudgetMinutes: budgetMinutes,
            Places: suggestions,
            RoutePolyline: route.EncodedPolyline,
            OriginPoint: ToDto(route.Points[0]),
            DestinationPoint: ToDto(route.Points[^1]));
    }

    private static GeoPointDto ToDto(GeoPoint point) =>
        new(point.Latitude, point.Longitude);

    /// <summary>
    /// Turns a stored photo name into a URL the app can load directly.
    /// </summary>
    /// <remarks>
    /// Asked for with <c>skipHttpRedirect</c> so Google answers with the signed URL as JSON
    /// rather than redirecting the image itself. That URL is safe to hand out — it carries no
    /// key and expires — which means the image bytes never pass through this server.
    /// </remarks>
    public async Task<string?> GetPhotoUrlAsync(
        string placeId,
        int index,
        int maxWidthPx,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(_options.ApiKey))
        {
            return null;
        }

        var stored = await store.FindAsync(placeId, cancellationToken);
        var name = stored?.Photos.ElementAtOrDefault(index);
        if (string.IsNullOrWhiteSpace(name))
        {
            return null;
        }

        var url = $"{_options.PlacesBaseUrl}/{name}/media" +
            $"?maxWidthPx={maxWidthPx}&skipHttpRedirect=true";

        using var message = new HttpRequestMessage(HttpMethod.Get, url);
        message.Headers.Add("X-Goog-Api-Key", _options.ApiKey);

        using var response = await httpClient.SendAsync(message, cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            var body = await response.Content.ReadAsStringAsync(cancellationToken);
            logger.LogWarning("Photo returned {Status}: {Body}", (int)response.StatusCode, body);
            return null;
        }

        var payload = await response.Content.ReadFromJsonAsync<PhotoMediaResponse>(cancellationToken);
        return payload?.PhotoUri;
    }

    private sealed record PhotoMediaResponse(
        [property: JsonPropertyName("photoUri")] string? PhotoUri);

    public async Task<IReadOnlyList<PlaceSuggestion>> AutocompleteAsync(
        string query,
        double? latitude = null,
        double? longitude = null,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(_options.ApiKey) || query.Trim().Length < 2)
        {
            return [];
        }

        var request = new AutocompleteRequest(
            Input: query,
            // Bias, not restrict: a driver searching "Porto" from Lisbon still means Porto.
            LocationBias: latitude is null || longitude is null
                ? null
                : new LocationBias(new Circle(new LatLngPayload(latitude.Value, longitude.Value), 50_000)));

        using var message = new HttpRequestMessage(
            HttpMethod.Post,
            $"{_options.PlacesBaseUrl}/places:autocomplete")
        {
            Content = JsonContent.Create(request),
        };
        message.Headers.Add("X-Goog-Api-Key", _options.ApiKey);
        message.Headers.Add(
            "X-Goog-FieldMask",
            "suggestions.placePrediction.placeId," +
            "suggestions.placePrediction.structuredFormat");

        using var response = await httpClient.SendAsync(message, cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            var body = await response.Content.ReadAsStringAsync(cancellationToken);
            logger.LogWarning("Autocomplete returned {Status}: {Body}", (int)response.StatusCode, body);
            return [];
        }

        var payload = await response.Content.ReadFromJsonAsync<AutocompleteResponse>(cancellationToken);

        return (payload?.Suggestions ?? [])
            .Select(suggestion => suggestion.PlacePrediction)
            .Where(prediction => prediction?.PlaceId is not null)
            .Select(prediction => new PlaceSuggestion(
                PlaceId: prediction!.PlaceId!,
                PrimaryText: prediction.StructuredFormat?.MainText?.Text ?? string.Empty,
                SecondaryText: prediction.StructuredFormat?.SecondaryText?.Text ?? string.Empty))
            .Where(suggestion => !string.IsNullOrWhiteSpace(suggestion.PrimaryText))
            .ToList();
    }

    /// <remarks>
    /// Detour minutes, distance off route and route progress are all properties of a drive, and
    /// this endpoint has no drive — they come back as zero. The client already has them from
    /// the suggestion list it opened the place from.
    /// </remarks>
    public async Task<PlaceDetail?> GetPlaceAsync(
        string placeId,
        CancellationToken cancellationToken = default)
    {
        var stored = await store.FindAsync(placeId, cancellationToken);

        // Only fetch upstream when it isn't already ours — details is the expensive call, and
        // the summary attached to a stored place cost a model run to produce.
        if (stored is null || string.IsNullOrWhiteSpace(stored.AiSummary))
        {
            var fetched = await FetchDetailsAsync(placeId, cancellationToken);
            if (fetched is not null)
            {
                var reviews = fetched.Reviews?
                    .Select(review => review.Text?.Text ?? review.OriginalText?.Text ?? string.Empty)
                    .Where(text => !string.IsNullOrWhiteSpace(text))
                    .ToList() ?? [];

                stored = await store.UpsertAsync(ToStored(fetched), reviews, cancellationToken);
            }
        }

        if (stored is null)
        {
            return null;
        }

        return new PlaceDetail(
            Id: stored.Id,
            Name: stored.Name,
            Category: stored.Category,
            Rating: stored.Rating,
            RatingCount: stored.RatingCount,
            DetourMinutes: 0,
            DistanceKm: 0,
            Latitude: stored.Latitude,
            Longitude: stored.Longitude,
            RouteProgress: 0,
            OpenUntil: stored.OpenUntil,
            Highlight: stored.Highlight,
            AiSummary: stored.AiSummary,
            Tags: stored.Tags,
            ReviewThemes: stored.ReviewThemes,
            PhotoCount: stored.Photos.Count);
    }

    /// <summary>
    /// Picks suggestions across the whole drive rather than the best ones overall.
    /// </summary>
    /// <remarks>
    /// Ordering by rating and taking the top twelve gives you twelve famous places in the
    /// city you left, because that is where the ratings are. Bucketing by how far along the
    /// route a place sits, and taking the best of each bucket, is what makes the far end of
    /// the drive appear at all. Places sitting on the origin or the destination are dropped —
    /// somewhere you are already going is not a detour.
    /// </remarks>
    private static List<PlaceSummary> SpreadAlongRoute(IEnumerable<PlaceSummary> places)
    {
        const int perBucket = 3;
        var buckets = Math.Max(1, MaxSuggestions / perBucket);

        return places
            .Where(place => place.RouteProgress is > EdgeMargin and < 1 - EdgeMargin)
            .GroupBy(place => Math.Min(buckets - 1, (int)(place.RouteProgress * buckets)))
            .SelectMany(bucket => bucket
                .OrderByDescending(place => place.Rating)
                .ThenByDescending(place => place.RatingCount)
                .Take(perBucket))
            .OrderBy(place => place.RouteProgress)
            .Take(MaxSuggestions)
            .ToList();
    }

    private async Task<IReadOnlyList<PlacePayload>> SearchNearbyAsync(
        GeoPoint centre,
        double radiusKm,
        CancellationToken cancellationToken)
    {
        var request = new SearchNearbyRequest(
            IncludedTypes: IncludedTypes,
            ExcludedTypes: ExcludedTypes,
            MaxResultCount: 10,
            LocationRestriction: new LocationRestriction(
                new Circle(
                    new LatLngPayload(centre.Latitude, centre.Longitude),
                    radiusKm * 1000)),
            RankPreference: "POPULARITY");

        using var message = new HttpRequestMessage(
            HttpMethod.Post,
            $"{_options.PlacesBaseUrl}/places:searchNearby")
        {
            Content = JsonContent.Create(request),
        };
        message.Headers.Add("X-Goog-Api-Key", _options.ApiKey);
        message.Headers.Add(
            "X-Goog-FieldMask",
            "places.id,places.displayName,places.location,places.rating," +
            "places.userRatingCount,places.primaryType,places.types,places.photos");

        using var response = await httpClient.SendAsync(message, cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            var body = await response.Content.ReadAsStringAsync(cancellationToken);
            logger.LogWarning("Places search returned {Status}: {Body}", (int)response.StatusCode, body);
            return [];
        }

        var payload = await response.Content.ReadFromJsonAsync<SearchNearbyResponse>(cancellationToken);
        return payload?.Places ?? [];
    }

    private async Task<PlacePayload?> FetchDetailsAsync(
        string placeId,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(_options.ApiKey))
        {
            return null;
        }

        using var message = new HttpRequestMessage(
            HttpMethod.Get,
            $"{_options.PlacesBaseUrl}/places/{placeId}");
        message.Headers.Add("X-Goog-Api-Key", _options.ApiKey);
        message.Headers.Add(
            "X-Goog-FieldMask",
            "id,displayName,location,rating,userRatingCount,primaryType,types," +
            "editorialSummary,regularOpeningHours,reviews,photos");

        using var response = await httpClient.SendAsync(message, cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            var body = await response.Content.ReadAsStringAsync(cancellationToken);
            logger.LogWarning(
                "Place details returned {Status} for {PlaceId}: {Body}",
                (int)response.StatusCode,
                placeId,
                body);
            return null;
        }

        return await response.Content.ReadFromJsonAsync<PlacePayload>(cancellationToken);
    }

    private static StoredPlace ToStored(PlacePayload place) => new()
    {
        Id = place.Id!,
        Name = place.DisplayName?.Text ?? "Unnamed place",
        Category = CategoryFor(place),
        Rating = place.Rating,
        RatingCount = place.UserRatingCount,
        Latitude = place.Location?.Latitude ?? 0,
        Longitude = place.Location?.Longitude ?? 0,
        OpenUntil = ClosingTimeToday(place.RegularOpeningHours),
        Highlight = place.EditorialSummary?.Text ?? string.Empty,
        Tags = HumanisedTypes(place),
        // Names, not URLs — an image URL needs the key, so the app asks us by index instead.
        Photos = (place.Photos ?? [])
            .Select(photo => photo.Name)
            .Where(name => !string.IsNullOrWhiteSpace(name))
            .Take(MaxPhotos)
            .Select(name => name!)
            .ToList(),
    };

    /// <summary>
    /// A detour is out and back, so the driving is doubled, plus a fixed cost for leaving the
    /// route at all. An estimate — the exact figure would need a routed call per candidate,
    /// which is one billed request per place per search.
    /// </summary>
    private static int EstimateDetourMinutes(double offRouteKm) =>
        (int)Math.Round(offRouteKm / DetourSpeedKmh * 60 * 2) + DetourOverheadMinutes;

    /// <summary>The furthest a place can sit off the route and still fit the budget.</summary>
    private static double MaxOffRouteKm(int budgetMinutes)
    {
        var drivingMinutes = Math.Max(0, budgetMinutes - DetourOverheadMinutes) / 2.0;
        var km = drivingMinutes / 60 * DetourSpeedKmh;
        // Places rejects a radius of zero, and anything past 20km stops being a detour.
        return Math.Clamp(km, 1, 20);
    }

    private static PlaceCategory CategoryFor(PlacePayload place)
    {
        var types = new List<string>();
        if (place.PrimaryType is not null)
        {
            types.Add(place.PrimaryType);
        }

        if (place.Types is not null)
        {
            types.AddRange(place.Types);
        }

        foreach (var type in types)
        {
            switch (type)
            {
                case "bakery" or "cafe" or "restaurant" or "market" or "food":
                    return PlaceCategory.Food;
                case "park" or "national_park" or "hiking_area" or "garden":
                    return PlaceCategory.Nature;
                case "museum" or "art_gallery" or "historical_landmark" or "church":
                    return PlaceCategory.Culture;
                case "tourist_attraction" when place.UserRatingCount < 200:
                    return PlaceCategory.HiddenGem;
                case "tourist_attraction":
                    return PlaceCategory.Viewpoints;
            }
        }

        return PlaceCategory.HiddenGem;
    }

    /// <summary>"tourist_attraction" is not something to show a person.</summary>
    private static IReadOnlyList<string> HumanisedTypes(PlacePayload place) =>
        (place.Types ?? [])
            .Take(3)
            .Select(type => type.Replace('_', ' '))
            .Select(type => char.ToUpperInvariant(type[0]) + type[1..])
            .ToList();

    private static string? ClosingTimeToday(OpeningHoursPayload? hours)
    {
        // Google numbers days from Sunday, as does DayOfWeek.
        var today = (int)DateTime.UtcNow.DayOfWeek;

        var period = hours?.Periods?.FirstOrDefault(p => p.Open?.Day == today && p.Close is not null);
        if (period?.Close is null)
        {
            return null;
        }

        return $"{period.Close.Hour:00}:{period.Close.Minute:00}";
    }

    // ---- wire shapes, private to this class --------------------------------------------

    private sealed record SearchNearbyRequest(
        [property: JsonPropertyName("includedTypes")] IReadOnlyList<string> IncludedTypes,
        [property: JsonPropertyName("excludedTypes")] IReadOnlyList<string> ExcludedTypes,
        [property: JsonPropertyName("maxResultCount")] int MaxResultCount,
        [property: JsonPropertyName("locationRestriction")] LocationRestriction LocationRestriction,
        [property: JsonPropertyName("rankPreference")] string RankPreference);

    private sealed record LocationRestriction(
        [property: JsonPropertyName("circle")] Circle Circle);

    private sealed record Circle(
        [property: JsonPropertyName("center")] LatLngPayload Center,
        [property: JsonPropertyName("radius")] double Radius);

    private sealed record LatLngPayload(
        [property: JsonPropertyName("latitude")] double Latitude,
        [property: JsonPropertyName("longitude")] double Longitude);

    private sealed record AutocompleteRequest(
        [property: JsonPropertyName("input")] string Input,
        [property: JsonPropertyName("locationBias")]
        [property: JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
        LocationBias? LocationBias);

    private sealed record LocationBias(
        [property: JsonPropertyName("circle")] Circle Circle);

    private sealed record AutocompleteResponse(
        [property: JsonPropertyName("suggestions")] IReadOnlyList<SuggestionPayload>? Suggestions);

    private sealed record SuggestionPayload(
        [property: JsonPropertyName("placePrediction")] PredictionPayload? PlacePrediction);

    private sealed record PredictionPayload(
        [property: JsonPropertyName("placeId")] string? PlaceId,
        [property: JsonPropertyName("structuredFormat")] StructuredFormatPayload? StructuredFormat);

    private sealed record StructuredFormatPayload(
        [property: JsonPropertyName("mainText")] TextPayload? MainText,
        [property: JsonPropertyName("secondaryText")] TextPayload? SecondaryText);

    private sealed record SearchNearbyResponse(
        [property: JsonPropertyName("places")] IReadOnlyList<PlacePayload>? Places);

    private sealed record PlacePayload(
        [property: JsonPropertyName("id")] string? Id,
        [property: JsonPropertyName("displayName")] TextPayload? DisplayName,
        [property: JsonPropertyName("location")] LatLngPayload? Location,
        [property: JsonPropertyName("rating")] double Rating,
        [property: JsonPropertyName("userRatingCount")] int UserRatingCount,
        [property: JsonPropertyName("primaryType")] string? PrimaryType,
        [property: JsonPropertyName("types")] IReadOnlyList<string>? Types,
        [property: JsonPropertyName("editorialSummary")] TextPayload? EditorialSummary,
        [property: JsonPropertyName("regularOpeningHours")] OpeningHoursPayload? RegularOpeningHours,
        [property: JsonPropertyName("reviews")] IReadOnlyList<ReviewPayload>? Reviews,
        [property: JsonPropertyName("photos")] IReadOnlyList<PhotoPayload>? Photos);

    private sealed record PhotoPayload([property: JsonPropertyName("name")] string? Name);

    private sealed record TextPayload([property: JsonPropertyName("text")] string? Text);

    private sealed record OpeningHoursPayload(
        [property: JsonPropertyName("periods")] IReadOnlyList<PeriodPayload>? Periods);

    private sealed record PeriodPayload(
        [property: JsonPropertyName("open")] TimePointPayload? Open,
        [property: JsonPropertyName("close")] TimePointPayload? Close);

    private sealed record TimePointPayload(
        [property: JsonPropertyName("day")] int Day,
        [property: JsonPropertyName("hour")] int Hour,
        [property: JsonPropertyName("minute")] int Minute);

    private sealed record ReviewPayload(
        [property: JsonPropertyName("text")] TextPayload? Text,
        [property: JsonPropertyName("originalText")] TextPayload? OriginalText);
}
