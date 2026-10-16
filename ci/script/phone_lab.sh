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
    # Resilient: on transport errors retry, then revive a stale server
    # (kill-server/start-server/connect) and retry again — the caller
    # sees one failure only when the phone is truly unreachable.
    local attempt out rc
    for attempt in 1 2; do
        out=$(timeout 90 adb -s "$PHONE" "$@" 2>&1); rc=$?
        if [ $rc -eq 0 ]; then echo "$out"; return 0; fi
        if ! echo "$out" | grep -qiE "offline|not found|no devices|no route|timeout|closed"; then
            echo "$out"; return $rc
        fi
        sleep 3
    done
    adb kill-server >/dev/null 2>&1
    adb start-server >/dev/null 2>&1
    adb connect "$PHONE" >/dev/null 2>&1
    sleep 3
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

# Wake the display (no secure lock on the lab phone; keyguard, if any,
# dismisses with MENU). Dumps taken while asleep come back empty.
plab_wake() {
    _adb shell "input keyevent WAKEUP" >/dev/null 2>&1
    sleep 1
    _adb shell "input keyevent 82" >/dev/null 2>&1
    sleep 1
}

# Fresh UI dump to stdout.
plab_dump() {
    _adb shell uiautomator dump /sdcard/window_dump.xml >/dev/null 2>&1
    _adb shell cat /sdcard/window_dump.xml
}

# All visible texts, one per line — text= AND content-desc= (Compose
# exposes some rendered content, e.g. chat message bodies, as
# content-desc only; text-only search misses it — proven 16.10).
plab_texts() { plab_dump | grep -o '\(text\|content-desc\)="[^"]*"' | sed 's/^[a-z-]*="//; s/"$//' | grep -v '^$'; }

# Screenshot to a local file (for canvas-rendered screens like the
# terminal, which expose nothing to uiautomator — proven 16.10).
# Usage: plab_shot /tmp/opencode/term.png
plab_shot() {
    local dest="$1" tmp
    [ -n "$dest" ] || { echo "plab_shot: need a destination path" >&2; return 1; }
    tmp="/sdcard/plab_shot.png"
    _adb shell "screencap -p $tmp" >/dev/null 2>&1 \
        && _adb pull "$tmp" "$dest" >/dev/null 2>&1 \
        && _adb shell "rm -f $tmp" >/dev/null 2>&1 \
        || { echo "plab_shot: failed" >&2; return 1; }
    echo "  shot  $dest"
}

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
# Wakes the display when dumps come back empty (asleep phone reads empty);
# prints a heartbeat on long waits so a stuck run is visible, not silent.
plab_wait_text() {
    local want="$1" timeout="${2:-30}" i empty_streak=0 texts
    for ((i=0; i<timeout; i+=2)); do
        texts=$(plab_texts 2>/dev/null)
        if echo "$texts" | grep -qF "$want"; then return 0; fi
        if [ -z "$texts" ]; then
            empty_streak=$((empty_streak+1))
            if [ "$empty_streak" -ge 2 ]; then plab_wake; empty_streak=0; fi
        else
            empty_streak=0
        fi
        if [ "$timeout" -ge 60 ] && [ "$i" -gt 0 ] && [ $((i % 60)) -eq 0 ]; then
            echo "  ... still waiting for '$want' (${i}s), screen top: $(echo "$texts" | head -3 | tr '\n' '|')" >&2
        fi
        sleep 2
    done
    echo "plab_wait_text: timeout waiting for '$want'" >&2
    return 1
}

# Expectation: $1 must become visible within $2s while doing stage $3.
# On timeout prints what the screen actually shows and fails — every stage
# states what it expects, nothing waits blindly.
plab_expect_text() {
    local want="$1" timeout="$2" stage="$3"
    if plab_wait_text "$want" "$timeout"; then return 0; fi
    echo "EXPECT FAILED [$stage]: wanted '$want', screen shows:" >&2
    plab_texts 2>/dev/null | head -20 >&2
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

# --- Run protocol: status file + ETA + early-wake wait ---------------------
# Long phone jobs (downloads, installs, polls) run detached (nohup) and
# report progress to a status file, one line per step:
#   <epoch> START total=<N>
#   <epoch> STEP <i>/<N> <detail>
#   <epoch> FINAL PASS=<n> FAIL=<m>
# The agent never sleeps blind: it asks plab_eta for an informed sleep,
# then plab_wait_final which returns EARLY when FINAL appears.
# Format is plain text so `tail -3 <file>` is always meaningful.

plab_status_init() { echo "$(date +%s) START total=$2" > "$1"; }
plab_status() { echo "$(date +%s) STEP $2 $3" >> "$1"; }
plab_status_final() { echo "$(date +%s) FINAL PASS=$2 FAIL=$3" >> "$1"; }

# One-line progress + ETA from a status file: completed steps, elapsed,
# per-step rate, estimated remaining. Prints e.g.:
#   done 3/24, elapsed 410s, ~1m40s/step, ETA ~52m
plab_eta() {
    python3 - "$1" <<'EOF'
import sys, time
f = sys.argv[1]
try:
    lines = open(f).read().splitlines()
except OSError:
    print('no status file yet'); sys.exit(0)
start = None; total = None; done = 0; last = None
for ln in lines:
    p = ln.split()
    if len(p) < 2: continue
    try: ts = int(p[0])
    except ValueError: continue
    if p[1] == 'START':
        start = ts
        for q in p[2:]:
            if q.startswith('total='): total = int(q[6:])
    elif p[1] == 'STEP':
        done += 1; last = ts
    elif p[1] == 'FINAL':
        print('FINISHED: ' + ' '.join(p[2:])); sys.exit(0)
if start is None:
    print('no START yet'); sys.exit(0)
now = int(time.time())
el = max(now - start, 1); done = max(done, 1)
rate = el / done
rem = (total - done) if total else None
def fmt(s):
    s = int(s)
    return (f'{s//3600}h{(s%3600)//60:02d}m' if s >= 3600 else (f'{s//60}m{s%60:02d}s' if s >= 60 else f'{s}s'))
tot = f'/{total}' if total else ''
eta = fmt(rem * rate) if rem is not None and rem > 0 else 'n/a'
print(f'done {done}{tot}, elapsed {fmt(el)}, ~{fmt(rate)}/step, ETA {eta}')
EOF
}

# Sleep until FINAL appears in $1 or $2 seconds pass. Checks every 30s
# (cheap: one file read, no ADB). Returns 0 on FINAL, 1 on timeout.
# Usage: plab_wait_final /tmp/job.status 3600  → then read the tail.
plab_wait_final() {
    local file="$1" timeout="${2:-3600}" waited=0
    while [ $waited -lt "$timeout" ]; do
        grep -q "FINAL" "$file" 2>/dev/null && return 0
        sleep 30
        waited=$((waited + 30))
    done
    grep -q "FINAL" "$file" 2>/dev/null && return 0 || return 1
}
