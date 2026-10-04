# Testing batch 11 — lifetime statistics and server-wide leaderboards

Base: Batch 10 / c3b7442ba005c02cd6f48850d193d1faf90fd742 / draft PR209.
Branch: feature/testing-fixes-batch11-20261004. Three approved action items implemented.
**0 implementation batches remain.** Cumulative licensed gameplay acceptance is separate.
[Approved rollout](TESTING_ROLLOUT_20261003.md) | [Task card](tasks/testing-fixes-batch11-20261004.md).

## Behavior

The pause menu has a native, full-width Leaderboard button directly below Disconnect,
with tiled vanilla stone tinted teal and the native focus border. Cameron's 2026-10-04
follow-up replaces the exhaustive registry catalog with eight curated statistics:

1. Dungeons completed (the default).
2. Cosmic mob spawners broken.
3. Mobs killed.
4. Death count.
5. Blocks traveled.
6. Doors unlocked.
7. Lesser Blooms harvested.
8. Time played.

The statistic and ranking lists scroll independently; small-screen statistic arrows only
scroll these eight choices. Search and statistic catalog pagination are removed. Player
rank paging, Refresh, Done, selection, narration and full-value tooltips remain. Separate
name/score columns protect score visibility. Time played uses native time formatting.
The same eight keys form the server allowlist; clients cannot request hidden categories.

Blocks traveled sums all sixteen native movement-distance counters, including walking,
sprinting, swimming, falling, flying and riding, then converts centimeters to whole blocks.
Live and offline values share the same conversion; fractional centimeters are combined
before rounding. This measures native recorded distance, not displacement or teleports.
Spawner breaks, mob kills and deaths keep native counting semantics, including creative-mode
mining exceptions. Completions reuse existing successful-run totals, not attempts or tier awards.

Existing Minecraft statistics remain authoritative and are never reset by this feature.
Live players use their server counter, including not-yet-saved changes; offline players
use native UUID statistics files and Minecraft's version fixer. Both failed and forfeited
run activity therefore remains available. Custom successful-action counters record doors
unlocked, attributed hostile kills, actual Lesser Bloom harvests, logins and dimension
changes. Hostile credit reuses the existing direct/companion/environmental credit resolver
and records one death once. Lesser harvests count the actor, separately from shared rewards.

Custom counters begin when this update observes events; unavailable history is not invented.
The additional underlying counters remain saved but are not exposed in the curated menu.
Existing recovered Spectral Blooms, retained Lesser Blooms, completions and successful-run
kills remain separate stored totals with unchanged reward semantics.
UUID is the identity throughout. Online names, saved last-known names and native cached UUID
names are used; unknown historical names display a short UUID with full UUID in the tooltip.
The cache lookup makes no external name-resolution request.

## Persistence, networking and cost

Optional activity data extends the existing cosmicdungeon_d1_lifetime_v1 SavedData with
last-known names and nonnegative saturating long counters. Old players, totals and Watson
receipts remain intact; unknown saved activity keys round-trip. Custom activity is marked
dirty on change and saved by normal world saving, independently from run cleanup. It is
not a crash-proof event ledger: abrupt process/storage failure can lose unsaved activity.
No second native-statistics store or SavedData subsystem was added. Native files are read
only and not rewritten, migrated or repaired by leaderboard queries.

Protocol 12 becomes 13; matching client/server jars are required. Clients submit only
bounded metric/filter/cursor requests, never scores or player data changes. Server metric
allowlisting, 250 ms per-player admission, 12-row packets and bounded decoder lists apply.
The client debounces requests, ignores stale request IDs and exposes read timeouts.
The screen shows cached data up to 30 seconds old. Rankings tie-break by UUID. Live changes
can alter positions between page requests; Refresh and returning to the first page obtain
a newer view. This is not a frozen multi-page historical snapshot.

One background archive job per server, with no queued backlog, keeps disk/JSON work off
the tick thread. Bounds are 4 MiB per file, 64 MiB per query, 10,000 directory entries and
a 10-second cooperative budget checked between files. A single parse may finish beyond
the time budget. A 12-entry ranking heap retains one page; at most 32 pages are cached.
Main-thread snapshots copy only lifetime owner rows and current online values, never
world blocks/entities. Work scales with recorded players, with the archive limits above.
Unreadable files/limits display an incomplete-archive status and no misleading partial
ranking. The worker stops with the server; logout clears request admission state.
No per-tick disk read, world scan, external service, new dependency or heap change was added.
Live multiplayer tick/frame cost remains unmeasured.

Authored stacks, account balances, transaction receipts, progression rewards, spawner
data/presets, registry IDs and access policy remain unchanged. No manual spawner migration
is required. Take the usual stopped-world backup before first use. Rolling back to an older
jar can discard newly saved activity fields on its next save; use the matching world backup
to preserve this history and honor the earlier batches' rollback boundaries.

## Verification

Java 21 test/build passed: 216 native NeoForge JUnit tests, zero failures/errors/skips.
The curated follow-up adds completion default/receipt retention, live/offline travel parity,
and rounding/malformed-distance checks; the catalog test now enforces the approved eight keys.
The 18 new tests cover old saves; failed/successful outcome isolation; UUID/name retention;
saturation and unknown keys; actual native counter aggregation; curated catalog coverage;
archive values, versioned read-only files, malformed/oversized input; stable tied cursor pages;
extreme scores; time/distance formatting; packet round trips/limits; minimum GUI geometry.
The native PauseScreen class also loads successfully through the new Mixin transformation
in the native test environment, without opening a world.
d1OfflineChecks passed, including 129 config checks and two configuration round trips.
Incidental config-example ordering was restored to its verified pre-run bytes.
All source JSON, changed-document links and scoped whitespace are checked.

Datagen is not applicable: only the hand-authored mixin manifest changed in resources.
No destructive clean, GameTest/dedicated-server launch, world entry or authored item edits.
AGENTS.md required no change. Existing unrelated historical-JAR deletion and generated-cache
edits remain excluded from this task's Git checkpoint.

Batch 10 source matched origin and fresh installed-client/TEST hashes both matched
1065277E98175235D2E131D11053D5B8C6472AD451AFAFAC799D0342A86152C6 before replacement.
The post-commit build/deployment/current-test/updater/client-startup receipt is recorded
outside Git in CosmicDungeon_AI/backups/testing-batch11-20261004/final-handoff.json
only after those operational checks complete. TEST is not restarted automatically.
Historical completed staging diffs are archived losslessly with hash/path receipts to
retain deployment headroom; active rollback JARs and authoritative source are preserved.

## Pending licensed TEST acceptance

1. Pause at small/large GUI scales: Leaderboard sits immediately below Disconnect, uses
   teal stone styling, opens to Dungeons completed and offers exactly the eight listed stats,
   supports scroll/selection/keyboard focus, and keeps long names/scores clear of navigation.
2. Break a Cosmic Spawner, travel, unlock a door, harvest a Lesser Bloom and kill mobs;
   fail/forfeit the run. Refresh after the cache window: observed totals remain, while
   successful-run reward metrics do not gain a false success. Repeat with two independent runs.
3. Compare online and offline players, ties and later pages; reconnect and restart TEST
   under the normal controlled process. Known history persists, names resolve locally,
   and corrupt/unreadable archive input produces a visible error rather than a partial board.
   Check mixed movement distances in whole blocks and native death/time counters.

Cumulative gameplay QA from Batches 1–10 remains pending. These are test tasks, not questions.
Possible future improvement: an authorized multiplayer archive/restart acceptance fixture.

## Exact files (25)

- [docs/ai/D1_REMAINING.md](D1_REMAINING.md)
- [docs/ai/TESTING_BATCH_11_20261004.md](TESTING_BATCH_11_20261004.md)
- [docs/ai/TESTING_ROLLOUT_20261003.md](TESTING_ROLLOUT_20261003.md)
- [docs/ai/tasks/testing-fixes-batch11-20261004.md](tasks/testing-fixes-batch11-20261004.md)
- [docs/releases/fragments/testing-fixes-batch11-20261004.md](../../docs/releases/fragments/testing-fixes-batch11-20261004.md)
- [src/main/java/net/goui/cosmicdungeon/client/ModNetworkClient.java](../../src/main/java/net/goui/cosmicdungeon/client/ModNetworkClient.java)
- [src/main/java/net/goui/cosmicdungeon/client/screen/LeaderboardRowButton.java](../../src/main/java/net/goui/cosmicdungeon/client/screen/LeaderboardRowButton.java)
- [src/main/java/net/goui/cosmicdungeon/client/screen/LeaderboardScreen.java](../../src/main/java/net/goui/cosmicdungeon/client/screen/LeaderboardScreen.java)
- [src/main/java/net/goui/cosmicdungeon/client/screen/TealLeaderboardButton.java](../../src/main/java/net/goui/cosmicdungeon/client/screen/TealLeaderboardButton.java)
- [src/main/java/net/goui/cosmicdungeon/door/DoorLockHandler.java](../../src/main/java/net/goui/cosmicdungeon/door/DoorLockHandler.java)
- [src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1KillStatistics.java](../../src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1KillStatistics.java)
- [src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1LesserBloomEvents.java](../../src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1LesserBloomEvents.java)
- [src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1LifetimeData.java](../../src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1LifetimeData.java)
- [src/main/java/net/goui/cosmicdungeon/leaderboard/LeaderboardMetrics.java](../../src/main/java/net/goui/cosmicdungeon/leaderboard/LeaderboardMetrics.java)
- [src/main/java/net/goui/cosmicdungeon/leaderboard/LeaderboardRanking.java](../../src/main/java/net/goui/cosmicdungeon/leaderboard/LeaderboardRanking.java)
- [src/main/java/net/goui/cosmicdungeon/leaderboard/LeaderboardService.java](../../src/main/java/net/goui/cosmicdungeon/leaderboard/LeaderboardService.java)
- [src/main/java/net/goui/cosmicdungeon/leaderboard/LifetimeActivity.java](../../src/main/java/net/goui/cosmicdungeon/leaderboard/LifetimeActivity.java)
- [src/main/java/net/goui/cosmicdungeon/leaderboard/LifetimeEvents.java](../../src/main/java/net/goui/cosmicdungeon/leaderboard/LifetimeEvents.java)
- [src/main/java/net/goui/cosmicdungeon/leaderboard/StatisticsArchive.java](../../src/main/java/net/goui/cosmicdungeon/leaderboard/StatisticsArchive.java)
- [src/main/java/net/goui/cosmicdungeon/mixin/StatsCounterAccess.java](../../src/main/java/net/goui/cosmicdungeon/mixin/StatsCounterAccess.java)
- [src/main/java/net/goui/cosmicdungeon/mixin/client/LeaderboardPauseMixin.java](../../src/main/java/net/goui/cosmicdungeon/mixin/client/LeaderboardPauseMixin.java)
- [src/main/java/net/goui/cosmicdungeon/network/LeaderboardPayloads.java](../../src/main/java/net/goui/cosmicdungeon/network/LeaderboardPayloads.java)
- [src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java](../../src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java)
- [src/main/resources/cosmicdungeon.mixins.json](../../src/main/resources/cosmicdungeon.mixins.json)
- [src/test/java/net/goui/cosmicdungeon/leaderboard/LeaderboardTest.java](../../src/test/java/net/goui/cosmicdungeon/leaderboard/LeaderboardTest.java)
