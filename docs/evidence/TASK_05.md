# TASK_05 evidence — Security Center, and the audit that corrected it

## Summary

TASK_05 shipped a Security Center that matched the artwork and did not match the build.
It asserted complete protection from a single boolean and presented six working switches,
three of which changed nothing. This document records both the shipped screen and the
correction, because the correction is the part that matters.

## The audit

Every claim on the screen was checked against the code that would have to back it.

| Claim as shipped | What the code actually did |
|---|---|
| "Fully Protected" | Conditioned on `isTunnelUp` alone — that the `tun` interface exists |
| "Your connection and data are secured" | Nothing verified traffic, leaks, or an exit address |
| Kill Switch — "Blocks internet if the VPN disconnects" | Written to DataStore; read by nothing. No `allowFamily`, no lockdown in `KXVpnService` |
| IPv6 Protection | No IPv6 routes are created anywhere in the build |
| Auto Reconnect | Written to DataStore; the tunnel state machine reconnects on network loss whether it is on or off |
| DNS Leak Protection | Replaces the `DNS` line in the user's own configuration with `1.1.1.1, 1.0.0.1` |
| Tracker Blocking | Replaces it with `94.140.14.14, 94.140.15.15`. Nothing is blocked by this app |
| Always-On VPN | Opens `Settings.ACTION_VPN_SETTINGS`, where the system grants it |

Two findings are more serious than the decorative switches:

1. **The DNS substitution was live on a first run.** Both DNS-reaching switches defaulted
   to `true`, so a user who imported a configuration and connected, without opening the
   security screen once, had their own `DNS` line replaced with a hardcoded third-party
   resolver. That is a real behaviour, undisclosed, changing the user's infrastructure.
2. **"Tracker Blocking" names a capability the app does not have.** It points DNS at a
   filtering resolver and calls itself a blocker. The distinction matters to anyone
   relying on it.

## The correction (`26cec8a`, cherry-picked to the TASK_05 branch as `f4bc7e7`)

- The hero card reads **"Tunnel interface is up" / "Traffic and leaks have not been
  verified"**. The old keys were deleted, not left unused, so nothing in either locale can
  assert protection.
- A new domain table, `SecurityEnforcement`, classifies every control as `ENFORCED`,
  `ENFORCED_WITH_DISCLOSURE` or `NOT_ENFORCED`. The screen asks the table instead of
  carrying its own answer, so a control cannot be rendered as working by accident.
- The three `NOT_ENFORCED` switches are **not operable** — disabled, badged
  "Not yet enforced", with the reason on the row. Same rule the Streaming/Gaming chips
  have followed since TASK_03.
- `dnsLeakProtection` and `trackerBlocking` now **default to off**, in the model and in the
  DataStore read path, and when on each row names the resolver it substitutes.
- `alwaysOnVpn` stays operable and says it opens Android's settings.

## Structural guarantees, as tests

`SecurityHonestyTest` fails the build if:

- a control is reported enforced when the table says it is not, or a new control is added
  without being classified;
- the string "Fully Protected", "connection and data are secured", or their Arabic
  equivalents reappears in **either** locale — the test reads the real resource files and
  asserts they exist first, so it cannot pass by missing them;
- default settings alter the user's `DNS` at all: `applySecuritySettings(config,
  SecuritySettings())` must return the input byte-for-byte;
- the modifier's resolvers drift from the constants the disclosure text quotes, which would
  make the screen describe a substitution that no longer happens.

## Gates

```
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
→ BUILD SUCCESSFUL, exit 0
unit tests=297 failures=0 errors=0
tools/pin_native_libraries.py      → PASS, 12 native libraries match the lock
tools/verify_apk_asset_hygiene.py  → PASS on all four checks
```

| Stage | SHA | Run | Result |
|---|---|---|---|
| Android CI | `33b0449` | 41694071426 | success |
| Device Runtime | `33b0449` | 41694071308 | success |
| Android CI | `f4bc7e7` | 41708860326 | success |
| Device Runtime | `f4bc7e7` | 41708860638 | **API 35 failed first, then success** |

The first API 35 failure was the emulator not booting at all: `adb` returned exit 1 from
the first second and the whole step lasted about eighty seconds, while API 26 in the same
run passed 23/23. The previous run on the same branch had both green. Re-running returned
success on both.

## Visual parity, measured not estimated

`tools/measure_reference.py` decodes the reference PNG with the standard library only —
there is no PIL and no ImageMagick on this machine — and reports geometry in dp at the
artwork's own 411dp width. Measured and applied:

- card edge sits at **16.6dp / 16.2dp** from the screen sides → `ScreenPadding` 24dp → **16dp**;
- route card spans 420.5→490.5dp → **70dp** tall;
- metric tile bands 500.1→562.1 and 578.7→640.3 → **62dp** tall, **~10dp** gutter;
- tiles are icon-left with the label above the figure, not a three-line stack.

The disclosure rows are deliberately taller than the artwork's single-line descriptions.
Matching the reference's density exactly would mean dropping the sentence that says what
the switch really does, and that trade goes against the user.

## What is NOT verified

- **No security claim in this screen is verified against live traffic.** There is no
  gateway, so no leak test, no exit address, no routed-traffic observation. The screen now
  says that instead of contradicting it.
- **The four unimplemented controls are still unimplemented.** The correction labels them
  honestly; it does not build them. Kill switch needs real Android lockdown policy, IPv6
  needs routes, auto-reconnect needs to read its setting.
- **No physical device.** `DEVICE_UNVERIFIED` stands.

## Status

`PR_OPEN` for `task/05-security-center` at `f4bc7e7` (PR #17), both workflows green on that
exact SHA. Not merged at the time of writing, and not `MERGED_VERIFIED` until it is, with
post-merge CI checked on the merge commit.
