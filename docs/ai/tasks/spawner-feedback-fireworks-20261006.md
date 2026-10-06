# Batch13 - spawner feedback and Pyroclast fireworks
Authorization: Cameron requested Batch13 on2026-10-06. Stop after13.
Branch: feature/spawner-feedback-fireworks-20261006, chained from Batch12 a60da5115d443e11ec36b18d75daa7bcb89d37f2.
Release target:1.6.3-alpha.1. Individual SetA branch remains unmerged until16.

## Scope and owned integration surfaces
Own spawner interaction/rendering, shared authorized rocket destruction, native GameTest registration and version/release notes.
Remove outline color (native outline path caused through-wall fill); use sparse small dust from native nearby-block animation.
Server sends a large title for wrong-tool mining, once per40ticks/player; weak keys avoid retained disconnected players.
No new packet schema, tick scans, dependencies or global queues. Existing creative building is unaffected.
Shared authorized rocket path snapshots loaded candidates/visibility before any removal; radius is inclusive Euclidean five blocks
to block centers. Maximum1,331 block checks per detonation, rays only for cosmic spawners. No chunk loading.
Honor explosion Start/Detonate and player BreakEvent vetoes, spawn/world-border/access checks, and native explosion loot/removal.
Mercenary destruction uses its admitted hirer for player protection checks. Event-added blocks/duplicates cannot expand the allowlist.
Damage stays in the existing enemy-only burst before block removal; no native full-world explosion or additional entity damage.

## Preservation and boundaries
No changes to placed-spawner NBT, CosmicSpawnerDataVersion152, preset formats, SpawnData/SpawnPotentials, registry IDs,
authored chest contents, reset snapshots, saved-data or network protocol17. No migration required.
Existing legacy-spawner round-trip tests remain required. Protected placed data is compared before/after native detonation.
Retain existing3-5 XP bottle loot and normal block-entity cleanup; whole-dungeon reset continues to restore its authored snapshot.
No changes to region flags, currency/trades, progression or teleportation. Existing event protections apply.
Datagen is not applicable: no model/tag/loot/recipe/resource JSON changes.
No server.properties edits, server restart, local Minecraft/GameTest launch or direct main push.

## Validation and delivery
Pending Java21 local build and full Integration Gate, including all43 native GameTests.
Three new native tests cover player/mercenary real shared detonation, radius/walls, pre-removal shielding,
Start/Detonate/Break vetoes, ordinary terrain, native bottle drops, duplicate/ended-run denial, title text and throttling.
Existing349JUnit +2loading assets and34Python checks, all source JSON and diff checks remain required.
Review comments, release receipt/hash provenance, independently eligible target installation and final narrative checkpoint pending.
TEST last checked21:29:57Z: no shutdown in latest session; server1.5.12-alpha.1 left intact. ADMIN closed,1.6.2-alpha.1 installed.
Google read-only authorization expired earlier today; cached Pyroclast doc16FD3wxi-Uen_DRzItDHdSrSZvkGNwa_r-ZeUYiswxoE
plus explicit settled Cameron requirements used. Audit/BATCH13_SPAWNER_SOURCE_20261006.json records cached hash; no fresh audit claimed.

## Licensed manual QA pending
1. Block a configured spawner's spawn space: faint red particles, no filled outline; a solid wall hides the effect. Unblock it and particles stop.
2. Start mining with hand/stick: exact title appears without spam; any pickaxe and creative editing avoid the warning.
3. Fire player and mercenary rockets near exposed cosmic spawners: within5blocks breaks; behind walls/outside radius/protected locations survive.
4. Check3-5 XP bottles per eligible spawner, nearby ordinary blocks and friendly entities unchanged; reset a copied dungeon and confirm authored spawners/settings return.
5. Use matching server/client runtime; verify companion installation in the licensed CurseForge profile independently.

Future improvement: measure visibility and particle subtlety on the authored map before tuning density.

## Exact changed files
- `gradle.properties`
- `src/main/java/net/goui/cosmicdungeon/client/render/blockentity/CosmicSpawnerRenderer.java`
- `src/main/java/net/goui/cosmicdungeon/block/custom/CosmicMobSpawnerBlock.java`
- `src/main/java/net/goui/cosmicdungeon/block/entity/CosmicSpawnerFeedback.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/d1/CosmicSpawnerFireworks.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/d1/CosmicSpawnerGameTests.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1RocketAbilities.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java`
- `src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java`
- `docs/releases/1.6.3-alpha.1.md`
- `docs/releases/fragments/batch13-spawner-fireworks.md`
- `docs/ai/tasks/spawner-feedback-fireworks-20261006.md`
- `docs/ai/PARTY_SKILLS_BATCHES_20261006.md`
- `docs/ai/D1_REMAINING.md`
