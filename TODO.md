# TODO — JW Library Auto

## Bug: CBS Still Plays Lesson 57 After All Fixes

**Status**: Unresolved. Shipped to user as lesson 57. All unit tests and instrumented tests pass.

### Symptoms

- Android Auto (via Gearhead / DHU) plays CBS lesson 57 instead of the correct lesson (68+)
- `ContentVerificationTest` (real MediaBrowser connection) **passes** — service returns correct URLs via `onLoadChildren`
- `CONTENT_CHECK` logcat lines show correct filenames after a fresh install
- Bug re-appears after DHU reconnect without reinstall

### Root Cause (suspected)

The service process stays alive between DHU sessions. ExoPlayer holds the last-played media item in memory (lesson 57 from a stale session).

When Gearhead reconnects it issues **`onPlay`** (resume) rather than **`onPlayFromMediaId`**. `PlaybackManager.onPlay()` calls `exoPlayer.play()` directly — it resumes the in-memory ExoPlayer state without re-fetching the media ID from `ContentRepository`.

Result: lesson 57 URL is still loaded in ExoPlayer and resumes playing, even though the Room cache is cleared and `onLoadChildren` would return the correct lesson.

### Fix Approach

**Short-term**: Intercept `onPlay` in `PlaybackManager`. If ExoPlayer has media from a previous session, re-fetch the current media ID from `ContentRepository` before calling `play()`. Something like:

```kotlin
override fun onPlay() {
    val currentId = mediaSession.controller.metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
    if (currentId != null && exoPlayer.mediaItemCount > 0) {
        // Re-fetch to ensure we have current-week URL, not stale in-memory state
        serviceScope.launch {
            val freshItem = contentRepository.resolveMediaItem(currentId)
            if (freshItem != null) prepareAndPlay(freshItem)
            else exoPlayer.play()
        }
    } else {
        exoPlayer.play()
    }
}
```

**Long-term**: Phase 5 — Media3 migration (see below). Media3 `MediaLibraryService` manages session state correctly across reconnects and eliminates this class of bug.

### Test to Add Before Closing

Add a test that simulates:
1. Service starts, ExoPlayer loads lesson 57 URL (simulating stale prior session)
2. `onPlay` is called (no `onPlayFromMediaId`)
3. Assert that PlaybackManager re-fetches the current week's URL, NOT resumes lesson 57

---

## Phase 5 — Media3 Migration

**Status**: Not started.

### What

Migrate `PlaybackManager` from the legacy stack:
```
MediaSessionCompat + ExoPlayer (manual sync)
```
to the modern Media3 stack:
```
MediaLibraryService + MediaSession (Media3) + ExoPlayer
```

### Why

1. **Eliminates the onPlay resume bug above** — Media3 `MediaSession` ties playback state to `MediaItem` metadata; reconnects re-resolve the item properly
2. **Eliminates manual state sync** — no more manually keeping `MediaSessionCompat` metadata in sync with ExoPlayer state
3. **Better session lifecycle** — Media3 handles Gearhead connect/disconnect correctly
4. **Official Android recommendation** — `MediaBrowserServiceCompat` is in maintenance mode; Media3 is the future

### Scope

- Replace `MediaBrowserServiceCompat` → `MediaLibraryService`
- Replace `MediaSessionCompat` → `MediaSession` (Media3)
- Replace manual ExoPlayer-to-session sync → `MediaSessionConnector` (built into Media3)
- Update `onGetRoot` / `onLoadChildren` → `onGetLibraryRoot` / `onGetChildren`
- Update `PlaybackManager` callbacks (`onPlay`, `onPlayFromMediaId`, `onPause`, etc.) → Media3 `SessionCallback`

### Risk

- `androidx.media:media` (legacy) and `androidx.media3` coexist — need careful migration to avoid duplicate session registration
- Gearhead compatibility: ensure `MediaLibraryService` is discovered correctly by Android Auto

---

## Data Maintenance — April 2026

- Update `res/raw/meeting_sections.json` with Bible reading and CBS assignments for weeks after 2026-04-27
- Update `JWOrgContentUrls.WATCHTOWER_OVERRIDES` for weeks after 2026-04-27
- Check whether WORKBOOK_OVERRIDES needs extension or if the dynamic API covers new dates
