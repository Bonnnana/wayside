using System.Security.Claims;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using Wayside.Api.Configuration;
using Wayside.Api.Data;
using Wayside.Api.Entities;
using Wayside.Api.Models.Api;
using Wayside.Api.Services.Auth;
using Wayside.Api.Services.Email;

namespace Wayside.Api.Endpoints;

public static class AuthEndpoints
{
    public static IEndpointRouteBuilder MapAuthEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/auth").WithTags("Auth");

        group.MapPost("/register", Register).WithName("Register");
        group.MapPost("/login", Login).WithName("Login");
        group.MapPost("/refresh", Refresh).WithName("Refresh");
        group.MapPost("/forgot-password", ForgotPassword).WithName("ForgotPassword");
        group.MapPost("/reset-password", ResetPassword).WithName("ResetPassword");
        group.MapPost("/logout", Logout).WithName("Logout");
        group.MapGet("/exists", UserExists).WithName("UserExists");
        group.MapPut("/profile", UpdateProfile).WithName("UpdateProfile").RequireAuthorization();

        return app;
    }

    private static async Task<IResult> Register(
        RegisterRequest request,
        UserManager<WaysideUser> users,
        IAuthTokenProcessor tokens,
        IOptions<JwtOptions> jwtOptions,
        CancellationToken cancellationToken)
    {
        var email = request.Email.Trim();

        var user = new WaysideUser
        {
            UserName = email,
            Email = email,
            FirstName = request.FirstName.Trim(),
            LastName = request.LastName.Trim(),
            // No email confirmation step: accounts are usable immediately.
            EmailConfirmed = true,
        };

        var result = await users.CreateAsync(user, request.Password);
        if (!result.Succeeded)
        {
            return Results.BadRequest(new AuthErrorResponse(
                "Could not create the account.",
                result.Errors.Select(e => e.Description).ToList()));
        }

        return Results.Ok(await IssueTokensAsync(user, users, tokens, jwtOptions.Value));
    }

    private static async Task<IResult> Login(
        LoginRequest request,
        UserManager<WaysideUser> users,
        IAuthTokenProcessor tokens,
        IOptions<JwtOptions> jwtOptions,
        CancellationToken cancellationToken)
    {
        var user = await users.FindByEmailAsync(request.Email.Trim());

        // One message for both "no such account" and "wrong password" — telling them apart
        // turns the endpoint into an account-enumeration oracle.
        if (user is null || !await users.CheckPasswordAsync(user, request.Password))
        {
            return Results.Json(
                new AuthErrorResponse("That email and password don't match."),
                statusCode: StatusCodes.Status401Unauthorized);
        }

        if (await users.IsLockedOutAsync(user))
        {
            return Results.Json(
                new AuthErrorResponse("Too many attempts. Try again later."),
                statusCode: StatusCodes.Status423Locked);
        }

        return Results.Ok(await IssueTokensAsync(user, users, tokens, jwtOptions.Value));
    }

    private static async Task<IResult> Refresh(
        RefreshRequest request,
        AppDbContext db,
        UserManager<WaysideUser> users,
        IAuthTokenProcessor tokens,
        IOptions<JwtOptions> jwtOptions,
        CancellationToken cancellationToken)
    {
        var user = await db.Users.SingleOrDefaultAsync(
            u => u.RefreshToken == request.RefreshToken, cancellationToken);

        if (user is null || user.RefreshTokenExpiresAtUtc <= DateTime.UtcNow)
        {
            return Results.Json(
                new AuthErrorResponse("That session has expired. Sign in again."),
                statusCode: StatusCodes.Status401Unauthorized);
        }

        // Rotate: the presented token is replaced, so it can't be replayed.
        return Results.Ok(await IssueTokensAsync(user, users, tokens, jwtOptions.Value));
    }

    private static async Task<IResult> ForgotPassword(
        ForgotPasswordRequest request,
        UserManager<WaysideUser> users,
        IEmailSender email,
        CancellationToken cancellationToken)
    {
        var user = await users.FindByEmailAsync(request.Email.Trim());

        // Always 200, whether or not the account exists — otherwise this endpoint reveals
        // which addresses are registered.
        if (user is not null)
        {
            var token = await users.GeneratePasswordResetTokenAsync(user);
            await email.SendPasswordResetAsync(user.Email!, token, cancellationToken);
        }

        return Results.Ok();
    }

    private static async Task<IResult> ResetPassword(
        ResetPasswordRequest request,
        UserManager<WaysideUser> users,
        CancellationToken cancellationToken)
    {
        var user = await users.FindByEmailAsync(request.Email.Trim());
        if (user is null)
        {
            return Results.BadRequest(new AuthErrorResponse("That reset code is not valid."));
        }

        var result = await users.ResetPasswordAsync(user, request.Token, request.NewPassword);
        if (!result.Succeeded)
        {
            return Results.BadRequest(new AuthErrorResponse(
                "That reset code is not valid.",
                result.Errors.Select(e => e.Description).ToList()));
        }

        // A password change ends every existing session.
        user.RefreshToken = null;
        user.RefreshTokenExpiresAtUtc = null;
        await users.UpdateAsync(user);

        return Results.Ok();
    }

    private static async Task<IResult> Logout(
        RefreshRequest request,
        AppDbContext db,
        CancellationToken cancellationToken)
    {
        var user = await db.Users.SingleOrDefaultAsync(
            u => u.RefreshToken == request.RefreshToken, cancellationToken);

        if (user is not null)
        {
            user.RefreshToken = null;
            user.RefreshTokenExpiresAtUtc = null;
            await db.SaveChangesAsync(cancellationToken);
        }

        return Results.Ok();
    }

    /// <summary>
    /// Name only — the app's only editable profile field today. Reads the caller from the JWT
    /// rather than a request-body id, so one account can never edit another's name.
    /// </summary>
    private static async Task<IResult> UpdateProfile(
        UpdateProfileRequest request,
        ClaimsPrincipal claims,
        UserManager<WaysideUser> users,
        CancellationToken cancellationToken)
    {
        var userId = claims.FindFirstValue(ClaimTypes.NameIdentifier);
        var user = userId is null ? null : await users.FindByIdAsync(userId);
        if (user is null)
        {
            return Results.Json(
                new AuthErrorResponse("That session is no longer valid. Sign in again."),
                statusCode: StatusCodes.Status401Unauthorized);
        }

        user.FirstName = request.FirstName.Trim();
        user.LastName = request.LastName.Trim();

        var result = await users.UpdateAsync(user);
        if (!result.Succeeded)
        {
            return Results.BadRequest(new AuthErrorResponse(
                "Could not update your profile.",
                result.Errors.Select(e => e.Description).ToList()));
        }

        return Results.Ok(
            new UserResponse(user.Id.ToString(), user.Email!, user.FirstName, user.LastName));
    }

    /// <summary>
    /// Lets the app split sign-in into "enter email" then "enter password", the way Ginga does.
    /// </summary>
    private static async Task<IResult> UserExists(
        string email,
        UserManager<WaysideUser> users)
    {
        var user = await users.FindByEmailAsync(email.Trim());
        return Results.Ok(new { exists = user is not null });
    }

    private static async Task<AuthTokensResponse> IssueTokensAsync(
        WaysideUser user,
        UserManager<WaysideUser> users,
        IAuthTokenProcessor tokens,
        JwtOptions options)
    {
        var (accessToken, expiresAtUtc) = tokens.GenerateAccessToken(user);

        user.RefreshToken = tokens.GenerateRefreshToken();
        user.RefreshTokenExpiresAtUtc = DateTime.UtcNow.AddDays(options.RefreshTokenDays);
        await users.UpdateAsync(user);

        return new AuthTokensResponse(
            AccessToken: accessToken,
            RefreshToken: user.RefreshToken!,
            ExpiresAtUtc: expiresAtUtc,
            User: new UserResponse(
                user.Id.ToString(), user.Email!, user.FirstName, user.LastName));
    }
}
