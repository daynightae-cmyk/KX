# Live blockers log — initial facts

| Dependency | Initial state | Safe non-provider fallback | Claim ban |
| --- | --- | --- | --- |
| Global VPN fleet / free exits | NONE provisioned or verified | Own gateway or user-import WireGuard | No fake global locations/load |
| Provider API key/credentials | NOT SUPPLIED | Offline-first app / local config | No invented accounts |
| Google Play signing or publishing | NOT SUPPLIED | Debug builds, validated signed release later | No released claim |
| Real physical Android test | NO evidence | Emulator labeled DEVICE_UNVERIFIED | No hardware verification |
| OpenVPN, obfuscation, port hopping, tracker filter | NOT IMPLEMENTED | Disable until independently built/tested | No fake active toggles |
| Marketing “Premium”, “Unlimited”, “No logs” | Unsubstantiated | Hide | No misleading product guarantees |

For every new blocker append: task, service, failing command/error, exact scope affected, fallback implemented, what cannot be verified, safe next action. Continue working on independent subcomponents but **never falsely close or merge unfinished acceptance scope**.

## TASK_00 — observed build/runtime blockers (2026-11-03)

- **Android dependency resolution — BLOCKED:** `gradle --no-daemon testDebugUnitTest lintDebug assembleDebug` stopped before compilation: Kotlin Android plugin 2.1.20 could not resolve. Maven Central and Gradle Plugin Portal endpoint probes failed DNS resolution (`curl (6) Could not resolve host`). Java/SDK/Gradle were successfully installed; no registry authentication was changed. Fallback: run the same startup-domain unit sources with a standalone compiler (9 pass; coroutines 1.8.0, not the app-pinned 1.10.2). This does not validate the Android build. Next: exact-SHA GitHub CI using the normal public repositories.
- **Runtime/visual gates — NOT_VERIFIED:** no compiled APK, no device run, and local `/dev/kvm` absent. CI emulator jobs and screenshot tests are provided, not claimed successful. No AR/EN/theme/size/TalkBack/process-death proof yet. Keep this PR draft; do not advance TASK_01 before acceptance/integration.
- **Physical Android device — DEVICE_UNVERIFIED:** release still requires real-device validation.
- No provider key is required for the current launch-only task. No user secret is requested. Global servers, transport and onboarding remain outside this task, not falsely delivered.

- **CI activation — BLOCKED:** GitHub publisher rejected `.github/workflows/android.yml` with `workflow_file_not_allowed` (HTTP 403); it explicitly confirmed nothing published. The managed publisher lacks workflows permission and cannot be repaired by changing the app installation. Recovery: keep the executable workflow as inert `docs/ci/android.yml.example` and publish all normal app/test/evidence changes as a draft. Owner must add the supplied template as `.github/workflows/android.yml` before CI can run. No user token is requested.
