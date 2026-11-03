# Live blockers log — initial facts

| Dependency | Initial state | Safe non-provider fallback | Claim ban |
| --- | --- | --- | --- |
| Global VPN fleet / free exits | NONE provisioned or verified | Own gateway or user-import WireGuard | No fake global locations/load |
| Provider API key/credentials | NOT SUPPLIED | Offline-first app / local config | No invented accounts |
| Google Play signing or publishing | NOT SUPPLIED | Debug builds, validated signed release later | No released claim |
| Real physical Android test | NO evidence | Emulator labeled DEVICE_UNVERIFIED | No hardware verification |
| OpenVPN, obfuscation, port hopping, tracker filter | NOT IMPLEMENTED | Disable until independently built/tested | No fake active toggles |
| Marketing “Premium”, “Unlimited”, “No logs” | Unsubstantiated | Hide | No misleading product guarantees |

For every new blocker append: task, service, failing command/error, exact scope affected, fallback implemented, what cannot be verified, safe next action. Continue working on independent subcomponents but **never falsely close or merge unfinished acceptance scope**.

## TASK_00 — 2026-11-03

### BLOCKER-001 · Android emulator / device runtime (G4)

- **Task:** TASK_00, and every later task that claims runtime evidence.
- **Service:** Android Emulator (or a physical device) for G4 and for instrumented test execution.
- **Observed state, measured not assumed:**
  - `D:/Android/sdk` has `platforms/android-36`, `build-tools 35.0.0/36.0.0`, `platform-tools` and `cmdline-tools/latest`, but **no `emulator/` directory, no `system-images/` directory and no AVDs**.
  - `Get-CimInstance Win32_ComputerSystem).HypervisorPresent` → **False**
  - `(Get-CimInstance Win32_Processor).VirtualizationFirmwareEnabled` → **True**
  - `Get-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform` → *"The requested operation requires elevation."*
- **Root cause:** BIOS VT-x is enabled but no Windows hypervisor is running, so WHPX is unavailable. The alternative (AEHD/HAXM) installs a kernel driver. Both paths require administrator elevation and a possible reboot.
- **Scope affected:** G4 runtime launch, logcat inspection, rotation/insets/lifecycle checks, `connectedDebugAndroidTest`, and all of G5 for later transport tasks.
- **Fallback actually implemented:** `BrandRenderTest` runs the real Compose tree under Robolectric 4.17 with `@GraphicsMode(NATIVE)` and writes 10 PNGs to `app/build/kx-screenshots/`. This validates composition, geometry, day/night, EN/AR, RTL/LTR, 360dp/411dp, 1.3x font scale and the degraded-storage state. `assembleDebugAndroidTest` compiles the instrumented suite so it cannot silently rot while unrunnable.
- **What this fallback does NOT prove:** real GPU behaviour, real display cutouts and insets, the platform splash animation on a real SystemUI, activity lifecycle under doze/process kill, touch latency, TalkBack on a real accessibility service, and any network behaviour.
- **Claim ban:** TASK_00 is `LOCALLY_VERIFIED` / `MERGED_VERIFIED` for its limited scope. It is **not** `DEVICE_UNVERIFIED`-free, not `RELEASE_VERIFIED`, and no runtime claim is made.
- **Safe next action:** needs an owner decision, because every option changes the machine rather than the repo —
  1. enable **Windows Hypervisor Platform** (admin, likely reboot), then `sdkmanager "emulator" "system-images;android-36;google_apis;x86_64"` and create an AVD;
  2. install the **AEHD** kernel driver (admin);
  3. attach a **physical Android device** over ADB;
  4. accept `DEVICE_UNVERIFIED` for every task and rely on CI.
  Option 4 is the current default. This agent will not enable a hypervisor or install a kernel driver without explicit owner approval.

### DEFERRED_EXTERNAL / environment notes from TASK_00

- **Newest AndroidX is unusable on this toolchain.** `androidx.core:1.19`, `compose-bom 2026.08.00+`, `navigation-compose 2.10.x` and `lifecycle 2.11.0` declare `minCompileSdk=37` and `minAndroidGradlePluginVersion=9.1.0`. The installed platform is android-36 and AGP 8.13.2 caps at compileSdk 36. Pinned to the newest compatible stack instead (documented with the measured values in `docs/evidence/TASK_00.md`). Upgrading to API 37 + AGP 9 requires `sdkmanager "platforms;android-37"` and an AGP major-version migration — not attempted, not needed for TASK_00.
- **Robolectric + Windows home directory containing a space** (`C:\Users\day night`). Robolectric builds its Maven repository path from `user.home`, URL-encodes the space and never decodes it, so it fails with `NoSuchFileException: C:\Users\day%20night\.m2\…` before any test body runs. Worked around by giving the test JVM a space-free `user.home` at `.robolectric-home/` (gitignored), applied only when the real home contains a space, so CI on Linux is unaffected. This is a local environment defect, not a project one.
- **`assets/reference/reference.rar`** appeared in the working tree during TASK_00. It was not created by this task, is not referenced by `docs/ASSET_MANIFEST.md`, and has been left untouched and uncommitted. Owner may want to confirm its origin.



### UPDATE to BLOCKER-001 — device runtime is now available on CI, not locally

Recorded during TASK_01 because it changes what later tasks may claim.

- **Still true:** this machine has no emulator (`D:/Android/sdk` has no `emulator/` or
  `system-images/`), no running hypervisor, and enabling one needs elevation. Local
  `connectedAndroidTest` remains impossible.
- **No longer true:** device runtime verification is unavailable. PR #7 added
  `.github/workflows/kx-device-runtime.yml`, which runs the instrumented suite on
  emulator **API 26 and API 35** (`pixel_2`, `google_apis`, x86_64, KVM) for every PR and
  every merge to main, and collects real framebuffer screenshots via
  `uiAutomation.takeScreenshot()`.
- **Consequence for G4:** tasks should report the CI emulator result as the runtime
  evidence and stop defaulting to `DEVICE_UNVERIFIED` without checking. `DEVICE_UNVERIFIED`
  now means specifically *no physical device*, which remains true.
- **Still blocked and unchanged:** a physical Android device, and any real VPN transport
  proof (no gateway is provisioned), so G5 network claims remain blocked until an owned
  endpoint exists.


### BLOCKER-002 — TASK_02 — no owned gateway for end-to-end tunnel proof

- **Task:** TASK_02 (and every later task that claims transport behaviour).
- **Service:** an authorised WireGuard gateway the app can actually connect to.
- **Observed state:** none provisioned. The repo has no gateway, no key material, no
  reachable peer. The only endpoints available are RFC 5737 documentation addresses
  (`203.0.113.0/24`), which are deliberately unroutable.
- **Root cause:** this machine has no Go and no NDK, so the tunnel library cannot be
  built here, and no WireGuard server is installed on the host. Standing up a real
  gateway needs either a remote host the owner controls, or a host-side WireGuard
  install plus a keypair.
- **Scope affected:** G5 handshake, route and DNS verification, IPv4/IPv6 leak checks,
  traffic proof through a controlled egress, and disconnect-restores-normal-routing.
- **Fallback actually implemented:** the complete local engine — the `VpnService`, the
  state machine with a structural no-fake-connected guarantee, the platform consent
  flow, the keystore-encrypted profile store and the dashboard. The instrumented suite
  proves the service declaration, the permissions, the consent API and a real
  AndroidKeyStore round-trip with tamper rejection.
- **What this fallback does NOT prove:** that packets traverse a tunnel, that a peer
  handshake completes, that DNS is tunnelled, that IPv6 does not leak, or that
  disconnecting restores normal routing.
- **Claim ban:** `VPN_END_TO_END_BLOCKED`. The tunnel is **not** claimed as verified,
  "protected" is never rendered, and no traffic, speed, latency or IP figure is
  displayed anywhere.
- **Safe next action:** the owner provisions an owned gateway (or authorises a host
  WireGuard install plus a generated keypair) and the G5 proof is run against it. The
  engine does not need changing to accept it — the backend already speaks WireGuard.

### DEFERRED / DISCLOSED — TASK_02 — prebuilt native tunnel binaries

- The owner approved `com.zaneschepke:wireguard-android:1.2.1`, which ships **12
  prebuilt `.so` files** with no corresponding source in this repository.
- **They have not been audited.** Every one is SHA-256 pinned in
  `docs/native-library-lock.md`, and `tools/pin_native_libraries.py` fails CI on any
  drift. The gate was verified by tampering with a hash and watching it reject the
  change.
- A stronger option remains open and is not blocked by anything in the repo: build the
  tunnel library from the upstream WireGuard Go source with a committed recipe. That
  needs a Go toolchain and the Android NDK on the build machine.

## TASK_03 — 2026-11-03

### BLOCKER-002 still applies — no owned gateway, so switching is proven against a fake

- TASK_03 needs two independently configured authorized endpoints to satisfy its own
  acceptance test 2. There is no gateway, so there is no second real endpoint and no
  live network to measure.
- **What was done instead, and what it does not prove:** the switch runs against a
  recording `TunnelPlatform` fake that answers nothing until the test tells it to. That
  proves the *ordering* (`stop` before the new configuration is applied), the rollback,
  and that `CONNECTED` is only reached when the platform reports the tunnel up. It does
  **not** prove that a real peer answers, that latency measurement produces sensible
  numbers on a real link, or that switching between two live servers behaves as modelled.
- **Claim ban:** multi-server availability, real ping values and any statement that
  switching "works over the network". `docs/evidence/TASK_03.md` repeats this.
- **Safe next action:** with an owned gateway, import two configurations pointing at it
  (or at two hosts), switch between them while connected, and capture the route, the
  assigned DNS and the actual traffic before and after.

### NOT-RUN — large system text on the server screen

- Robolectric's `@Config(qualifiers = …)` does not carry a font scale, so the
  "rotation and large text" half of acceptance test 3 is only half covered: landscape is
  rendered (`35-servers-populated-night-640-landscape-en.png`, which is how the hidden
  list was found), large text is not.
- The one fixed height on the screen was changed to `heightIn(min = 54.dp)` so the search
  field can grow, which removes the most likely clipping point. That is a mitigation, not
  a test result.
- **Safe next action:** render at 200% font scale on an emulator once G4 device runs are
  extended with a `wm` font-scale step, or on a physical device.

### NOT A GATE — third-party Vercel checks on every PR

- Every pull request in this repository also shows two failing checks, **"Vercel"** and
  **"Vercel Deployments"**, with `Account is blocked` at
  `https://vercel.com/knowledge/why-is-my-account-deployment-blocked`.
- They come from a Vercel GitHub App installed on the account, not from anything in this
  repository, and they were already failing before TASK_03. **No workflow in
  `.github/workflows/` depends on them** and no merge has been made on their account.
- Recorded so a future reader does not mistake them for a broken gate, and so the real
  gates — `Android CI` and `KX Android Device Runtime` — are the only ones cited as
  evidence.
- **Owner action only:** either resolve the Vercel account or remove the integration from
  the repository. Nothing an implementation task can do about it.

## TASK_04 / TASK_05 — 2026-11-04

### BLOCKER-003 — Robolectric fetches its `android-all` jar from Maven during the test run

- The first post-merge `Android CI` run on `eef3765` **failed**, leaving `main` red:

  ```
  ConnectionRingGeometryTest > classMethod FAILED
      java.lang.AssertionError at MavenArtifactFetcher.java:130
          Caused by: java.io.IOException at HttpURLConnection.java:2034
  ```

  `classMethod` means the class never executed. Three Robolectric classes failed the same
  way in one run, two of which had passed on every previous run, and the device workflow —
  the same suite on real emulators — was green in the same minute. Re-running the failed
  job returned success.
- **This is not a defect in the app and not a passed gate.** It is a reproducibility hole:
  a Maven outage can turn `main` red at any time, and a re-run makes it green again without
  anything changing, which is exactly how a real regression would hide.
- **Safe fix, not yet done:** pin Robolectric's artifacts into the repository or the Gradle
  cache (`robolectric.offline=true` with `robolectric.dependency.dir`, or resolve
  `android-all` through the dependency configuration so the build cache holds it), then
  make CI fail if the fetch is attempted.
- **Claim ban:** CI results on Robolectric suites are recorded with their run IDs and with
  any re-run stated, never as "it was green".

### PROCESS GAP — evidence arrived after the merge, and the ledger disagreed with the tree

- TASK_04 merged as `eef3765` with **no** `docs/evidence/TASK_04.md`, and TASK_05 was
  opened with none either, while `docs/PROGRESS_LEDGER.md` still said `NOT_STARTED` for
  both. A reader of the ledger would have concluded the app had no connected screen.
- Both files were written later, from recorded CI runs and downloaded artifacts, and the
  ledger rows now match the tree. The rule in `AGENTS.md` is that evidence ships in the
  same task; recording the breach here is the minimum correction, not a substitute.
- **Prevention:** the ledger row and the evidence file are now staged in the same commit as
  the code they describe, and a task is not opened as a PR until both exist.

### RESOLVED — a duplicate render was presented as evidence

- `53-security-night-411-en-switches-off.png` was **byte-identical** to the switches-on
  frame it was meant to contrast with: the only control whose stored value is visible is
  Always-On VPN, and at 891dp it sits below the fold, so the scroll view drew the same five
  rows twice.
- The frame was retaken at a taller viewport, the stale file was deleted, and the guarantee
  is now asserted on the semantics tree (`notEnforcedControlsNeverLookEnforced`) instead of
  inferred from a picture. That test also caught its own first mistake: matching a node by
  its visible title reads the inner `Text`, which carries no content description, so the
  assertion passed against an empty string before it was rewritten to match the announced
  description itself.
