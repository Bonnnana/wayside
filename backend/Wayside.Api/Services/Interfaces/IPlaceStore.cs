using Wayside.Api.Entities;

namespace Wayside.Api.Services.Interfaces;

/// <summary>
/// Where places live between requests, and the only place a summary is generated.
/// </summary>
public interface IPlaceStore
{
    Task<StoredPlace?> FindAsync(string placeId, CancellationToken cancellationToken = default);

    Task<IReadOnlyList<StoredPlace>> FindManyAsync(
        IReadOnlyCollection<string> placeIds,
        CancellationToken cancellationToken = default);

    /// <summary>
    /// Writes a place and, the first time it is seen, generates its summary from
    /// <paramref name="reviews"/>. Later calls refresh the place data but keep the summary —
    /// regenerating it would bill again for text that hasn't changed.
    /// </summary>
    Task<StoredPlace> UpsertAsync(
        StoredPlace place,
        IReadOnlyList<string> reviews,
        CancellationToken cancellationToken = default);
}
