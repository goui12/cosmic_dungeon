# Testing fixes, batch 3 — 2026-10-03

Authorized: verify batch 2, complete batch 3, then stop and list remaining batches.
Base: 067f832e, feature/testing-fixes-batch2-20261003, draft PR201.

Three changes:
1. Server-authoritative developer-only spawner equipment and preset authoring.
2. Fixed top/N/W/S/E/NE/NW/SW/SE placement, block-only collision checks, red blocked state, no radius control.
3. Independent fresh-mob activation; idle until nearby player detection or direct damage, with all living spawned mobs still counted.

Files: CosmicMobSpawnerBlock, CosmicSpawnerBlockEntity, SpawnerCommand,
CosmicSpawnerHoverOverlay, CosmicSpawnerHudOptionsScreen; new cohesive placement,
runtime and awareness helpers, native BaseSpawner delay accessor and Mob AI mixin;
cosmicdungeon.mixins.json; scoped native JUnit tests; spawner docs/release fragment/reports.
Hotspots owned exclusively: spawner lifecycle/configuration, scoped mixin registration.
No CosmicDungeonMod, ModNetwork, currency, inventory, class chest, teleport or reset edits.

Reuse native delay/weighted potential selection, spawn hooks, saved presets, tags, and
loaded membership counts. Keep legacy SpawnRange serialized unchanged but unused by
fixed placement. Blocked state is transient client synchronization only. Optional per-mob
activation metadata defaults safely for legacy mobs; no destructive migration or world scan.
Player proximity uses an eight-block, line-of-sight detection range; checks every ten ticks.
Blocked attempts retry every ten ticks. No material resource increase or new dependency.

Validate Java21 build/native JUnit, all source JSON and scoped diff; inspect native
startup for mixin application. No generated resources, so datagen does not apply.
No destructive clean or dedicated/GameTest launch. Licensed TEST QA remains explicit.
Preserve unrelated staged historical JAR deletion, generated caches and logs.
