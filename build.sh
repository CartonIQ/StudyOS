#!/data/data/com.termux/files/usr/bin/bash
# Run from the StudyOS folder:  bash build.sh
set -e
cd "$(dirname "$0")"
SRC="${1:-$HOME/HelloAdMob}"
[ -f gradlew ] || cp "$SRC/gradlew" .
[ -f gradle/wrapper/gradle-wrapper.jar ] || cp "$SRC/gradle/wrapper/gradle-wrapper.jar" gradle/wrapper/
chmod +x gradlew
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug --no-daemon
APK=app/build/outputs/apk/debug/app-debug.apk
cp "$APK" /sdcard/Download/StudyOS-debug.apk 2>/dev/null && echo "Copied to Download/StudyOS-debug.apk"
echo "APK: $PWD/$APK"
