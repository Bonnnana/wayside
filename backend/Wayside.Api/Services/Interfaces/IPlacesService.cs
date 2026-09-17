using Wayside.Api.Models.Api;

namespace Wayside.Api.Services.Interfaces;

/// <summary>
/// Finds and reads places. The only component that talks to Google Places — nothing else in
/// the app should hold the Maps key or know the upstream response shape.
/// </summary>
public interface IPlacesService
{
    /// <summary>
    /// Places worth stopping at along a drive, filtered to those whose detour fits the budget.
    /// </summary>
    /// <returns>
    /// Null when the drive itself couldn't be worked out — an unrecognised place, or a pair
    /// with no road between them. That is a different answer from "no places fit the budget",
    /// and the caller has to be able to tell them apart.
    /// </returns>
    Task<SuggestionsResponse?> GetSuggestionsAsync(
        string origin,
        string destination,
        int budgetMinutes,
        string? originPlaceId = null,
        string? destinationPlaceId = null,
        CancellationToken cancellationToken = default);

    /// <summary>
    /// Place predictions for a partly typed query, for the search dropdown.
    /// </summary>
    /// <param name="latitude">Where the user is, when known — biases results towards them.</param>
    Task<IReadOnlyList<PlaceSuggestion>> AutocompleteAsync(
        string query,
        double? latitude = null,
        double? longitude = null,
        CancellationToken cancellationToken = default);

    /// <summary>
    /// A loadable URL for one of a place's photos, or null when there isn't one.
    /// </summary>
    /// <remarks>
    /// The app can't build these itself: the photo endpoint needs the Maps key, and that key
    /// must not leave the server.
    /// </remarks>
    Task<string?> GetPhotoUrlAsync(
        string placeId,
        int index,
        int maxWidthPx,
        CancellationToken cancellationToken = default);

    /// <summary>Full detail for one place, including its stored summary.</summary>
    Task<PlaceDetail?> GetPlaceAsync(string placeId, CancellationToken cancellationToken = default);
}
