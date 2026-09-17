using Microsoft.AspNetCore.Identity;

namespace Wayside.Api.Entities;

/// <summary>
/// An account. Extends Identity's user so password hashing, lockout, and the reset-token
/// providers come from the framework rather than being hand-rolled.
/// </summary>
public sealed class WaysideUser : IdentityUser<Guid>
{
    public string FirstName { get; set; } = string.Empty;

    public string LastName { get; set; } = string.Empty;

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;

    /// <summary>Opaque refresh token, replaced on every refresh so a stolen one is single-use.</summary>
    public string? RefreshToken { get; set; }

    public DateTime? RefreshTokenExpiresAtUtc { get; set; }

    public string FullName => $"{FirstName} {LastName}".Trim();
}
