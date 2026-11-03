# Native library pin for com:zaneschepke:wireguard-android:1.2.1

These are the prebuilt Go/WireGuard `.so` blobs that ship inside the AAR. They
are binary-only: no source for them is in this repository and none has been
audited here. The owner chose this repackaged backend deliberately, so the
compensating control is that CI fails if any byte of any native library differs
from what was recorded on 2026-11-03.

Regenerate with: `python3 tools/pin_native_libraries.py --write`

| path | bytes | sha256 |
|---|---|---|
| `jni/arm64-v8a/libwg-go.so` | 2940136 | `9118b6a0bb9157d0bff8e6d31f6725068ef73ec93ec3fc6a725efffaca3275cb` |
| `jni/arm64-v8a/libwg-quick.so` | 29960 | `281cd3b4d1225e83ae17b4249fd2da616d64698e8709384ee8b3ede33282bc07` |
| `jni/arm64-v8a/libwg.so` | 75592 | `2a7934408857573a226c37808b9607a810dac01218ae4ef1e112e25b0e0d536d` |
| `jni/armeabi-v7a/libwg-go.so` | 2687700 | `3e47ffe7387197635fd7d62bd632f9eee5fed0122488c7ebbe1ced9af3d2fedb` |
| `jni/armeabi-v7a/libwg-quick.so` | 22920 | `749075b4a5661dae2e169759e75a1d216872b9217f2cf3188be48ae873fe9f83` |
| `jni/armeabi-v7a/libwg.so` | 47184 | `e3335373856c658f47b8ddb669d04b5c95cebce72cbb096aa772c7117de5d658` |
| `jni/x86/libwg-go.so` | 2668184 | `2a0d3542b645b1dcfc2a020cbcfba37b5305357786c3b87bb921eb52a45c5250` |
| `jni/x86/libwg-quick.so` | 29092 | `7a648812c2dedcd8a27e8d304962bb679016ceba7b48e9edfa4445da7f272320` |
| `jni/x86/libwg.so` | 66044 | `d36b504e090992f7d5e529db55951d6df9fa0da7785fde92257ac5ed8473a2ef` |
| `jni/x86_64/libwg-go.so` | 3047880 | `17ddd23bc829b3177a7ac2495bd40c10708c45d8ff2d203810b8e59fbb676f7f` |
| `jni/x86_64/libwg-quick.so` | 31624 | `44496fdc7fc672b2f432357cc24ee888471ab8b7c3a533ba07900a1e16c000ed` |
| `jni/x86_64/libwg.so` | 96056 | `44a074f66cf7bd802467e57448a0dc432b5c409d408e462df7ad333f27355bf6` |

AAR sha256: `b5a17f1c38ddc4a9bfe60fd8abfae45cf1bd3b11ebb77be77651292b8e168c44`
