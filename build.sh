#!/usr/bin/env bash
#
# Build the Findroid debug APK (phone) with Docker and copy the result into
# docker-output/ with a timestamp.
#
# Usage:
#   ./build.sh                 # arm64-v8a (default)
#   TARGET_ABI=armeabi-v7a ./build.sh
#
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE="${FINDOID_BUILD_IMAGE:-findroid-build}"
ABI="${TARGET_ABI:-arm64-v8a}"
OUTPUT_DIR="$REPO_DIR/docker-output"

# Git-Bash on Windows rewrites container paths; disable that.
export MSYS_NO_PATHCONV=1

echo "==> Building :app:phone:assembleLibreDebug (abi=$ABI)"
docker run --rm \
    -v "$REPO_DIR:/work" \
    -v findroid-gradle:/root/.gradle \
    -v findroid-android:/root/.android \
    -w /work \
    "$IMAGE" \
    ./gradlew ktfmtFormat ktfmtCheck :app:phone:assembleLibreDebug -PtargetAbi="$ABI" "$@"

APK_SRC="$REPO_DIR/app/phone/build/outputs/apk/libre/debug/phone-libre-${ABI}-debug.apk"
if [[ ! -f "$APK_SRC" ]]; then
    echo "error: APK not found at $APK_SRC" >&2
    exit 1
fi

STAMP="$(date +%Y%m%d-%H%M)"
APK_DST="$OUTPUT_DIR/phone-libre-${ABI}-debug-${STAMP}.apk"

mkdir -p "$OUTPUT_DIR"
cp "$APK_SRC" "$APK_DST"

echo "==> APK: $APK_DST"
