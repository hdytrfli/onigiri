#!/usr/bin/env bash
#
# Build the Findroid phone APK with Docker and copy the result into
# docker-output/ with a timestamp.
#
# Usage:
#   ./build.sh                          # staging, arm64-v8a
#   BUILD_TYPE=debug ./build.sh         # debug
#   TARGET_ABI=armeabi-v7a ./build.sh
#
# Build types: staging (default; app id .staging, minified, signed with the
# debug keystore), debug (app id .debug), release (app id dev.jdtech.jellyfin,
# minified, unsigned).
#
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE="${FINDOID_BUILD_IMAGE:-findroid-build}"
ABI="${TARGET_ABI:-arm64-v8a}"
BUILD_TYPE="${BUILD_TYPE:-staging}"
OUTPUT_DIR="$REPO_DIR/docker-output"

# Capitalise the build type for the Gradle task name (debug -> Debug).
TASK_TYPE="$(printf '%s' "${BUILD_TYPE:0:1}" | tr '[:lower:]' '[:upper:]')${BUILD_TYPE:1}"

# Git-Bash on Windows rewrites container paths; disable that.
export MSYS_NO_PATHCONV=1

echo "==> Building :app:phone:assembleLibre${TASK_TYPE} (abi=$ABI, type=$BUILD_TYPE)"
docker run --rm \
    -v "$REPO_DIR:/work" \
    -v findroid-gradle:/root/.gradle \
    -v findroid-android:/root/.android \
    -w /work \
    "$IMAGE" \
    ./gradlew ktfmtFormat ":app:phone:assembleLibre${TASK_TYPE}" -PtargetAbi="$ABI" "$@"

APK_SRC="$REPO_DIR/app/phone/build/outputs/apk/libre/${BUILD_TYPE}/phone-libre-${ABI}-${BUILD_TYPE}.apk"
if [[ ! -f "$APK_SRC" ]]; then
    # Release builds without a signing config are named "-unsigned".
    ALT_SRC="${APK_SRC%.apk}-unsigned.apk"
    if [[ -f "$ALT_SRC" ]]; then
        APK_SRC="$ALT_SRC"
    else
        echo "error: APK not found at $APK_SRC" >&2
        exit 1
    fi
fi

STAMP="$(date +%Y%m%d-%H%M)"
APK_DST="$OUTPUT_DIR/phone-libre-${ABI}-${BUILD_TYPE}-${STAMP}.apk"

mkdir -p "$OUTPUT_DIR"
cp "$APK_SRC" "$APK_DST"

echo "==> APK: $APK_DST"
