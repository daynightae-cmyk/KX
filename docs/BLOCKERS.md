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
