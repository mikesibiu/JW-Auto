#!/usr/bin/env python3
import csv
from collections import defaultdict
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MONTH_ORDER = {m.lower(): i for i, m in enumerate([
    "January","February","March","April","May","June","July","August","September","October","November","December"], start=1)}

def csv_path(name):
    return ROOT / name


def infer_week_start(label, current_year, last_month):
    clean = label.replace("—", "-").replace("–", "-")
    core = clean.split(",")[0].strip()
    tokens = core.split()
    month_name = tokens[0]
    month_index = MONTH_ORDER[month_name.lower()]
    if last_month is not None and month_index < last_month:
        current_year += 1
    day_token = tokens[1]
    if "-" in day_token:
        start_day = day_token.split("-")[0]
    else:
        start_day = day_token
    date_obj = datetime.strptime(f"{month_name} {start_day} {current_year}", "%B %d %Y")
    return date_obj.strftime("%Y-%m-%d"), current_year, month_index


def load_rows(path, label_field, start_year):
    rows = []
    year = start_year
    last_month = None
    with path.open() as f:
        reader = csv.DictReader(f)
        for row in reader:
            label = row[label_field].strip()
            url = row["MP3 URL"].strip()
            week_start, year, last_month = infer_week_start(label, year, last_month)
            rows.append((week_start, url))
    return rows


def load_sections(path, start_year):
    data = defaultdict(lambda: {"Bible Reading": [], "Congregation Bible Study": []})
    year = start_year
    last_month = None
    with path.open() as f:
        reader = csv.DictReader(f)
        for row in reader:
            label = row["Meeting Week"].strip()
            url = row["MP3 URL"].strip()
            section = row["Section"].strip()
            week_start, year, last_month = infer_week_start(label, year, last_month)
            data[week_start][section].append(url)
    return data


def emit_map(name, rows):
    lines = [f"private val {name} = mapOf("]
    for start, url in rows:
        lines.append(f"    \"{start}\" to \"{url}\",")
    lines.append(")\n")
    print("\n".join(lines))


def emit_sections(data):
    lines = ["private val MEETING_SECTIONS = mapOf("]
    for week_start, sections in sorted(data.items()):
        bible = sections.get("Bible Reading", [])
        study = sections.get("Congregation Bible Study", [])
        lines.append(f"    \"{week_start}\" to MeetingSections(")
        lines.append("        bibleReading = listOf(")
        for url in bible:
            lines.append(f"            \"{url}\",")
        lines.append("        ),")
        lines.append("        congregationStudy = listOf(")
        for url in study:
            lines.append(f"            \"{url}\",")
        lines.append("        )")
        lines.append("    ),")
    lines.append(")\n")
    print("\n".join(lines))


def main():
    workbook_rows = load_rows(csv_path("meeting_workbook_mp3s.csv"), "Meeting Week", 2025)
    watchtower_rows = load_rows(csv_path("watchtower_study_mp3s.csv"), "Study Week", 2025)
    sections = load_sections(csv_path("meeting_subsections_mp3s.csv"), 2025)
    emit_map("WORKBOOK_OVERRIDES", workbook_rows)
    emit_map("WATCHTOWER_OVERRIDES", watchtower_rows)
    emit_sections(sections)


if __name__ == "__main__":
    main()
