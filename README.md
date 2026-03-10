# JW Library Auto — Android Auto Media App

Stream weekly meeting content and Kingdom songs through Android Auto for safe in-car spiritual enrichment.

## Overview

**JW Library Auto** is an Android Auto media app that streams real JW.org content:

- Weekly Bible reading assignments
- Watchtower Study articles
- Congregation Bible Study (CBS) lessons
- Meeting Workbook audio
- Kingdom Songs (grouped, browsable)
- Bible Dramas
- JW Broadcasting (Mediator API)

Content is organized as **This Week / Last Week / Next Week**, calculated from the real device date using Monday-based week boundaries.

## Architecture

```
Android Auto (Gearhead)
        │ binds
        ▼
JWLibraryAutoService          ← MediaBrowserServiceCompat
   ├── ContentRepository       ← browsable media tree + week routing
   │     └── JWOrgRepository   ← cache → jw.org API → JSON fallback → inline map
   │           ├── Room DB     ← caches API responses (TTL: 5 weeks)
   │           └── MeetingSectionsProvider  ← reads meeting_sections.json
   ├── PlaybackManager         ← ExoPlayer + AudioFocus + MediaSession
   └── ContentSyncWorker       ← WorkManager 24h background pre-fetch
```

### Content Resolution Chain

For most content (Watchtower, Workbook, Songs, Broadcasting):
1. **Room cache** — if fresh (< 5 weeks), return cached URL
2. **jw.org API** — live network fetch, cache result
3. **JSON fallback** — `res/raw/meeting_sections.json`
4. **Inline override map** — `JWOrgContentUrls.kt` (WATCHTOWER_OVERRIDES, WORKBOOK_OVERRIDES)

For **Bible reading** and **CBS** specifically:
- Room cache is **bypassed entirely** — these always read from `meeting_sections.json`
- Reason: JSON is the authoritative source; Room TTL caused stale-data bugs

### Key Files

| File | Purpose |
|------|---------|
| `service/JWLibraryAutoService.kt` | MediaBrowserService, session lifecycle |
| `data/JWOrgRepository.kt` | Content resolution chain, cache-bust logic |
| `data/ContentRepository.kt` | Browsable tree builder, week routing |
| `data/api/JWOrgContentUrls.kt` | WATCHTOWER_OVERRIDES, WORKBOOK_OVERRIDES |
| `data/meeting/MeetingSectionsProvider.kt` | Parses `meeting_sections.json` |
| `playback/PlaybackManager.kt` | ExoPlayer + AudioFocus |
| `res/raw/meeting_sections.json` | Bible reading + CBS URLs through 2026-04-27 |

## Data Coverage

| Content | Source | Coverage |
|---------|--------|----------|
| Bible reading | `meeting_sections.json` | Through 2026-04-27 |
| CBS lessons | `meeting_sections.json` | Through 2026-04-27 |
| Watchtower | WATCHTOWER_OVERRIDES | Through 2026-04-27 |
| Workbook | WORKBOOK_OVERRIDES + API | API handles dates beyond 2026-02-23 |
| Songs | jw.org API + Room | Dynamic |
| JW Broadcasting | Mediator API | Dynamic (no offline cache) |

**Action required ~April 2026**: Update `meeting_sections.json` and `WATCHTOWER_OVERRIDES` with new weeks.

## Cache-Bust on APK Update

`JWOrgRepository.clearCacheIfVersionChanged()` runs synchronously in `Service.onCreate()` via `runBlocking(Dispatchers.IO)`. If `BuildConfig.VERSION_CODE` differs from the value stored in SharedPreferences, the entire Room cache is cleared before any content request is served.

**Rule**: Bump `versionCode` in `app/build.gradle.kts` on every install.

## Building

### Requirements

- JDK 17 (Temurin recommended — Homebrew Java 25 breaks Gradle)
- Android SDK 34

### Build Commands

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home

# Debug build
./gradlew assembleDebug

# Release build (signed)
./gradlew assembleRelease

# Run unit tests (QA gate — must pass before install)
./gradlew test
```

## QA / Testing

### Unit Tests

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew test
```

Key test suites:

| Suite | What it catches |
|-------|----------------|
| `WeeklyContentSnapshotTest` | Exact filename assertions for all weeks Mar–Apr 2026 |
| `CacheBustAndFetchTest` | Cache-bust correctness, stale URL protection |
| `ServiceSmokeTest` | Service `onCreate()` doesn't crash (Room threading, WorkManager init) |
| `JWOrgRepositoryTest` | Live network calls to jw.org API (requires internet) |
| `JWOrgRepositoryDramaTest` | Drama URL resolution with fake services |

### On-Device Verification

`ContentVerificationTest` (instrumented) connects as a real `MediaBrowserCompat` client to `JWLibraryAutoService` and asserts:

- CBS is NOT lesson 57 (known bad mapping bug)
- CBS lesson is 68 or later (correct for current weeks)
- Bible reading is NOT Isaiah 40 or earlier
- All 4 "This Week" items are present

```bash
# WARNING: uninstalls app after test run — only run with explicit permission
./gradlew connectedDebugAndroidTest
```

### Runtime Logcat Verification

After installing, CBS and Bible reading URLs are logged:

```
adb logcat | grep CONTENT_CHECK
```

Example output:
```
CONTENT_CHECK congregation_study 2026-03-09 -> [lfb_E_068.mp3, lfb_E_069.mp3]
CONTENT_CHECK bible_reading 2026-03-09 -> [Isa_E_043.mp3, Isa_E_044.mp3]
```

## Security

- Package validation in `onGetRoot()` — only Android Auto / Google Assistant can bind
- HTTPS-only URI validation in `PlaybackManager`
- Network security config with cleartext blocked
- `allowBackup="false"` in manifest

## Known Issues

See [TODO.md](TODO.md) for active bugs and planned work.

## Android Auto Testing

```bash
# Start Desktop Head Unit
$ANDROID_SDK_ROOT/extras/google/auto/desktop-head-unit

# Enable developer mode on phone
adb shell settings put global android_auto_dev_mode 1
```

## License

Reference implementation for personal/educational use. Respect jw.org terms of service for content distribution.
