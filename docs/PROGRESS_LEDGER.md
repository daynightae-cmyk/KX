# KX VPN — immutable evidence-based progress ledger

At the planning baseline (`705af83`) this repository contained only reference PNGs and
task documents — no code, tests, CI, gateway, APK or release. That statement no longer
holds: from TASK_00 onward there is a real Android application in the tree. Each row
below records only what has primary evidence, and each task's full evidence lives in
`docs/evidence/TASK_XX.md`.

| Task | Status | Implementation SHA | PR | Merge SHA | Test/device proof |
| --- | --- | --- | --- | --- | --- |
| TASK_00 | MERGED_VERIFIED (limited scope, see note) | `9278d72fb6bce645f7df22bab4504cbd174f5ad4` | [#1](https://github.com/daynightae-cmyk/KX/pull/1) | `98e6e46cb5d21c4933bc47e949a0a122a16f25a8` | 33/33 JVM tests re-executed on `main` with `--rerun-tasks`; lint clean; `assembleDebug` + `assembleDebugAndroidTest` green; CI success on both the PR SHA (run 41599482673) and the merge SHA (run 41600267115); 10 Compose renders in `docs/evidence/TASK_00/`. **DEVICE_UNVERIFIED** — no emulator/device (BLOCKER-001). |
| TASK_01 | PR_OPEN | pending | pending | pending | 93/93 JVM tests, lint clean, both APKs built, 9 new Compose renders. Instrumented suite rewritten for the real first-run routes and executed by the device-runtime workflow on emulator API 26/35. |
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

`origin/main` advances with each accepted merge; read it live rather than trusting a
number in this file. The chain so far, in order:

| Merge | What it was | CI |
|---|---|---|
| `98e6e46` | PR #1 — TASK_00 implementation | run 41600267115 success |
| `a692ff4` | PR #3 — render dir declared as Gradle task output | run 41603418054 success |
| `143a0aa` | PR #4 — bounded startup read + safe test cleanup | run 41606402710 success |

## TASK_00 reconciliation of parallel work

Two other branches carried TASK_00 work alongside PR #1. Both were inspected, neither
was discarded:

- **PR #4 `agent/task-00-startup-deadline` — MERGED.** It fixed two real defects in the
  code I shipped. (1) `PreferencesLaunchStateRepositoryTest.tearDown` called
  `it.parentFile.deleteRecursively()` on directories it owned, so `parentFile` was the
  **shared system temp directory** — a destructive test. It now deletes only a unique
  per-test root. (2) `MainActivity` bounded the *system* splash at 3s, but the Compose
  launch screen had no bound, so a store that never emits would sit on "Preparing"
  forever; `readLaunchSnapshot()` now bounds the read itself and resolves as degraded.
  Its secret-scan test also stopped swallowing an `IOException` that would have made
  the security assertion pass vacuously. Verified locally with `--rerun-tasks`:
  **44 tests, 0 failures**, lint clean, `assembleDebug` green.
- **PR #2 `agent/kx-task-00-native-splash` — code NOT merged, branch retained.** It is a
  second full TASK_00 implementation under package `com.knoux.kx`; merging it would
  duplicate every screen, theme, route and test, which the owner directive forbids.
  Two unique documents from it were salvaged: `NOTICE.md` (TASK_00 requires a
  license/attribution note and PR #1 shipped without one — a real gap) and
  `docs/AGENT_UNATTENDED_CONTINUATION.md`. Its `docs/ci/android.yml.example` is
  superseded by the live `.github/workflows/ci.yml`, not discarded.

## External deferrals
A task with a demonstrably complete, tested and merged provider-free supported scope may be `MERGED_VERIFIED` **only for that expressly limited scope**; list all optional `DEFERRED_EXTERNAL` features and their failures in `docs/BLOCKERS.md` and the task evidence. An external deferral is never a success claim about that feature.
