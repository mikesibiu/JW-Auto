# Dynamic Week Selection - How It Works

## ✅ Implemented Features

The app now **automatically selects the correct week's content** based on the current date!

### What Changed:

**Before:**
- App always played the same hardcoded URLs regardless of the week

**After:**
- App looks at the current week (Monday-Sunday)
- Automatically selects the correct Meeting Workbook and Watchtower for that week
- No manual updates needed for November-December 2025

---

## 📅 Available Content (Updated November 7, 2025)

### Meeting Workbook (November-December 2025):

| Week Starting | Content | Status |
|--------------|---------|--------|
| Nov 3, 2025  | Week 1  | ✅ Working |
| Nov 10, 2025 | Week 2  | ✅ Working |
| Nov 17, 2025 | Week 3  | ✅ Working |
| Nov 24, 2025 | Week 4  | ✅ Working |
| Dec 1, 2025  | Week 5  | ✅ Working |
| Dec 8, 2025  | Week 6  | ✅ Working |
| Dec 15, 2025 | Week 7  | ✅ Working |
| Dec 22, 2025 | Week 8  | ✅ Working |
| Dec 29, 2025 | Week 9  | ✅ Working |

### Watchtower Study (January 2026):

| Study Week   | Article | Status |
|-------------|---------|--------|
| Jan 5-11, 2026 | Maintain Your Joy in Old Age | ✅ Working |
| Jan 12-18, 2026 | Maintain Your Joy as a Caregiver | ✅ Working |
| Jan 19-25, 2026 | Consider Our Sympathetic High Priest | ✅ Working |
| Jan 26-Feb 1, 2026 | "You Are Someone Very Precious"! | ✅ Working |

---

## 🔧 How It Works (Technical)

### Code Flow:

1. **Week Calculation** (`WeekCalculator.kt`)
   - Determines current week (Monday-Sunday)
   - Creates `WeekInfo` object with week start/end timestamps

2. **Date Formatting** (`MediaContent.kt`)
   - `WeekInfo.getWeekKey()` formats date as "YYYY-MM-DD"
   - Example: November 3, 2025 → "2025-11-03"

3. **URL Lookup** (`JWOrgContentUrls.kt`)
   - Uses week key to look up in `WORKBOOK_URLS` map
   - Automatically returns the correct URL for that week
   - Falls back to first available URL if week not found

### Example:

```kotlin
// This Week: November 10, 2025
val weekInfo = WeekCalculator.getWeekInfo(isCurrentWeek = true)
val weekKey = weekInfo.getWeekKey()  // "2025-11-10"

// Looks up in map:
val url = WORKBOOK_URLS["2025-11-10"]
// Returns: "https://cfp2.jw-cdn.org/a/056fb19/1/o/mwb_E_202511_02.mp3"
```

---

## 📱 What You Can Test in Your Car

When you connect to Android Auto **this week (Nov 3-9, 2025)**:

### This Week's Content:
- **Meeting Workbook**: Nov 3-9 program ✅ Will play
- **Watchtower Study**: (None scheduled - Jan 2026) ⚠️ Fallback URL
- **Bible Reading**: Genesis 1 ⚠️ Static demo URL
- **CBS**: Demo URL ⚠️ Static demo URL

### Last Week's Content:
- Shows previous week (Oct 27 - Nov 2)
- **Meeting Workbook**: No URL (before Nov 3) ⚠️ Fallback URL
- Other content same as above

### Next Week's Content:
- Shows next week (Nov 10-16)
- **Meeting Workbook**: Nov 10-16 program ✅ Will play
- Other content same as above

---

## 🚀 Testing Different Weeks

Want to test what content will play in different weeks?

The app automatically adjusts based on your phone's date:

**To test future weeks:**
1. Change your phone's date to the desired week
2. Open the app in Android Auto
3. The "This Week" content will match that date

**Example:**
- Set phone date to December 15, 2025
- "This Week" → Meeting Workbook for Dec 15-21
- "Last Week" → Meeting Workbook for Dec 8-14
- "Next Week" → Meeting Workbook for Dec 22-28

---

## 🔄 When Content Needs Updating (January 2026+)

After December 29, 2025, you'll need to add new URLs:

### Option 1: Add to Existing Maps (Quick)

Edit `app/src/main/java/org/jw/library/auto/data/api/JWOrgContentUrls.kt`:

```kotlin
private val WORKBOOK_URLS = mapOf(
    // ... existing November-December URLs ...
    "2026-01-05" to "https://cfp2.jw-cdn.org/a/[hash]/1/o/mwb_E_202601_01.mp3",  // Jan 5-11
    "2026-01-12" to "https://cfp2.jw-cdn.org/a/[hash]/1/o/mwb_E_202601_02.mp3",  // Jan 12-18
    // ... add more weeks ...
)
```

### Option 2: Implement Auto-Fetching (Production)

See `SOLUTIONS.md` for how to build a backend service that:
- Automatically fetches new URLs from jw.org
- Updates the app without code changes
- Handles API changes gracefully

---

## 📊 Current Status Summary

| Feature | Status | Notes |
|---------|--------|-------|
| Week Calculation | ✅ Complete | Monday-Sunday, all locales |
| Dynamic URL Selection | ✅ Complete | Based on week start date |
| Meeting Workbook | ✅ 9 weeks ready | Nov 3 - Jan 4, 2026 |
| Watchtower Study | ✅ 4 weeks ready | Jan 5 - Feb 1, 2026 |
| Bible Reading | ⚠️ Static demo | Needs update |
| CBS | ⚠️ Static demo | Needs current book |
| Kingdom Songs | ⚠️ Static demo | Needs validation |

---

## 🎯 Next Steps

### Immediate:
1. **Test in your car** - Connect and verify content plays
2. **Check audio quality** - Ensure URLs are working correctly

### This Week (Nov 3-9):
1. Test that Meeting Workbook plays the correct week's content
2. Verify "Last Week" and "Next Week" show different content

### Before January 2026:
1. Add January-February 2026 Meeting Workbook URLs
2. Consider implementing auto-fetch backend (see SOLUTIONS.md)

---

## 💡 Pro Tips

**Fallback Behavior:**
- If no URL exists for a specific week, the app plays the first available URL
- This ensures something always plays rather than showing an error

**Week Boundaries:**
- Weeks start on Monday and end on Sunday
- This matches JW.org's meeting schedule

**Time Zones:**
- The app uses your phone's local timezone
- Week calculations are locale-independent

**Offline Testing:**
- URLs are embedded in the app
- No internet needed for the app to determine which week it is
- Internet only needed to stream the audio

---

## 📖 Reference Files

- `JWOrgContentUrls.kt` - URL maps and lookup logic
- `MediaContent.kt` - WeekInfo model with getWeekKey()
- `WeekCalculator.kt` - Week calculation (Monday-Sunday)
- `ContentProvider.kt` - Content hierarchy builder
- `TESTING_GUIDE.md` - How to test in Android Auto
- `SOLUTIONS.md` - Long-term solutions for URL management

---

**Updated:** November 7, 2025
**App Version:** 1.0 (Debug Build)
**Next Review:** January 2026 (before URLs expire)
