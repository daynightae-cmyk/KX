#!/usr/bin/env python3
"""
Verifies the claims NOTICE.md makes about the shipped APK, from the artifact itself.

This exists because a first draft of that note used
`unzip -l app-debug.apk | grep -i reference` as its check, which matched four
`META-INF/androidx.datastore_datastore-preferences*` entries — the substring
"reference" inside "preferences". The claim was true; the command proving it was not.
A verification step that produces false positives is worse than none, because it trains
the reader to accept the conclusion without looking.

Usage: python3 tools/verify_apk_asset_hygiene.py [path/to/app-debug.apk]
Exit code 0 = all checks passed.
"""
import re
import sys
import zipfile
from pathlib import Path

DEFAULT_APK = Path("app/build/outputs/apk/debug/app-debug.apk")

# The 11 owner-supplied design references, by exact basename.
REFERENCE_ASSETS = [
    "00-brand-logo.png", "01-welcome.png", "02-home-disconnected.png",
    "03-server-selection.png", "04-connected-session.png", "05-security-center.png",
    "06-split-tunneling.png", "07-advanced-network.png", "08-statistics.png",
    "09-import-configuration.png", "10-profile-settings.png",
]

# Framework-internal PNGs are expected and are matched by directory, not by name, so a
# real reference asset smuggled in under a renamed file would still trip the size check.
FRAMEWORK_RES_PREFIXES = ("res/",)

PRIVATE_KEY_PATTERNS = [
    rb"BEGIN (?:RSA |EC |OPENSSH |DSA |ENCRYPTED )?PRIVATE KEY",
    rb"preshared_key\s*=\s*[A-Za-z0-9+/]{32,}",
    rb"private_key\s*=\s*[A-Za-z0-9+/]{32,}",
]

# Reference PNGs are 1.3-1.8 MB each. No legitimate resource in this build is that big.
SUSPICIOUS_ENTRY_BYTES = 900_000


def check(apk: Path) -> int:
    if not apk.is_file():
        print(f"FAIL  {apk} not found. Build it first: ./gradlew :app:assembleDebug")
        return 1

    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        failures = []

        # 1. Reference artwork must not be shipped.
        for asset in REFERENCE_ASSETS:
            hits = [n for n in names if n.endswith("/" + asset) or n == asset]
            if hits:
                failures.append(f"reference asset shipped in APK: {hits}")

        # 2. No bundled font files (typography is the platform family).
        fonts = [n for n in names if re.search(r"\.(ttf|otf|ttc)$", n, re.I)]
        if fonts:
            failures.append(f"bundled font files present: {fonts}")

        # 3. Nothing in the APK may be a private key or preshared key.
        for name in names:
            if not (name.endswith(".txt") or name.endswith(".xml") or name.endswith(".pb")
                    or name.startswith("assets/")):
                continue
            data = archive.read(name)
            for pattern in PRIVATE_KEY_PATTERNS:
                if re.search(pattern, data):
                    failures.append(f"possible key material in {name}")

        # 4. A renamed reference asset would still show up as an oversized PNG.
        oversized = [
            (info.filename, info.file_size)
            for info in archive.infolist()
            if info.filename.lower().endswith(".png") and info.file_size > SUSPICIOUS_ENTRY_BYTES
        ]
        if oversized:
            failures.append(f"unexpectedly large PNG(s) shipped: {oversized}")

    print(f"APK under review: {apk}  ({apk.stat().st_size:,} bytes, {len(names)} entries)")
    if failures:
        for f in failures:
            print(f"FAIL  {f}")
        return 1

    print("PASS  no reference design asset is packaged")
    print("PASS  no bundled font file (platform sans only)")
    print("PASS  no private/preshared key material in text, xml, pb or assets entries")
    print(f"PASS  no PNG larger than {SUSPICIOUS_ENTRY_BYTES:,} bytes "
          "(a renamed reference asset would trip this)")
    return 0


if __name__ == "__main__":
    sys.exit(check(Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_APK))
