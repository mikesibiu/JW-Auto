#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -z "${JAVA_HOME:-}" ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null)" || {
    echo "JDK 17 not found. Install it or set JAVA_HOME to a JDK 17 path." >&2
    exit 1
  }
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

ADB_BIN="${ADB_BIN:-adb}"
if ! command -v "$ADB_BIN" >/dev/null 2>&1; then
  echo "ADB not found in PATH." >&2
  exit 1
fi

"$ADB_BIN" start-server >/dev/null
CONNECTED_DEVICES=$("$ADB_BIN" devices | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')
if [[ "$CONNECTED_DEVICES" == "0" ]]; then
  echo "No connected adb devices detected." >&2
  exit 1
fi

uninstall_if_present() {
  local package="$1"
  if "$ADB_BIN" shell pm list packages "$package" | grep -q "$package"; then
    "$ADB_BIN" uninstall "$package" >/dev/null && echo "Uninstalled $package" || true
  fi
}

uninstall_if_present org.jw.library.auto.dev
uninstall_if_present org.jw.library.auto

(cd "$ROOT_DIR" && ./gradlew clean assembleDebug)

APK_PATH="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found at $APK_PATH" >&2
  exit 1
fi

"$ADB_BIN" install -r "$APK_PATH"
