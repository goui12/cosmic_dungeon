# Batch22 - Player Theurgist crafting and resurrection

Approved card22 only. Branch feature/player-theurgist-actions-20261007,1.6.12-alpha.1.
Baseline Batch21 a039d8a633a44a1f67efc4f938f962583de85ce4.
Stop after CurseForge publication and stopped TEST sFTP; do not begin23.

## Behavior and boundaries

Skills produces one physical native splash potion for20 Brewing Supplies (normal)
or40 (epic), without stand/bottle/fuel requirements. Normal pool: night vision,
invisibility, fire resistance, swiftness, healing, regeneration, strength, luck.
Epic pool: only native tierII swiftness/healing/regeneration/strength.
A living actual Theurgist in the active run needs a free main/hotbar slot, native
inventory menu, empty cursor, resource affordability and clean transaction state.
Delivery and debit share one verified native owner save. Failed staging restores
both; uncertain saves retain the paired state and disconnect for authoritative reload.
Full inventory creates no dropped item and spends nothing.

Existing splash/cloud effect wrappers enforce player throw-time Theurgist attribution.
A compact additive entity marker freezes both Theurgist and non-Theurgist throws,
copies to lingering clouds and survives owner absence/save-load. Existing unmarked
projectiles read existing progression provenance/native owner without rewriting saves.
Positive effects cannot benefit/harm hostile mobs (including undead heal inversion);
known allied undead cannot be hurt by a beneficial effect. Allies, pets, mercenaries
and neutral native behavior remain. Manual negative brewing and other classes keep
their existing behavior. No authored stack, chest, tag or loadout changes.

On latest real uncancelled death, living same-run Theurgists are notified to open
inventory. Each dead teammate has a Skills button; empty lists show disabled
Resurrect..., and affordability/current eligibility determine enabled state. One
explicit offer per target binds caster, active run and latest-death UUID. A second
caster cannot replace it; explicit player offers suppress competing mercenary offers.
Death screen Accept/Decline binds the displayed offer. Stale generations, duplicate
acceptance, old player objects, dead/offline/wrong-run casters, insufficient resources,
forfeit/completion, ordinary respawn and cleanup cannot authorize revival. Decline
costs nothing. Player resurrection has no cooldown or additional skill-level gate.

Successful acceptance uses the existing native respawn engine, exact latest-death
position/rotation and five-second protection, retaining native clone/connection and
physical-drop layout behavior. Shared latest-death capture now covers every active
dungeon; mercenary availability remains D1-only with its existing Positive Potions10
and three-minute cooldown. No inventory is recreated, no old physical drops are
reclaimed, and previous bed/anchor configuration is preserved.

## Persistence and recovery

Potion creation uses the existing additive class-resource ledger with no schema change.
Pending accepted player revivals use bounded schema1 SavedData
cosmicdungeon_player_revivals_v1 plus player-root theurgist_revival_reservation_v1
and compact theurgist_revival_receipt_v1. Missing keys/file are supported old saves;
unknown existing fields are copied. Malformed/future data fail closed and remain intact.
No spawner, registry ID, unloaded-chunk migration or replacement resource store exists.

The world prepare and both owner reservations are proved before claiming the shared
death token. No resource debit occurs yet. A hook immediately before native respawn
event dispatch records positioned living-target success, before any save-capable
listener; the replacement location/root then receive native save proof. The world
outcome is persisted before the caster's exact120 debit/receipt and both acknowledgements.
Completed plans retire their images/indexes; already receipted owners never pay twice.
A durable target success receipt recovers the debit after interruption; missing success
cancels incomplete preparation without cost. Uncertain writes retain reservations,
freeze conflicting actions/cleanup and disconnect rather than guessing/refunding.
If the caster reconnects before an undecided target, their actions stay blocked until
the target's authoritative player load resolves the outcome; no background offline
save editing occurs. Invalid/mismatched proofs require save review.

Before intentional rollback, settle all pending transfers and back up world/player
files together. Older releases ignore additive records and lack their guards; rollback
across pending revival decisions is unsupported. Completed operations need no migration.
Do not delete pending records or restore only one participant. All GUI capability
flags remain advisory; the server validates execution.

## Validation and performance

Java21 full local Gradle build, source JSON/diff checks, full clean Integration Gate
and release native GameTests are required. No datagen inputs/resources changed.
Five new native fixtures exercise exact crafting/full-inventory native save pairing,
accepted resurrection/competition/latest-death/no-cooldown/protection, durable
success/cancel recovery, real splash/lingering effects and saved cloud attribution.
Unit coverage includes exact pools, filtering, strict pending journal schemas/debits,
copy/unknown-field retention, bounded codecs and authoritative client consent state.
All previous native IDs remain; actual counts/outcomes are recorded in BatchRunner
receipts rather than pre-claimed here.

Capabilities synchronize once per second with deltas; lifecycle/actions acknowledge
immediately. Bounded target lists, one ephemeral offer per target,5-tick action gates,
cached native SavedData and per-effect local checks avoid new scans/pathfinding or
runtime dependencies. Persistent I/O occurs only at requested actions/recovery.
Crash fixtures simulate durable prefixes/reloads, not OS/disk fault injection.

## Licensed manual QA - TESTING

1. In a Theurgist run, use Skills at19/20 and39/40 supplies; normal/epic craft exact
   native physical splashes. With all36 main slots full, inventory and resources stay
   unchanged. Check partial/free slots, item dragging, trade/container races and relog.
2. Splash positive potions on allies, their pets/mercenaries and nearby hostile/undead
   mobs. Allies receive valid benefits; hostiles receive none. Verify manual negative
   brewing/throws and a non-Theurgist throw retain their intended behavior.
3. Kill a teammate, inspect each resurrection button/120-cost hover and notification,
   then decline: no debit. Accept: exact latest death position, one120 debit, five-second
   protection, actual dropped items only, retained bed settings and no cooldown.
4. Race two casters and an eligible mercenary, repeat stale clicks, respawn normally,
   die again, disconnect, forfeit/complete and change resource affordability before
   acceptance. Only the current valid accepted offer may execute.
5. At supported GUI scales, resize/scroll Skills with many dead teammates, inspect
   disabled placeholder/costs and Accept/Decline placement and tooltips. Full licensed
   visual/multiplayer/world and CurseForge app acceptance remain pending.

## Delivery and remaining work

Publish unique Alpha runtime+loading companion once, then exact runtime to stopped
TEST through pinned sFTP. Record compact completion and pause after22. No local-client
copy, post-publication hash/readback, game launch/restart or image handoff.
Leave Set C unmerged until26. Four cards23-26 remain: paid wolf commands/run lifecycle,
core modes, advanced modes/performance and final integration/Beta.

## Exact changed files

- docs/ai/D1_REMAINING.md
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
- docs/ai/tasks/player-theurgist-actions-20261007.md
- docs/releases/1.6.12-alpha.1.md
- docs/releases/fragments/batch-22-player-theurgist.md
- gradle.properties
- src/main/java/net/goui/cosmicdungeon/client/DungeonDeathScreen.java
- src/main/java/net/goui/cosmicdungeon/client/ModNetworkClient.java
- src/main/java/net/goui/cosmicdungeon/client/screen/ClassResourceHelp.java
- src/main/java/net/goui/cosmicdungeon/client/screen/settings/CosmicDungeonOptionsIntegration.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/ClassResourceClient.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/TheurgistClient.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/DeathInventoryRecovery.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryResurrection.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryResurrectionState.java
- src/main/java/net/goui/cosmicdungeon/mixin/ResurrectionReceiptMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/SkillCloudMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/SkillSplashPotionMixin.java
- src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java
- src/main/java/net/goui/cosmicdungeon/network/TheurgistPayloads.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/ClassResourceService.java
- src/main/java/net/goui/cosmicdungeon/playerclass/skill/SkillPotions.java
- src/main/java/net/goui/cosmicdungeon/playerclass/theurgist/RevivalData.java
- src/main/java/net/goui/cosmicdungeon/playerclass/theurgist/RevivalPlan.java
- src/main/java/net/goui/cosmicdungeon/playerclass/theurgist/TheurgistActionEvents.java
- src/main/java/net/goui/cosmicdungeon/playerclass/theurgist/TheurgistActionGameTests.java
- src/main/java/net/goui/cosmicdungeon/playerclass/theurgist/TheurgistActions.java
- src/main/java/net/goui/cosmicdungeon/playerclass/theurgist/TheurgistCrafting.java
- src/main/java/net/goui/cosmicdungeon/playerclass/theurgist/TheurgistPotionCatalog.java
- src/main/java/net/goui/cosmicdungeon/playerclass/theurgist/TheurgistPotionEvents.java
- src/main/java/net/goui/cosmicdungeon/playerclass/theurgist/TheurgistPotionGameTests.java
- src/main/java/net/goui/cosmicdungeon/playerclass/theurgist/TheurgistPotionProtection.java
- src/main/java/net/goui/cosmicdungeon/playerclass/theurgist/TheurgistRevival.java
- src/main/java/net/goui/cosmicdungeon/transaction/InventoryTransactionGuard.java
- src/main/resources/cosmicdungeon.mixins.json
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/TheurgistClientTest.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
- src/test/java/net/goui/cosmicdungeon/network/TheurgistPayloadsTest.java
- src/test/java/net/goui/cosmicdungeon/playerclass/theurgist/RevivalPlanTest.java
- src/test/java/net/goui/cosmicdungeon/playerclass/theurgist/TheurgistPotionCatalogTest.java
- src/test/java/net/goui/cosmicdungeon/playerclass/theurgist/TheurgistPotionProtectionTest.java
