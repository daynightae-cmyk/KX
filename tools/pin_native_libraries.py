#!/usr/bin/env python3
"""
Pin and verify the prebuilt native libraries inside the WireGuard backend AAR.

The owner chose a repackaged WireGuard Android backend rather than building the Go
tunnel from source, because this machine has no Go and no NDK. That choice ships
binary-only `.so` files that nobody here has audited. The compensating control is
this script: CI recomputes every native library hash from the resolved artifact and
fails if anything differs from `docs/native-library-lock.md`.

Usage:
    python3 tools/pin_native_libraries.py            # verify against the lock file
    python3 tools/pin_native_libraries.py --write    # regenerate the lock file
Exit 0 = matches (or written). Exit 1 = drift, or the AAR could not be located.
"""
import hashlib
import os
import re
import sys
from pathlib import Path

GROUP = "com/zaneschepke"
NAME = "wireguard-android"
VERSION = "1.2.1"
LOCK = Path("docs/native-library-lock.md")

ROW = re.compile(r"^\| `(jni/[^`]+)` \| (\d+) \| `([0-9a-f]{64})` \|$")


def cache_roots() -> list[Path]:
    """
    Every place Gradle or Maven may have put the artifact.

    GRADLE_USER_HOME must be honoured: it is an ordinary way to relocate the cache
    (this project's dev machine sets it to a different drive), and a first version of
    this tool searched only `~/.gradle`. It found nothing and reported "could not locate
    the AAR" even though the dependency had just resolved successfully — a gate that
    fails for the wrong reason is a gate nobody trusts.
    """
    roots: list[Path] = []
    gradle_home = os.environ.get("GRADLE_USER_HOME")
    if gradle_home:
        roots.append(Path(gradle_home) / "caches" / "modules-2")
    roots.append(Path.home() / ".gradle" / "caches" / "modules-2")
    maven_home = os.environ.get("MAVEN_REPO_LOCAL")
    if maven_home:
        roots.append(Path(maven_home))
    roots.append(Path.home() / ".m2" / "repository")
    return [root for root in roots if root.is_dir()]


def find_aar() -> Path | None:
    for base in cache_roots():
        for candidate in base.rglob(f"{NAME}-{VERSION}.aar"):
            if candidate.is_file():
                return candidate
    return None


def entries(aar: Path) -> list[tuple[str, int, str]]:
    import zipfile

    out = []
    with zipfile.ZipFile(aar) as archive:
        for info in archive.infolist():
            if not info.filename.startswith("jni/") or not info.filename.endswith(".so"):
                continue
            if info.is_dir():
                continue
            digest = hashlib.sha256(archive.read(info.filename)).hexdigest()
            out.append((info.filename, info.file_size, digest))
    out.sort()
    return out


def read_lock() -> dict[str, tuple[int, str]]:
    if not LOCK.is_file():
        return {}
    table = {}
    for line in LOCK.read_text(encoding="utf-8").splitlines():
        match = ROW.match(line.strip())
        if match:
            table[match.group(1)] = (int(match.group(2)), match.group(3))
    return table


def write_lock(aar: Path, rows: list[tuple[str, int, str]]) -> None:
    lines = [
        "# Native library pin for " + f"{GROUP}:{NAME}:{VERSION}".replace("/", ":"),
        "",
        "These are the prebuilt Go/WireGuard `.so` blobs that ship inside the AAR. They",
        "are binary-only: no source for them is in this repository and none has been",
        "audited here. The owner chose this repackaged backend deliberately, so the",
        "compensating control is that CI fails if any byte of any native library differs",
        "from what was recorded on 2026-11-03.",
        "",
        "Regenerate with: `python3 tools/pin_native_libraries.py --write`",
        "",
        "| path | bytes | sha256 |",
        "|---|---|---|",
    ]
    for path, size, digest in rows:
        lines.append(f"| `{path}` | {size} | `{digest}` |")
    lines += [
        "",
        f"AAR sha256: `{hashlib.sha256(aar.read_bytes()).hexdigest()}`",
        "",
    ]
    LOCK.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    write = "--write" in sys.argv
    aar = find_aar()
    if aar is None:
        print(
            "FAIL  could not locate the resolved AAR in the Gradle or Maven cache.\n"
            "      Run a build first so the artifact is downloaded, e.g.:\n"
            "        ./gradlew :app:assembleDebug",
        )
        return 1

    rows = entries(aar)
    if not rows:
        print("FAIL  the AAR contains no jni/*.so entries; the artifact changed shape")
        return 1

    if write:
        write_lock(aar, rows)
        print(f"WROTE {LOCK} with {len(rows)} pinned native libraries")
        return 0

    expected = read_lock()
    if not expected:
        print(f"FAIL  {LOCK} is missing or empty; run with --write after review")
        return 1

    problems = []
    for path, size, digest in rows:
        if path not in expected:
            problems.append(f"NEW native library not in the lock file: {path}")
        else:
            lock_size, lock_digest = expected[path]
            if lock_digest != digest:
                problems.append(f"HASH DRIFT {path}: locked {lock_digest} now {digest}")
            elif lock_size != size:
                problems.append(f"SIZE DRIFT {path}: locked {lock_size} now {size}")
    for path in expected:
        if path not in {row[0] for row in rows}:
            problems.append(f"REMOVED native library that was pinned: {path}")

    if problems:
        print(f"FAIL  native library drift detected ({len(problems)} finding(s)):")
        for problem in problems:
            print(f"  - {problem}")
        return 1

    print(f"PASS  {len(rows)} native libraries match the lock file exactly")
    return 0


if __name__ == "__main__":
    sys.exit(main())
