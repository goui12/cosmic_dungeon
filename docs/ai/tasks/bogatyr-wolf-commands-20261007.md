# Batch23: Bogatyr Wolfpack care and run-only lifecycle

Approved card23 only; version1.6.13-alpha.1. Branch feature/bogatyr-wolf-commands-20261007
starts at Batch22 e1b00cff0f45cb1d611636f683af3cb52f7012e0. Set C remains unmerged until26.
The coordinator owns networking, command transactions, persistence/reset and release.
One cooperative worker owns the client adapter and disjoint native/unit fixtures.

## Behavior and authority

The reusable Skills panel keeps movable placement, minimize/title/Kibble, Request
Supplies/Recycle and ten equal wolf actions. Breed/Summon/Regroup/Heal use exact
server quotes; six future mode rows are visible and disabled. Commands validate
class, alive active membership, menu/cursor/transaction state, generation, ownership,
current run/dimension, native loaded identity, eligibility, affordability and placement
again on the server. Rejections acknowledge a new generation. A one-second command
rate limit prevents repeated disk work. Periodic views inspect one owner's indexed
pack once per second, with changed-only payloads and no world scans.

Breed costs5 per healthy adult with age0 and no existing love; partial affordability,
including1, is allowed. It stands selected wolves and starts native love/mating;
native offspring enrollment keeps own pups tame. Juveniles/cooldowns/no-ops are excluded.
Summon costs30 only for one accepted, safely placed tame wolf. Heal spends5 per injured
living loaded wolf, full health in lowest-health order, with affordable subsets.
Regroup costs1 per actually moved living loaded eligible wolf and requires enough for
the entire affected pack. Safe destinations stay in loaded current-dimension chunks;
a passenger, vehicle or leashed pack member disables Regroup until the whole pack can move. It never searches chunks,
revives wolves or delivers archives. No new cap is imposed without measurements.

## Save and cleanup compatibility

Native active-run wolf saves remain authoritative. The existing companion codec overlays
known fields onto original root, per-entry and archive fields so unknown data survives.
Durable run tombstones precede cleanup. Ended entries/archives leave active indices;
retired images are retained for audit and cannot be delivered. Loaded retirement and
late native loads reject ended-run wolves before enrollment, so old packs cannot be
adopted into a later run. Active pending native deliveries are preserved until observed
or their own run ends; old call/recover commands explain the current Regroup action.
Retired archives no longer demand entity chunks or obstruct reset.

A separate versioned pending-command journal retains native wolf preimages and resource
ledger identity and a verified original-owner save before effects. Successful effects are saved and checked against native
entity files, then an entities_saved receipt fixes exact outcomes and cost. The owner
ledger debit and command receipt share one verified player save. Native reload can settle
that exact outcome once; completed receipts retire the pending journal. Malformed
identity, phase, outcome count/cost or run/budget fails closed. An interrupted prepared
phase has an unknown effect outcome and stays held for explicit save review: no blind
replay, replacement spawn or refund. Pending commands block resource/inventory cleanup
even when the owner is offline. Existing transport/deployment policy is unchanged.

No authored chest/loadout/renamed rocket/spawner data changes. No new dependencies.
Native entity-save verification occurs for accepted commands, not per tick; this is a
bounded user-action disk cost. Periodic owner roster iteration scales with actual pack
size; current packs remain uncapped.

## Validation and acceptance

Required: Java21 full local build (without clean), JSON/diff checks, component and
independent review, then exact-source full clean CI/native GameTests. No datagen inputs
changed. Local compilation is not a gameplay test. Controller receipts record actual
counts/source/CI/publication/deployment; this document does not pre-claim delivery.

Initial CI passed the clean build and62/64 native cases; two new reload fixtures exposed
missing native world-player registration in the test harness. The fixtures now register
and remove actual player entities across reloads, retaining strict ownership/bond/no-replay
assertions. Paid roster ownership uses saved UUIDs plus current server-player identity.

Native/unit coverage targets partial care/cooldowns, full heal ordering, successful and
rejected summon, loaded-only whole-affordability Regroup, duplicate/stale actions,
native player/entity save pairing, exact settlement/no replay and run retirement with
late loads, unknown-field compatibility and bounded client payloads.

Licensed manual QA remains TESTING: drag/minimize/reset Wolfpack at narrow/wide GUI
scales, confirm all ten rows/scrollbar/tooltips and resource updates, watch ordinary wolf
pathfinding/mating and tame pups, exercise multiplayer ownership and dimension separation,
then completion/forfeit/reset and an unloaded old wolf returning. Actual OS/disk-fault
injection is unperformed; uncertain saved preparations intentionally require review.

## Release and recovery

Publish unique Alpha runtime plus loading companion through the single CurseForge
publisher after all gates, then update only stopped TEST through pinned sFTP.
No local client/helper installation, game/server launch or post-CurseForge artifact
hash checks. Finish the compact controller receipt and pause after23;24-26 remain.
Before publication, rollback is the scoped source commit. After live saves adopt this
run-only policy, do not restore the old archive-delivery code against newer data; retain
save evidence and use a forward correction. Retired packs intentionally cannot be recalled.

## Exact changed files

- docs/ai/D1_REMAINING.md
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
- docs/ai/tasks/bogatyr-wolf-commands-20261007.md
- docs/releases/1.6.13-alpha.1.md
- docs/releases/fragments/batch-23-bogatyr-wolf-commands.md
- gradle.properties
- src/main/java/net/goui/cosmicdungeon/client/ModNetworkClient.java
- src/main/java/net/goui/cosmicdungeon/client/screen/ClassResourceHelp.java
- src/main/java/net/goui/cosmicdungeon/client/screen/HelpMenuContent.java
- src/main/java/net/goui/cosmicdungeon/client/screen/settings/CosmicDungeonOptionsIntegration.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/BogatyrClient.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/ClassResourceClient.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/main/java/net/goui/cosmicdungeon/network/BogatyrPayloads.java
- src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrActionEvents.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrActions.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrCommandData.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrCommandGameTests.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrCommands.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrCompanionData.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrCompanions.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrRecovery.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrRunLifecycle.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrWolfEvents.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/WolfCommandRules.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/ClassResourceService.java
- src/main/java/net/goui/cosmicdungeon/transaction/InventoryTransactionGuard.java
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/BogatyrClientTest.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
- src/test/java/net/goui/cosmicdungeon/network/BogatyrPayloadsTest.java
- src/test/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrLifecycleDataTest.java
- src/test/java/net/goui/cosmicdungeon/playerclass/bogatyr/WolfCommandRulesTest.java
