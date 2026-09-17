# Wayside API

Backend for the [Wayside](../) Android app. Serves place suggestions along a route, generates
review summaries, and keeps the API keys off the device.

ASP.NET Core minimal APIs, .NET 10.

## Running it

```bash
dotnet run --project Wayside.Api
```

Then:

```bash
curl http://localhost:5263/health
curl "http://localhost:5263/api/routes/suggestions?origin=Alfama&destination=Sintra&budget=30"
curl http://localhost:5263/api/places/peninha
```

OpenAPI lives at `/openapi/v1.json` in development.

## Endpoints

| Method | Route | Returns |
|---|---|---|
| `GET` | `/health` | Liveness probe |
| `GET` | `/routes/suggestions?origin=&destination=&budget=` | Places along the drive that fit the detour budget |
| `GET` | `/places/{placeId}` | Full detail for one place, including its summary |

## Configuration

Secrets are read from user-secrets in development — never from `appsettings.json`, which is
committed:

```bash
cd Wayside.Api
dotnet user-secrets set "GoogleMaps:ApiKey" "AIza..."   # Places + Routes, IP-restricted
dotnet user-secrets set "Gemini:ApiKey" "AIza..."       # review summaries, free tier
```

The app starts without either. No Maps key means places come from a fixed sample set instead
of from Google; no Gemini key means places are stored without a generated summary.

## Project layout

```
Wayside.Api/
  Configuration/   options bound from configuration
  Endpoints/       minimal-API route groups
  Services/        one interface per capability, implementations alongside
  Models/Api/      the contract with the mobile client
```

See `CLAUDE.md` for architecture and conventions in detail.
