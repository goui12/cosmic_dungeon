# Instanced Dungeon 1 Runtime Architecture — Phase 0 feasibility ADR

**Audited `origin/main`:** `5e4f219c42cf4c64081447505704d246867f4e5d` (2026-07-19).
**Decision:** **DYNAMIC_UNSUPPORTED_ALTERNATIVE_REQUIRES_APPROVAL**.
**Correction scope:** documentation only. No runtime, registry, SavedData, network, resource, generated-data, command, or filesystem behavior changes.

## 1. Decision status and executive conclusion

The approved per-run dynamic-key architecture (A) is **not authorized**. Exact 1.21.10/NeoForge 21.10.64 source proves startup construction and shutdown closure of server levels, but does not prove a supported lifecycle to add a new level stem/key and `ServerLevel` to an already running server, synchronize that registry to connected clients, independently close/remove the level, then delete its storage. The only public map exposure found is NeoForge's mutable world map accessor; using it to invent a lifecycle is implementation-dependent and does not supply construction, client synchronization, or closure sequencing.

**Coherent fallback assessed:** predeclare a finite pair pool in datapack/registry before startup; a mod handler for the supported **`ServerAboutToStartEvent`** reconciles verified startup-readable manifests, deletes/reseeds only verified slot paths, and writes slot metadata before `loadLevel()`; the next `createLevels()` opens every declared slot; parties lease a clean pair once; ended pairs are quarantined/inaccessible until shutdown; no slot is reused in that uptime. This is an in-mod startup provisioning model, not runtime level creation or runtime reuse. It requires a restart after every consumed pair before reuse, but **does not require an external preparation tool**. It therefore **does not satisfy the approved no-restart deletion/reuse requirement**. Product approval is required before any fallback PR.

## 2. Reproducible source/API evidence matrix

**Artifact/search scope.** Inspected repository `gradle.properties` (`minecraft_version=1.21.10`, `neo_version=21.10.64`) and repository-local `SourceCode/neoforge-21.10.64-merged.jar/`. Search covered `net.minecraft.server`, `server.level`, `world.level.storage`, `world.level.chunk.storage`, `world.level.entity`, `network.protocol.game`, and `net.neoforged.neoforge.{server,event,common}`; queried `createLevels`, `levels`, `ServerLevel`, `close`, `LevelStem`, `LevelStorage`, `PersistentEntity`, `IOWorker`, `PortalForcer`, `ticket`, `LevelEvent`, and server lifecycle events. “Not found” below means this exact source tree and these class families were searched; it is not evidence of an API outside the artifact.

| Subject / exact source member | Access / lifecycle | Proves | Does **not** prove / reliance |
| --- | --- | --- | --- |
| `net.minecraft.server.MinecraftServer#createLevels(): void`, lines 400–470; `Registry<LevelStem>` iteration and `new ServerLevel(...)` | `protected`; startup after `initServer` invokes it | All registered non-overworld `LevelStem` entries are iterated and inserted into `levels`; predeclared slots become live owners at startup | No safe post-start call, no client sync, no individual removal. Calling it again is unsupported/implementation-dependent |
| `MinecraftServer#levels: Map<ResourceKey<Level>, ServerLevel>`, line 223; `getLevel(ResourceKey<Level>)`, `getAllLevels()` lines 1271/1279 | field `private`; accessors `public` read only | Server owns active levels | Direct mutation requires reflection; `forgeGetWorldMap(): synchronized Map<...>` lines 1934–35 exposes the map but does not document or implement a complete add/remove lifecycle |
| `MinecraftServer#createLevels` calls `new ServerLevel(...)` at lines 411/455 then `levels.put(...)`, `LevelEvent.Load` | protected startup code | Level load events occur **after** construction/storage ownership | No before-each-level event/hook for safely replacing its dimension tree |
| `ServerLevel#ServerLevel(MinecraftServer, Executor, LevelStorageAccess, ServerLevelData, ResourceKey<Level>, LevelStem, ChunkProgressListener, boolean, long, List<CustomSpawner>, boolean, RandomSequences)` lines 218–300 | `public` constructor but engine-owned dependency graph | Required server, executor, storage, data, stem, listener, seed, spawners/random services | Public constructor is not a supported registration API; it does not update server/client/portal/player lifecycle |
| `LevelStorageSource.LevelStorageAccess#getDimensionPath(ResourceKey<Level>)` used by `ServerLevel` line 241, `ServerChunkCache` line 97, `ChunkMap` lines 180/184 | public usage; construction-time | Storage path is opened/resolved while level is constructed | `ServerAboutToStartEvent` is before `loadLevel`/`createLevels`; it is a supported pre-level-load hook, subject to a startup-readable manifest |
| `ServerLevel#chunkSource`; `ServerChunkCache#close(): void`, lines 323–327 | field inherited/internal; close `public` | cache saves, closes `DimensionDataStorage`, light engine, `ChunkMap` | Independent close while still in `MinecraftServer.levels` is not a supported lifecycle |
| `ChunkMap#close(): void`, lines 438–445 | `public`; called from cache close | closes worldgen/light dispatchers, POI manager, parent chunk storage | Does not remove tickets, players, server map ownership, entity manager |
| `ServerLevel#entityManager: PersistentEntitySectionManager<Entity>`, lines 193/241–249; `ServerLevel#close()` lines 1787–89 | field `private`; close `public` | entity storage under `dimension/entities`; close eventually closes entity manager | No public accessor/independent orchestration contract; live memory can remain until close |
| `IOWorker#synchronize(boolean): CompletableFuture<Void>` lines 154–166; `close()` lines 238–246 | public | pending writes may be flushed; close executor and `RegionFileStorage` | A flush does not retire the owner or rebuild it from replaced files |
| `RegionFileStorage#close()` lines 93–103; `SectionStorage#close()` lines 301–302 | public | cached region files / section storage close | Only after owner invokes close |
| `ServerChunkCache` data storage (`dimension/data`); `ChunkMap` POI (`dimension/poi`) and region storage; structure manager supplied to constructor | constructor-owned | data/POI/chunk/structure services bind to a level path | Exact standalone structure-close API was not located; conclusion is **unproven**, not assumed |
| `ServerLevel#getForcedChunks`, `ServerChunkCache` distance manager/tickets; `ServerLevel#portalForcer: PortalForcer`, lines 198/264 and `getPortalForcer()` line 1311 | public getters / private owner | forced chunks/tickets and portal state are level-associated cleanup concerns | No audited public one-level teardown sequence proves their independent release |
| `MinecraftServer#close(): void`, lines 655–710, loops `getAllLevels()`, posts `LevelEvent.Unload`, calls `ServerLevel.close()` | `public`; full server shutdown | Normal close sequence is global shutdown, then storage source close | No public `removeLevel`/`closeLevel` API was found |
| `ServerLifecycleHooks#handleServerAboutToStart(MinecraftServer)` line 89 posts `ServerAboutToStartEvent`; `DedicatedServer#initServer` lines 273–275 and `IntegratedServer#initServer` lines 80–81 call it before `loadLevel()`; `MinecraftServer#loadLevel(): void` lines 375–381 calls `createLevels()` | public static hook/event; `loadLevel` protected | Supported pre-level-load hook on dedicated and integrated servers. Event Javadoc says before the server begins loading anything. `MinecraftServer#getWorldPath(LevelResource): Path` is public (line 2096); `DimensionType#getStorageFolder(ResourceKey<Level>, Path): Path` is public (line 133) | Does not make ordinary level SavedData available before levels load; startup must use a separate validated manifest. It does not provide runtime add/remove/reuse |
| Client login classes searched: `ClientboundLoginPacket`, `ClientboundRespawnPacket`, `ClientboundPlayerPositionPacket`; change uses `ServerPlayer#teleportTo(ServerLevel,...)` | protocol classes / public teleport | login/respawn carries known dimension context; teleport changes player level | No post-login registry/LevelStem-add or level-forget protocol/API found; connected-client dynamic addition/removal is **unproven** |
| NeoForge runtime API search: `DimensionManager`, `createLevel`, `removeLevel`, `unloadLevel`, `registerLevel` across `net.neoforged.neoforge` | source search | no such supported runtime lifecycle API located | Absence is scoped negative evidence; do not infer API exists |

**Private/reflective surfaces dynamic code would otherwise need:** `MinecraftServer.levels`; registry internals for level stems/dimension types; `ServerChunkCache.chunkMap`; `ChunkMap.visibleChunkMap`; entity/chunk storage caches. These are forbidden as lifecycle mechanisms. `DungeonWorldSnapshotService` already uses reflective access to the latter two; that is evidence of fragility, not precedent.

## 3. Option comparison and coherent Option B lifecycle

| Option | Verdict | Exact lifecycle / compromises |
| --- | --- | --- |
| A. Unique keys per run | Unsupported | Requires new stem/key after startup, live map mutation, connection registry sync, and independent removal/close; no complete supported API evidence |
| B. Bounded predeclared pairs | Requires approval; operationally changed | In-mod `ServerAboutToStartEvent` verifies/deletes/reseeds slots before `loadLevel`/`createLevels`; every stem then becomes live. One lease per uptime; runtime quarantine; restart-only reclamation. Not a no-restart solution |
| C. Other fresh isolated world | No source-supported candidate | Shared coordinates rejected; any genuine world still needs a registered live level and the same lifecycle problem |

**Answer to the required B questions.** (1) Yes: `createLevels` iterates stems and constructs all. (2) During `ServerLevel` construction, when entity/cache/map constructors resolve dimension paths. (3) Yes: `ServerAboutToStartEvent` is before `loadLevel`; (4–5) the mod can derive root/path via public APIs and copy/delete only manifest-verified slot trees before slot owners exist, but normal level SavedData is not yet available, so a separate startup-readable ownership/run manifest is required. (6) A snapshot published after startup cannot prepare a clean live slot without restart; no. (7) A preseeded, already-loaded slot can be leased after startup only as a normal already-existing world; routing and starter-room/index work are future server policy/normal gameplay changes, but fresh-copy provenance must have been established before startup. (8) normal block/index writes are possible but do not prove transaction safety; future implementation must test. (9) quarantine/inaccessibility until restart is policy-feasible, not engine cleanup. (10) no supported same-process close/wipe/reuse. (11) thus single-use per uptime. (12) initial seed: one pre-start preparation/start; new snapshot: stop, seed, start; reclaim: stop, verify/delete/seed, start; crash: stop/start reconciliation; locked cleanup: stopped retry, then start. (13) no; (14) yes for startup filesystem provisioning, provided the manifest is outside ordinary level SavedData and path/ownership validation succeeds. (15) when exhausted, deny new runs until an operator performs the stopped-server cycle.

## 4. Product requirements delta

| Approved criterion | Restart-only preseeded slots verdict |
| --- | --- |
| Fresh isolated paired worlds / distinct roles | Supported with bounded capacity; seeded in-mod at next startup |
| Two concurrent parties | Supported with bounded capacity: at least two preseeded pairs |
| Same-pair rift/RD, shared external return, template protection, no cross-instance access | Still unproven pending resolver PR; architecture can support it |
| Completion while members are external | Still unproven pending persisted membership/recovery policy |
| Same-process unload/deletion; monsters disappear with deleted instance | Unsupported |
| No required restart; capacity reuse | Unsupported / operationally changed |
| Crash recovery / active-run restart | Supported only after restart, but full reconciliation implementation remains unproven |
| Existing-world compatibility | Still unproven until migration PR; ADR requires additive loader |
| Client synchronization | Supported for predeclared login-time levels; dynamic update unsupported/unproven |
| In-mod startup seeding / external tooling | Supported in mod at `ServerAboutToStartEvent`; external tooling unnecessary (optional offline backup aid only) |
| No reflection / no engine patch | Supported by this fallback if it uses public path APIs and validated manifests |
| Dedicated / integrated server | Still unproven; both use startup construction but require separate QA |

## 5. PR #181 failure analysis

PR #181 implementation (`DungeonWorldSnapshotService`) calls `MinecraftServer.saveEverything`, `ServerLevel.save`, `ServerChunkCache.save`, then `prepareLevelForFilesystemRestore`, `clearForcedChunks`, `driveUnloadPasses`, `flushChunkIoWorker`, `invalidateChunkAndEntityIoCaches`, `clearRuntimeChunkAccessCaches`, `purgeLoadedNonPlayerEntities`, and `enforcePostRestoreChunkDrain`. It reflects `ServerChunkCache.chunkMap`, `ChunkMap.visibleChunkMap`, holder `getLatestChunk`/`getTickingChunk`/`getChunkToSend`, and chunk-map worker/cache members via `Field#setAccessible`/`Method#invoke`.

**Proven:** these operations do not call `ServerLevel.close`, `PersistentEntitySectionManager.close`, `ServerChunkCache.close`, `ChunkMap.close`, `IOWorker.close`, `RegionFileStorage.close`, or remove the level from `MinecraftServer.levels`; relogging changes a player connection, not the live `ServerLevel`; full restart runs the server-wide close sequence and reconstructs levels. **Possible remaining writers/state:** `IOWorker` pending writes, `ChunkMap`/POI/chunk caches, entity manager sections/entities, tickets/forced chunks, and portal state. **Unproven:** which one produced the reported surviving monster. Therefore another cache-clearing patch is prohibited: it would again mutate private internals without retiring every owner.

## 6. Current repository call graph and routing ownership

All listed paths exist on audited main. “Resolver” means future canonical destination resolver; “No physical select” is always Yes for player-facing paths.

| Path / class / exact method | Current behavior and assumption | Future PR / resolver |
| --- | --- | --- |
| `dungeon/DungeonDefinition.java` `containsDimension`; `DungeonDefinitions.java` `DUNGEON_1`, `byDimension` | static canonical keys; physical location denotes dungeon | persistence/routing; Yes / Yes |
| `DungeonRunState.java`; `DungeonRunRegistryData.java` `RunRecord`, `startRun`, `findActiveOrResettingRun`, `hasActiveOrResettingRun` | static `dungeon_dimension_ids`; one active/resetting run | contract PR; Yes / Yes |
| `DungeonLifecycleService.java` `startRun`, `kickRunMember`, `onPlayerExitedThroughResetRift`, `performPendingRecoveryIfNeeded`, `finishRun`, `evacuatePlayersInDungeon`, `applyRecoveryToLivePlayer`, `teleportToSafeOverworld` | membership/physical containment; direct `teleportTo`; party/kick/recovery | routing/reclamation; Yes / Yes |
| `DungeonWorldSnapshotService.java` `saveSnapshot`, `resetToSnapshot`, reflection helpers | copies static template paths beneath live owners | publication; forbidden from instance physical selection |
| `PendingDungeonRecoveryData.java` `RecoveryRecord`; `DungeonRunProgressData.java` `BloomMaskRecord`, `CompletionRecord` | recovery and bloom/completion keyed to static run | contract PR; Yes / Yes |
| `command/WorldCommand.java` RD teleport lambda, `save`, `reset`, `runs` | direct target-level teleport and static dungeon commands | routing/command PR; Yes / Yes |
| `block/custom/ClassSelectorBlock.java`; `block/entity/ClassSelectorBlockEntity.java`; `ClassSelectorReadyManager.java`; `ClassSelectorTeleportUtil.java` | selector starts party/teleports configured destination | routing PR; Yes / Yes |
| `DungeonStarterRoomPaster.java` `pasteStarterRoom` | pastes into selected dungeon level | staging PR; no direct resolver, no physical select |
| `rift/RiftRegistryData.java` `DestinationRecord`, `PortalRecord`, `getDestination`; `block/custom/CosmicRiftTileBlock.java` use/teleport path | global canonical RD/portal records | routing/rift PR; Yes / Yes |
| `command/RiftCommand.java`; `RiftDestinationCommand.java`; `ClassSelectorDestinationCommand.java` | create/configure portal/RD names | rift PR; command validation / Yes |
| `rift/SafeTeleportUtil.java` `findSafeTeleportPos`, `teleportSafely` | direct `ServerPlayer.teleportTo` helper | must receive resolved level only / Yes |
| `door/DoorPassageTracker.java`; `DoorLockData.java`; `DoorPassageData.java` | persistent door/passage state keyed by existing world context | contract/reclamation; not resolver / no physical select |
| `auth/AccessPolicy.java`; `Authority.java` | permission/rank checks | routing PR; AccessPolicy authoritative / Yes |
| `DungeonLifecycleEvents.java`, `DungeonRespawnEvents.java`, `DungeonGroupSplitEvents.java`, `ClassCloneEvents.java`, `CosmicDungeonMod.java` server events | login/logout/death/respawn/dimension/server lifecycle registrations | recovery PR; Yes where teleporting |
| `potion/CompanionshipTeleportService.java`; `DefaultRiftDestinations.java#teleportToMainVillage`; `progression/ProgressionService.java`; `achievement/plantflags/*`, `advancement/BloomSharedAdvancements.java` | external travel/progression/Plant Flags/Lesser Bloom/achievements | routing contract must preserve membership; no physical selection |
| `network/ModNetwork.java`, `client/ModNetworkClient.java`, `client/CosmicDungeonClient.java` | payload/client bootstrap; no audited runtime dimension sync | network only if future contract needs it; client never selects physical key |

**Direct `ServerPlayer.teleportTo` call sites identified:** `WorldCommand`, `DungeonLifecycleService`, `SafeTeleportUtil`, and `CompanionshipTeleportService`. Any future call resolving canonical Dungeon 1/D1 Nether must call the resolver; none may independently choose an instance.

## 7. Persistence and migration inventory

| Surface | Current ID/fields/default/legacy | Proposed additive data and loader/failure/test | Authority for deletion |
| --- | --- | --- | --- |
| `DungeonRunRegistryData.java` | `cosmicdungeon_dungeon_runs`; `next_run_id`, runs: run/dungeon/dimension IDs/state/members/leader/reason/started/completion exits/snapshots; optional defaults; no version | descriptor, role map, snapshot/manifest IDs, checkpoints; old loader produces legacy-template-only record; malformed -> FAILED/diagnostic; codec round trip/migration tests | **Never** legacy IDs; only compatible run + validated manifest |
| `PendingDungeonRecoveryData.java` | `cosmicdungeon_pending_dungeon_recovery`; recovery entries optional list | run/instance checkpoint reference; old recovery remains safe recovery; test old/new | never |
| `DungeonRunProgressData.java` | `cosmicdungeon_dungeon_progress_v1`; bloom masks, completions default lists | instance-aware derived linkage only if needed; old preserved; tests | never |
| `RiftRegistryData.java` | `cosmicdungeon_rifts_v2`; destinations/portals; legacy `cosmicdungeon_rifts` loader | derived instance index keyed by manifest/role, rebuildable; old canonical records preserved; migration/rebuild tests | **never**; derived indexes never authorize deletion |
| `DoorLockData.java`, `DoorPassageData.java`, `DoorPassageTracker.java` | existing door SavedData names/records; no audited version | descriptor-qualified data or derived cleanup rules; old data retained; tests | never |
| selector/class player NBT (`ClassNbtUtil.java`, `DungeonLifecycleService` temporary tags) | player root `cosmicdungeon`; `class_id`, `extra`, temporary run keys | membership pointer only with recovery fallback; clone/logout tests | never |
| future instance ownership manifest | new, versioned, exact normalized pair paths, slot keys, hashes, run/template IDs | strict parser; missing/mismatch -> retain/quarantine/diagnose; containment/symlink tests | **only** validated manifest + compatible run may authorize cleanup |
| future template snapshot manifest | new immutable paired snapshot hash/roles | atomic publish/rollback; invalid -> no activation; pair tests | never authorizes deletion |

Uncertain data is retained and diagnosed. Legacy static dimension IDs never authorize template deletion. Derived rift indexes never authorize filesystem deletion.

## 8. Future serial task records

All future tasks require explicit product choice; launch/merge order is listed and adjacent tasks are serial because they share persistence, routing, lifecycle, or registry hotspots.

### 1. Paired-instance persistence contract — `codex/d1-instance-contract`
* **Behavior/dependencies/order:** additive descriptors/manifests and safe legacy reader; depends on approval, launch/merge 1, cannot parallelize with tasks 2–6 (their schema consumers).
* **Files/hotspots/forbidden:** `dungeon/DungeonRunRegistryData.java`, `PendingDungeonRecoveryData.java`, tests; owns SavedData/migration; possible `CosmicDungeonMod`; forbids registry, rift routing, world IO, commands, client.
* **Effects/risk:** SavedData+migration HIGH; no registry/network; shared jar server/client safe.
* **Validation:** `./gradlew clean build`; deterministic codec old/new/invalid tests; `./gradlew runGameTestServer`; JSON scan; no datagen; dedicated backup/upgrade/rollback QA; release fragment required.

### 2. Canonical destination policy — `codex/d1-routing-policy`
* **Behavior/dependencies/order:** resolver and fail-closed policy; depends 1, launch/merge 2; cannot parallelize with 1/3/5/6, owns rift/RD/teleport/AccessPolicy hotspot.
* **Files/hotspots/forbidden:** `rift/*`, selector paths, `WorldCommand`, `AccessPolicy`; possible `ModNetwork`; forbids level creation/deletion, registry, migration format changes.
* **Effects/risk:** server authorization/network audit HIGH; no registry; deterministic matrix tests + build/GameTests/JSON scan, no datagen; dedicated two-party/external-rift/death/logout QA; fragment required.

### 3. Paired template publisher — `codex/d1-paired-template-publication`
* **Behavior/dependencies/order:** atomic pair manifest publication; depends 1, launch/merge 3; cannot parallelize with 4/5/6 due snapshot/lifecycle ownership.
* **Files/hotspots/forbidden:** `DungeonWorldSnapshotService`, commands/tests; owns snapshot/persistence; forbids runtime slots, registry, client/network.
* **Effects/risk/validation:** filesystem persistence HIGH; build/GameTests, deterministic atomic/rollback/Spawner-NBT tests, JSON scan, no datagen; dedicated template backup/restore QA; fragment required.

### 4. Approved slot provisioning boundary — `codex/d1-slot-provisioning`
* **Behavior/dependencies/order:** define startup-readable manifests and in-mod `ServerAboutToStartEvent` reconciliation/seeding; depends 1/3, launch/merge 4; serial with 3/5.
* **Files/hotspots/forbidden:** new tool/docs/manifest tests (exact files chosen after approval); owns storage operation; possible Gradle packaging; forbids server map mutation, reflection, registry hot changes unless separately approved.
* **Effects/risk/validation:** operational HIGH; no in-mod network; build/tool tests, containment tests, JSON if manifests JSON, datagen N/A; Windows/Linux stopped-server lock QA; fragment required.

### 5. Startup lease/routing integration — `codex/d1-slot-leasing`
* **Behavior/dependencies/order:** lease preseeded pair once, indexes/starter room/routing; depends 1–4, launch/merge 5; serial with all adjacent.
* **Files/hotspots/forbidden:** lifecycle, rift, selector, starter room, events/tests; owns travel/lifecycle; possible `CosmicDungeonMod`; forbids same-process deletion/reuse and dynamic registry mutation.
* **Effects/risk/validation:** persistence/routing HIGH; build/GameTests/matrix tests/JSON scan/no datagen; dedicated two-party, reconnect, active-run restart QA; fragment required.

### 6. Quarantine and stopped-server reclamation — `codex/d1-slot-reclamation`
* **Behavior/dependencies/order:** ENDING/DELETE_PENDING, member recovery, verified manifest handoff; depends 5, launch/merge 6; cannot parallelize because it changes lifecycle semantics.
* **Files/hotspots/forbidden:** lifecycle/recovery/rift indexes/tests; owns cleanup; forbids live map reflection/close/reuse and template deletion.
* **Effects/risk/validation:** destructive-world-data HIGH; build/GameTests, crash/manifest/containment tests, JSON scan/no datagen; dedicated crash/locked-path/manual backup QA; fragment required.

## 9. Test strategy

Deterministic tests: identity/routing matrix, authorization denials, manifest containment, legacy codec loading. Filesystem tests: atomic staging, hashes, rollback, symlink traversal, uncertain-orphan retention. GameTests: policy and existing-world teleport behavior only; they cannot prove OS locks, process crashes, connected-client registry mutation, or independent `ServerLevel` teardown. Real dedicated/integrated QA: two parties, client login, external return, logout/death/respawn, restart at every checkpoint, Windows/Linux locks, backup/rollback. No runtime behavior was tested by this ADR.

## 10. Approval options and stop conditions

**OPTION 1 — Preserve no-restart requirement.** Campaign remains blocked; launch no implementation PR. Wait for a supported NeoForge runtime API, approve separately scoped engine modification, or revise product architecture.

**OPTION 2 — Approve bounded predeclared slots.** Capacity equals configured paired slots; each is single-use per uptime; restarts are required for initial seed, a newly published snapshot, reclamation, crash recovery, and locked cleanup; in-mod startup provisioning is supported; external tooling is unnecessary. Waive/modify no-restart deletion/reuse and immediate new-snapshot requirements. Remaining unknowns include complete routing/migration and dedicated/integrated operational validation; do not approve until these operational compromises are explicitly accepted.

**OPTION 3 — Reject both.** Stop the campaign and retain this ADR as evidence.

Existing 1.5.0/1.5.1 worlds, templates, rifts, RD destinations, doors, spawners/presets, player data, and legacy runs remain untouched by this documentation change. No implementation phase is authorized by this ADR.

## 11. Corrected startup provisioning evidence and boundaries

**Dedicated order:** `DedicatedServer#initServer()` calls `ServerLifecycleHooks.handleServerAboutToStart(this)` (line 273), then `this.loadLevel()` (line 275). **Integrated order:** `IntegratedServer#initServer()` calls the same hook (line 80), then `this.loadLevel()` (line 81). `ServerLifecycleHooks#handleServerAboutToStart(MinecraftServer)` is public static and posts `ServerAboutToStartEvent`; its Javadoc states it occurs before the server begins loading anything. `MinecraftServer#loadLevel(): void` is protected and calls `createLevels()`. Thus no slot `ServerLevel`, `ServerChunkCache`, entity manager, or dimension IO worker has yet been constructed when the event runs.

The server already owns the world `LevelStorageAccess` and world lock at this phase; this prevents a second process from opening the world, not the owning server process from creating/copying/deleting its own verified subdirectories. `MinecraftServer#getWorldPath(LevelResource): Path` and `DimensionType#getStorageFolder(ResourceKey<Level>, Path): Path` are public path derivations. The model may therefore use canonical containment checks and normal `java.nio.file` work under the owned world tree before dimension owners open. Source evidence does not itself prove every filesystem operation atomic on every OS; future containment/Windows/Linux tests remain mandatory.

Ordinary `SavedData` is tied to a loaded level and is unavailable as the authority at this phase. The future contract must maintain a startup-readable, atomically published ownership/run manifest outside per-level `DimensionDataStorage`, plus the paired template manifest. The startup handler reads it, retains/diagnoses uncertainty, reclaims only `DELETE_PENDING` paths whose compatible manifest/run/containment all validate, copies a selected published pair, writes the new manifest, and lets normal startup open the declared keys. It must not infer authority from folder presence, static IDs, rift/door indexes, or ordinary SavedData alone.

## 12. Exact expanded repository inventory

The rows in section 6 are the routing inventory; additionally, exact lifecycle/non-routing entries are: `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleEvents.java` event handlers (login/logout/dimension/server tick); `DungeonRespawnEvents.java` respawn handler; `DungeonGroupSplitEvents.java` split handler; `src/main/java/net/goui/cosmicdungeon/playerclass/api/ClassCloneEvents.java` clone/death handler; `src/main/java/net/goui/cosmicdungeon/achievement/plantflags/PlantFlagData.java`, `PlantFlagEvents.java`, and `PlantFlagService.java`; `src/main/java/net/goui/cosmicdungeon/advancement/BloomSharedAdvancements.java`; `src/main/java/net/goui/cosmicdungeon/block/custom/LesserBloomBlock.java`; and `src/main/java/net/goui/cosmicdungeon/progression/ProgressionService.java`. They persist/derive player or progression state and must be audited by the recovery/lease PR; they are forbidden from independently selecting a physical instance. `CosmicDungeonMod` is the central event-registration hotspot. `ModNetwork`, `ModNetworkClient`, and `CosmicDungeonClient` remain client/shared-jar audit points; no client payload may select a physical key.

## 13. Pairwise future-task parallelism matrix

| Task | 1 contract | 2 publisher | 3 slot registration | 4 startup seed | 5 routing/lease | 6 selector | 7 quarantine/recovery | 8 regression |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 contract | — | No | No | No | No | No | No | No |
| 2 publisher | No | — | No | No | No | No | No | No |
| 3 slot registration | No | No | — | No | No | No | No | No |
| 4 startup seed | No | No | No | — | No | No | No | No |
| 5 routing/lease | No | No | No | No | — | No | No | No |
| 6 selector | No | No | No | No | No | — | No | No |
| 7 quarantine/recovery | No | No | No | No | No | No | — | No |
| 8 regression | No | No | No | No | No | No | No | — |

“ No” is deliberate: each task consumes outputs or exclusive hotspots from the preceding task. Revised split: 1 persistence/startup manifest; 2 paired publication; 3 predeclared registration/datagen; 4 `ServerAboutToStartEvent` seed/reclaim; 5 resolver/runtime lease; 6 selector startup; 7 quarantine/crash recovery; 8 final regression. Each must state the 32 PLANNER fields before launch; none is authorized here.
