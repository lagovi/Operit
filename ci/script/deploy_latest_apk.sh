#!/usr/bin/env bash
# Download the APK from the latest successful "Android Build" run and install
# it to the connected device via adb.
# Usage: ci/script/deploy_latest_apk.sh [run_id]
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT_DIR="${1:-}"
RUN_ID="${2:-}"

cd "$REPO_ROOT"

if [[ -z "$RUN_ID" ]]; then
    RUN_ID="$(gh run list --workflow "Android Build" --status success --limit 1 --json databaseId --jq '.[0].databaseId')"
fi
if [[ -z "$RUN_ID" ]]; then
    echo "no successful Android Build run found" >&2
    exit 1
fi
echo "[*] run: $RUN_ID"

if [[ -z "$OUT_DIR" ]]; then
    OUT_DIR="$HOME/operit-fork/apk/run-$RUN_ID"
fi
mkdir -p "$OUT_DIR"
gh run download "$RUN_ID" --pattern 'operit-android-*' -D "$OUT_DIR"

APK="$(find "$OUT_DIR" -name '*.apk' | head -1)"
if [[ -z "$APK" ]]; then
    echo "no apk in artifact" >&2
    exit 1
fi
echo "[*] installing: $APK"
adb install -r "$APK"
echo "[*] done"
