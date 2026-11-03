# TASK_02 — Disconnected Home and real VPN lifecycle

**Execution:** STRICT SERIAL, no jumping ahead. Owner goal: polished faithful native UI **and** real service behavior in each full-stack PR.
**One exact screenshot:** [02-home-disconnected.png](../../assets/reference/02-home-disconnected.png)
**Reference path:** `assets/reference/02-home-disconnected.png`
**Source hash:** verify against `docs/ASSET_MANIFEST.md`; never edit this immutable reference.
**Git branch:** `task/02-home-disconnected` (or compatible task namespace).
**Prerequisite:** TASK_01 has MERGED_VERIFIED with exact origin/main SHA and evidence.
**Task owner:** implementation agent; no assumption that work is already done.

## Objective
Build the Android VPN session engine and a live Home whose Connect action corresponds to actual platform state.

## Reality, baseline and scope isolation
- Verify `D:\KNOUX-Workspace\KX`, remote `https://github.com/daynightae-cmyk/KX.git`, `git fetch`, HEAD SHA, origin/main SHA, status, reflog, stash, all worktrees, outstanding PR/CI, untracked/unique work, active agents.
- Protect all unique work without destructive reset, clean, force push or branch deletion. Stay isolated from parallel agents; preserve previous completed behavior, identity and tests.
- Read `AGENTS.md`, `docs/ENGINEERING_CONTRACT.md`, `docs/DESIGN_SYSTEM.md`, `docs/SECURITY_MODEL.md`, `docs/QUALITY_GATES.md` first; check earlier ledger evidence before writing code.
- Open this **specific PNG** and verify SHA-256. Convert the artwork into responsive, semantic Kotlin/Compose widgets and real actions; **never** display the whole artwork as an unclickable screen.
- The source PNG includes demonstrative values that are not live data. No inventing provider APIs, account status, countries, ping, privacy score, encryption state, download/upload measurements, subscription or endpoint.
- Restrict changes to necessary app modules, resources, tests, CI and evidence for THIS task. Avoid refactoring unrelated screens or introducing architecture duplication.

## Visual UI/UX implementation — exact-screen fidelity
1. Match midnight background, large frosted connect orb, central power glyph, orbit nodes, translucent Best Location card, Ping/Download/Upload/IP cards, five-tab navigation.
2. Disabled/unknown metrics show -- and 'No tunnel configured' truth; no fake numbers, no invented Best Location, no invented 'IP Masking'.
3. Connection button visually reflects state transitions, with shimmer/spinner only while backend attempts a real connection.

## Full-stack functionality — domain, data, Android platform
1. Integrate audited WireGuard Android tunnel library (or justified open implementation) pinned to compatible version; use real Android VpnService permission flow.
2. Expose SessionRepository + finite-state machine with true config validation, permission denial, connect, disconnect, onRevoke, network loss, process death and foreground-service behavior.
3. Add safe minimal user-owned test configuration path required to exercise tunnel now (minimal file picker/parser or lab fixture outside repo), later reused in TASK_09.
4. Home reads backend/session state via StateFlow, with idempotent commands, cancellation and race-safe connect/disconnect; no placebo success.
5. Settings/security/stats/navigation tabs route to appropriate honest screens or temporarily explicitly labeled unavailable navigation, never false active services.

## Specific acceptance tests and runtime evidence
1. Unit tests exhaustive transitions: repeated connect, denied permission, invalid key, race disconnect/connect, backend error and cold restart.
2. Instrumented emulator VPN consent UI + valid owned gateway handshake; verify traffic route through controlled egress and disconnect restores normal routing.
3. No owned gateway? deliver local engine and emulator state tests; mark VPN_END_TO_END_BLOCKED and do not claim verified tunnel or full merge.
4. Kill/revoke VPN; airplane mode; background/foreground; check Notification channel/lifecycle; compare AR/EN Day/Night screenshots.

## Security, privacy and truthfulness
1. No embedded private keys, fake IP/speed/latency or cross-app tunnel leaks; redact logging.
2. Obtain explicit user permission; comply with Android VpnService lifecycle and lockdown considerations.

## Global verification protocol — mandatory for this task
1. Run JVM unit tests including new boundary/race/validation cases; collect command, exit code, passing count and path to reports.
2. Run Android Lint and `assembleDebug`; confirm Gradle/environment versions and no unreviewed dependency/license or security advisories.
3. Execute Android emulator UI tests, real navigation, device rotation/insets where applicable, process restart, error/permission path and foreground/background behavior; save screenshots at 360dp & 411dp, AR/EN, Day/Night plus reference-comparison report.
4. For VPN or network-related functionality, test an **authorized owned gateway or explicitly user-imported endpoint**; capture redacted handshake, route, assigned DNS, IPv4/IPv6 behavior, actual traffic and disconnect. No server available means this proof is BLOCKED, not PASS.
5. If available test physical Android device; otherwise state DEVICE_UNVERIFIED, not production-ready. Inspect logcat for crashes and leaked keys.
6. Create branch/PR, review CI for exact implementation SHA, address findings, merge only once this task meets acceptance gates, verify remote `origin/main` SHA and CI on merged commit. Do not proceed to next task before passing all mandatory merge gates. A missing external provider goes to `docs/BLOCKERS.md`; continue independent work **without inventing closure**.

## Acceptance criteria
1. Connection button reaches real system VPN service; state displayed cannot be falsified by timers.
2. Owned gateway e2e traffic and teardown proof recorded for full acceptance, otherwise task remains blocked truthfully.
4. AR/EN RTL/LTR, Day/Dark, dynamic text, semantics, 48dp touch, screen spacing, animation quality and insets are visually checked and results attached.
5. No stub/Fake success in production; update `docs/PROGRESS_LEDGER.md`, `docs/BLOCKERS.md` and `docs/evidence/TASK_02.md` with exact verified evidence.
6. PR merged to main with recorded merge commit, exact origin/main SHA and post-merge checks **before starting TASK_03**.

## Completion report template (copy then replace with real evidence)
```yaml
task: TASK_02
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
