# Testing fixes, batch 3 — implementation report

Source branch: feature/testing-fixes-batch3-20261003. Base: 067f832e / draft PR201.
Authorization: verify batch 2, complete these three changes, then stop before batch 4.

## Changes

1. Spawner equipment configuration is developer-only on the server. Hand interaction,
   command authoring, direct target mutation and preset keybind application all enforce
   the existing Developer rank. Operator permission alone cannot change equipment.
2. Spawns try top, north, west, south, east, northeast, northwest, southwest, southeast,
   at centered fixed one-block offsets. Actual final entity and passenger bounds must
   clear blocks, the world border and loaded chunks; entity overlap is allowed. A red
   outline and red developer HUD warning identify blocked spawners. Retry interval is
   ten ticks. The obsolete radius row/control is removed.
3. Fresh spawned mobs wait for their own visible survival/adventure player within
   eight blocks, checked every ten ticks, or direct damage from a living attacker.
   Older mobs' target alerts cannot wake fresh waves. Goal and brain AI are both gated.
   Fighting and idle mobs share the existing dimension-specific live membership cap.
   Passenger groups cannot exceed the remaining cap. Existing native despawn/chunk
   visibility membership semantics remain; this is not a persistent census of unloaded
   entity files.

## Batch 2 verification

067f832e matches its remote task branch and draft PR201. Source review covers trade
layout/hitboxes, all gallery description presentation paths and progress refresh,
death-screen confirmation, native dead-player respawn and durable inventory recovery.
The current Java21 build and native/offline regression checks pass. The completed
20261003T095034672Z-5ab234b0 deployment and public current-test publication were recovered
from their receipts, independently reverified, and the installed updater returned
Already current at revision 067f832e. Installed client and TEST SHA256:
536A2913377D4B8793C5F4F20D16BBEA8339D0B9296B89260BA3112E27A5EC2F.
The older CURRENT_STATE statement that this deployment was pending was stale.
No gameplay acceptance is inferred from those receipts.

## Validation and limitations

Java21 native JUnit covers fixed placement order and every fallback, blocked retries,
large bounds, the real native delay mixin, legacy spawner equipment/delay/cap/boss data,
weighted potentials and authored absolute Pos preservation, transient status sync,
independent persisted awareness, and full passenger cap arithmetic. The existing
d1OfflineChecks suite also passes, including 2,575 spawner membership/budget checks.
The final test total, JSON validation, source/JAR hashes, startup and deployment receipts
are recorded in the external task evidence and final handoff. No tests were removed.

No datagen applies: no generated models, recipes, loot or advancements changed.
The historical tracked JAR deletion and unrelated caches/logs are preserved. No
destructive clean or local dedicated/GameTest server launch is authorized or performed.
Native client startup checks mixin loading; licensed gameplay/visual acceptance is pending.

## Compatibility, authority and performance

No existing spawner, preset JSON/NBT, registry ID or save version is renamed or removed.
Existing SpawnRange and authored Pos fields remain serialized for lossless compatibility;
fixed placement ignores them at runtime. Native delay reset, weighted selection, spawn
finalization/position events and authored equipment/drop rules are retained.
Explicit position-event rejection remains effective; solid-block collision cannot be
overridden by an event. Boss one-shot behavior and existing cap limiting remain.
No spawner/preset migration or manual replacement is required, including unloaded old
spawners when they next load. Legacy compatibility checks prove load/save preservation.

CosmicSpawnBlocked exists only in the block-entity update tag, never durable saves.
The additive per-mob cosmicdungeon:spawner_awake flag survives entity save/reload;
missing legacy flags initialize from an existing target, with no world scan. Rollback
can ignore the optional flag. Existing source metadata and authored items stay intact.
TEST deployment takes automatic prior-JAR backups; existing host world backup practice
still applies before testing. No world/config/server.properties changes are made.

All placement, permissions, cap and activation decisions are server-authoritative.
Rendering is client-only; no extra custom packet, runtime dependency or global watcher.
Membership lookups reuse the constant-time live index. Dormant detection checks are
staggered, local and throttled; no new every-tick pathfinding or whole-world scan.
Currency, trading, class items/chests, rifts, doors, progression and reset behavior are
unchanged. Normal mob deaths continue through existing loot and group-reward handlers.

## Licensed TEST acceptance

1. As Dungeoneer (including an operator), try hand equipment changes, equipment commands
   and preset keybinds; all must fail. Repeat as Developer; authored stack components
   and drop settings must survive a save/reload.
2. Obstruct top and each successive position, then all nine. Check exact order, no
   solid-block overlap, red blocked indication and automatic recovery after clearing.
   Repeat with large slime/magma cube, a passenger group, overlapping mobs and a boss.
3. Fight an old wave beyond eight blocks while another spawns. New mobs should wait.
   Approach in sight or directly hit one to activate it. Check cap with both idle and
   fighting mobs, plus reload, separate spawners/instances and ordinary non-spawner AI.

Eight implementation batches remain (4–11): see TESTING_ROLLOUT_20261003.md.
Cumulative licensed acceptance is separate. Future improvement: a configurable
activation-distance control after gameplay balance feedback.

## Exact changed files

- src/main/java/net/goui/cosmicdungeon/mixin/BaseSpawnerAccess.java
- src/main/java/net/goui/cosmicdungeon/block/entity/CosmicSpawnerPlacement.java
- src/main/java/net/goui/cosmicdungeon/block/entity/CosmicSpawnerRuntime.java
- src/main/java/net/goui/cosmicdungeon/block/entity/CosmicSpawnerAwareness.java
- src/main/java/net/goui/cosmicdungeon/mixin/CosmicSpawnerAwarenessMixin.java
- src/main/java/net/goui/cosmicdungeon/block/custom/CosmicMobSpawnerBlock.java
- src/main/java/net/goui/cosmicdungeon/block/entity/CosmicSpawnerBlockEntity.java
- src/main/java/net/goui/cosmicdungeon/block/entity/CosmicSpawnerEntities.java
- src/main/java/net/goui/cosmicdungeon/block/entity/SpawnerMembership.java
- src/main/java/net/goui/cosmicdungeon/client/CosmicSpawnerHoverOverlay.java
- src/main/java/net/goui/cosmicdungeon/client/render/blockentity/CosmicSpawnerRenderer.java
- src/main/java/net/goui/cosmicdungeon/client/screen/settings/CosmicSpawnerHudOptionsScreen.java
- src/main/java/net/goui/cosmicdungeon/command/SpawnerCommand.java
- src/main/resources/cosmicdungeon.mixins.json
- src/test/java/net/goui/cosmicdungeon/block/entity/CosmicSpawnerPolicyTest.java
- docs/Spawners/Spawner_Systems.md
- docs/ai/D1_REMAINING.md
- docs/ai/TESTING_ROLLOUT_20261003.md
- docs/ai/TESTING_BATCH_2_20261003.md
- docs/ai/TESTING_BATCH_3_20261003.md
- docs/ai/tasks/testing-fixes-batch3-20261003.md
- docs/releases/fragments/testing-fixes-batch3-20261003.md

Final automated verification: 101 JUnit tests, zero failures/errors/skips; 1,997 source
JSON files parsed; 22-file allowlist passes git diff --check. Java21 Gradle build passed.
The full offline regression run passed before the final passenger-cap addition; that
addition is covered by the final build's native JUnit test. Native client/deployment
status follows in the external completion receipt; no claim of licensed gameplay QA.
