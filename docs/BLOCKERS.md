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

