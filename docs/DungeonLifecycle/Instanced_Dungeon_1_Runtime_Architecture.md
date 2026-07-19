# Instanced Dungeon 1 Runtime Architecture — Phase 0 feasibility ADR

**Audited main:** `5e4f219c42cf4c64081447505704d246867f4e5d` (the current `origin/main` on 2026-07-19).
**Decision status:** **DYNAMIC_UNSUPPORTED_ALTERNATIVE_REQUIRES_APPROVAL**.
**Scope:** evidence and future design only; this ADR changes no runtime, storage, registry, network, resource, or generated data behavior.

## 1. Decision status

A new `ResourceKey<Level>` is not, by itself, a runtime dimension. The audited 1.21.10 server constructs its level map during startup from the registered `LevelStem` set. Phase 0 found no public NeoForge 21.10.64 API that creates/removes a `ServerLevel`, mutates the live level-stem registry, synchronizes a newly added dimension to connected clients, and later closes/removes it. Therefore Option A cannot be authorized. The safest product-compliant alternative is a **bounded, startup-predeclared paired slot pool** (Option B), but accepting its bounded capacity and implementing its complete clone/unload transaction require product-owner approval.

## 2. Executive conclusion

Fresh isolated paired storage is possible in principle only where both physical level keys are registered before startup and remain registered for the process lifetime. A run would lease one predeclared PRIMARY/NETHER slot pair, clone one published paired template snapshot into that pair only while it has no live storage owner, load/reconcile it through normal startup ownership, and route server-side by run membership. This is not an authorization to implement it: live in-process slot unloading/recloning also lacks an audited supported removal API, so the conservative compliant operational model is **allocate slots before server start; delete/reseed only during controlled stopped-server/startup reconciliation**. If product requires immediate same-process slot reuse, this campaign stops until NeoForge supplies a supported lifecycle API or a separately audited engine change is approved.

## 3. Observed defect and PR #181 analysis

**Runtime observation supplied to this task:** `/world reset dungeon_1` restores blocks but post-snapshot monsters survive in the same process; relog does not help; restart does. **Repository evidence:** PR #181 is merge `b97861b2`, with implementation commit `be73c122`; `DungeonWorldSnapshotService` saves, reflects into `ServerChunkCache`/`ChunkMap`, flushes, copies files, clears caches, removes loaded non-player entities, and drains chunks. It imports `Field`/`Method` and accesses private implementation details.

**Engine evidence:** a `ServerLevel` owns a `ServerChunkCache`; `ServerChunkCache.close()` saves then closes `DimensionDataStorage`, light engine, and `ChunkMap`; `ChunkMap.close()` closes worldgen/light dispatchers, POI manager, and storage; `IOWorker.close()` closes its executor and region storage. Those owners remain live during the reset. `ChunkMap` resolves `region`, `poi`, and `data` under the level's dimension path. Replacing files beneath these open owners is consequently unsupported.

**Inference (not a proven single root cause):** an already-created entity manager/chunk map/IO worker can retain entities, pending writes, region handles, tickets, or in-memory chunk state and can write stale state back after copy. Restart succeeds because normal server shutdown closes those owners and startup reconstructs them from disk. The exact surviving owner in the reported reproduction remains unproven; Phase 0 must not claim PR #181's reflection-driven cache invalidation is a safe replacement lifecycle.

## 4. Source/API evidence and option comparison

### Exact inspected artifacts

* Repository coordinates: `gradle.properties` declares Minecraft `1.21.10` and NeoForge `21.10.64`.
* Deobfuscated source artifact inspected: `SourceCode/neoforge-21.10.64-merged.jar/` (the repository's merged Minecraft/NeoForge source tree), including `net.minecraft.server.MinecraftServer`, `server.level.ServerLevel`, `ServerChunkCache`, `ChunkMap`, `ServerPlayer`, `world.level.dimension.LevelStem`, and chunk-storage `IOWorker`, `RegionFileStorage`, and `SectionStorage`.
* No separate public NeoForge runtime-level lifecycle API was found in that exact source tree. This is negative evidence from source inspection, not proof an undocumented external API can never exist.

| Requirement | A: unique dynamic keys | B: predeclared slot pairs | C: other genuine fresh worlds |
| --- | --- | --- | --- |
| Isolation/simultaneous parties | Yes if safely created | Yes, capped by configured slot pairs | No supported alternative found; shared coordinates are rejected |
| Level stems/registry | Requires live registry mutation and client knowledge; unsupported evidence | Data-driven stems registered before startup | Any equivalent still needs registered physical levels |
| Creation/client synchronization | No audited public API; unsafe to use private `MinecraftServer.levels`/registry maps | Existing keys are known at login; no dynamic registry sync | None found |
| In-process unload/removal | No audited public remove/close API | Also unproven for reuse; use stopped-server/startup reclamation | Same issue |
| Filesystem deletion | Prohibited while owner exists | Safe only after verified close/no owner; conservative stopped-server action | Same |
| Restart/crash recovery | Registry availability remains uncertain | Deterministic: slots and stems reconstruct at startup | No better supported mechanism |
| Reflection/engine patching | Required and forbidden | Avoidable for routing; immediate reuse would again require it | No supported candidate |
| Complexity/testability | High/unsafe | High but bounded and testable around policy/staging | Not available |
| Required routing | Possible only after unsupported lifecycle | Compatible with server-side resolver | Not available |

`LevelStem` is a data-driven registry value; that proves startup dimension definitions, not post-start mutation. `MinecraftServer` owns the active level map and constructs levels during startup; `ServerLevel` construction takes server, executor, storage access, level data, key, stem, listener, debug flag, seed sequences, and random-sequence state—proof it is not a trivial `new` operation. The map/constructor access required for A is implementation-dependent/private. No inspected class proves safe connected-client addition/removal. A login/respawn client packet carrying dimensions does not prove a protocol for adding/removing them after connection.

## 5. Proposed instance identity

Future persisted descriptor (not added here): `templateDungeonId="dungeon_1"`; monotonically allocated `runId`; opaque `instanceId`; `primaryPhysicalDimensionId`; `netherPhysicalDimensionId`; explicit `PRIMARY`/`NETHER` role map; immutable published paired `templateSnapshotId`; lifecycle state; owner party/member UUIDs; manifest version and canonical contained paths; creation, state-transition, evacuation, close, deletion-attempt, and cleanup timestamps. The manifest must state the exact slot keys and be required before any deletion; no legacy static record alone authorizes template or slot deletion.

## 6. Lifecycle state machine

`CREATING -> ACTIVE -> ENDING -> DELETE_PENDING ->` removed is the normal sequence. `CREATING`, `ENDING`, or `DELETE_PENDING` may transition to `FAILED` with a diagnostic and no template fallback. Creation checkpoints are identity reservation, manifest durable, both paths staged, both paths validated, physical levels enterable, indexes rebuilt, then ACTIVE. A member may re-enter only ACTIVE and only the descriptor's complete pair. Startup reconciles durable records first: incomplete/unknown ownership becomes FAILED/diagnostic, not automatically deleted; DELETE_PENDING is retried only after ownership/path validation at a safe stopped/startup phase.

## 7. Paired template publication

Future `/world save dungeon_1` must publish one pair, not independent arbitrary live-folder copies. It must gate authored template mutation, evacuate or reject active template use, force the normal server save/chunk save path, validate both canonical levels, capture blocks/block entities and intentionally authored entities from a controlled snapshot representation, and preserve Cosmic Spawner NBT unchanged. Door/key state, rift anchors/tiles/configuration, canonical RD destinations, and a manifest must be captured with role-qualified identifiers. Write to a unique staging directory, validate hashes/counts/role pair, atomically publish the manifest last, retain the prior published pair for rollback, and never publish a half pair. Existing `DungeonWorldSnapshotService` folder copy is evidence of current behavior, not approval to copy under live owners.

## 8. Runtime creation and staging

The required transaction is: reserve CREATING; choose an unused registered slot pair; normalize and containment-check manifest-owned paths; stage both copies outside live paths; write ownership markers; validate pair/hash/role metadata; reconstruct only instance-local indexes; atomically place storage only when no live `ServerLevel`/worker owns either path; load through supported startup ownership; validate both levels and routing; then mark ACTIVE before any player teleport. On failure, do not teleport and retain uncertain data for diagnostics. **Important:** Phase 0 found no source-supported same-running-server `ServerLevel` creation/client synchronization transaction. Thus actual activation is limited to startup until an approved supported API exists.

## 9. Runtime unload and deletion

A future implementation must block entry/writes; evacuate every member including those outside the pair; close containers/session state; clear temporary run state; release forced chunks/tickets; unload chunks/entities; flush and close entity/chunk/POI/structure/region/data IO; remove level ownership; prove no player, server map, cache, worker, or handle remains; remove only instance-owned indexes; and delete only normalized manifest-owned paths. Locked files cause `DELETE_PENDING`, with retries only in a safe startup/stopped phase. `ServerChunkCache.close`, `ChunkMap.close`, `IOWorker.close`, `RegionFileStorage.close`, and `SectionStorage.close` show why deletion before closure is forbidden. Windows can refuse open handles; Linux unlink semantics do not make deletion safe because an open descriptor can still reference/defer writes. No inspected public API proves the required removal/closure sequence for one live level.

## 10. Canonical versus physical destination model

Keep stable canonical records: canonical template dungeon ID, logical role, authored position/orientation. Server resolver inputs are authenticated player, lifecycle membership, physical-origin ownership, destination record, and an explicit AccessPolicy-approved developer template-bypass context. It resolves a physical `ServerLevel` only after membership/descriptor/role checks and returns structured `Success`, `Denied`, `RecoveryRequired`, or `Unavailable`; the client never supplies an instance ID/key. Origin ownership mismatching membership fails closed.

## 11. Mandatory routing matrix

| Situation | Required result |
| --- | --- |
| Active PRIMARY -> canonical D1 Nether | own NETHER |
| Active NETHER -> canonical D1 | own PRIMARY |
| Active -> external | external unchanged; membership retained |
| Active outside pair -> canonical D1 / Nether | own PRIMARY / NETHER |
| Party A/B use one external return rift | resolver returns each party's own pair |
| Nonmember uses canonical D1 destination | authored template destination |
| Explicit AccessPolicy developer bypass | deliberate canonical template destination |
| Creating/ending/delete-pending/failed/missing/partial | deny/recover; never template fallback |
| Origin instance A, membership B/none | fail closed and recover |
| Nonmember physically in disposable instance | no implicit membership |
| Run ends while member is external | revoke return, evacuate/recover member |

## 12. Rift and RD model

Canonical destination names remain stable authoring identifiers; physical instance resolution happens server-side. `RiftRegistryData` currently persists `cosmicdungeon_rifts_v2` and has a legacy loader, destinations, and portal records; future instance-qualified portal/tile indexes likely require an additive schema/manifest-index design and migration review. Derive indexes from the paired snapshot and role/instance descriptor, rebuild on startup, and delete only records matching that descriptor. Template portals and legacy destinations remain canonical. Reject persistent authoring inside disposable worlds unless explicitly instance-scoped. No payload or client-provided string may name a physical instance.

## 13. Access Policy and template access

Ordinary active routing is membership-based and server authoritative. Nonmembers retain canonical authored routing. Template access requires a separate explicit diagnostic/bypass intent checked through `AccessPolicy`; developer rank/`Authority` alone must not turn ordinary rifts into a bypass. Future design should choose `AccessPolicy` as the one authoritative destination authorization boundary and have commands/rifts call it; this is a recommendation, not an implementation. All unavailable/ambiguous paths deny with server diagnostics.

## 14. Persistence and migration

Future work must add backward-compatible fields to `DungeonRunRegistryData` (`cosmicdungeon_dungeon_runs`): descriptors, roles, snapshot ID, lifecycle checkpoints, ownership manifest reference, and player lookup semantics. Existing `dungeon_dimension_ids`, static run records/states, `PendingDungeonRecoveryData`, `DungeonRunProgressData` bloom/completion data, door records, selector state, and possible `RiftRegistryData` indexes require an explicit old-shape loader and round-trip tests. Preserve existing SavedData IDs/field names; old static records are template references only and **must never grant deletion authority**. Phase 0 changes no schema, ID, NBT, migration, Cosmic Spawner format, preset, or player data.

## 15. Crash and restart recovery

At startup: inspect manifests and registry first; validate canonical containment and paired completeness; reconcile CREATING staging, post-folder/pre-load, post-load/pre-teleport, ACTIVE, evacuation-before-delete, locked DELETE_PENDING, orphan, missing/partial pair, stale/missing portal index, and externally located members. Known manifest-owned incomplete data may be quarantined/diagnosed; uncertain ownership is never automatically deleted. Active runs reconstruct slots using startup-registered keys; failed/missing storage denies re-entry and schedules member recovery. Restart is the only source-supported reconstruction boundary found.

## 16. Command contract

Future contracts: `/world save dungeon_1` publishes one validated paired template snapshot; `/world reset dungeon_1` remains an explicit canonical-template operation and must reject ambiguity/active instances; `/world runs [dungeon_1]` lists descriptors/states; termination/reset requires one run ID; routing diagnostics reports canonical input, membership, resolved role/key, and denial reason; template access is explicit and AccessPolicy-gated; orphan cleanup requires verified manifest ownership and should default to diagnostics. Commands must never fan out to multiple instances implicitly.

## 17. Exact future serial PR decomposition

1. **“Define paired-instance persistence contract”** (`codex/d1-instance-contract`): codecs/migrations/tests for descriptors, manifest references, and legacy safe loader. Owns SavedData/migration hotspots; depends on this approval; forbids registry/routing/world IO; build/GameTests plus codec tests; dedicated migration backup QA; release fragment required. Must precede all consumers.
2. **“Canonical Dungeon 1 destination resolver”** (`codex/d1-routing-policy`): pure server policy, all direct rift/RD/class-selector/reset-return paths, AccessPolicy boundary, diagnostics. Owns rift/RD/teleport/access hotspots; depends on 1; forbids level lifecycle; pure tests/GameTests and multiplayer QA; fragment required. Cannot parallelize with 1 or lifecycle work.
3. **“Publish paired template snapshots”** (`codex/d1-paired-template-publication`): manifest/staging/validation/rollback while preserving spawner NBT. Owns dungeon snapshot/persistence hotspots; depends on 1; forbids runtime instance creation; filesystem tests and dedicated-server save QA; fragment required. Serial with 1 and 4.
4. **“Predeclared Dungeon 1 slot-pair bootstrap”** (`codex/d1-slot-pool-bootstrap`): data-driven declared slots and startup reconciliation only. Owns dimension registration/server startup hotspot; depends on explicit product approval and 1/3; forbids dynamic registry mutation and in-process reuse; integration/dedicated client login QA; fragment required. Cannot parallelize with any registry/startup task.
5. **“Paired-instance staging and startup activation”** (`codex/d1-instance-staging`): CREATING/ACTIVE transaction, indexes, recovery. Owns dungeon/rift persistence and startup lifecycle; depends on 2–4; forbids deletion/reuse; filesystem tests, restart and multi-client QA; fragment required.
6. **“Run ending and conservative reclamation”** (`codex/d1-instance-reclamation`): ENDING/DELETE_PENDING, evacuation/session cleanup, stopped-server deletion. Owns lifecycle/rift/access hotspots; depends on 5; forbids reflection/private map mutation; locked-file and crash QA; fragment required. Same-process reuse remains forbidden unless a later source-supported API audit changes the decision.

## 18. Test strategy

Pure deterministic tests cover identity, role routing, all matrix denials, and containment. Codec/migration tests load every old run/rift shape and prove static records cannot delete templates. Filesystem tests cover staging, atomic manifest publish, hash validation, traversal/symlink rejection, and uncertain-orphan preservation. GameTests can cover in-server policy/teleport behavior but cannot honestly prove connected-client dynamic dimension protocol, OS locks, crash timing, or private-engine lifecycle. Real dedicated-server tests need two parties, external return rifts, restart during each checkpoint, Windows/Linux locks, client joins before/after activation, disconnect/death/logout, and server stop. Manual evidence remains required for client disappearance and live-world backup/restore.

## 19. Approval gates and unresolved decisions

Product owner must approve the bounded predeclared-slot compromise (capacity, restart-only activation/reuse, operational downtime) or reject it. Decide completion-exited re-entry semantics, legacy active-run recovery, verified-orphan retention/deletion policy, template bypass authorization/audit, and whether the campaign stops permanently without supported same-process lifecycle APIs. Dynamic A is not silently replaced by B.

## 20. Backup, update, rollback, and campaign stop conditions

Before any future migration, back up complete world folders, template snapshots, rift/RD data, doors, Cosmic Spawners/presets, player data, and `cosmicdungeon_*` SavedData. Existing 1.5.0/1.5.1 templates, static dimensions, rifts, RD destinations, doors, spawners, presets, player data, and legacy records remain untouched by this ADR. Upgrade must retain old readers and use manifests before deleting anything; rollback must restore the backup and retain uncertain instance folders. Stop the campaign if source-supported server/client creation/removal/closure cannot be demonstrated, if ownership is uncertain, if canonical containment fails, or if tests show a path can enter a template/other party's pair.

## Repository audit inventory and future write hotspots

Current static definitions are `DungeonDefinition`, `DungeonDefinitions`, `DungeonRunState`, `DungeonRunRegistryData`, `DungeonLifecycleService`, `DungeonWorldSnapshotService`, `PendingDungeonRecoveryData`, `WorldCommand`, `RiftRegistryData`, `SafeTeleportUtil`, `DoorPassageTracker`, `DoorLockData`, and `DoorPassageData`. Lifecycle events/respawn code, class-selector destination command/block paths, `CosmicRiftTileBlock`, `RiftCommand`, `RiftDestinationCommand`, reset-trigger rifts, progression/Plant Flags/Lesser Bloom/achievement paths, party leadership/kicking, disconnect/death/logout/dimension-change/server-stop listeners, `CosmicDungeonMod`, networking/client bootstrap, and dedicated-server classloading boundaries are future read/write audit hotspots.

Known static assumptions: `DungeonDefinitions.DUNGEON_1` fixes `cosmicdungeon:dungeon_1` and `cosmicdungeon:dungeon_1_nether`; run registry stores `dungeon_dimension_ids`; `findActiveOrResettingRun(dungeonId)` and `hasActiveOrResettingRun` impose one run per dungeon; lifecycle evacuation and recovery use physical dimension containment; snapshot service resolves the static definition's levels. Direct destinations include rift/RD teleports, class-selector destination resolution, `WorldCommand` RD teleport, safe teleport helpers, lifecycle evacuation/recovery, and reset rifts; each requires the future resolver audit before implementation.
