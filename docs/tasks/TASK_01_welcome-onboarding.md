# TASK_01 — Welcome, first-run onboarding and import entry

**Execution:** STRICT SERIAL, no jumping ahead. Owner goal: polished faithful native UI **and** real service behavior in each full-stack PR.
**One exact screenshot:** [01-welcome.png](../../assets/reference/01-welcome.png)
**Reference path:** `assets/reference/01-welcome.png`
**Source hash:** verify against `docs/ASSET_MANIFEST.md`; never edit this immutable reference.
**Git branch:** `task/01-welcome-onboarding` (or compatible task namespace).
**Prerequisite:** TASK_00 has MERGED_VERIFIED with exact origin/main SHA and evidence.
**Task owner:** implementation agent; no assumption that work is already done.

## Objective
Deliver real tappable first-run UX that feeds native app navigation and prepares honest import/config consent without faking network service.

## Reality, baseline and scope isolation
- Verify `D:\KNOUX-Workspace\KX`, remote `https://github.com/daynightae-cmyk/KX.git`, `git fetch`, HEAD SHA, origin/main SHA, status, reflog, stash, all worktrees, outstanding PR/CI, untracked/unique work, active agents.
- Protect all unique work without destructive reset, clean, force push or branch deletion. Stay isolated from parallel agents; preserve previous completed behavior, identity and tests.
- Read `AGENTS.md`, `docs/ENGINEERING_CONTRACT.md`, `docs/DESIGN_SYSTEM.md`, `docs/SECURITY_MODEL.md`, `docs/QUALITY_GATES.md` first; check earlier ledger evidence before writing code.
- Open this **specific PNG** and verify SHA-256. Convert the artwork into responsive, semantic Kotlin/Compose widgets and real actions; **never** display the whole artwork as an unclickable screen.
- The source PNG includes demonstrative values that are not live data. No inventing provider APIs, account status, countries, ping, privacy score, encryption state, download/upload measurements, subscription or endpoint.
- Restrict changes to necessary app modules, resources, tests, CI and evidence for THIS task. Avoid refactoring unrelated screens or introducing architecture duplication.

## Visual UI/UX implementation — exact-screen fidelity
1. Match dark-indigo atmospheric wave background, large glass KX orb, luminous Get Started and Import Config pill controls.
2. Preserve spacious centered KX wordmark, accessible visual hierarchy and subtle orbit highlights.
3. Support hero scaling below 360dp; use scroll-safe layout and screen-reader clear button labels; never use static screenshot background.

## Full-stack functionality — domain, data, Android platform
1. Get Started routes to disconnected Home; Import Config routes to supported early configuration entry, not a nonfunctional placeholder.
2. Persist onboarding completed only when user completes flow; choose conditional launch routing on subsequent app starts.
3. Present transparent information on what a VPN is, privacy limitations and cost boundaries; no unsupported 'private/borderless' absolutes.
4. Prepare permission education; actual Android VPN consent only when user initiates connecting, not automatically at onboarding.
5. Back navigation, import cancel, empty state, denied permission and network-off handling.

## Specific acceptance tests and runtime evidence
1. First install, completed onboarding, wipe app data, process death, back button, wrong navigation target and double-tap.
2. Accessibility semantic tests for both CTAs and AR RTL text expansion; Day/Night screenshot comparisons.
3. Assert no unexpected VPN permission prompt or network request on first run.
4. Navigation UI tests cover import-entry fallback when configuration not yet available.

## Security, privacy and truthfulness
1. Never silently enroll in providers/accounts or upload a config; imports are local.
2. No hardcoded fake connected state on landing.

## Global verification protocol — mandatory for this task
1. Run JVM unit tests including new boundary/race/validation cases; collect command, exit code, passing count and path to reports.
2. Run Android Lint and `assembleDebug`; confirm Gradle/environment versions and no unreviewed dependency/license or security advisories.
3. Execute Android emulator UI tests, real navigation, device rotation/insets where applicable, process restart, error/permission path and foreground/background behavior; save screenshots at 360dp & 411dp, AR/EN, Day/Night plus reference-comparison report.
4. For VPN or network-related functionality, test an **authorized owned gateway or explicitly user-imported endpoint**; capture redacted handshake, route, assigned DNS, IPv4/IPv6 behavior, actual traffic and disconnect. No server available means this proof is BLOCKED, not PASS.
5. If available test physical Android device; otherwise state DEVICE_UNVERIFIED, not production-ready. Inspect logcat for crashes and leaked keys.
6. Create branch/PR, review CI for exact implementation SHA, address findings, merge only once this task meets acceptance gates, verify remote `origin/main` SHA and CI on merged commit. Do not proceed to next task before passing all mandatory merge gates. A missing external provider goes to `docs/BLOCKERS.md`; continue independent work **without inventing closure**.

## Acceptance criteria
1. Both CTA routes are real; persistence and restarts behave as expected.
2. Onboarding is localized, accessible and visually faithful without frozen artwork.
4. AR/EN RTL/LTR, Day/Dark, dynamic text, semantics, 48dp touch, screen spacing, animation quality and insets are visually checked and results attached.
5. No stub/Fake success in production; update `docs/PROGRESS_LEDGER.md`, `docs/BLOCKERS.md` and `docs/evidence/TASK_01.md` with exact verified evidence.
6. PR merged to main with recorded merge commit, exact origin/main SHA and post-merge checks **before starting TASK_02**.

## Completion report template (copy then replace with real evidence)
```yaml
task: TASK_01
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
