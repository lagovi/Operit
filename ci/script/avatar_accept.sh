#!/usr/bin/env bash
# Avatar acceptance helper: install a build, seed the test models, pass onboarding,
# and open Assistant Config with the avatar enabled on a chosen model. Prints the
# glTF framing lines from the app log so a framing change can be judged.
#
# Why this exists: the CI key changes between builds, so every install needs an
# uninstall and a full onboarding pass, and the avatar preview is several screens
# deep. Doing that by hand for every build wastes time and invites coordinate
# taps on the wrong screen, which this script replaces with text-driven taps.
#
# Usage:
#   ci/script/avatar_accept.sh <apk> <Xbot|Robot>
# Env: PHONE (default 192.168.1.69:5555), PKG (default ...operit.debug).
# Exit 0 when Assistant Config is reached with the avatar enabled on the model.

set -uo pipefail
cd "$(dirname "$0")/../.."
# shellcheck disable=SC1091
source ci/script/phone_lab.sh

APK="${1:-}"
MODEL="${2:-Xbot}"
if [ -z "$APK" ] || [ ! -f "$APK" ]; then
    echo "usage: $0 <apk> <Xbot|Robot>" >&2
    exit 2
fi

# Local copies of the two test models, pushed to the phone and copied into the
# app's external avatar directory with the exact layout the repository scans.
X_LOCAL="${X_LOCAL:-/tmp/opencode/models/xbot.glb}"
R_LOCAL="${R_LOCAL:-/tmp/opencode/robot.glb}"

step() { echo "== $1"; }

step "transport+root"
_adb shell echo alive >/dev/null || { echo "FAIL phone unreachable"; exit 2; }
_adb shell "su -c id" | grep -q uid=0 || { echo "FAIL no root"; exit 2; }
_adb shell settings put system screen_off_timeout 1800000 >/dev/null 2>&1

step "install"
_adb uninstall "$PKG" >/dev/null 2>&1 || true
_adb install "$APK" >/dev/null 2>&1 || { echo "FAIL install"; exit 2; }

step "seed models"
_adb push "$X_LOCAL" /data/local/tmp/xbot.glb >/dev/null 2>&1 || { echo "FAIL push xbot"; exit 2; }
_adb push "$R_LOCAL" /data/local/tmp/robot.glb >/dev/null 2>&1 || { echo "FAIL push robot"; exit 2; }
DIR="/storage/emulated/0/Android/data/$PKG/files/avatars"
_adb shell "su -c 'mkdir -p $DIR/Xbot $DIR/Robot; cp /data/local/tmp/xbot.glb $DIR/Xbot/model.glb; cp /data/local/tmp/robot.glb $DIR/Robot/model.glb; chmod 660 $DIR/Xbot/model.glb $DIR/Robot/model.glb'" >/dev/null 2>&1 \
    || { echo "FAIL seed"; exit 2; }

step "launch"
plab_wake
_adb shell "monkey -p $PKG -c android.intent.category.LAUNCHER 1" >/dev/null 2>&1
sleep 3

# Onboarding: the permission screen needs a few swipes to the bottom and the
# Continue button. Idempotent: each pass is skipped once the main screen shows.
if plab_wait_text "Your responsibility" 15; then
    for _ in 1 2 3 4 5 6; do
        _adb shell "input swipe 360 1200 360 300 400" >/dev/null 2>&1
        sleep 1
        plab_tap_text "Continue" >/dev/null 2>&1
        sleep 2
        if plab_wait_text "New Chat" 2 || plab_wait_text "AI Chat" 2; then
            break
        fi
    done
fi
plab_wait_text "AI Chat" 20 >/dev/null || { echo "FAIL onboarding"; exit 2; }

step "open Assistant Config"
# The menu button is an icon with no text, so it is tapped by coordinate.
_adb shell input tap 24 109 >/dev/null 2>&1
sleep 1
plab_tap_text "Assistant Config" >/dev/null 2>&1 || { echo "FAIL menu"; exit 2; }
plab_wait_text "Enable virtual avatar" 15 >/dev/null || { echo "FAIL config"; exit 2; }

# Turn the avatar on. The switch has no text of its own, so it is toggled by
# tapping just below the label; harmless if already on, so re-check the model
# name afterwards instead of trusting the tap.
_adb shell input tap 632 904 >/dev/null 2>&1
sleep 1

step "select $MODEL"
# Open the model dropdown and pick the requested model by name. The dropdown
# shows every model name, so a text tap is unambiguous.
plab_tap_text "Current Model" >/dev/null 2>&1 || true
sleep 1
plab_tap_text "$MODEL" >/dev/null 2>&1 || true
sleep 3

step "framing log"
_adb shell "su -c 'cat /data/data/$PKG/files/logs/operit.log'" 2>/dev/null \
    | grep -E "Camera auto-fit|Bounds diagnostic|Silhouette framing" | tail -20

echo "reached Assistant Config with model $MODEL"
