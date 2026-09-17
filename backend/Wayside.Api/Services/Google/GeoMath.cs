namespace Wayside.Api.Services.Google;

/// <summary>A point on the earth, in degrees.</summary>
public readonly record struct GeoPoint(double Latitude, double Longitude);

/// <summary>
/// The small amount of geometry this service needs. Not a general-purpose library — every
/// method here exists because one of the route calculations below calls it.
/// </summary>
public static class GeoMath
{
    private const double EarthRadiusKm = 6371.0088;

    /// <summary>Great-circle distance in kilometres.</summary>
    public static double DistanceKm(GeoPoint a, GeoPoint b)
    {
        var dLat = ToRadians(b.Latitude - a.Latitude);
        var dLon = ToRadians(b.Longitude - a.Longitude);
        var lat1 = ToRadians(a.Latitude);
        var lat2 = ToRadians(b.Latitude);

        var h = Math.Sin(dLat / 2) * Math.Sin(dLat / 2) +
                Math.Sin(dLon / 2) * Math.Sin(dLon / 2) * Math.Cos(lat1) * Math.Cos(lat2);

        return 2 * EarthRadiusKm * Math.Asin(Math.Min(1, Math.Sqrt(h)));
    }

    /// <summary>
    /// How far a place sits off the route, and how far along the route it is (0..1).
    /// </summary>
    /// <remarks>
    /// Measured to the nearest vertex rather than to the nearest point on each segment. The
    /// polyline Google returns is dense enough that the difference is metres, and the exact
    /// version costs a projection per segment per candidate place.
    /// </remarks>
    public static (double OffRouteKm, double Progress) NearestOnRoute(
        IReadOnlyList<GeoPoint> route,
        GeoPoint place)
    {
        if (route.Count == 0)
        {
            return (0, 0);
        }

        var cumulative = CumulativeDistances(route);
        var total = cumulative[^1];

        var bestIndex = 0;
        var bestDistance = double.MaxValue;

        for (var i = 0; i < route.Count; i++)
        {
            var distance = DistanceKm(route[i], place);
            if (distance < bestDistance)
            {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        var progress = total <= 0 ? 0 : cumulative[bestIndex] / total;
        return (bestDistance, Math.Clamp(progress, 0, 1));
    }

    /// <summary>
    /// Points spaced roughly <paramref name="spacingKm"/> apart along the route, always
    /// including the first and last. These are where the place searches are centred.
    /// </summary>
    public static IReadOnlyList<GeoPoint> SampleAlong(
        IReadOnlyList<GeoPoint> route,
        double spacingKm)
    {
        if (route.Count == 0)
        {
            return [];
        }

        var cumulative = CumulativeDistances(route);
        var samples = new List<GeoPoint> { route[0] };
        var nextAt = spacingKm;

        for (var i = 1; i < route.Count; i++)
        {
            if (cumulative[i] >= nextAt)
            {
                samples.Add(route[i]);
                nextAt = cumulative[i] + spacingKm;
            }
        }

        if (samples.Count == 1 || !samples[^1].Equals(route[^1]))
        {
            samples.Add(route[^1]);
        }

        return samples;
    }

    private static double[] CumulativeDistances(IReadOnlyList<GeoPoint> route)
    {
        var cumulative = new double[route.Count];
        for (var i = 1; i < route.Count; i++)
        {
            cumulative[i] = cumulative[i - 1] + DistanceKm(route[i - 1], route[i]);
        }

        return cumulative;
    }

    private static double ToRadians(double degrees) => degrees * Math.PI / 180;
}
