# Quick Start Guide

## Three Simple Scripts

### 1. Update Meeting Workbooks
```bash
python3 scripts/update_meeting_workbook.py
```
**Updates:** `meeting_workbook_mp3s.csv`
**Contains:** Full weekly meeting MP3s (one per week)

---

### 2. Update Watchtower Studies
```bash
python3 scripts/update_watchtower_study.py
```
**Updates:** `watchtower_study_mp3s.csv`
**Contains:** Weekly Watchtower study article MP3s (one per week)

---

### 3. Build Meeting Subsections
```bash
python3 scripts/build_subsections_csv.py
```
**Creates:** `meeting_subsections_mp3s.csv`
**Contains:** Individual Bible reading chapters and CBS lessons (multiple per week)

---

## Output Files

| File | What It Contains | Example |
|------|------------------|---------|
| `meeting_workbook_mp3s.csv` | Complete weekly meetings | 1 MP3 = entire meeting |
| `watchtower_study_mp3s.csv` | Weekly Watchtower studies | 1 MP3 = one study article |
| `meeting_subsections_mp3s.csv` | Bible chapters + CBS lessons | 5 MP3s = 3 Bible chapters + 2 lessons |

---

## First Time Setup

```bash
# Navigate to project directory
cd /Users/mfarace/ClaudeProjects/AndroidApps

# Run all three scripts
python3 scripts/build_subsections_csv.py
python3 scripts/update_meeting_workbook.py
python3 scripts/update_watchtower_study.py
```

---

## Regular Maintenance

**Weekly or Monthly:**
```bash
python3 scripts/update_meeting_workbook.py
python3 scripts/update_watchtower_study.py
```

**When adding new months:**
1. Edit `scripts/meeting_schedule_data.py`
2. Add new week entries
3. Run: `python3 scripts/build_subsections_csv.py`

---

## What's the Difference?

### Full Meeting (meeting_workbook_mp3s.csv)
```
November 17-23 → One 6-minute MP3 with entire meeting
```

### Subsections (meeting_subsections_mp3s.csv)
```
November 17-23 → Five separate MP3s:
  - Song of Solomon 6 (Bible Reading)
  - Song of Solomon 7 (Bible Reading)
  - Song of Solomon 8 (Bible Reading)
  - Lesson 36 (CBS)
  - Lesson 37 (CBS)
```

---

## Need More Help?

See full documentation: `scripts/CSV-README.md`
