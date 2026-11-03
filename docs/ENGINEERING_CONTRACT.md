# Android full-stack engineering contract

## Stack and architecture
Kotlin, Gradle Kotlin DSL, Jetpack Compose, Material 3 customized to the reference, AndroidX Navigation, ViewModel/StateFlow, Coroutines. Room/DataStore where appropriate. Architecture is UI → presentation/state → use cases → repositories → actual Android VPN/secure storage infrastructure. Avoid single-class monolith; maintain testability. Source code must compile and instrumented navigation must behave on real emulator.

## Real VPN engine
Use Android `VpnService` with system user consent, lifecycle notifications, onRevoke handling, foreground service behavior and a pinned/reviewed WireGuard tunnel integration where supported. No custom cryptographic protocol. Stable state machine:
UNCONFIGURED → DISCONNECTED → PREPARING_PERMISSION → CONNECTING → CONNECTED → DISCONNECTING; and FAILED / RECONNECTING / REVOKED when applicable.
Represent connection state only from backend/platform facts. Actual traffic checks/metrics separately prove reachability. Handle no config, invalid config, VPN service already in use, permission denied, airplane mode, Wi-Fi↔mobile, process death, doze, gateway loss and network restart.

## Functional honesty
User-supplied WireGuard config and owned test gateway are valid zero-subscription starting paths. No fabricated country fleet or latencies. If a paid/proprietary provider is unavailable, implement the provider-agnostic local function; disable/block only features genuinely dependent on missing external resources. Do not assert OpenVPN/obfuscation/port hopping/tracker blocking until separate audited implementation.

## Secure data
Never output private keys, PSK, auth tokens or live config URIs in Git, logs, evidence, analytics or screenshots. Encrypt/wrap sensitive stored material using platform primitives as appropriate, with no plaintext fallback. Apply parser size/encoding limits and safe invalid-input errors. Do not allow backups to extract secrets without user authorization. No synthetic privacy score or no-logs guarantee.

## UX contract
One image per feature; build responsive **native Compose elements**, not screenshot overlays. Glass orb, flowing cyan/violet ribbons, orbital nodes, deep navy and pearl Day surface. Touch targets >=48dp, reduced motion, readable contrast, large font test, screen-reader semantics, portrait insets, AR/EN language resources, RTL/LTR navigation and real back behavior. Keep interaction state truthful.

## Mandatory feature-level tests
Kotlin JVM unit tests on state/domain, parser/storage edge cases, instrumentation UI navigation/accessibility, lint + assembleDebug + Android emulator startup/logcat, screenshots compared to reference, and controlled-gateway connection + DNS/IPv4/IPv6 leak checks for any network claim. Device hardware tests required before release. Clear actual pass/fail status with exact SHA and logs.
