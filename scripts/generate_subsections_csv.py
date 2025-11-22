#!/usr/bin/env python3
"""
Generate complete meeting subsections CSV with Bible Reading and CBS MP3s
Matches all weeks from the meeting workbook CSV
"""

import csv
import json
import re
from urllib.request import urlopen
from urllib.parse import urlencode
from urllib.error import HTTPError
from pathlib import Path

# Configuration
WORKBOOK_CSV = Path(__file__).parent.parent / "meeting_workbook_mp3s.csv"
OUTPUT_CSV = Path(__file__).parent.parent / "meeting_subsections_mp3s.csv"
BASE_URL = "https://b.jw-cdn.org/apis/pub-media/GETPUBMEDIALINKS"

# Bible book to number mapping
BIBLE_BOOKS = {
    'Genesis': 1, 'Exodus': 2, 'Leviticus': 3, 'Numbers': 4, 'Deuteronomy': 5,
    'Joshua': 6, 'Judges': 7, 'Ruth': 8, '1 Samuel': 9, '2 Samuel': 10,
    '1 Kings': 11, '2 Kings': 12, '1 Chronicles': 13, '2 Chronicles': 14,
    'Ezra': 15, 'Nehemiah': 16, 'Esther': 17, 'Job': 18, 'Psalms': 19,
    'Proverbs': 20, 'Ecclesiastes': 21, 'Song of Solomon': 22, 'Isaiah': 23,
    'Jeremiah': 24, 'Lamentations': 25, 'Ezekiel': 26, 'Daniel': 27,
    'Hosea': 28, 'Joel': 29, 'Amos': 30, 'Obadiah': 31, 'Jonah': 32,
    'Micah': 33, 'Nahum': 34, 'Habakkuk': 35, 'Zephaniah': 36, 'Haggai': 37,
    'Zechariah': 38, 'Malachi': 39, 'Matthew': 40, 'Mark': 41, 'Luke': 42,
    'John': 43, 'Acts': 44, 'Romans': 45, '1 Corinthians': 46, '2 Corinthians': 47,
    'Galatians': 48, 'Ephesians': 49, 'Philippians': 50, 'Colossians': 51,
    '1 Thessalonians': 52, '2 Thessalonians': 53, '1 Timothy': 54, '2 Timothy': 55,
    'Titus': 56, 'Philemon': 57, 'Hebrews': 58, 'James': 59, '1 Peter': 60,
    '2 Peter': 61, '1 John': 62, '2 John': 63, '3 John': 64, 'Jude': 65,
    'Revelation': 66
}

# Cache for Bible and lesson MP3s
BIBLE_CACHE = {}
LESSON_CACHE = {}


def normalize_text(text):
    """Remove zero-width characters and normalize dashes"""
    # Remove zero-width spaces and other invisible chars
    normalized = text.replace('\u200b', '').replace('\u200c', '').replace('\u200d', '')
    # Normalize dashes
    normalized = normalized.replace('–', '-').replace('—', '-')
    return normalized.strip()


def fetch_json(url):
    """Fetch JSON from URL"""
    try:
        with urlopen(url, timeout=15) as response:
            return json.loads(response.read().decode('utf-8'))
    except Exception as e:
        print(f"  Error fetching JSON: {e}")
        return None


def fetch_html(url):
    """Fetch HTML from URL"""
    try:
        with urlopen(url, timeout=15) as response:
            return response.read().decode('utf-8')
    except Exception as e:
        return None


def get_bible_mp3s(book_num):
    """Fetch all chapter MP3s for a Bible book"""
    if book_num in BIBLE_CACHE:
        return BIBLE_CACHE[book_num]

    params = {
        'pub': 'bi12',
        'fileformat': 'MP3',
        'booknum': str(book_num),
        'output': 'json',
        'langwritten': 'E'
    }

    url = f"{BASE_URL}?{urlencode(params)}"
    data = fetch_json(url)

    chapters = {}
    if data and 'files' in data:
        for lang_data in data['files'].values():
            if 'MP3' in lang_data:
                for item in lang_data['MP3']:
                    title = item.get('title', '')
                    mp3_url = item.get('file', {}).get('url', '')
                    # Extract chapter number from title
                    match = re.search(r'(\d+)', title)
                    if match and mp3_url:
                        chapter_num = int(match.group(1))
                        chapters[chapter_num] = mp3_url

    BIBLE_CACHE[book_num] = chapters
    return chapters


def get_lesson_mp3s():
    """Fetch all CBS lesson MP3s"""
    if LESSON_CACHE:
        return LESSON_CACHE

    params = {
        'pub': 'lfb',
        'fileformat': 'MP3',
        'output': 'json',
        'langwritten': 'E'
    }

    url = f"{BASE_URL}?{urlencode(params)}"
    data = fetch_json(url)

    if data and 'files' in data:
        for lang_data in data['files'].values():
            if 'MP3' in lang_data:
                for idx, item in enumerate(lang_data['MP3']):
                    mp3_url = item.get('file', {}).get('url', '')
                    if mp3_url:
                        # Index is the lesson number
                        LESSON_CACHE[idx] = mp3_url

    return LESSON_CACHE


def parse_week_content(week_name):
    """Fetch and parse a week's meeting content"""
    # Normalize week name for URL
    week_normalized = normalize_text(week_name)
    week_url = week_normalized.replace(' ', '-')

    # Determine year and workbook issue
    if 'November' in week_name or 'December' in week_name:
        year = '2025'
        if 'December 29' in week_name or 'January 4' in week_name:
            year = '2025'  # This week spans years
        workbook = 'november-december-2025-mwb'
    elif 'January' in week_name or 'February' in week_name:
        year = '2026'
        workbook = 'january-february-2026-mwb'
    elif 'March' in week_name or 'April' in week_name or 'May' in week_name:
        year = '2026'
        workbook = 'march-april-2026-mwb'
    else:
        return None, None

    url = f"https://www.jw.org/en/library/jw-meeting-workbook/{workbook}/Life-and-Ministry-Meeting-Schedule-for-{week_url}-{year}/"

    html = fetch_html(url)
    if not html:
        return None, None

    # Extract Bible reading info
    bible_chapters = []
    # Pattern: Book Chapter:Verses (may span multiple chapters)
    # Examples: "Song of Solomon 6:1–7:13", "Isaiah 1:1-31"

    # Look for Bible book name followed by chapter references
    book_pattern = r'((?:[12] )?[A-Z][a-z]+(?: [A-Z][a-z]+)*)\s+(\d+)(?::(\d+))?(?:[–-](\d+))?(?::(\d+))?'
    matches = re.findall(book_pattern, html)

    bible_book = None
    start_chapter = None
    end_chapter = None

    for match in matches:
        book = match[0].strip()
        if book in BIBLE_BOOKS:
            bible_book = book
            start_ch = int(match[1])
            # Check if it spans chapters
            if match[3]:  # Has a dash followed by another number
                end_ch = int(match[3])
            else:
                end_ch = start_ch

            bible_chapters = list(range(start_ch, end_ch + 1))
            break

    # Extract CBS lesson info
    lessons = []
    # Pattern: "lfb lesson 36" or "lessons 36-37" or "lesson 36, intro to section 7, and lesson 37"
    lesson_pattern = r'lfb[^<]*?lesson[s]?\s+(\d+)(?:\s*[,\-]\s*(?:and\s+)?(?:lesson\s+)?(\d+))?'
    lesson_matches = re.findall(lesson_pattern, html, re.IGNORECASE)

    for match in lesson_matches:
        lesson1 = int(match[0])
        lessons.append(lesson1)
        if match[1]:
            lesson2 = int(match[1])
            # If they're sequential, add all in between
            if lesson2 > lesson1:
                for l in range(lesson1 + 1, lesson2 + 1):
                    if l not in lessons:
                        lessons.append(l)
            else:
                if lesson2 not in lessons:
                    lessons.append(lesson2)

    return (bible_book, bible_chapters), lessons


def generate_csv():
    """Generate complete CSV with all subsections"""
    print("Generating meeting subsections CSV...")
    print("=" * 60)

    # Read existing workbook weeks
    weeks = []
    with open(WORKBOOK_CSV, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            weeks.append(row['Meeting Week'])

    print(f"Found {len(weeks)} weeks to process\n")

    # Fetch Bible and lesson data
    print("Fetching CBS lesson MP3s...")
    get_lesson_mp3s()
    print(f"  Loaded {len(LESSON_CACHE)} lessons\n")

    all_rows = []
    successful = 0
    failed = 0

    for week in weeks:
        print(f"Processing: {week}")

        # Parse week content
        bible_info, lessons = parse_week_content(week)

        if bible_info and bible_info[0]:
            bible_book, chapters = bible_info

            if bible_book and chapters:
                # Fetch Bible MP3s for this book
                book_num = BIBLE_BOOKS[bible_book]
                chapter_mp3s = get_bible_mp3s(book_num)

                # Add row for each chapter
                for chapter in chapters:
                    if chapter in chapter_mp3s:
                        all_rows.append({
                            'week': week,
                            'section': 'Bible Reading',
                            'reference': f"{bible_book} {chapter}",
                            'url': chapter_mp3s[chapter]
                        })
                        print(f"  ✓ {bible_book} {chapter}")

        if lessons:
            # Add row for each lesson
            for lesson_num in lessons:
                if lesson_num in LESSON_CACHE:
                    all_rows.append({
                        'week': week,
                        'section': 'Congregation Bible Study',
                        'reference': f"Lesson {lesson_num}",
                        'url': LESSON_CACHE[lesson_num]
                    })
                    print(f"  ✓ Lesson {lesson_num}")

        if (bible_info and bible_info[0]) or lessons:
            successful += 1
            chapter_count = len(bible_info[1]) if bible_info and bible_info[0] else 0
            print(f"  Found {chapter_count} chapters, {len(lessons)} lessons")
        else:
            failed += 1
            print(f"  ✗ Could not parse content")

        print()

    # Write CSV
    OUTPUT_CSV.parent.mkdir(parents=True, exist_ok=True)

    with open(OUTPUT_CSV, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['Meeting Week', 'Section', 'Reference', 'MP3 URL'])

        for row in all_rows:
            writer.writerow([row['week'], row['section'], row['reference'], row['url']])

    print("=" * 60)
    print(f"✓ Created {OUTPUT_CSV}")
    print(f"✓ Total rows: {len(all_rows)}")
    print(f"✓ Successful weeks: {successful}/{len(weeks)}")
    print(f"✗ Failed weeks: {failed}/{len(weeks)}")


if __name__ == '__main__':
    generate_csv()
