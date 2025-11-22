#!/usr/bin/env python3
"""
Build meeting subsections CSV using predefined schedule data
Fast and reliable generation of Bible Reading and CBS MP3 URLs
"""

import csv
import json
from urllib.request import urlopen
from urllib.parse import urlencode
from pathlib import Path
from meeting_schedule_data import MEETING_SCHEDULE

# Configuration
OUTPUT_CSV = Path(__file__).parent.parent / "meeting_subsections_mp3s.csv"
BASE_URL = "https://b.jw-cdn.org/apis/pub-media/GETPUBMEDIALINKS"

# Bible book to number mapping
BIBLE_BOOKS = {
    'Song of Solomon': 22,
    'Isaiah': 23,
}

# Caches
BIBLE_CACHE = {}
LESSON_CACHE = {}


def fetch_json(url):
    """Fetch JSON from URL"""
    with urlopen(url, timeout=15) as response:
        return json.loads(response.read().decode('utf-8'))


def get_bible_mp3s(book_num):
    """Fetch all chapter MP3s for a Bible book"""
    if book_num in BIBLE_CACHE:
        return BIBLE_CACHE[book_num]

    print(f"  Fetching Bible book {book_num}...")

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
                    mp3_url = item.get('file', {}).get('url', '')
                    # Try to extract chapter from title or URL
                    title = item.get('title', '')
                    # Title format is usually just the chapter number
                    try:
                        chapter_num = int(title)
                        chapters[chapter_num] = mp3_url
                    except:
                        # Try extracting from URL
                        import re
                        match = re.search(r'_(\d+)\.mp3', mp3_url)
                        if match:
                            chapter_num = int(match.group(1))
                            chapters[chapter_num] = mp3_url

    BIBLE_CACHE[book_num] = chapters
    print(f"    Found {len(chapters)} chapters")
    return chapters


def get_lesson_mp3s():
    """Fetch all CBS lesson MP3s"""
    if LESSON_CACHE:
        return LESSON_CACHE

    print("Fetching CBS lessons...")

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
                        LESSON_CACHE[idx] = mp3_url

    print(f"  Loaded {len(LESSON_CACHE)} lessons\n")
    return LESSON_CACHE


def build_csv():
    """Build CSV from schedule data"""
    print("=" * 60)
    print("Building Meeting Subsections CSV")
    print("=" * 60)
    print()

    # Fetch lesson data
    get_lesson_mp3s()

    all_rows = []

    for week, data in MEETING_SCHEDULE.items():
        print(f"Processing: {week}")

        # Add Bible readings
        if 'bible' in data and data['bible']:
            book_name, chapters = data['bible']
            book_num = BIBLE_BOOKS.get(book_name)

            if book_num:
                chapter_mp3s = get_bible_mp3s(book_num)

                for chapter in chapters:
                    if chapter in chapter_mp3s:
                        all_rows.append({
                            'week': week,
                            'section': 'Bible Reading',
                            'reference': f"{book_name} {chapter}",
                            'url': chapter_mp3s[chapter]
                        })
                        print(f"  ✓ {book_name} {chapter}")

        # Add CBS lessons
        if 'lessons' in data:
            for lesson_num in data['lessons']:
                if lesson_num in LESSON_CACHE:
                    all_rows.append({
                        'week': week,
                        'section': 'Congregation Bible Study',
                        'reference': f"Lesson {lesson_num}",
                        'url': LESSON_CACHE[lesson_num]
                    })
                    print(f"  ✓ Lesson {lesson_num}")

        print()

    # Write CSV
    OUTPUT_CSV.parent.mkdir(parents=True, exist_ok=True)

    with open(OUTPUT_CSV, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['Meeting Week', 'Section', 'Reference', 'MP3 URL'])

        for row in all_rows:
            writer.writerow([row['week'], row['section'], row['reference'], row['url']])

    print("=" * 60)
    print(f"✓ Created: {OUTPUT_CSV}")
    print(f"✓ Total rows: {len(all_rows)}")
    print(f"✓ Weeks processed: {len(MEETING_SCHEDULE)}")
    print("=" * 60)


if __name__ == '__main__':
    build_csv()
