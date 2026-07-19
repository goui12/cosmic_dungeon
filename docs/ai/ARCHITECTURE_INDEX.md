# Cosmic Dungeon Architecture Index

Static architecture audit, prepared from the repository at mod version 1.5.1. “None found” means this audit did not find an implementation or test in the inspected source; it is not proof that the behavior is absent at runtime.

## Project Baseline

| Item | Source-confirmed value |
| --- | --- |
| Minecraft / NeoForge / Java | 1.21.10 / 21.10.64 / Java 21 |
| Mod ID / current version | `cosmicdungeon` / 1.5.1 |
| Main source | `src/main/java/net/goui/cosmicdungeon` |
| Runtime resources | `src/main/resources` |
| Client/server generated resources | `src/generated/resources_client` / `src/generated/resources_server` |
| Build / GameTests | `./gradlew clean build` / `./gradlew runGameTestServer`; CI workflow `.github/workflows/build.yml` (`Integration Gate`) runs both for PRs to and pushes to `main`, plus manual dispatch. |
| Client/server datagen | `./gradlew clientData` / `./gradlew serverData` |

## Core Entry Points

| Entry | Exact path | Responsibility |
| --- | --- | --- |
| Mod bootstrap | `src/main/java/net/goui/cosmicdungeon/CosmicDungeonMod.java` | Mod lifecycle, NeoForge event subscriptions, command registration, all central registrations, network registration, reload listener, client bootstrap gate. |
| Client bootstrap | `src/main/java/net/goui/cosmicdungeon/client/CosmicDungeonClient.java` | Client renderers, models, overlays, keybinds, particles, menu screens, and config-screen integration. |
| Command registration | `src/main/java/net/goui/cosmicdungeon/CosmicDungeonMod.java` | Calls each command class from `onRegisterCommands`. |
| Network bootstrap | `src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java` | Registers payload handlers through NeoForge’s payload registrar. |
| Client network dispatch | `src/main/java/net/goui/cosmicdungeon/client/ModNetworkClient.java` and `src/main/java/net/goui/cosmicdungeon/network/ClientNetworkDispatch.java` | Client payload endpoint/dispatch support. |
| Menus/screens | `src/main/java/net/goui/cosmicdungeon/menu/ModMenus.java` and `src/main/java/net/goui/cosmicdungeon/client/CosmicDungeonClient.java` | Deferred menu registration and client screen binding. |
| Datagen bootstrap | `src/main/java/net/goui/cosmicdungeon/datagen/DataGenerators.java` | Datagen providers. |

## Exclusive Integration Hotspots

| Hotspot | Paths / subsystem | Why high risk | Tests / validation | Related documentation |
| --- | --- | --- | --- | --- |
| Registries | `block/ModBlocks.java`, `block/entity/ModBlockEntities.java`, `entity/ModEntities.java`, `item/ModItems.java`, `menu/ModMenus.java`, `particle/ModParticleTypes.java`, `sound/ModSounds.java`, `component/ModDataComponents.java`, `item/ModCreativeModeTabs.java`, `effect/ModMobEffects.java` | Registry IDs and central initialization are compatibility surfaces. | `./gradlew clean build`; datagen when applicable. | `docs/Developer_Documentation_1.5.md` |
| Mod entry | `CosmicDungeonMod.java` | Central lifecycle, event, and registration coordination. | Build; `runGameTestServer`. | `docs/Developer_Documentation_1.5.md` |
| Network | `network/ModNetwork.java`, `client/ModNetworkClient.java`, `network/payload`, `network/handler` | Codec, direction, and server validation must agree. | `TradeFinalizationGameTests.java`; manual multiplayer QA. | `docs/Trading/Trading_Guide.md` |
| Menus/help | `menu/ModMenus.java`, `client/screen/HelpMenuContent.java` | Menu IDs and navigation content are centralized. | Build; manual GUI QA. | `docs/Help_Menu.md` |
| Persistence/migration | `block/entity/CosmicSpawnerBlockEntity.java`, `block/entity/CosmicSpawnerPreset.java`, all `*Data.java` SavedData classes | Live-world and serialized compatibility. | Source contains migration logic; no dedicated migration test found. | `docs/Spawners/Spawner_Systems.md`, `docs/DungeonLifecycle/Dungeon_Lifecycle_System.md` |
| Transactions | `economy/CurrencyService.java`, `vendor/VendorService.java`, `trade/TradeFinalizationService.java`, `playerclass/dragoon/repair` | Currency/inventory transfers require server-side atomicity. | `TradeFinalizationGameTests.java`. | `docs/Economy/Economy_and_Currency.md`, `docs/Trading/Trading_Guide.md`, `docs/Classes/Dragoon_Repair_System.md` |
| Dungeon/reset | `dungeon/DungeonLifecycleService.java`, `dungeon/DungeonWorldSnapshotService.java`, `dungeon/PendingDungeonRecoveryData.java` | World restoration and recovery affect player/world state. | No dedicated test found. | `docs/DungeonLifecycle/Dungeon_Lifecycle_System.md` |
| Travel/access/class | `rift`, `potion/CompanionshipTeleportService.java`, `auth/AccessPolicy.java`, `playerclass` | Authorization and safe destination handling are gameplay/security boundaries. | Build; manual dedicated-server QA. | `docs/Rifts/Rift_System_Guide.md`, `docs/Classes/Class_Restrictions_and_Inventory.md` |
| Release assembly | `docs/releases/Update_1.5.1.md` | Contract-designated release single-writer document. | Documentation review. | `docs/releases/Update_1.5.1.md` |

The planner may add verified paths after reviewing this index; it must declare ownership before editing any hotspot.

## Subsystem Map

### Access Policy
- **Purpose/packages:** rank and permission enforcement in `auth`; key classes `AccessPolicy`, `Authority`, `RankStore`, and `RankEnforcementEvents`.
- **Initialization/client/network/persistence:** referenced by server handlers and commands; no client implementation found; `PasswordStore` and `RankStore` are persistence candidates; exact formats were not audited beyond source-file discovery.
- **Tests/docs/dependencies/hotspots:** no automated test found; `docs/REGION_PROTECTION_GUIDE.md`; depends on commands, rifts, vendors, and network; hotspot is travel/access enforcement.

### Classes and class-attuned equipment
- **Purpose/packages:** class identity, restrictions, extra inventory, and class items in `playerclass/api`, `playerclass/dragoon`, `playerclass/metalmancer`, `playerclass/ore`, `playerclass/pyroclast`, and `playerclass/theurgist`.
- **Key/init:** `ClassData`, `ClassNbtUtil`, `DungeoneerClassService`, `ClassItemRestrictionEvents`, `ClassNet`; client screens/overlays include `ExtraInventoryScreen` and `HotbarOverlay`.
- **Persistence/network/tests/docs:** player persistent NBT root `cosmicdungeon` with `class_id` and `extra`; `ClassPayloads`/`ClassNet`; no dedicated test found; docs in `docs/Classes`; hotspot is class enforcement and serialized player data.

### Achievements and advancements
- **Purpose/packages:** achievement counters and advancement grants in `achievement` and `advancement`.
- **Key/init:** `CosmicAdvancementUtil`, `AchievementCounterData`, `BindingIdolAchievements`; event registrations are inspected through the mod/event infrastructure.
- **Persistence/network/tests/docs:** `AchievementCounterData` and plant-flag data are persisted candidates; no payload family or tests found; `docs/Achievements/Achievements_and_Advancements.md`; hotspot is persistence when formats change.

### Currency, vendors, and player trading
- **Purpose/packages:** currency in `economy`; vendor profiles/purchases in `vendor`; player trade session/finalization in `trade`.
- **Key/init:** `CurrencyService`, `VendorProfileManager`, `VendorService`, `TradeFinalizationService`, `TradeEvents`; vendor reload listener is registered by `CosmicDungeonMod`.
- **Client/network/persistence/tests/docs:** `VendorScreen`, `TradeScreen`; `VendorPayloads`, `TradePayloads`; SavedData classes `PlayerCurrencyData`, `VendorPurchaseLimitData`, `TradeSessionData`; `TradeFinalizationGameTests.java`; docs `docs/Economy/Economy_and_Currency.md`, `docs/Vendor.md`, `docs/Trading/Trading_Guide.md`; transaction and network hotspots.

### Dragoon Repair Affinity
- **Purpose/packages:** repair UI and transaction logic under `playerclass/dragoon/repair`.
- **Client/network/persistence/tests/docs:** `client/screen/DragoonRepairScreen.java`, `network/DragoonRepairPayloads.java`; no format version or automated test found; docs `docs/Classes/Dragoon_Repair_Affinity.md` and `docs/Classes/Dragoon_Repair_System.md`; transaction hotspot.

### Cosmic Mob Spawners
- **Purpose/packages:** block/entity, presets, spawn defaults, rendering, and commands in `block/custom`, `block/entity`, `client`, and `command`.
- **Key/init:** `CosmicMobSpawnerBlock`, `CosmicSpawnerBlockEntity`, `CosmicSpawnerPreset`, `ModBlocks`, `ModBlockEntities`; client renderer is registered in `CosmicDungeonClient`.
- **Persistence/network/tests/docs:** block entity `CosmicSpawnerDataVersion` current 152, legacy 150; preset version 4, legacy 2; intrinsic-drop data version 2; keybind payload and label payload; no dedicated migration test found; `docs/Spawners/Spawner_Systems.md`, `docs/Spawner_Commands_Features.md`; persistence, registry, and networking hotspots.

### Dungeon lifecycle, snapshots, doors, rifts, factions, and progression
- **Purpose/packages:** `dungeon` manages run lifecycle, snapshots and recovery; `door` manages locks and passage; `rift` manages destinations; `faction` and `progression` store player state.
- **Key/init:** `DungeonLifecycleService`, `DungeonWorldSnapshotService`, `DoorLockHandler`, `RiftRegistryData`, `SafeTeleportUtil`, `FactionService`, `ProgressionService`.
- **Persistence/tests/docs:** SavedData classes are catalogued below; no dedicated tests found; docs in `docs/DungeonLifecycle`, `docs/Doors`, `docs/Rifts`, `docs/Progression`, and `docs/Factions`; reset, migration, and travel hotspots.

### Help menu, settings, networking, and datagen
- **Purpose/packages:** UI under `client/screen` and `client/screen/settings`; payloads in `network`; datagen under `datagen`.
- **Key/init:** `HelpMenuContent`, `HelpMenuScreen`, `CosmicDungeonOptionsIntegration`, `ModNetwork`, and `DataGenerators`.
- **Tests/docs/dependencies:** no UI/network tests beyond trade GameTests found; `docs/Help_Menu.md`; Help content, network registration, and generated-resource hotspots.

## Persistence and Compatibility Matrix

| System | Storage / identifier | Version / older support | Migration location | Risk / tests |
| --- | --- | --- | --- | --- |
| Cosmic spawner block entity | Block-entity NBT `CosmicSpawnerDataVersion` | 152; legacy 150 | `CosmicSpawnerBlockEntity.java` load path | High; no dedicated compatibility test found. |
| Spawner preset | NBT `presetVersion` | 4; legacy 2 | `CosmicSpawnerPreset.java` | High; no dedicated compatibility test found. |
| Spawner intrinsic drops | NBT `version`, `rules` | 2; older support not explicit | `CosmicSpawnerEntityIntrinsicDropData.java` | High; no dedicated test found. |
| Rift registry | SavedData `cosmicdungeon_rifts_v2`; legacy `cosmicdungeon_rifts` | v2 ID; legacy file detected | `RiftRegistryData.java` | High; no dedicated compatibility test found. |
| Dungeon run registry/progress/recovery | SavedData `cosmicdungeon_dungeon_runs`, `cosmicdungeon_dungeon_progress_v1`, `cosmicdungeon_pending_dungeon_recovery` | Version fields not found | Respective `dungeon/*Data.java` | High; no dedicated test found. |
| Doors | SavedData `DoorLockData` / `DoorPassageData` storage names | Version fields not found | Respective data classes | High; no dedicated test found. |
| Currency/vendor/progression/factions | SavedData IDs ending `_v1` | v1 IDs; older support not found | Respective data classes | High; no dedicated test found. |
| Player classes | Player persistent NBT `cosmicdungeon`, `class_id`, `extra` | No explicit version field found | `ClassNbtUtil.java`, clone events | High; no dedicated test found. |

## Network and Menu Matrix

| Family | Registration / direction | Server validation | Menu/screen / cleanup | Hotspot |
| --- | --- | --- | --- | --- |
| Class | `ClassPayloads.java` via `ModNetwork.java`; bidirectional | `ClassNet.java` | Extra inventory / client screens; lifecycle code must be reviewed per change | Network, class persistence |
| Rift | `RiftPayloads.java`; C2S/S2C | `ModNetwork.java` checks player, developer authority, distance, and data | `RiftConfigScreen.java`; no cleanup inventory found | Network, travel/access |
| Vendor/trade | `VendorPayloads.java`, `TradePayloads.java` | Services/handlers must be inspected per payload | `VendorScreen.java`, `TradeScreen.java`; `TradeSessionData` | Network, transaction |
| Repair/teleport | `DragoonRepairPayloads.java`, `CompanionshipTeleportPayloads.java` | `ModNetwork.java` dispatch/service paths | Repair/companionship screens; no cleanup inventory found | Network, transaction/travel |
| Region/spawner/RF/shake | payload and handler classes under `network` | Region server handler and `ModNetwork` paths | overlays/keybinds; no unified cleanup registry found | Network, spawner |

## Registration Matrix

| Registration | Exact file | Initialization path |
| --- | --- | --- |
| Items / creative tabs | `item/ModItems.java`, `item/ModCreativeModeTabs.java` | `CosmicDungeonMod` constructor |
| Blocks / block entities | `block/ModBlocks.java`, `block/entity/ModBlockEntities.java` | `CosmicDungeonMod` constructor |
| Entities / effects | `entity/ModEntities.java`, `effect/ModMobEffects.java` | `CosmicDungeonMod` constructor |
| Menus | `menu/ModMenus.java` | `CosmicDungeonMod`; screens in `CosmicDungeonClient` |
| Particles / sounds | `particle/ModParticleTypes.java`, `sound/ModSounds.java` | `CosmicDungeonMod` constructor |
| Data components | `component/ModDataComponents.java` | `CosmicDungeonMod` constructor |

## Automated Test Inventory

- **GameTest:** `src/main/java/net/goui/cosmicdungeon/trade/TradeFinalizationGameTests.java` is registered by `CosmicDungeonMod`; it covers trade finalization behavior. No separate conventional unit-test source tree was found.
- **Test helpers:** None found outside that GameTest class.
- **CI:** `.github/workflows/build.yml` (`Integration Gate`) runs standard-library JSON validation under `src`, then wrapper-based clean build and GameTests on Java 21; it uploads seven-day commit-SHA integration evidence. Datagen remains task-conditional.
- **Validation script:** `scripts/deploy-mod.ps1` exists; it is deployment-oriented, not established as a test script by this audit.

## Documentation Map

- **Player-facing:** `docs/Classes`, `docs/Spawners`, `docs/Doors`, `docs/Rifts`, `docs/Trading`, `docs/Economy`, `docs/Factions`, `docs/Progression`, and `docs/Help_Menu.md` describe gameplay systems.
- **Developer/operator:** `docs/Developer_Documentation_1.5.md`, `docs/REGION_PROTECTION_GUIDE.md`, and `docs/IN_GAME_COMMANDS.md`.
- **Release notes:** `docs/releases/Update_1.4.8_Preparation.md`, `Update_1.4.9.md`, `Update_1.5.0.md`, and `Update_1.5.1.md`.
- **Audit finding:** both `docs/IN_GAME_COMMANDS.md` and `docs/commands/In_Game_Commands.md`, and both upper/lower-case region-guide paths, exist; this is a potential duplication to resolve only in a dedicated documentation task.

## Planner Guidance

- Independent read-only audits and isolated client rendering/model changes may commonly run in parallel.
- Never share a wave for registry, `CosmicDungeonMod`, `ModNetwork`, `ModMenus`, Help content, persistence/migration, transactions, dungeon reset, travel/access, or release-assembly hotspots.
- Run client/server datagen for changes to datagen-managed models, item definitions, blockstates, tags, recipes, loot tables, or advancements.
- Run GameTests for changes covered by existing GameTests; add targeted tests for server-side transaction or migration changes where practical.
- Require migration review for every SavedData, NBT, preset, serialized schema, registry ID, or storage-version change.
- Require manual Minecraft QA for UI/rendering, multiplayer/networking, commands, vendor/trade/inventory behavior, rifts/teleportation, dungeon reset/recovery, and live-world migration behavior.

## Audit Limitations

- Static inspection cannot prove runtime registration order, dedicated-server classloading, packet safety, atomic transactions, complete lifecycle cleanup, or live-world migration success.
- The repository contains only one discovered GameTest and no discovered conventional unit-test source tree; absence of a test is reported as “not found,” not as a guarantee of missing coverage.
- Several persisted formats use codecs or implementation details not exhaustively enumerated here; consult the referenced class before changing any format.
