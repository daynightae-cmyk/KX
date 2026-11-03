# TASK_02 — candidate, not complete / no merge

Base: `1080767f9529ad1793e88822a4a3b353384140f2` (TASK_01 integrated).
Branch: `agent/task02-wireguard-session`. No gateway, key or endpoint was invented.
Reference SHA-256 matched: `825c75b082fdb1e2c4528ad3f62c5b1b35b003b069d55773df39c7a995f3a28e`;
source image opened. Raw artwork remains unchanged; Home uses existing native KX
orb/background/cards. Visual fidelity/complete reference layout is NOT VERIFIED.

## Actual code

- WireGuard Android tunnel artifact pinned at `1.0.20230706`; exact tag source was
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
2.1.20/JDK17 and compiler-bundled coroutines 1.8.0, JUnit4.13.2: **11 tests passed**.
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
