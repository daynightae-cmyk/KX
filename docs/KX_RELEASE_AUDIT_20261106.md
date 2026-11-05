# KX Private VPN — evidence-based release audit (2026-11-06)

Status: IN_PROGRESS / NOT RELEASED. This is an audit and owner-directed UX change, not a release declaration.

## 1. Sources actually inspected
- Owner-provided screen recording: `42195.mp4`, ~104.8s, H.264 720×1624 + AAC mono. Sampled at 5s intervals and inspected frames at 17, 35, 57 seconds. The recording's spoken audio has NOT been automatically transcribed or independently verified; do not quote dialogue from it.
- Connected Windows device: `DESKTOP-VQKU9P8` using Desktop Commander; repo: `D:\KNOUX-Workspace\KX`.
- `git fetch origin`, Git worktree/status/branch/log, `gh pr list`, `gh run list`, `AGENTS.md`, task specs, progress ledger, blockers, source files and tests.
- No physical Android device is currently visible to `D:\Android\sdk\platform-tools\adb.exe devices -l` (empty list). No direct install, click, battery, DNS or WireGuard handshake has been observed on the user's phone.

## 2. Reality / integration baseline
- `origin/main = eef3765f72670acebf1f668dfd82a715bc3287f3` (merge PR #16, TASK_04).
- TASK_00–04: merged to main. GitHub Android CI and device workflows report successful runs on the TASK_04 merge SHA.
- TASK_05: PR #17 is OPEN DRAFT, head `ce11c1b...` with green Android CI and emulator run. **Not merged**.
- TASK_06: active local branch `task/06-split-tunneling` at `37258391617a48991b837e291107f9b9f1d274d0`; uncommitted `VpnConfigModifier.kt`, `SplitTunnelingRepository.kt`, `ui/split/` and 3 drawables. This content is PRESERVED and MUST NOT be reset, deleted or overwritten.
- TASK_07–10: no verified implementation/merge on current main. Premium licensing not implemented in current Android source.
- GitHub PR #15 also remains open on `agent/task04-live-counters`, PR #13 remains open on profile-vault integrity; inspect uniqueness before merging.
- Repo `app/build.gradle.kts` declares `versionName 0.1.0`, minSdk 26, compileSdk and targetSdk 36.
- `AGENTS.md` requires single task integration with evidence and post-merge checks.

## 3. Visual recording observations / source mismatch
- ~17s: KX orb, Arabic disconnected status, explicit WireGuard import requirement, missing saved profile; no connected state.
- ~30s: extra tools area is displayed.
- ~35s: storage-analysis page reporting ~21% used and local storage measurements.
- ~52–57s: Security and Settings; Settings says no local Premium subscription, and notifications unavailable in that build.
- ~80–100s: Statistics page and chart placeholders/zero measurements.
- `git grep` over current `app/src/main` returned NO matching implementation for the recording's storage-analysis / Premium screen; there may be an APK/source mismatch. APK package + versionCode + signing certificate + Git SHA have not been matched to the phone. Do not assume pushing a repo change updates this installed build.

## 4. Important functional blockers
- No owned WireGuard gateway, valid remote peer or confirmed handshake. No internet route / DNS leak / IPv6 leak / switch / reconnect end-to-end proof. The app supports importing user-owned WireGuard profiles but does not provide a global free VPN fleet.
- No local Android phone connected via ADB. CI emulators API 26/35 are available in GitHub workflow, but do not prove this phone's OEM behavior.
- Existing Security Center explicitly disables unenforced controls. Do not label them protected or switch them on without wiring.
- Robolectric Maven `android-all` download can fail even when code is sound; correlate failures with exact logs, no silent retry claims.
- Sensitive-looking `Personal access token.pdf` exists locally in the checkout and has not been opened. Treat as potentially sensitive; review storage/ignore policy before any release bundle, without printing token values.

## 5. Owner-directed launch UX change (separate safe worktree)
- Isolated branch: `feature/kx-interactive-entry-20261106`
- Worktree: `D:\KNOUX-Workspace\KX-ENTRY-20261106` based on `origin/main`; original TASK_06 tree untouched.
- `LaunchScreen.kt`: no automatic navigation; only tapping the existing KX orb when launch state is resolved and intro is complete navigates. Rapid repeat taps are guarded.
- Original vector Compose brand/identity retained, with subtle native 3D perspective, scale and opacity intro. Standard intro ~1450ms, reduced-motion mode stays static. No new artwork/reference is fabricated.
- EN/AR tap hint and truthful VPN footnote included. Robolectric and Android instrumented tests updated for explicit entry; exact test/build outcomes recorded separately.
- **Not merged**; must pass local compile, JVM tests, Android CI/emulator API 26/35 and visual accessibility comparison before PR can be merged.

## 6. Premium activation — production contract, not implemented yet
- OWNER UX: user taps `KX Pro` > `Contact via WhatsApp` > sends activation request and a privacy-safe license/device request ID. Owner issues a distinct activation credential. User pastes code > secure server validation > signed/authoritative entitlement > features unlock. Status must show pending/active/expired/revoked truthfully.
- Never embed a shared PIN, API service key, WhatsApp Business token, or `service_role` secret in APK. A hard-coded 4–6 digit local PIN can be extracted/reused and is unacceptable for paid entitlement.
- A user-supplied WhatsApp number and a dedicated KX entitlement backend are **not yet known**. Two Supabase accounts were inspected; no project explicitly named KX was present. Never modify an unrelated existing database.
- Backend design: issued codes stored salted/hashed server-side; per-license expiry, maximum redemptions, activated device count, audit history, revoke/renew endpoints and rate limit. Validate with authenticated Edge Function and strict DB policies, not client-only checks. Supabase current Edge Function JWT/auth modes must be rechecked before implementation.
- No auto-reading personal WhatsApp messages: a `wa.me/<owner-number>` link only initiates user-controlled contact. WhatsApp Business Cloud API automation requires separately authorized business integration, opt-in, webhooks and credentials.
- Core VPN safety is FREE by default: connecting, truthful tunnel status, permission disclosure and fundamental leak safeguards MUST NOT be withheld as premium.
- Suggested genuinely useful paid extras once proven end-to-end: multi-profile presets, encrypted config backup/restore, per-app split-tunnel presets, advanced local traffic-history export, automations for verified reconnect, private custom server bundles. Do not advertise any feature until implemented and tested.
- Google Play digital-content payment policy must be reviewed if distribution through Play is planned. External distribution rules differ.

## 7. Release gates required before owner calls it done
1. Reconcile installed APK signing/package/version/SHA with repository and the reference design shown in the video.
2. Integrate TASK_05 after reviewing PR #17 and its current head; preserve PR #15 and #13 unique changes.
3. Finish TASK_06 without destroying or overwriting the existing uncommitted agent changes.
4. Implement/test TASK_07–10 with locale, RTL, day/night, accessibility, startup, error and lifecycle checks.
5. Add one owned functioning WireGuard gateway or authorized lab tunnel; capture handshake, egress IP, DNS/IPv6 leak results, disconnect restore and offline fail-safe on a real device.
6. Introduce dedicated entitlement backend with owner WhatsApp contact and licensing tests before unlocking Pro.
7. Run full JVM tests, lint, reproducible debug/release builds, emulator API 26/35, physical-device smoke/rotation/notification tests, compare screenshots to references and inspect CI at exact SHA.
8. Only then perform signing, release-artifact SHA-256 inspection, distribution, and store status verification.

## 8. No fake closure
The connected Windows repo and GitHub show real engineering progress; current evidence does **NOT** establish a finished VPN release, a valid connected gateway, an active Pro billing/licensing system, or correspondence of the phone's APK to the inspected source.
