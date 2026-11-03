# TASK_00 — permanent runtime CI and native visual matrix

Status: **PARTIAL** — device execution and original screenshot collection VERIFIED
on the implementation SHA below; human visual review/physical device NOT VERIFIED.
**No merge authorized/performed by this agent.**
This checkpoint supplements earlier evidence; it does not overwrite other agents'
ledgers or claim that local Windows virtualization was changed.

## Superseding the former workflow blocker

The owner reported workflow installation at `19c0acb`. A fresh main read resolved to
`8bd13929cba45cd923468fb87f5098e5f979253b`; the permanent
`.github/workflows/kx-device-runtime.yml` was inspected there and runs automatically
on PRs to main and pushes to main, using the existing single-shell helper.
**No YAML upload, replacement credential or per-branch workflow copy is needed.**
No workflow edit was attempted. Historical local hypervisor limitations still apply
to that local machine, but do not establish a blocker to GitHub-hosted emulator tests.
PR #4 had already been merged by another actor; this invocation did not merge it.

## Verified integrated-baseline CI

Exact main SHA: `8bd13929cba45cd923468fb87f5098e5f979253b`.

| Check | Primary result |
|---|---|
| Build, lint and unit tests | Completed success, run [41608365989](https://github.com/daynightae-cmyk/KX/actions/runs/41608365989), job 125824135675 |
| API 26 | [Run 41608365980](https://github.com/daynightae-cmyk/KX/actions/runs/41608365980), job 125824241374: 6 instrumentation tests completed, zero failures, `BUILD SUCCESSFUL in 1m 21s` |
| API 35 | Same runtime run, job 125824240976: 6 instrumentation tests completed, zero failures, `BUILD SUCCESSFUL in 1m 23s` |
| Evidence collector | Both jobs completed success; validated a 1080×1920 **post-test relaunch** PNG after Gradle removed the instrumentation-time directory |

Uploaded runtime artifacts: API 26 `13217962058`, API 35 `13217363780`.
These captures are explicitly **not** instrumentation-time matrix screenshots. The
logs prove the runtime suite executed and the collector produced files, not that
someone inspected the images, TalkBack or every lifecycle/visual requirement.
Physical hardware and release/transport/network checks remain unverified/outside
this launch-only implementation. No VPN exists yet.

## New isolated implementation — draft PR #9

Branch: `agent/task-00-runtime-error-contract` (name retained; change is visual
runtime verification, not a parallel app). Published implementation SHA:
`977984b516abc9b29797ae274d82ff76195b86aa`.
PR: https://github.com/daynightae-cmyk/KX/pull/9

- New parameterized device suite uses **existing** native KX theme, reduced-motion
  policy, resources and first-run placeholder. 16 distinct combinations:
  AR/EN × day/night × 360/411dp × 1.0/1.3 font scale.
- Captures top and scrolled-action states (32 requested matrix PNGs), checks the
  action is reachable, disabled truthfully and at least 48dp tall.
- These are composition configuration overrides on a real emulator, **not** proof
  of an implemented locale/theme settings switch or an onboarding flow.
- Existing MainActivity startup/recreation tests remain intact. No production UI,
  manifest, dependencies, design references, gateway, workflow or account changes.
- AGP's `android.injected.androidTest.leaveApksInstalledAfterRun=true` keeps original
  screenshots available to the existing collector after tests, without rewriting
  the other agent's post-test fallback. This is the same test option used by
  [AndroidX](https://android.googlesource.com/platform/frameworks/support/+/HEAD/playground-common/androidx-shared.properties).
  It retains test APK/data on the test device; developers using personal devices
  should use a dedicated test profile/device and remove test builds themselves when
  finished. No release app behavior or permissions are changed.

## Local checks and remaining evidence

- `git diff --check`: success.
- Matrix cross-product uniqueness/count: 16 distinct variants.
- Existing `python3 tools/test_android_device_runner.py`: success, seven stub-command
  cases (not Android execution).
- Latest implementation's build/lint/JVM/instrumentation compilation CI completed
  success at observation, run [41608955650](https://github.com/daynightae-cmyk/KX/actions/runs/41608955650).
- Device jobs for implementation SHA, run
  [41608955511](https://github.com/daynightae-cmyk/KX/actions/runs/41608955511),
  completed **success on both APIs**. The full completed logs were fetched/read:
  - API 26, job `125825807004`: **22/22 device tests completed, 0 skipped, 0 failed**,
    `BUILD SUCCESSFUL in 1m 38s`; **33 original files pulled, 0 skipped**.
  - API 35, job `125825806681`: **22/22 device tests completed, 0 skipped, 0 failed**,
    `BUILD SUCCESSFUL in 2m 43s`; **33 original files pulled, 0 skipped**.
  - Each set comprises 32 requested matrix screenshots plus the existing launch
    screenshot. Collection did **not** take the post-test reinstall fallback.
  - API 26 artifact: [13218635756](https://github.com/daynightae-cmyk/KX/actions/runs/41608955511/artifacts/13218635756).
  - API 35 artifact: [13218316991](https://github.com/daynightae-cmyk/KX/actions/runs/41608955511/artifacts/13218316991).
  - Build/lint/JVM/assembly job `125826018612` succeeded. JVM task was cache-restored;
    no newly executed 44-unit-test run is inferred from this job. Instrumentation
    compilation and APK build also succeeded; actual device counts above executed.
  - Existing Compose test-rule deprecation warnings remain; no deprecated API
    migration or test weakening was smuggled into this focused change.
- No downloaded/reviewed runtime screenshot matrix is claimed. Uploaded output must
  be checked for all variants, meaningful rendered content, AR/EN text, clipping,
  contrast and insets before visual acceptance. A default relaunch fallback must not
  be substituted for missing per-variant evidence.
- No merge, branch deletion, forced update, reset, cleanup of other working sets or
  modification of PR #2/#5/#6 was performed. Other agents' private working sets are
  not visible; work used a clean isolated main-SHA checkout.

Next: finish automatic exact-head CI investigation/corrections, review real visual
artifacts where accessible, and leave reviewable work **unmerged**, per the owner's
latest instruction. TASK_01–21 are not claimed delivered by this verification PR.

## Exact scope and remaining verification boundary

The connected GitHub read tools expose check metadata and job logs, not downloadable
Actions artifact content. Thus original PNG **collection/upload** is verified from
logs, but no pixel-level inspection, approved reference comparison or screenshot
download is claimed here. The artifact links above preserve the precise evidence
for visual review. This does not make the matrix a fake UI: actual native components
were rendered and their scroll/disabled/size assertions executed on both emulators.

The final evidence-document commit is documentation-only and triggers its own CI;
its check state must not be confused with the completed implementation-SHA run.
The branch stays reviewable and unmerged. No request for another YAML upload is
necessary, and no next feature is represented as complete by this runtime work.
