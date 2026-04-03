# JW Library Auto — Lessons Learned
## For building an iOS / Apple CarPlay version

This document captures architectural decisions, API discoveries, and hard-won
debugging insights from building the Android Auto version. An iOS CarPlay app
would use the same jw.org APIs and content structure.

---

## 1. jw.org API — How It Works

All content is fetched from two public endpoints (no authentication required):

### Pub-Media endpoint
```
GET https://b.jw-cdn.org/apis/pub-media/GETPUBMEDIALINKS
    ?output=json
    &pub=<PUBCODE>
    &fileformat=MP3
    &alllangs=0
    &langwritten=E
    &txtCMSLang=E
    &issue=<YYYYMM>   # bimonthly for mwb, monthly for w
```

Key `pub` codes:
| Code | Content |
|------|---------|
| `mwb` | Meeting Workbook (bimonthly: Jan/Mar/May/Jul/Sep/Nov) |
| `w` | Watchtower Study Edition (monthly) |
| `lfb` | "Enjoy Life Forever" CBS book |
| `nwt` | New World Translation (Bible) |
| `sjjm` | Kingdom Songs (with male vocals) |

Response structure: `files.E.MP3[]` — each item has:
- `title` — human-readable (e.g., "March 9-15", "Song 15", "Isaiah 43")
- `file.url` — CDN MP3 URL
- `track` — 1-based track number within the issue
- `docid` — wol.jw.org page ID (MWB tracks only — very useful)
- `booknum` — Bible book number (NWT tracks only)
- `label` — display label (e.g., "March 9–15", "Lesson 68")

### WOL (Watchtower Online Library) docid pages
```
GET https://wol.jw.org/en/wol/d/r1/lp-e/<docid>
```
Returns HTML. For MWB weekly pages, contains:
- Header with week date and Bible book/chapter: `MARCH 9-15 ISAIAH 43`
- Bible reading assignment: `Bible Reading (4 min.) Isa 44:9-20`
- CBS content: `lfb lessons 68-69` or `lfb intro to section 11 and lessons 68-69`

---

## 2. Issue/Date Mapping

### Meeting Workbook (MWB)
- Bimonthly: January issue covers Jan+Feb, March covers Mar+Apr, etc.
- Issue code formula: if month is odd → use it; if even → use month-1
  - March 9 week → issue `202603` (March issue)
  - April 6 week → issue `202603` (still March issue)
  - April 27 week → issue `202605` (May issue starts)
- Each issue has 8 tracks, one per week, sorted by `track` field
- Track `title` contains the week date: `"March 9-15"` or `"April 27–May 3"`

### Watchtower Study Edition
- Monthly: January issue covers meetings in February/March (~2 months ahead)
- To find the Watchtower for a given meeting week:
  1. Try issue = (meeting month − 2) — covers most weeks
  2. Try issue = (meeting month − 3) — covers early weeks of a month
- Match track by title: track title contains meeting week in parentheses e.g.,
  `"(March 9-15)"` or `"(March 2-8)"` — regex: `\(MonthName Day[^0-9]`

### CBS (Congregation Bible Study — "Enjoy Life Forever" book)
- Book has 120+ lessons; no date-to-lesson mapping in the API
- The wol.jw.org MWB page tells you which lessons: `lfb lessons 68-69`
- Pattern handles: `lfb lessons 68-69` AND `lfb intro to section 11 and lessons 68-69`
- **Do NOT include section intro audio** — user confirmed: lessons only
- Once you have lesson numbers, look up URLs via the LFB pub-media API:
  `pub=lfb` returns all tracks with `label` like `"Lesson 68"`

### Bible Reading (NWT)
- MWB header "MARCH 9-15 ISAIAH 43" gives start chapter (43)
- Bible reading assignment "Isa 44:9-20" gives end chapter (44)
- Combine → weekly range: Isaiah 43–44
- Chapter → URL: `pub=nwt&issue=0` returns all Bible chapters;
  response keyed by book number, chapters array indexed 0-based

---

## 3. Content Architecture

### What needs dynamic fetching vs. static data

| Content | Dynamic | Notes |
|---------|---------|-------|
| Meeting Workbook audio | Yes — API | Direct MP3 URL from mwb pub-media |
| Watchtower audio | Yes — API | Match by title string with week date |
| Bible reading URLs | Yes — API | NWT catalog + week range from wol.jw.org |
| CBS lesson URLs | Yes — API | LFB catalog + lesson numbers from wol.jw.org |
| Bible book catalog | Yes — API | NWT: one fetch, cache forever |
| LFB lesson catalog | Yes — API | LFB: one fetch, cache for session |
| Song audio | Yes — API | sjjm pub-media, all songs at once |

### Caching strategy
- Bible/Song catalogs: fetch once per app session (or daily)
- Weekly schedule (lesson numbers + chapter range): cache by ISO week date key
- MWB/Watchtower URLs: cache by week date key
- **Cache invalidation**: bump app version → clear content cache on launch

---

## 4. CBS Bugs — History and Fix

**The hard bug**: early version tried to infer CBS audio from track ordering in the
LFB API, which included section intro audio as the first track of each section.
This caused the wrong audio to play (intro instead of lesson content).

**Root cause**: LFB tracks include:
- Track type A: section intro (e.g., "Section 11 Introduction")
- Track type B: individual lessons (e.g., "Lesson 68", "Lesson 69")

**Fix**: Always derive lesson numbers from the wol.jw.org MWB page text, then
look up those specific lesson numbers in the LFB catalog by matching `label`.
Never use track position/ordering — always match by lesson number.

---

## 5. Bible Chapter Range — Parsing Strategy

**Wrong approach**: tried to infer end chapter from the next week's start chapter
(fragile — requires an extra network call, fails for last week of issue).

**Right approach** (confirmed against live data):
1. Parse `MARCH 9-15 ISAIAH 43` from page header → book=Isaiah, startCh=43
2. Parse `Bible Reading (4 min.) Isa 44:9-20` from page body → endCh=44
3. Combine → Isaiah 43–44

The Bible reading assignment always cites the exact end chapter. This is reliable
and requires no additional network requests.

**Parsing pitfall**: simple `find()` on the page will match date ranges like
"MARCH 2-8" before finding "ISAIAH 41". Use `findAll()` and skip matches where
the group text isn't a recognized Bible book name.

---

## 6. iOS / CarPlay Implementation Notes

### Framework equivalents

| Android | iOS |
|---------|-----|
| `MediaBrowserServiceCompat` | `MPPlayableContentDataSource` + `MPPlayableContentDelegate` OR Media3 MediaLibraryService → iOS has no exact equivalent; use `MPMusicPlayerController` or `AVQueuePlayer` |
| `MediaSessionCompat` | `MPRemoteCommandCenter` + `MPNowPlayingInfoCenter` |
| ExoPlayer | `AVPlayer` / `AVQueuePlayer` |
| `WorkManager` | `BGTaskScheduler` (background fetch) |
| `SharedPreferences` | `UserDefaults` |
| `Room` DB | `CoreData` or `SQLite` (via GRDB/SQLite.swift) |
| OkHttp | `URLSession` |

### CarPlay media app structure
- Implement `CPTemplateApplicationSceneDelegate` for CarPlay scene
- Use `CPListTemplate` for browsable lists (equivalent to `BrowsableItem`)
- Use `CPNowPlayingTemplate` for playback screen
- Register `CPMediaItemPlayableContent` capability in entitlements
- Audio session: `.playback` category with `.allowBluetooth` option

### Key iOS gotcha: background audio
- Must declare `audio` background mode in Info.plist
- `AVAudioSession.sharedInstance().setCategory(.playback)` in `AppDelegate`
- CarPlay audio continues when phone screen is off — same as Android Auto

### Networking
- `URLSession` is the equivalent of OkHttp — use `async/await` with `URLSession.data(for:)`
- No Gson: use `Codable` structs to decode JSON from jw.org API
- Same API endpoints work — no platform-specific differences

### Content model (same for both platforms)
The jw.org API returns identical JSON on iOS and Android. All parsing logic in
this doc applies directly. Reuse the same URL patterns, issue formulas, and
regex patterns (Swift `NSRegularExpression` or Swift `Regex`).

---

## 7. Data Verification

**Always verify `meeting_sections.json` (or equivalent) against wol.jw.org.**
In development, Bible reading chapters were off by 4 chapters (had Isaiah 40
for the week of March 9 when it should be Isaiah 43–44). The wol.jw.org
docid pages are the authoritative source.

Verification URL pattern: `https://wol.jw.org/en/wol/d/r1/lp-e/<docid>`

---

## 8. Known Edge Cases

| Scenario | Handling |
|----------|----------|
| Week spans two months (e.g., April 27–May 3) | Track title cross-month regex: `^Month Day[–-]` |
| Last week of MWB issue | Fetch next issue's first week to derive end chapter (or use Bible reading line) |
| CBS week has no lessons (holiday/assembly) | `parseCbsLessons` returns empty list; gracefully show nothing |
| Watchtower track not found in −2 months | Try −3 months before falling back |
| LFB catalog not yet loaded | `ensureLfbCatalog()` — fetch on demand, single network call |
| Room DB cache stale after data fix | Bump versionCode → `clearCacheIfVersionChanged()` wipes content cache |

---

## 9. What Worked Well

- Using `docid` from the MWB pub-media API to fetch wol.jw.org pages — single
  source of truth for week schedule
- Bimonthly issue formula for MWB — clean and reliable
- LFB lesson number → URL lookup via `label` field matching (not track order)
- Caching parsed schedules in SharedPreferences/UserDefaults keyed by ISO date
- Version-bust cache clearing to force reload after data fixes

---

*Generated from Android Auto implementation — April 2026*
