# TASK_00 — native launch foundation

## Status: PR_OPEN (draft) / BLOCKED acceptance

This is an implementation candidate, **not a completed task or VPN product**. TASK_01–10 have not been started. Do not merge or advance the queue without G0–G7 evidence.

- BASE_SHA: `705af83d04399e6a3dd909015f8796a6cb0c0dbe`
- IMPLEMENTATION_SHA (published code commit): `27e4673238bc0a77bde95e268001611d76168682` (verified publisher commit).
- PR_URL: https://github.com/daynightae-cmyk/KX/pull/2
- MERGE_SHA: NONE
- ORIGIN_MAIN_SHA at clone: `705af83d04399e6a3dd909015f8796a6cb0c0dbe`
- Branch: `agent/kx-task-00-native-splash` (isolated from main and other agents).
- Source reference: `assets/reference/00-brand-logo.png`; SHA-256 matched the immutable manifest: `c0b92dd84d60f6f0938bff177d7066a2f74a3eab59ba282438abb76da5f8681e`.

## G0 — observed baseline

Connected GitHub checkout, not the owner's Windows workspace. At clone: clean worktree, no stashes, one local worktree, one shallow baseline commit, no open PRs, no baseline check runs. The clone initially contains only docs/references. Other agents' private workspaces and unpublished branches cannot be inspected here; no claim that they are idle. Existing assets and all prior task specifications are preserved.

## Implemented scope

- Android application `com.knoux.kx`, API 26–35, Kotlin 2.1.20, Gradle 8.13, AGP 8.9.1, Compose BOM 2025.04.01, Java 17; version catalog and generated wrapper with distribution checksum.
- Android system splash into native Compose launch UI, responsive drawn orb/shield/ribbon/jewels, glass card/button/background components, bounded entrance fade honoring platform animation duration scale. No flattened design screen bundled in the APK.
- Day/night resources and English/Arabic text/layout, scrollable large-text content, semantic headings, decorative logo hidden from accessibility, minimum 48dp button heights.
- DataStore reads a versioned first-run flag. Startup never writes onboarding acceptance. First-run and returning destinations are deliberately limited foundation landing pages, **not** implemented TASK_01 onboarding or a VPN home.
- Lifecycle ViewModel, cancellation-aware startup read, duplicate-read guard, non-destructive IO error/retry; navigation state restored by Navigation Compose. No blocking splash timer or invented progress.
- Functional About/back navigation explaining exactly what this build does not do.
- No Internet/VPN/camera/storage permission requested. Backups disabled. No secrets, account, provider, endpoint, payment, metrics or network features.
- Inactive CI template (`docs/ci/android.yml.example`) with build/lint/JVM and API 26/35 emulator jobs; launch/recreation/navigation/error UI tests and 16 day/night/locale/width/font screenshot cases. These jobs/tests are defined, **not yet verified**. The workflow cannot be activated by this publishing integration.

## G3 — actual local commands and outcomes

Tool bootstrap succeeded: Temurin JDK 17.0.13+11, Android SDK platform 35, build tools 35.0.0, standalone Kotlin compiler 2.1.20, generated Gradle 8.13 wrapper.

1. `sha256sum assets/reference/00-brand-logo.png` — PASS, matches manifest above.
2. `./gradlew --version` — PASS, Gradle 8.13; wrapper downloaded and validated the pinned distribution checksum `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`.
3. `gradle --no-daemon testDebugUnitTest lintDebug assembleDebug` (installed Gradle 8.13) — BLOCKED during configuration, before compilation. Kotlin plugin could not resolve from Google/Maven Central/Gradle Plugin Portal. Public Maven/Plugin hosts return `curl: (6) Could not resolve host` in this sandbox. Android SDK and Google Android Maven endpoints were reachable; no registry credentials were changed.
4. Independent compilation/run of **the same** `Startup.kt` + `StartupControllerTest.kt` with standalone Kotlin 2.1.20, JUnit 4.13.2, Hamcrest 1.3 and compiler-bundled coroutines **1.8.0** — PASS, `OK (9 tests)`. This is a narrow domain smoke test, **not** evidence that the Gradle app with its pinned coroutines 1.10.2, Compose or Android dependencies compiles.

Reproduction for step 4 (installed distributions; these are tools, not application dependency overrides):

```sh
CP="$KOTLIN_HOME/lib/kotlinx-coroutines-core-jvm.jar:$GRADLE_HOME/lib/junit-4.13.2.jar:$GRADLE_HOME/lib/hamcrest-core-1.3.jar"
kotlinc app/src/main/java/com/knoux/kx/startup/Startup.kt \
  app/src/test/java/com/knoux/kx/startup/StartupControllerTest.kt \
  -cp "$CP" -include-runtime -d startup-tests.jar
java -cp "startup-tests.jar:$CP" org.junit.runner.JUnitCore com.knoux.kx.startup.StartupControllerTest
```

Tests cover initial state, first-run/returning decisions, disk failure, retry, duplicate in-flight reads, cancellation, new-controller restoration, and no implicit acceptance. Exact local output is in `TASK_00-local-checks.txt`.

## G1/G2/G4/G5 — unverified / remaining

- No APK or emulator runtime verified yet. No KVM device exists locally; build dependency resolution is the first blocker. An owner-installed CI workflow is the next path to Android build and emulator evidence.
- Screenshots, reference silhouette comparison, actual AR/EN/RTL, day/night, 360/411dp, 1.3× text, system reduced-motion behavior, TalkBack, startup timing, rotation, cold process kill/restart and crash logcat: **NOT_VERIFIED**. Test definitions/previews do not count as evidence.
- Physical device: **DEVICE_UNVERIFIED**.
- Native artwork is a derivative candidate, not a pixel-fidelity claim. Detailed visual review/tuning must follow real screenshots.
- Returning route is covered by domain tests, not a completed home. No current production control can mark onboarding accepted.
- No network claim; tunnel/DNS/IP checks are not applicable to this task's implemented launch-only scope. Future VPN work still requires a controlled gateway.
- Dependency license/advisory review and redistribution rights: pending before release (see `NOTICE.md`).

## G6/G7 — integration and next action

Open a **draft** PR, have the owner install the supplied workflow template as `.github/workflows/android.yml`, inspect exact published-SHA CI, resolve build/runtime findings, collect emulator and visual evidence, perform review, and obtain verified integration before advancing. No merge performed. Existing main and all TASK_01–10 specifications remain untouched.

### Publication constraint

The first draft publication was rejected with `workflow_file_not_allowed` / HTTP 403 because this integration cannot create GitHub Actions workflows. The tool confirmed **nothing was published**. No authentication or app permissions were changed. The same workflow is preserved as the inert `docs/ci/android.yml.example` plus a downloadable handoff; it must be installed by the owner at `.github/workflows/android.yml`. It does not run from `docs/ci/`. This is an additional blocker to exact-SHA CI, not a request for credentials.

### Observed publication/check state

Draft PR #2 was published and read back: isolated head branch, base `main` still `705af83d04399e6a3dd909015f8796a6cb0c0dbe`, `merged=false`. For the published implementation SHA above: no Android Actions build jobs exist; CodeRabbit was skipped, Gitar and Vercel Agent Review were in progress at observation. These are not Android build evidence. The separate Vercel deployment commit status is `failure`, description **Account is blocked.** No web deployment or account/billing change is part of this native Android PR; do not treat that deployment status as an Android compiler result. Final documentation-only commits have their own check state.

Additional local validation: all XML and version-catalog TOML parsed, all 16 Arabic/English string keys match, main manifest declares no permissions and disables backup, `git diff --check` passed. Compose BOM metadata was retrieved, selecting UI/animation 1.8.0 and Material3 1.3.2; relevant Card/Button/animation/screenshot API source signatures were inspected. This is not a substitute for compilation or visual validation.
