# TASK_08 — Real local statistics, counters, charts and privacy

**Execution:** STRICT SERIAL, no jumping ahead. Owner goal: polished faithful native UI **and** real service behavior in each full-stack PR.
**One exact screenshot:** [08-statistics.png](../../assets/reference/08-statistics.png)
**Reference path:** `assets/reference/08-statistics.png`
**Source hash:** verify against `docs/ASSET_MANIFEST.md`; never edit this immutable reference.
**Git branch:** `task/08-statistics` (or compatible task namespace).
**Prerequisite:** TASK_07 has MERGED_VERIFIED with exact origin/main SHA and evidence.
**Task owner:** implementation agent; no assumption that work is already done.

## Objective
Replace illustrative 48.7GB/112.4Mbps/98 privacy score with true measured local data and user-controlled retention.

## Reality, baseline and scope isolation
- Verify `D:\KNOUX-Workspace\KX`, remote `https://github.com/daynightae-cmyk/KX.git`, `git fetch`, HEAD SHA, origin/main SHA, status, reflog, stash, all worktrees, outstanding PR/CI, untracked/unique work, active agents.
- Protect all unique work without destructive reset, clean, force push or branch deletion. Stay isolated from parallel agents; preserve previous completed behavior, identity and tests.
- Read `AGENTS.md`, `docs/ENGINEERING_CONTRACT.md`, `docs/DESIGN_SYSTEM.md`, `docs/SECURITY_MODEL.md`, `docs/QUALITY_GATES.md` first; check earlier ledger evidence before writing code.
- Open this **specific PNG** and verify SHA-256. Convert the artwork into responsive, semantic Kotlin/Compose widgets and real actions; **never** display the whole artwork as an unclickable screen.
- The source PNG includes demonstrative values that are not live data. No inventing provider APIs, account status, countries, ping, privacy score, encryption state, download/upload measurements, subscription or endpoint.
- Restrict changes to necessary app modules, resources, tests, CI and evidence for THIS task. Avoid refactoring unrelated screens or introducing architecture duplication.

## Visual UI/UX implementation — exact-screen fidelity
1. Match reference 4 info cards, daily trend chart, connection-time donut, history list and branded nav.
2. With no sessions, show elegant empty chart and explanatory text, never sample metrics in production.
3. Interactive time filters 7D/30D/3M/1Y, accessible numeric chart descriptions and AR-localized dates.

## Full-stack functionality — domain, data, Android platform
1. Collect bytes and session duration from actual backend; persist minimal aggregate snapshots with versioned schema/migrations.
2. Use monotonic session start/duration and handle process restarts, retention, timezone changes, duplicate snapshot prevention and overflow.
3. Server usage history derived from actual selected configs, no sensitive endpoint details or personal IP in stored records.
4. Most-used-server only when recorded; 'privacy score' removed unless a rigorous defined, reviewed metric exists.
5. Clear/reset history action with confirmation; export anonymized aggregates only if explicitly requested and audited.

## Specific acceptance tests and runtime evidence
1. Unit tests for aggregation boundary, day rollover DST/timezone, empty/large volume, duplicate events and migration.
2. Integrated controlled traffic counter reconciliation between task 04 tunnel and graphs; test privacy reset and no inadvertent telemetry.
3. Screenshots compare graph/card ratios and visual hierarchy to image in AR/EN Day/Dark; accessibility labels for axes.

## Security, privacy and truthfulness
1. Data minimization: no full browsing history, no packet capture, no sensitive IP in telemetry.
2. Clear history truly removes relevant data with tests.

## Global verification protocol — mandatory for this task
1. Run JVM unit tests including new boundary/race/validation cases; collect command, exit code, passing count and path to reports.
2. Run Android Lint and `assembleDebug`; confirm Gradle/environment versions and no unreviewed dependency/license or security advisories.
3. Execute Android emulator UI tests, real navigation, device rotation/insets where applicable, process restart, error/permission path and foreground/background behavior; save screenshots at 360dp & 411dp, AR/EN, Day/Night plus reference-comparison report.
4. For VPN or network-related functionality, test an **authorized owned gateway or explicitly user-imported endpoint**; capture redacted handshake, route, assigned DNS, IPv4/IPv6 behavior, actual traffic and disconnect. No server available means this proof is BLOCKED, not PASS.
5. If available test physical Android device; otherwise state DEVICE_UNVERIFIED, not production-ready. Inspect logcat for crashes and leaked keys.
6. Create branch/PR, review CI for exact implementation SHA, address findings, merge only once this task meets acceptance gates, verify remote `origin/main` SHA and CI on merged commit. Do not proceed to next task before passing all mandatory merge gates. A missing external provider goes to `docs/BLOCKERS.md`; continue independent work **without inventing closure**.

## Acceptance criteria
1. All charts reflect recorded events and can be recomputed.
2. No arbitrary privacy score or fake speed figures.
4. AR/EN RTL/LTR, Day/Dark, dynamic text, semantics, 48dp touch, screen spacing, animation quality and insets are visually checked and results attached.
5. No stub/Fake success in production; update `docs/PROGRESS_LEDGER.md`, `docs/BLOCKERS.md` and `docs/evidence/TASK_08.md` with exact verified evidence.
6. PR merged to main with recorded merge commit, exact origin/main SHA and post-merge checks **before starting TASK_09**.

## Completion report template (copy then replace with real evidence)
```yaml
task: TASK_08
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
