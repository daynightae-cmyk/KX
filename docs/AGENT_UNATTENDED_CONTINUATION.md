# KX VPN — UNATTENDED CONTINUATION DIRECTIVE

OWNER AUTHORIZATION: Continue executing during my absence with maximum safe effort, **without asking for permission between independent non-destructive implementation tasks**. Aim to finish **TASK_00 through TASK_10**; if bounded by execution/tool/context budgets, complete the maximum verified number (prefer 5–6 if feasible) and leave a durable handoff. This is an instruction for the **running agent**, not a claim that an agent can work after its session terminates.

## Current known reality — verify afresh
Repository: https://github.com/daynightae-cmyk/KX
Canonical local folder: D:\KNOUX-Workspace\KX
Integrated branch: main
Serial task manifest: docs/TASK_ORDER.md
Visual sources: assets/reference/00-brand-logo.png ... 10-profile-settings.png

Two TASK_00 candidate PRs may overlap:
- PR #1, branch task/00-identity-splash, has an Android workflow and independent changes. Do not delete, overwrite or automatically dismiss it.
- PR #2, branch agent/kx-task-00-native-splash, is draft and has another independent TASK_00 implementation.
- The inactive CI template for PR #2 was promoted to .github/workflows/android.yml in the PR #2 branch. It runs build/lint/unit and Android emulator tests for API 26/35. At the time of creation, CI was only **in progress**, not verified green.
- The prior PR #1 runner log for SHA 7589e19 reported './gradlew: Permission denied' (exit 126), so do not blindly copy failing CI steps. The new workflow includes chmod +x ./gradlew.
- The local working directory has been observed on task/00-identity-splash with a unique untracked reference.rar. Treat this as protected, do not delete or modify it.
- Full integration baseline on main at planning setup: 705af83d04399e6a3dd909015f8796a6cb0c0dbe. Inspect newer SHAs; do NOT treat this historical SHA as current.

## The highest-priority next action
1. Inspect GitHub and local Git reality: HEAD, origin/main, both open PR diffs, tests, active CI runs, branches, worktrees, untracked/reflog/stash and other agent activity. Recover and safeguard all unique work in PR #1 and PR #2; do not duplicate features, throw away branches, overwrite dirty worktrees or force-push.
2. Watch the actual CI run for PR #2 exact head SHA and inspect failed job logs; perform a narrowly scoped repair for any verified failure. Confirm API 26/35 emulator tests, build/lint/unit test reports, screenshot evidence and test device limits. CI start is NOT PASS.
3. Compare both TASK_00 implementations and choose a safe integration strategy retaining valuable native code, design details, security tests and CI; preserve unrelated unique work. Only merge a properly verified PR into main once full TASK_00 acceptance gates pass. No fake closure.
4. Once TASK_00 is merged and remote main SHA/CI verified, proceed in strict order: TASK_01 (Welcome) → TASK_02 (Home + actual WireGuard-backed Android VPN service) → TASK_03 (real configs/servers) → TASK_04 (connected session) → TASK_05 (Security Center) → TASK_06 (Split Tunneling) → TASK_07 (Advanced Network) → TASK_08 (Statistics) → TASK_09 (Secure Import) → TASK_10 (Settings/release readiness).
5. For each new task READ its dedicated docs/tasks/TASK_XX file and linked source screenshot, implement full Compose UI + domain + secure storage + Android backend + error/empty states + AR/EN + RTL + Day/Dark + tests + runtime screenshots + evidence, create PR, confirm actual CI, merge safely into main, verify post-merge SHA, update ledger and move to the next task automatically.

## Never stop just because an optional provider is missing
- Prioritize a zero-subscription provider-independent product: user-owned WireGuard config + owned/authorized VPN test gateway; free public global VPN servers and infinite bandwidth must not be promised.
- If service keys, private gateway, Google Play credentials, third-party provider access, domain/payment/login integration or hardware are unavailable, work on all independently testable components within the task, disable unsupported options and record the exact reason in docs/BLOCKERS.md.
- An unavailable OPTIONAL external subfeature may be deferred and honestly excluded from the supported scope; never call it delivered. If a CORE acceptance gate is blocked, do not merge incomplete functionality by changing the goalposts, bypassing tests or faking a successful network connection.
- Try safe technical fixes first: runner permissions, Gradle repository access, Android SDK setup, dependency compatibility, flaky emulator startup, visual mismatches, test assertions. Do not assume a local DNS/Maven failure automatically reproduces on GitHub Actions.
- Avoid waiting indefinitely: retry only with bounded exponential/backoff or well-defined limits; after diagnosis, proceed with other valid work in the current task and document the block.

## Bans
- No prompts between tasks, no mere plans, no screenshots pasted as entire fake UI, no hardcoded connected/server/ping/upload/download/IP values, no fake VPN service.
- No reset --hard, clean, destructive branch or worktree deletion, force push, secrets exposure, credentials in commits, unsupported protocol/privacy claims, or unapproved destructive cleanup.
- No "100% / PASS / COMPLETE / MERGED / RELEASED" unless primary command output, exact commit SHA, verified CI, screenshot and network proof justify it.
- No user-facing promise to run after the current invocation has ended; if the agent's execution budget ends, submit a complete resumable checkpoint.

## Task completion report — required each cycle
TASK_ID; ACTUAL_BASE_SHA; PR; IMPLEMENTATION_SHA; CI_RUN_URL; TEST_NAMES_COUNTS_RESULTS; EMULATOR_API; DEVICE_VERIFICATION_STATUS; SCREENSHOT_DIFF; REAL_SERVICE_NETWORK_PROOF; SECURITY_FINDINGS; MERGE_SHA; ORIGIN_MAIN_SHA; DEFERRED_EXTERNAL; BLOCKERS; NEXT_TASK.
Update docs/PROGRESS_LEDGER.md and docs/evidence/TASK_XX.md with reproducible evidence.

**START NOW from the precise current Git/CI state. Keep working through as many genuinely verified tasks as the current execution allows without further questions.**

---

## Reconciliation status — 2026-11-03 (recorded when this file was integrated)

This directive was authored on the `agent/kx-task-00-native-splash` branch (PR #2),
which carried a second, independent TASK_00 implementation under the package
`com.knoux.kx`.

Outcome, recorded so nobody has to rediscover it:

- **TASK_00 is merged and CI-verified** from PR #1, under `com.kx.vpn`. Current
  integrated head: `143a0aa…` (includes PR #3 cache fix and PR #4 startup-deadline fix).
- **PR #2's code was not merged.** Integrating a second parallel app implementation
  would duplicate every screen, theme, test and route, which the owner directive
  explicitly forbids ("Prevent duplicate implementations"). The branch
  `agent/kx-task-00-native-splash` is **retained untouched** so nothing is lost.
- **Two unique documents from PR #2 were salvaged here:** this file, and `NOTICE.md`
  (which closed a real gap — TASK_00 requires a license/attribution note and PR #1
  shipped without one).
- **PR #2's `docs/ci/android.yml.example` is superseded**, not discarded: it existed
  because that agent's publisher could not write `.github/workflows/*` (HTTP 403
  `workflow_file_not_allowed`). `.github/workflows/ci.yml` is live, runs on every push
  and PR, and is green — including a Gradle-wrapper validation step, the 11 reference
  asset SHA-256 checks and a secret scan.
- **The emulator question is now an owner decision, not an agent one.** This machine
  has VT-x enabled in firmware but no running hypervisor, and no `emulator` or
  `system-images` packages. Enabling WHPX or installing AEHD needs elevation and
  possibly a reboot. The owner chose to continue without an emulator, so **G4 stays
  `DEVICE_UNVERIFIED` for every task** and is recorded as BLOCKER-001 rather than being
  quietly satisfied by JVM renders.
