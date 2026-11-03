# TASK_10 — Profile, global preferences, disclosure and release readiness

**Execution:** STRICT SERIAL, no jumping ahead. Owner goal: polished faithful native UI **and** real service behavior in each full-stack PR.
**One exact screenshot:** [10-profile-settings.png](../../assets/reference/10-profile-settings.png)
**Reference path:** `assets/reference/10-profile-settings.png`
**Source hash:** verify against `docs/ASSET_MANIFEST.md`; never edit this immutable reference.
**Git branch:** `task/10-profile-settings` (or compatible task namespace).
**Prerequisite:** TASK_09 has MERGED_VERIFIED with exact origin/main SHA and evidence.
**Task owner:** implementation agent; no assumption that work is already done.

## Objective
Deliver fully functional settings, optional profile only if provider exists, and complete final integrated Android product verification.

## Reality, baseline and scope isolation
- Verify `D:\KNOUX-Workspace\KX`, remote `https://github.com/daynightae-cmyk/KX.git`, `git fetch`, HEAD SHA, origin/main SHA, status, reflog, stash, all worktrees, outstanding PR/CI, untracked/unique work, active agents.
- Protect all unique work without destructive reset, clean, force push or branch deletion. Stay isolated from parallel agents; preserve previous completed behavior, identity and tests.
- Read `AGENTS.md`, `docs/ENGINEERING_CONTRACT.md`, `docs/DESIGN_SYSTEM.md`, `docs/SECURITY_MODEL.md`, `docs/QUALITY_GATES.md` first; check earlier ledger evidence before writing code.
- Open this **specific PNG** and verify SHA-256. Convert the artwork into responsive, semantic Kotlin/Compose widgets and real actions; **never** display the whole artwork as an unclickable screen.
- The source PNG includes demonstrative values that are not live data. No inventing provider APIs, account status, countries, ping, privacy score, encryption state, download/upload measurements, subscription or endpoint.
- Restrict changes to necessary app modules, resources, tests, CI and evidence for THIS task. Avoid refactoring unrelated screens or introducing architecture duplication.

## Visual UI/UX implementation — exact-screen fidelity
1. Match KX logo orb, profile header, glass grouped settings rows, pill toggles, bottom nav and lavender/cyan atmospheric lighting.
2. Do not render fake user@kxvpn.com or Premium unless real authentication/subscription service genuinely exists; instead show Local Profile / No account required.
3. Night and Day layouts fully rendered, Arabic RTL labels and overflow-safe bottom navigation.

## Full-stack functionality — domain, data, Android platform
1. Functional language switch AR/EN with proper configuration persistence; appearance Day/Dark/System; accessibility/reduced-motion support.
2. Notification and background connection preferences wired to actual Android capabilities; startup 'auto-connect' conditioned on system permission and verified config.
3. Account screen either truthful local device profile or verified remote provider integration; no pretend login/password or upgrade flow.
4. About/version/build/license disclosures from actual metadata, Privacy Policy local truthful draft and support link only if valid, export/erase user data and secret stores.
5. App-wide navigation deep links, preferences synchronization, settings reset with explicit confirmation, offline/notification permission denial and process death.
6. Final release-readiness audit: dependency/OSS license review, secure signing (not shared), Play VpnService requirements, privacy disclosure, memory/performance, battery, device compatibility, E2E server/leak tests; **do not claim released until proof**.

## Specific acceptance tests and runtime evidence
1. Preferences persist across kill/upgrade/restore; language/theme transition no navigation break; bidi UI semantics.
2. End-to-end tour of ALL 10 screens and reference comparisons (one image per screen), Day/Night AR/EN, two form factors.
3. Clean CI exact SHA, lint, unit, instrumentation, controlled VPN network path, dns/IPv6 and split routing, device/emulator, app cold start and background.
4. Secret scanning and privacy disclosure review; test lifecycle/notifications during always-on, reboot and permission changes.
5. If external/provider/Play/hardware still unavailable, record unreleased status in final blocker report; never fabricate production readiness.

## Security, privacy and truthfulness
1. No fabricated identities, policy URLs, premium flags or anonymous/no-logs claims.
2. Secure deletion, least privilege, privacy retention, dependency licensing and release integrity.

## Global verification protocol — mandatory for this task
1. Run JVM unit tests including new boundary/race/validation cases; collect command, exit code, passing count and path to reports.
2. Run Android Lint and `assembleDebug`; confirm Gradle/environment versions and no unreviewed dependency/license or security advisories.
3. Execute Android emulator UI tests, real navigation, device rotation/insets where applicable, process restart, error/permission path and foreground/background behavior; save screenshots at 360dp & 411dp, AR/EN, Day/Night plus reference-comparison report.
4. For VPN or network-related functionality, test an **authorized owned gateway or explicitly user-imported endpoint**; capture redacted handshake, route, assigned DNS, IPv4/IPv6 behavior, actual traffic and disconnect. No server available means this proof is BLOCKED, not PASS.
5. If available test physical Android device; otherwise state DEVICE_UNVERIFIED, not production-ready. Inspect logcat for crashes and leaked keys.
6. Create branch/PR, review CI for exact implementation SHA, address findings, merge only once this task meets acceptance gates, verify remote `origin/main` SHA and CI on merged commit. Do not proceed to next task before passing all mandatory merge gates. A missing external provider goes to `docs/BLOCKERS.md`; continue independent work **without inventing closure**.

## Acceptance criteria
1. All settings have true effect, are persisted, and are localized.
2. Whole app integration passes recorded checks; production release status remains honest if signing/provider/device proof unavailable.
4. AR/EN RTL/LTR, Day/Dark, dynamic text, semantics, 48dp touch, screen spacing, animation quality and insets are visually checked and results attached.
5. No stub/Fake success in production; update `docs/PROGRESS_LEDGER.md`, `docs/BLOCKERS.md` and `docs/evidence/TASK_10.md` with exact verified evidence.
6. PR merged to main with recorded merge commit, exact origin/main SHA and post-merge checks **before starting TASK_11** (final task: release decision, no successor).

## Completion report template (copy then replace with real evidence)
```yaml
task: TASK_10
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

## Owner-added Founder / Developer experience

The About/settings scope now includes [TASK_10A](TASK_10A_founder-developer-identity.md):
a brand-native Founder & Developer route with owner-supplied identity, grouped expertise,
real safe contact/copy actions and native accessibility/visual/runtime evidence. This
is a registered requirement, **NOT-IMPLEMENTED**, and does not bypass serial gates.
