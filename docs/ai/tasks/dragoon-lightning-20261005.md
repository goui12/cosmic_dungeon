# Batch 8: Dragoon mercenary lightning, 2026-10-05

Authorized scope: Batch 8 only. Branch feature/dragoon-lightning-20261005, stacked on
Batch 7 source c45af42e4926300d21420004e1bd92cd52e143c2 / PR224. Version 1.5.10-alpha.1.

## Behavior and integration

Server post-damage events bank successful melee/attributed-projectile hits, including
killing hits and absorbed damage. Initial threshold 50; max(5,51-level) lowers it once per
level until 46. Casts consume that threshold; actual secondary damage adds one hit each.
A successful cast earns exactly one shared skill event, with the existing triangular
1/2/3/... increments, group announcements and HUD tooltip. Failed casts refund the charge.

The existing staggered ten-tick AI decision dispatches at most one cast per20 active ticks.
No casts inside damage callbacks, recursive bursts, queued work lists or offline catch-up.
Native damage respects shields/armor/immunity. Stored power is the latest primary hit's
final health/absorption damage times existing Config.CHAIN_DAMAGE; secondary hits do not
replace or recursively multiply that power. Last primary victim is excluded from the cast.

Reuse the configured Dragoon radius (32), candidates (256), targets (64) and ten-particle
arc; existing player logic only exposes its renderer. Bounded snapshot, sort and LOS work
per cast, with no new global entity cache or world scan. Both mercenary and hirer range
must contain the target. Protection covers players, hired companions, owned pets, NPCs,
neutral animals and scoreboard allies. Active contract/run, living online hirer, dimension,
sealed run and cleanup state are checked server-side. No new client gameplay authority.

Exclusive surfaces: MercenaryEntity optional entity serialization, DungeonInstanceGameTests
registration, gradle.properties version. No competing writer. No core mod, packet, menu,
registry, spawner, teleport, door/key, transaction/currency or authored chest edits.

## Persistence and compatibility

Optional mercenary_lightning codec stores hits, last primary power/victim and active cooldown.
Missing/invalid field loads empty with no manual migration. Existing entity keys, supplies
and run skill-counter identities are retained. Same entity death/revival/reload preserves
charge and levels; a newly instantiated dungeon begins empty at level1. Counter saturation
prevents integer overflow. No spawner/preset migration or world recreation.
Normal full-world backups remain the rollback source; old binaries will ignore the new
optional field and cannot promise retaining its charge on a later downgrade/resave.

## Validation

Java21 build passed: 329 native tests and two loading-helper tests. A literal newline
in a newly added native fixture assertion was corrected after compilation caught it;
all gameplay assertions remain. Publisher tests and JSON/diff checks follow before
commit; CI clean build plus31 GameTests are required before tagging. Final results, exact source,
artifact hashes, CurseForge receipt and deployment status are recorded in PR/operational
checkpoint. No local client/GameTest/server launches or licensed gameplay acceptance.
Datagen is not applicable: no generated resources, recipes, tags, loot, models or IDs changed.

## Manual TEST QA (pending)

1. Hire a Dragoon, land50 successful enemy hits and inspect the lightning and level2 message.
   Check that misses/immune/allied hits earn nothing and the original victim is not struck twice.
2. Fight a group: secondary hits advance the next charge without recursive bursts.
   Three total successful casts reach level3; six reach4; ten reach5. Check inventory hover.
3. Stand beside allies, owned wolves, mercenaries and neutral mobs; verify protection.
   Put enemies behind walls and beyond range; verify no arc damage through them.
4. Kill/revive the mercenary, then save/relog: levels and partial charge persist.
   Disconnect/exit the hirer or forfeit: no cast. Start a new dungeon: empty charge and level1.

Future improvement: use licensed crowded-room QA to tune high-level cadence if needed.
One planned batch remains: 9 Theurgist resurrection; do not start it in this task.
Completed-set main merge/Beta follows Batch9 and validation.

## Source alignment

Google metadata/body refresh attempted2026-10-05; existing read-only OAuth expired.
Cached Dragoon overview144 (2026-08-28) has no mercenary hit-count rule. Explicit latest
Cameron requirements govern. Private Audit/BATCH8_LIGHTNING_SOURCE_REFRESH_20261005.json
records source ID/hash/failure. No fresh Google semantic-alignment claim.

## Exact files

- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryLightning.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryLightningState.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryLightningGameTests.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEntity.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryBrain.java
- src/main/java/net/goui/cosmicdungeon/playerclass/dragoon/DragoonPassiveEvents.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryLightningTest.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
- gradle.properties
- docs/ai/CURSEFORGE_AND_MERCENARY_BATCHES_20261004.md
- docs/ai/D1_REMAINING.md
- docs/releases/1.5.10-alpha.1.md
- docs/releases/fragments/dragoon-lightning-20261005.md
- docs/ai/tasks/dragoon-lightning-20261005.md
