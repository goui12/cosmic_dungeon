# Testing fixes, batch 2 — implementation report

Branch feature/testing-fixes-batch2-20261003; base ecd24c00 / draft PR200.
This pass adds exactly three requested changes. Combined TEST handoff includes batch 1.

## Changes

- Trade now reads Their Offer, My Offer, My Balance, My Inventory. Both offer panels,
  slots, currency icons, status text and accept/cancel hitboxes move together. The exact
  inventory account renderer occupies the reserved middle row. Styling, denomination
  integers, hover details and trade transactions remain.
- Unfinished advancement descriptions show ???????? in the details panel, hover tooltip
  and accessible button narration. Server-supplied completion reveals the real description
  and rebuilds widgets. Names, icons, layout, progress and award rules remain.
- Forfeit is available below the native dungeon death-menu buttons. Explicit confirmation
  opens/views the existing ballot and votes Yes using existing ordered /ff commands.
  Server membership, two-thirds threshold, expiry and duplicate-vote rules remain.
  Failed-run cleanup automatically respawns connected dead members at the normal safe
  outside destination before the existing pre-entry inventory handoff. It never briefly
  respawns them at the hostile dungeon bed. Ordinary active-run deaths still respawn normally.

## Validation

Java 21.0.12.101 build passed; 90 JUnit tests passed with zero failures, errors or skipped
tests. Updated layout separation checks cover the added balance row; the new respawn gate
regression covers active/ending, living/dead and connected/disconnected states. Existing
ballot consent/expiry/isolation and native inventory-handoff interruption tests also passed.
The full existing d1OfflineChecks task passed during batch 1; batch 2's build reran its
relevant native JUnit coverage. All 1,997 source JSON files parse; scoped diff checks pass.
Compiler deprecation notes are warnings, not build failures.

No datagen applies to Java presentation/lifecycle changes. No destructive clean, GameTest
or dedicated-server launch was performed. Native development-client startup and installed
TEST/client replacement have separate receipts in CosmicDungeon_AI; the final handoff
reports their actual results. No gameplay or visual acceptance is claimed from compilation.

## Persistence and integration

No save-format/schema, registry, packet or dependency change. Existing saved run decisions,
escrow, receipts, offline recovery and transaction gates remain authoritative. Failed-run
respawn requires the reset decision to be flushed and other custody to be resolved.
Native PlayerList respawn handles cloning/synchronization; its connection is rebound exactly
as in the native respawn command handler. Cleanup then restores the recorded outside items.
The safe outside respawn configuration matches existing successful cleanup behavior.
A missing safe position or unresolved custody holds cleanup for the existing reset retry.

No authored chest items, world/template blocks, spawner settings, doors/keys, rifts, class
balance, currency quantities, active configs or server.properties were changed.
The UI remains client-only; the server alone decides votes, respawn and inventory recovery.
Work is event-driven and bounded by the current run roster; no added polling loop or watcher.

## Licensed TEST acceptance

1. Trade with another player: confirm the four-section order; click each offered denomination,
   shift-click items and check accept/cancel and hover regions at minimum and normal GUI scales.
2. Open a locked advancement: details, hover and narration show ????????; earn it and confirm
   the actual description appears without reopening. Existing earned records stay readable.
3. Die alone in an instance: Forfeit, Yes. Return alive outside with exact pre-entry belongings;
   dungeon loot is discarded and the instance follows its normal failed-run cleanup.
4. Repeat with mixed living/dead and all-dead parties. The existing two-thirds vote is required.
   Reject/let a vote expire: no automatic respawn or inventory restoration should occur.
5. Recheck ordinary death/respawn, retained quest items, reconnect, and paused recovery safety.
   Connected-member automatic recovery is implemented; disconnected/restart behavior still
   requires licensed regression testing through the existing durable handoff paths.

Nine implementation batches remain: spawners; Tamsin group/class controls; capacity/group HUD;
difficulty; skills; mercenary hiring/equipment; mercenary AI/brewing; mercenary HUD/respawn;
leaderboards. [Full queue](TESTING_ROLLOUT_20261003.md). Cumulative licensed testing is separate.

Possible future improvement: show the live forfeit tally directly on the death screen.

## Exact changed files

- docs/ai/D1_REMAINING.md
- docs/ai/TESTING_BATCH_2_20261003.md
- docs/ai/TESTING_ROLLOUT_20261003.md
- docs/ai/tasks/testing-fixes-batch2-20261003.md
- docs/releases/fragments/testing-fixes-batch2-20261003.md
- src/main/java/net/goui/cosmicdungeon/client/DungeonDeathScreen.java
- src/main/java/net/goui/cosmicdungeon/client/advancements/CosmicAdvancementScreen.java
- src/main/java/net/goui/cosmicdungeon/client/screen/TradeScreen.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonDeathRecovery.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonForfeitService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleService.java
- src/main/java/net/goui/cosmicdungeon/trade/TradeScreenLayout.java
- src/test/java/net/goui/cosmicdungeon/dungeon/DungeonForfeitTest.java
- src/test/java/net/goui/cosmicdungeon/trade/TradeScreenLayoutTest.java
