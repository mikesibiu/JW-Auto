#!/usr/bin/env python3
"""
Fetch individual section MP3 URLs for each meeting workbook week
Extracts Bible Reading and Congregation Bible Study MP3s
"""

import csv
import json
import re
from urllib.request import urlopen
from urllib.parse import urlencode, quote
from urllib.error import HTTPError
from pathlib import Path

# Configuration
CSV_FILE = Path(__file__).parent.parent / "meeting_subsections_mp3s.csv"
BASE_URL = "https://b.jw-cdn.org/apis/pub-media/GETPUBMEDIALINKS"
MONTHS_TO_FETCH = 6

# Bible book name to number mapping
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

# Cache for lesson mappings
LESSON_CACHE = {}


def fetch_json(url):
    """Fetch JSON data from URL"""
    try:
        with urlopen(url, timeout=15) as response:
            data = response.read().decode('utf-8')
            return json.loads(data)
    except HTTPError as e:
        if e.code == 404:
            return None
        print(f"Warning: HTTP error {e.code} for {url}")
        return None
    except Exception as e:
        print(f"Error fetching {url}: {e}")
        return None


def fetch_html(url):
    """Fetch HTML content from URL"""
    try:
        with urlopen(url, timeout=15) as response:
            return response.read().decode('utf-8')
    except Exception as e:
        print(f"Error fetching {url}: {e}")
        return None


def get_lesson_mapping():
    """Fetch and cache lesson number to MP3 URL mapping"""
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

    if not data or 'files' not in data:
        return {}

    # Extract lesson mappings
    for lang_data in data['files'].values():
        if 'MP3' in lang_data:
            for idx, item in enumerate(lang_data['MP3']):
                mp3_url = item.get('file', {}).get('url', '')
                if mp3_url:
                    # Lesson numbers start at 0 in the API
                    LESSON_CACHE[idx] = mp3_url

    return LESSON_CACHE


def get_bible_chapter_url(book_name, chapter):
    """Get MP3 URL for a Bible chapter"""
    book_num = BIBLE_BOOKS.get(book_name)
    if not book_num:
        return None

    # Format: bi12_{booknum}_Ca_E_{chapter}.mp3
    # Note: book number needs to be zero-padded to 2 digits, chapter doesn't
    chapter_str = str(chapter).zfill(2)
    book_str = str(book_num).zfill(2)

    # Construct URL - this is the known pattern for Bible audio
    return f"https://cfp2.jw-cdn.org/a/bi12_{book_str}_Ca_E_{chapter_str}.mp3"


def parse_week_page(html_content, week_name):
    """Parse a week's HTML page to extract Bible reading and CBS references"""
    if not html_content:
        return []

    sections = []

    # Extract Bible reading - look for patterns like "Song of Solomon 2:1-17"
    # The pattern is typically: Book Chapter:Verses
    bible_pattern = r'((?:[12] )?[A-Z][a-z]+(?: [A-Z][a-z]+)*)\s+(\d+):(\d+(?:-\d+)?)'
    bible_matches = re.findall(bible_pattern, html_content)

    if bible_matches:
        # Get the first match (main Bible reading)
        book, chapter, verses = bible_matches[0]
        book = book.strip()
        chapter_num = int(chapter)

        # Get MP3 URL for this chapter
        mp3_url = get_bible_chapter_url(book, chapter_num)
        if mp3_url:
            sections.append({
                'week': week_name,
                'section': 'Bible Reading',
                'reference': f"{book} {chapter}:{verses}",
                'url': mp3_url
            })

    # Extract Congregation Bible Study lesson references
    # Look for patterns like "lfb lesson 32" or "lessons 32-33"
    cbs_pattern = r'lfb["\s]+(?:lesson|lessons)\s+(\d+)(?:\s*[,\-]\s*(\d+))?'
    cbs_matches = re.findall(cbs_pattern, html_content, re.IGNORECASE)

    lesson_map = get_lesson_mapping()

    if cbs_matches:
        for match in cbs_matches:
            lesson1 = int(match[0])
            lesson2 = int(match[1]) if match[1] else lesson1

            for lesson_num in range(lesson1, lesson2 + 1):
                mp3_url = lesson_map.get(lesson_num)
                if mp3_url:
                    sections.append({
                        'week': week_name,
                        'section': 'Congregation Bible Study',
                        'reference': f"Lesson {lesson_num}",
                        'url': mp3_url
                    })

    return sections


def fetch_meeting_weeks():
    """Fetch all meeting weeks from the API"""
    from datetime import datetime, timedelta

    weeks = []
    today = datetime.now()

    # Generate issue codes
    for i in range(MONTHS_TO_FETCH):
        date = today + timedelta(days=30 * i)
        issue_code = date.strftime('%Y%m')

        params = {
            'issue': issue_code,
            'output': 'json',
            'pub': 'mwb',
            'fileformat': 'MP3',
            'alllangs': '0',
            'langwritten': 'E',
            'txtCMSLang': 'E'
        }

        url = f"{BASE_URL}?{urlencode(params)}"
        data = fetch_json(url)

        if data and 'files' in data:
            for lang_data in data['files'].values():
                if 'MP3' in lang_data:
                    for item in lang_data['MP3']:
                        title = item.get('title', '')
                        # Only weekly items (have date ranges)
                        if '-' in title or '–' in title:
                            weeks.append(title)

    return weeks


def fetch_all_subsections():
    """Fetch all sub-section MP3s for all meeting weeks"""
    print("Fetching lesson mappings...")
    get_lesson_mapping()
    print(f"Loaded {len(LESSON_CACHE)} lessons")

    print("\nFetching meeting weeks...")
    weeks = fetch_meeting_weeks()
    print(f"Found {len(weeks)} weeks")

    all_sections = []

    for week in weeks:
        print(f"\nProcessing week: {week}")

        # Convert week name to URL-friendly format
        week_url = week.replace(' ', '-').replace('–', '-')

        # Construct URL to the week's page
        # Pattern: Life-and-Ministry-Meeting-Schedule-for-November-17-23-2025
        year = "2025" if "November" in week or "December" in week else "2026"
        url_path = f"Life-and-Ministry-Meeting-Schedule-for-{week_url}-{year}"

        # Determine which workbook issue
        if "November" in week or "December" in week:
            workbook_issue = "november-december-2025-mwb"
        elif "January" in week or "February" in week:
            workbook_issue = "january-february-2026-mwb"
        elif "March" in week or "April" in week or "May" in week:
            workbook_issue = "march-april-2026-mwb"
        else:
            continue

        page_url = f"https://www.jw.org/en/library/jw-meeting-workbook/{workbook_issue}/{url_path}/"

        print(f"  Fetching: {page_url}")
        html = fetch_html(page_url)

        if html:
            sections = parse_week_page(html, week)
            all_sections.extend(sections)
            print(f"  Found {len(sections)} sections")
        else:
            print(f"  Could not fetch page")

    return all_sections


def write_csv(sections):
    """Write sections to CSV file"""
    if not sections:
        print("No sections to write")
        return

    CSV_FILE.parent.mkdir(parents=True, exist_ok=True)

    with open(CSV_FILE, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['Meeting Week', 'Section', 'Reference', 'MP3 URL'])

        for section in sections:
            writer.writerow([
                section['week'],
                section['section'],
                section['reference'],
                section['url']
            ])

    print(f"\n✓ Wrote {len(sections)} sections to {CSV_FILE}")


def main():
    print("=" * 60)
    print("Meeting Workbook Sub-Section MP3 Fetcher")
    print("=" * 60)

    sections = fetch_all_subsections()
    write_csv(sections)

    print("\nDone!")


if __name__ == '__main__':
    main()
