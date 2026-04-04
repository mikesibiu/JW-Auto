# JW Library Auto — Romanian Language Support: Lessons Learned

## jw.org API Language Codes

- Romanian is **`M`** (Română), NOT `RO`, `ROL`, or any ISO code.
- English is `E`.
- Pass `langwritten=M&txtCMSLang=M` to every jw.org pub-media API call for Romanian content.
- Source: confirmed by live API calls to `https://b.jw-cdn.org/apis/pub-media/GETPUBMEDIALINKS`.

## What Content Is Available in Romanian

| Content | Romanian available? | Notes |
|---------|---------------------|-------|
| Bible (NWT) | ✅ Yes | `pub=nwt&langwritten=M` |
| Watchtower | ✅ Yes | `pub=w&langwritten=M` |
| Kingdom Songs | ✅ Yes | `pub=sjjc&langwritten=M` |
| LFB (CBS) | ✅ Yes | `pub=lfb&langwritten=M` |
| Meeting Workbook | ❌ No | API returns 404; fall back to English MWB silently |

## Meeting Workbook Fallback Pattern

The MWB (`pub=mwb`) returns a 404/empty response for `langwritten=M`. The page exists at
`jw.org/ro/library/jw-meeting-workbook/` but the audio tracks are EN-only.

**Pattern used:** try Romanian → if null, try English → if null, use hard-coded fallback URL.
```kotlin
val url = fetchWorkbookUrl(issue, track, lang)
    ?: fetchWorkbookUrl(issue, track, LanguagePreference.LANG_ENGLISH)
    ?: fallbackWorkbookUrl(weekStart)
```

## Watchtower Date Format Differs by Language

- English title: `"Speak Truth Graciously (March 2-8)"`
- Romanian title: `"Vorbiți cu adevăr (2-8 martie)"` — **day comes first**, then month name

The original regex `\(March \d` was English-only. The language-agnostic fix matches both:
```kotlin
val pattern = Regex("\\((?:[A-Za-z]+ )?$day[^0-9]")
```
This matches `(March 2-…` (EN) and `(2-8 …` (RO) because:
- EN: `(?:[A-Za-z]+ )?` consumes "March ", then `$day` matches the number
- RO: `(?:[A-Za-z]+ )?` matches nothing (zero occurrences), then `$day` matches the leading day number

## Cache Key Namespacing

All Room DB cache keys were updated to include the language prefix so EN and RO content coexist:
```kotlin
fun cacheKey(contentType: String, weekStart: String, lang: String = "E"): String =
    "$lang:$contentType:$weekStart"
// e.g. "M:watchtower:2026-03-02"
```

Without namespacing, switching language would serve cached EN content for RO requests.

## Per-Language In-Memory Caches

Two in-memory caches are language-sensitive and must be invalidated on toggle:

1. **Bible catalog** (`bibleCatalog` + `bibleCatalogLang`): tracks for each book differ by language.
   Invalidated by checking `bibleCatalogLang != langCode` before returning cached value.

2. **LFB catalog** (`lfbCatalogByLang` map): one `LfbLessonCatalog` per language, keyed by lang code.
   The map is cleared entirely on language toggle.

## Song Cache Is Language-Keyed in Room

`CachedSong.language` column already existed. Added `getSongsByLanguage(lang)` and
`deleteSongsByLanguage(lang)` to `SongDao` so EN and RO songs coexist in the DB without
wiping the other language on refresh.

## Language Toggle UI

- Toggle item is the **first** item in the root browse list (most visible in Auto head unit).
- It is a **playable** (non-browsable) item — tapping it fires `onPlayFromMediaId`.
- The service intercepts `LANG_TOGGLE_ID` before any URL-resolution logic, toggles the pref,
  clears in-memory caches, and calls `notifyChildrenChanged(ROOT_ID)` to force a UI refresh.
- Label text is self-describing: `"🌐 English  |  tap pentru Română"` / `"🌐 Română  |  tap for English"`.

## Auto-Detection

`LanguagePreference.get()` falls back to `autoDetect()` when no override is stored:
```kotlin
private fun autoDetect(): String =
    if (Locale.getDefault().language == "ro") LANG_ROMANIAN else LANG_ENGLISH
```
Romanian device → Romanian content automatically, no user action needed.

## Watchtower Override Map Is English-Only

`JWOrgContentUrls.watchtowerOverrideUrl()` contains hard-coded EN URLs as a safety net.
When language is Romanian, the override map is skipped entirely (dynamic-only, no fallback URL).
This is intentional — if the dynamic Romanian WT fetch fails, we return the generic fallback,
not an English URL dressed as Romanian.

## LfbLessonCatalog Constructor Change

`LfbLessonCatalog` gained a `langCode: String = "E"` parameter. The SharedPrefs key is
namespaced: `"lfb_catalog_cache_$langCode"`. This ensures EN and RO LFB indexes are cached
separately with independent 30-day TTLs.
