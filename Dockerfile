# Local Docker build environment for Findroid (matches CI: Temurin JDK 17).
# platforms;android-37 is only published on the canary channel, so it is
# installed explicitly here (otherwise AGP re-downloads it into the throwaway
# container on every build).
# Build:  docker build -t findroid-build .
# Run:    bash build.sh
# The findroid-android volume persists the container's debug keystore, so all
# local builds share one signature and install as updates (app data kept).
FROM eclipse-temurin:17-jdk-noble

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

RUN apt-get update && apt-get install -y --no-install-recommends unzip curl git \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p $ANDROID_HOME/cmdline-tools \
    && curl -o /tmp/cmdtools.zip https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip \
    && unzip -q /tmp/cmdtools.zip -d $ANDROID_HOME/cmdline-tools \
    && mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest \
    && rm /tmp/cmdtools.zip \
    && yes | sdkmanager --licenses \
    && sdkmanager "platform-tools" "platforms;android-36" "build-tools;37.0.0" \
    && sdkmanager --channel=3 "platforms;android-37.0"

WORKDIR /work
