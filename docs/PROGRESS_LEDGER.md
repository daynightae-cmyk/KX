# KX VPN — immutable evidence-based progress ledger

At the planning baseline (`705af83`) this repository contained only reference PNGs and
task documents — no code, tests, CI, gateway, APK or release. That statement no longer
holds: from TASK_00 onward there is a real Android application in the tree. Each row
below records only what has primary evidence, and each task's full evidence lives in
`docs/evidence/TASK_XX.md`.

| Task | Status | Implementation SHA | PR | Merge SHA | Test/device proof |
| --- | --- | --- | --- | --- | --- |
| TASK_00 | MERGED_VERIFIED (limited scope, see note) | `9278d72fb6bce645f7df22bab4504cbd174f5ad4` | [#1](https://github.com/daynightae-cmyk/KX/pull/1) | `98e6e46cb5d21c4933bc47e949a0a122a16f25a8` | 33/33 JVM tests re-executed on `main` with `--rerun-tasks`; lint clean; `assembleDebug` + `assembleDebugAndroidTest` green; CI success on both the PR SHA (run 41599482673) and the merge SHA (run 41600267115); 10 Compose renders in `docs/evidence/TASK_00/`. **DEVICE_UNVERIFIED** — no emulator/device (BLOCKER-001). |
| TASK_01 | MERGED_VERIFIED (limited scope) | `0de690bfb0c37def858a65900dcf92d4095272d9` | [#10](https://github.com/daynightae-cmyk/KX/pull/10) | `572ac8e157f7c36b45d7d3e9495af21a75dc1f95` | 93/93 JVM tests; lint clean; debug+test APKs; 9 Compose renders; **instrumented suite executed on emulator API 26 and API 35 — 10/10, 0 failed, 0 skipped** (runs 41613401712, 41615330348) with real framebuffer screenshots committed. No physical device. No transport. |
| TASK_02 | MERGED_VERIFIED (engine/state/storage/UI) + VPN_END_TO_END_BLOCKED | `8a4b79f` | [#12](https://github.com/daynightae-cmyk/KX/pull/12) | `eaf264a51ccb2c5124e33ec7f920831f630e7051` | 158/158 JVM tests; lint clean; debug+test APKs; **16/16 instrumented tests on emulator API 26 and API 35** including a real AndroidKeyStore round-trip; real `VpnService` + state machine + keystore-encrypted profiles; 6 home renders. **VPN_END_TO_END_BLOCKED** — no owned gateway, so no handshake/traffic/DNS/leak proof. |
| TASK_03 | MERGED_VERIFIED (catalogue/switch/UI/device) + VPN_END_TO_END_BLOCKED | `ae64e22` (impl `1d2ab73`) | [#14](https://github.com/daynightae-cmyk/KX/pull/14) | `d6dc659f66df9cb3f83b057ae7abdccf2685e3c8` | 242/242 JVM tests; lint clean; debug+test APKs; 7 Compose renders incl. landscape and offline; **23/23 instrumented tests on emulator API 26 and API 35, 0 failed / 0 skipped, empty crash log**; selection changes the profile the tunnel starts and survives restart; live switch ordering asserted as `[stop, start(<new config>)]` against a recording fake platform. Post-merge CI on `d6dc659` re-executed `testDebugUnitTest`, `lintDebug`, both `assemble` tasks and the device suite (runs 41657201778, 41657201495). **VPN_END_TO_END_BLOCKED** — no owned gateway, so multi-server switching over a real network is unverified. |
| TASK_04 | MERGED_VERIFIED (telemetry/UI/device) + BLOCKED_NETWORK_VERIFY | `1f7c96b` | [#16](https://github.com/daynightae-cmyk/KX/pull/16) | `eef3765f72670acebf1f668dfd82a715bc3287f3` | 23/23 instrumented tests on API 26 and API 35, 0 failed / 0 skipped, exit code 0 (runs 41683174319/41683174323 on the PR SHA; 41694367873/41694367932 on the merge). Monotonic session timer, throughput from two real counter samples, ping measured against the user's own endpoint, tunnel address read from the user's configuration; the reference's Mbps, public IP and country pair are not reproduced. **The first post-merge CI run failed** on a Robolectric `android-all` Maven fetch and passed on re-run — see BLOCKER-003. Evidence file was written two tasks late. |
| TASK_05 | PR_OPEN + SECURITY_CLAIMS_CORRECTED | `f4bc7e7` (fixes `33b0449`) | [#17](https://github.com/daynightae-cmyk/KX/pull/17) | not merged | 298/298 JVM tests; lint clean; debug+test APKs; native-pin and APK-hygiene gates pass; CI 41708860326 and device 41708860638 green on `f4bc7e7` (API 35 failed once on an emulator that never booted, then passed on re-run). **The screen as first committed claimed "Fully Protected" from `isTunnelUp` alone and presented three switches that nothing reads; DNS-rewriting switches defaulted ON and silently replaced the user's own `DNS` line with hardcoded AdGuard/Cloudflare resolvers on a first run.** Corrected: hero card states what is and is not verified, unimplemented controls are disabled with the reason, DNS rewriting defaults off and names the resolver, and `SecurityHonestyTest` fails the build if any of it returns. |
| TASK_06 | IN_PROGRESS | none | none | none | Split-tunneling repository, screen and `VpnConfigModifier.applySplitTunnelingSettings` exist in the working tree only. Nothing verified yet. |
| TASK_07 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_08 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_09 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_10 | NOT_STARTED | none | none | none | NOT_RUN |

Valid statuses: NOT_STARTED, IN_PROGRESS, LOCALLY_VERIFIED, DEVICE_UNVERIFIED, BLOCKED_EXTERNAL, PR_OPEN, MERGED_VERIFIED, RELEASE_VERIFIED. Never mark MERGED_VERIFIED without remote main SHA and CI/evidence. Never mark RELEASE_VERIFIED without actual Android device security+network verification and distribution checks.

## Current integrated head

`origin/main` advances with each accepted merge; read it live rather than trusting a
number in this file. Current integrated head: `d6dc659f66df9cb3f83b057ae7abdccf2685e3c8`
(TASK_03 merged). The chain so far, in order:

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

| `8bd1392` | PR #8 — NOTICE.md + continuation directive + APK hygiene gate | run 41608365980 success |
| `572ac8e` | PR #10 — TASK_01 welcome, disclosure, config inspection | build 41615330466 + device 41615330348 success |
| `eaf264a` | PR #12 — TASK_02 VpnService, state machine, encrypted profiles | build 41629115629 + device 41629115640 success |
| `d6dc659` | PR #14 — TASK_03 server selection driving the real transport | CI 41657201778 + device 41657201495 success, tasks re-executed |
