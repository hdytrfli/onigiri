#!/usr/bin/env bash
#
# Build the Findroid phone APK with Docker using a throwaway build container.
#
# The container runs the Gradle build as its main process, so the full build
# output goes to the container's log and is visible with:
#
#   docker logs -f findroid-build-container      # live, from another terminal
#   docker logs findroid-build-container         # the last build, after exit
#
# Nothing is re-downloaded between builds: the Android SDK lives in the image,
# the Gradle/Kotlin caches live in the named volumes `findroid-gradle` and
# `findroid-android`, and the repo (with its build/ dirs) is bind-mounted. The
# container itself is recreated every run, so no stale state can accumulate.
#
# Usage:
#   ./build.sh                          # staging, arm64-v8a
#   BUILD_TYPE=debug ./build.sh         # debug
#   TARGET_ABI=armeabi-v7a ./build.sh
#   ./build.sh -x ktfmtFormatScripts    # extra args are passed to Gradle
#
# Build types: staging (default; app id .staging, minified, signed with the
# debug keystore), debug (app id .debug), release (app id dev.jdtech.jellyfin,
# minified, unsigned).
#
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE="${FINDOID_BUILD_IMAGE:-findroid-build}"
CONTAINER="${FINDOID_BUILD_CONTAINER:-findroid-build-container}"
ABI="${TARGET_ABI:-arm64-v8a}"
BUILD_TYPE="${BUILD_TYPE:-staging}"
OUTPUT_DIR="$REPO_DIR/docker-output"

# Git-Bash on Windows rewrites container paths; disable that.
export MSYS_NO_PATHCONV=1

ensure_image() {
    if ! docker image inspect "$IMAGE" >/dev/null 2>&1; then
        echo "==> Building image $IMAGE"
        docker build -t "$IMAGE" "$REPO_DIR"
    fi
}

# Capitalise the build type for the Gradle task name (debug -> Debug).
TASK_TYPE="$(printf '%s' "${BUILD_TYPE:0:1}" | tr '[:lower:]' '[:upper:]')${BUILD_TYPE:1}"

ensure_image

# Recreate the build container so its log only holds this run's output.
docker rm -f "$CONTAINER" >/dev/null 2>&1 || true

echo "==> Building :app:phone:assembleLibre${TASK_TYPE} (abi=$ABI, type=$BUILD_TYPE)"
echo "    logs: docker logs -f $CONTAINER"
docker run \
    --name "$CONTAINER" \
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
