# TASK_05 — Security Center grounded in Android system state

**Execution:** STRICT SERIAL, no jumping ahead. Owner goal: polished faithful native UI **and** real service behavior in each full-stack PR.
**One exact screenshot:** [05-security-center.png](../../assets/reference/05-security-center.png)
**Reference path:** `assets/reference/05-security-center.png`
**Source hash:** verify against `docs/ASSET_MANIFEST.md`; never edit this immutable reference.
**Git branch:** `task/05-security-center` (or compatible task namespace).
**Prerequisite:** TASK_04 has MERGED_VERIFIED with exact origin/main SHA and evidence.
**Task owner:** implementation agent; no assumption that work is already done.

## Objective
Turn protection controls into defensible real Android behaviors, not decorative On toggles.

## Reality, baseline and scope isolation
- Verify `D:\KNOUX-Workspace\KX`, remote `https://github.com/daynightae-cmyk/KX.git`, `git fetch`, HEAD SHA, origin/main SHA, status, reflog, stash, all worktrees, outstanding PR/CI, untracked/unique work, active agents.
- Protect all unique work without destructive reset, clean, force push or branch deletion. Stay isolated from parallel agents; preserve previous completed behavior, identity and tests.
- Read `AGENTS.md`, `docs/ENGINEERING_CONTRACT.md`, `docs/DESIGN_SYSTEM.md`, `docs/SECURITY_MODEL.md`, `docs/QUALITY_GATES.md` first; check earlier ledger evidence before writing code.
- Open this **specific PNG** and verify SHA-256. Convert the artwork into responsive, semantic Kotlin/Compose widgets and real actions; **never** display the whole artwork as an unclickable screen.
- The source PNG includes demonstrative values that are not live data. No inventing provider APIs, account status, countries, ping, privacy score, encryption state, download/upload measurements, subscription or endpoint.
- Restrict changes to necessary app modules, resources, tests, CI and evidence for THIS task. Avoid refactoring unrelated screens or introducing architecture duplication.

## Visual UI/UX implementation — exact-screen fidelity
1. Match pale glass card, shield orb, security list row rhythm and six toggles; replace unverified Fully Protected headline with specific measured security summary.
2. Each row has truthful On/Off/Unavailable/Requires System Setting and clear documentation; glass toggles preserve contrast and focus.

## Full-stack functionality — domain, data, Android platform
1. System 'Always-on VPN' and Block connections without VPN: explain Android device-settings setup, detect accessible status; do not falsely toggle app-wide lockdown.
2. DNS and IPv6 protection status derived from actual tunnel routes/resolver configuration and tested leak behavior; auto-reconnect uses existing state machine.
3. Kill-switch label maps to verifiable Android lockdown or audited mechanism; if not available provide help link/status unavailable instead of fake On.
4. Tracker blocking requires real DNS/content filter and test coverage; otherwise hidden/coming-later without promises.
5. Persist user preferences as intents, not as false proof; surface diagnostic errors, stale status and settings deep-link fallback.

## Specific acceptance tests and runtime evidence
1. System settings navigation, state refresh, consent denial, revoked tunnel and network switch.
2. Leak tests with own gateway: DNS requests/IPv6 route (where supported), lockdown behavior only if device configured.
3. UI semantics and AR/EN day/night screenshot; toggles reveal truth after app relaunch.
4. Security threat model review; fail closed or explicit unsupported status.

## Security, privacy and truthfulness
1. No 'Fully Protected' status solely based on VPN UP.
2. No false No Logs/anonymous/zero leakage claims.

## Global verification protocol — mandatory for this task
1. Run JVM unit tests including new boundary/race/validation cases; collect command, exit code, passing count and path to reports.
2. Run Android Lint and `assembleDebug`; confirm Gradle/environment versions and no unreviewed dependency/license or security advisories.
3. Execute Android emulator UI tests, real navigation, device rotation/insets where applicable, process restart, error/permission path and foreground/background behavior; save screenshots at 360dp & 411dp, AR/EN, Day/Night plus reference-comparison report.
4. For VPN or network-related functionality, test an **authorized owned gateway or explicitly user-imported endpoint**; capture redacted handshake, route, assigned DNS, IPv4/IPv6 behavior, actual traffic and disconnect. No server available means this proof is BLOCKED, not PASS.
5. If available test physical Android device; otherwise state DEVICE_UNVERIFIED, not production-ready. Inspect logcat for crashes and leaked keys.
6. Create branch/PR, review CI for exact implementation SHA, address findings, merge only once this task meets acceptance gates, verify remote `origin/main` SHA and CI on merged commit. Do not proceed to next task before passing all mandatory merge gates. A missing external provider goes to `docs/BLOCKERS.md`; continue independent work **without inventing closure**.

## Acceptance criteria
1. Every enabled protection is backed by platform state plus tests.
2. Unavailable capabilities aren't represented as ON.
4. AR/EN RTL/LTR, Day/Dark, dynamic text, semantics, 48dp touch, screen spacing, animation quality and insets are visually checked and results attached.
5. No stub/Fake success in production; update `docs/PROGRESS_LEDGER.md`, `docs/BLOCKERS.md` and `docs/evidence/TASK_05.md` with exact verified evidence.
6. PR merged to main with recorded merge commit, exact origin/main SHA and post-merge checks **before starting TASK_06**.

## Completion report template (copy then replace with real evidence)
```yaml
task: TASK_05
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
