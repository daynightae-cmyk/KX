# TASK_00 evidence — Brand identity, native Android foundation, Splash and launch gate

## Summary of what actually happened

TASK_00 turns the planning-only repository into a real, building, testable Android
application. It is the first commit in this repo that contains executable code.

Everything below was run on 2026-11-03 on the canonical checkout. Commands, exit codes
and counts are copied from the tool output, not restated from memory.

## Gate-by-gate result

| Gate | Result | Basis |
|---|---|---|
| G0 Reality | PASS | Recorded below; clean tree at baseline, one worktree, no stash, no competing branches. |
| G1 Visual | PASS WITH RECORDED DEVIATIONS | 10 real Compose renders produced by `BrandRenderTest`; compared against `00-brand-logo.png`. No device screenshots. |
| G2 Function | PASS | Real DataStore-backed launch gate, real Navigation Compose graph, real Android 12+ splash integration. No simulated state. |
| G3 Automated | PASS | 33/33 JVM unit tests, `lintDebug` clean, `assembleDebug`, `assembleDebugAndroidTest`. |
| G4 Runtime | **NOT VERIFIED — DEVICE_UNVERIFIED** | No accelerated emulator is possible on this machine. See blockers. |
| G5 VPN/security | NOT APPLICABLE / partial | TASK_00 ships no transport, so there is nothing to leak-test. Permission and secret review done. |
| G6 Integration | see PR section | PR + exact-SHA CI + merge recorded after this file is committed. |
| G7 Handoff | PASS | This file, the ledger and `docs/BLOCKERS.md`. |

## G0 — Reality

```
HEAD at start:            705af83d04399e6a3dd909015f8796a6cb0c0dbe
origin/main at start:     705af83d04399e6a3dd909015f8796a6cb0c0dbe
git status --porcelain:   (empty)
git worktree list:        D:/KNOUX-Workspace/KX 705af83 [main]   (single worktree)
git stash list:           (empty)
git remote -v:            origin https://github.com/daynightae-cmyk/KX.git
branches:                 main, remotes/origin/main only
gh auth:                  logged in as daynightae-cmyk (repo, workflow, read:org)
```

Reference asset verified before implementation:

```
sha256sum assets/reference/00-brand-logo.png
c0b92dd84d60f6f0938bff177d7066a2f74a3eab59ba282438abb76da5f8681e  matches docs/ASSET_MANIFEST.md
```

The image was opened and inspected. Its country list, Mbps, Ping, `user@…`, Premium and
"Fully Protected" elements are treated as fictional design values; none of them appear
in this build.

One untracked file, `assets/reference/reference.rar`, was present in the working tree
and was **not** created by this task. It is left untouched and uncommitted.

## Toolchain discovered (and what is missing)

| Component | State |
|---|---|
| JDK | 21.0.12.1 Microsoft OpenJDK (only JDK on the machine) |
| Android SDK | `D:/Android/sdk`, `platforms/android-36`, `build-tools 35.0.0 / 36.0.0`, `platform-tools` (adb), `cmdline-tools/latest` (sdkmanager 19.0) |
| Gradle | no system `gradle`; wrapper generated from the cached official `gradle-8.14.6` distribution |
| Emulator | **absent** — no `emulator/`, no `system-images/`, no AVDs |
| Virtualization | `VirtualizationFirmwareEnabled=True`, `HypervisorPresent=False` |

## Dependency selection — measured, not guessed

The newest AndroidX cannot be used here. `core:1.19`, `compose-bom 2026.08+`,
`navigation-compose 2.10` and `lifecycle 2.11` all declare
`minCompileSdk=37` and `minAndroidGradlePluginVersion=9.1.0` in
`META-INF/com/android/build/gradle/aar-metadata.properties`. The installed platform is
android-36 and AGP 8.13.2 supports compileSdk 36 at most.

`tools/probe_min_compile_sdk.py` reads those properties directly from the published
AARs. The first version of that tool reported `minCompileSdk=0` for everything because
`startswith("minCompileSdk")` also matched `minCompileSdkExtension` and overwrote the
real value; it was corrected to exact key matching.

Pinned stack (newest that fits):

| Artifact | Version | Declared minCompileSdk |
|---|---|---|
| Gradle | 8.14.6 (wrapper, SHA-256 pinned) | — |
| AGP | 8.13.2 | — |
| Kotlin | 2.3.21 | — |
| compose-bom | 2026.06.01 (ui/foundation 1.11.4) | 35 |
| androidx.core | 1.18.0 | 36 |
| androidx.activity:activity-compose | 1.13.0 | 36 |
| androidx.lifecycle | 2.10.0 | 35 |
| androidx.navigation:navigation-compose | 2.9.5 | 35 |
| androidx.datastore | 1.3.0 | none |
| androidx.core:core-splashscreen | 1.2.0 | 35 |

The Gradle wrapper distribution checksum is pinned in
`gradle/wrapper/gradle-wrapper.properties`:

```
distributionSha256Sum=7988ed071b2a07900e2ec715fca15c6ab72bce6db433af301fa7fa7e9407bd24
gradle/wrapper/gradle-wrapper.jar sha256: ad16bca46cb71bb5887c546860d93992aeee35c1fdaf4a242b0a612375d937e0
```

## G3 — Automated verification

Command (clean run, so nothing is inherited from a previous state):

```
./gradlew --no-daemon clean :app:testDebugUnitTest :app:lintDebug \
                        :app:assembleDebug :app:assembleDebugAndroidTest
```

Result: `BUILD SUCCESSFUL`, exit code 0.

```
com.kx.vpn.domain.LaunchDecisionTest                   tests=5 fail=0 err=0 skip=0
com.kx.vpn.ui.theme.KXMotionTest                       tests=6 fail=0 err=0 skip=0
com.kx.vpn.ui.brand.KXOrbGeometryTest                  tests=7 fail=0 err=0 skip=0
com.kx.vpn.data.PreferencesLaunchStateRepositoryTest   tests=5 fail=0 err=0 skip=0
com.kx.vpn.ui.BrandRenderTest                          tests=10 fail=0 err=0 skip=0
                                                     ----
TOTAL                                                   33      0     0     0
```

Artifacts:

```
app/build/outputs/apk/debug/app-debug.apk                 13,127,035 bytes
  sha256 895ee16b3b3fcd851f8242d2def01585dbadeb99c84eaa208175aee74b38a5f6
app/build/outputs/apk/androidTest/debug/…                 1,032,112 bytes
app/build/reports/lint-results-debug.html                 150,863 bytes
app/build/reports/lint-results-debug.sarif                 54,063 bytes
```

Lint ran with `abortOnError = true` and `checkDependencies = true`. Two real errors
were found and fixed rather than suppressed:

1. `NewApi` — `android:windowLightNavigationBar` in the theme requires API 27 while
   minSdk is 26. Fixed with an explicit `tools:targetApi="27"` on the item.
2. `LocalContextGetResourceValueCall` — a test read strings through
   `LocalContext.current` inside composition. Fixed by using `stringResource`, which is
   recomposition-correct.

One finding was a false positive and was resolved by removal rather than suppression:
`PropertyEscape` flagged `local.properties` and recommended the exact text the file
already contained. That file is gitignored and machine-local; deleting it lets AGP fall
back to the `ANDROID_HOME` that is already set system-wide.

## Bugs found in my own code during verification

Recording these because "tests pass" is not the interesting part; what the tests caught is.

1. **`jvmToolchain(17)` on a JDK-21-only machine** — failed Gradle configuration
   outright. Replaced with `jvmTarget = 17` on the running JDK.
2. **`local.properties` escaping** — `printf` produced `sdk.dir=D\:\Android\sdk`, which
   Java properties decode to `D:Androidsdk` (a drive-relative path). AGP's SDK locator
   threw "The filename, directory name, or volume label syntax is incorrect".
3. **Property/function name collision** — `KXApplication.launchStateRepository` shadowed
   the top-level `launchStateRepository(context)` factory. Renamed to
   `createLaunchStateRepository`.
4. **`catch` placed before `map`** in the DataStore flow — would have emitted a
   `LaunchSnapshot` into a `Flow<Preferences>` pipeline. Reordered.
5. **`SharingStarted.WhileSubscribed` on the launch gate** — the platform splash polls
   the state *before* Compose subscribes, so the flow would have stayed at `Loading`
   until the 3-second safety deadline. Changed to `Eagerly`.
6. **`LaunchedEffect(resolved, intro.value)`** — keying on an animating float restarts
   the coroutine every frame. Keyed on a settled boolean instead.
7. **Robolectric on a Windows home directory containing a space** — Robolectric derives
   its Maven repository from `user.home`, converts it to a file URL and never decodes
   it, so it looked for `C:\Users\day%20night\.m2\…` and failed before any test body
   ran. The test JVM is given a space-free `user.home` under `.robolectric-home/`.
   It must live outside `build/`: the first version put it there and `clean` deleted it
   mid-run, producing "Couldn't create lock file".
8. **`captureToImage()` never completes under Robolectric** — it forces a window redraw
   and waits for a frame callback that does not arrive, timing out at 2s in all 10
   render tests. Replaced with explicit measure/layout/draw of the decor view into a
   bitmap, which exercises the same Compose render path through the same native backend.
9. **A geometry test asserted `bounds.top == top`** for the shield crest. Wrong on the
   test's side, not the code's: the first Bézier handle sits above the anchor, so the
   bounding box legitimately overshoots. Assertion corrected to a range.

## G1 — Visual comparison against `00-brand-logo.png`

Renders are produced by `BrandRenderTest` (Robolectric native graphics) into
`app/build/kx-screenshots/`:

| File | Configuration |
|---|---|
| `01-orb-night-360-en.png` | Night, 360dp, EN |
| `02-orb-day-360-en.png` | Day, 360dp, EN |
| `03-orb-night-411-en.png` | Night, 411dp |
| `04-placeholder-night-360-ar.png` | Night, 360dp, Arabic RTL, real `values-ar` |
| `05-placeholder-day-411-ar.png` | Day, 411dp, Arabic RTL |
| `06-…-fontscale13-degraded.png` | 1.3x font scale, degraded-storage state |
| `07-component-sheet-day-360-en.png` | GlassCard, 3 button states, IconAction |
| `08-launch-night-360-en.png` | Full launch screen |
| `09-launch-night-360-en-loading.png` | Store silent — gate must hold |
| `10-orb-inline-40dp-night.png` | Emblem at inline size |

Confirmed present and matching the reference: circular pearl-glass orb, ribbon-folded
shield forming the X, three orbiting jewel nodes on a tilted elliptical ring, four-point
sparkles, flowing lower ribbons, gradient KX wordmark, midnight night surface and pale
lilac day surface.

### Palette: measured values vs the suggested tokens

`docs/DESIGN_SYSTEM.md` lists suggested tokens "to tune after visual pixel sampling".
Sampling was done with `System.Drawing` pixel probes on the 1254×1254 artwork. Several
suggestions are measurably wrong:

| Token | Suggested | Sampled | Probe |
|---|---|---|---|
| Artwork backdrop | `#0B102C` | `#000000` | (10,10), (1240,1240) |
| Violet (shield body) | `#6537E8` | `#5932D3` | (520,330) |
| Cyan | `#5BCFFD` | `#6FB3FC` | (322,375) node |
| Orchid | `#BE69EF` | `#D169FC` | (352,640) node |
| Orb lavender | — | `#B6A5FC` | (300,520) |
| Orb glow | — | `#D7D4FE` | (627,700) |
| Orb rim | — | `#C2DAFD` | (627,105) |
| Shield deep | — | `#2F19BA` | (640,600) |
| Wordmark ink | — | `#140A82` | (400,860) |

`KXPalette.kt` keeps both sets and documents which is measured and which is suggested.
The pure-black backdrop is the artwork matte, not a UI token, so app surfaces still use
midnight `#0B102C`.

### Recorded deviations from the reference

These are differences a reviewer should know about rather than discover:

1. **The reference is a soft 3D render; the app draws vector geometry.** Diffraction
   bloom, per-pixel specular noise and the volumetric glass depth cannot be reproduced
   with Canvas gradients at reasonable frame cost, and `DESIGN_SYSTEM.md` explicitly
   warns against expensive blur. The silhouette and colour structure match; the
   material realism does not.
2. **The wordmark is real text, not painted glyphs.** This was a deliberate choice: it
   makes the mark follow the user's font scale and readable by a screen reader. The
   consequence is that it uses the platform sans rather than the reference's custom
   letterforms, and its weight differs slightly per device font.
3. **The ribbon X is two stroked Bézier folds**, which reads correctly at hero size but
   simplifies to a solid mass at the 40dp inline size (see render 10).
4. **Day-mode orb is pulled down in core luminance** (`#EDE9FF` rather than the sampled
   `#D7D4FE`) so the emblem does not disappear against the pale lilac surface. Contrast
   was protected over literal colour copying, as `DESIGN_SYSTEM.md` instructs.

## G2 — Function

Wired end to end, not stubbed:

- `MainActivity` calls `installSplashScreen()` before `super.onCreate()` and holds the
  platform splash with `setKeepOnScreenCondition` while the launch state is `Loading`,
  bounded by a hard 3-second deadline so a wedged store cannot strand the splash.
- `LaunchViewModel` maps a real DataStore flow into `LaunchUiState`, `Eagerly` shared.
- `decideLaunchDestination()` is a pure function; a degraded store fails toward Welcome
  and can never be read as "already set up".
- `PreferencesLaunchStateRepository` is tested against a real on-disk DataStore,
  including a 64-byte garbage file and a path that cannot be opened as a directory.
  Both produce `storageDegraded = true`, which the UI surfaces as a plain-language
  notice rather than silently pretending to be a fresh install.
- Navigation Compose graph with `Launch → Welcome | Home`; the launch node is popped
  inclusively so back never replays the splash.
- `completeOnboarding()` exists as the persistence contract but **has no caller in
  TASK_00** — onboarding is not marked accepted before TASK_01 builds the flow that
  accepts it.

## Functional honesty checks that were run

- The manifest declares **zero permissions**. No INTERNET, no camera, no storage.
- `PlaceholderScreen` states in both languages that there is no server, no traffic and
  no protection status in this build.
- `LaunchInstrumentedTest.noProtectionClaimIsEverRendered` asserts on a device that
  "Connected", "Fully Protected", "Protected", "Premium" and "Unlimited" never appear.
  It is compiled but not executed (no device).
- The launch screen's footnote is always visible: "This build contains no VPN
  connection. Nothing is routed or protected yet."
- A test asserts the persisted preferences file contains no private-key, preshared-key,
  bearer, password or wireguard material.

## Security review performed for this slice

- `allowBackup=false`, `fullBackupContent=false`, and `data_extraction_rules.xml`
  excludes `pref`, `file` and `database` from both cloud backup and device transfer.
  The comment in that file states the obligation for TASK_09.
- No signing material, keystore or token exists anywhere in the tree; `.gitignore`
  already excludes `*.jks`, `*.keystore`, `*.p12`, `*.pfx`, `*.pem`, `*.key`, `*.ovpn`,
  `.env`.
- CI runs a secret-pattern scan over the whole tree on every push and PR.
- CI validates the Gradle wrapper jar (`gradle/actions/wrapper-validation@v4`) and
  re-verifies all 11 reference asset SHA-256 hashes, so a swapped asset or a poisoned
  wrapper fails the build.
- No logging of configuration content anywhere in the slice.

## What is NOT done, and is not claimed as done

- No emulator or physical-device run. G4 is `DEVICE_UNVERIFIED`.
- No VPN transport of any kind. No handshake, route, DNS or leak testing exists because
  there is nothing to test yet; that is TASK_02 onward.
- No release build, no signing, no Play artifacts. `release` keeps minification off
  until a reviewed R8 ruleset and a signing pipeline exist.
- Robolectric renders are JVM Skia output. They prove the composition tree draws the
  intended geometry, colours, RTL layout and font scaling. They do not prove behaviour
  on a real GPU, real display cutouts, real insets or a real activity lifecycle.

## Status

`LOCALLY_VERIFIED` for the provider-independent TASK_00 scope, pending PR/CI/merge, at
which point it becomes `MERGED_VERIFIED` **for that scope only**. It never becomes
`DEVICE_UNVERIFIED`-free until a real device run happens, which is not possible on this
machine without owner-level changes to Windows virtualization.
