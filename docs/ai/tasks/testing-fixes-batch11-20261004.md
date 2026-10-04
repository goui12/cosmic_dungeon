# Batch 11: server lifetime statistics and leaderboards

Authorized by Cameron 2026-10-04 03:14 PDT. Base Batch10 / c3b7442ba005c02cd6f48850d193d1faf90fd742 / PR209. One branch and draft PR; final planned implementation batch.

Three actions: retain server-wide native/custom activity including failed/forfeited runs; teal stone-texture Leaderboard button directly below Disconnect; searchable/selectable and scrollable metric/ranking pages. Preserve known history without inventing unrecorded events.

Reuse native stats files/counters and D1LifetimeData. Optional UUID-keyed activity/name fields preserve old totals and Watson receipts. New custom counters start when observed; existing success-only totals remain explicitly labeled. A bounded background reader handles offline archive queries; no world scanning, per-tick I/O, new persistence subsystem or network-supplied values. Server packets validate keys/cursors/pages, rate-limit and return bounded pages; client rejects stale replies.

Single-writer hotspots: D1LifetimeData codec, successful door/bloom hooks, ModNetwork/client dispatch and new payload codecs, stats-map accessor and client pause-menu injection plus mixin manifest. Expected leaderboard service/rules/screen, tests, relevant docs/release fragment. No spawner NBT/preset changes, authored stack changes, account rewrites or generated resources.

Verify Batch10 provenance; Java21 tests/build, old-save and native codec round trips, activity/outcome independence, native archive parsing, ranking ties/cursors, pagination and network bounds, UI geometry; all src JSON/doc links/diff checks. No clean, dedicated/GameTest launch or agent world entry. Scoped commit/push/draft PR, post-commit build, stopped TEST/closed-client deployment, current-test/updater verification, fresh development-client handoff. Review512MiB audit retention before deployment without losing rollback/source bytes.

## October 4 curated leaderboard correction

Cameron superseded the exhaustive catalog with five named statistics plus a few useful extras.
Expose Dungeons completed, Cosmic mob spawners broken, Mobs killed, Death count, Blocks traveled,
Doors unlocked, Lesser Blooms harvested and Time played. The last three are implementation choices,
not separately answered questions. Remove search/catalog pages; retain small-screen list scrolling
and player rank pages. Server allowlist uses exactly these eight entries; default is completions.
Travel aggregates native movement modes into whole blocks equally for live/offline history.
No persistence, registry, packet-format or migration change. Existing collection remains intact.
Files: LeaderboardMetrics, StatisticsArchive, LeaderboardScreen, LeaderboardTest and related
Batch11 docs/rollout/release fragment. Java21 build and216 native tests pass. Live UI acceptance
and deployment await the active client closing and TEST stopping. Keep this correction on PR210.
