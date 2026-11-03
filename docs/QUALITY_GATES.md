# PASS/merge gate per TASK_XX

G0 **Reality**: record HEAD, origin/main SHA, clean/dirty status, branches/worktrees/reflog/stash/untracked, live agents, active PRs and latest CI; preserve unique changes.
G1 **Visual**: task's linked PNG opened and hash matched; Compose screenshot comparisons at multiple widths and font scales; EN/AR, RTL/LTR, Day/Dark; orb/ribbons/cards/layout/navigation; real tappable components not flattened bitmap.
G2 **Function**: domain + use case + service + persistence/permissions fully wired; all actions work or explicitly disabled with reason, no fake server/metrics/tunnel state; deny/offline/invalid inputs covered.
G3 **Automated**: relevant `testDebugUnitTest`/module test task, lint, `assembleDebug`, instrumentation UI tests, actual command logs and counts. If toolchain missing, NOT_VERIFIED, not PASS.
G4 **Runtime**: launch on emulator, inspect logcat, test device rotation/insets/power/network lifecycles where relevant; physical Android device test for release, else DEVICE_UNVERIFIED.
G5 **VPN/security**: for transport features validate system consent and user-owned test endpoint, handshake, route, DNS, IPv4/IPv6, reconnect, kill-switch/lockdown semantics, secrets redaction, hostile inputs; none may be faked.
G6 **Integration**: feature branch, PR, reviewed changes, CI success on **exact HEAD SHA**, merge into `main`, post-merge `origin/main` SHA and tests. If PR/CI unavailable record BLOCKED, not MERGED_VERIFIED.
G7 **Handoff**: `docs/evidence/TASK_XX.md`, screenshots, ledger updated, blockers documented, release claims withheld until actual device and distribution validation.

Evidence fields: BASE_SHA / IMPLEMENTATION_SHA / PR / MERGE_SHA / ORIGIN_MAIN_SHA / FILES / TEST_COMMANDS_AND_RESULTS / DEVICE_EMULATOR / VISUAL_DIFF / ACTUAL_SERVICE / NETWORK_PROOF / SECURITY_PROOF / BLOCKERS / STATUS / NEXT_ACTION.
No “100%”, PASS, RELEASED or COMPLETE based on build success, images, or another agent's summary.

## When optional external services are unavailable
Use `DEFERRED_EXTERNAL` for an unavailable **optional** external subfeature only. Test and merge the real provider-free scope with explicit exclusions, never claim the deferred subfeature works, then continue the queue. An unavailable externally hosted fleet is **not** a reason to stop building a WireGuard-config-based VPN client. Core Android tunnel/network functionality still requires controlled-gateway proof before being marked verified.
