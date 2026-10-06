# Batch 6 - Bogatyr wolf scaling

Cameron authorized Batch6 on2026-10-05 after checking the earlier review notes.
Branch feature/bogatyr-scaling-20261005, based on0eb14ac3ebebed6ff3c5d07ea01bba8c40d22c92.
Version1.5.8-alpha.1. Stop beforeBatch7.

## Behavior and ownership
Own MercenaryWolves, the entity's transient command cursor, a small balance helper,
wolf regression/GameTests and suite registration. Also own the publisher/workflow
approval gate requested by the unresolved publication note. No other writer is active.
No CosmicDungeonMod, registry, packet, currency or saved schema changes.

Baseline five wolves, one summon per2400active ticks. Interval is
ceil(2400/(1+0.06*(level-1))), floor400ticks. Cap5+floor((level-1)/2);
level3=6,5=7,10=9,25=17,50=29. Every second level-up grants a slot.
The existing integer skill range bounds arithmetic; no new arbitrary gameplay level cap.
Successful native insertion earns one event, then starts the new level's shorter timer.
Failed insertion rolls back its reserved roster identity and earns nothing. Failed placement
retains the one-second retry. Saved remaining timers are clamped to the current interval
during active processing, never simulate offline catch-up or emit multiple summons at once.

Capacity comes from existing run/contract skill counters, including when the mercenary is
unloaded. Existing identities can rejoin even if a counter was reduced; no saved wolves are
deleted by cap recalculation. New identities require a free slot. Observed death frees it;
unload/dimension change does not. Death/revival retains skill counters; a new run resets.
Existing player Bogatyr code remains uncapped and separate. Armor/allied damage/potion
guards, loot behavior and authored chest contents are untouched.

More native wolves cost more AI work as requested. Command work remains five roster
lookups per existing10-tick mercenary decision, rotating fairly through larger packs.
No larger entity scans, new global cache, network traffic or runtime dependency.
Dismissal marks the full pack first; bounded immediate removal is followed by existing
40-tick per-wolf validation and join rejection for unloaded wolves.

## Release follow-up and earlier reviews
Batch5 workflow37270083845 attempt2 succeeded; main9067112,companion9066516,archive9067979.
Earlier Batch1 validation satisfied; Batch2 fixture corrected; Batch3 run-dimension and
fund checks verified in current source; Batch4/5 no inline findings. Existing toolchain
notices/deprecations do not fail builds and are outside this feature's source changes.
The companion's New/unapproved status caused repeated HTTP400/API1018 relation rejection.
New CLI/Actions releases omit that relation unless CURSEFORGE_LOADING_RELATION_APPROVED=true
(or --link-loading). They still upload exact main/helper to their separate projects and
GitHub, record the deferred relation, and skip a redundant parent archive for that explicit
pair. Legacy receipt recovery remains unchanged. Unknown POST outcomes remain blocked;
approved flags never claim to retroactively change an accepted file.
No complete CurseForge app migration claim until companion approval and licensed app QA.

## Validation
Six new native tests cover growth/cadence bounds, saved rosters and reset, reduced counters,
bounded rotating commands and unloaded/sitting/foreign entries. A new registered GameTest
exercises server-backed skill/capacity, revival and timer adjustment. Three publisher tests
cover deferred-pair uploads, retries/approval changes and unchanged unknown-upload guards.
Local Java21 build passed315native+2helper tests with zero failures/skips;14publisher checks passed.
CI clean build/26GameTests and exact release artifacts remain delivery gates. Final results
are recorded in the PR/operational receipt.
No datagen applies: Java logic, test registration, tooling and docs only.
No save migration, registry/spawner/preset/network format change.
Google metadata refresh failed expired saved read-only OAuth; private
Audit/BATCH6_WOLVES_SOURCE_REFRESH_20261005.json records cachedIDs/hashes and failure.
Explicit current Cameron requirements govern; no fresh canon-alignment claim.

## Pending licensed QA
1. Hire Bogatyr: five baseline slots and first summon after two active minutes.
2. Observe level3 sixth slot and shorter intervals; kill a summoned wolf and verify
   replacement, then unload/reload without obtaining extra slots.
3. Verify wolves beyond the fifth fight, accept armor and resist allied attacks/potions;
   dungeoneer wolves stay uncapped. Dismiss/finish the run and verify full pack retirement.
4. Mercenary death/revive/relog retains Wolves; next dungeon starts at level1.
No automatic local game/server/launcher launch or server restart.
Future improvement: measure native wolf AI cost during licensed high-level pack testing.
Three later batches remain:7 Pyroclast fireworks,8 Dragoon lightning,9 Theurgist resurrection.

## Exact files changed
- .github/workflows/curseforge-release.yml
- docs/CURSEFORGE_RELEASES.md
- docs/ai/CURSEFORGE_AND_MERCENARY_BATCHES_20261004.md
- docs/ai/D1_REMAINING.md
- docs/ai/tasks/bogatyr-scaling-20261005.md
- docs/releases/1.5.8-alpha.1.md
- docs/releases/fragments/bogatyr-scaling-20261005.md
- gradle.properties
- scripts/curseforge_release.py
- scripts/tests/test_curseforge_release.py
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEntity.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryWolves.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryWolfBalance.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryWolfGameTests.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
- src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryWolfScalingTest.java
