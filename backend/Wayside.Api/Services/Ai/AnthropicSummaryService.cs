using Anthropic;
using Anthropic.Models.Messages;
using Microsoft.Extensions.Options;
using Wayside.Api.Configuration;
using Wayside.Api.Models.Api;
using Wayside.Api.Services.Interfaces;

namespace Wayside.Api.Services.Ai;

/// <inheritdoc />
public sealed class AnthropicSummaryService : ISummaryService
{
    private const string SystemPrompt =
        """
        You summarise reviews of places a driver might stop at on a road trip.

        Write two or three sentences describing what reviewers consistently say. Report only
        what the reviews support — if they disagree, say so; if something is mentioned once,
        leave it out. Prefer the concrete detail a traveller can act on (when the pastries come
        out of the oven, that the last stretch of road is single-lane) over adjectives.

        Do not open with the place name, do not recommend, and do not use marketing language.
        Return the summary text alone, with no preamble or heading.
        """;

    private readonly AnthropicClient _client;
    private readonly AnthropicOptions _options;
    private readonly ILogger<AnthropicSummaryService> _logger;

    public AnthropicSummaryService(
        AnthropicClient client,
        IOptions<AnthropicOptions> options,
        ILogger<AnthropicSummaryService> logger)
    {
        _client = client;
        _options = options.Value;
        _logger = logger;
    }

    public async Task<PlaceInsights> SummariseReviewsAsync(
        string placeName,
        IReadOnlyList<string> reviews,
        CancellationToken cancellationToken = default)
    {
        if (reviews.Count == 0)
        {
            return PlaceInsights.None;
        }

        var userContent =
            $"""
            Place: {placeName}

            Reviews:
            {string.Join("\n\n", reviews.Select(r => $"- {r}"))}
            """;

        var response = await _client.Messages.Create(new MessageCreateParams
        {
            Model = _options.Model,
            MaxTokens = _options.MaxTokens,
            System = SystemPrompt,
            // Summarising supplied text is not a reasoning-heavy task; low effort keeps
            // latency and cost down without hurting the result.
            OutputConfig = new OutputConfig { Effort = Effort.Low },
            Messages = [new() { Role = Role.User, Content = userContent }],
        });

        // Safety classifiers can decline a request: HTTP 200 with stop_reason "refusal" and
        // an empty or partial Content. Check before reading blocks.
        if (response.StopReason == "refusal")
        {
            _logger.LogWarning(
                "Summary refused for {PlaceName}: {Category}",
                placeName,
                response.StopDetails?.Category);
            return PlaceInsights.None;
        }

        var text = response.Content
            .Select(block => block.Value)
            .OfType<TextBlock>()
            .Select(block => block.Text);

        // Themes are not asked for here: this implementation is the fallback, kept working
        // so the DI line can be swapped back, and the detail screen tolerates an empty list.
        return new PlaceInsights(string.Join("\n", text).Trim(), []);
    }
}
