#!/usr/bin/env bash
#
# Publishes one on-demand payload file to the fork's own release.
#
# The app downloads payloads by tag from `util/OfflinePayloads.kt`; the tag and
# asset name must match the RemoteAsset url exactly. Payload archives are never
# tracked in git, so this is the only way one reaches a device.
#
# Usage:
#   ci/script/publish_payload_release.sh <tag> <file> [title]
#
# Example (terminal rootfs):
#   ci/script/publish_payload_release.sh terminal-rootfs-v1 \
#       /tmp/ubuntu-noble-aarch64-pd-v4.18.0.tar.xz
#
# Prints the byte size and sha256 that must be copied into OfflinePayloads.kt.

set -euo pipefail

REPO="${PAYLOAD_REPO:-lagovi/Operit}"
TAG="${1:?usage: publish_payload_release.sh <tag> <file> [title]}"
FILE="${2:?usage: publish_payload_release.sh <tag> <file> [title]}"
TITLE="${3:-$TAG}"
NAME="$(basename "$FILE")"

[ -f "$FILE" ] || { echo "no such file: $FILE" >&2; exit 1; }

BYTES="$(stat -c %s "$FILE")"
SHA="$(sha256sum "$FILE" | cut -d' ' -f1)"
echo "asset:  $NAME"
echo "bytes:  $BYTES"
echo "sha256: $SHA"

NOTE="$(cat <<EOF
> **ALPHA.** This fork is 100% vibe-coded (written with AI assistance) and every release here is ALPHA: expect rough edges and breaking changes. Stable upstream: [AAswordman/Operit](https://github.com/AAswordman/Operit).

On-demand payload for the English-only fork (Q2 size work). The APK no longer bundles it; the app downloads it once and verifies sha256. File: $NAME ($BYTES B, sha256 $SHA).
EOF
)"

if gh release view "$TAG" -R "$REPO" >/dev/null 2>&1; then
  gh release upload "$TAG" "$FILE" -R "$REPO" --clobber
else
  gh release create "$TAG" "$FILE" -R "$REPO" --title "$TITLE" --notes "$NOTE" --prerelease
fi
