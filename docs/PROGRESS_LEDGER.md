# KX VPN — immutable evidence-based progress ledger

The initial planning commit contained only references/documents. This isolated branch adds a TASK_00 implementation candidate and tests/CI definitions. It is **not** a completed task, verified APK or VPN service. TASK_01–10 remain NOT_STARTED. See [primary evidence](evidence/TASK_00.md).

| Task | Status | Implementation SHA | PR | Merge SHA | Test/device proof |
| --- | --- | --- | --- | --- | --- |
| TASK_00 | IN_PROGRESS / acceptance BLOCKED | see draft PR implementation commit | pending publication | none | 9 standalone domain tests passed; Android build/device/visual NOT_VERIFIED |
| TASK_01 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_02 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_03 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_04 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_05 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_06 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_07 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_08 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_09 | NOT_STARTED | none | none | none | NOT_RUN |
| TASK_10 | NOT_STARTED | none | none | none | NOT_RUN |

Valid statuses: NOT_STARTED, IN_PROGRESS, LOCALLY_VERIFIED, DEVICE_UNVERIFIED, BLOCKED_EXTERNAL, PR_OPEN, MERGED_VERIFIED, RELEASE_VERIFIED. Never mark MERGED_VERIFIED without remote main SHA and CI/evidence. Never mark RELEASE_VERIFIED without actual Android device security+network verification and distribution checks.

## External deferrals
A task with a demonstrably complete, tested and merged provider-free supported scope may be `MERGED_VERIFIED` **only for that expressly limited scope**; list all optional `DEFERRED_EXTERNAL` features and their failures in `docs/BLOCKERS.md` and the task evidence. An external deferral is never a success claim about that feature.
