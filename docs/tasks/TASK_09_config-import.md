# TASK_09 — Secure import/export from QR, .conf, clipboard and manual entry

**Execution:** STRICT SERIAL, no jumping ahead. Owner goal: polished faithful native UI **and** real service behavior in each full-stack PR.
**One exact screenshot:** [09-import-configuration.png](../../assets/reference/09-import-configuration.png)
**Reference path:** `assets/reference/09-import-configuration.png`
**Source hash:** verify against `docs/ASSET_MANIFEST.md`; never edit this immutable reference.
**Git branch:** `task/09-config-import` (or compatible task namespace).
**Prerequisite:** TASK_08 has MERGED_VERIFIED with exact origin/main SHA and evidence.
**Task owner:** implementation agent; no assumption that work is already done.

## Objective
Provide complete secure self-sufficient onboarding of VPN configs, with strict parsers and no secret leakage.

## Reality, baseline and scope isolation
- Verify `D:\KNOUX-Workspace\KX`, remote `https://github.com/daynightae-cmyk/KX.git`, `git fetch`, HEAD SHA, origin/main SHA, status, reflog, stash, all worktrees, outstanding PR/CI, untracked/unique work, active agents.
- Protect all unique work without destructive reset, clean, force push or branch deletion. Stay isolated from parallel agents; preserve previous completed behavior, identity and tests.
- Read `AGENTS.md`, `docs/ENGINEERING_CONTRACT.md`, `docs/DESIGN_SYSTEM.md`, `docs/SECURITY_MODEL.md`, `docs/QUALITY_GATES.md` first; check earlier ledger evidence before writing code.
- Open this **specific PNG** and verify SHA-256. Convert the artwork into responsive, semantic Kotlin/Compose widgets and real actions; **never** display the whole artwork as an unclickable screen.
- The source PNG includes demonstrative values that are not live data. No inventing provider APIs, account status, countries, ping, privacy score, encryption state, download/upload measurements, subscription or endpoint.
- Restrict changes to necessary app modules, resources, tests, CI and evidence for THIS task. Avoid refactoring unrelated screens or introducing architecture duplication.

## Visual UI/UX implementation — exact-screen fidelity
1. Match large glass Import Configuration header, four action cards, QR scanning orb and recent import cards.
2. Recent imports show only saved real configs and redacted safe labels; no fake sample 'US-01' entries.
3. Camera permission educational state, QR focus, successful import details and validation error state in same visual system.

## Full-stack functionality — domain, data, Android platform
1. WireGuard .conf file via Android Storage Access Framework; QR payload scanner only after camera consent; clipboard paste only on user action; manual config form with validation.
2. Parser: max size, supported characters/encoding, required public/private key forms, CIDR/routes/DNS/endpoint validation, duplicate detection, safe nickname generation.
3. Private key storage with Android appropriate protected storage; no plaintext fallback; never expose secrets in recent list, debug, analytics or notifications.
4. Import/replace conflicts, cancellation, revoked file permission, malformed QR, empty clipboard, invalid schema and large malicious data handled gracefully.
5. Integrate repository/session engine established TASK_02; imported config appears in TASK_03 and is connectable; deleting/clearing scrubs stored secrets and dependent saved selection.
6. Unsupported OpenVPN may be detected and rejected with helpful guidance, not silently translated or promised.

## Specific acceptance tests and runtime evidence
1. Fuzz/property-based parser tests with invalid/oversized/duplicate inputs and redaction assertion.
2. Instrumented import via document picker and QR scanner (permission deny/grant), clipboard, manual, app restart, delete/rollback.
3. Owned endpoint QR/.conf roundtrip and actual VPN connect; secret absence from logcat/screenshots/backups.
4. Screen comparisons reference AR/EN Day/Night; TalkBack scanner and form errors.

## Security, privacy and truthfulness
1. No automatic external fetching, no logging QR or config content, no plaintext preferences.
2. Backup exclusion, key wrapping failure handling, clear-all secure semantics and negative tests.

## Global verification protocol — mandatory for this task
1. Run JVM unit tests including new boundary/race/validation cases; collect command, exit code, passing count and path to reports.
2. Run Android Lint and `assembleDebug`; confirm Gradle/environment versions and no unreviewed dependency/license or security advisories.
3. Execute Android emulator UI tests, real navigation, device rotation/insets where applicable, process restart, error/permission path and foreground/background behavior; save screenshots at 360dp & 411dp, AR/EN, Day/Night plus reference-comparison report.
4. For VPN or network-related functionality, test an **authorized owned gateway or explicitly user-imported endpoint**; capture redacted handshake, route, assigned DNS, IPv4/IPv6 behavior, actual traffic and disconnect. No server available means this proof is BLOCKED, not PASS.
5. If available test physical Android device; otherwise state DEVICE_UNVERIFIED, not production-ready. Inspect logcat for crashes and leaked keys.
6. Create branch/PR, review CI for exact implementation SHA, address findings, merge only once this task meets acceptance gates, verify remote `origin/main` SHA and CI on merged commit. Do not proceed to next task before passing all mandatory merge gates. A missing external provider goes to `docs/BLOCKERS.md`; continue independent work **without inventing closure**.

## Acceptance criteria
1. Four interactive import methods work for supported formats; real WireGuard tunnel consumes saved config.
2. Hostile configs cannot crash or leak raw secrets.
4. AR/EN RTL/LTR, Day/Dark, dynamic text, semantics, 48dp touch, screen spacing, animation quality and insets are visually checked and results attached.
5. No stub/Fake success in production; update `docs/PROGRESS_LEDGER.md`, `docs/BLOCKERS.md` and `docs/evidence/TASK_09.md` with exact verified evidence.
6. PR merged to main with recorded merge commit, exact origin/main SHA and post-merge checks **before starting TASK_10**.

## Completion report template (copy then replace with real evidence)
```yaml
task: TASK_09
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
