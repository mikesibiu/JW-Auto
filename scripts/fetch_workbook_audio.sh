#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VENV_ACTIVATE="$ROOT_DIR/.venv/bin/activate"

if [[ ! -f "$VENV_ACTIVATE" ]]; then
  cat <<MSG >&2
Virtual environment not found at $ROOT_DIR/.venv.
Create it first:
  cd $ROOT_DIR
  python3 -m venv .venv
  source .venv/bin/activate
  pip install --upgrade pip requests beautifulsoup4
MSG
  exit 1
fi

# Default issues if none provided
issues=("202511" "202601")
if [[ $# -gt 0 ]]; then
  issues=("$@")
fi

source "$VENV_ACTIVATE"

for issue in "${issues[@]}"; do
  url="https://www.jw.org/download/?issue=${issue}&output=html&pub=mwb&fileformat=MP3%2CAAC&alllangs=0&langwritten=E&txtCMSLang=E&isBible=0"
  output="$ROOT_DIR/workbook_${issue}.csv"
  echo "Fetching issue ${issue} -> $output"
  "$ROOT_DIR/scripts/extract_jw_audio.py" "$url" > "$output"
done

deactivate
