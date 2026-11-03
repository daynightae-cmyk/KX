# TASK_03 evidence — Server selection that actually selects

## Summary

TASK_03 turns `assets/reference/03-server-selection.png` into a working screen over the
endpoints **the user owns**, and makes tapping one of them change the configuration the
tunnel uses — including a live switch with rollback.

**What is verified:** the catalogue rules, the measurement path, the switch state machine
and its execution against a real `VpnService` session manager, persistence of the choice
across restart, the UI in EN/AR, LTR/RTL, day/night, portrait and landscape, the offline
refusal, and — on emulator API 26 and API 35 — the multi-write catalogue store and the
screen on a real renderer.

**What is not verified:** that any real endpoint answered. There is still no owned
WireGuard gateway in this environment, so no handshake, no routed traffic, no DNS and no
leak proof. `VPN_END_TO_END_BLOCKED` from TASK_02 stands, and multi-server claims remain
unverified for the same reason.

## The one decision that shaped the task

The reference shows seven countries with latencies and "Load 32%" bars. There is no
server fleet, no provider API and no legitimate source for either. So:

- **No "Load" column exists in this build at all.** Nothing in the app can measure a
  server's capacity. A bar would be a drawn number, and a drawn number is fabrication.
- **The list is the user's own configurations.** An empty catalogue renders an invitation
  to import, not a plausible list of popular countries.
- **A latency is shown only if a real TCP connect was timed and is still fresh.**
  Otherwise `--`, with no bolt glyph beside it.
- **Streaming and Gaming chips are present and disabled**, because no configuration
  declares those capabilities. Their accessibility labels state the reason.

## G3 — Automated verification

```
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
→ BUILD SUCCESSFUL, exit 0
unit tests=242 failures=0 errors=0
```

| Suite | Tests | Covers |
|---|---|---|
| `ServerCatalogueTest` | 32 | Rows, recommendation, filter, sort, search, host validation, endpoint reader, profile identity |
| `EndpointCatalogueRepositoryTest` | 15 | Metadata round-trip, favourites, the choice, anti-forgery encoding |
| `ProfileRepositoryTest` | 11 | Multi-profile, activation refusal, cache eviction on removal |
| `EndpointSwitchControllerTest` | 7 | Switch ordering, rollback, busy refusal, unknown endpoint |
| `ServerSwitchStateMachineTest` | 7 | The reducer's transition table |
| `ServersViewModelTest` | 5 | Offline refusal, freshness consistency, filter/row agreement |
| `ServerRenderTest` | 7 | Compose renders (see G1) |
| previous TASK_00–02 suites | 165 | — |

### What the tests caught, in order

1. **A failed switch had nothing to roll back to.** `reduceServerSwitch` carries
   `previousEndpointId` through the switch, but the controller only ever set it on a
   successful `Connected`. On the *first* switch after a cold start it was null, so a
   rollback silently restored nothing. Fixed by seeding it from the catalogue before
   `SwitchRequested`. Found by `aFailedLiveSwitchRollsTheChoiceBackAndDoesNotReconnect`.
2. **The "Fastest" filter used a different clock than the rows.** It called
   `displayLatency(measuredAtEpochMillis)` — the measurement's own timestamp — so a
   measurement the rows already refused to display still won the filter. The result was a
   row selected as "fastest" showing `--`. `filter()` now takes the same `now` as
   `buildRows`, and `theFastestFilterExcludesStaleMeasurements` asserts it.
3. **The screen read the wall clock inside the composable.** That is how the first
   committed render came to show "Fastest measured" and `--` and "stale" on one row: the
   fixture stamped a measurement in 2023 and the composable compared it to 2026. Beyond
   the test, it meant freshness could change between building a row and drawing it, and
   that a row going stale while open would not update. Freshness now travels with the
   rows in `ServerListState.generatedAtMillis`, so the badge, the number and the stale
   marker are always derived from one instant.
4. **Landscape hid the servers completely.** The first landscape render filled the
   viewport with the 168dp brand orb and the display title and pushed the list off
   screen. `BoxWithConstraints` now shrinks the header below 560dp of height and the list
   takes the remaining space with `weight(1f)` and scrolls.
5. **Measured latencies were unreadable in day mode.** The cyan used at night sits at
   roughly 2:1 on the pale lilac surface. A `latency` token was added to the glass scheme
   with a deep-blue day value; `KXPalette.LatencyDay` is explicitly documented as a
   derived accessibility token, not a sampled one, because the reference is night-only
   and claiming otherwise would be false provenance.

### The switch is real, not a state variable

`EndpointSwitchController` executes the effects the reducer emits, against the same
`TunnelSessionManager` the home screen uses. `aLiveSwitchTearsTheOldTunnelDownBeforeApplyingTheNewOne`
asserts the platform call sequence literally:

```
[stop, start(<the new configuration text>)]
```

and that the state is only `CONNECTED` after the fake platform reports the tunnel up. The
rollback test asserts the opposite: after a failed connect the previous endpoint is
selected again and **no further `start` is issued** — a rollback restores the choice, it
does not silently reconnect to a server the user just asked to leave.

`FakePlatform` records calls and answers nothing until the test tells it to. That is what
makes these ordering claims worth making: nothing is up unless the test said so.

## G2 — Function

- **Selection controls the transport.** Tapping a row activates that profile in the vault
  and records the choice in the catalogue; the next `connect()` uses it. Verified by
  `EndpointSwitchControllerTest` and by `choosingAServerWhileDisconnectedChangesTheChoiceAndStartsNothing`,
  which also asserts that choosing while disconnected starts **nothing**.
- **It survives restart.** `KXApplication.restoreActiveEndpoint()` re-joins the stored
  choice to the stored profile on cold start, asynchronously so a disk read never blocks
  startup. A choice that no longer resolves falls back to a profile that does, and if
  none does the app stays unconfigured rather than guessing.
- **Multiple profiles are real now.** Each import is stored under an id derived from
  `SHA-256(host:port|peerPublicKey)`, so re-importing the same file updates one entry
  instead of duplicating, and two accounts on one host stay distinct. Importing no longer
  overwrites the server in use: `save` only takes over when nothing was active, so adding
  a second server cannot move a running tunnel underneath the user.
- **Labels come from the user's file name**, read from the document provider, with the
  host as the fallback. No country is ever inferred from an IP.
- **Measurement is bounded**: one batch, at most 32 endpoints, at most 4 concurrent
  sockets, a 3s connect timeout, and the host is validated before it reaches a socket.
- **Offline is refused before any socket opens**, and the screen says why instead of
  spinning. `AndroidNetworkAvailability` uses `NET_CAPABILITY_VALIDATED`, which is the
  difference between "an interface exists" and "a probe could plausibly answer".
- **Hosts are validated, not sanitised.** `EndpointValidator.isValidHost()` refuses
  spaces, URLs, newlines and injection-shaped strings outright; a rejected endpoint is
  never stored.

## G5 — Security review

- The catalogue stores **metadata only** — label, optional country code, host, port, and
  the id of the encrypted profile. The configuration text stays in the keystore vault and
  this repository never sees it.
- Endpoint records are URL-encoded and joined with `\u001F`/`\u001E`, so a label
  containing a separator cannot forge an extra endpoint. Asserted by
  `aLabelContainingSeparatorsCannotForgeAnotherEndpoint`.
- Reading back a choice that names a deleted endpoint drops it instead of resurrecting it,
  so a stale preference cannot point the tunnel at nothing.
- `ProfileRepository.remove()` evicts the decrypted text it cached for that profile.
  Forgetting a configuration must not leave its private key in memory.
- Selecting an endpoint whose profile is not on the device fails with `UnknownEndpoint`
  and changes nothing. There is no path in this task that reports a server as chosen when
  it could not be connected to.
- No key material, endpoint secret or configuration text is logged or placed in a
  semantics tree; the APK hygiene gate re-checks the built artifact.

## G1 — Visual comparison against `03-server-selection.png`

Reference SHA-256 `8D2C742A…C587`, verified against `docs/ASSET_MANIFEST.md` and opened
directly rather than from memory.

Renders in `docs/evidence/TASK_03/`: populated night 411dp, empty day 360dp, Arabic RTL
night, measuring with the list retained, stale measurements with a failed switch,
landscape, and offline day mode.

The structure matches — atmospheric backdrop, brand orb, title, glass search pill, the
four filter chips, glass rows with flag, name, latency and star, and a highlighted first
row. Deliberate deviations:

1. **No Load column.** Nothing measures capacity; the space is used for the host instead.
2. **Streaming and Gaming are dimmed and untappable**, with the reason in the label.
3. **The search pill has no microphone.** Voice search is not implemented, and an icon
   that does nothing is the unreachable control the visual QA list forbids.
4. **"Recommended" is a text caption, not a pill**, and the equivalent here is "Chosen" —
   the reference's badge means "best", ours means "the one you picked", which is a claim
   this app can actually support.
5. **Flags appear only when the user supplied a country code.** They are generated from
   the platform's regional-indicator glyphs, so no third-party flag artwork is bundled.
   An endpoint with no country shows a neutral dot.

## What is NOT done, and is not claimed

- **`VPN_END_TO_END_BLOCKED`.** No owned gateway, so no handshake, routed-traffic, DNS or
  leak proof. Acceptance criterion 4 of the task spec cannot be met in this environment
  and is recorded as blocked, not passed.
- **Multi-server switching over a real network is unverified.** The ordering and rollback
  are proven against a recording fake platform, not against two live endpoints. The task
  spec anticipates exactly this: "one endpoint is sufficient for baseline but multi-server
  claims remain unverified".
- **No physical device.** `DEVICE_UNVERIFIED` stands; emulator runtime comes from CI.
- **Large system text was not rendered.** Robolectric's qualifier string does not carry a
  font scale, so this is untested rather than assumed fine. The fixed-height search field
  was changed to `heightIn(min = 54.dp)` so it can grow, which removes the most likely
  clipping point but is not a substitute for the test.
- **No last-measured timestamp is shown.** The spec asks for one; the row shows a stale
  marker instead. A wall-clock string would be more informative and belongs with TASK_08's
  statistics work, where the formatting rules live.
- **The settings gear is disabled**, as it has been since TASK_02. Settings is TASK_10.

## G4 — Device runtime

`ServerCatalogueInstrumentedTest` runs on the `KX Android Device Runtime` workflow, on
emulator API 26 (Android 8.0.0) and API 35 (Android 15), `pixel_2`, `google_apis`, x86_64:

```
API 26: tests="23" failures="0" errors="0" skipped="0"   test-exit-code.txt: 0
API 35: tests="23" failures="0" errors="0" skipped="0"   test-exit-code.txt: 0
crash-logcat.txt: 0 bytes on both — no crash during the run
```

Seven of those are new in this task, and two of them are the reason the file exists:

- **Several writes to the same preferences file on a real Android file system.** The JVM
  behavioural tests use an in-memory store because androidx DataStore's `ATOMIC_MOVE`
  without `REPLACE_EXISTING` cannot overwrite an existing file on Windows. On device that
  limitation does not apply, and the multi-write path is the shipped path — so
  `severalWritesRoundTripThroughTheRealDataStore` and
  `theChosenEndpointSurvivesARepositoryReopen` prove it there rather than assuming it.
- **The screen composes on a real renderer**: tapping a row reports the endpoint it was
  given, an unmeasured row renders no latency substring at all (matched as a substring, so
  it would actually find an invented number), and a disabled capability chip is displayed
  and announced as not enabled.

### The device run rejected my own test first

The first attempt asserted that a disabled chip exposes **no `OnClick` semantics
action**. It failed on both emulators — and it was the test that was wrong, not the app:
`Modifier.clickable(enabled = false)` still publishes `OnClick`, because that is how
Compose lets TalkBack announce a control as present but unavailable. The assertion now
checks the enabled flag, which is what the user actually experiences. Recorded here
because "the gate failed, then the test was corrected" is different from "the test passed
first time".

The `post-test-launch.png` in the artifact is the app's **launch** screen captured after
the run, with a provenance file saying so. It is not a screenshot of the server screen and
is not presented as one.

## G6 — Integration

| Field | Value |
|---|---|
| BASE_SHA | `3654a0ff788ead2735b0d22ab997f36d25d0a1ad` |
| IMPLEMENTATION_SHA | `ae64e2206b9bc218036317a0f11b128459ba0506` (PR head; `1d2ab73` is the implementation commit) |
| PR | https://github.com/daynightae-cmyk/KX/pull/14 |
| MERGE_SHA | pending |
| ORIGIN_MAIN_SHA | pending |

CI on the exact implementation SHA:

```
Repository hygiene            pass  (run 41655231971)
Build, lint and unit tests    pass  (run 41655231971)
Android emulator API 26       pass  (run 41655230608)
Android emulator API 35       pass  (run 41655230608)
```

## Status

`LOCALLY_VERIFIED` for the catalogue, the switch, the persistence and the UI, and
**`VPN_END_TO_END_BLOCKED`** for the transport. Not `RELEASE_VERIFIED`.
