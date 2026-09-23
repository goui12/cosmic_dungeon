# D1 Batch 38 - menu state, spawner work bounds and player help

Completed in code on2026-09-20 UTC. Authorized Batch38 only; stop after the final local commit.
Parent:bbdd8fe3e1211669595e1a88853ae61a8e4fae1c. Branch:feature/d1-canon-config-20260916.
This is an implementation/review checkpoint, not licensed gameplay acceptance or deployment.

## Completed work

- B38-1: vendor/trade/repair native menu openings carry a unique session identity.
  Vendor opens/results/quotes and trade/repair state are accepted only for that current opening.
  Reused container IDs and old cached views cannot repaint a new session. Repair initialization
  preserves an already-valid view. Server transaction permissions/custody checks remain intact.
- B38-2: existing open account views poll scalar balances at a configurable interval and send
  refreshes only when those values change. Vendor refreshes do not clear sale selections.
  Shared UUID accounts and the existing denomination formatter remain authoritative.
- B38-3: Cosmic Spawners use an in-memory index of native-visible tagged entities instead of
  repeated full-level scans. Visible/hidden transitions, Tags edits, health, removal and reload
  update exact live counts, including wandering members. Native /data UUID restoration is covered.
  Admission applies existing presets/defaults immediately; later maintenance shares a fair
  global budget. Disabled/completed spawners stay inactive. Level/server shutdown releases state.
- B38-4: help now explains invitation leadership/personal readiness, per-run/lifetime progress,
  logical death currency versus legacy coins, Companionship's paid-at-drink cooldown, current
  trident/brewing behavior and Beluzon's service. Six D1 class pages are reachable; future-class
  and developer-validation pages are removed from navigation. Achievement listings use names.
- B38-5: [cumulative TEST handoff](D1_CUMULATIVE_TEST_HANDOFF.md) records19 acceptance passes,
  all101 original audit dispositions, and explicit legacy/UI/performance follow-ups.
  [Remaining plan](D1_REMAINING.md): **0 currently scheduled implementation/review batches**.

Counts remain46 implemented/runtime-unverified,19 partial D1,9 preserved pending verification
and27 D2+ deferred. Completing the numbered plan does not close those native or deferred items.
M09 still lacks dedicated HUD, ordinary-inventory and class-chest balance panels; the source-backed
code TODO records their layout/client-cost design requirement. Use /currency balance meanwhile.
M104/M105 retain actual performance, GUI, translation and lore-timing acceptance.

## Sources and tuning

Direct Cameron instructions take precedence; the current Q&A and retained debloated MASTER
mechanics/newest applicable linked Docs remain the authority. Nine relevant Doc metadata checks
were unchanged. SOURCE_REVIEW.json records exactly which bodies/sections were semantically read;
download status alone is not source review. Native document hashes and all three workbook hashes
match the verified baseline. No source workbook, authored world or active config was edited.

CosmicDungeon.config adds only:

| Section/key | Default | Allowed range |
| --- | --- | --- |
| Performance.menuBalancePollTicks |20 ticks|5..200 |
| Performance.spawnerMaintenanceVisitsPerTick |512 global visits/tick|16..4096 |

All previous gameplay defaults compare equal. all_vendors_prices.config is unchanged.
The index adds temporary server memory proportional to visible tagged entities and their markers,
plus queued loaded spawners; it is released by native removal/level unload/server stop.
No extra heap, runtime library, profiler, background agent or client entity scan was added.
Native admission work remains part of spawning/loading; the512 bound governs queued maintenance,
not arbitrary native entity loading. Actual performance benefit requires measured TEST results.

## Automated validation

Java21: gradlew.bat d1OfflineChecks build --offline --console=plain passed.

- **15,615 offline checks in43 groups**, up2,683 from Batch37.
  New:2,575 index/lifecycle/budget/native-descriptor checks,35 menu/session/wire checks,
  69 navigation checks and4 config checks. Existing transactions/NBT/recipe/combat/travel checks pass.
- Two config round trips pass; invalid new settings clamp to their documented bounds.
- All1,964 source JSON parse, including the manual mixin registration. git diff --check passes.
- Native menu provider/factory APIs, visible-entity callbacks, health/data/UUID method descriptors
  and packaged classes/mixin JSON were checked offline. This does not execute native mixins.
- Exact source/workbook and pre-existing generated-cache hashes match. All tracked files outside
  the37-file allowlist match the baseline, including AGENTS.md and the tracked1.5.0 jar.
- First compile corrected a final SimpleMenuProvider inheritance attempt and two leftover scan
  calls. Later review added visible/hidden transitions, disabled-spawner guards and native /data
  identity restoration. Final checks cover the corrected code.
- No datagen was needed: only Java, tests, config examples, docs and established manual mixin JSON
  changed. No clean, GameTest, client/server launch, deployment, push or source synchronization write.

Candidate: build/libs/cosmicdungeon-1.5.1.jar
SHA-256:fdc5e7e43dc4f3af0ecc91bac3499df984405ab9189a0ca86aaab4c561fad72a
This build binary is not committed or deployed; the tracked1.5.0 jar is preserved.

## Compatibility and regression boundaries

Batch38 changes **network protocol5 to6**, native menu extra data and vendor state payloads.
Use matching CosmicDungeon jars on client/server. Display session identities are transient;
existing saved menu custody/session receipts keep their format. Incoming transaction validation
continues to use the existing server-side permissions, roles, inventory and custody checks;
this change specifically rejects stale displayed state, not a claim that every C2S packet changed.

No SavedData/NBT/schema/preset version, registry ID, player UUID account key or external format
changes in Batch38. No migration or manual spawner recreation is required. Old entity Tags rebuild
the derived index as their chunks become visible. Admission rejection does not add a phantom member.
Authored SpawnData, weighted potentials, equipment, preset files, caps, one-shot flags and drops stay
saved in their existing forms. Use a full world/player/config/preset backup for future upgrades;
earlier D1 batches have their own save additions, so whole-pass rollback requires matched backups.

Access Policy, classes, rifts/destinations, doors/keys, currency transfers, progression, faction,
achievements and inventory journals are not rewritten. Their cumulative offline fixtures pass where
available; actual two-instance interactions, native permission/collision/veto behavior and GUI layout
remain **NOT RUN**. Common menu/index/network code has no new client-only imports.

Outside-active inventory snapshots still run at the established20-tick cadence plus lifecycle
captures. A future dirty optimization must preserve in-place component, durability and cursor
changes; the detailed code TODO forbids relying only on an inventory change count.

## Required licensed TEST and graphics

Follow the [cumulative checklist](D1_CUMULATIVE_TEST_HANDOFF.md), especially:

1. Open a vendor/trade/repair, change account balance/capacity elsewhere, compare /currency balance,
   then close/reopen/reconnect and resize. Old-session state must be ignored; selections survive refresh.
2. Reload old capped/weighted/one-shot spawners. Wander, kill/revive, retag and unload/reload mobs.
   Confirm hidden/visible transitions and disabled spawners; compare original NBT/preset files.
3. Exercise Batch35 recipes/crafting, Batch36 effects and Batch37 travel/bindings in two real runs.
   Verify save interruption recovery and separate instance progress from lifetime totals.
4. Measure server/client baseline versus candidate on a consistent world copy; tune the shared
   maintenance budget only after observing entity load, MSPT, memory plateau and network traffic.

No new PNG required. Existing optional tamsin_d1_map.png is512x256: route to Base Camp,
signed -JHW; the drawn fallback remains functional.

Future improvement: use the measured TEST profile to choose a suitable shared maintenance budget.

## Exact files in this checkpoint

- docs/ai/D1_BATCH_38.md
- docs/ai/D1_CUMULATIVE_TEST_HANDOFF.md
- docs/ai/D1_REMAINING.md
- docs/releases/fragments/d1-batch-38-menu-spawner-help.md
- docs/config-examples/CosmicDungeon.config
- src/main/java/net/goui/cosmicdungeon/Config.java
- src/main/java/net/goui/cosmicdungeon/block/entity/CosmicSpawnerBlockEntity.java
- src/main/java/net/goui/cosmicdungeon/block/entity/CosmicSpawnerEntities.java
- src/main/java/net/goui/cosmicdungeon/block/entity/SpawnerMembership.java
- src/main/java/net/goui/cosmicdungeon/block/entity/SpawnerMaintenanceQueue.java
- src/main/java/net/goui/cosmicdungeon/client/ModNetworkClient.java
- src/main/java/net/goui/cosmicdungeon/client/screen/VendorScreen.java
- src/main/java/net/goui/cosmicdungeon/client/screen/TradeScreen.java
- src/main/java/net/goui/cosmicdungeon/client/screen/DragoonRepairScreen.java
- src/main/java/net/goui/cosmicdungeon/client/screen/HelpMenuContent.java
- src/main/java/net/goui/cosmicdungeon/menu/ModMenus.java
- src/main/java/net/goui/cosmicdungeon/menu/VendorMenu.java
- src/main/java/net/goui/cosmicdungeon/menu/SessionMenu.java
- src/main/java/net/goui/cosmicdungeon/menu/SessionMenuProvider.java
- src/main/java/net/goui/cosmicdungeon/menu/MenuBalanceRefresh.java
- src/main/java/net/goui/cosmicdungeon/mixin/CosmicSpawnerEntityMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/CosmicSpawnerTrackingMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/CosmicSpawnerHealthMixin.java
- src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java
- src/main/java/net/goui/cosmicdungeon/network/VendorPayloads.java
- src/main/java/net/goui/cosmicdungeon/trade/TradeMenu.java
- src/main/java/net/goui/cosmicdungeon/trade/TradeSessionData.java
- src/main/java/net/goui/cosmicdungeon/playerclass/dragoon/repair/DragoonRepairMenu.java
- src/main/java/net/goui/cosmicdungeon/playerclass/dragoon/repair/DragoonRepairSessionData.java
- src/main/java/net/goui/cosmicdungeon/vendor/VendorInteractionEvents.java
- src/main/java/net/goui/cosmicdungeon/vendor/VendorService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/package-info.java
- src/main/resources/cosmicdungeon.mixins.json
- src/test/java/net/goui/cosmicdungeon/block/entity/SpawnerMembershipChecks.java
- src/test/java/net/goui/cosmicdungeon/menu/MenuBalanceChecks.java
- src/test/java/net/goui/cosmicdungeon/menu/D1HelpNavigationChecks.java
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java
