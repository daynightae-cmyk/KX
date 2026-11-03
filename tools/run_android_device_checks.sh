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
if (( test_status != 0 )); then
    # Diagnostics only: never mask, retry or convert a failed test to success.
    python3 tools/summarize_android_test_failures.py || echo "Failure summary unavailable; original test failure preserved."
fi

# Always attempt both collectors, including when tests fail. A collector failure
# must not erase the original test exit status or turn it into a green job.
collection_status=0
"$adb" logcat -d -b crash > "$evidence/crash-logcat.txt" 2>&1 || collection_status=$?
remote="/sdcard/Android/data/$app_id/files/Pictures"
if "$adb" shell test -d "$remote"; then
    "$adb" pull "$remote" "$evidence/runtime-screenshots" || collection_status=$?
else
    # Gradle's connectedDebugAndroidTest may uninstall the target app (and its
    # external screenshots) after testing. Never pretend the original screenshots
    # survived. Install the verified debug APK again and record a separately
    # labeled real framework screenshot of the post-test launch instead.
    echo "Instrumentation screenshot directory absent; collecting a fresh post-test launch."
    apk="app/build/outputs/apk/debug/app-debug.apk"
    screenshot_dir="$evidence/runtime-screenshots"
    screenshot="$screenshot_dir/post-test-launch.png"
    if [[ ! -s "$apk" ]]; then
        echo "Missing built debug APK: $apk" >&2
        collection_status=1
    elif ! "$adb" install -r "$apk"; then
        echo "Reinstallation for post-test visual evidence failed" >&2
        collection_status=1
    elif ! "$adb" shell am start -W -n "$app_id/.MainActivity"; then
        echo "Cannot launch app for visual evidence" >&2
        collection_status=1
    else
        sleep 3
        mkdir -p "$screenshot_dir"
        if ! "$adb" exec-out screencap -p > "$screenshot"; then
            echo "ADB framebuffer screenshot failed" >&2
            collection_status=1
        elif ! python3 - "$screenshot" <<'PY'
from pathlib import Path
import struct
import sys
payload = Path(sys.argv[1]).read_bytes()
if len(payload) < 100 or payload[:8] != bytes((137, 80, 78, 71, 13, 10, 26, 10)):
    raise SystemExit("missing or invalid screenshot PNG")
width, height = struct.unpack(">II", payload[16:24])
if width < 200 or height < 300:
    raise SystemExit(f"unexpected screenshot dimensions: {width}x{height}")
print(f"Verified actual post-test emulator screenshot: {width}x{height}")
PY
        then
            collection_status=1
        else
            printf '%s\n' "Captured with adb screencap after re-installing/launched actual app; NOT the instrumentation-time screenshot." > "$evidence/screenshot-provenance.txt"
        fi
    fi
fi
printf '%s\n' "$collection_status" > "$evidence/collection-exit-code.txt"

if (( test_status != 0 )); then
    exit "$test_status"
fi
exit "$collection_status"
