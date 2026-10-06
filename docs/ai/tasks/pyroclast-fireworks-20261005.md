# Batch 7 - Pyroclast fireworks

Cameron authorized Batch 7 on 2026-10-05. Branch feature/pyroclast-fireworks-20261005,
based on 30c5d50dc0d1765e17a10e3be098c61277ea0861. Version 1.5.9-alpha.1.
Stop before Batch 8. No concurrent writer.

## Behavior and balance

Player and mercenary rockets share the enemy-only damage policy. Tenfold configurable
multiplier on existing base powers gives 120/150 HP centers for Cinderbite/Cindermaul,
before distance falloff, armor/shields and existing player skill modifiers. Native unnamed
rockets use Cinderbite base power. No authored stack, chest payload or loadout edits.
Historical low rocket powers are superseded by Cameron's explicit high-damage request;
base configuration values stay compatible, with a new default multiplier field.

Existing radius 5 and candidate budget 256 (configured bounds unchanged), two wall rays
per enemy and no terrain damage. Players, mercenaries, owned companions, friendly NPCs,
neutral animals and scoreboard allies are excluded. Inactive/dead/departed/class-switched
or changed-run launchers cannot turn a captured rocket into later vanilla damage.
A spent stamp suppresses repeated detonations. The player-only owner cast is removed.

Mercenary virtual ammunition starts at five, restores one per 600 active ticks at level 1,
and stops producing at capacity. Interval ceil(600/(1+.06*(level-1))), floor 100.
Capacity 5+floor((level-1)/2): six at 3, seven at 5, 29 at 50. Existing integer skill range
bounds arithmetic. Reduced counters never erase already held stock. No materials consumed
or collected; unrelated equipment/supply copies remain unchanged.
One damaging burst earns one success across all targets; blocked/immune/empty bursts earn
none. Existing skill thresholds, HUD, group chat, death preservation and new-run reset apply.

Prioritize clusters before arrows/melee using the existing nearby snapshot. At most 48
eligible candidates and eight aim centers (384 distance checks), only when the shared
two-second attack clock is ready and stock exists. No additional world scans or global cache.
Extra native rockets have short ordinary lifetimes; no new visual particle multiplier.

## Ownership, persistence and boundaries

Own rocket policy/config, mercenary brain/entity stock and test-suite registration.
New optional mercenary_fireworks entity codec stores count/timer; absent older data defaults
to five/600 and all existing contract, supplies, rest and timer fields retain their names.
Death/revival/reload retains stock and earned levels. Offline time produces no backlog.
New projectile admission/spent fields are additive persistent data on rockets; native
projectile owner data remains authoritative. No manual world migration is required.
No registry/spawner/preset/packet format changes, runtime dependencies or heap changes.
Access/run gates, class-bound ammunition checks and native damage hooks remain authoritative.
No door/key, trade/currency, vendor, teleport, authored chest or dungeon reset changes.
Server-authoritative common code; both client and server use the matching runtime JAR.

## Validation and delivery

Native regression tests cover restock boundaries/scaling/caps, serialization, bounded cluster
selection, enemy filtering and configured damage. Three new GameTests cover actual native
explosion damage/walls/allies, denied rocket handling and entity save/revival/new-run stock.
Java 21 build, full JSON validation and diff checks precede source commit.
CI clean build/GameTests and exact release artifacts are delivery gates; final results
are recorded in the PR and operational receipts. No datagen applies to these Java changes.
No local game, server, GameTest or launcher launch is authorized or performed.

Google metadata refresh failed expired saved read-only OAuth; private
Audit/BATCH7_FIREWORKS_SOURCE_REFRESH_20261005.json records the cached overview ID/hash and
failure. Current explicit Cameron requirements govern; no fresh canon alignment claim.

## Pending licensed QA

1. Fire Pyroclast rockets near groups of enemies, players, mercenaries and armored wolves:
   high enemy damage, no friendly damage, no terrain damage; solid walls block damage.
2. Hire Pyroclast and fight a cluster: rockets take priority, initial five-shot reserve,
   at least two seconds between attacks; empty reserve restores one per 30 active seconds.
3. Inventory HUD Fireworks rises only for a burst that damages an enemy, once per burst;
   group announces level-ups. At level 3, capacity is six and restocking is faster.
4. Mercenary death/revive/relog retains progress and stock; a new dungeon resets them.
   Check departing/changing class before impact produces no stale rocket damage.

Possible future improvement: tune multiplier and cluster selection with licensed boss-fight
and crowded-room measurements. Two later batches remain: 8 Dragoon lightning;
9 Theurgist resurrection. Full cumulative multiplayer QA remains separate.

## Exact files changed

- docs/ai/CURSEFORGE_AND_MERCENARY_BATCHES_20261004.md
- docs/ai/D1_REMAINING.md
- docs/ai/tasks/pyroclast-fireworks-20261005.md
- docs/releases/1.5.9-alpha.1.md
- docs/releases/fragments/pyroclast-fireworks-20261005.md
- gradle.properties
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryBrain.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEntity.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryFireworkStock.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryFireworks.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryFireworkGameTests.java
- src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1AbilityConfig.java
- src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1RocketAbilities.java
- src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1RocketGameTests.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
- src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryFireworksTest.java
