# TASK_06 — Split tunneling per-app routing with clear leak semantics

**Execution:** STRICT SERIAL, no jumping ahead. Owner goal: polished faithful native UI **and** real service behavior in each full-stack PR.
**One exact screenshot:** [06-split-tunneling.png](../../assets/reference/06-split-tunneling.png)
**Reference path:** `assets/reference/06-split-tunneling.png`
**Source hash:** verify against `docs/ASSET_MANIFEST.md`; never edit this immutable reference.
**Git branch:** `task/06-split-tunneling` (or compatible task namespace).
**Prerequisite:** TASK_05 has MERGED_VERIFIED with exact origin/main SHA and evidence.
**Task owner:** implementation agent; no assumption that work is already done.

## Objective
Implement actual per-app VPN include/exclude policy mapped to Android API and reflected in tunnel configuration.

## Reality, baseline and scope isolation
- Verify `D:\KNOUX-Workspace\KX`, remote `https://github.com/daynightae-cmyk/KX.git`, `git fetch`, HEAD SHA, origin/main SHA, status, reflog, stash, all worktrees, outstanding PR/CI, untracked/unique work, active agents.
- Protect all unique work without destructive reset, clean, force push or branch deletion. Stay isolated from parallel agents; preserve previous completed behavior, identity and tests.
- Read `AGENTS.md`, `docs/ENGINEERING_CONTRACT.md`, `docs/DESIGN_SYSTEM.md`, `docs/SECURITY_MODEL.md`, `docs/QUALITY_GATES.md` first; check earlier ledger evidence before writing code.
- Open this **specific PNG** and verify SHA-256. Convert the artwork into responsive, semantic Kotlin/Compose widgets and real actions; **never** display the whole artwork as an unclickable screen.
- The source PNG includes demonstrative values that are not live data. No inventing provider APIs, account status, countries, ping, privacy score, encryption state, download/upload measurements, subscription or endpoint.
- Restrict changes to necessary app modules, resources, tests, CI and evidence for THIS task. Avoid refactoring unrelated screens or introducing architecture duplication.

## Visual UI/UX implementation — exact-screen fidelity
1. Same pale lilac branded header, top two-mode segmented control, searchable installed-app list with app icons and glass rows.
2. Lists show installed relevant applications and actual selection; artwork's Chrome/YouTube/Spotify icons are examples, not hardcoded dependencies.
3. Live count summary depends on real installed/selected apps; consistent 48dp toggles and RTL-safe labels.

## Full-stack functionality — domain, data, Android platform
1. Android VpnService.Builder.setAllowedApplications OR setDisallowedApplications, never both for same VPN session; use support-specific routing APIs as appropriate.
2. Persist allow/deny mode + stable package IDs; reconcile removed apps, apps without visibility, renamed packages and Android package visibility restrictions.
3. On policy change, explain tunnel rebuild necessity; safely tear down/re-establish route with user awareness and audit applicable traffic.
4. Search application labels localized, icons loaded safely, special work profile/system apps considered only if supported.
5. Explain bypass privacy tradeoff, DNS policy, exceptional traffic, lockdown incompatibility, fallback when unsupported.

## Specific acceptance tests and runtime evidence
1. Emulator with installed test apps; verify one app traverses tunnel and another bypasses (or fails under lockdown), using controlled endpoint logging without personal data.
2. Permission, app uninstall/reinstall, quick toggle, app catalogue empty/long, user cancellation, lifecycle/reconnect cases.
3. Screenshot diffs AR/EN, Day/Night and small/large screen; talkback per-app state.

## Security, privacy and truthfulness
1. Avoid promising that VPN-only routing covers excluded apps or non-TUN system traffic.
2. No extraction of installed application list outside device.

## Global verification protocol — mandatory for this task
1. Run JVM unit tests including new boundary/race/validation cases; collect command, exit code, passing count and path to reports.
2. Run Android Lint and `assembleDebug`; confirm Gradle/environment versions and no unreviewed dependency/license or security advisories.
3. Execute Android emulator UI tests, real navigation, device rotation/insets where applicable, process restart, error/permission path and foreground/background behavior; save screenshots at 360dp & 411dp, AR/EN, Day/Night plus reference-comparison report.
4. For VPN or network-related functionality, test an **authorized owned gateway or explicitly user-imported endpoint**; capture redacted handshake, route, assigned DNS, IPv4/IPv6 behavior, actual traffic and disconnect. No server available means this proof is BLOCKED, not PASS.
5. If available test physical Android device; otherwise state DEVICE_UNVERIFIED, not production-ready. Inspect logcat for crashes and leaked keys.
6. Create branch/PR, review CI for exact implementation SHA, address findings, merge only once this task meets acceptance gates, verify remote `origin/main` SHA and CI on merged commit. Do not proceed to next task before passing all mandatory merge gates. A missing external provider goes to `docs/BLOCKERS.md`; continue independent work **without inventing closure**.

## Acceptance criteria
1. Real per-package routing verified with controlled network and state after reconnect.
2. No toggle is only a visual illusion.
4. AR/EN RTL/LTR, Day/Dark, dynamic text, semantics, 48dp touch, screen spacing, animation quality and insets are visually checked and results attached.
5. No stub/Fake success in production; update `docs/PROGRESS_LEDGER.md`, `docs/BLOCKERS.md` and `docs/evidence/TASK_06.md` with exact verified evidence.
6. PR merged to main with recorded merge commit, exact origin/main SHA and post-merge checks **before starting TASK_07**.

## Completion report template (copy then replace with real evidence)
```yaml
task: TASK_06
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
