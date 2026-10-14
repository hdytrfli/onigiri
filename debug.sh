#!/usr/bin/env bash
#
# Build (in Docker) and deploy/debug on a USB-connected device WITHOUT installing
# the Android SDK or a JDK on the host.
#
# The build already runs in Docker via build.sh. For USB you only need `adb`,
# which this script downloads as a portable copy into .tools/ (gitignored) if it
# isn't already on your PATH.
#
# Usage:
#   ./debug.sh build              build the staging APK (Docker) and install it
#   ./debug.sh install [apk]      install the newest staging APK (or a given path)
#   ./debug.sh push               copy the newest APK to /sdcard/Download (for MIUI)
#   ./debug.sh logcat [args...]   follow the log (defaults to the app's pid)
#   ./debug.sh launch             force-stop then launch the app
#   ./debug.sh devices            list connected devices
#   ./debug.sh shell [cmd...]     open a device shell
#   ./debug.sh clear              clear the app's data
#   ./debug.sh uninstall          uninstall the app
#
# USB note: Docker on Windows can't access a USB device, so adb has to run on the
# host. This script keeps that to a portable download only.
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOOLS_DIR="$REPO_DIR/.tools"
APP_ID="dev.jdtech.jellyfin.staging"

# --- locate (or fetch) adb --------------------------------------------------
if command -v adb >/dev/null 2>&1; then
    ADB="$(command -v adb)"
else
    case "$(uname -s)" in
        MINGW* | MSYS* | CYGWIN*) PT_ZIP="platform-tools-latest-windows.zip"; PT_BIN="adb.exe" ;;
        Darwin) PT_ZIP="platform-tools-latest-darwin.zip"; PT_BIN="adb" ;;
        *) PT_ZIP="platform-tools-latest-linux.zip"; PT_BIN="adb" ;;
    esac
    ADB="$TOOLS_DIR/platform-tools/$PT_BIN"
    if [[ ! -f "$ADB" ]]; then
        echo "==> Downloading Android platform-tools (adb only) into .tools/"
        mkdir -p "$TOOLS_DIR"
        curl -fL --progress-bar -o "$TOOLS_DIR/$PT_ZIP" \
            "https://dl.google.com/android/repository/$PT_ZIP"
        if command -v unzip >/dev/null 2>&1; then
            unzip -q -o "$TOOLS_DIR/$PT_ZIP" -d "$TOOLS_DIR"
        elif command -v powershell >/dev/null 2>&1; then
            powershell -NoProfile -Command \
                "Expand-Archive -Force '$TOOLS_DIR/$PT_ZIP' '$TOOLS_DIR'"
        else
            tar -xf "$TOOLS_DIR/$PT_ZIP" -C "$TOOLS_DIR"
        fi
        rm -f "$TOOLS_DIR/$PT_ZIP"
        [[ -f "$ADB" ]] || { echo "error: adb not found at $ADB" >&2; exit 1; }
    fi
fi

adb() { "$ADB" "$@"; }

# Git-Bash passes /c/... paths that a Windows adb.exe can't read; convert when we can.
host_path() {
    if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else printf '%s' "$1"; fi
}

newest_apk() {
    ls -t "$REPO_DIR"/docker-output/phone-libre-*-staging-*.apk 2>/dev/null | head -1
}

install_apk() {
    local path="${1:-}"
    [[ -n "$path" ]] || path="$(newest_apk)"
    [[ -n "$path" && -f "$path" ]] || {
        echo "error: no APK found — run './debug.sh build' first" >&2
        exit 1
    }
    echo "==> Installing $path"
    adb install -r "$(host_path "$path")"
}

adb start-server >/dev/null

case "${1:-}" in
    build)
        bash "$REPO_DIR/build.sh" -x ktfmtFormatScripts
        install_apk
        ;;
    install)
        install_apk "${2:-}"
        ;;
    logcat)
        shift || true
        if [[ $# -eq 0 ]]; then
            pid="$(adb shell pidof -s "$APP_ID" | tr -d '\r')"
            if [[ -n "$pid" ]]; then
                adb logcat --pid="$pid"
            else
                adb logcat
            fi
        else
            adb logcat "$@"
        fi
        ;;
    launch)
        adb shell am force-stop "$APP_ID"
        adb shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 >/dev/null
        echo "==> Launched $APP_ID"
        ;;
    devices) adb devices -l ;;
    push)
        apk="$(newest_apk)"
        [[ -n "$apk" && -f "$apk" ]] || {
            echo "error: no APK found — run './debug.sh build' first" >&2
            exit 1
        }
        echo "==> Pushing $(basename "$apk") to /sdcard/Download/"
        MSYS_NO_PATHCONV=1 adb push "$(host_path "$apk")" /sdcard/Download/
        echo "==> Now open it from the tablet's Files app (Download) to install."
        echo "    (MIUI blocks adb installs, so this manual step is needed.)"
        ;;
    shell)
        shift || true
        adb shell "$@"
        ;;
    clear) adb shell pm clear "$APP_ID" ;;
    uninstall) adb uninstall "$APP_ID" ;;
    *)
        sed -n '2,22p' "$0"
        exit 1
        ;;
esac
