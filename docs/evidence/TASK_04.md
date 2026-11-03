# TASK_04 evidence — Connected session, real counters, monotonic timer

## Provenance of this document

TASK_04 was merged into `main` as `eef3765` **without an evidence file**, and the ledger
row still said `NOT_STARTED`. This document was written afterwards, from the recorded CI
runs and artifacts, so the ledger and the tree agree. That ordering is a process failure
and is recorded in `docs/BLOCKERS.md` rather than smoothed over: the acceptance rule is
"evidence in the same PR", and here the evidence came two tasks late.

## Summary

The connected screen shows a session timer from a monotonic clock, throughput derived
from the tunnel's own byte counters, a ping measured against the user's own endpoint, and
the tunnel address read from the user's own configuration. The reference's Mbps figures,
public IP and country pair are not reproduced because nothing in this build produces them.

## Gates

```
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
→ BUILD SUCCESSFUL, exit 0
```

| Stage | SHA | Run | Result |
|---|---|---|---|
| Android CI (pull_request) | `1f7c96b` | 41683174319 | success |
| KX Android Device Runtime (pull_request) | `1f7c96b` | 41683174323 | success |
| Android CI (push, merge commit) | `eef3765` | 41694367873 | **failed first, then success** |
| KX Android Device Runtime (push) | `eef3765` | 41694367932 | success |

Instrumented execution from the merge-commit artifact:

```
api-26: tests="23" failures="0" errors="0" skipped="0"   test-exit-code.txt: 0
api-35: tests="23" failures="0" errors="0" skipped="0"   test-exit-code.txt: 0
```

### The merge-commit CI was red, and it was not the code

The first post-merge `Android CI` run failed with:

```
ConnectionRingGeometryTest > classMethod FAILED
    java.lang.AssertionError at MavenArtifactFetcher.java:130
        Caused by: java.io.IOException at HttpURLConnection.java:2034
```

`classMethod` means the class never ran: Robolectric was fetching its `android-all` jar
from Maven at test time and the request failed. Three test classes hit it, two of which
had passed on every earlier run, and the device workflow — which runs the same suite on
real emulators — was green in the same minute. Re-running the failed job returned
success. So this is a reproducibility weakness in how Robolectric resolves its artifacts,
not a defect in the app, but it did leave `main` red and it is the kind of failure that
"CI was green on the PR" hides.

**Open follow-up:** pin or pre-seed Robolectric's `android-all` artifacts so a Maven
outage cannot turn `main` red. Tracked in `docs/BLOCKERS.md`.

## What is verified

- **Monotonic duration.** `SessionTelemetry.elapsedMillis` takes only monotonic readings
  and returns null when the source goes backwards, so a reboot or a clock edit cannot
  produce a negative or future-dated session. `ConnectedSessionTrackerTest` drives a fake
  clock and asserts it.
- **Throughput needs two trustworthy samples.** A counter reset — which is what a
  reconnect does to per-tunnel WireGuard statistics — yields no rate rather than a
  negative one, and the next window measures normally from the new base.
- **Nothing is sampled while the tunnel is down**, and disconnecting clears every number,
  so the screen cannot display a stale figure from a session that ended.
- **The headline follows the backend.** `ConnectedSessionContent` reads
  `sessionManager.state`, not the telemetry mirror, so "Connected" is drawn only for
  `TunnelState.CONNECTED`. `43-connecting-night-360-en.png` is that state rendered.
- **Ping is a real measurement** against the endpoint the user selected, with the instant
  it was taken; an endpoint that does not answer is reported as failed, not as 0 ms.
- **Tunnel address is read** from the user's `Address` line
  (`ProfileEndpointReader.tunnelAddress`), and the public exit address is stated as not
  verified rather than shown.

## What is NOT verified

- **`BLOCKED_NETWORK_VERIFY`.** No owned gateway, so no traffic actually traversed a
  tunnel, no exit IP was compared, no DNS or IPv6 leak test was run, and no counter
  growth was observed against a controlled transfer. The counters are proven to be read
  and computed correctly; that they correspond to real bytes crossing a real tunnel is
  not proven here.
- **Throughput has never been seen on a live link.** Every rate in the renders and tests
  comes from supplied samples.
- **No physical device.** `DEVICE_UNVERIFIED` stands for that specifically.

## Visual parity

`docs/evidence/TASK_04/` holds the frames produced at merge time. A later structural-parity
pass (`26cec8a`) re-measured the screen against the artwork and changed the ring from
248dp to 300dp, the metric tiles to icon-left at 62dp with a 10dp gutter, the route card
to 70dp, and the screen padding from 24dp to 16dp; those frames live in
`docs/evidence/TASK_05/` because they were committed with that correction.

## Status

`MERGED_VERIFIED` for the telemetry model, the tracker, the screen and the emulator
runtime — with `BLOCKED_NETWORK_VERIFY` for anything about real traffic, and with the
evidence-lateness defect above recorded rather than hidden.
