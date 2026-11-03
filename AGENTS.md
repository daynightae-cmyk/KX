# KX VPN — OWNER EXECUTION DIRECTIVE

**HIGHEST PRIORITY: EXECUTE, DO NOT ONLY PLAN. NEVER MARK A TASK DONE WITHOUT PRIMARY EVIDENCE.**

Canonical checkout: `D:\KNOUX-Workspace\KX`
Remote: `https://github.com/daynightae-cmyk/KX.git`
Target integrated branch: `main`
Ordered scope: `docs/TASK_ORDER.md`.

## Reality and preservation before every mutation
1. Check current directory, HEAD, `origin/main`, `git status --porcelain=v1`, `git remote -v`, branch history, `git worktree list`, reflog, `git stash list`, untracked files, open PRs, latest CI, active agents.
2. Preserve all unique content before modifying; do not reset --hard, clean, force push, overwrite branches, delete worktrees or destroy files. If another agent operates concurrently, avoid clobbering their working set.
3. Open the exact one-to-one task screenshot and verify SHA-256. Treat depicted country list, public IP, Mbps, Ping, user@..., Premium, Fully Protected etc. as **fictional design values**.
4. Implement native UI + navigation + runtime/domain service + persistence + platform + error states + AR/EN + RTL/LTR + day/night + tests **in the same task**, not split into cosmetic-only and functional-only closure.

## Serial progression: never skip or jump
TASK_00 (logo/splash) → TASK_01 (welcome) → TASK_02 (home & tunnel session) → TASK_03 (servers) → TASK_04 (connected) → TASK_05 (security) → TASK_06 (split tunnel) → TASK_07 (network) → TASK_08 (stats) → TASK_09 (import) → TASK_10 (settings/release checks).
After each task: run real unit + UI/instrumentation tests, lint/build, startup & interaction, screenshot comparison, network checks if relevant. Record exact commands, output, SHA, device/emulator status, PR, CI, merge SHA and origin/main HEAD. Merge verified PR to main; **do not begin N+1 before N is merged and post-merge checked**.

## Blocked provider/secrets
External API keys, server fleet, payment or release credentials are not required for local baseline. Use honest, disabled UI and a provider-free user-owned WireGuard configuration/controlled lab tunnel. Record unavailable functionality and exact root cause in `docs/BLOCKERS.md`; continue only independent safe work within the current slice. Never claim a blocked requirement passed or merge a deceptive/unsafe feature. Do not wait indefinitely for optional external services. If essential acceptance remains blocked, mark BLOCKED rather than force the merge.

## Submission template per task
BASE_SHA; IMPLEMENTATION_SHA; PR_URL; MERGE_SHA; ORIGIN_MAIN_SHA; changed files; visual before/after images and reference diff; AR/EN and Day/Dark checks; command outputs and tests; runtime screenshots + emulator/physical device; VPN network/DNS/IP proof where relevant; security review; known blockers; exact status (NOT_STARTED/IN_PROGRESS/DEVICE_UNVERIFIED/BLOCKED/PR_OPEN/MERGED_VERIFIED). Build success alone is not PASS. No unsolicited destructive cleanup.

## External dependency continuation exception — explicit owner instruction
An OPTIONAL provider, subscription, API key, global server fleet, Play signing credential, or account integration must **never stop progress**. Do not attempt it indefinitely: implement a real provider-free equivalent where possible; otherwise keep its control hidden/disabled and clearly describe what was not delivered. Scope the PR to the real functioning capability, with the optional unsupported capability recorded as `DEFERRED_EXTERNAL` in `docs/BLOCKERS.md`, CI/evidence and ledger. A fully tested and secure provider-independent slice MAY merge despite a separately documented optional external feature being deferred. This is not permission to call that external feature complete.
For CORE VPN behaviors, do not substitute fake backend verification. Provision an authorized local/self-hosted WireGuard test gateway when possible at zero subscription cost. If core traffic tests remain blocked, keep implementation evidence separate and do not claim network verification. Continue safe independent work within the current task, not phantom completion.
