#!/usr/bin/env bash
# Q2 subpack acceptance: clean install -> download template from the fork
# release -> repack -> sign -> installable APK -> offline reuse.
#
# Every step below was proven live on 2026-10-10 (HANDOFF 13); the script
# only encodes them with algorithmic checks, no screenshots. Human needed
# for nothing: folder picking goes through kernel taps (phone_lab.sh).
#
# Usage: ci/script/accept_q2_export.sh <local-apk>
# Env: PHONE (default 192.168.1.69:5555), PKG (default ...operit.debug).
# Exit 0 = full DoD green.
# NOTE: step 3 rewritten 10.10 for the single-screen onboarding (was
# agreement -> tour -> welcome -> permissions -> level); first live run pending.

set -uo pipefail
cd "$(dirname "$0")/../.."
# shellcheck disable=SC1091
source ci/script/phone_lab.sh

APK="${1:?usage: accept_q2_export.sh <local-apk>}"
pass=0; failed=0
ok()   { echo "  ok    $1"; pass=$((pass+1)); }
bad()  { echo "  FAIL  $1"; failed=$((failed+1)); }
step() { echo "== $1"; }

# 0. transport + root
step "transport+root"
adb -s "$PHONE" shell echo alive >/dev/null || { echo "FAIL phone unreachable"; exit 2; }
adb -s "$PHONE" shell "su -c id" | grep -q "uid=0" && ok "root" || { bad "root"; exit 2; }

# 1. clean install
step "clean install"
adb -s "$PHONE" uninstall "$PKG" >/dev/null 2>&1
adb -s "$PHONE" install "$APK" >/dev/null || { bad "install"; exit 2; }
ok "clean install"

# 2. grants (shell set; wizard still taps through, grants make it pass)
step "grants (shell set; wizard still taps through, grants make it pass)"
for p in READ_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION RECORD_AUDIO; do
    adb -s "$PHONE" shell pm grant "$PKG" "android.permission.$p"
done
adb -s "$PHONE" shell appops set "$PKG" MANAGE_EXTERNAL_STORAGE allow
adb -s "$PHONE" shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow
adb -s "$PHONE" shell dumpsys deviceidle whitelist +"$PKG" >/dev/null
ok "grants"

# 3. first launch: single onboarding screen (liability + permissions +
step "first launch: single onboarding screen (liability + permissions +"
# level on one scroll) -> Continue -> chat. Continue sits at the bottom,
# so swipe up until it is visible; grants from step 2 make the rows pass.
adb -s "$PHONE" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 4
plab_wait_text "Your responsibility" 30 || { bad "onboarding"; exit 2; }
for _ in 1 2 3 4 5; do plab_texts | grep -q "Continue" && break; adb -s "$PHONE" shell input swipe 360 1100 360 400 400; sleep 1; done
plab_tap_text "Continue" || { bad "onboarding continue"; exit 2; }
sleep 2
plab_wait_text "New Chat" 30 && ok "onboarding" || { bad "onboarding"; exit 2; }

# 4. Toolbox -> HTML Packager -> Select Folder (picker) -> USE -> Allow.
# An "Announcement" dialog (Got it) can pop up at any moment; dismiss
# it whenever it is visible, before every navigation step.
plab_dismiss_got_it() {
    for _ in 1 2 3; do
        plab_texts | grep -q "Got it" || return 0
        plab_tap_text "Got it"; sleep 1.5
    done
}
plab_dismiss_got_it
plab_tap 49 110; sleep 1.5
plab_dismiss_got_it
plab_tap_text "Toolbox"; sleep 1.5
plab_dismiss_got_it
for _ in 1 2 3 4 5 6; do plab_texts | grep -q "HTML Packager" && break; adb -s "$PHONE" shell input swipe 360 1100 360 400 400; sleep 1; done
sleep 1
plab_tap_text "HTML Packager" || { bad "packager open"; exit 2; }
plab_wait_text "Select Folder" 20 || { bad "packager open"; exit 2; }
# Kernel taps occasionally do not register: tap until the picker is open.
for _ in 1 2 3; do
    plab_tap_text "Select Folder"; sleep 4
    plab_assert_pkg "com.google.android.documentsui" >/dev/null 2>&1 && break
done
plab_assert_pkg "com.google.android.documentsui" && ok "picker" || { bad "picker"; exit 2; }
# NOTE: the fixture folder /sdcard/q2html/index.html must exist (created once via shell).
plab_wait_text "q2html" 20 || { bad "fixture folder"; exit 2; }
plab_tap_text "ИСПОЛЬЗОВАТЬ ЭТУ ПАПКУ"; sleep 3
if plab_texts | grep -q "РАЗРЕШИТЬ"; then plab_tap_text "РАЗРЕШИТЬ"; sleep 3; fi
plab_wait_text "Selected: q2html" 20 && ok "folder picked" || { bad "folder picked"; exit 2; }

# 5. online export: download + repack + sign, output exists
step "online export: download + repack + sign, output exists"
adb -s "$PHONE" logcat -c
plab_tap_text "Generate Package"; sleep 2
plab_tap 258 784; sleep 2
plab_wait_text "Configure Android App" 20 || { bad "export dialog"; exit 2; }
plab_tap 508 1224
plab_wait_text "Export Successful" 600 || { bad "online export"; adb -s "$PHONE" logcat -d | grep -i -E "Exception|not available" | head -3; exit 2; }
OUT_APK=$(plab_texts | grep -m1 "WebApp_.*\.apk")
[ -n "$OUT_APK" ] || { bad "output path"; exit 2; }
ok "online export: $OUT_APK"
SZ=$(adb -s "$PHONE" shell "stat -c %s /storage/emulated/0/Download/Operit/exports/$OUT_APK")
[ "$SZ" -gt 40000000 ] && ok "output size $SZ" || { bad "output size $SZ"; exit 2; }

# 6. template leaf intact (size+sha of the release payload)
step "template leaf intact (size+sha of the release payload)"
plab_assert_leaf_file files/subpack/apk_editor_android.apk 48139093 c56b23a8 \
    && ok "leaf intact" || { bad "leaf intact"; exit 2; }

# 7. offline reuse: block ONLY the app uid via iptables (never svc wifi — it kills ADB).
step "offline reuse: block ONLY the app uid via iptables (never svc wifi — it kills ADB)."
AID=$(adb -s "$PHONE" shell dumpsys package "$PKG" 2>/dev/null | grep -m1 userId | grep -o "[0-9]*")
adb -s "$PHONE" shell "su -c 'iptables -A OUTPUT -m owner --uid-owner $AID -j REJECT'"
adb -s "$PHONE" logcat -c
plab_tap_text "Close"; sleep 1.5
plab_tap_text "Generate Package"; sleep 2
plab_tap 258 784; sleep 2
plab_tap 508 1224
plab_wait_text "Export Successful" 300 && ok "offline export" || { bad "offline export"; adb -s "$PHONE" shell "su -c 'iptables -D OUTPUT -m owner --uid-owner $AID -j REJECT'"; exit 2; }
adb -s "$PHONE" shell "su -c 'iptables -D OUTPUT -m owner --uid-owner $AID -j REJECT'"
if adb -s "$PHONE" logcat -d | grep -qiE "RemoteAssetFetcher.*(download|fetch)|downloading.*apk_editor"; then bad "offline downloaded"; exit 2; else ok "offline: no download"; fi

echo
echo "PASS=$pass FAIL=$failed"
[ "$failed" -eq 0 ]
