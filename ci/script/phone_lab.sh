#!/usr/bin/env bash
# Phone-lab helper: rooted-device taps and algorithmic UI checks.
#
# Why this exists: `adb shell input tap` is ignored by some system windows
# (proven on DocumentsUI folder rows and the USE button, 09.10), while a
# real finger works. With root we can inject kernel-level touches via
# sendevent on the touchscreen device, which no app can distinguish from
# a finger. All checks read uiautomator dumps and logcat, never screenshots.
#
# Requirements: rooted phone (Magisk `su`), stable ADB-TCP (see HANDOFF
# cheat-sheet: `...:5555`). Touchscreen device auto-detected by name.
#
# Usage:
#   source ci/script/phone_lab.sh
#   plab_tap 360 839            # kernel tap, 1:1 dump coords
#   plab_dump > /tmp/dump.xml  # fresh uiautomator dump
#   plab_wait_text "Selected: q2html" 30
#   plab_assert_leaf_file files/subpack/apk_editor_android.apk 48139093 c56b23a8...

set -uo pipefail

PHONE="${PHONE:-192.168.1.69:5555}"
PKG="${PKG:-com.ai.assistance.operit.debug}"
PLAB_TOUCH_DEV="${PLAB_TOUCH_DEV:-}"
PLAB_TRACK_ID=20

_adb() {
    # Hard timeout: a stalled ADB transport must fail fast, never hang
    # the caller forever (10.10: accept run hung with zero output).
    timeout 90 adb -s "$PHONE" "$@"
}

# Detect the touchscreen event node once (ILITEK_TDDI here; match by ABS_X range).
plab_touch_dev() {
    if [ -n "$PLAB_TOUCH_DEV" ]; then echo "$PLAB_TOUCH_DEV"; return 0; fi
    local dev
    dev=$(_adb shell "su -c getevent -p" 2>/dev/null | awk '
        /^add device/ { dev=$4 }
        /0035.*max 719/ { print dev; exit }')
    # fallback: first device with multitouch slot support
    if [ -z "$dev" ]; then
        dev=$(_adb shell "su -c getevent -p" 2>/dev/null | awk '
            /^add device/ { dev=$4 }
            /002f.*max/ { print dev; exit }')
    fi
    PLAB_TOUCH_DEV="$dev"
    echo "$dev"
}

# Kernel-level tap at dump coordinates. Returns nonzero when root is missing.
plab_tap() {
    local x="$1" y="$2" dev tid
    dev=$(plab_touch_dev)
    [ -n "$dev" ] || { echo "plab_tap: no touch device (root?)" >&2; return 1; }
    PLAB_TRACK_ID=$((PLAB_TRACK_ID + 1))
    tid=$PLAB_TRACK_ID
    _adb shell "su -c 'sh -c \"
        sendevent $dev 3 47 0;
        sendevent $dev 1 330 1;
        sendevent $dev 3 57 $tid;
        sendevent $dev 3 53 $x;
        sendevent $dev 3 54 $y;
        sendevent $dev 3 48 9;
        sendevent $dev 3 58 50;
        sendevent $dev 0 0 0;
        sleep 0.12;
        sendevent $dev 3 57 4294967295;
        sendevent $dev 1 330 0;
        sendevent $dev 0 0 0\"'" >/dev/null || return 1
    sleep 0.5
    return 0
}

# Plain input tap (works for in-app controls; kept as fallback).
plab_tap_input() { _adb shell input tap "$1" "$2"; sleep 0.5; }

# Fresh UI dump to stdout.
plab_dump() {
    _adb shell uiautomator dump /sdcard/window_dump.xml >/dev/null 2>&1
    _adb shell cat /sdcard/window_dump.xml
}

# All visible texts, one per line.
plab_texts() { plab_dump | grep -o 'text="[^"]*"' | sed 's/^text="//; s/"$//' | grep -v '^$'; }

# Bounds "x1,y1-x2,y2" (as "x1 y1 x2 y2") of the clickable ancestor of a text.
plab_bounds_of() {
    local want="$1"
    plab_dump | python3 -c "
import sys,xml.etree.ElementTree as ET
d=sys.stdin.read().split('?>',1)[1]; r=ET.fromstring(d)
par={c:p for p in r.iter() for c in p}
for n in r.iter('node'):
    if n.get('text')=='''$want''':
        cur=n
        if cur.get('clickable')=='true':
            print(' '.join(__import__('re').findall(r'\d+', cur.get('bounds'))))
            break
        for _ in range(5):
            cur=par.get(cur)
            if cur is None: break
            if cur.get('clickable')=='true':
                print(' '.join(__import__('re').findall(r'\d+', cur.get('bounds'))))
                break
        break"
}

# Center of "x1 y1 x2 y2" bounds.
plab_center() { awk '{printf "%d %d", ($1+$3)/2, ($2+$4)/2}'; }

# Tap the clickable ancestor of a visible text. Fails when absent.
plab_tap_text() {
    local b c
    b=$(plab_bounds_of "$1") || return 1
    [[ "$b" =~ ^[0-9]+[[:space:]][0-9]+[[:space:]][0-9]+[[:space:]][0-9]+$ ]] \
        || { echo "plab_tap_text: bad bounds for '$1': '$b'" >&2; return 1; }
    c=$(echo "$b" | plab_center)
    plab_tap $c
}

# Wait up to $2 seconds for $1 (fixed string) to appear in UI texts.
plab_wait_text() {
    local want="$1" timeout="${2:-30}" i
    for ((i=0; i<timeout; i+=2)); do
        if plab_texts 2>/dev/null | grep -qF "$want"; then return 0; fi
        sleep 2
    done
    echo "plab_wait_text: timeout waiting for '$want'" >&2
    return 1
}

# Assert a file under the app data dir has exact size and sha256 prefix.
plab_assert_leaf_file() {
    local rel="$1" size="$2" sha_prefix="$3" out
    out=$(_adb shell "run-as $PKG sh -c 'ls -l $rel && sha256sum $rel'" 2>&1)
    echo "$out" | grep -q " $size " || { echo "plab_assert_leaf_file: size mismatch: $out" >&2; return 1; }
    echo "$out" | grep -q "^$sha_prefix" || { echo "plab_assert_leaf_file: sha mismatch: $out" >&2; return 1; }
    echo "  ok    $rel size=$size sha=$sha_prefix..."
    return 0
}

# Assert packages of the current window (space-separated) match expectation.
plab_assert_pkg() {
    local want="$1" got
    got=$(plab_dump | grep -o 'package="[^"]*"' | sed 's/package="//; s/"//' | sort -u | tr '\n' ' ')
    [ "$got" = "$want " ] || [ "$got" = "$want" ] || { echo "plab_assert_pkg: want [$want] got [$got]" >&2; return 1; }
    return 0
}
