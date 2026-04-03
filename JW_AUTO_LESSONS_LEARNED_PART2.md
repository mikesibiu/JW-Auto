# JW Library Auto — Lessons Learned, Part 2
## Browsable Content Tree Design — Bible & Songs Navigation

This document covers the UI/browsing architecture decisions made for Bible chapter
navigation and Kingdom Songs grouping. Part 1 covers jw.org API patterns.

---

## 1. Android Auto Browsable Tree — How It Works

Android Auto uses a **media browse tree** where each item is either:
- **Browsable** — a folder; tapping it opens a child list (no playback)
- **Playable** — a leaf; tapping it starts audio immediately

An item **cannot be both** browsable and playable. If you want a book to be
playable (tap = play from ch.1) AND browsable (tap = open chapters), you must
choose one. We chose browsable — the chapter list is always shown.

### The Tree in This App

```
Root
├── Weekly Meetings
│   ├── This Week's Meeting Content
│   │   ├── Bible Reading        [playable — playlist of chapters]
│   │   ├── Watchtower Study     [playable — single track]
│   │   ├── Congregation Bible Study [playable — playlist of lessons]
│   │   └── Meeting Workbook     [playable — single track]
│   ├── Last Week's Content      [same structure]
│   └── Next Week's Content      [same structure]
│
├── Bible & Songs
│   ├── Hebrew Scriptures        [browsable]
│   │   ├── Gen — Genesis        [browsable → chapters]
│   │   ├── Ex — Exodus          [browsable → chapters]
│   │   └── … (39 books)
│   ├── Greek Scriptures         [browsable]
│   │   ├── Matt — Matthew       [browsable → chapters]
│   │   └── … (27 books)
│   └── Kingdom Songs            [browsable]
│       ├── Songs 001–020        [browsable]
│       │   ├── 001 - Jehovah…   [playable]
│       │   └── … (20 songs)
│       └── … (8 groups)
│
└── JW Broadcasting
    ├── [Month] JW Broadcasting  [playable]
    └── Governing Body Update …  [playable — sorted newest first]
```

---

## 2. Kingdom Songs — Grouping Pattern

**Problem:** 160+ songs would create a list too long to scroll safely while driving.

**Solution:** Group into folders of 20, labeled "Songs 001–020", "Songs 021–040", etc.

**ID scheme:**
```
songs_group_0   → browsable folder "Songs 001–020"
songs_group_1   → browsable folder "Songs 021–040"
song-15         → playable leaf "015 - [Title]"
```

**Key details:**
- Numbers are zero-padded to 3 digits for display ("015", not "15") — sorts correctly
- Group index is 0-based; display numbers are 1-based
- Songs are fetched from the `sjjm` (Kingdom Songs with male vocals) pub-media API
- Each song is a single track — no playlist chaining needed

**iOS/CarPlay equivalent:**
Use `CPListTemplate` for both the group level and the song level.
Each `CPListItem` at the group level has `showsDisclosureIndicator = true`.

---

## 3. Bible Chapter Navigation — Grouping Pattern

**Problem:** Some books (Psalms 150 ch, Isaiah 66 ch, Genesis 50 ch) would produce
unnavigably long chapter lists while driving.

**Solution:** Same folder-grouping pattern as Kingdom Songs, but sized for chapters:
- Group size: **10 chapters per folder** (songs use 20 — chapters are shorter/more numerous in long books)
- **Threshold:** Books with ≤ 10 chapters skip the group level entirely — chapters shown directly

**Tree structure for a long book (e.g., Isaiah — 66 chapters):**
```
Isaiah (browsable)
├── Chapters 1–10   (browsable)
│   ├── Chapter 1   (playable)
│   └── … Chapter 10
├── Chapters 11–20  (browsable)
└── … Chapters 61–66
```

**Tree structure for a short book (e.g., Ruth — 4 chapters):**
```
Ruth (browsable)
├── Chapter 1   (playable)
├── Chapter 2   (playable)
├── Chapter 3   (playable)
└── Chapter 4   (playable)
```

**ID scheme:**
```
bible-hebrew-23          → browsable book folder (Isaiah, book #23)
bible-hebrew-23-cg-0     → browsable chapter group (cg = chapter group, index 0)
bible-hebrew-23-ch-1     → playable chapter leaf
```

Parsing from ID:
- Split on `"-cg-"` to separate book ID from group index
- Split on `"-ch-"` to separate book ID from chapter number
- `bookId.split("-").last().toInt()` → book number

**Playback behavior for chapter items:**
Each chapter leaf carries a **playlist** of all remaining chapters in the book,
not just a single chapter. Tapping "Chapter 5" plays chapters 5, 6, 7 … to end.
This lets the user pick up anywhere and continue listening through the book —
better for car use than stopping after one chapter.

```
Chapter 5 item:
  streamUrl = chapter5.mp3
  playlistUrls = [chapter5.mp3, chapter6.mp3, … lastChapter.mp3]
```

---

## 4. Book Display Conventions

Bible books are displayed as:
- **Title:** Abbreviation (e.g., "Isa", "Matt", "Gen") — short enough for car screen
- **Subtitle:** Full name (e.g., "Isaiah", "Matthew", "Genesis")

This matches the JW Library convention and keeps the list scannable at a glance.

Kingdom Songs are displayed as:
- **Title:** "NNN - Song Title" (zero-padded number + title)
- No subtitle

---

## 5. NWT Audio API — What It Does and Doesn't Have

The NWT (`pub=nwt`) pub-media API returns:
- All 66 Bible books
- Each chapter as a separate MP3 track
- `booknum` field (1–66) to identify which book
- `track` field (1-based) for chapter ordering within the book
- Title format: "Genesis - Chapter 1", "Isaiah - Chapter 41", etc.

**What it does NOT have:**
- Book introduction/preface audio — no such tracks exist in the API response
- Chapter summaries or cross-reference audio
- Track 0 intros — the first track for each book is track 1 = Chapter 1

The `BibleBookAudio.intro` field exists in the data model and is wired through
the UI, so if jw.org ever adds intro tracks, they would appear automatically
without code changes. Currently it is always `null`.

---

## 6. Routing Chapter Group IDs — Pattern to Follow

In `ContentRepository.getChildren(parentId)`, the routing uses simple string matching:

```kotlin
// Bible book (e.g., "bible-hebrew-23")
parentId.matches(Regex("bible-(hebrew|greek)-\\d+")) -> bibleChapters(parentId)

// Chapter group (e.g., "bible-hebrew-23-cg-0")
parentId.contains("-cg-") -> bibleChapterGroup(parentId)

// Song group (e.g., "songs_group_2")
parentId.startsWith("songs_group_") -> loadSongGroup(index)
```

**Rule:** More specific patterns must be checked before less specific ones.
`"-cg-"` check must come after the book regex check — otherwise a group ID
could accidentally match the book regex (it won't, but order still matters for clarity).

---

## 7. iOS / CarPlay Equivalent Implementation Notes

### CPListTemplate structure
```swift
// Book list
let bookItems = bibleBooks.map { book in
    CPListItem(text: book.abbreviation, detailText: book.title)
}
let bookTemplate = CPListTemplate(title: "Hebrew Scriptures",
                                   sections: [CPListSection(items: bookItems)])

// Chapter groups (for long books)
let groupItems = chapterGroups.map { group in
    CPListItem(text: "Chapters \(group.first)–\(group.last)",
               detailText: nil,
               image: nil,
               accessoryImage: nil,
               accessoryType: .disclosureIndicator)
}

// Chapter leaves
let chapterItems = chapters.map { ch in
    CPListItem(text: "Chapter \(ch.number)", detailText: book.title)
}
```

### Playlist from a chapter
In iOS, `AVQueuePlayer` handles playlists natively:
```swift
let items = chapterUrls.dropFirst(selectedIndex).map {
    AVPlayerItem(url: $0)
}
let player = AVQueuePlayer(items: items)
```

### Group size recommendation
Use **10 chapters per group** for Bible (same as Android version).
Use **20 songs per group** for Kingdom Songs (matches Android version).
These sizes were tuned for comfortable in-car navigation.

---

## 8. Content That Is Single-Track vs. Playlist

| Content | Single track | Playlist |
|---------|-------------|----------|
| Meeting Workbook | Yes | No |
| Watchtower Study | Yes | No |
| Bible Reading (weekly) | No | Yes — 1–3 chapters |
| CBS (weekly) | No | Yes — 2 lessons |
| Bible chapter (tap) | No | Yes — chapter to end of book |
| Kingdom Song | Yes | No |
| JW Broadcasting | Yes | No |

---

## 9. Lessons Learned — UI Design for Driving

1. **Never show more than ~15–20 items in a list** — drivers can't scroll safely
2. **Group size matters** — 10 for ordered content (chapters), 20 for catalog content (songs)
3. **Skip grouping for short content** — showing 2 sub-folders for a 4-chapter book is worse than showing 4 chapters directly. Threshold: group only if count > group size
4. **Items can't be both playable and browsable** — choose browsable when the content has sub-items; the user will learn to navigate in
5. **Subtitles help context** — show book name as subtitle on chapter items so users know where they are after scrolling
6. **Playlist-from-selection** — always build playlist from the tapped item to the end; don't stop after one chapter. In-car listening is linear.

---

*Generated from Android Auto implementation — April 2026*
*See Part 1 (JW_AUTO_LESSONS_LEARNED.md) for jw.org API patterns and iOS framework equivalents.*
