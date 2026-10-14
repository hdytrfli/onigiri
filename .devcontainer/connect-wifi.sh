#!/usr/bin/env bash
#
# Connect this devcontainer's adb to a phone over Wi-Fi (Android 11+ "Wireless
# debugging"). Run it inside the devcontainer.
#
#   1. Phone: Settings > Developer options > Wireless debugging > On.
#   2. Tap "Pair device with pairing code": note IP:PAIR_PORT and the 6-digit code.
#   3. bash .devcontainer/connect-wifi.sh pair <ip>:<pair-port> <code>
#   4. Back on the Wireless debugging screen note the "IP address & Port"
#      (a different port), then:
#      bash .devcontainer/connect-wifi.sh connect <ip>:<connect-port>
#
# After that `adb devices` should list the phone and you can run, e.g.:
#   adb install -r docker-output/phone-libre-arm64-v8a-staging-*.apk
#   adb logcat
set -euo pipefail

usage() {
    cat <<'EOF'
Usage:
  connect-wifi.sh pair    <ip>:<pair-port> [code]
  connect-wifi.sh connect <ip>:<connect-port>
  connect-wifi.sh devices
  connect-wifi.sh reset
EOF
}

adb start-server >/dev/null

case "${1:-}" in
    pair)
        [[ $# -ge 2 ]] || { usage; exit 1; }
        if [[ $# -ge 3 ]]; then
            adb pair "$2" "$3"
        else
            adb pair "$2"
        fi
        ;;
    connect)
        [[ $# -ge 2 ]] || { usage; exit 1; }
        adb connect "$2"
        ;;
    devices)
        adb devices -l
        exit 0
        ;;
    reset)
        adb kill-server
        adb start-server
        ;;
    *)
        usage
        exit 1
        ;;
esac

echo
adb devices -l
