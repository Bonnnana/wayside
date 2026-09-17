namespace Wayside.Api.Services.Google;

/// <summary>
/// Decodes Google's encoded polyline format — the shape of a route, delivered as an ASCII
/// string of deltas rather than a list of coordinates.
/// </summary>
public static class PolylineDecoder
{
    public static IReadOnlyList<GeoPoint> Decode(string? encoded)
    {
        if (string.IsNullOrEmpty(encoded))
        {
            return [];
        }

        var points = new List<GeoPoint>();
        var index = 0;
        var lat = 0;
        var lng = 0;

        while (index < encoded.Length)
        {
            lat += DecodeValue(encoded, ref index);
            lng += DecodeValue(encoded, ref index);
            points.Add(new GeoPoint(lat / 1e5, lng / 1e5));
        }

        return points;
    }

    /// <summary>
    /// One signed value: five-bit chunks, low bits first, each chunk after the first flagged
    /// by bit 6, and the whole thing zig-zag encoded so negatives survive.
    /// </summary>
    private static int DecodeValue(string encoded, ref int index)
    {
        var shift = 0;
        var result = 0;
        int chunk;

        do
        {
            chunk = encoded[index++] - 63;
            result |= (chunk & 0x1f) << shift;
            shift += 5;
        }
        while (chunk >= 0x20 && index < encoded.Length);

        return (result & 1) != 0 ? ~(result >> 1) : result >> 1;
    }
}
