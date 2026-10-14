# Lightweight devcontainer for Findroid: Android platform-tools (adb) + JDK 17.
#
# This image intentionally does NOT include the full Android SDK (platforms /
# build-tools). Build the APK on the host with `bash build.sh` and use this
# container to install / log / forward to a device over ADB.
#
# ADB over Wi-Fi: see .devcontainer/README.md.
FROM eclipse-temurin:17-jdk-noble

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:/opt/platform-tools

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl unzip git iproute2 \
    && rm -rf /var/lib/apt/lists/* \
    && curl -fsSL -o /tmp/platform-tools.zip https://dl.google.com/android/repository/platform-tools-latest-linux.zip \
    && unzip -q /tmp/platform-tools.zip -d /opt \
    && rm /tmp/platform-tools.zip \
    && adb version

WORKDIR /work
