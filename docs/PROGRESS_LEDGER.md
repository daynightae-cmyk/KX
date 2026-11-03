# KX VPN — immutable evidence-based progress ledger

At the planning baseline (`705af83`) this repository contained only reference PNGs and
task documents — no code, tests, CI, gateway, APK or release. That statement no longer
holds: from TASK_00 onward there is a real Android application in the tree. Each row
below records only what has primary evidence, and each task's full evidence lives in
`docs/evidence/TASK_XX.md`.

| Task | Status | Implementation SHA | PR | Merge SHA | Test/device proof |
| --- | --- | --- | --- | --- | --- |
| TASK_00 | MERGED_VERIFIED (limited scope, see note) | `9278d72fb6bce645f7df22bab4504cbd174f5ad4` | [#1](https://github.com/daynightae-cmyk/KX/pull/1) | `98e6e46cb5d21c4933bc47e949a0a122a16f25a8` | 33/33 JVM tests re-executed on `main` with `--rerun-tasks`; lint clean; `assembleDebug` + `assembleDebugAndroidTest` green; CI success on both the PR SHA (run 41599482673) and the merge SHA (run 41600267115); 10 Compose renders in `docs/evidence/TASK_00/`. **DEVICE_UNVERIFIED** — no emulator/device (BLOCKER-001). |
| TASK_01 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_02 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_03 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_04 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_05 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_06 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_07 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_08 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_09 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_10 | NOT_STARTED | none | none | none | NOT_RUN |

Valid statuses: NOT_STARTED, IN_PROGRESS, LOCALLY_VERIFIED, DEVICE_UNVERIFIED, BLOCKED_EXTERNAL, PR_OPEN, MERGED_VERIFIED, RELEASE_VERIFIED. Never mark MERGED_VERIFIED without remote main SHA and CI/evidence. Never mark RELEASE_VERIFIED without actual Android device security+network verification and distribution checks.

## Current integrated head

`origin/main = a692ff4dd86a53e82bd67e7c73ab26f5d564f9db` — Android CI green on this
exact SHA (run 41603418054). TASK_00's feature merge was `98e6e46…`; main has since
advanced by the evidence-docs commit and one CI cache-correctness fix
(`4129256…`), so the merge SHA in the table above is the *task* merge, not the
current head. Read `docs/evidence/TASK_00.md` for the full chain.

## External deferrals
A task with a demonstrably complete, tested and merged provider-free supported scope may be `MERGED_VERIFIED` **only for that expressly limited scope**; list all optional `DEFERRED_EXTERNAL` features and their failures in `docs/BLOCKERS.md` and the task evidence. An external deferral is never a success claim about that feature.
