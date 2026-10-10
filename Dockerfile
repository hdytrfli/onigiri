# Local Docker build environment for Findroid (matches CI: Temurin JDK 17).
# NOTE: the public SDK repository does not publish platforms;android-37 yet
# (CI gets it preinstalled on the runner image), so this image aliases a copy
# of android-36 as android-37. The app targets SDK 36, so this is fine for
# local validation builds; CI remains the source of truth.
# Build:  docker build -t findroid-build .
# Run:    docker run --rm -v "$PWD:/work" -v findroid-gradle:/root/.gradle \
#             -w /work findroid-build ./gradlew :app:phone:assembleLibreDebug -PtargetAbi=arm64-v8a
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
    && cp -r $ANDROID_HOME/platforms/android-36 $ANDROID_HOME/platforms/android-37 \
    && sed -i 's/android-36/android-37/g' $ANDROID_HOME/platforms/android-37/package.xml

WORKDIR /work
