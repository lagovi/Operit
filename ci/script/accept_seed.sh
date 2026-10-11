#!/usr/bin/env bash
# Seed acceptance (templates + emoji, Q2): clean install -> emoji seeds at
# startup in background -> walker binds a Web project (template seed
# downloads inside the bind) -> file proofs for both seeds and both copies.
#
# Usage: ci/script/accept_seed.sh <local-apk>
# Env: PHONE (default 192.168.1.69:5555), PKG (default ...operit.debug).
# Exit 0 = full DoD green.

set -uo pipefail
cd "$(dirname "$0")/../.."
# shellcheck disable=SC1091
source ci/script/phone_lab.sh
# shellcheck disable=SC1091
source ci/script/uawalk.sh

APK="${1:?usage: accept_seed.sh <local-apk>}"
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

# 2. grants + launch (emoji seeding starts at startup, in background)
step "grants+launch"
adb -s "$PHONE" shell "pm grant $PKG android.permission.READ_EXTERNAL_STORAGE; pm grant $PKG android.permission.WRITE_EXTERNAL_STORAGE; pm grant $PKG android.permission.ACCESS_FINE_LOCATION; pm grant $PKG android.permission.ACCESS_COARSE_LOCATION; pm grant $PKG android.permission.RECORD_AUDIO; appops set $PKG MANAGE_EXTERNAL_STORAGE allow; appops set $PKG SYSTEM_ALERT_WINDOW allow; dumpsys deviceidle whitelist +$PKG" >/dev/null 2>&1 && ok "grants" || bad "grants"
adb -s "$PHONE" shell "monkey -p $PKG -c android.intent.category.LAUNCHER 1" >/dev/null 2>&1
plab_expect_text "Your responsibility" 60 "onboarding" || { bad "onboarding"; exit 2; }
for _ in 1 2 3 4 5; do
    adb -s "$PHONE" shell "input swipe 360 1200 360 300 400" && sleep 2
    plab_tap_text "Continue" 2>/dev/null
    sleep 2
    plab_wait_text "New Chat" 3 && break
done
plab_expect_text "New Chat" 30 "onboarding passthrough" && ok "chat" || { bad "chat"; exit 2; }

# 3. template seed through the walker (download happens inside the bind)
step "walker scenario"
uaw_run ci/script/scen/seed_templates.uaw || exit 1
ok "walker scenario green"

# 4. file proofs (emoji had the whole run to seed in background)
step "file proofs"
for _ in $(seq 1 30); do
    adb -s "$PHONE" shell "run-as $PKG ls files/seed/emoji.zip.seed-ok files/seed/templates.zip.seed-ok" 2>/dev/null | grep -q "seed-ok" && break
    sleep 4
done
plab_assert_leaf_file "files/seed/templates.zip" "4227058" "e86ed55b" || bad "templates zip"
plab_assert_leaf_file "files/seed/emoji.zip" "3614642" "061c97f0" || bad "emoji zip"
adb -s "$PHONE" shell "run-as $PKG ls files/seed/templates/web files/seed/emoji" 2>/dev/null | grep -q . && ok "unpacked trees" || bad "unpacked trees"
adb -s "$PHONE" shell "run-as $PKG find files/workspace -name index.html" 2>/dev/null | grep -q "index.html" && ok "workspace index.html" || bad "workspace index.html"
adb -s "$PHONE" shell "run-as $PKG find files/custom_emoji -type f" 2>/dev/null | grep -q . && ok "copied emojis" || bad "copied emojis"

echo "PASS=$pass FAIL=$failed"
[ "$failed" -eq 0 ]
