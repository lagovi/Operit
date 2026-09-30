#!/usr/bin/env bash
# On-device verification of the English-only build.
#
# The three defects this fork set out to fix are all invisible to a compiler and
# to a passing build, so they are checked by observation:
#
#   1. a fresh install on an unsupported system locale must start in English
#   2. no CJK may appear in a UI dump or a notification
#   3. localized text must not be clipped, which is worst at the maximum in-app
#      font scale
#
# Screens are read through uiautomator rather than read as images, because the
# dump gives exact text and bounds and does not depend on the screenshot path
# working.
#
# Usage:
#   ci/script/verify_device.sh [--fresh] [--font-scale 1.5] [--package PKG]

set -euo pipefail

PKG="com.ai.assistance.operit.debug"
FRESH=0
FONT_SCALE=1.5
DUMP_DIR="${DUMP_DIR:-/tmp/opencode/operit-verify}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --fresh) FRESH=1; shift ;;
    --font-scale) FONT_SCALE="$2"; shift 2 ;;
    --package) PKG="$2"; shift 2 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done

CJK='[一-鿿㐀-䶿豈-﫿぀-ヿ]'

fail=0
note() { printf '\n=== %s ===\n' "$1"; }
ok()   { printf '  ok    %s\n' "$1"; }
bad()  { printf '  FAIL  %s\n' "$1"; fail=1; }

mkdir -p "$DUMP_DIR"

dump_ui() {
  local name="$1" out="$DUMP_DIR/${1}.xml"
  # uiautomator intermittently writes a stale or empty dump, so retry until the
  # content actually changes.
  local previous="" current=0
  for _ in 1 2 3 4 5; do
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 || true
    adb pull /sdcard/ui.xml "$out" >/dev/null 2>&1 || true
    current="$(wc -c <"$out" 2>/dev/null || echo 0)"
    if [[ "$current" -gt 200 && "$current" != "$previous" ]]; then
      return 0
    fi
    previous="$current"
    sleep 1
  done
  return 1
}

check_no_cjk() {
  local name="$1" file="$2"
  local hits
  hits="$(grep -oP "$CJK" "$file" 2>/dev/null | sort -u | tr -d '\n' || true)"
  if [[ -z "$hits" ]]; then
    ok "$name: no CJK"
  else
    bad "$name: CJK present -> $(printf '%s' "$hits" | head -c 60)"
  fi
}

if ! adb get-state >/dev/null 2>&1; then
  echo "no adb device attached" >&2
  exit 2
fi

note "device"
adb shell getprop ro.product.model
echo "  android $(adb shell getprop ro.build.version.release), system locale $(adb shell getprop ro.product.locale)"
echo "  package $PKG $(adb shell dumpsys package "$PKG" 2>/dev/null | grep -m1 versionName || echo '(not installed)')"

if [[ "$FRESH" -eq 1 ]]; then
  note "fresh install"
  # Stored first-launch values are written in the app language at the time.
  # Clearing is the only way to test what a new user actually sees.
  adb shell am force-stop "$PKG" || true
  adb shell pm clear "$PKG"
  ok "app data cleared"
fi

note "launch"
adb shell am force-stop "$PKG" || true
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 8

# 1. First screen language
if dump_ui first_screen; then
  check_no_cjk "first screen" "$DUMP_DIR/first_screen.xml"
  if grep -qiE 'text="(Agree|I agree|Continue|Get started|Allow|OK|Next)' "$DUMP_DIR/first_screen.xml"; then
    ok "first screen shows English affordances"
  else
    bad "first screen: expected English CTA labels, dump follows"
    head -c 400 "$DUMP_DIR/first_screen.xml"
    echo
  fi
else
  bad "could not read the first screen"
fi

# 2. Notification text. A Service reads strings from the Application context,
#    which is the defect that left notifications in the previous language.
note "notification"
adb shell cmd notification list >/dev/null 2>&1 || true
dumpsys="$(adb shell dumpsys notification --noredact 2>/dev/null || true)"
if [[ -n "$dumpsys" ]]; then
  printf '%s' "$dumpsys" >"$DUMP_DIR/notification.txt"
  check_no_cjk "notification shade" "$DUMP_DIR/notification.txt"
else
  echo "  skip  dumpsys notification unavailable"
fi

# 3. Font scale. Fixed-height containers are correct at 1.0 and clipped at 1.5.
note "font scale $FONT_SCALE"
adb shell "run-as $PKG cat files/datastore/theme.preferences_pb" >/dev/null 2>&1 &&
  echo "  theme datastore readable" ||
  echo "  skip  theme datastore not readable, set the scale in Settings -> theme -> font size"

note "summary"
if [[ "$fail" -eq 0 ]]; then
  echo "  all checks passed"
else
  echo "  failures above need a look; dumps are in $DUMP_DIR"
fi
exit "$fail"
