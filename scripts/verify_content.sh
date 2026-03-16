#!/bin/bash
# Runtime content verification — triggers the ContentSyncWorker via WorkManager
# and asserts the correct CBS and bible reading filenames appear in logcat.
#
# Usage: ./scripts/verify_content.sh [--date YYYY-MM-DD]
# - If --date is provided, the script filters log lines for that weekStart date.
# - Run after installing the APK. Requires ADB and a connected device.

set -e

FILTER_DATE=""
if [ "$1" = "--date" ] && [ -n "${2:-}" ]; then
  FILTER_DATE="$2"
fi

PACKAGE="org.jw.library.auto"
SERVICE=".service.JWLibraryAutoService"
WAIT_SECS=15

echo "=== JW Library Auto — Runtime Content Verification ==="

# Clear existing logcat
adb logcat -c
echo "Logcat cleared."

# Force-stop any running instance then restart clean
adb shell am force-stop "$PACKAGE"
sleep 1

# Start the MediaBrowserService directly — this triggers onCreate which
# schedules the immediate ContentSyncWorker, which calls getCongregationStudyUrls
adb shell am startservice \
    -n "${PACKAGE}/${SERVICE}" \
    -a "android.media.browse.MediaBrowserService" 2>/dev/null || true

echo "Service started. Waiting ${WAIT_SECS}s for ContentSyncWorker to load content..."
sleep "$WAIT_SECS"

# Capture all logcat from our package (CONTENT_CHECK lines are Log.i)
if [ -n "$FILTER_DATE" ]; then
  LOGCAT=$(adb logcat -d 2>/dev/null | grep "CONTENT_CHECK" | grep "$FILTER_DATE" || true)
else
  LOGCAT=$(adb logcat -d 2>/dev/null | grep "CONTENT_CHECK" || true)
fi

if [ -z "$LOGCAT" ]; then
    echo ""
    echo "CONTENT_CHECK lines not found — dumping last 50 lines from our process for diagnosis:"
    adb logcat -d 2>/dev/null | grep -i "$PACKAGE\|JWOrgRepo\|ContentSync\|CONTENT_CHECK" | tail -50
    echo ""
    echo "FAIL: No CONTENT_CHECK log lines found. WorkManager sync may not have run yet."
    exit 1
fi

echo ""
echo "=== Resolved URLs (with workbook labels) ==="
echo "$LOGCAT"
echo ""

CBS_LINE=$(echo "$LOGCAT" | grep "congregation_study" | tail -1)
BIBLE_LINE=$(echo "$LOGCAT" | grep "bible_reading" | tail -1)

FAIL=0

# CBS must NOT be lesson 57 (the known wrong lesson)
if echo "$CBS_LINE" | grep -q "lfb_E_057"; then
    echo "FAIL: CBS is still resolving lfb_E_057 (wrong — expected 068+ for current week)"
    FAIL=1
fi

# CBS must be lesson 068 or higher
if [ -n "$CBS_LINE" ] && ! echo "$CBS_LINE" | grep -qE "lfb_E_0[6-9][0-9]|lfb_E_[1-9][0-9]{2}"; then
    echo "FAIL: CBS lesson number too low — expected 068+ for current week"
    echo "      Got: $CBS_LINE"
    FAIL=1
fi

# Bible must NOT be Isa_E_40 or earlier (the known wrong chapters)
if echo "$BIBLE_LINE" | grep -qE "Isa_E_(0[0-9]|[1-3][0-9]|40)\.mp3"; then
    echo "FAIL: Bible reading resolved a chapter too early for current week"
    echo "      Got: $BIBLE_LINE"
    FAIL=1
fi

if [ $FAIL -eq 0 ]; then
    echo "PASS: CBS and bible reading URLs are correct for the current week."
    echo "  CBS:   $CBS_LINE"
    echo "  Bible: $BIBLE_LINE"
fi

exit $FAIL
