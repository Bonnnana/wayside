namespace Wayside.Api.Configuration;

/// <summary>
/// Server-side Google Maps Platform key, used for Places and Routes.
/// </summary>
/// <remarks>
/// This is a different key from the one in the Android app. The app's key is restricted to
/// the package name plus signing certificate and is only good for rendering the map; this one
/// is unrestricted by package and must never reach a client. Restrict it by IP to the API host.
/// </remarks>
public sealed class GoogleMapsOptions
{
    public const string SectionName = "GoogleMaps";

    public string ApiKey { get; init; } = string.Empty;

    public string PlacesBaseUrl { get; init; } = "https://places.googleapis.com/v1";

    public string RoutesBaseUrl { get; init; } = "https://routes.googleapis.com";
}
