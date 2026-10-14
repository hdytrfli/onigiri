#!/usr/bin/env bash
#
# Build the Findroid phone APK with Docker, using a *long-lived* build container
# so the Gradle daemon and caches stay warm between builds (faster incremental
# builds). The result is copied into docker-output/ with a timestamp.
#
# Usage:
#   ./build.sh                          # staging, arm64-v8a
#   BUILD_TYPE=debug ./build.sh         # debug
#   TARGET_ABI=armeabi-v7a ./build.sh
#   ./build.sh --status                 # show the build container state
#   ./build.sh --stop                   # stop and remove the build container
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

ensure_container() {
    local running
    running="$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null || true)"
    if [[ "$running" != "true" ]]; then
        docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
        echo "==> Starting long-lived build container $CONTAINER"
        docker run -d --name "$CONTAINER" \
            -v "$REPO_DIR:/work" \
            -v findroid-gradle:/root/.gradle \
            -v findroid-android:/root/.android \
            -w /work \
            "$IMAGE" sleep infinity >/dev/null
    fi
}

case "${1:-}" in
    --stop)
        if docker rm -f "$CONTAINER" >/dev/null 2>&1; then
            echo "Stopped $CONTAINER"
        else
            echo "$CONTAINER is not running"
        fi
        exit 0
        ;;
    --status)
        docker ps -a --filter "name=^${CONTAINER}$" \
            --format 'table {{.Names}}\t{{.Status}}\t{{.Image}}'
        exit 0
        ;;
esac

# Capitalise the build type for the Gradle task name (debug -> Debug).
TASK_TYPE="$(printf '%s' "${BUILD_TYPE:0:1}" | tr '[:lower:]' '[:upper:]')${BUILD_TYPE:1}"

ensure_image
ensure_container

echo "==> Building :app:phone:assembleLibre${TASK_TYPE} (abi=$ABI, type=$BUILD_TYPE)"
docker exec -w /work "$CONTAINER" \
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
