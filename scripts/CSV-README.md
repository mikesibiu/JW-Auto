# JW.org MP3 Update Scripts

Complete documentation for the JW.org MP3 fetching and updating scripts for your Android Auto app.

## Overview

This suite of scripts automatically fetches MP3 URLs from jw.org for:
1. **Weekly Meeting Workbooks** - Complete meetings
2. **Watchtower Study Articles** - Weekly study articles
3. **Meeting Sub-Sections** - Individual Bible readings and CBS lessons

## Files Generated

| CSV File | Content | Rows | Script |
|----------|---------|------|--------|
| `meeting_workbook_mp3s.csv` | Full weekly meetings | 26+ | `update_meeting_workbook.py` |
| `watchtower_study_mp3s.csv` | Weekly Watchtower studies | 25+ | `update_watchtower_study.py` |
| `meeting_subsections_mp3s.csv` | Individual Bible/CBS sections | 110+ | `build_subsections_csv.py` |

---

## Script 1: update_meeting_workbook.py

### Purpose
Updates the meeting workbook CSV with new weekly meeting MP3s.

### What It Does
- Fetches the next 6 months of meeting workbook issues
- Extracts MP3 URLs for each week
- Compares with existing CSV to avoid duplicates
- Appends only new weeks

### Usage

```bash
python3 scripts/update_meeting_workbook.py
```

### Output Example
```
Updating Meeting Workbook CSV...
Found 26 existing weeks in CSV
Checking 6 issue codes: 202511, 202512, 202601...
Fetching issue 202511... Found 9 weeks
Fetching issue 202601... Found 8 weeks

New weeks found:
  - March 30–April 5
  - April 6-12

Added 2 new week(s) to meeting_workbook_mp3s.csv
```

### CSV Format
```csv
Meeting Week,MP3 URL
November 3-9,https://cfp2.jw-cdn.org/a/.../mwb_E_202511_01.mp3
November 10-16,https://cfp2.jw-cdn.org/a/.../mwb_E_202511_02.mp3
```

### Configuration
Edit these variables in the script:
```python
CSV_FILE = Path(__file__).parent.parent / "meeting_workbook_mp3s.csv"
MONTHS_TO_FETCH = 6  # Look ahead 6 months
```

---

## Script 2: update_watchtower_study.py

### Purpose
Updates the Watchtower study CSV with new weekly study article MP3s.

### What It Does
- Fetches the next 6 months of Watchtower issues
- Extracts study article MP3 URLs with their study weeks
- Filters out non-study articles (life stories, Q&A, etc.)
- Appends only new weeks

### Usage

```bash
python3 scripts/update_watchtower_study.py
```

### Output Example
```
Updating Watchtower Study CSV...
Found 21 existing weeks in CSV
Checking 6 issue codes: 202511, 202512, 202601...
Fetching issue 202602... Found 4 study weeks

New weeks found:
  - April 6-12
  - April 13-19

Added 4 new week(s) to watchtower_study_mp3s.csv
```

### CSV Format
```csv
Study Week,MP3 URL
November 10-16,https://cfp2.jw-cdn.org/a/.../w_E_202509_01.mp3
November 17-23,https://cfp2.jw-cdn.org/a/.../w_E_202509_02.mp3
```

### Configuration
```python
CSV_FILE = Path(__file__).parent.parent / "watchtower_study_mp3s.csv"
MONTHS_TO_FETCH = 6  # Look ahead 6 months
```

---

## Script 3: build_subsections_csv.py

### Purpose
Generates a complete CSV with individual MP3s for each Bible reading chapter and CBS lesson.

### What It Does
- Uses predefined meeting schedule data (`meeting_schedule_data.py`)
- Fetches Bible chapter MP3s from the Bible itself
- Fetches CBS lesson MP3s from "Enjoy Life Forever" book
- Creates individual rows for each chapter and lesson

### Usage

```bash
python3 scripts/build_subsections_csv.py
```

### Output Example
```
Building Meeting Subsections CSV
============================================================

Fetching CBS lessons...
  Loaded 118 lessons

Processing: November 17-23
  Fetching Bible book 22...
    Found 8 chapters
  ✓ Song of Solomon 6
  ✓ Song of Solomon 7
  ✓ Song of Solomon 8
  ✓ Lesson 36
  ✓ Lesson 37

============================================================
✓ Created: meeting_subsections_mp3s.csv
✓ Total rows: 110
✓ Weeks processed: 26
```

### CSV Format
```csv
Meeting Week,Section,Reference,MP3 URL
November 17-23,Bible Reading,Song of Solomon 6,https://...bi12_22_Ca_E_06.mp3
November 17-23,Bible Reading,Song of Solomon 7,https://...bi12_22_Ca_E_07.mp3
November 17-23,Bible Reading,Song of Solomon 8,https://...bi12_22_Ca_E_08.mp3
November 17-23,Congregation Bible Study,Lesson 36,https://...lfb_E_037.mp3
November 17-23,Congregation Bible Study,Lesson 37,https://...lfb_E_038.mp3
```

### Data Source
The script uses `meeting_schedule_data.py` which contains the mapping of weeks to Bible readings and CBS lessons. To add new weeks:

1. Edit `scripts/meeting_schedule_data.py`
2. Add new entries to `MEETING_SCHEDULE` dictionary:

```python
"May 4-10": {
    "bible": ("Isaiah", [56, 57]),
    "lessons": [80, 81]
},
```

3. Run the script again

---

## Common Features

### Duplicate Prevention
All scripts use smart comparison to avoid duplicates:
- Normalizes week strings (handles different dash types: -, –, —)
- Removes zero-width spaces and invisible characters
- Only appends truly new weeks

### Error Handling
- Gracefully handles 404 errors for future issues not yet published
- Continues processing if one month fails
- Reports which issues succeeded/failed

### No Dependencies
All scripts use only Python standard library:
- `urllib` for HTTP requests
- `json` for API responses
- `csv` for file I/O
- No external packages needed

---

## Recommended Workflow

### Initial Setup
```bash
# Generate all CSVs from scratch
python3 scripts/build_subsections_csv.py
python3 scripts/update_meeting_workbook.py
python3 scripts/update_watchtower_study.py
```

### Regular Updates (Weekly or Monthly)
```bash
# Check for new weeks and update
python3 scripts/update_meeting_workbook.py
python3 scripts/update_watchtower_study.py

# Rebuild subsections if new months added
python3 scripts/build_subsections_csv.py
```

### Automation with Cron
Add to crontab (`crontab -e`):

```bash
# Update every Monday at 9 AM
0 9 * * 1 cd /Users/mfarace/ClaudeProjects/AndroidApps && /usr/bin/python3 scripts/update_meeting_workbook.py
0 9 * * 1 cd /Users/mfarace/ClaudeProjects/AndroidApps && /usr/bin/python3 scripts/update_watchtower_study.py

# Rebuild subsections monthly (first Monday at 10 AM)
0 10 1-7 * 1 cd /Users/mfarace/ClaudeProjects/AndroidApps && /usr/bin/python3 scripts/build_subsections_csv.py
```

---

## Understanding the Data

### Meeting Workbook Structure
A weekly meeting workbook MP3 contains:
- Opening song and comments
- Treasures from God's Word
- **Bible Reading** (individual section)
- Apply Yourself to the Field Ministry
- Living as Christians
- **Congregation Bible Study** (individual section)
- Concluding song

The full MP3 has all these combined. The subsections CSV provides access to just the Bible Reading and CBS portions as individual files.

### Watchtower Study Articles
Published monthly, each issue contains 4-5 study articles for specific weeks. The script extracts only the study articles (which have assigned study weeks) and ignores other content.

### Bible Reading & CBS Lessons
- **Bible chapters**: Dramatic Bible readings from the NWT
- **CBS lessons**: From the "Enjoy Life Forever" interactive Bible course book
- Both are separate publications with their own MP3s

---

## Troubleshooting

### "No new weeks found"
✓ This is normal - means your CSV is up to date

### "HTTP Error 404"
✓ Normal - that issue hasn't been published yet

### Script runs but finds 0 sections
- Check `meeting_schedule_data.py` has entries for those weeks
- Verify Bible book names match exactly: "Song of Solomon", "Isaiah"

### Week names don't match
- The scripts normalize dashes and spaces automatically
- As long as the basic date range matches, it will work

---

## File Locations

```
AndroidApps/
├── meeting_workbook_mp3s.csv          # Output: Full meetings
├── watchtower_study_mp3s.csv          # Output: Study articles
├── meeting_subsections_mp3s.csv       # Output: Individual sections
└── scripts/
    ├── update_meeting_workbook.py     # Script 1
    ├── update_watchtower_study.py     # Script 2
    ├── build_subsections_csv.py       # Script 3
    ├── meeting_schedule_data.py       # Data for subsections
    └── README.md                      # This file
```

---

## API Endpoints Used

All scripts fetch from jw.org's official media API:

```
https://b.jw-cdn.org/apis/pub-media/GETPUBMEDIALINKS
```

**Parameters:**
- `pub`: Publication code (mwb, w, bi12, lfb)
- `issue`: Issue code (YYYYMM format)
- `fileformat`: MP3
- `output`: json
- `langwritten`: E (English)

**Publications:**
- `mwb`: Meeting Workbook
- `w`: Watchtower (study edition)
- `bi12`: Bible (New World Translation)
- `lfb`: "Enjoy Life Forever" book

---

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Verify your Python version: `python3 --version` (need 3.6+)
3. Check jw.org is accessible: `curl https://www.jw.org`
4. Review script output for specific error messages

## License

These scripts are provided as-is for personal use with jw.org content.
