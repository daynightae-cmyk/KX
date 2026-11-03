# Integrated profile-store removal integrity — PARTIAL, no merge

Baseline: `3654a0ff788ead2735b0d22ab997f36d25d0a1ad` (other actor's integrated
TASK_02 from PR #12). Branch `agent/profile-vault-removal-integrity`.

The prior alternative TASK_02 branch/#11 is retained with its unique cancellation
fixes and confirmed-removal UI. It now conflicts with main's independently integrated
engine, so Actions do not start on its latest commit. Do not force that parallel
application over main or call its new deletion UI device-verified. No merge was
performed by this agent; this patch preserves the integrated architecture.

Inspected integrated FileProfileBlobStore: read/delete did not validate IDs; clearAll
attempted every directory entry (including unrelated files), and delete/clearAll
silently ignored deletion failures. Tests with isolated nonsecret temporary fixtures
reproduced these boundaries: **5 tests, 3 failures** against exact baseline source
(invalid IDs accepted; unrelated notes deleted; unsuccessful deletion reported no
error). No real user-data loss or external exploitation is claimed observed.

Narrow correction: validate profile IDs before every read/write/delete, remove only
validated store-owned profile/temp names, preserve unrelated entries, refuse recursive
directory deletion, and throw on unsuccessful or unconfirmed removal. Thus callers
cannot mistake failed removal for success; eraseEverything does not destroy its key
after an unconfirmed clearAll. This does not promise physical secure erasure, all-or-
nothing batch deletion, symlink/hostile same-UID isolation or a complete security audit.

Same five tests with corrected source: **5 passed** using standalone Kotlin2.1.20,
JDK17, JUnit4.13.2 (pure file-store code, no Android types). Cases: exact profile+temp
removal, idempotent missing removal, invalid-ID rejection, unrelated file/directory
preservation, and failed directory-as-profile removal. Only directories created by the
tests are cleaned. No dependencies/workflow/UI/permission or engine changes.

Exact-head automatic Android CI remains required; local tests do not equal runtime
or production acceptance. No controlled gateway/handshake/route/DNS/leak/physical-
device claim. No merge, forced history update or deletion of another agent's work.
