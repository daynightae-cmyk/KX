#!/usr/bin/env python3
"""
Find the newest AndroidX stack that still fits the locally installed platform.

AGP 8.13.2 supports compileSdk 36 at most, and each AndroidX AAR declares its own
requirements in META-INF/com/android/build/gradle/aar-metadata.properties as
`minCompileSdk` and `minAndroidGradlePluginVersion`. This reads those values directly
instead of inferring them from release notes.

Usage: python3 tools/probe_min_compile_sdk.py [max_compile_sdk] [max_agp]
"""
import io
import re
import sys
import urllib.request
import zipfile
from concurrent.futures import ThreadPoolExecutor

GOOGLE = "https://dl.google.com/android/maven2/"
METADATA = "META-INF/com/android/build/gradle/aar-metadata.properties"


def fetch(url: str) -> bytes:
    with urllib.request.urlopen(url, timeout=60) as response:
        return response.read()


def read_metadata(group: str, name: str, version: str) -> dict:
    path = f"{group.replace('.', '/')}/{name}/{version}/{name}-{version}.aar"
    try:
        payload = fetch(GOOGLE + path)
    except Exception as error:  # noqa: BLE001
        return {"error": f"{type(error).__name__}: {error}"}
    try:
        with zipfile.ZipFile(io.BytesIO(payload)) as archive:
            raw = archive.read(METADATA).decode("utf-8", "replace")
    except KeyError:
        # No AAR metadata at all: the artifact imposes no compileSdk floor.
        return {"minCompileSdk": 0, "minAndroidGradlePluginVersion": "0"}
    values = {}
    for line in raw.splitlines():
        if "=" not in line:
            continue
        # Exact key match. A startswith() test also catches minCompileSdkExtension
        # and silently overwrites the value we actually care about.
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def version_key(version: str):
    parts = re.findall(r"\d+", version)
    return tuple(int(p) for p in parts[:4]) + (0,) * (4 - len(parts[:4]))


def bom_ui_version(bom_version: str) -> str | None:
    """Compose BOMs are dependency-Management POMs; find the ui version they pin."""
    url = (
        f"{GOOGLE}androidx/compose/compose-bom/{bom_version}/"
        f"compose-bom-{bom_version}.pom"
    )
    pom = fetch(url).decode("utf-8", "replace")
    match = re.search(
        r"<artifactId>ui</artifactId>\s*<version>([^<]+)</version>", pom
    )
    return match.group(1) if match else None


def check(label: str, group: str, aar_name: str, version: str,
          max_sdk: int, max_agp: tuple) -> tuple:
    meta = read_metadata(group, aar_name, version)
    if "error" in meta:
        return (label, version, None, None, False, meta["error"])
    sdk = int(meta.get("minCompileSdk", "0"))
    agp = meta.get("minAndroidGradlePluginVersion", "0")
    agp_tuple = tuple(int(p) for p in re.findall(r"\d+", agp)[:3])
    agp_tuple = agp_tuple + (0,) * (3 - len(agp_tuple))
    fits = sdk <= max_sdk and agp_tuple <= max_agp
    return (label, version, sdk, agp, fits, "")


def main() -> int:
    max_sdk = int(sys.argv[1]) if len(sys.argv) > 1 else 36
    max_agp_raw = sys.argv[2] if len(sys.argv) > 2 else "8.13.2"
    max_agp = tuple(int(p) for p in max_agp_raw.split("."))

    targets = [
        ("androidx.core:core", "androidx.core", "core",
         ["1.13.1", "1.15.0", "1.16.0", "1.17.0", "1.18.0", "1.19.0"]),
        ("androidx.lifecycle:lifecycle-runtime-android", "androidx.lifecycle",
         "lifecycle-runtime-android",
         ["2.8.7", "2.9.0", "2.9.4", "2.10.0", "2.11.0"]),
        ("androidx.activity:activity", "androidx.activity", "activity",
         ["1.9.3", "1.10.1", "1.11.0", "1.12.0", "1.13.0"]),
        ("androidx.navigation:navigation-compose", "androidx.navigation",
         "navigation-compose", ["2.8.9", "2.9.0", "2.10.0", "2.10.2"]),
        ("androidx.datastore:datastore-preferences", "androidx.datastore",
         "datastore-preferences", ["1.1.7", "1.2.0", "1.3.0"]),
        ("androidx.core:core-splashscreen", "androidx.core", "core-splashscreen",
         ["1.0.1", "1.2.0"]),
        ("androidx.test:monitor", "androidx.test", "monitor",
         ["1.7.1", "1.7.2", "1.8.0"]),
        ("androidx.test:core", "androidx.test", "core", ["1.6.1", "1.7.0"]),
        ("androidx.test.ext:junit", "androidx.test.ext", "junit",
         ["1.2.1", "1.3.0"]),
        ("androidx.test.espresso:espresso-core", "androidx.test.espresso",
         "espresso-core", ["3.6.1", "3.7.0"]),
    ]

    # Compose: probe the ui-android AAR each BOM pins, since the BOM is what we depend on.
    bom_jobs = []
    for bom in ["2025.10.00", "2026.01.00", "2026.03.01", "2026.04.01",
                "2026.05.00", "2026.06.01", "2026.08.00", "2026.09.00"]:
        try:
            ui_version = bom_ui_version(bom)
        except Exception as error:  # noqa: BLE001
            print(f"  BOM {bom}: pom fetch failed {error}")
            continue
        if ui_version:
            bom_jobs.append((f"compose-bom {bom} -> ui {ui_version}",
                             "androidx.compose.ui", "ui-android", ui_version))

    jobs = [(label, group, name, version)
            for label, group, name, versions in targets for version in versions] + bom_jobs

    with ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(
            lambda job: check(job[0], job[1], job[2], job[3], max_sdk, max_agp), jobs))

    print(f"Constraints: compileSdk <= {max_sdk}, AGP <= {max_agp_raw}\n")
    best: dict = {}
    for label, version, sdk, agp, fits, error in results:
        note = error if error else f"minCompileSdk={sdk} minAGP={agp}"
        print(f"{'OK ' if fits else '   '} {label:<46} {version:<10} {note}")
        if fits:
            current = best.get(label)
            if current is None or version_key(version) > version_key(current):
                best[label] = version

    print("\n=== newest compatible pick per artifact ===")
    for label, _, _, _ in targets:
        print(f"  {label:<52} {best.get(label, 'NONE')}")
    for label, _, _, _ in bom_jobs:
        if label in best:
            print(f"  {label:<52} {best[label]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
