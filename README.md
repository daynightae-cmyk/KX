# KX VPN — KNOUX Private Network

**Repository state (2026-11-03):** planning, brand references and executable agent task specifications. **No functional VPN app, APK, configured gateway, tested network or released service is claimed by this setup.**

## Purpose
Build a genuine native Android VPN client in Kotlin + Jetpack Compose based on the supplied KX circular glass-orbit identity. Work strictly in production-quality, verified vertical slices. A provider-free path (user-owned WireGuard endpoint/imported config) is the initial transport target; global exit servers are **not free by definition**.

## Start here
1. Read [AGENTS.md](AGENTS.md), [docs/ENGINEERING_CONTRACT.md](docs/ENGINEERING_CONTRACT.md), [docs/QUALITY_GATES.md](docs/QUALITY_GATES.md) and [docs/SECURITY_MODEL.md](docs/SECURITY_MODEL.md).
2. Inspect [docs/ASSET_MANIFEST.md](docs/ASSET_MANIFEST.md): one source screenshot per task; 11 source images in [assets/reference](assets/reference).
3. Execute [docs/TASK_ORDER.md](docs/TASK_ORDER.md): **TASK_00 then TASK_01...TASK_10**. No next task until previous task passes the defined gate and is merged into main.
4. Update [docs/PROGRESS_LEDGER.md](docs/PROGRESS_LEDGER.md), [docs/BLOCKERS.md](docs/BLOCKERS.md), and `docs/evidence/TASK_XX.md` with actual evidence, not reports from another agent.

## Nonnegotiables
Images are references, not UI screenshots or assets to paste as full-screen backgrounds. No fake connected/protected/paid/server/speed/latency/IP status. Never commit secrets or invent external services. Main is integrated truth; inspect open branches/worktrees for unique work. Work in isolated PRs, verify exact SHA + CI + real runtime; only then merge. See contract for complete checklist.

**Suggested baseline:** Kotlin, Android SDK, Jetpack Compose / Material 3, Coroutines, StateFlow, Room/DataStore as needed, Android VpnService + audited WireGuard tunnel library. Do not assert unsupported protocols until implemented and tested.

Links: https://developer.android.com/develop/connectivity/vpn | https://github.com/WireGuard/wireguard-android
