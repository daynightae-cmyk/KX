# Android CI handoff

`android.yml.example` is an **inactive template**, not a running GitHub Actions workflow. The connected publisher cannot create or modify `.github/workflows/*`; publication returned `workflow_file_not_allowed` (HTTP 403) and explicitly confirmed nothing was published.

The repository owner can copy this file to `.github/workflows/android.yml` using their normal repository editing workflow. No API key, replacement token or installation-permission change is needed or requested. Once installed, inspect build/lint/test and emulator results for the exact PR HEAD before declaring TASK_00 verified. Do not bypass the checks to advance the task queue.
