# TASK_02 evidence — Home dashboard and the real VPN session manager

## Summary

TASK_02 replaces the home placeholder with a working dashboard and puts a real
Android `VpnService` behind a real tunnel state machine, with profiles encrypted by
the platform keystore.

**What is verified:** the engine, the state machine, the consent plumbing, the service
declaration, the keystore, the encrypted storage and the UI.

**What is not verified:** an end-to-end tunnel. There is no owned gateway in this
environment, so no handshake, no routed traffic, no DNS behaviour and no leak test was
performed. That is recorded as `VPN_END_TO_END_BLOCKED` and it is the reason this task
is not claimed as a fully verified tunnel.

## The tunnel backend decision

The owner was asked to choose, because the options have materially different trust
properties, and chose **a repackaged WireGuard Android backend with pinned hashes**.

- Dependency: `com.zaneschepke:wireguard-android:1.2.1`.
- It ships **12 prebuilt native libraries** (`libwg-go.so` and friends, ~2.9 MB per ABI).
  No source for them is in this repository and **none has been audited here**.
- The compensating control is `tools/pin_native_libraries.py`, which hashes every
  native library out of the resolved AAR and compares it against
  `docs/native-library-lock.md`. CI fails on any drift.
- The gate was proven to fail: one hex digit was tampered with in the lock file and
  the tool correctly reported `HASH DRIFT` with a non-zero exit.

Two defects were found and fixed in that tool itself:

1. It parsed `minCompileSdk` with `startswith()`, which also matched
   `minCompileSdkExtension` and silently overwrote the value — the first version
   reported every artifact as compatible. Corrected to exact key matching.
2. It searched only `~/.gradle` and found nothing, because this machine sets
   `GRADLE_USER_HOME` elsewhere. A gate that fails for the wrong reason is one nobody
   trusts, so it now honours the environment variable and prints every root it
   searched.

## G3 — Automated verification

```
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
→ BUILD SUCCESSFUL, exit 0
unit tests=158 failures=0 errors=0
```

| Suite | Tests | Covers |
|---|---|---|
| `TunnelStateMachineTest` | 25 | The whole state × event table |
| `TunnelSessionManagerTest` | 17 | Idempotency, races, consent, revocation |
| `ProfileVaultTest` | 17 | Encryption envelope, tamper rejection, no plaintext |
| `WireGuardConfigValidatorTest` | 23 | Parser boundary (from TASK_01) |
| `BoundedFileReaderTest` | 9 | Size ceiling, endless stream (from TASK_01) |
| `HomeRenderTest` | 6 | Home screen renders in each state |
| plus TASK_00/TASK_01 suites | 61 | — |

### The structural no-fake-connected guarantee

`TunnelStateMachineTest` enumerates every (state, event) pair and asserts that the
only transitions **into** `CONNECTED` come from `CONNECTING` or `RECONNECTING` and are
driven by `TunnelUp` — which the controller emits only when the WireGuard backend
reports `Tunnel.State.UP`. There is no clock anywhere in the state machine, so
acceptance criterion 1 ("state displayed cannot be falsified by timers") holds by
construction rather than by discipline.

The test caught its own imprecision first: it flagged identity transitions out of an
already-`CONNECTED` state, which is correct behaviour, so the property was restated as
"only arrivals count" rather than weakened.

### The two real defects the tests caught

1. **`StoredProfile` was a data class, so its generated `toString()` printed the private
   key.** Any log line or crash report that included it would have leaked the key. The
   `toString()` is now overridden with a redacted form. This was found by
   `resultTypesDoNotPrintTheSecret`, which asserts the secret is absent from the
   rendered result.
2. **`KXVpnService` declared a property named `onStateChange` that collided with the
   `Tunnel.onStateChange(Tunnel.State)` method it had to implement.** The compiler
   caught it; the property is now `onBackendStateChanged` and the interface method
   adapts the library's enum.

### Deliberate naming that keeps a promise honest

`TunnelSessionState.isTunnelUp` was originally `isProtected`. It was renamed because
the backend reporting UP means *the tun interface exists and a handshake is being
attempted* — it does not mean traffic flows, that a peer completed a handshake, or
that anyone is protected. `docs/SECURITY_MODEL.md` requires "VPN UP" and "Internet
Verified" to stay distinct, and a property called `isProtected` would have invited a
future task to draw a shield on the strength of it.

## G2 — Function

- **Real `VpnService`** (`KXVpnService`) extending the library's `GoBackend.VpnService`,
  declared with `android:permission="android.permission.BIND_VPN_SERVICE"` and
  `exported=false`, so no other app can bind or start it.
- **Real consent flow**: `VpnService.prepare()` is called only from a connect action,
  never at onboarding. `POST_NOTIFICATIONS` is requested at the same moment and the
  connection proceeds even if it is denied.
- **`onRevoke` is handled**: Android can withdraw the permission at any time, and the
  session is torn down when it does.
- **Foreground notification** with its own channel, posted before the backend work
  starts so a throwing backend cannot leave a started service without one.
- **Profiles are encrypted at rest** with a non-exportable AES-256-GCM AndroidKeyStore
  key, with **no plaintext fallback**: if the key is unavailable the save fails and
  nothing is written. A test asserts that the stored blob contains neither
  `PrivateKey` nor the endpoint line.
- **Home shows only measured values.** The reference artwork's "Best Location" card
  and Ping/Download/Upload/IP Masking tiles are rendered with `--` and "not measured",
  and the location card says plainly that no location has been chosen. The upload and
  download tiles show the backend's real byte counters when a tunnel is up, and nothing
  otherwise.

### Permission set, and why each one

| Permission | Reason |
|---|---|
| `INTERNET` | The tunnel carries traffic. |
| `ACCESS_NETWORK_STATE` | To observe Wi-Fi/mobile changes so a lost link becomes `RECONNECTING` rather than a stale `CONNECTED`. |
| `FOREGROUND_SERVICE` | The tunnel must survive the app going to the background. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required alongside `foregroundServiceType="specialUse"`. |
| `POST_NOTIFICATIONS` | The ongoing tunnel notification; requested at connect time. |

`BIND_VPN_SERVICE` is a *service* permission, not a user-granted one, and is asserted
by an instrumented test.

## G5 — Security review performed

- No private key, preshared key or config text is logged, printed or placed in a
  semantics tree. Two unit tests and one render test assert this.
- The profile store is in app-private storage **and** encrypted; private storage stops
  other apps, encryption stops offline extraction of the device.
- Erasing removes the blobs before destroying the key, so a user who asks to be
  forgotten does not leave unreadable ciphertext behind. The ordering is asserted.
- A tampered, truncated or wrong-key blob is rejected as corrupt rather than
  decrypted into garbage.
- The native binaries are hash-pinned and CI-enforced, and the fact that they were not
  audited is recorded here rather than glossed.

## G1 — Visual comparison against `02-home-disconnected.png`

Renders in `docs/evidence/TASK_02/`: disconnected without a profile, disconnected with
a profile (411dp), Arabic RTL, connected with real counters, failed with a real
reason, and the bottom tab bar.

The first pass was visibly wrong against the reference and was fixed before merging:
the connect control was a flat gradient disc with no glass structure, and the metric
tiles overlapped their own text. The orb is now layered — bright outer body, a darker
inner lens and a luminous sweep-gradient rim — and the tiles place the value and its
unit on separate lines.

### Recorded deviations

1. **The tab bar shows only Home as available.** Servers, Security, Stats and Settings
   are rendered in their inactive style and are **disabled**, with the reason in their
   accessibility label. A tab that looks tappable but does nothing is the
   "invisible or unreachable control" the visual QA list forbids.
2. **The bottom bar is not part of the home scroll**, matching the reference where it
   is pinned.
3. **The metric tiles are wider than in the reference** so the unit text does not wrap
   into the tile below at 360dp.

## What is NOT done, and is not claimed

- **`VPN_END_TO_END_BLOCKED`.** There is no owned WireGuard gateway in this
  environment, so **no handshake, no routed traffic, no DNS behaviour, no IPv4/IPv6
  leak test and no teardown-to-normal-routing proof was performed**. The task
  specification is explicit that in this situation the task "remains blocked
  truthfully"; it is not counted as a verified tunnel.
- **The instrumented suite proves the platform plumbing** — service declaration,
  permissions, consent API and a real AndroidKeyStore round-trip with tamper
  rejection — but it does not prove that packets traverse a tunnel.
- **No physical device.** `DEVICE_UNVERIFIED` stands for that specifically; the
  emulator runtime evidence comes from the CI device workflow.
- **`foregroundServiceType="specialUse"` with subtype `vpn_tunnel`** is the declaration
  lint requires. A store submission would have to justify that subtype, and that
  justification is a release gate, not something this task settles.
- **No kill-switch or lockdown.** `docs/SECURITY_MODEL.md` requires a kill switch to
  be tied to actual Android lockdown policy; none is implemented and no such claim is
  made.

## Status

`LOCALLY_VERIFIED` for the engine, state machine, storage, service plumbing and UI, and
**`VPN_END_TO_END_BLOCKED`** for the transport. Not `RELEASE_VERIFIED`.
