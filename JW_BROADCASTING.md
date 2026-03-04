# JW Broadcasting Integration Guide

## Overview
JW Broadcasting support surfaces two Mediator API feeds directly inside Android Auto:
- **Monthly Program** (`StudioMonthlyPrograms`)
- **Governing Body Updates** (`StudioNewsReports`)

The integration was added in commit `bbfa07f` (November 22, 2025) and is powered by the classes in `app/src/main/java/org/jw/library/auto/data/JWBroadcastRepository.kt` and `app/src/main/java/org/jw/library/auto/data/api/MediatorApiService.kt`.

## Architecture
1. `MediatorApiClient` configures Retrofit against `https://mediator.jw.org/services/media/` with logging and TLS defaults.
2. `MediatorApiService` exposes `GET categories/{language}/{category}?detailed=1` returning the category payload (key, localized name, media list, file URLs).
3. `JWBroadcastRepository` calls the service for both categories, filters to items published within the last year, and maps them into `MediaContent` objects that the rest of the app renders alongside weekly meeting content.
4. UI strings live in `app/src/main/res/values/strings.xml` (`content_broadcasting_monthly`, `content_broadcasting_update`).

```
Mediator API → MediatorApiClient/Service → JWBroadcastRepository → MediaContent tree → Android Auto UI
```

## Configuration Notes
- **Language**: Hardcoded to English (`E`). Add locale switching by making the `LANGUAGE` constant dynamic in `JWBroadcastRepository` and exposing a settings toggle.
- **Filtering**: Items older than one year are dropped (`Duration.ofDays(365)`), ensuring the UI feels current.
- **Ordering**: Results are sorted by `firstPublished` descending so the latest updates appear first.
- **Fallbacks**: Missing titles default to localized strings and missing URLs are skipped entirely to prevent playback failures.
- **ID Scheme**: Each `MediaContent` id is prefixed with `broadcast-` and includes the Mediator GUID/natural key to avoid collisions with meeting-week nodes.

## Testing Checklist
1. **Smoke test**
   - Build/install debug build.
   - Launch Android Auto (device or DHU) and open “JW Broadcasting”.
   - Confirm the Monthly Program and Governing Body nodes list recent items with publish dates.
2. **Playback**
   - Select the top item in each category and verify the MP3/MP4 stream plays without validation errors (they share the standard HTTPS whitelist).
3. **Offline behavior**
   - Disconnect network and refresh the subtree: Mediator items should disappear until a new fetch runs (currently there is no cache layer for them).
4. **Localization**
   - Switch device language: only Month/Day formatting changes today. If broader localization is required, add translations for the string resources mentioned above.

## Troubleshooting
- **Empty list**: Check `adb logcat | grep JWBroadcastRepository` for HTTP failures. Mediator occasionally returns HTTP 403 when rate limits trigger; retry after a few minutes.
- **Stale dates**: Ensure the device clock/timezone are correct—publish dates are parsed from ISO 8601 strings and compared against `Instant.now()`.
- **Playback blocked**: Verify the stream’s host (`download-a.akamaihd.net`, `mediator.jw.org`) remains in `allowedHosts` inside `PlaybackManager`.
- **Extending coverage**: Add more categories (e.g., `StudioCongregationUpdates`) by updating the constants in `JWBroadcastRepository` and adding localized strings.

Keep this guide alongside `PHASE1_API_INTEGRATION.md` and `PHASE2_CACHING_COMPLETE.md` so future contributors can trace how streaming content and Mediator feeds fit together.
