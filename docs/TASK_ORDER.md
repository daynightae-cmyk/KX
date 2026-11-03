# Mandatory serial queue
One full-stack task, one visual reference, one reviewed PR and verified merge at a time.

| Order | Complete task specification | One-to-one image | Functional slice |
|---|---|---|---|
| 00 | [TASK_00](tasks/TASK_00_identity-splash.md) | [PNG](../assets/reference/00-brand-logo.png) | Build baseline/design system/real splash |
| 01 | [TASK_01](tasks/TASK_01_welcome-onboarding.md) | [PNG](../assets/reference/01-welcome.png) | Onboarding+permissions/first run |
| 02 | [TASK_02](tasks/TASK_02_home-disconnected.md) | [PNG](../assets/reference/02-home-disconnected.png) | Home+real VPN session manager |
| 03 | [TASK_03](tasks/TASK_03_server-selection.md) | [PNG](../assets/reference/03-server-selection.png) | Verified location/config chooser |
| 04 | [TASK_04](tasks/TASK_04_connected-session.md) | [PNG](../assets/reference/04-connected-session.png) | Verified tunnel state+traffic |
| 05 | [TASK_05](tasks/TASK_05_security-center.md) | [PNG](../assets/reference/05-security-center.png) | Truthful security diagnostics |
| 06 | [TASK_06](tasks/TASK_06_split-tunneling.md) | [PNG](../assets/reference/06-split-tunneling.png) | Per-app routing policy |
| 07 | [TASK_07](tasks/TASK_07_advanced-network.md) | [PNG](../assets/reference/07-advanced-network.png) | Supported network settings |
| 08 | [TASK_08](tasks/TASK_08_statistics.md) | [PNG](../assets/reference/08-statistics.png) | Measured local usage/history |
| 09 | [TASK_09](tasks/TASK_09_config-import.md) | [PNG](../assets/reference/09-import-configuration.png) | QR/file/manual secure import |
| 10 | [TASK_10](tasks/TASK_10_profile-settings.md) | [PNG](../assets/reference/10-profile-settings.png) | User settings+release preflight |

**Hard rule:** Task N+1 cannot begin until N passes G0–G7 in `docs/QUALITY_GATES.md`, has a reviewed PR, successful exact-SHA CI, merge and post-merge verification, plus updated evidence ledger. If required external proof impossible, mark BLOCKED and work only on safe independent portions. Don't say COMPLETE.

A minimal secure configuration handoff may be implemented under Task 02 to demonstrate real session control; the rich import UI and hostile-import handling are expanded in Task 09, not duplicated.

Lifecycle: Reality → protect unique work → exact image QA → native UI+service+data → tests → emulator/device runtime → security/network proof → PR/CI → merge main → post-merge SHA + ledger → next task.

## Nonstop handling for optional third-party dependencies
Avoid getting stuck on external keys/providers. Defer an *optional external* capability into `docs/BLOCKERS.md`, disable unsupported UI, and merge the demonstrably complete **provider-independent scoped deliverable** only after all of its gates pass. Then take the next task. This is permitted by the owner; it does not mean the missing provider feature passed, and does not remove genuine core VPN/traffic verification. If the core essential slice fails, do not falsely merge it simply to advance the counter.
