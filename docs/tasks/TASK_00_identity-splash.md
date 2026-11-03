# TASK_00 — Brand identity, native Android foundation, Splash and launch gate

**Execution:** STRICT SERIAL, no jumping ahead. Owner goal: polished faithful native UI **and** real service behavior in each full-stack PR.
**One exact screenshot:** [00-brand-logo.png](../../assets/reference/00-brand-logo.png)
**Reference path:** `assets/reference/00-brand-logo.png`
**Source hash:** verify against `docs/ASSET_MANIFEST.md`; never edit this immutable reference.
**Git branch:** `task/00-identity-splash` (or compatible task namespace).
**Prerequisite:** Repository setup assets and instructions present, no previous feature task.
**Task owner:** implementation agent; no assumption that work is already done.

## Objective
Create the first launchable Android project and real branded Splash experience, without adding VPN/connection claims before an engine exists.

## Reality, baseline and scope isolation
- Verify `D:\KNOUX-Workspace\KX`, remote `https://github.com/daynightae-cmyk/KX.git`, `git fetch`, HEAD SHA, origin/main SHA, status, reflog, stash, all worktrees, outstanding PR/CI, untracked/unique work, active agents.
- Protect all unique work without destructive reset, clean, force push or branch deletion. Stay isolated from parallel agents; preserve previous completed behavior, identity and tests.
- Read `AGENTS.md`, `docs/ENGINEERING_CONTRACT.md`, `docs/DESIGN_SYSTEM.md`, `docs/SECURITY_MODEL.md`, `docs/QUALITY_GATES.md` first; check earlier ledger evidence before writing code.
- Open this **specific PNG** and verify SHA-256. Convert the artwork into responsive, semantic Kotlin/Compose widgets and real actions; **never** display the whole artwork as an unclickable screen.
- The source PNG includes demonstrative values that are not live data. No inventing provider APIs, account status, countries, ping, privacy score, encryption state, download/upload measurements, subscription or endpoint.
- Restrict changes to necessary app modules, resources, tests, CI and evidence for THIS task. Avoid refactoring unrelated screens or introducing architecture duplication.

## Visual UI/UX implementation — exact-screen fidelity
1. Reproduce the exact luminous circular pearl/glass orb with soft shield ribbon geometry; preserve source image as licensed design input, not a stretched UI overlay.
2. Use the exact KX mark with violet/cyan highlights; splash has smooth appearance and reduced-motion accessibility fallback, no fake progress percentage.
3. Create reusable Compose brand components: KXOrb, GlassCard, OrbitParticle, GradientOutline, KXButton, IconAction, brand typography, navigation patterns.
4. Day and Night theme previews; AR/EN app label and locale handling; correct density, 360/411dp portrait, 1.3× text and 48dp targets.

## Full-stack functionality — domain, data, Android platform
1. Initialize Kotlin/Gradle Android application with deterministic package namespace, version catalogs and stable compatible versions; avoid insecure/unverified dependencies.
2. Actual Android 12+ SplashScreen integration and Navigation Compose root; launch transitions to appropriate first-run onboarding screen rather than a dead-end screen.
3. Build persistent first-launch decision (DataStore), startup failure fallback and safe restore after process death; do not mark onboarding accepted until TASK_01.
4. Include Android manifest placeholders only when required; do not ask unnecessary Internet/VPN permissions before engine is added.
5. Create CI Android debug build, static lint, JVM unit test and instrumentation job scaffold with cache and strict exit codes.

## Specific acceptance tests and runtime evidence
1. Gradle wrapper verification; clean compile + assembleDebug + lint + testDebugUnitTest; verify emulator startup does not crash or show blank activity.
2. Instrumentation test: system splash exits, KX brand composition renders and first-run routing occurs; fast relaunch, cold launch and process kill/restart.
3. Pixel-based screenshots on Day/Night, English/Arabic and two viewport sizes; compare splash orb silhouette to this reference.
4. Check launch latency and no infinite animation; run TalkBack and reduced-motion behavior; verify no VPN/protected state is implied.

## Security, privacy and truthfulness
1. No accidental camera/network/storage permission; no account, server, “Premium” or tunnel settings promised.
2. License/attribution note for assets and libraries; preserve raw PNG as supplied and keep generated vector layers clearly derivative.

## Global verification protocol — mandatory for this task
1. Run JVM unit tests including new boundary/race/validation cases; collect command, exit code, passing count and path to reports.
2. Run Android Lint and `assembleDebug`; confirm Gradle/environment versions and no unreviewed dependency/license or security advisories.
3. Execute Android emulator UI tests, real navigation, device rotation/insets where applicable, process restart, error/permission path and foreground/background behavior; save screenshots at 360dp & 411dp, AR/EN, Day/Night plus reference-comparison report.
4. For VPN or network-related functionality, test an **authorized owned gateway or explicitly user-imported endpoint**; capture redacted handshake, route, assigned DNS, IPv4/IPv6 behavior, actual traffic and disconnect. No server available means this proof is BLOCKED, not PASS.
5. If available test physical Android device; otherwise state DEVICE_UNVERIFIED, not production-ready. Inspect logcat for crashes and leaked keys.
6. Create branch/PR, review CI for exact implementation SHA, address findings, merge only once this task meets acceptance gates, verify remote `origin/main` SHA and CI on merged commit. Do not proceed to next task before passing all mandatory merge gates. A missing external provider goes to `docs/BLOCKERS.md`; continue independent work **without inventing closure**.

## Acceptance criteria
1. APK builds and starts on emulator, navigates from splash to a usable first-run route.
2. Glass visual primitives are reusable in subsequent screens with screenshot evidence.
3. No optional screen or service is represented as implemented.
4. AR/EN RTL/LTR, Day/Dark, dynamic text, semantics, 48dp touch, screen spacing, animation quality and insets are visually checked and results attached.
5. No stub/Fake success in production; update `docs/PROGRESS_LEDGER.md`, `docs/BLOCKERS.md` and `docs/evidence/TASK_00.md` with exact verified evidence.
6. PR merged to main with recorded merge commit, exact origin/main SHA and post-merge checks **before starting TASK_01**.

## Completion report template (copy then replace with real evidence)
```yaml
task: TASK_00
status: NOT_STARTED
baseline_sha: UNVERIFIED
implementation_sha: NOT_CREATED
pr_url: NONE
merge_sha: NONE
origin_main_sha: UNVERIFIED
changed_files: []
runtime: NOT_RUN
test_commands_results: []
screen_comparison: NOT_RUN
ar_en_day_dark: NOT_VERIFIED
android_emulator: NOT_VERIFIED
physical_device: DEVICE_UNVERIFIED
wireguard_handshake_and_network: NOT_VERIFIED
secrets_and_permissions: NOT_VERIFIED
external_blockers: []
known_gaps: []
next_action: COMPLETE_THIS_TASK_BEFORE_NEXT
```
