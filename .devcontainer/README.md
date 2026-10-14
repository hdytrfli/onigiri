# Findroid devcontainer (adb)

A lightweight container for debugging the app on a physical phone over **ADB over
Wi-Fi**. It has Android **platform-tools** (adb) and **JDK 17** but *not* the
full Android SDK — build the APK on the host with `bash build.sh` and use this
container to install / log / forward.

## Open it

VS Code → **Dev Containers: Reopen in Container** (needs the *Dev Containers*
extension and Docker running). The repo is mounted at `/work`.

The adb keys live in the `findroid-adb-keys` volume, so a phone that you paired
once stays trusted across container rebuilds.

## Pair over Wi-Fi (Android 11+)

1. On the phone: **Settings → Developer options → Wireless debugging → On**.
2. Tap **Pair device with pairing code**. Note the `IP:PAIR_PORT` and the
   6-digit code.
3. In the container:

   ```bash
   bash .devcontainer/connect-wifi.sh pair 192.168.1.42:37421 123456
   ```

4. Back on the **Wireless debugging** screen, note **IP address & Port** (a
   *different* port), then:

   ```bash
   bash .devcontainer/connect-wifi.sh connect 192.168.1.42:40001
   ```

5. `adb devices` should now list the phone.

> The connect port changes every time Wireless debugging is toggled, so step 4
> may need to be repeated (pairing is only needed once per network/reboot).

## Everyday commands

```bash
# install the APK you built on the host (staging app id: dev.jdtech.jellyfin.staging)
adb install -r docker-output/phone-libre-arm64-v8a-staging-*.apk

# logs
adb logcat

# clear app data / uninstall
adb shell pm clear dev.jdtech.jellyfin.staging
adb uninstall dev.jdtech.jellyfin.staging
```

## Storage & git

- The repo is **bind-mounted** at `/work`, so it *is* your host checkout — edits,
  build outputs and `docker-output/*.apk` are written to the host and survive
  container rebuilds.
- Everything outside `/work` (`/root`, `/opt`, `/tmp`, …) lives in the container
  and is **lost on rebuild**, except mounted volumes. The `findroid-adb-keys`
  volume keeps your adb keys.
- Git history is intact (`.git` is on the host via the mount). If `git` reports
  *"detected dubious ownership in repository"*, the container sets
  `safe.directory /work` automatically; verify with:

  ```bash
  git -C /work log --oneline -3
  ```

## Notes / troubleshooting

- The container must be able to reach the phone on the LAN. If `adb connect`
  times out, check that the host and phone are on the same network and that the
  phone's firewall allows it.
- If adb gets stuck: `bash .devcontainer/connect-wifi.sh reset`.
- Android Studio's own adb uses port 5037 on the host; this container runs its
  own adb server, so the two don't conflict.
- This container has no Android SDK on purpose. To build inside a container use
  the existing `Dockerfile` + `bash build.sh` instead.
