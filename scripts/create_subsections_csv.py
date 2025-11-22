#!/usr/bin/env python3
"""
Create CSV with Bible Reading and Congregation Bible Study MP3 URLs
Uses the meeting schedule data to map weeks to their audio sections
"""

import csv
from pathlib import Path

CSV_FILE = Path(__file__).parent.parent / "meeting_subsections_mp3s.csv"

# Known Bible book chapters for each week (from meeting workbook)
# Format: week: (book, chapter, lesson_start, lesson_end)
WEEK_DATA = {
    # November-December 2025 (based on actual meeting workbook)
    "November 3-9": ("Song of Solomon", 2, 32, 33),
    "November 10-16": ("Song of Solomon", 3, 34, 35),
    "November 17-23": ("Song of Solomon", 6, 36, 37),
    "November 24-30": ("Song of Solomon", 7, 38, 39),
    "December 1-7": ("Isaiah", 1, 40, 41),
    "December 8-14": ("Isaiah", 2, 42, 43),
    "December 15-21": ("Isaiah", 5, 44, 44),
    "December 22-28": ("Isaiah", 7, 45, 46),
    "December 29-January 4": ("Isaiah", 9, 47, 48),
    # January-February 2026
    "January 5-11": ("Isaiah", 11, 49, 50),
    "January 12-18": ("Isaiah", 14, 51, 52),
    "January 19-25": ("Isaiah", 17, 53, 53),
    "January 26-February 1": ("Isaiah", 22, 54, 54),
    "February 2-8": ("Isaiah", 24, 55, 56),
    "February 9-15": ("Isaiah", 26, 57, 58),
    "February 16-22": ("Isaiah", 29, 59, 60),
    "February 23-March 1": ("Isaiah", 32, 61, 61),
    # March-April 2026
    "March 2-8": ("Isaiah", 35, 62, 63),
    "March 9-15": ("Isaiah", 37, 64, 65),
    "March 16-22": ("Isaiah", 40, 66, 67),
    "March 23-29": ("Isaiah", 41, 68, 69),
    "March 30-April 5": ("Isaiah", 43, 70, 71),
    "April 6-12": ("Isaiah", 44, 72, 73),
    "April 13-19": ("Isaiah", 48, 74, 75),
    "April 20-26": ("Isaiah", 49, 76, 77),
    "April 27-May 3": ("Isaiah", 51, 78, 79),
}

# Bible book to number mapping
BIBLE_BOOKS = {
    'Song of Solomon': 22,
    'Isaiah': 23,
}

# Lesson to file mapping (from the API response)
LESSON_TO_FILE = {
    32: "lfb_E_039.mp3",
    33: "lfb_E_040.mp3",
    34: "lfb_E_041.mp3",
    35: "lfb_E_042.mp3",
    36: "lfb_E_043.mp3",
    37: "lfb_E_044.mp3",
    38: "lfb_E_045.mp3",
    39: "lfb_E_047.mp3",
    40: "lfb_E_048.mp3",
    41: "lfb_E_049.mp3",
    42: "lfb_E_050.mp3",
    43: "lfb_E_051.mp3",
    44: "lfb_E_053.mp3",
    45: "lfb_E_054.mp3",
    46: "lfb_E_055.mp3",
    47: "lfb_E_056.mp3",
    48: "lfb_E_057.mp3",
    49: "lfb_E_058.mp3",
    50: "lfb_E_059.mp3",
    51: "lfb_E_061.mp3",
    52: "lfb_E_062.mp3",
    53: "lfb_E_063.mp3",
    54: "lfb_E_064.mp3",
    55: "lfb_E_065.mp3",
    56: "lfb_E_066.mp3",
    57: "lfb_E_067.mp3",
    58: "lfb_E_068.mp3",
    59: "lfb_E_070.mp3",
    60: "lfb_E_071.mp3",
    61: "lfb_E_072.mp3",
    62: "lfb_E_073.mp3",
    63: "lfb_E_074.mp3",
    64: "lfb_E_075.mp3",
    65: "lfb_E_076.mp3",
    66: "lfb_E_077.mp3",
    67: "lfb_E_079.mp3",
    68: "lfb_E_080.mp3",
    69: "lfb_E_081.mp3",
    70: "lfb_E_082.mp3",
    71: "lfb_E_083.mp3",
    72: "lfb_E_084.mp3",
    73: "lfb_E_085.mp3",
    74: "lfb_E_086.mp3",
    75: "lfb_E_087.mp3",
    76: "lfb_E_088.mp3",
    77: "lfb_E_089.mp3",
    78: "lfb_E_090.mp3",
    79: "lfb_E_091.mp3",
}


def get_bible_url(book_name, chapter):
    """Generate Bible reading MP3 URL"""
    book_num = BIBLE_BOOKS.get(book_name)
    if not book_num:
        return None

    chapter_str = str(chapter).zfill(2)
    book_str = str(book_num).zfill(2)

    return f"https://cfp2.jw-cdn.org/a/bi12_{book_str}_Ca_E_{chapter_str}.mp3"


def get_lesson_url(lesson_num):
    """Generate Congregation Bible Study lesson MP3 URL"""
    filename = LESSON_TO_FILE.get(lesson_num)
    if not filename:
        return None

    return f"https://cfp2.jw-cdn.org/a/{filename.replace('lfb_E_', '').replace('.mp3', '')}/1/o/{filename}"


def create_csv():
    """Create CSV with all sub-sections"""
    rows = []

    for week, (book, chapter, lesson_start, lesson_end) in WEEK_DATA.items():
        # Add Bible reading
        bible_url = get_bible_url(book, chapter)
        if bible_url:
            rows.append({
                'week': week,
                'section': 'Bible Reading',
                'reference': f"{book} {chapter}",
                'url': bible_url
            })

        # Add CBS lessons
        for lesson_num in range(lesson_start, lesson_end + 1):
            lesson_url = get_lesson_url(lesson_num)
            if lesson_url:
                rows.append({
                    'week': week,
                    'section': 'Congregation Bible Study',
                    'reference': f"Lesson {lesson_num}",
                    'url': lesson_url
                })

    # Write CSV
    CSV_FILE.parent.mkdir(parents=True, exist_ok=True)

    with open(CSV_FILE, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['Meeting Week', 'Section', 'Reference', 'MP3 URL'])

        for row in rows:
            writer.writerow([row['week'], row['section'], row['reference'], row['url']])

    print(f"✓ Created {CSV_FILE}")
    print(f"✓ Added {len(rows)} sections across {len(WEEK_DATA)} weeks")


if __name__ == '__main__':
    create_csv()
