# TASK_01 evidence — Welcome, first-run onboarding and configuration entry

## Summary

TASK_01 replaces the TASK_00 first-run placeholder with three real screens and the
domain logic behind them: a welcome route with two working controls, a disclosure step
whose Continue is locked until the user has acknowledged it, and a configuration entry
that genuinely reads, bounds and parses a WireGuard file and reports what it found.

Onboarding acceptance is now actually persisted — TASK_00 shipped the write path with no
caller on purpose; this task is that caller.

## Gate-by-gate result

| Gate | Result | Basis |
|---|---|---|
| G0 Reality | PASS | Baseline recorded below; branch created from verified `origin/main`. |
| G1 Visual | PASS WITH RECORDED DEVIATIONS | 9 new Compose renders (`11`–`19`) covering Day/Night, EN/AR, RTL/LTR, 360dp/411dp. |
| G2 Function | PASS | Real navigation, real DataStore write, real SAF picker, real parser. No placeholder left on the first-run path. |
| G3 Automated | PASS | 93/93 JVM tests, lint clean with `abortOnError`, `assembleDebug`, `assembleDebugAndroidTest`. |
| G4 Runtime | **PASS on CI emulators** | Instrumented suite executed on emulator API 26 and API 35: `10/10 completed, 0 failed, 0 skipped` on both. Real framebuffer screenshots in `docs/evidence/TASK_01/device/`. Physical device still not done. |
| G5 VPN/security | PARTIAL | Parser boundary and secret-redaction are tested. No transport exists yet, so no handshake/DNS/leak proof is possible. |
| G6 Integration | see PR section | |
| G7 Handoff | PASS | This file, renders, ledger and blockers updated. |

## G0 — Reality

```
baseline origin/main:  8bd13929cba45cd923468fb87f5098e5f979253b  (verified by git fetch)
branch:                task/01-welcome-onboarding
reference asset:       assets/reference/01-welcome.png
sha256:                464a7c4f7ff3bdb0c703578cb886cf942cdd09e6d09fb6dad149bdd9996458c6
                       matches docs/ASSET_MANIFEST.md
```

The image was opened and inspected before implementation.

### Important context discovered mid-task

`main` had advanced beyond my own merge: PR #7 added
`.github/workflows/kx-device-runtime.yml`, which runs the instrumented suite on a real
Android emulator (API 26 and API 35, `pixel_2`, `google_apis`, x86_64, KVM) for every PR
and every merge to main, and collects framebuffer screenshots.

That changes the standing of G4 for this task. BLOCKER-001 (no local emulator) is still
true **on this machine**, but runtime verification is no longer impossible — it happens on
CI. The status below reflects that distinction rather than defaulting to
`DEVICE_UNVERIFIED` out of habit.

## What was built

### Screens

- **`WelcomeScreen`** — the native implementation of `01-welcome.png`: atmospheric wave
  backdrop drawn with parametric Béziers (`KXAtmosphericBackground`), the hero orb, the
  "KX / Private VPN" lockup with drawn cyan accent rules, and two full-width glass pills
  (`KXPillButton`, capsule shape, height coerced to ≥48dp so the visible pill and the
  hit area are the same thing).
- **`OnboardingScreen`** — three disclosure cards (what a VPN does, privacy limits, cost
  boundaries), an unticked acknowledgement checkbox, and a Continue control that is
  disabled until it is ticked.
- **`ImportEntryScreen`** — SAF `OpenDocument` picker, bounded read, strict parse, and a
  factual summary of what was found. The Apply control is present but disabled with the
  reason stated.

### Domain

- **`WireGuardConfigValidator`** — the parser boundary. 64 KiB and 2048 line ceilings
  applied before parsing, strict UTF-8 on the byte entry point, control-character
  rejection, exact 32-byte base64 key validation, CIDR/port/MTU/keepalive range checks,
  duplicate-peer detection. Errors carry a field name plus an enum message id and never a
  value.
- **`BoundedFileReader`** — enforces the ceiling while streaming, not after, and reads one
  byte past the limit to distinguish "exactly at cap" from "over cap" without buffering.
- **`OnboardingViewModel`** — the single write path for acceptance.
- **`ImportEntryViewModel`** — Idle / Reading / Inspected / Rejected / Unreadable, keeping
  "could not read" distinct from "read and invalid".

### Routing

`Launch → Welcome → Onboarding → Home` and `Welcome → Import`. Onboarding pops the Welcome
node inclusively on completion so back from Home does not walk the user back through their
own setup flow.

## G3 — Automated verification

```
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
→ BUILD SUCCESSFUL, exit 0

unit tests=93 failures=0 errors=0
```

Test inventory added by this task:

| Suite | Tests | Covers |
|---|---|---|
| `WireGuardConfigValidatorTest` | 23 | Parser boundary + redaction |
| `BoundedFileReaderTest` | 9 | Size ceiling, endless stream, IO/permission failure, stream closure |
| `OnboardingViewModelTest` | 6 | Lock/unlock, single write, failed write, retry |
| `WelcomeRenderTest` | 9 | Renders of all three screens + key-leak guard |

Two deliberate negative tests are worth naming because they are the ones that would fail
if the honesty design ever regressed:

- `privateKeyNeverReachesTheRenderedTree` walks the **semantics tree** and the rejection
  state and asserts the private key string is absent from both.
- `failureOutputNeverEchoesThePrivateKey` asserts the same for the error path, where a
  tempting `"$field: $value"` message would leak it.

## Bugs found while building this, in my own code

1. **Canonical WireGuard keys are CamelCase without separators** — `AllowedIPs`,
   `ListenPort`, `PersistentKeepalive`. I compared lowercased raw keys against hyphenated
   literals, so three fields matched nothing and were silently ignored. A full-tunnel
   config was reported as not routing all traffic. Keys are now normalised, which also
   accepts the hyphenated variant some tooling emits.
2. **The "current peer block has content" flag was set for every assignment line**,
   including `[Interface]` lines. The first `[Peer]` header therefore looked back at a
   phantom peer block and reported missing `AllowedIPs` — every valid config was rejected.
   The flag is now scoped to the Peer section.
3. **The UTF-8 check was unreachable dead code** when the entry point took a `String`,
   because a String cannot contain malformed UTF-8. The boundary was moved to a
   `ByteArray` entry point, which is also what the file picker actually produces, and is
   now genuinely tested.
4. **`stringResource` inside `joinToString`** — a plain lambda, so the compiler rejected
   it. Fixed by rendering one row per finding instead of building a joined string; the
   workaround would have been to drop localisation.
5. **ViewModels constructed inside `@Composable` lambdas in tests** — lint's
   `ViewModelConstructorInComposable` caught this. Hoisted out of composition.
6. **A test I wrote would have deadlocked**: collecting a never-emitting flow with
   `toList()` blocks the test dispatcher forever. Removed rather than left as a latent
   hang.

## G4 — Actual emulator execution

From the `KX Android Device Runtime` workflow, run `41613401712`, on the PR head SHA:

```
emulator-5554 - 15 Tests   10/10 completed. (0 skipped) (0 failed)   # API 35, google_apis x86_64, pixel_2
emulator-5554 - 8.0.0 Tests 10/10 completed. (0 skipped) (0 failed)   # API 26
```

Both jobs green. The screenshots in `docs/evidence/TASK_01/device/` are framebuffer
captures taken by the instrumentation itself, not JVM renders.

### The device screenshot caught a visual defect my renders also had

The first API 35 capture showed a **grey smudge in the upper-left corner in Day mode**.
The corner vignette in `KXAtmosphericBackground` was hard-coded to 40% black, which reads
as depth on the near-black night surface but mixes into mud over the pale lilac day
surface.

Two things worth recording honestly:

1. The defect was **also present in my own JVM render** `12-welcome-day-411-en.png`. The
   rendering pipeline caught it; I did not look at that frame closely enough before
   shipping it in the PR. The device screenshot is what made it obvious.
2. The fix is a theme-aware `atmosphereVignette` token on `KXGlassScheme`: night keeps a
   deep navy, day deepens toward violet instead of black. Re-rendered and re-verified;
   night is unchanged.

This is the concrete argument for running on a device even when JVM renders exist: the
render proves the composition, the device proves what a user actually sees.

## G1 — Visual comparison against `01-welcome.png`

New renders in `docs/evidence/TASK_01/`:

| File | Configuration |
|---|---|
| `11-welcome-night-360-en.png` | Welcome, night, 360dp EN |
| `12-welcome-day-411-en.png` | Welcome, day, 411dp EN |
| `13-welcome-night-360-ar.png` | Welcome, night, 360dp Arabic RTL |
| `14-onboarding-night-360-ar.png` | Disclosure, Arabic — the text-expansion case |
| `15-onboarding-night-360-en.png` | Disclosure, Continue locked |
| `16-import-idle-night-360-en.png` | Import, idle |
| `17-import-rejected-night-360-en.png` | Import, hostile file with findings |
| `18-import-inspected-night-360-en.png` | Import, valid config summary |
| `19-import-inspected-day-411-ar.png` | Import summary, Arabic day 411dp |

Faithful to the reference: deep-indigo atmosphere with luminous flowing wave bands, the
large glass orb with its orbit ring and jewel nodes, the KX lockup with cyan accent rules
either side, a gradient primary pill and a glass secondary pill.

### Deliberate deviations, with reasons

1. **"Private. Fast. Borderless." is not reproduced.** `docs/tasks/TASK_01` explicitly
   bans unsupported private/borderless absolutes, and this build has no tunnel engine at
   all. The tagline is replaced with "Bring your own configuration. No account and no
   bundled server fleet." — a statement about the product model, not a performance claim.
   `noProtectionOrMarketingClaimIsEverRendered` asserts on a device that "Borderless",
   "Premium", "Unlimited", "No logs", "Protected" and "Connected" never appear.
2. **The wave backdrop is drawn, not shipped as a bitmap.** A flattened background would
   be the "screenshot as UI" the contract forbids, and would not scale between 360dp and
   tablets. The trade is that the reference's volumetric light bloom is approximated by
   gradient bands rather than matched pixel-for-pixel.
3. **The status bar contents (12:30, 98% battery, carrier icons) are not reproduced** —
   those are the real device chrome in the artwork, owned by the system.
4. **Hero orb gives up size below 360dp** before the controls do, so both pills stay
   reachable on narrow screens instead of scrolling off.

## G5 — Security and privacy actually tested

- **Nothing is persisted by the import path.** The state is in-memory only; the file
  states this on screen ("Nothing is saved and nothing is uploaded").
- **The private key is never displayed, logged or placed in the semantics tree** — two
  tests assert this directly.
- **No network permission exists**, so "not uploaded" is enforced by the manifest, not
  just promised by the copy.
- **No permission is requested during onboarding.**
  `noUnexpectedPermissionDialogOnFirstRun` asserts it on a device. The contract says real
  VPN consent belongs to the moment the user initiates a connection, which is TASK_02.
- **Hostile input is bounded**: size, line count, encoding, control characters and key
  shape are all rejected before any allocation grows.

## Functional honesty

- The disclosure text says a VPN does not make you anonymous, that the gateway server can
  see destination addresses and traffic volume, and that no no-logs claim is made because
  there is no operator to make one.
- Cost boundaries are stated: no bundled fleet, no free unlimited exit, no subscription,
  and continuing purchases or enrols nothing.
- Import Apply is disabled with the reason "There is no tunnel engine in this build yet".

## Status

`LOCALLY_VERIFIED` for the provider-independent TASK_01 scope, plus instrumented execution
on CI emulators where available. Not `RELEASE_VERIFIED`; no transport claim; no physical
device claim.
