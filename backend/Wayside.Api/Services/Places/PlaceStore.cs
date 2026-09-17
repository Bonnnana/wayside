using Microsoft.EntityFrameworkCore;
using Wayside.Api.Data;
using Wayside.Api.Entities;
using Wayside.Api.Services.Interfaces;

namespace Wayside.Api.Services.Places;

/// <inheritdoc />
public sealed class PlaceStore(
    AppDbContext db,
    ISummaryService summaries,
    ILogger<PlaceStore> logger) : IPlaceStore
{
    public Task<StoredPlace?> FindAsync(
        string placeId,
        CancellationToken cancellationToken = default) =>
        db.Places.SingleOrDefaultAsync(place => place.Id == placeId, cancellationToken);

    public async Task<IReadOnlyList<StoredPlace>> FindManyAsync(
        IReadOnlyCollection<string> placeIds,
        CancellationToken cancellationToken = default)
    {
        if (placeIds.Count == 0)
        {
            return [];
        }

        return await db.Places
            .Where(place => placeIds.Contains(place.Id))
            .ToListAsync(cancellationToken);
    }

    public async Task<StoredPlace> UpsertAsync(
        StoredPlace place,
        IReadOnlyList<string> reviews,
        CancellationToken cancellationToken = default)
    {
        var existing = await FindAsync(place.Id, cancellationToken);

        place.RefreshedUtc = DateTime.UtcNow;

        if (existing is null)
        {
            Apply(place, await GenerateInsightsAsync(place, reviews, cancellationToken));

            db.Places.Add(place);
            await db.SaveChangesAsync(cancellationToken);
            return place;
        }

        existing.Name = place.Name;
        existing.Category = place.Category;
        existing.Rating = place.Rating;
        existing.RatingCount = place.RatingCount;
        existing.Latitude = place.Latitude;
        existing.Longitude = place.Longitude;
        existing.OpenUntil = place.OpenUntil;
        existing.Highlight = place.Highlight;
        existing.TagsJson = place.TagsJson;
        existing.PhotosJson = place.PhotosJson;
        // Themes come from the model, so a refresh must not wipe them.
        if (place.ReviewThemes.Count > 0)
        {
            existing.ReviewThemesJson = place.ReviewThemesJson;
        }
        existing.RefreshedUtc = place.RefreshedUtc;

        // The summary is kept unless there isn't one — a place whose first ingest ran without
        // an AI key, or was refused, gets another chance here rather than staying blank forever.
        if (string.IsNullOrWhiteSpace(existing.AiSummary))
        {
            Apply(existing, await GenerateInsightsAsync(existing, reviews, cancellationToken));
        }

        await db.SaveChangesAsync(cancellationToken);
        return existing;
    }

    private static void Apply(StoredPlace place, PlaceInsights insights)
    {
        if (string.IsNullOrWhiteSpace(insights.Summary))
        {
            return;
        }

        place.AiSummary = insights.Summary;
        place.ReviewThemes = insights.Themes;
        place.SummaryGeneratedUtc = DateTime.UtcNow;
    }

    private async Task<PlaceInsights> GenerateInsightsAsync(
        StoredPlace place,
        IReadOnlyList<string> reviews,
        CancellationToken cancellationToken)
    {
        if (reviews.Count == 0)
        {
            // Worth saying out loud: an empty summary with no other log line looks like the
            // model failed, when in fact there was nothing to summarise.
            logger.LogInformation("No reviews returned for {Place}; nothing to summarise.", place.Name);
            return PlaceInsights.None;
        }

        try
        {
            return await summaries.SummariseReviewsAsync(place.Name, reviews, cancellationToken);
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            // A summary is a nicety; failing to write the place because of it would not be.
            logger.LogWarning(ex, "Could not summarise reviews for {Place}.", place.Name);
            return PlaceInsights.None;
        }
    }
}
