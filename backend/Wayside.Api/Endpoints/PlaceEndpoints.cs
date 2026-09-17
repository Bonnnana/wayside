using Wayside.Api.Models.Api;
using Wayside.Api.Services.Interfaces;

namespace Wayside.Api.Endpoints;

/// <summary>
/// The read surface the Android app talks to. Every route here returns models from
/// <c>Models.Api</c> — upstream Google shapes never leave the service layer.
/// </summary>
public static class PlaceEndpoints
{
    /// <summary>Wide enough for a phone's detail header without paying for more.</summary>
    private const int DefaultPhotoWidth = 1000;

    public static IEndpointRouteBuilder MapPlaceEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/routes").WithTags("Routes");

        group.MapGet("/suggestions", async (
            string origin,
            string destination,
            int budget,
            string? originPlaceId,
            string? destinationPlaceId,
            IPlacesService places,
            CancellationToken cancellationToken) =>
        {
            if (string.IsNullOrWhiteSpace(origin) || string.IsNullOrWhiteSpace(destination))
            {
                return Results.BadRequest("origin and destination are required.");
            }

            if (budget is < 0 or > 240)
            {
                return Results.BadRequest("budget must be between 0 and 240 minutes.");
            }

            var suggestions = await places.GetSuggestionsAsync(
                origin, destination, budget, originPlaceId, destinationPlaceId, cancellationToken);

            // 404, not an empty list: the client shows "nothing fits your budget" for an empty
            // list, and that would be a lie when the place simply wasn't found.
            return suggestions is null
                ? Results.NotFound(new AuthErrorResponse(
                    "We couldn't find a drive between those two. Pick them from the suggestions."))
                : Results.Ok(suggestions);
        })
        .WithName("GetSuggestions")
        .WithSummary("Places worth stopping at along a drive, within a detour budget.");

        group.MapGet("/directions", async (
            string origin,
            string destination,
            string? originPlaceId,
            string? destinationPlaceId,
            string? stops,
            IRouteDirectionsService routes,
            CancellationToken cancellationToken) =>
        {
            if (string.IsNullOrWhiteSpace(origin) || string.IsNullOrWhiteSpace(destination))
            {
                return Results.BadRequest("origin and destination are required.");
            }

            // One pipe-separated parameter rather than repeated ones: the mobile client's
            // URL builder takes a single value per key. Pipe, not comma, because a stop may
            // be a "lat,lng" pair.
            var waypoints = stops?
                .Split('|', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);

            var route = await routes.ComputeAsync(
                origin, destination, originPlaceId, destinationPlaceId, waypoints, cancellationToken);

            if (route is null || route.Points.Count == 0)
            {
                return Results.NotFound();
            }

            return Results.Ok(new DirectionsResponse(
                DriveMinutes: route.DriveMinutes,
                DistanceKm: Math.Round(route.DistanceKm, 1),
                Polyline: route.EncodedPolyline,
                OriginPoint: new GeoPointDto(route.Points[0].Latitude, route.Points[0].Longitude),
                DestinationPoint: new GeoPointDto(route.Points[^1].Latitude, route.Points[^1].Longitude)));
        })
        .WithName("GetDirections")
        .WithSummary("The drive between two points, with no suggestions attached.");

        app.MapGet("/places/autocomplete", async (
            string query,
            double? lat,
            double? lng,
            IPlacesService places,
            CancellationToken cancellationToken) =>
        {
            if (string.IsNullOrWhiteSpace(query))
            {
                return Results.Ok(Array.Empty<PlaceSuggestion>());
            }

            return Results.Ok(await places.AutocompleteAsync(query, lat, lng, cancellationToken));
        })
        .WithTags("Places")
        .WithName("AutocompletePlaces")
        .WithSummary("Place predictions for a partly typed search.");

        app.MapGet("/places/{placeId}/photos/{index:int}", async (
            string placeId,
            int index,
            int? maxWidth,
            IPlacesService places,
            CancellationToken cancellationToken) =>
        {
            var url = await places.GetPhotoUrlAsync(
                placeId, index, maxWidth ?? DefaultPhotoWidth, cancellationToken);

            // A redirect rather than a proxy: the signed URL Google hands back carries no key
            // and expires, so the image can go straight from their CDN to the device.
            return url is null ? Results.NotFound() : Results.Redirect(url);
        })
        .WithTags("Places")
        .WithName("GetPlacePhoto")
        .WithSummary("Redirects to one of a place's photos.");

        app.MapGet("/places/{placeId}", async (
            string placeId,
            IPlacesService places,
            CancellationToken cancellationToken) =>
        {
            var place = await places.GetPlaceAsync(placeId, cancellationToken);
            return place is null ? Results.NotFound() : Results.Ok(place);
        })
        .WithTags("Places")
        .WithName("GetPlace")
        .WithSummary("Full detail for one place, including its stored summary.");

        return app;
    }
}
