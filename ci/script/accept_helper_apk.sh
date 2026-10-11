#!/usr/bin/env bash
# Helper-APK on-demand acceptance: clean install -> DEBUGGER/ACCESSIBILITY
# tabs -> install buttons download the APKs from the fork release into
# cacheDir (size+sha verified) -> system installer appears. No helper is
# actually installed (the phone's Shizuku setup stays intact).
#
# UI flow runs through uawalk (ci/script/uawalk.sh): every step states what
# it taps and what it expects; mismatches abort with screen context.
# Shell-side setup (transport/install/grants) + file asserts live here.
#
# Usage: ci/script/accept_helper_apk.sh
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

# 1. target build + grants (incl. install-unknown-apps: the system installer
# would otherwise block with a settings detour, which is not what we test)
step "build+grants"
adb -s "$PHONE" shell dumpsys package "$PKG" | grep -q "lastUpdateTime" && ok "pkg present" || { bad "pkg $PKG missing"; exit 2; }
adb -s "$PHONE" shell "pm grant $PKG android.permission.READ_EXTERNAL_STORAGE; pm grant $PKG android.permission.WRITE_EXTERNAL_STORAGE; pm grant $PKG android.permission.ACCESS_FINE_LOCATION; pm grant $PKG android.permission.ACCESS_COARSE_LOCATION; pm grant $PKG android.permission.RECORD_AUDIO; appops set $PKG MANAGE_EXTERNAL_STORAGE allow; appops set $PKG SYSTEM_ALERT_WINDOW allow; appops set $PKG REQUEST_INSTALL_PACKAGES allow; dumpsys deviceidle whitelist +$PKG" >/dev/null 2>&1 && ok "grants" || bad "grants"

# 2. bring the app to chat (BACK out of wherever it is; pass onboarding if fresh)
step "to chat"
adb -s "$PHONE" shell "monkey -p $PKG -c android.intent.category.LAUNCHER 1" >/dev/null 2>&1
sleep 3
for _ in 1 2 3 4 5; do
    plab_wait_text "New Chat" 3 && break
    plab_wait_text "Your responsibility" 3 && break
    adb -s "$PHONE" shell "input keyevent KEYCODE_BACK"
    sleep 2
done
if plab_wait_text "Your responsibility" 5; then
    adb -s "$PHONE" shell "input swipe 360 1200 360 300 400"
    sleep 2
    for _ in 1 2 3; do
        plab_tap_text "Continue" 2>/dev/null && break
        sleep 2
    done
fi
plab_expect_text "New Chat" 30 "onboarding passthrough" && ok "chat" || { bad "chat"; exit 2; }

# 3. cache must be clean before the UI flow: drop leftovers of killed runs
# so both downloads are proven fresh (setup, not a fallback)
step "cache clean"
adb -s "$PHONE" shell "run-as $PKG rm -f cache/shizuku.apk cache/accessibility.apk" >/dev/null 2>&1
if adb -s "$PHONE" shell "run-as $PKG ls cache/" 2>/dev/null | grep -q "\.apk"; then
    bad "cache not clean at start"
else
    ok "cache clean at start"
fi

# 4. UI flow through the walker (aborts with context on any mismatch)
step "walker scenario"
uaw_run ci/script/scen/helper_apk.uaw || exit 1
ok "walker scenario green"

# 5. file proofs: exact size + sha of both downloads
step "file proofs"
plab_assert_leaf_file "cache/shizuku.apk" "2571773" "6e273ab0" || bad "shizuku file"
plab_assert_leaf_file "cache/accessibility.apk" "2763980" "b6bac271" || bad "a11y file"

echo "PASS=$pass FAIL=$failed"
[ "$failed" -eq 0 ]
