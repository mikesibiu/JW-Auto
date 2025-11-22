# Meeting Workbook Sub-Section MP3s

## Overview

Each meeting workbook week contains individual MP3 files for:
1. **Bible Reading** - Dramatic Bible reading audio for the assigned chapters
2. **Congregation Bible Study** - Audio for the "Enjoy Life Forever" book lessons

## File Structure

`meeting_subsections_mp3s.csv` contains:
- Meeting Week
- Section (Bible Reading or Congregation Bible Study)
- Reference (Book/Chapter or Lesson number)
- MP3 URL (direct download link)

## How to Find MP3 URLs

### Bible Reading URLs

Bible readings use this URL pattern:
```
https://cfp2.jw-cdn.org/a/bi12_{book}_{Ca}_E_{chapter}.mp3
```

Where:
- `{book}` = Bible book number (zero-padded, 2 digits)
  - Song of Solomon = 22
  - Isaiah = 23
  - etc.
- `{chapter}` = Chapter number (zero-padded, 2 digits)

**Example:** Song of Solomon 6 = `bi12_22_Ca_E_06.mp3`

However, the hash part (e.g., `/a/7fb7bc/1/o/`) varies per file. To get the complete URL:

```bash
# Fetch for a specific book
python3 -c "
import json
from urllib.request import urlopen
from urllib.parse import urlencode

params = {
    'pub': 'bi12',
    'fileformat': 'MP3',
    'booknum': '22',  # Song of Solomon
    'output': 'json',
    'langwritten': 'E'
}

url = 'https://b.jw-cdn.org/apis/pub-media/GETPUBMEDIALINKS?' + urlencode(params)

with urlopen(url, timeout=15) as response:
    data = json.loads(response.read().decode('utf-8'))
    for lang in data['files'].values():
        if 'MP3' in lang:
            for item in lang['MP3']:
                print(f\"{item.get('title')}: {item.get('file', {}).get('url')}\")
"
```

### Congregation Bible Study URLs

CBS lessons use the "Enjoy Life Forever" book (`lfb` publication):

```bash
# Fetch all lessons
python3 -c "
import json
from urllib.request import urlopen
from urllib.parse import urlencode

params = {
    'pub': 'lfb',
    'fileformat': 'MP3',
    'output': 'json',
    'langwritten': 'E'
}

url = 'https://b.jw-cdn.org/apis/pub-media/GETPUBMEDIALINKS?' + urlencode(params)

with urlopen(url, timeout=15) as response:
    data = json.loads(response.read().decode('utf-8'))
    for lang in data['files'].values():
        if 'MP3' in lang:
            for idx, item in enumerate(lang['MP3']):
                print(f\"Index {idx}: {item.get('title')} - {item.get('file', {}).get('url')}\")
"
```

## Finding Which References Go With Which Week

1. Visit the meeting workbook page for that week:
   ```
   https://www.jw.org/en/library/jw-meeting-workbook/[issue]/Life-and-Ministry-Meeting-Schedule-for-[Week]/
   ```

2. Look for sections with headphone icons:
   - **Bible Reading** section shows the book and chapters
   - **Congregation Bible Study** section shows the lesson number(s)

3. Use the commands above to fetch the MP3 URLs for those specific references

## Example

For week November 17-23, 2025:
- Bible Reading: Song of Solomon 6-8
- CBS: Lessons 36-37 (from "Enjoy Life Forever")

The CSV would contain 5 rows:
- 3 rows for Bible chapters 6, 7, 8
- 2 rows for CBS lessons 36, 37

## Automation

The process of mapping weeks to Bible/CBS references could be automated by:
1. Scraping each week's meeting page
2. Extracting the Bible reading chapters
3. Extracting the CBS lesson numbers
4. Fetching the corresponding MP3 URLs from the APIs

This is left as a future enhancement due to the complexity of HTML parsing across different week structures.

## Bible Book Numbers

| Book | Number |
|------|--------|
| Song of Solomon | 22 |
| Isaiah | 23 |
| Jeremiah | 24 |
| Ezekiel | 26 |
| Daniel | 27 |
| Matthew | 40 |
| Mark | 41 |
| Luke | 42 |
| John | 43 |
| Acts | 44 |
| Romans | 45 |

(Full list in the Bible book mapping in the scripts)
