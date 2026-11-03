# Security & privacy requirements

Threat inventory: hostile QR/file config, secret disclosure in persisted prefs/logs/backup, forged connection status, revoked VPN consent, DNS or IPv6 leaks, per-app route mistakes, malicious gateway, network captive portals and fabricated privacy claims.

- Parse only explicit supported formats; impose size/rate limits, strict key validation, URI normalization and safe error messages. No executing input scripts or blindly fetching URLs.
- Secrets: do not commit, print, screenshot, analytics-track or log private/preshared keys or bearer tokens. Use Android platform key protection with safe lock/clear/reset semantics. No fallback to unencrypted secrets.
- Android VPN API: system permission is mandatory, never bypass; revalidate after revocation/reboot. Always-on and “block without VPN” require user/device settings; do not pretend a local switch grants system-wide lockdown.
- Kill switch must be tied to actual Android lockdown policy or a correctly implemented and audited network control; if unsupported show guidance instead of enabled badge.
- DNS/IPv6 protection: verify routes/resolvers and leak behavior for supported mode; unsupported traffic families must fail closed where appropriately required and user-informed. Never hide leaks with UI claims.
- Per-app split tunneling: supported allowlist **or** denylist semantics, not both concurrently. Explain bypass policy clearly.
- Connection status: tunnel backend + network proof distinguish VPN UP from Internet Verified. “Fully Protected”/“Privacy Score” require defensible measure and cannot be inferred merely from session on.
- Storage: minimum local session metadata; user can clear; records don't contain private keys/IP history by default; document retention.
- Dependencies/license/security scan, least privileges, release signing and policy review before production publishing.
