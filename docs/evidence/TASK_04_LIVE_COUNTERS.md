# TASK_04 — live backend counter slice (PARTIAL / no merge)

Base `44faa61cb7564a0768dfc88a27205a6188c74d71`, branch
`agent/task04-live-counters`. PR #14 (TASK_03) was read back as merged by another
actor; latest main and integrated architecture inspected. Ledger's TASK_03 row is
stale, so it is not used as evidence that no implementation exists. No merge by this
agent; prior #13 work remains independent/unmerged. No replacement/duplicate engine.

TASK_04 reference opened, SHA-256 matched:
`f0cfe658e5f66dc9c6dbcdcd371fff0b825b2f30a32e9c089d5c811a32ffef30`.
This is a functionality slice in the existing Home metrics, **not** the full new
connected-screen design or visual acceptance.

## Observed cause and implementation

Existing Home already displays backend totalRx/totalTx when `session.stats` exists,
and the service already exposes real WireGuard counters. However nothing called
`refreshStatistics()` in production. The update also queried platform.statistics
inside StateFlow.update, which may reevaluate under contention; a late result could
attach to a disconnected/reconnected session and stale totals could remain during
transitions.

- Process-owned coroutine now samples existing backend totals every second only
  while backend state is UP, using IO dispatcher for native reads. No test packets,
  external probes, public-IP lookups, persisted traffic history or synthetic speed.
- State-driven collectLatest stops polling while DOWN/disconnecting, and cancellation
  propagates. Distinct UP state prevents counters themselves restarting the sampler.
- Read occurs once outside update lambda. Generation check rejects a result if a
  session/profile transition happened during read. Totals clear immediately on leaving
  UP; missing/throwing/negative readings show unavailable, not invented zero. Actual
  reported zero remains valid. No backend status is inferred from a missing reading.
- Five added tests cover sampling activation/frequency/stop, old-result races across
  disconnect/reconnect, missing/error/negative data and invalid frequency.

## Verification status at publication

Diff checks passed; source/API contracts inspected (repository-pinned coroutines).
The new coroutine-test suite requires app dependencies unavailable through local
Maven egress; exact-head automatic Android CI must establish compilation/unit/device
results. No local Gradle pass fabricated. No rendered UI layout, strings, permissions,
dependencies, workflow or other-agent branch changed; existing metrics become live
only when actual backend samples exist.

## Mandatory gaps — not claimed complete

No owned/authorized gateway is available here. Handshake, counter growth under an
actual controlled transfer, route/DNS/IPv4/IPv6/egress/leak and disconnect network proof
remain **BLOCKED_NETWORK_VERIFY**. FakePlatform is a test boundary, not fake production
traffic. Timer, rates, connected-screen polish, external reachability, TalkBack/visual
comparison and physical-device acceptance are not delivered by this slice. No full
TASK_04/TASK_05/TASK_06 completion, protection, anonymity or release claim. Keep draft
and **unmerged**, per owner instruction. No keys/YAML uploads requested.
