# TASK_02 — candidate, not complete / no merge

Base: `1080767f9529ad1793e88822a4a3b353384140f2` (TASK_01 integrated).
Branch: `agent/task02-wireguard-session`. No gateway, key or endpoint was invented.
Reference SHA-256 matched: `825c75b082fdb1e2c4528ad3f62c5b1b35b003b069d55773df39c7a995f3a28e`;
source image opened. Raw artwork remains unchanged; Home uses existing native KX
orb/background/cards. Visual fidelity/complete reference layout is NOT VERIFIED.

## Actual code

- WireGuard Android tunnel artifact pinned at `1.0.20260102`; exact tag source was
  downloaded from upstream GitHub and inspected for Backend/Tunnel/Config signatures,
  lifecycle and callback behavior. Apache-2.0 upstream notices/licenses apply.
  This is an integration candidate, **not an independent security audit**. Native
  16KB-page compatibility, current dependency advisories and license inventory are
  release gates; no latest-version claim. No custom cryptography/protocol.
- Home consent uses Android VpnService.prepare and rechecks approval before starting
  the non-exported foreground owner service. Notification has a disconnect action.
  Service is NOT_STICKY; no reboot/always-on/automatic connection claim. Library's
  real VpnService is contributed by dependency manifest and performs tunnel setup.
- Application-scoped session state receives actual GoBackend setState/callback facts.
  Serialized commands; no config cannot connect; denied consent, exception categories,
  duplicate commands, disconnect/connect race and DOWN during start covered by tests.
  UP explicitly means **tunnel backend only**, not handshake, Internet verified,
  leak protection, paid status, country fleet or security score.
- Minimal Home file picker validates the existing bounded inspector and native parser,
  then saves one profile using AndroidKeyStore AES-256-GCM + AtomicFile in noBackup
  storage, without a plaintext fallback or retained URI. Before picker dispatch the UI
  explicitly explains replacement/local encryption. Invalid import does not replace
  the prior saved profile. Secret bytes are cleared after processing; native Config
  necessarily holds key material in memory. No config/key/exception text is logged
  by the application or put into UI StateFlow. The upstream library may log endpoint
  DNS hostnames; it must be reviewed for production diagnostic policy.
- Metrics are explicitly unavailable, not synthesized. Additional tabs/fleet/import
  polish belong to subsequent scope and are not faked.

## Actual local checks

Same session controller production/test source compiled with standalone Kotlin
2.1.20/JDK17 and compiler-bundled coroutines 1.8.0, JUnit4.13.2: **12 tests passed**.
This is a focused state-machine check, **not** the app-pinned Kotlin2.3.21 /
coroutines1.11.0 Android Gradle suite. Resource XML/catalog TOML parsed, diff checks
passed. Exact-SHA GitHub automatic CI must determine Android build/Lint/device state.
Added a real-device AndroidKeystore round-trip/tamper test; not yet run at writing.

## Remaining / blocked acceptance

- **VPN_END_TO_END_BLOCKED:** no owned/authorized controlled gateway is available to
  this session. No handshake, route, DNS, IPv4/IPv6, leak or traffic-through-egress
  proof. Do not send/import private keys through chat. No endpoint provisioning or
  payment is performed. A provider-free user-owned config is supported as a path,
  not proof that a gateway exists or works.
- Foreground lifecycle/revocation/no-network/notification-denial/kill/doze/Android
  specialUse policy require real-device exercise and policy review. Native setState
  UP alone is insufficient. No release or complete lifecycle claim.
- Screenshots, Home reference comparison, AR/EN/day/night/font/insets/TalkBack and
  user-driven system consent have not been verified for this candidate at writing.
- TASK_02 must stay draft/partial, **unmerged per owner instruction**. TASK_03–06
  depend on its essential traffic acceptance and therefore are NOT_STARTED here;
  no five-task completion claim. Do not bypass tests or alter the serial contract
  to inflate task counts. All prior branches and other agents' work preserved.

## Candidate corrections before verification

- Review identified service destruction clearing a prior failure via disconnect.
  Cleanup now still invokes backend teardown but preserves the failure indication;
  a new regression brings the standalone suite to **12 passing tests**.
- First inspected the historical 20230706 tag, then found the published maintained
  release 20260102 in Maven Central and inspected that exact upstream tag too.
  It builds at compileSdk36/minSdk24 (fits this app) and enables flexible native
  page sizes. The dependency was updated before acceptance; actual packaged 16KB
  device compatibility remains unverified. No audit/security-advisory clean claim.
- Added device native-library loading/version check without connecting a tunnel,
  plus real Home unconfigured-state/import-scroll/screenshot checks. These are not
  consent/handshake/traffic proof and do not modify onboarding state.

## Exact implementation CI — actual measured result

Published code SHA `d650567bff5f1472d303c2f485cf7cc2684d7fc1`, draft
https://github.com/daynightae-cmyk/KX/pull/11 (not merged).

Build run https://github.com/daynightae-cmyk/KX/actions/runs/41622896512,
job `125873245200` completed success. Full completed log was read:
**105 executed unit-test PASSED lines**, including all **12 SessionController tests**;
`lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` each BUILD SUCCESSFUL.
This is now app-pinned-dependency CI evidence, unlike the narrower standalone smoke
run. Automatic review/hygiene succeeded; skipped draft reviewer is not an approval.

Runtime run https://github.com/daynightae-cmyk/KX/actions/runs/41622896518:
- API26 job `125872929600`: **13 instrumentation tests completed, 0 failures**,
  BUILD SUCCESSFUL in 3m7s.
- API35 job `125872929328`: **13 instrumentation tests completed, 0 failures**,
  BUILD SUCCESSFUL in 3m26s.
- The suite includes native GoBackend library loading/version, real AndroidKeyStore
  encryption round-trip/tamper rejection, unconfigured Home disabled connect and
  reachable import control, and existing first-run/navigation/recreation regressions.
  **It does not connect a tunnel or prove a gateway handshake.**
- Original screenshot directory was removed by Gradle in this run, so the collector
  truthfully generated post-test relaunch PNGs instead. These are not proof of Home
  visual fidelity. A subsequent test-only Gradle option retains original screenshots
  for collection; final exact-head CI must validate that separate change.
- Artifacts from this measured run:
  API26 https://github.com/daynightae-cmyk/KX/actions/runs/41622896518/artifacts/13224048269
  API35 https://github.com/daynightae-cmyk/KX/actions/runs/41622896518/artifacts/13225136874

No secret configs or gateway addresses were generated/printed/committed. No merge,
provider purchase, workflow edit, release, other-agent branch overwrite or completion
of TASK_03–06 occurred. Controlled transport, consent/revocation/foreground lifecycle,
true process-kill, network-change/airplane/doze, complete Home visuals and physical
hardware remain acceptance gates. The automatic workflow is operational; no YAML
request is needed. Status remains **PARTIAL / VPN_END_TO_END_BLOCKED**, not COMPLETE.

## Follow-up: cancellation is not teardown (2026-11-03)

Baseline for this patch: draft #11 head `ce7fcb2c2236296466f4b5a2b309e61a2cc0deec`.
The candidate's cancellation/exception path changed state to Failed but never called
`TunnelDriver.stop()`. The actual adapter uses `withContext(IO)` around native work;
that work may have completed before the cancelled owner resumes. A UI failure does
not prove that Android/native routing was torn down.

Reproduced **before changing production code** with two focused regressions:
14 tests run / **2 failed** — cancelled-start cleanup and thrown-start cleanup both
observed no stop request (expected 1, actual 0 / stopped flag false). These are
controlled-driver reproductions, not evidence of a real-device leak or production harm.

Fix and safeguards:
- Failed/cancelled/invalidated starts attempt backend stop outside parent cancellation
  before returning an error; cancellation still propagates to the caller.
- Cancelled disconnect also retries shutdown outside the cancelled owner.
- Cleanup has a five-second **cooperative** timeout. It cannot forcibly interrupt
  blocking JNI/OS code; no hard deadline or guaranteed shutdown claim.
- False/throwing/timed-out shutdown yields explicit `CleanupRequired`, not a clean
  disconnected state. Profile replacement and new connection commands are blocked;
  Home explains uncertainty in AR/EN and keeps Disconnect available. The foreground
  owner does not stop itself solely because this unresolved condition is a failure.
- Confirmed subsequent Disconnect unblocks operation. A permission-denial callback
  cannot erase the cleanup warning. Backend error messages/config values stay out
  of the state. Cancellation of a command still waiting for the mutex cannot stop
  another command's tunnel.

Seven added regression cases cover cancellation, partial start exception, unsuccessful
cleanup/retry, exception redaction, cancelled stop, native-style blocking IO completion
following cancellation and cancelled queued commands. Existing cleanup test now counts
its follow-up stop relative to the immediate failed-start cleanup rather than assuming
that no earlier cleanup occurred. Test-driver failure returning false for BOTH start
and stop now requires CleanupRequired; this is a stricter honest state, not a relaxed
assertion.

Standalone source check: **19 tests passed** using Kotlin2.1.20/JDK17/JUnit4.13.2 and
compiler coroutines1.8.0; Android app-pinned suite and automatic device CI must be read
on the patch's exact published SHA. No real gateway/handshake/DNS/route/traffic proof,
TalkBack, unknown-cleanup native lifecycle proof or visual comparison is inferred from
these controlled tests. Existing task blockers remain. No merge or workflow change.
