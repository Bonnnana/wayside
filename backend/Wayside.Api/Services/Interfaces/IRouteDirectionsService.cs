using Wayside.Api.Services.Google;

namespace Wayside.Api.Services.Interfaces;

/// <param name="DriveMinutes">Driving time with no stops.</param>
/// <param name="Points">The route's shape, dense enough to measure distance off it.</param>
public sealed record DriveRoute(
    int DriveMinutes,
    double DistanceKm,
    IReadOnlyList<GeoPoint> Points,
    /// <summary>The same shape, still encoded, to hand to the client untouched.</summary>
    string? EncodedPolyline);

/// <summary>The drive itself: how long it takes, how far it is, and where it goes.</summary>
public interface IRouteDirectionsService
{
    /// <remarks>
    /// A place ID is preferred over text when one is known: text is re-interpreted on every
    /// call, so "Sintra" can resolve to a different Sintra than the one the user picked.
    /// </remarks>
    /// <param name="stops">
    /// Places to pass through, in the order given. Routes keeps that order — it is the
    /// driver's plan, not something to optimise away.
    /// </param>
    Task<DriveRoute?> ComputeAsync(
        string origin,
        string destination,
        string? originPlaceId = null,
        string? destinationPlaceId = null,
        IReadOnlyList<string>? stops = null,
        CancellationToken cancellationToken = default);
}
