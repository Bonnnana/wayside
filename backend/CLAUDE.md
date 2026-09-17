# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The Wayside API. It serves the Android app in `../mobile` and exists to do three things the
client can't: hold the keys, cut the cost of upstream calls by caching and sharing results
across users, and own the logic that decides which places are worth a detour.

ASP.NET Core minimal APIs on .NET 10, one project: `Wayside.Api`.

## Commands

```bash
dotnet build
dotnet run --project Wayside.Api      # http://localhost:5263
dotnet test
dotnet format

# Schema changes
dotnet ef migrations add <Name> --project Wayside.Api
```

Migrations are applied on startup by `Database.MigrateAsync()`. Never go back to
`EnsureCreated`: it creates a database from nothing and does nothing at all when the file
already exists, so a new table shows up as "no such table" at runtime instead of as a failed
startup. A change to an entity means a migration in the same commit.

OpenAPI is served at `/openapi/v1.json` in development.

## Layout

```
Wayside.Api/
  Program.cs           DI wiring, JSON options, endpoint registration
  Configuration/       strongly-typed options bound from configuration
  Endpoints/           minimal-API route groups, one static class per area
  Services/
    Interfaces/        one interface per capability
    Ai/                summary generation (Gemini, Anthropic)
    Google/            Places, Routes, and the geometry they need
    Places/            PlaceStore — persistence and the ingest-time summary
  Models/Api/          the contract with the mobile client
```

## Layer rules

**Endpoints** parse and validate input, call one service, and return a model from `Models/Api`.
No upstream types, no business logic, no HTTP clients.

**Services** own one capability each, behind an interface in `Services/Interfaces` and registered
in `Program.cs`. A service is the only thing that knows an upstream API exists — its shapes stay
inside it and are mapped to `Models/Api` before returning.

**Models/Api** is the contract with the app. A change here is a change to `../mobile` — update
both together. Google and Anthropic response types never appear in this namespace.

## Secrets

Nothing that authenticates goes in `appsettings.json` — it's committed. Local development uses
user-secrets, which are stored outside the repository:

```bash
cd Wayside.Api
dotnet user-secrets set "Gemini:ApiKey" "AIza..."
dotnet user-secrets set "GoogleMaps:ApiKey" "AIza..."
```

Two keys, two different threat models:

- **`GoogleMaps:ApiKey`** is a *separate key* from the one in the Android app. The app's key is
  restricted to the package name plus signing certificate and only renders the map. This one
  carries no such restriction, so it must never reach a client — restrict it by IP to the API
  host and keep Places and Routes calls server-side.
- **`Gemini:ApiKey`** is an AI Studio key and has no package or IP restriction available at
  all. Anyone holding it can spend against the account, which is the entire reason this
  service exists rather than the app calling the model itself.

Both options classes fall back to empty rather than throwing, so the app starts without either
key: no Maps key means sample places, no Gemini key means no new summaries.

## Generating summaries

`ISummaryService` returns a `PlaceInsights` — the summary plus up to three review themes — and
has two implementations. `GeminiSummaryService` is the registered one; `AnthropicSummaryService`
still compiles against the same interface, so switching back is one line in `Program.cs`.

**Gemini** was chosen because a Google AI Studio key has a free tier, and condensing supplied
review text needs nothing more than the cheapest model. Points that matter:

- **Model is `gemini-3.5-flash-lite`**, configured in `GeminiOptions` rather than hardcoded.
- **The REST endpoint is called directly**, no SDK — the call is one POST, and a dependency
  that must be kept in step with a fast-moving API earns its place only if it does more.
- **`responseMimeType` is `application/json`.** Asking for JSON in the prompt is a request;
  this makes it a guarantee. Parsing is still wrapped in try/catch.
- **Check `finishReason` before reading parts.** A blocked or truncated answer is HTTP 200
  with a reason that is not `STOP`, often with no parts at all.
- **The key is optional.** `GeminiOptions.IsConfigured` is false without one and the service
  returns `PlaceInsights.None`, so the API runs and serves stored text.

**Generate on ingest, not on read.** `PlaceStore.UpsertAsync` generates a summary the first
time a place is stored and keeps it afterwards; a refresh updates the place data but not the
summary. Generating per page-view is slow for the user and bills repeatedly for text that
doesn't change. A place whose first ingest had no key gets another attempt on the next upsert.

## Google Places and Routes

`IPlacesService` has two implementations and `Program.cs` picks between them on whether
`GoogleMaps:ApiKey` is set: `GooglePlacesService` when there is a key, `SamplePlacesService`
when there isn't. A fresh clone therefore runs and answers requests with no credentials at all.

`GooglePlacesService` works in three steps: Routes computes the drive, points are sampled along
its polyline every `SampleSpacingKm`, and Places is asked what is near each sample. Both calls
are billed per field, so the `X-Goog-FieldMask` headers ask for exactly what is used — widening
one costs money.

**Detour minutes are an estimate**, not a routed figure: distance off route, doubled for the
return, at `DetourSpeedKmh`, plus a fixed overhead for leaving the route. The exact number
would need a routed call per candidate place per search.

**Route-relative values don't belong to a place.** Detour minutes, distance off route and route
progress depend on the drive, so they are computed per request and never stored — and
`/places/{id}`, which has no drive, returns them as zero. The client keeps the values it
already had from the suggestion list.

**Read the Maps Platform terms before designing storage.** Place IDs can be kept indefinitely,
but most other Places content has caching limits — `StoredPlace.RefreshedUtc` is what an expiry
sweep would work from. Your own derived data — generated summaries, review themes, detour
scores — isn't subject to that and can be stored freely.

## Conventions

- `sealed` on classes not designed for inheritance; `record` for models.
- Every I/O method takes a `CancellationToken` and passes it down.
- Enums serialise as names, not ordinals (`JsonStringEnumConverter` in `Program.cs`) — so
  reordering `PlaceCategory` can't silently re-map categories on the client.
- Validate at the endpoint, return `Results.BadRequest` with a reason; don't let bad input reach
  a service.
- Comments explain *why*, not *what*.

## Build config

- .NET 10, nullable reference types and implicit usings on.
- `Microsoft.OpenApi` is pinned to a 2.x version. Do not upgrade it to 3.x: the ASP.NET Core
  XML-comment source generator writes to `IOpenApiMediaType.Example`, which became read-only in
  3.x, and the build fails inside generated code.
- The solution is `Wayside.slnx` — .NET 10's XML solution format, not the older `.sln`.
