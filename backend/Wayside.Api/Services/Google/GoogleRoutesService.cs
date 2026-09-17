using System.Globalization;
using System.Net.Http.Json;
using System.Text.Json.Serialization;
using Microsoft.Extensions.Options;
using Wayside.Api.Configuration;
using Wayside.Api.Services.Interfaces;

namespace Wayside.Api.Services.Google;

/// <inheritdoc />
public sealed class GoogleRoutesService(
    HttpClient httpClient,
    IOptions<GoogleMapsOptions> options,
    ILogger<GoogleRoutesService> logger) : IRouteDirectionsService
{
    private readonly GoogleMapsOptions _options = options.Value;

    public async Task<DriveRoute?> ComputeAsync(
        string origin,
        string destination,
        string? originPlaceId = null,
        string? destinationPlaceId = null,
        IReadOnlyList<string>? stops = null,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(_options.ApiKey))
        {
            return null;
        }

        var request = new ComputeRoutesRequest(
            Origin: Waypoint.For(origin, originPlaceId),
            Destination: Waypoint.For(destination, destinationPlaceId),
            // In the order the driver put them in. optimizeWaypointOrder would reshuffle the
            // plan, which is the one thing they explicitly chose.
            Intermediates: stops?.Count > 0
                ? stops.Select(stop => Waypoint.ForStop(stop)).ToList()
                : null,
            TravelMode: "DRIVE",
            // Traffic-aware routing costs more and changes the answer minute to minute; a
            // detour budget doesn't need that precision.
            RoutingPreference: "TRAFFIC_UNAWARE");

        using var message = new HttpRequestMessage(
            HttpMethod.Post,
            $"{_options.RoutesBaseUrl}/directions/v2:computeRoutes")
        {
            Content = JsonContent.Create(request),
        };
        message.Headers.Add("X-Goog-Api-Key", _options.ApiKey);
        // Routes bills by the fields you ask for, so ask for exactly three.
        message.Headers.Add(
            "X-Goog-FieldMask",
            "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline");

        using var response = await httpClient.SendAsync(message, cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            var body = await response.Content.ReadAsStringAsync(cancellationToken);
            logger.LogWarning(
                "Routes returned {Status} for {Origin} → {Destination}: {Body}",
                (int)response.StatusCode,
                origin,
                destination,
                body);
            return null;
        }

        var payload = await response.Content.ReadFromJsonAsync<ComputeRoutesResponse>(
            cancellationToken);

        var route = payload?.Routes?.FirstOrDefault();
        if (route is null)
        {
            return null;
        }

        return new DriveRoute(
            DriveMinutes: (int)Math.Round(ParseSeconds(route.Duration) / 60.0),
            DistanceKm: route.DistanceMeters / 1000.0,
            Points: PolylineDecoder.Decode(route.Polyline?.EncodedPolyline),
            EncodedPolyline: route.Polyline?.EncodedPolyline);
    }

    /// <summary>Durations arrive as protobuf strings — "3600s", not a number.</summary>
    private static double ParseSeconds(string? duration)
    {
        if (string.IsNullOrWhiteSpace(duration))
        {
            return 0;
        }

        var trimmed = duration.TrimEnd('s');
        return double.TryParse(trimmed, NumberStyles.Float, CultureInfo.InvariantCulture, out var seconds)
            ? seconds
            : 0;
    }

    private sealed record ComputeRoutesRequest(
        [property: JsonPropertyName("origin")] Waypoint Origin,
        [property: JsonPropertyName("destination")] Waypoint Destination,
        [property: JsonPropertyName("intermediates")]
        [property: JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
        IReadOnlyList<Waypoint>? Intermediates,
        [property: JsonPropertyName("travelMode")] string TravelMode,
        [property: JsonPropertyName("routingPreference")] string RoutingPreference);

    /// <summary>
    /// Routes takes exactly one of these per waypoint. A place ID resolves to the place the
    /// user actually picked; an address is Google's best guess at what the text meant.
    /// </summary>
    private sealed record Waypoint(
        [property: JsonPropertyName("address")]
        [property: JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
        string? Address = null,
        [property: JsonPropertyName("placeId")]
        [property: JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
        string? PlaceId = null,
        [property: JsonPropertyName("location")]
        [property: JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
        WaypointLocation? Location = null)
    {
        public static Waypoint For(string text, string? placeId) =>
            string.IsNullOrWhiteSpace(placeId) ? FromText(text) : new Waypoint(PlaceId: placeId);

        /// <summary>A stop arrives as either a place ID or a "lat,lng" pair.</summary>
        public static Waypoint ForStop(string stop) => FromText(stop);

        /// <summary>
        /// "41.99,21.41" is a position, not an address. Sending it as one asks Routes to
        /// geocode a string that looks like nothing a person would type, and it can fail or
        /// land somewhere else entirely.
        /// </summary>
        private static Waypoint FromText(string text)
        {
            var parts = text.Split(',');
            if (parts.Length == 2 &&
                double.TryParse(parts[0], NumberStyles.Float, CultureInfo.InvariantCulture, out var lat) &&
                double.TryParse(parts[1], NumberStyles.Float, CultureInfo.InvariantCulture, out var lng) &&
                lat is >= -90 and <= 90 && lng is >= -180 and <= 180)
            {
                return new Waypoint(Location: new WaypointLocation(new LatLngPayload(lat, lng)));
            }

            // Not coordinates and not a place ID — it's whatever the driver typed.
            return new Waypoint(Address: text);
        }
    }

    private sealed record WaypointLocation(
        [property: JsonPropertyName("latLng")] LatLngPayload LatLng);

    private sealed record LatLngPayload(
        [property: JsonPropertyName("latitude")] double Latitude,
        [property: JsonPropertyName("longitude")] double Longitude);

    private sealed record ComputeRoutesResponse(
        [property: JsonPropertyName("routes")] IReadOnlyList<RoutePayload>? Routes);

    private sealed record RoutePayload(
        [property: JsonPropertyName("duration")] string? Duration,
        [property: JsonPropertyName("distanceMeters")] int DistanceMeters,
        [property: JsonPropertyName("polyline")] PolylinePayload? Polyline);

    private sealed record PolylinePayload(
        [property: JsonPropertyName("encodedPolyline")] string? EncodedPolyline);
}
