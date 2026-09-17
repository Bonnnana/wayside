using System.Text;
using System.Text.Json.Serialization;
using Anthropic;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using Wayside.Api.Configuration;
using Wayside.Api.Data;
using Wayside.Api.Endpoints;
using Wayside.Api.Entities;
using Wayside.Api.Services;
using Wayside.Api.Services.Ai;
using Wayside.Api.Services.Auth;
using Wayside.Api.Services.Email;
using Wayside.Api.Services.Google;
using Wayside.Api.Services.Interfaces;
using Wayside.Api.Services.Places;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddOpenApi();

// Serialise enums as names, not ordinals. Reordering PlaceCategory would otherwise silently
// re-map every category on the client.
builder.Services.ConfigureHttpJsonOptions(options =>
    options.SerializerOptions.Converters.Add(new JsonStringEnumConverter()));

// ---- Configuration ----------------------------------------------------------------
// Keys come from user-secrets in development and the host's secret store in production.
// Never from appsettings.json — that file is committed.
builder.Services.Configure<AnthropicOptions>(
    builder.Configuration.GetSection(AnthropicOptions.SectionName));
builder.Services.Configure<GoogleMapsOptions>(
    builder.Configuration.GetSection(GoogleMapsOptions.SectionName));
builder.Services.Configure<GeminiOptions>(
    builder.Configuration.GetSection(GeminiOptions.SectionName));
builder.Services.Configure<JwtOptions>(
    builder.Configuration.GetSection(JwtOptions.SectionName));
builder.Services.Configure<EmailOptions>(
    builder.Configuration.GetSection(EmailOptions.SectionName));

// ---- Persistence ------------------------------------------------------------------
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseSqlite(builder.Configuration.GetConnectionString("Default")
                      ?? "Data Source=wayside.db"));

// ---- Identity ---------------------------------------------------------------------
builder.Services
    .AddIdentity<WaysideUser, IdentityRole<Guid>>(options =>
    {
        options.Password.RequiredLength = 8;
        options.Password.RequireDigit = true;
        options.Password.RequireLowercase = true;
        options.Password.RequireUppercase = true;
        options.Password.RequireNonAlphanumeric = true;
        options.User.RequireUniqueEmail = true;
        // No confirmation step — accounts work as soon as they're created.
        options.SignIn.RequireConfirmedEmail = false;
        options.Lockout.MaxFailedAccessAttempts = 10;
        options.Lockout.DefaultLockoutTimeSpan = TimeSpan.FromMinutes(15);
        // Password reset is a code someone reads out of an email and types on a phone. The
        // default provider emits a ~200-char Data Protection blob; the phone provider emits
        // six digits. ResetPasswordAsync validates against whichever is configured here.
        options.Tokens.PasswordResetTokenProvider = TokenOptions.DefaultPhoneProvider;
    })
    .AddEntityFrameworkStores<AppDbContext>()
    // Supplies the password-reset token provider used by /auth/forgot-password.
    .AddDefaultTokenProviders();

// ---- Authentication ---------------------------------------------------------------
var jwt = builder.Configuration.GetSection(JwtOptions.SectionName).Get<JwtOptions>()
          ?? new JwtOptions();

builder.Services
    .AddAuthentication(options =>
    {
        options.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
        options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
    })
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidateAudience = true,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            ValidIssuer = jwt.Issuer,
            ValidAudience = jwt.Audience,
            IssuerSigningKey = new SymmetricSecurityKey(
                Encoding.UTF8.GetBytes(jwt.EffectiveSecret)),
            // Tokens expire when they say they expire, not five minutes later.
            ClockSkew = TimeSpan.Zero,
        };
    });

builder.Services.AddAuthorization();

// ---- Services ---------------------------------------------------------------------
builder.Services.AddSingleton(sp =>
{
    var options = sp.GetRequiredService<IOptions<AnthropicOptions>>().Value;
    return string.IsNullOrWhiteSpace(options.ApiKey)
        // Falls back to ANTHROPIC_API_KEY, or an `ant auth login` profile.
        ? new AnthropicClient()
        : new AnthropicClient { ApiKey = options.ApiKey };
});

builder.Services.AddScoped<IAuthTokenProcessor, AuthTokenProcessor>();
builder.Services.AddScoped<IEmailSender, SmtpEmailSender>();
// Summaries run on Gemini: a Google AI Studio key has a free tier, and the work — condensing
// supplied review text — needs no more than the cheapest model. AnthropicSummaryService is
// still in the tree and implements the same interface, so swapping back is this one line.
builder.Services.AddHttpClient<ISummaryService, GeminiSummaryService>();

builder.Services.AddScoped<IPlaceStore, PlaceStore>();
builder.Services.AddHttpClient<IRouteDirectionsService, GoogleRoutesService>();

// Which place source depends on whether there is a key to use. Without one the API still
// answers every request from the sample set, so the app runs on a fresh clone; with one it
// serves real places along the real drive. Nothing above this line changes either way.
var googleMaps = builder.Configuration
    .GetSection(GoogleMapsOptions.SectionName)
    .Get<GoogleMapsOptions>() ?? new GoogleMapsOptions();

if (string.IsNullOrWhiteSpace(googleMaps.ApiKey))
{
    builder.Services.AddScoped<IPlacesService, SamplePlacesService>();
}
else
{
    builder.Services.AddHttpClient<IPlacesService, GooglePlacesService>();
}

var app = builder.Build();

// Apply migrations on boot. This replaces EnsureCreated, which can only create a database
// from nothing — it silently does nothing when the file already exists, so a schema change
// lands as "no such table" at runtime rather than as a failed startup.
using (var scope = app.Services.CreateScope())
{
    await scope.ServiceProvider.GetRequiredService<AppDbContext>().Database.MigrateAsync();
}

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}

app.UseHttpsRedirection();
app.UseAuthentication();
app.UseAuthorization();

app.MapGet("/health", () => Results.Ok(new { status = "ok" }))
    .WithTags("Health")
    .WithSummary("Liveness probe.");

// Everything the app calls sits under /api, so the client can send one base path and the
// infrastructure library's URL builder never has to deal with an empty path segment.
var api = app.MapGroup("/api");
api.MapAuthEndpoints();
api.MapPlaceEndpoints();

app.Run();
