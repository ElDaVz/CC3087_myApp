#!/usr/bin/env bash
set -euo pipefail

# A dedicated AVD prevents replacing an existing real-project installation.
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"
if (( $# )); then
    echo "Usage: scripts/run-firebase-local.sh (no build overrides)" >&2
    exit 1
fi
if [[ -f app/google-services.json ]]; then
    echo "Keep the real google-services.json outside this emulator-only checkout." >&2
    exit 1
fi
sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
adb="$sdk_dir/platform-tools/adb"
[[ -x "$adb" ]] || { echo "Set ANDROID_HOME to your Android SDK." >&2; exit 1; }
export ANDROID_SERIAL="${ANDROID_SERIAL:-emulator-5580}"
avd_name="$("$adb" -s "$ANDROID_SERIAL" emu avd name | head -n 1 | tr -d '\r')"
if [[ "$avd_name" != FormLink_Local_API_37 ]]; then
    echo "Refusing to install: select the dedicated FormLink_Local_API_37 AVD." >&2
    exit 1
fi
for port in 9099 8080; do
    if ! (echo > "/dev/tcp/127.0.0.1/$port") 2>/dev/null; then
        echo "Start npm run emulators --prefix backend-tests (demo-formlink); port $port is unavailable." >&2
        exit 1
    fi
done
"$adb" -s "$ANDROID_SERIAL" shell am force-stop com.uvg.cc3087.myapp
./gradlew installDebug -PfirebaseEmulators=true -PtelemetryEnabled=false \
    --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process
"$adb" -s "$ANDROID_SERIAL" shell am force-stop com.uvg.cc3087.myapp
"$adb" -s "$ANDROID_SERIAL" shell am start -W -n com.uvg.cc3087.myapp/.MainActivity
