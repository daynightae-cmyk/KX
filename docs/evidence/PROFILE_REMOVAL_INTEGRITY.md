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

## Follow-up — bounded encrypted profile reads and writes

Branch baseline for this addition: published #13 `d60ccba247617ec1de3eb7e66f9910ae6b05415a`.
Live main remains `3654a0f` at inspection; TASK_03 is active in another agent's PR #14.
This addition does not touch that branch or duplicate server-selection work.

The blob store used `File.readBytes()` without a size ceiling. Vault's decrypted-size
check happened later, after the complete ciphertext was already allocated. The write
boundary likewise accepted oversized byte arrays and could replace a prior profile.
Four new boundary cases against baseline: **9 tests, 3 failures** — oversized-by-one
and 8MiB sparse blobs were returned, and an oversized write was accepted. Exact-limit
input passed. Fixtures contain no keys; no real OOM/production damage was induced.

Correction: default ciphertext ceiling **65,568 bytes** = current KXV1 header (4) +
64KiB plaintext + AES-GCM nonce (12) + tag (16), matching the inspected production
cipher. Reject oversized writes before temp creation/replacement; reject oversized
files without modifying/deleting them. Read length is only a fast check; streamed
reads enforce the cap and consume at most one extra byte to distinguish exact-limit
EOF from growth. A pathological zero-length bulk read uses a one-byte progress fallback.
IO/security failures preserve the nullable-read contract, not a fake valid profile.

An explicit constructor limit supports other reviewed envelope sizes; it must be
positive and at most 1MiB. Current production callers use the default. This is not a
new parser, encryption protocol, UI error taxonomy or unrestricted large-profile mode.
Future cipher/header/quota changes must revise the envelope contract deliberately.
Empty or corrupt under-limit data still passes to the existing vault validator and
is not claimed valid by the file store. Slow/blocking IO is not time-bounded by this
byte ceiling. No symlink/hostile same-UID isolation or security audit claim.

Added eight tests (13 total file-store cases): exact-boundary read; oversized rejection
without deletion; sparse large file rejection; prior-profile preservation on oversized
write; growing stream stops after cap+1; zero-read progress; empty/short byte fidelity;
invalid-limit rejection before IO/directory creation. **13/13 pass** with the same
source under standalone Kotlin2.1.20/JDK17/JUnit4.13.2. No Android/dependency APIs were
introduced. Exact-head full Android CI is still required and will be inspected before
calling those checks verified. No UI/screenshot/handshake or new roadmap task claim.

No merge, workflow edits, history rewrite, unrelated-file cleanup or other-agent
branch changes. Original #11 work remains preserved, not force-merged into the
independently integrated app. This follow-up is narrow data-boundary hardening.

## Follow-up — no in-place write fallback

Inspected #13 baseline `0dd21030a42ffb32cd8b521f556e9b6b084c7aa3`: a failed temporary
rename fell back to `target.writeBytes(bytes)`, truncating the previous encrypted blob
before the replacement was complete. That contradicted the comment promising crash-
safe publication. No real data loss/crash was observed or induced; cause identified
from the exact write path.

The writer now syncs its owned temporary ciphertext then publishes via same-directory
`Files.move(ATOMIC_MOVE, REPLACE_EXISTING)` (available on supported API26+). If the
filesystem cannot perform atomic publication, it fails rather than overwriting the
original in place. A finally block only attempts cleanup of its own temporary file;
a failed write must not delete a prior profile or recurse into directories.

Three additional tests bring the focused suite to **16 passed locally** (same pure
JVM source, Kotlin2.1.20/JDK17/JUnit4.13.2). They exercise injected atomic-publication
failure with old-byte/non-target preservation, real complete repeated replacement,
and temporary-write failure without target modification. Test injection affects only
the internal helper; production always calls the real NIO atomic move. Added an
Android-device test on a unique nonsecret cache fixture to verify the actual Android
filesystem accepts complete atomic replacement; device result pending exact-head CI.

Limits: fsync covers file content, not directory power-loss durability; there is no
hard power-cut proof or concurrent-writer/symlink/hostile same-UID guarantee. A storage
provider without atomic replacement is intentionally rejected. No UI, import format,
permission, dependency, workflow, transport or other-agent branch changed. No merge.
