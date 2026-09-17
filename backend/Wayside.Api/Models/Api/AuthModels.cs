using System.ComponentModel.DataAnnotations;

namespace Wayside.Api.Models.Api;

public sealed record RegisterRequest(
    [Required, EmailAddress] string Email,
    [Required, MinLength(8)] string Password,
    [Required, MaxLength(100)] string FirstName,
    [Required, MaxLength(100)] string LastName);

public sealed record LoginRequest(
    [Required, EmailAddress] string Email,
    [Required] string Password);

public sealed record RefreshRequest([Required] string RefreshToken);

public sealed record ForgotPasswordRequest([Required, EmailAddress] string Email);

public sealed record ResetPasswordRequest(
    [Required, EmailAddress] string Email,
    [Required] string Token,
    [Required, MinLength(8)] string NewPassword);

public sealed record UpdateProfileRequest(
    [Required, MaxLength(100)] string FirstName,
    [Required, MaxLength(100)] string LastName);

public sealed record UserResponse(
    string Id,
    string Email,
    string FirstName,
    string LastName);

/// <summary>
/// What the app stores after login. <c>ExpiresAtUtc</c> lets the client refresh proactively
/// instead of waiting for a 401.
/// </summary>
public sealed record AuthTokensResponse(
    string AccessToken,
    string RefreshToken,
    DateTime ExpiresAtUtc,
    UserResponse User);

/// <summary>Shape returned for every failed auth call, so the client parses one thing.</summary>
public sealed record AuthErrorResponse(string Message, IReadOnlyList<string>? Errors = null);
