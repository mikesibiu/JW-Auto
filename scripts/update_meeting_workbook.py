#!/usr/bin/env python3
"""
Update Meeting Workbook CSV with new weeks
Fetches MP3 URLs from jw.org and appends only new weeks to the CSV
"""

import csv
import json
from datetime import datetime, timedelta
from pathlib import Path
from urllib.request import urlopen
from urllib.parse import urlencode
from urllib.error import HTTPError

# Configuration
CSV_FILE = Path(__file__).parent.parent / "meeting_workbook_mp3s.csv"
BASE_URL = "https://b.jw-cdn.org/apis/pub-media/GETPUBMEDIALINKS"
MONTHS_TO_FETCH = 6  # Fetch 6 months ahead


def normalize_week(week_str):
    """Normalize week string by replacing different dash types and removing zero-width chars"""
    # Replace all types of dashes/hyphens with regular dash
    normalized = week_str.replace('–', '-').replace('—', '-').replace('\u2013', '-').replace('\u2014', '-')
    # Remove zero-width spaces and other invisible characters
    normalized = normalized.replace('\u200b', '').replace('\u200c', '').replace('\u200d', '')
    # Remove regular spaces around dashes for consistency
    normalized = normalized.replace(' - ', '-').replace(' -', '-').replace('- ', '-')
    return normalized.strip()


def get_existing_weeks(csv_file):
    """Read existing CSV and return set of normalized week strings"""
    existing_weeks = set()
    if csv_file.exists():
        with open(csv_file, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                existing_weeks.add(normalize_week(row['Meeting Week']))
    return existing_weeks


def fetch_workbook_data(issue_code):
    """Fetch MP3 data for a specific issue from jw.org API"""
    params = {
        'issue': issue_code,
        'output': 'json',
        'pub': 'mwb',
        'fileformat': 'MP3',
        'alllangs': '0',
        'langwritten': 'E',
        'txtCMSLang': 'E'
    }

    try:
        url = f"{BASE_URL}?{urlencode(params)}"
        with urlopen(url, timeout=10) as response:
            data = response.read().decode('utf-8')
            return json.loads(data)
    except HTTPError as e:
        if e.code == 404:
            return None  # Issue not yet available
        else:
            print(f"Warning: Got HTTP error {e.code} for issue {issue_code}")
            return None
    except Exception as e:
        print(f"Error fetching issue {issue_code}: {e}")
        return None


def parse_workbook_data(data):
    """Parse JSON response and extract week/URL pairs"""
    weeks = []
    if not data or 'files' not in data:
        return weeks

    for lang_data in data['files'].values():
        if 'MP3' in lang_data:
            for item in lang_data['MP3']:
                title = item.get('title', '')
                mp3_url = item.get('file', {}).get('url', '')

                # Only include items that look like weekly content
                # (have date ranges in the title)
                if mp3_url and ('-' in title or '–' in title):
                    # Extract just the date part (e.g., "November 3-9")
                    # Titles are like "November 3-9" or similar
                    weeks.append({
                        'week': title,
                        'url': mp3_url
                    })

    return weeks


def generate_issue_codes(months_ahead):
    """Generate issue codes for current month and months ahead"""
    issue_codes = []
    today = datetime.now()

    for i in range(months_ahead):
        date = today + timedelta(days=30 * i)
        # Issue code format: YYYYMM (e.g., 202511 for November 2025)
        issue_code = date.strftime('%Y%m')
        if issue_code not in issue_codes:
            issue_codes.append(issue_code)

    return issue_codes


def update_csv(new_weeks):
    """Append new weeks to CSV file"""
    if not new_weeks:
        print("No new weeks to add")
        return

    # Ensure parent directory exists
    CSV_FILE.parent.mkdir(parents=True, exist_ok=True)

    # If CSV doesn't exist, create it with header
    if not CSV_FILE.exists():
        with open(CSV_FILE, 'w', encoding='utf-8', newline='') as f:
            writer = csv.writer(f)
            writer.writerow(['Meeting Week', 'MP3 URL'])

    # Append new weeks
    with open(CSV_FILE, 'a', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        for week_data in new_weeks:
            writer.writerow([week_data['week'], week_data['url']])

    print(f"Added {len(new_weeks)} new week(s) to {CSV_FILE}")


def main():
    print("Updating Meeting Workbook CSV...")

    # Get existing weeks
    existing_weeks = get_existing_weeks(CSV_FILE)
    print(f"Found {len(existing_weeks)} existing weeks in CSV")

    # Generate issue codes to check
    issue_codes = generate_issue_codes(MONTHS_TO_FETCH)
    print(f"Checking {len(issue_codes)} issue codes: {', '.join(issue_codes)}")

    # Fetch data for all issues
    all_weeks = []
    for issue_code in issue_codes:
        print(f"Fetching issue {issue_code}...", end=' ')
        data = fetch_workbook_data(issue_code)
        if data:
            weeks = parse_workbook_data(data)
            all_weeks.extend(weeks)
            print(f"Found {len(weeks)} weeks")
        else:
            print("Not available")

    # Filter out existing weeks (compare normalized versions)
    new_weeks = [w for w in all_weeks if normalize_week(w['week']) not in existing_weeks]

    if new_weeks:
        print(f"\nNew weeks found:")
        for week_data in new_weeks:
            print(f"  - {week_data['week']}")

        # Update CSV
        update_csv(new_weeks)
    else:
        print("\nNo new weeks found. CSV is up to date.")

    print("\nDone!")


if __name__ == '__main__':
    main()
