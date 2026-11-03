# TASK_07 — Advanced network and protocol controls, with capability gating

**Execution:** STRICT SERIAL, no jumping ahead. Owner goal: polished faithful native UI **and** real service behavior in each full-stack PR.
**One exact screenshot:** [07-advanced-network.png](../../assets/reference/07-advanced-network.png)
**Reference path:** `assets/reference/07-advanced-network.png`
**Source hash:** verify against `docs/ASSET_MANIFEST.md`; never edit this immutable reference.
**Git branch:** `task/07-advanced-network` (or compatible task namespace).
**Prerequisite:** TASK_06 has MERGED_VERIFIED with exact origin/main SHA and evidence.
**Task owner:** implementation agent; no assumption that work is already done.

## Objective
Provide actual available transport/network controls, not fabricated OpenVPN/obfuscation/port-hopping support.

## Reality, baseline and scope isolation
- Verify `D:\KNOUX-Workspace\KX`, remote `https://github.com/daynightae-cmyk/KX.git`, `git fetch`, HEAD SHA, origin/main SHA, status, reflog, stash, all worktrees, outstanding PR/CI, untracked/unique work, active agents.
- Protect all unique work without destructive reset, clean, force push or branch deletion. Stay isolated from parallel agents; preserve previous completed behavior, identity and tests.
- Read `AGENTS.md`, `docs/ENGINEERING_CONTRACT.md`, `docs/DESIGN_SYSTEM.md`, `docs/SECURITY_MODEL.md`, `docs/QUALITY_GATES.md` first; check earlier ledger evidence before writing code.
- Open this **specific PNG** and verify SHA-256. Convert the artwork into responsive, semantic Kotlin/Compose widgets and real actions; **never** display the whole artwork as an unclickable screen.
- The source PNG includes demonstrative values that are not live data. No inventing provider APIs, account status, countries, ping, privacy score, encryption state, download/upload measurements, subscription or endpoint.
- Restrict changes to necessary app modules, resources, tests, CI and evidence for THIS task. Avoid refactoring unrelated screens or introducing architecture duplication.

## Visual UI/UX implementation — exact-screen fidelity
1. Build reference pale glass protocol tabs, MTU slider, DNS picker and advanced setting rows with accurate status.
2. Visually distinguish locked/unsupported protocol cards and explain why; adjust labels for real WireGuard-first app.
3. Value display for MTU/keepalive follows committed config and transport state, not fake illustration.

## Full-stack functionality — domain, data, Android platform
1. WireGuard-only initial protocol with explicit installed capabilities; optional OpenVPN requires separate audited engine and tests before tappable.
2. Validate/apply MTU to tunnel, DNS resolver choice to VPN configuration, persistent keepalive where supported; guard against invalid ranges.
3. Safe config changes while running: transactional restart or wait-until-disconnect with explicit result and rollback.
4. Obfuscation and port hopping are NOT inherent to WireGuard; leave disabled or omit unless genuine backed implementation and controlled tests.
5. Automatic means measured/grounded selection from supported transports, not nonexistent magically self-healing protocol.

## Specific acceptance tests and runtime evidence
1. Unit input boundaries for MTU, DNS, keepalive and rollback; saved and reloaded values.
2. Controlled gateway tests for connectivity/DNS after settings; failing DNS reveals error, no false connected.
3. Unsupported protocol cannot be activated; UI test asserts disabled explanation; reference screenshots AR/EN Day/Night.

## Security, privacy and truthfulness
1. No unintended DNS downgrade or silent bypass; validate imported DNS endpoints.
2. Do not log config/secrets or claim obfuscation without traffic verification.

## Global verification protocol — mandatory for this task
1. Run JVM unit tests including new boundary/race/validation cases; collect command, exit code, passing count and path to reports.
2. Run Android Lint and `assembleDebug`; confirm Gradle/environment versions and no unreviewed dependency/license or security advisories.
3. Execute Android emulator UI tests, real navigation, device rotation/insets where applicable, process restart, error/permission path and foreground/background behavior; save screenshots at 360dp & 411dp, AR/EN, Day/Night plus reference-comparison report.
4. For VPN or network-related functionality, test an **authorized owned gateway or explicitly user-imported endpoint**; capture redacted handshake, route, assigned DNS, IPv4/IPv6 behavior, actual traffic and disconnect. No server available means this proof is BLOCKED, not PASS.
5. If available test physical Android device; otherwise state DEVICE_UNVERIFIED, not production-ready. Inspect logcat for crashes and leaked keys.
6. Create branch/PR, review CI for exact implementation SHA, address findings, merge only once this task meets acceptance gates, verify remote `origin/main` SHA and CI on merged commit. Do not proceed to next task before passing all mandatory merge gates. A missing external provider goes to `docs/BLOCKERS.md`; continue independent work **without inventing closure**.

## Acceptance criteria
1. Every enabled control has real effect verified on transport or reports failure.
2. Unsupported controls cannot masquerade as operational.
4. AR/EN RTL/LTR, Day/Dark, dynamic text, semantics, 48dp touch, screen spacing, animation quality and insets are visually checked and results attached.
5. No stub/Fake success in production; update `docs/PROGRESS_LEDGER.md`, `docs/BLOCKERS.md` and `docs/evidence/TASK_07.md` with exact verified evidence.
6. PR merged to main with recorded merge commit, exact origin/main SHA and post-merge checks **before starting TASK_08**.

## Completion report template (copy then replace with real evidence)
```yaml
task: TASK_07
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
