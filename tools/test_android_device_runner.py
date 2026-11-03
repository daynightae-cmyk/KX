"""Exit-status/evidence regression only; stubs do not constitute Android proof."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


class AndroidDeviceRunnerTest(unittest.TestCase):
    def test_failure_statuses_and_evidence_are_preserved(self):
        script = Path(__file__).with_name("run_android_device_checks.sh").resolve()
        with tempfile.TemporaryDirectory(prefix="kx-runner-regression-") as temp:
            root = Path(temp)
            gradle = root / "gradle"
            gradle.write_text('#!/bin/sh\nexit "${TEST_EXIT:-0}"\n')
            gradle.chmod(0o755)
            adb = root / "adb"
            adb.write_text('''#!/bin/sh
case "$1" in
 logcat) echo test-crash-log; exit "${LOG_EXIT:-0}";;
 shell) exit "${DIR_EXIT:-0}";;
 pull) mkdir -p "$3"; printf test-png > "$3/capture.png"; exit "${PULL_EXIT:-0}";;
esac
exit 9
''')
            adb.chmod(0o755)
            cases = [
                ({}, 0),
                ({"TEST_EXIT": "7"}, 7),
                ({"TEST_EXIT": "7", "LOG_EXIT": "8"}, 7),
                ({"LOG_EXIT": "8"}, 8),
                ({"PULL_EXIT": "6"}, 6),
                ({"DIR_EXIT": "1"}, 1),
                ({"KX_APP_ID": "invalid/path"}, 2),
            ]
            for index, (overrides, expected) in enumerate(cases):
                with self.subTest(overrides=overrides):
                    evidence = root / f"evidence-{index}"
                    env = dict(os.environ, KX_GRADLEW=str(gradle), KX_ADB=str(adb),
                               KX_DEVICE_EVIDENCE_DIR=str(evidence))
                    env.update(overrides)
                    result = subprocess.run(["bash", str(script)], env=env,
                                            capture_output=True, text=True, timeout=10)
                    self.assertEqual(expected, result.returncode, result.stderr)
                    if expected != 2:
                        self.assertEqual(overrides.get("TEST_EXIT", "0"),
                                         (evidence / "test-exit-code.txt").read_text().strip())
                        self.assertTrue((evidence / "crash-logcat.txt").exists())


if __name__ == "__main__":
    unittest.main()
