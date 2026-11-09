#!/data/data/com.termux/files/usr/bin/bash
set -e
GRADLE_BIN="/data/data/com.termux/files/home/.gradle/wrapper/dists/gradle-8.7-bin/bhs2wmbdwecv87pi65oeuq5iu/gradle-8.7/bin/gradle"
export ANDROID_HOME="/data/data/com.termux/files/home/android-sdk"
export ANDROID_SDK_ROOT="/data/data/com.termux/files/home/android-sdk"
exec "$GRADLE_BIN" "$@"
