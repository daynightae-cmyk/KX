#!/usr/bin/env bash
# Call as ONE emulator-runner script command: bash tools/run_android_device_checks.sh
# The runner executes separate YAML script lines in separate shells, so shell
# variables and multiline if blocks must live inside this file instead.
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

gradlew="${KX_GRADLEW:-./gradlew}"
adb="${KX_ADB:-adb}"
app_id="${KX_APP_ID:-com.kx.vpn}"
evidence="${KX_DEVICE_EVIDENCE_DIR:-app/build/device-evidence}"
if [[ ! "$app_id" =~ ^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$ ]]; then
    echo "Invalid application ID" >&2
    exit 2
fi
mkdir -p "$evidence" || exit 2

test_status=0
"$gradlew" --no-daemon --stacktrace connectedDebugAndroidTest || test_status=$?
printf '%s\n' "$test_status" > "$evidence/test-exit-code.txt"

# Always attempt both collectors, including when tests fail. A collector failure
# must not erase the original test exit status or turn it into a green job.
collection_status=0
"$adb" logcat -d -b crash > "$evidence/crash-logcat.txt" 2>&1 || collection_status=$?
remote="/sdcard/Android/data/$app_id/files/Pictures"
if "$adb" shell test -d "$remote"; then
    "$adb" pull "$remote" "$evidence/runtime-screenshots" || collection_status=$?
else
    echo "Runtime screenshot directory unavailable; visual evidence is incomplete." >&2
    collection_status=1
fi
printf '%s\n' "$collection_status" > "$evidence/collection-exit-code.txt"

if (( test_status != 0 )); then
    exit "$test_status"
fi
exit "$collection_status"
