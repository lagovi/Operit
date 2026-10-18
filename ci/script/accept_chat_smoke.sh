#!/usr/bin/env bash
# Chat smoke acceptance: proves a live chat round-trip renders a reply,
# and that a model-down error dialog FAILS the run instead of the harness
# reading a blank assistant body as a pass (backlog N61).
#
# Unlike accept_bc_identity/accept_seed this does NOT reinstall: it runs
# against the build already on the phone. It still passes through
# onboarding when the device happens to be freshly installed.
#
# Usage: ci/script/accept_chat_smoke.sh
# Env: PHONE (default 192.168.1.69:5555), PKG (default ...operit.debug).
# Exit 0 = full DoD green.

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

# 1. launch and reach the chat screen (pass onboarding when present)
step "launch to chat"
plab_wake
adb -s "$PHONE" shell "monkey -p $PKG -c android.intent.category.LAUNCHER 1" >/dev/null 2>&1
if plab_wait_text "Your responsibility" 15; then
    for _ in 1 2 3 4 5; do
        adb -s "$PHONE" shell "input swipe 360 1200 360 300 400" && sleep 2
        plab_tap_text "Continue" 2>/dev/null
        sleep 2
        plab_wait_text "Enter your question..." 3 && break
    done
fi
plab_expect_text "Enter your question..." 30 "chat" && ok "chat reached" || { bad "chat"; exit 2; }

# 2. the walker scenario: send a message, fail on the model-down dialog
step "walker scenario"
uaw_run ci/script/scen/chat_smoke.uaw || exit 1
ok "chat round-trip, no model-down dialog"

echo "PASS=$pass FAIL=$failed"
[ "$failed" -eq 0 ]
