#!/usr/bin/env python3
"""Extract MP3 download links from jw.org meeting workbook or download pages.

This script now supports two kinds of URLs:
1. Individual week pages (with "Life-and-Ministry-Meeting-Schedule" in the path).
2. Download pages such as
   https://www.jw.org/download/?issue=202511&output=html&pub=mwb&fileformat=MP3%2CAAC&alllangs=0&langwritten=E&txtCMSLang=E&isBible=0
   which list every MP3/AAC file for a publication.

Usage:
  ./scripts/extract_jw_audio.py URL1 URL2 ...
  ./scripts/extract_jw_audio.py --from-file meeting_urls.txt

Outputs CSV (label,page_url,mp3_url).
Install deps once: python3 -m pip install requests beautifulsoup4
"""

import argparse
import csv
import json
import sys
from pathlib import Path
from typing import Iterable, List
from urllib.parse import urljoin, urlparse

import requests
from bs4 import BeautifulSoup

DOWNLOAD_HOST = "www.jw.org"


def iter_urls(args: argparse.Namespace) -> Iterable[str]:
    for url in args.urls:
        yield url
    if args.from_file:
        for line in Path(args.from_file).read_text().splitlines():
            stripped = line.strip()
            if stripped and not stripped.startswith("#"):
                yield stripped


def fetch_download_page_links(page_url: str, timeout: int = 30) -> List[str]:
    resp = requests.get(page_url, timeout=timeout)
    resp.raise_for_status()
    soup = BeautifulSoup(resp.text, "html.parser")
    mp3s = set()
    # JW download pages embed JSON inside a <script id="__NEXT_DATA__">
    script_tag = soup.find("script", id="__NEXT_DATA__")
    if script_tag:
        data = json.loads(script_tag.string)
        for item in _extract_next_data_mp3s(data):
            mp3s.add(item)
    # Fallback: scrape visible links in page
    for tag in soup.find_all("a", href=True):
        href = tag["href"]
        if href.lower().endswith(".mp3"):
            mp3s.add(urljoin(page_url, href))
    return sorted(mp3s)


def _extract_next_data_mp3s(data: dict) -> Iterable[str]:
    try:
        entries = data["props"]["pageProps"]["listData"]["files"]
    except KeyError:
        return []
    mp3s = []
    for entry in entries:
        url = entry.get("fileUrl")
        if url and url.lower().endswith(".mp3"):
            mp3s.append(url)
    return mp3s


def fetch_week_page_links(page_url: str, timeout: int = 30) -> List[str]:
    resp = requests.get(page_url, timeout=timeout)
    resp.raise_for_status()
    soup = BeautifulSoup(resp.text, "html.parser")
    mp3s = set()

    def maybe_add(raw_url: str) -> None:
        if raw_url and raw_url.lower().endswith(".mp3"):
            mp3s.add(urljoin(page_url, raw_url))

    for tag in soup.find_all("a", href=True):
        maybe_add(tag["href"])
    for source in soup.find_all("source", src=True):
        maybe_add(source["src"])
    for audio in soup.find_all("audio", src=True):
        maybe_add(audio["src"])

    return sorted(mp3s)


def label_from_url(url: str) -> str:
    path = urlparse(url).path.rstrip("/")
    if not path:
        return url
    last_segment = path.split("/")[-1]
    cleaned = last_segment.replace("-", " ")
    return cleaned.capitalize()


def is_download_page(url: str) -> bool:
    parsed = urlparse(url)
    return parsed.netloc.endswith(DOWNLOAD_HOST) and "download" in parsed.path


def main() -> int:
    parser = argparse.ArgumentParser(description="Extract JW meeting MP3 links")
    parser.add_argument("urls", nargs="*", help="Page URLs to scan")
    parser.add_argument("--from-file", dest="from_file", help="Text file containing URLs (one per line)")
    parser.add_argument("--timeout", type=int, default=30, help="HTTP timeout in seconds")
    args = parser.parse_args()

    all_urls = list(iter_urls(args))
    if not all_urls:
        parser.error("Provide at least one URL via arguments or --from-file")

    writer = csv.writer(sys.stdout)
    writer.writerow(["label", "page_url", "mp3_url"])

    for page_url in all_urls:
        try:
            if is_download_page(page_url):
                mp3s = fetch_download_page_links(page_url, timeout=args.timeout)
            else:
                mp3s = fetch_week_page_links(page_url, timeout=args.timeout)
        except Exception as exc:  # pylint: disable=broad-except
            print(f"ERROR,{page_url},{exc}", file=sys.stderr)
            continue

        if not mp3s:
            print(f"WARNING: no MP3 links found on {page_url}", file=sys.stderr)
            continue

        label = label_from_url(page_url)
        for mp3 in mp3s:
            writer.writerow([label, page_url, mp3])

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
