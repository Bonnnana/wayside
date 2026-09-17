using Wayside.Api.Models.Api;

namespace Wayside.Api.Services.Interfaces;

/// <summary>What a model derives from a place's reviews.</summary>
/// <param name="Summary">Two or three sentences on what reviewers consistently say.</param>
/// <param name="Themes">
/// Recurring points, with how many of the supplied reviews mention each. Counts are over the
/// reviews given, not over every review the place has.
/// </param>
public sealed record PlaceInsights(string Summary, IReadOnlyList<ReviewTheme> Themes)
{
    public static readonly PlaceInsights None = new(string.Empty, []);
}

/// <summary>
/// Turns a place's reviews into the summary and themes shown on the detail screen.
/// </summary>
/// <remarks>
/// Call this when a place first enters the database, not when a user opens it. Generating per
/// view is slow for the user and bills once per view for text that doesn't change.
/// </remarks>
public interface ISummaryService
{
    /// <param name="placeName">Used only to ground the summary; never the source of facts.</param>
    /// <param name="reviews">Review text to summarise. Summarise only what these say.</param>
    Task<PlaceInsights> SummariseReviewsAsync(
        string placeName,
        IReadOnlyList<string> reviews,
        CancellationToken cancellationToken = default);
}
