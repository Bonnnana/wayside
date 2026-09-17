namespace Wayside.Api.Configuration;

public sealed class JwtOptions
{
    public const string SectionName = "Jwt";

    /// <summary>Signing secret. At least 32 bytes — HMAC-SHA256 rejects anything shorter.</summary>
    public string Secret { get; init; } = string.Empty;

    /// <summary>
    /// The key actually used to sign and validate. Falls back to a fixed development value so
    /// the app boots before a secret is configured — both the signer and the validator read
    /// this, so they can never disagree.
    /// </summary>
    /// <remarks>
    /// The fallback is public knowledge and worthless in production, where a real secret is
    /// configured. Set one with:
    /// <c>dotnet user-secrets set "Jwt:Secret" "&lt;32+ random bytes&gt;"</c>
    /// </remarks>
    public string EffectiveSecret => string.IsNullOrWhiteSpace(Secret)
        ? "wayside-development-only-signing-key-change-me"
        : Secret;

    public string Issuer { get; init; } = "wayside-api";

    public string Audience { get; init; } = "wayside-app";

    /// <summary>Short by design — the refresh token is what keeps a session alive.</summary>
    public int AccessTokenMinutes { get; init; } = 30;

    public int RefreshTokenDays { get; init; } = 30;
}

public sealed class EmailOptions
{
    public const string SectionName = "Email";

    public string Host { get; init; } = string.Empty;

    public int Port { get; init; } = 587;

    public string Username { get; init; } = string.Empty;

    public string Password { get; init; } = string.Empty;

    public string FromAddress { get; init; } = "no-reply@wayside.app";

    public string FromName { get; init; } = "Wayside";

    /// <summary>
    /// When false, reset emails are written to the log instead of sent. Lets the whole flow be
    /// exercised before SMTP credentials exist.
    /// </summary>
    public bool Enabled { get; init; }
}
