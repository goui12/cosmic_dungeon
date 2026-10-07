# Batch25: Bogatyr advanced wolf modes

Approved card25 only; version1.6.15-alpha.1 on feature/bogatyr-advanced-modes-20261007.
Baseline Batch24:810c141471a3e9a6220961d17afdac79337726cf. Set C remains unmerged until26.
Root owns AI/network/save/effect integration, native tests and release; one worker owns
client selection and pure tactics rules/tests, with independent native integration review.

## Behavior and compatibility

All six free buttons share authoritative server selection and existing pending controls.
Strategic ranks ranged mobs first, descending maximum health next, then stable UUID.
Native ranged attackers and specialized ranged casters are recognized.
Search and Rescue chooses nearby living online same-run party players at6HP or less,
interposes and redirects their actual attackers, including native brain targets.
Diverted attackers remain associated with the protected player while engaged.
Companionship heals2HP per100 continuous server ticks once per protected player across
all guarding wolves and owners. Presence/run/mode/distance are rechecked at healing time;
leaving protection, death, logout or run change resets cadence. Offline time adds nothing.
The new beneficial cosmicdungeon:companionship indicator reuses the vanilla regeneration
sprite. No existing healing effect identity existed; the unrelated teleport-selection
Potion of Companionship is unchanged.

Danger Close uses a moving16-block 3D boundary. A valid current kill is retained;
afterward threats to the master rank first. Initial/recomputed paths and cheap native
path progress checks reject excursions. When the master moves beyond the boundary,
wolves stop attacking and take bounded inward return paths, including partial paths.
Native knockback/physics and obstruction can affect actual position; this is a navigation
and combat constraint, not teleportation or world clipping.

Existing schema1 owner/run records and all old/unknown fields remain. Enum values append;
legacy values/ordinals are unchanged, unknown future modes remain passive/noneditable.
No world/spawner/chest/loadout/renamed-rocket migrations. Mode saves remain normal native
SavedData; no per-tick/click disk work. Network protocol advances to25 for six-mode peers.
A new mob-effect registry entry is additive; use normal world backups for rollback,
and prefer forward corrections rather than removing a registered effect mid-session.

## Performance and evidence

Expensive target queries are shared per owner, globally budgeted and staggered around
20ticks. Friendly/dead mobs are filtered before the accepted-candidate budget so large
packs cannot hide enemies. Cheap mode/leash/target eligibility stays responsive.
Native path work retains20-24tick cadence. Rescue avoids repeatedly redirecting the same
attacker among guards of one patient. No per-wolf world scan, packet polling or hot logs.
Opt-in native CI instrumentation compares Aggressive baseline with each advanced mode
at1/30/120wolves for400 synthetic decision ticks, moving the target periodically and
exercising native path requests. It asserts target coverage and scan/decision bounds;
the CI WOLF_WORK lines and batch-25-performance receipt retain actual timing/counters.
These are native decision/path samples, not full physics/TPS or licensed co-op benchmarks.
No heap/dependency change and no unconditional30-wolf cap. Revisit the conditionally
authorized cap only if measured cost demonstrates optimization is insufficient.

## Validation and manual acceptance

Required: relevant runClientData and review of its sole atlas alias, full Java21 local
build without clean, all source JSON/diff checks and exact-source full clean CI with72
native GameTests. Five new cases cover ranged/max-health ordering and mode save reload;
actual mob/brain redirection and sustained rescue; per-player healing across owners and
stale protection; moving radius/current-kill/path bypasses; and native1/30/120pack work.
Existing core-mode authority, lifecycle and friendly protection tests remain required.
Controller receipts record actual test counts and exact source/CI/publication evidence;
this report does not pre-claim their outcome.

Licensed QA remains TESTING: select all six modes at narrow/wide GUI scale and confirm
one selected border; compare ranged/high-health target order; injure a party player to
3 hearts and observe interposition, attacker diversion and one-heart/5sec healing with
one and many guards. Walk the master around obstacles and across16blocks while a wolf
fights; verify target retention only inside the moving boundary and return behavior.
Check reconnect/run changes, other owners' packs and party HUD Companionship icon.
Natural terrain, physical knockback, rendering and sustained real co-op remain unplayed.

## Release and stop

Publish the unique Alpha runtime and matching loading companion after required gates.
Update only stopped TEST through pinned sFTP. No local-client copy, game/server launch,
post-publication artifact hashes or image handoff checks. Complete compact evidence and
pause after25. One card remains:26 integration and final completed-set Beta.

## Exact changed files

- docs/ai/D1_REMAINING.md
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
- docs/ai/tasks/bogatyr-advanced-modes-20261007.md
- docs/releases/1.6.15-alpha.1.md
- docs/releases/fragments/batch-25-bogatyr-advanced-modes.md
- gradle.properties
- src/generated/resources_client/assets/minecraft/atlases/mob_effects.json
- src/main/java/net/goui/cosmicdungeon/client/screen/ClassResourceHelp.java
- src/main/java/net/goui/cosmicdungeon/client/screen/HelpMenuContent.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/BogatyrClient.java
- src/main/java/net/goui/cosmicdungeon/datagen/D1EffectAtlasProvider.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/main/java/net/goui/cosmicdungeon/effect/CompanionshipMobEffect.java
- src/main/java/net/goui/cosmicdungeon/effect/ModMobEffects.java
- src/main/java/net/goui/cosmicdungeon/mixin/BogatyrNavigationMixin.java
- src/main/java/net/goui/cosmicdungeon/network/BogatyrPayloads.java
- src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrAggro.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrBoundary.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrCommandGameTests.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrModes.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrRanged.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrRescue.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrTacticsGameTests.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrThreats.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrWork.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/WolfMode.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/WolfTacticsRules.java
- src/main/resources/assets/cosmicdungeon/lang/en_us.json
- src/main/resources/cosmicdungeon.mixins.json
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/BogatyrModesClientTest.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
- src/test/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrModeDataTest.java
- src/test/java/net/goui/cosmicdungeon/playerclass/bogatyr/WolfTacticsRulesTest.java
