# Fresh dungeon instances — 2026-09-25

User authority: Cameron explicitly requests a full copy of the latest `/world save dungeon_1` snapshot for every party, never-reused increasing world IDs, and complete disposal after forfeit. Copy latency is accepted.

Dependent branch: `fix/fresh-dungeon-instances-20260925`, based on `feature/dungeon-forfeit-20260925` at cb426390. No main merge or production deployment.

## Scope and owned integration hotspots

This task owns dungeon snapshots and reset/lifecycle orchestration, DungeonRunRegistryData, instance travel/rift mappings, Watson placement, class-selector entry cleanup, and the common server-access mixin configuration. No concurrent task owns these files. Supporting classes: DungeonSnapshotFiles, DungeonInstanceWorlds, DungeonServerAccess; focused filesystem and codec/routing tests accompany the change.

Required behavior: flush and atomically publish an authored snapshot including entities/POI; reserve the next run ID durably; copy all snapshot dimension files into nonexistent paths; construct new native ServerLevels; retire them only after existing player inventory and companion recovery permits it. Failed preparation retains a durable empty reservation for safe cleanup. Interrupted retirement must never regenerate a missing directory on restart.

## Persistence and compatibility

The existing next_run_id counter and actual dungeon_dimension_ids remain authoritative. PREPARING is a new state with an empty roster; optional retiring_instances defaults to an empty list when loading existing saves. No item, block, entity, dimension-type, spawner or inventory identifiers/fields are renamed. Existing fixed-slot run records remain readable. Existing snapshots remain readable; incomplete new saves use hidden staging directories excluded from selection/pruning. Class chest contents are copied without mutation.

A full world backup rollback also rolls back its run counter; this is not a globally external ID service. Existing conflicting directories must be refused, never merged or overwritten. Back up the complete server world before updating. Drain all runs and retain a pre-update backup before downgrading; the older build does not understand runtime-created dimensions or preparation/retirement records.

Concurrency stays at ten runs. File-copy latency is accepted; failed cleanup retains a slot and reports the error instead of silently recycling it. No new runtime dependency or client packet is introduced; native respawn packets carry the new dimension key with an existing registered dimension type.

## Validation plan

- Java 21 build/JUnit; filesystem copy independence for blocks/entities/POI/data, refusal of existing targets, snapshot publication failure, ID reservation/cancellation/restart, legacy codec defaults and dimension routing.
- Existing native inventory handoff fixtures and GameTest registry serialization must remain passing.
- Datagen is not applicable: no generated resources change. Preserve historic tracked JAR and unrelated caches.
- Native GameTest/dedicated launch remains separately gated by AGENTS.md. A build is not live-world acceptance.
- TEST QA: save a named villager; enter/forfeit/re-enter; prove unique F3 dimension keys, villager returns and old zombies/dog do not; repeat after server restart; verify overworld/nether rifts, doors/spawners and Watson; test two concurrent parties; repeat original-inventory recovery and offline-owner cleanup.

## Completion and remaining QA

Implementation/review batches remaining for this request: **0**. Licensed gameplay acceptance and deployment remain separate.

Java 21 builds passed; 54 JUnit tests passed (0 failed, 0 skipped), including 12 new filesystem/generation checks, existing 683 inventory handoff assertions and native GameTest registry serialization. All 1,997 src JSON files parsed. Final build and Git provenance receipts are retained in the external task backup folder. No datagen, destructive clean, dedicated/GameTest server launch or gameplay was performed.

Reviewed boundaries: access checks and classes remain server authoritative; physical-to-template mappings now use the stored run; rifts and Watson use actual physical keys; doors, regions, spawners and achievements continue through that shared translation. Inventory handoffs, pending Chop journeys, companion archives and death-currency proof guard retirement. No chest stacks, entity/spawner NBT, item payloads, currency amounts, or client packet codecs are rewritten. The native level constructor and close path still require licensed TEST runtime validation.

Existing active fixed-slot runs are allowed to finish and are retired by closing their worlds. The older slot-zero migration path and explicit template maintenance reset are retained for compatibility; ordinary new-party entry and instance retirement no longer hot-restore a loaded world. Failed cleanup holds its slot and logs its cause. Empty preparation cleanup is distinct from player recovery and validates that no player roster or inventory image exists.

SFTP inventory: 2,046 log/crash files tracked; changed latest/debug data inspected through 2026-09-25T17:34Z. Earlier repeated POI mismatch errors correlate with old instance preparation. No new WARN/ERROR/FATAL entries appeared in the final delta (1 latest line, 7 debug lines); no new crash report. That does not resolve the earlier unrelated 08:07 connection-reset diagnosis.

TEST server and licensed client were still running during validation. No deployed JAR or live world has been changed by this task. Fresh stopped-server/closed-client confirmation is required by AGENTS.md before applying the matching build to both destinations. The running client cannot display this build; do not start a duplicate runClient. Full-world backup and short snapshot/re-entry/restart/multiplayer checks remain mandatory before promoting beyond TEST.

Potential future improvement: expose the source snapshot and physical run ID in developer diagnostics.

## Exact changed files

- `docs/ai/tasks/fresh-dungeon-instances-20260925.md`
- `docs/releases/fragments/fresh-dungeon-instances-20260925.md`
- `src/main/java/net/goui/cosmicdungeon/block/custom/ClassSelectorEntryService.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceSlots.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceWorlds.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleEvents.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleService.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonRunRegistryData.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonRunState.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonSnapshotFiles.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonTravelRouter.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonWorldSnapshotService.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1WatsonData.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1WatsonService.java`
- `src/main/java/net/goui/cosmicdungeon/mixin/DungeonServerAccess.java`
- `src/main/java/net/goui/cosmicdungeon/rift/RiftRegistryData.java`
- `src/main/resources/cosmicdungeon.mixins.json`
- `src/test/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGenerationTest.java`
- `src/test/java/net/goui/cosmicdungeon/dungeon/DungeonSnapshotFilesTest.java`
