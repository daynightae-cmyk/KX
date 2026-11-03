"""Print exception TYPES and source frames, never failure messages or test values.

Instrumentation artifacts may be unavailable through connected log-reading tools.
This exposes a small diagnostic index in the existing CI log without dumping configs,
UI hierarchies, logcat, XML properties, exception messages or credential material.
The runner still returns the original nonzero Gradle exit status.
"""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

TYPE = re.compile(r"^(?:(?:Caused by|Suppressed):\s*)?([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+)(?=[:\s]|$)")
FRAME = re.compile(r"^at\s+([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+)\(([^()]+\.\w+:\d+)\)$")
IDENTIFIER = re.compile(r"[A-Za-z_$][\w.$]*\Z")
SOURCE = re.compile(r"[A-Za-z_$][\w.$-]*\.(?:kt|java):\d+\Z")


def summarize(root: Path) -> list[str]:
    lines = []
    for report in sorted(root.rglob("TEST-*.xml")):
        try:
            tree = ET.parse(report)
        except (ET.ParseError, OSError):
            lines.append("Instrumentation XML unreadable (contents withheld)")
            continue
        for case in tree.iter("testcase"):
            failures = list(case.findall("failure")) + list(case.findall("error"))
            if not failures:
                continue
            name = case.attrib.get("classname", "")
            lines.append("Failed test class: " + (name if IDENTIFIER.fullmatch(name) else "[withheld]"))
            for failure in failures:
                emitted = 0
                for raw in (failure.text or "").splitlines():
                    raw = raw.strip()
                    kind = TYPE.match(raw)
                    frame = FRAME.match(raw)
                    if kind:
                        lines.append("Exception type: " + kind.group(1))
                        emitted += 1
                    elif frame and SOURCE.fullmatch(frame.group(2)):
                        lines.append("Frame: " + frame.group(1) + "(" + frame.group(2) + ")")
                        emitted += 1
                    if emitted == 20:
                        break
    return lines or ["No structured instrumentation failure detail found; original test exit remains authoritative."]


if __name__ == "__main__":
    root = Path(sys.argv[1]) if len(sys.argv) == 2 else Path("app/build/outputs/androidTest-results")
    print("\n".join(summarize(root)))
