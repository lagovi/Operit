#!/usr/bin/env bash
# Terminal-env setup acceptance (task 6): tap through to Start Setup, then
# poll the DEBUGGER "Operit Terminal" row until it flips to Granted
# (pnpm+python+pip present in the proot session). The install resumes
# across interruptions by design, so a timeout here just means "run again".
#
# No reinstall: wiping would delete the env being installed.
#
# Usage: ci/script/accept_terminal_setup.sh
# Env: PHONE (default 192.168.1.69:5555), PKG (default ...operit.debug).
# Exit 0 = row green.

set -uo pipefail
cd "$(dirname "$0")/../.."
# shellcheck disable=SC1091
source ci/script/phone_lab.sh
# shellcheck disable=SC1091
source ci/script/uawalk.sh

pass=0; failed=0
ok()   { echo "  ok    $1"; pass=$((pass+1)); }
bad()  { echo "  FAIL  $1"; failed=$((failed+1)); }
step() { echo "== $1"; }

# Screen context for every navigation failure (package + screen top),
# walker-style. Defined once, used by to_chat/to_debugger_row/terminal_state.
where() {
    local pkg texts
    pkg=$(adb -s "$PHONE" shell "dumpsys window" 2>/dev/null | grep -m1 "mCurrentFocus=" | grep -o "u0 [a-zA-Z0-9._/]*" | head -1)
    texts=$(plab_texts 2>/dev/null | head -8 | tr '\n' '|')
    echo "at package=[${pkg:-unknown}] screen=[$texts]" >&2
}

to_chat() {
    # Anchor: "Enter question" (the chat input hint). NOT "New Chat": that
    # text exists only when the chat list is empty; a used chat (history,
    # bound workspace) still shows the input. Learned the hard way: 18
    # BACKs walked past a history-carrying chat without ever matching.
    adb -s "$PHONE" shell "monkey -p $PKG -c android.intent.category.LAUNCHER 1" >/dev/null 2>&1
    sleep 3
    for _round in 1 2 3; do
        for _ in 1 2 3 4 5 6; do
            plab_wait_text "Enter question" 3 && return 0
            adb -s "$PHONE" shell "input keyevent KEYCODE_BACK"
            sleep 2
        done
        # Bound workspace view traps BACK: the Code icon toggles back to chat.
        if plab_texts 2>/dev/null | grep -qF "Preview"; then
            plab_tap 671 110
            sleep 3
            plab_wait_text "Enter question" 10 && return 0
        fi
        adb -s "$PHONE" shell "monkey -p $PKG -c android.intent.category.LAUNCHER 1" >/dev/null 2>&1
        sleep 3
    done
    where
    return 1
}

# Walk to the terminal screen and report its state: RUNNING (install in
# progress), NEED_TAP (setup screen idle), or UNKNOWN.
terminal_state() {
    to_chat || return 1
    plab_tap 49 110 && sleep 3
    plab_tap_text "Toolbox" 2>/dev/null || return 1
    sleep 3
    for _ in 1 2 3 4; do
        plab_texts 2>/dev/null | grep -qF "Command Terminal" && break
        adb -s "$PHONE" shell "input swipe 360 1200 360 300 400" < /dev/null && sleep 2
    done
    plab_tap_text "Command Terminal" 2>/dev/null || return 1
    sleep 5
    if plab_texts 2>/dev/null | grep -qF "Interrupt"; then
        echo "RUNNING"
    elif plab_texts 2>/dev/null | grep -qF "Start Setup"; then
        echo "NEED_TAP"
    else
        echo "UNKNOWN"
    fi
    return 0
}

to_debugger_row() {
    # From anywhere: home the app, walk to Permissions -> DEBUGGER, print
    # the status text following "Operit Terminal".
    to_chat || return 1
    plab_tap 49 110 && sleep 3
    for _ in 1 2 3; do
        plab_tap_text "Permissions" 2>/dev/null && break
        sleep 3
    done
    sleep 2
    plab_wait_text "Permission Level" 20 || { where; return 1; }
    for _ in 1 2 3; do
        plab_tap_text "DEBUGGER" 2>/dev/null && break
        sleep 3
    done
    sleep 3
    plab_wait_text "Debug Permission" 20 || { where; return 1; }
    adb -s "$PHONE" shell "input swipe 360 1200 360 300 400" && sleep 2
    plab_dump | python3 -c "
import sys, re
texts = []
x = sys.stdin.read()
for m in re.finditer(r'text=\"([^\"]*)\"', x):
    t = m.group(1)
    if t:
        texts.append(t)
for i, t in enumerate(texts):
    if t == 'Operit Terminal' and i + 1 < len(texts):
        print(texts[i + 1])
        break
"
}

# 0. transport + root
step "transport+root"
adb -s "$PHONE" shell echo alive >/dev/null || { echo "FAIL phone unreachable"; exit 2; }
orig_off_timeout=$(adb -s "$PHONE" shell settings get system screen_off_timeout 2>/dev/null | tr -d '\r') || orig_off_timeout=""
adb -s "$PHONE" shell settings put system screen_off_timeout 1800000 >/dev/null 2>&1
restore_display() {
    [ -n "${orig_off_timeout:-}" ] && adb -s "$PHONE" shell settings put system screen_off_timeout "$orig_off_timeout" >/dev/null 2>&1
}
trap restore_display EXIT
adb -s "$PHONE" shell "su -c id" | grep -q "uid=0" && ok "root" || { bad "root"; exit 2; }

# 1. target build present (never reinstall here)
step "build present"
adb -s "$PHONE" shell dumpsys package "$PKG" | grep -q "lastUpdateTime" && ok "pkg present" || { bad "pkg $PKG missing"; exit 2; }

# 2. tap part, unless the install is already running
step "tap part"
state=$(terminal_state) || state="NAVFAIL"
echo "  terminal state: $state"
if [ "$state" = "RUNNING" ]; then
    ok "install already running"
else
    to_chat || { bad "to chat"; exit 2; }
    uaw_run ci/script/scen/terminal_setup.uaw || exit 1
    ok "setup started"
fi

# 3. poll the row (up to ~75 min; install resumes, so timeout = retry later)
step "poll Operit Terminal row"
for ((round = 1; round <= 15; round++)); do
    adb -s "$PHONE" shell settings put system screen_off_timeout 1800000 >/dev/null 2>&1
    status=$(to_debugger_row) || status="NAVFAIL"
    echo "  round $round: Operit Terminal = [$status]"
    if [ "$status" = "Granted" ]; then
        ok "row green"
        echo "PASS=$pass FAIL=$failed"
        exit 0
    fi
    sleep 300
done
bad "row still not green after ~75 min (install resumes; run again)"
echo "PASS=$pass FAIL=$failed"
exit 1
