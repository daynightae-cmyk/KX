# TASK_00 reconciliation checkpoint — 2026-11-03

This supplements, and does not overwrite, either candidate's original evidence.
No merged/device-complete status is claimed by this checkpoint.

## Observed reality

- Fresh integrated baseline inspected: `d362ea2f28a5983dcf823a986de280e32f49d47a`.
  PR #1 already merged `com.kx.vpn`; its ledger discloses that runtime is unverified.
- Draft #2 remains preserved, with independent `com.knoux.kx` source and user-installed
  Android CI, head `ca2d1d06b6e1f835345bdca7576bb4a0db5f16f5` at inspection.
  It conflicts with integrated main. Do not merge a second app over the first.
- PR #3 (`4129256a0725cecb575993966a25132e7387b852`) addresses undeclared cached
  screenshot outputs. Its CI is green at observation. Its branch/build edits were
  not overwritten or duplicated here.
- Current main-based work: [draft #4](https://github.com/daynightae-cmyk/KX/pull/4),
  branch `agent/task-00-startup-deadline`. First published code SHA
  `12843db0bea7cda2a4d404b7053a647e502a661e`; later runner/capture changes have
  their own published SHA. No merge performed. Other agents' private working sets
  cannot be inspected; isolated clones were clean before edits, with no stashes.

## Primary CI logs inspected — do not conflate these results

Run https://github.com/daynightae-cmyk/KX/actions/runs/41600114925 on draft #2:

- API 26 job `125796596229`: **19 instrumented tests completed, zero failures**,
  `BUILD SUCCESSFUL in 3m 6s`; then the evidence shell fragment failed with
  `Syntax error: end of file unexpected (expecting "fi")`.
- API 35 job `125796595118`: **19 instrumented tests completed, zero failures**,
  `BUILD SUCCESSFUL in 2m 54s`; the same evidence collection error follows.
- Build/lint job `125796595391`: APK packaging reached, but Lint reports **32
  errors** under warnings-as-errors; first is `OldTargetApi` on target 35.
  The full report was uploaded as an artifact but is not available through the
  connected repository read tools. Do not guess that all 32 have the same cause.

The emulator action executes each `script:` line in a separate shell. Thus `set +e`,
`test_exit=$?`, and a multiline `if ... then ... fi` do not share a shell; even a
failed Gradle run could lose its exit status. Both the parser and status handling
need correction. Passing draft #2 tests is **not runtime proof for main or #4**,
which use different source/package/dependencies.

## Main-based defects corrected in #4

1. Test teardown previously deleted `fixtureDirectory.parentFile`, the **shared
   system temp directory**, rather than the fixture. New unique per-test roots
   contain cleanup, and a sibling sentinel regression checks non-target files.
   No unrelated data loss was observed/reproduced; the destructive code path was
   identified by inspection. The original cleanup was not run locally.
2. The persisted-secret check previously converted any file-read failure into an
   empty string and passed. It now requires a real nonempty readable stored file.
3. Only the system splash had a timeout. A silent preferences Flow left Compose
   preparing forever. The initial snapshot now resolves within three seconds to
   the existing degraded Welcome state, without resetting data or accepting setup.
   Cancellation/errors remain distinct; subsequent onboarding must explicitly
   navigate after acceptance rather than treating launch as an ongoing home feed.
4. Device tests now wait for asynchronous destination resolution and call activity
   recreation what it is, **not** process death. They compare persisted launch state
   across recreation and capture a real framework screenshot for collection.

## Reproducible local evidence

- Standalone compiler/JUnit run of the same new `ReadLaunchSnapshot` production/test
  sources: **9/9 passed** (`OK (9 tests)`, 0.162s). Kotlin 2.1.20, JDK 17.0.13,
  JUnit 4.13.2, compiler-bundled coroutines **1.8.0**. This is not the Gradle app's
  pinned Kotlin 2.3.21/coroutines 1.11.0 suite or Android verification.
- `bash -n tools/run_android_device_checks.sh`: passed.
- `python3 tools/test_android_device_runner.py`: seven stub-command exit/evidence
  cases; verifies test failure precedence, collection failure, missing screenshots
  and invalid package rejection. **Not an emulator run.**
- `git diff --check`: passed. No permission, asset, UI layout, dependency, workflow,
  gateway, account or destructive user-data change.

## CI/runtime integration still required

Main's current workflow compiles instrumentation but does not execute an emulator.
No change to `.github/workflows/*` is included because the connected publisher
cannot publish workflow edits. The executable collector is ready for an owner-managed
workflow; invoke **one** command inside the emulator action:

```yaml
script: bash tools/run_android_device_checks.sh
```

It defaults to main's `com.kx.vpn`. A historical branch using another package must
set `KX_APP_ID` explicitly. Upload `app/build/device-evidence/` plus the normal
`app/build/reports/androidTests/` and `app/build/outputs/androidTest-results/`.
Screenshots are required by the collector; a missing directory is a failed evidence
collection, not silent success. Test exits take precedence over collector exits.
This helper does not install or provision an emulator by itself.

Exact-SHA CI, emulator runs against the **integrated application**, visual comparison,
TalkBack, genuine process-kill recovery and review/merge/post-merge checks remain
required. Do not advance to TASK_01 on the strength of the other candidate's device
run or a new CI job merely starting. Physical-device/release verification remains
outstanding. Phone Repair Lab scope is recorded separately; TASK_11–21 NOT_STARTED.

## Exact-SHA CI feedback and correction

Run https://github.com/daynightae-cmyk/KX/actions/runs/41603415089 on
`a70a24a4218f7a4c81d6fb141c4d0badbcfd708d` executed **44 tests: 43 passed, 1 failed**.
The new real-DataStore sibling-sentinel cleanup regression and stored-file inspection
both passed. All existing render tests plus the new empty-store navigation test passed.

The sole failure was the new error-propagation test's `assertSame` on IOException.
Coroutine stack-trace recovery can copy the exception across a suspension boundary.
This was reproduced locally with the unchanged failing test using
`java -Dkotlinx.coroutines.debug=on ... org.junit.runner.JUnitCore ...`: 8 passed,
1 failed, `expected same IOException ... was not IOException ...` with the same message.
It is an identity assumption in the test, not a swallowed-error bug in production.
The test now still requires an actual thrown failure of the same class/message (never
a degraded return value) and passes in both normal and debug/recovery-enabled local
runs, 9/9 each. No production error handling, Lint or CI gate was relaxed. A fresh
exact-head CI run remains required; the failed run did not reach Lint/assembly.
