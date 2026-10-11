#!/usr/bin/env bash
# Fork identity (task 18, B+C) acceptance: clean install -> onboarding shows
# the attribution line -> About shows it -> launcher shows "Operit EN".
#
# UI flow inside the app runs through uawalk; install/grants/onboarding
# passthrough/launcher check live here.
#
# Usage: ci/script/accept_bc_identity.sh <local-apk>
# Env: PHONE (default 192.168.1.69:5555), PKG (default ...operit.debug).
# Exit 0 = full DoD green.

set -uo pipefail
cd "$(dirname "$0")/../.."
# shellcheck disable=SC1091
source ci/script/phone_lab.sh
# shellcheck disable=SC1091
source ci/script/uawalk.sh

APK="${1:?usage: accept_bc_identity.sh <local-apk>}"
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

# 1. clean install
step "clean install"
adb -s "$PHONE" uninstall "$PKG" >/dev/null 2>&1
install_out=$(timeout 600 adb -s "$PHONE" install "$APK" 2>&1) || true
echo "$install_out" | grep -q "Success" || {
    echo "install attempt 1: $install_out" >&2
    sleep 5
    install_out=$(timeout 600 adb -s "$PHONE" install "$APK" 2>&1) || true
}
echo "$install_out" | grep -q "Success" && ok "clean install" || { bad "install: $install_out"; exit 2; }

# 2. grants
step "grants"
adb -s "$PHONE" shell "pm grant $PKG android.permission.READ_EXTERNAL_STORAGE; pm grant $PKG android.permission.WRITE_EXTERNAL_STORAGE; pm grant $PKG android.permission.ACCESS_FINE_LOCATION; pm grant $PKG android.permission.ACCESS_COARSE_LOCATION; pm grant $PKG android.permission.RECORD_AUDIO; appops set $PKG MANAGE_EXTERNAL_STORAGE allow; appops set $PKG SYSTEM_ALERT_WINDOW allow; dumpsys deviceidle whitelist +$PKG" >/dev/null 2>&1 && ok "grants" || bad "grants"

# 3. onboarding attribution (shows once per install — catch it here)
step "onboarding attribution"
adb -s "$PHONE" shell "monkey -p $PKG -c android.intent.category.LAUNCHER 1" >/dev/null 2>&1
plab_expect_text "Your responsibility" 60 "onboarding" || { bad "onboarding"; exit 2; }
plab_expect_text "community fork of Operit" 20 "onboarding attribution" && ok "onboarding attribution" || { bad "onboarding attribution"; exit 2; }
adb -s "$PHONE" shell "input swipe 360 1200 360 300 400" && sleep 2
for _ in 1 2 3; do
    plab_tap_text "Continue" 2>/dev/null && break
    sleep 2
done
plab_expect_text "New Chat" 30 "onboarding passthrough" && ok "chat" || { bad "chat"; exit 2; }

# 4. About attribution through the walker
step "walker scenario"
uaw_run ci/script/scen/bc_identity.uaw || exit 1
ok "about attribution"

# 5. launcher label (system UI: HOME, open the drawer, read the icon text)
step "launcher label"
adb -s "$PHONE" shell "input keyevent KEYCODE_HOME" && sleep 2
adb -s "$PHONE" shell "input swipe 360 1400 360 400 400" && sleep 3
plab_wait_text "Operit EN" 20 && ok "launcher label Operit EN" || { bad "launcher label"; plab_texts | head -30 >&2; }
if plab_texts | grep -qF "Operit Debug"; then
    bad "old label still present"
else
    ok "old label gone"
fi

echo "PASS=$pass FAIL=$failed"
[ "$failed" -eq 0 ]
