using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using Wayside.Api.Configuration;
using Wayside.Api.Entities;

namespace Wayside.Api.Services.Auth;

public interface IAuthTokenProcessor
{
    (string Token, DateTime ExpiresAtUtc) GenerateAccessToken(WaysideUser user);

    string GenerateRefreshToken();
}

public sealed class AuthTokenProcessor(IOptions<JwtOptions> options) : IAuthTokenProcessor
{
    private readonly JwtOptions _options = options.Value;

    public (string Token, DateTime ExpiresAtUtc) GenerateAccessToken(WaysideUser user)
    {
        var signingKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_options.EffectiveSecret));
        var credentials = new SigningCredentials(signingKey, SecurityAlgorithms.HmacSha256);

        Claim[] claims =
        [
            new(JwtRegisteredClaimNames.Sub, user.Id.ToString()),
            // Unique per token, so an individual token can be revoked later without
            // invalidating every token the user holds.
            new(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString()),
            new(JwtRegisteredClaimNames.Email, user.Email!),
            new(ClaimTypes.NameIdentifier, user.Id.ToString()),
        ];

        var expires = DateTime.UtcNow.AddMinutes(_options.AccessTokenMinutes);

        var token = new JwtSecurityToken(
            issuer: _options.Issuer,
            audience: _options.Audience,
            claims: claims,
            expires: expires,
            signingCredentials: credentials);

        return (new JwtSecurityTokenHandler().WriteToken(token), expires);
    }

    public string GenerateRefreshToken()
    {
        // Opaque and random — unlike the access token it carries no claims, so it is only
        // useful against the row that stores it.
        var bytes = RandomNumberGenerator.GetBytes(64);
        return Convert.ToBase64String(bytes);
    }
}
