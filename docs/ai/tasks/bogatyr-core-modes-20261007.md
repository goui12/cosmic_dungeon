# Batch24: Bogatyr core wolf modes

Approved card24 only; version1.6.14-alpha.1 on feature/bogatyr-core-modes-20261007.
Baseline is Batch23 43f6f31151360df64ea247833bb33b8c44d82005. Set C remains unmerged until26.
Root owns server AI, save/network integration, native regressions and release. One worker
owns client model/render/adapter and disjoint unit tests, with independent native-hook review.

## Behavior and authority

Defensive protects the master and responds to attacks on the master or current-run pack.
It rejects native passive-prey hunting and the master's outgoing-only attack assistance.
Aggressive selects hostile mobs nearest the master, then works outward within the configured
follow radius. Friendly players, pets, NPCs and owned companions retain protection.
Stand Ground immediately stops active movement goals/navigation, clears target, anger and
retaliation, and sits. Dedicated MOVE/JUMP control and native damage/sit hooks keep the wolf
sitting even under attack; owner teleport is suppressed. Choosing another core mode stands
loaded wolves up immediately. Ordinary physics and incoming damage remain native.

Three core buttons are free; one selected border follows the server acknowledgment, including
while pending/disabled. The existing ten equal rows, scrolling, movement/minimize and care
previews remain. Three advanced modes stay visibly disabled until25. Breed is unavailable
during Stand Ground, avoiding a charge for wolves forbidden to move; select another mode
before breeding. Summon, Heal and explicit Regroup retain their established rules.

Server execution rechecks session generation, active run, actual current player, class,
alive status, menu/cursor and transaction holds. Mode/care commands share authoritative
generations and client pending state. Rejected/stale requests acknowledge a fresh snapshot.
No Kibble debit, inventory changes, chunk loads, archive delivery or replacement wolves occur.

## Saves and performance

The existing companion SavedData receives additive schema1 wolf_modes records keyed by
owner UUID/run. Missing legacy state defaults to Defensive. Unknown root/per-mode fields
round-trip; unknown/malformed future mode state remains untouched, passive and noneditable.
Loaded wolves adopt current owner/run state on native admission and cheap tick checks;
unloaded wolves are never fetched. Reconnect uses the same world state and a fresh packet
generation; later runs start independently. Mode changes mark native SavedData dirty and
use normal world saves, with no per-click or per-tick disk work. A crash before a normal
save can lose the latest free preference, without item/resource effects or replay hazards.
Native save/readback regression proves saved preferences reload. No schema/registry IDs
are renamed. Rollback preserves additive records but older runtime ignores these controls;
take normal world backups before version changes and prefer a forward correction.

Spatial target queries remain owner-shared, candidate-bounded and globally budgeted.
Wolf choices are staggered around the existing default20-tick interval; cheap eligibility
checks reject stale/friendly/dead targets between choices. Actual damage records a bounded
200-tick pack retaliation history without scans. Target ordering is distance then UUID.
Native follow/melee heavy path rebuilding is staggered around20-24 ticks, preserving native
movement and attack ticks and path-failure backoff. Packs remain uncapped. No dependencies,
heap changes, authored chest/loadout/renamed rocket/spawner rewrites or datagen inputs.

## Validation and acceptance

Required gates: full Java21 local build without clean, all source JSON/diff checks,
focused client/payload/old-save tests and exact-source clean CI with67 native GameTests.
Three new native cases exercise actual damage and sitting hooks, immediate goal/target
clearing, defensive/nearest-hostile selection and allies, path cadence, stale/class/run
authority, zero-resource mode changes, reconnect and unloaded native wolf/save adoption.
Task/controller receipts record actual counts and completed source/CI/release evidence;
this report does not pre-claim publication or deployment.

Licensed manual QA remains TESTING: open inventory Wolfpack at narrow/wide GUI scales,
switch each core mode and check one selected border; attack the master/pack and verify
Defensive response; observe nearest-first Aggressive combat; issue Stand Ground mid-path
and under damage, then resume a different mode. Check another player's pack is unchanged,
reconnect/reload an active pack, and confirm Breed is free of charges while disabled.
Real co-op rendering, long-duration pathfinding and physical knockback acceptance are unplayed.

## Release and stop

Publish unique Alpha runtime and matching client-only loading companion through CurseForge
after all gates. Update only stopped TEST through pinned sFTP. No local client installation,
game/server launch, post-publication artifact hashes or extra handoff-image checks.
Write the compact completion receipt and pause after24. Two cards remain:25 advanced
wolf modes/performance,26 integration and completed-set Beta.

## Exact changed files

- docs/ai/D1_REMAINING.md
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
- docs/ai/tasks/bogatyr-core-modes-20261007.md
- docs/releases/1.6.14-alpha.1.md
- docs/releases/fragments/batch-24-bogatyr-core-modes.md
- gradle.properties
- src/main/java/net/goui/cosmicdungeon/Config.java
- src/main/java/net/goui/cosmicdungeon/client/screen/ClassResourceHelp.java
- src/main/java/net/goui/cosmicdungeon/client/screen/HelpMenuContent.java
- src/main/java/net/goui/cosmicdungeon/client/screen/settings/CosmicDungeonOptionsIntegration.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/BogatyrClient.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/SkillsPanelComponent.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/SkillsPanelModel.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/main/java/net/goui/cosmicdungeon/mixin/BogatyrDamageMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/BogatyrFollowMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/BogatyrMeleeMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/BogatyrSitMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/BogatyrTeleportMixin.java
- src/main/java/net/goui/cosmicdungeon/network/BogatyrPayloads.java
- src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrActions.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrCommandGameTests.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrCommands.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrCompanionData.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrModes.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrThreats.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/WolfMode.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/WolfModeData.java
- src/main/resources/cosmicdungeon.mixins.json
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/BogatyrClientTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/BogatyrModesClientTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/SkillsPanelModelTest.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
- src/test/java/net/goui/cosmicdungeon/network/BogatyrModesPayloadTest.java
- src/test/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrModeDataTest.java
