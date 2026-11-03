# TASK_04 — Connected dashboard, connection verification, counters and disconnect

**Execution:** STRICT SERIAL, no jumping ahead. Owner goal: polished faithful native UI **and** real service behavior in each full-stack PR.
**One exact screenshot:** [04-connected-session.png](../../assets/reference/04-connected-session.png)
**Reference path:** `assets/reference/04-connected-session.png`
**Source hash:** verify against `docs/ASSET_MANIFEST.md`; never edit this immutable reference.
**Git branch:** `task/04-connected-session` (or compatible task namespace).
**Prerequisite:** TASK_03 has MERGED_VERIFIED with exact origin/main SHA and evidence.
**Task owner:** implementation agent; no assumption that work is already done.

## Objective
Build the true Connected screen and traffic-aware session with honest reachability and clean teardown.

## Reality, baseline and scope isolation
- Verify `D:\KNOUX-Workspace\KX`, remote `https://github.com/daynightae-cmyk/KX.git`, `git fetch`, HEAD SHA, origin/main SHA, status, reflog, stash, all worktrees, outstanding PR/CI, untracked/unique work, active agents.
- Protect all unique work without destructive reset, clean, force push or branch deletion. Stay isolated from parallel agents; preserve previous completed behavior, identity and tests.
- Read `AGENTS.md`, `docs/ENGINEERING_CONTRACT.md`, `docs/DESIGN_SYSTEM.md`, `docs/SECURITY_MODEL.md`, `docs/QUALITY_GATES.md` first; check earlier ledger evidence before writing code.
- Open this **specific PNG** and verify SHA-256. Convert the artwork into responsive, semantic Kotlin/Compose widgets and real actions; **never** display the whole artwork as an unclickable screen.
- The source PNG includes demonstrative values that are not live data. No inventing provider APIs, account status, countries, ping, privacy score, encryption state, download/upload measurements, subscription or endpoint.
- Restrict changes to necessary app modules, resources, tests, CI and evidence for THIS task. Avoid refactoring unrelated screens or introducing architecture duplication.

## Visual UI/UX implementation — exact-screen fidelity
1. Reference light lavender/day backdrop, dynamic ring with shield check, country route card, actual session timer and transparent stat tiles.
2. Connected view shown only after backend state confirms tunnel UP, with a separate Internet Reachability verification and no automatic 'Fully Protected'.
3. Dark/Day equivalent with same KX orb identity and resize-safe charts/status.

## Full-stack functionality — domain, data, Android platform
1. Tunnel handshake and traffic route checks to controlled endpoint; separate verified-exit-IP lookup (only with user consent if external), IPv4/IPv6 route/DNS status.
2. Download/upload byte counters from actual tunnel interface/backend, sampling and units; Ping only via real probe with timestamp and timeout; no speed-test claim unless measured with proper method.
3. Session start uses monotonic elapsed time while live; pause/reset correctly across disconnect, reboot, service restart and clock changes.
4. Disconnect genuinely tears down service/routes and clears notification; Change Server uses Task 03 catalogue.
5. Handle lost gateway, network switch, fail/rollback, quick user tapping and error disclosures.

## Specific acceptance tests and runtime evidence
1. Emulator traffic through test WireGuard egress, query route before/during/after, verify actual exit IP and DNS where feasible.
2. Counter growth on controlled transfer, monotonic timer, rapid connect/disconnect, foreground/background, device rotation, loss/reconnect.
3. Reference screenshots on Day/Dark, AR/EN, narrow widths; remove displayed example numbers from original design.
4. If no test gateway provisioned, explicitly BLOCKED_NETWORK_VERIFY, no false Connected or synthetic IP.

## Security, privacy and truthfulness
1. Key material absent from screenshots/logs and traffic test output; protect real IP values in evidence.
2. Internet reachability != anonymity or complete leak prevention; UI claims precise.

## Global verification protocol — mandatory for this task
1. Run JVM unit tests including new boundary/race/validation cases; collect command, exit code, passing count and path to reports.
2. Run Android Lint and `assembleDebug`; confirm Gradle/environment versions and no unreviewed dependency/license or security advisories.
3. Execute Android emulator UI tests, real navigation, device rotation/insets where applicable, process restart, error/permission path and foreground/background behavior; save screenshots at 360dp & 411dp, AR/EN, Day/Night plus reference-comparison report.
4. For VPN or network-related functionality, test an **authorized owned gateway or explicitly user-imported endpoint**; capture redacted handshake, route, assigned DNS, IPv4/IPv6 behavior, actual traffic and disconnect. No server available means this proof is BLOCKED, not PASS.
5. If available test physical Android device; otherwise state DEVICE_UNVERIFIED, not production-ready. Inspect logcat for crashes and leaked keys.
6. Create branch/PR, review CI for exact implementation SHA, address findings, merge only once this task meets acceptance gates, verify remote `origin/main` SHA and CI on merged commit. Do not proceed to next task before passing all mandatory merge gates. A missing external provider goes to `docs/BLOCKERS.md`; continue independent work **without inventing closure**.

## Acceptance criteria
1. Displayed tunnel status matches actual backend lifecycle.
2. Real traffic and clean disconnect proven with reproducible safe evidence.
4. AR/EN RTL/LTR, Day/Dark, dynamic text, semantics, 48dp touch, screen spacing, animation quality and insets are visually checked and results attached.
5. No stub/Fake success in production; update `docs/PROGRESS_LEDGER.md`, `docs/BLOCKERS.md` and `docs/evidence/TASK_04.md` with exact verified evidence.
6. PR merged to main with recorded merge commit, exact origin/main SHA and post-merge checks **before starting TASK_05**.

## Completion report template (copy then replace with real evidence)
```yaml
task: TASK_04
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
