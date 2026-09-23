# D1 Batch37 - achievements, progression bindings and travel

Completed in code on 2026-09-20 UTC. Authorized Batch37 only; STOP before38.
Parent: 2a4f5a3f6a0605b128808037daec2aafa140e535.
Branch: feature/d1-canon-config-20260916. Final local commit follows validation and these notes.
This is an implementation checkpoint, not licensed gameplay acceptance or deployment.

## Changes

- B37-1: Stairway now awards the ordinary Elytra from the authored World Spawn chest.
  MASTER Achievements!B18/C18 and Q&A D76 correct the old active-D1-only trigger.
  New schema2 receipts explicitly use run0/Overworld. Old schema1 positive-run receipts and
  already-earned legacy advancements prevent duplicate delivery. Full inventory can retry.
- B37-2: Wolves in Piglin Clothing means six distinct characters wearing vanilla Piglin Heads
  simultaneously at authored Camp4, per MASTER B20/C20. The linked six-wolves text is flavor.
  A bounded roster sample grants the existing per-run/UUID entitlement machinery; no accumulated
  head count, wolf-cap change, custom equipment or new texture. New name-only advancement is datagen.
- B37-3: Template and physical rifts revalidate active ownership, class, exited membership,
  startup/sealed outcomes, transaction holds and outside/orphan Chop escrow. Normal travel cannot
  enter from outside with the wrong inventory or escape D1 around the Chop/cleanup journals.
  Explicit known Main Village aliases require personal access; developer authoring access remains.
- B37-4: D1 reset rifts let the existing saved cleanup own the exit before any tile teleport.
  RESETTING members remain under lifecycle recovery, even before per-owner handoff creation.
  Denied attempts are throttled on the Overworld clock; logout/stop clear the transient map.
  The Village helper uses the router's resolved physical destination, not the unvalidated template.
- B37-5: Companionship selection belongs to its original run and global server-clock window.
  Both endpoints must still be active, healthy participants inside that instance with settled
  custody. Arrival uses a bounded loaded-chunk safe-position search and player collision check.
  Success consumes selection; failed targets/cancelled travel never remove the paid cooldown.
  Logout, death, clone, dimension change and server stop invalidate transient selections.
  The legacy item remains registered; no undocumented recipe/unlock or future waypoint is added.

[Binding checklist](D1_BATCH_37_BINDINGS.md) covers source locations, actual block/region identity,
keys, 36 startup paste requests and every native acceptance item. Existing chest quantities,
schematics, spawners, locks, rifts, inventories and world placements were not edited.

## Authority and configuration

Direct Cameron instructions > current Q&A > retained MASTER mechanics and newest applicable linked
Docs. All20 relevant Doc metadata checks were unchanged; bodies were reviewed for this scope.
Private SOURCE_REFRESH.json preserves fetch status; SOURCE_REVIEW.json separately records meaning,
workbook hashes and choices. Story Template's pageBreak warning is layout only.

CosmicDungeon.config adds five server settings:

| Section/key | Default | Unit/purpose |
| --- | --- | --- |
| Achievements.piglinHeadPlayers | 6 | Distinct simultaneous characters, range1..6 |
| Achievements.piglinHeadPollTicks | 20 | One bounded roster sample per active run per interval |
| SharedTravel.companionshipSeconds | 300 | Existing five-minute cooldown/selection default |
| SharedTravel.riftCooldownTicks | 12 | Existing successful rift cooldown |
| SharedTravel.riftRetryTicks | 40 | Denied/unsafe attempt delay, minimum20 ticks |

Other gameplay config values and all_vendors_prices.config are unchanged. Generated examples
include defaults; active server configs were not opened or edited.
Current bell-window default remains two seconds with its existing documented D70 alternative.
Q&A's six physical Blooms/Watson success rule overrides the older three/six unlock split.

## Validation

Java21: gradlew.bat runServerData d1OfflineChecks build --offline --console=plain passed.
A final d1OfflineChecks build --offline --console=plain passed after the helper review correction.
No clean, GameTest, game client/server, deployment, push or remote-world operation was run.

- 12,932 offline checks across40 groups, up128 from Batch36.
- New:55 travel/session checks,42 Piglin/retention checks,22 old/new reward receipt checks,
  and9 config checks. Existing 4,112 Watson and683 handoff checks also pass cumulatively.
- Both .config round trips pass. Old values compare equal after removing only the five new keys.
- Native compressed NBT round trips cover old/new reward inventory-receipt pairs and Piglin
  entitlements surviving cleanup. No registry-backed live player/menu fixture is claimed.
- All1,964 source JSON parse. Datagen adds exactly one advancement JSON; its name-only display,
  vanilla Piglin Head icon, impossible manual criterion and packaged translation were checked.
- The generated advancement and CompanionshipSelection class are present in the final JAR.
- Source/workbook hashes, original generated-cache edits and all tracked files outside the
  explicit allowlist match the baseline. git diff --check passes.
- Known malformed-NBT fixture logging and existing Gradle deprecation notices are not test failures.
- The second code review tightened reset save ordering, denied-rift throttling, loaded-only
  companion search and use of the resolved Village destination. Final checks cover that code.

Candidate JAR: build/libs/cosmicdungeon-1.5.1.jar
SHA-256: f7731fdb6438b09118418de95380785f1b2b3fb69d546e8f818dc4fc55aa8181
The tracked1.5.0 JAR is preserved; candidate build output is not committed or deployed.

## Compatibility and boundaries

The existing stairway_reward_v1 player key remains. Schema1 positive-run receipts are read
without rewrite; schema2 accepts only typed run0 and minecraft:overworld. Unknown/malformed
receipts fail closed. New and old receipts share the existing same-player save/readback protocol.
No new SavedData type or format for run state, lifetime totals, escrow, spawners, doors or rifts;
the new achievement uses existing credit strings. No packet, item/block ID or mod dependency change.

Old D1 Stairway bindings remain saved and are reported as needing World Spawn rebind; no guessed
coordinate migration or world edit. Back up all world/player/config files together before upgrade.
An older jar does not understand schema2 receipts: roll back only a coherent complete backup,
not selected player/world files. Existing totals and grandfathered unlocks are retained.

All enforcement is common/server-side. Datagen loaded the common event subscribers successfully;
it does not prove runtime event ordering, cancelled native teleports, packet/menu behavior,
multiplayer correctness or performance. No extra client allocation/scan loop or heap increase.
Camp4 work is bounded to the instance roster; safe search is radius6, vertical -2..4, and skips
unloaded candidates only for companions. Existing lifecycle/rift loading behavior is preserved.
Unsafe fluids, damaging supports and border violations are rejected by the shared safety helper.

M79 is now implemented/runtime-unverified: the missing Piglin mechanic is supplied and the false
companion conflict removed. M81/M93/M101 stay partial for authored-world, legacy destination and
native save/lifecycle acceptance. Counts:46 implemented-unverified,19 partial D1,9 preserved,
27 D2+ deferred =101. This distinction is not a claim that world bindings were verified.

## Remaining licensed TEST acceptance

1. Bind the actual World Spawn uppermost chest; test old v1/new v2 receipts, full/reopen,
   double chest, another chest, reconnect/clone and native save/readback failures.
2. Author/inspect Camp4 bounds; test six real heads together and five/sequential/duplicate,
   other-instance, spectator/developer/dead/outside-escrow exclusions; reset and reload credits.
3. Test Tamsin entry, same-instance Earth/Nether rifts, locked Village aliases, raw physical
   slot targets and cross-slot attempts. Verify valid Chop round trips keep both inventories.
4. Interrupt reset saves before the decision, owner handoff and teleport; check group recovery,
   no raw tile escape/respawn overwrite, bounded denial messages and retained original belongings.
5. Use Companionship across valid instance dimensions; test offline/dead/foreign/outside targets,
   timeout, later run, logout/reconnect, invalid inventory custody, blocked/cancelled arrival.
6. Follow the [binding checklist](D1_BATCH_37_BINDINGS.md) for journals, flags, Watson, all regions,
   actual lock/key mappings, spawner presets and36 authored startup placements. Do not bulk-replace.
7. Verify per-run objectives reset while earned achievements, prior access and success-only
   lifetime totals persist. Later dungeons and Wither variants remain explicit code TODOs.

## Remaining plan and graphics

One planned batch remains:38, help/balance displays, performance bounds and cumulative licensed
TEST handoff (M09/M104/M105). Include Batch35 recipe/crafting and Batch36 combat gates.
No permission for38, game launch or deployment is inferred from this report.
A useful next improvement is to consolidate the TEST checklist with the final help/display review.

No new required PNG. Optional existing tamsin_d1_map.png:512x256, route to Base Camp, signed -JHW.
The drawn fallback is implemented.

## Exact files in this checkpoint

- src/main/java/net/goui/cosmicdungeon/Config.java
- src/main/java/net/goui/cosmicdungeon/achievement/CosmicAchievementIds.java
- src/main/java/net/goui/cosmicdungeon/achievement/d1/D1AchievementRegionService.java
- src/main/java/net/goui/cosmicdungeon/achievement/d1/D1InstanceAchievements.java
- src/main/java/net/goui/cosmicdungeon/achievement/d1/D1ObjectiveBindings.java
- src/main/java/net/goui/cosmicdungeon/achievement/d1/D1WorldAchievements.java
- src/main/java/net/goui/cosmicdungeon/achievement/d1/StairwayReward.java
- src/main/java/net/goui/cosmicdungeon/achievement/d1/D1PiglinAchievement.java
- src/main/java/net/goui/cosmicdungeon/achievement/d1/PiglinDisguiseRules.java
- src/main/java/net/goui/cosmicdungeon/block/custom/CosmicRiftTileBlock.java
- src/main/java/net/goui/cosmicdungeon/datagen/ModAdvancementProvider.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonTravelRouter.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonTravelRules.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/package-info.java
- src/main/java/net/goui/cosmicdungeon/potion/CompanionshipTeleportService.java
- src/main/java/net/goui/cosmicdungeon/potion/CompanionshipSelection.java
- src/main/java/net/goui/cosmicdungeon/potion/PotionOfCompanionshipItem.java
- src/main/java/net/goui/cosmicdungeon/rift/DefaultRiftDestinations.java
- src/main/java/net/goui/cosmicdungeon/rift/SafeTeleportUtil.java
- src/main/resources/assets/cosmicdungeon/lang/en_us.json
- src/generated/resources_server/data/cosmicdungeon/advancement/achievements/wolves_in_piglin_clothing.json
- src/test/java/net/goui/cosmicdungeon/achievement/d1/D1JournalRewardChecks.java
- src/test/java/net/goui/cosmicdungeon/achievement/d1/D1PiglinChecks.java
- src/test/java/net/goui/cosmicdungeon/dungeon/DungeonTravelChecks.java
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java
- docs/config-examples/CosmicDungeon.config
- docs/ai/D1_BATCH_37.md
- docs/ai/D1_BATCH_37_BINDINGS.md
- docs/ai/D1_REMAINING.md
- docs/releases/fragments/d1-batch37-achievements-travel.md
