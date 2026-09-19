# D1 Batch 26 â€” death currency

Completed in code on 2026-09-19; runtime acceptance remains pending.
Authorization: Batch 26 only, then local commit and stop. Batch 27 is not started.
Baseline: d96118247ffe101fba8d7de511387c1f45b9414e on feature/d1-canon-config-20260916.

## Behavior

- Below 20 Trace: no loss. Otherwise max(1,floor(balance*0.02)), capped at balance if developers change defaults.
- The existing CosmicDungeon.config Economy section controls threshold, fraction, minimum and bounded recovery/checkpoint/retry work.
- Confirmed ServerPlayer death creates a unique life-token intent after the cancelled-death early return and ordinary loot handling.
- Pending deaths block new account activity. Only undecided money reservations cancel; existing trade/repair/shop custody records retain their normal owner-return paths.
- Debit, active logical drop and ledger entry share the existing account image. A verified intent can finish after restart without the owner online.
- One ordinary ItemEntity shows the existing Trace graphic. Its amount, owner, transaction UUID, run ID, creation timestamp and native entity image live in the account.
- Any eligible player with room for the complete amount can collect. No partial pickup, inventory stack, hopper/mob collection, merging or special lifetime.
- Native item physics, damage, portal movement and expiry hooks remain. Normal item age/lifespan/position survive checkpoints; unloaded chunks do not age items.
- Account checkpoints record the last saved state. An abrupt crash can lose unsaved position/age ticks, as with other world saves; this is not a wall-clock expiry guarantee.
- Projections are excluded from native entity/chunk saves and therefore from filesystem world templates. Loaded chunks recover projections through a bounded queue; no forced chunk loads or full-entity scans.
- Pickup transfers existing supply. Despawn/destruction removes supply without charging the owner again. Resetting a dimension reconciles loaded and unloaded records there before deletion.
- Supply reports now include active_death_drops, pending_deaths and total_supply. Existing threshold notifications and final-review evidence are reused.
- Uncertain writes hold currency until readback succeeds; structural corruption holds for developer review with evidence retained.

## Validation

Java 21 d1OfflineChecks and build passed with --offline: 2,549 checks and two configuration round trips.
Batch 26 adds 116 death formula/recovery/native-image/supply checks and 7 configuration checks.
Fixtures include persisted intents, every modeled account save cut through collection, repeat deaths,
competing recipients, capacity refusal, reservation cancellation, despawn replay, separate runs,
native-age/portal-image preservation, legacy missing fields and malformed/partial save rejection.
These are offline account/serialization fixtures, not native process-kill or Minecraft integration tests.

All 1,963 source JSON files parse. git diff --check passed. Native source inspection verifies the exact
ServerPlayer.die final-return placement, ItemEntity merge/pickup/expiry behavior and Entity save/removal hooks.
The existing deliberate malformed-Bogatyr fixture logs an expected error and passes its rejection assertion.
Gradle reports existing deprecation warnings; no heap/runtime dependency change.

Built JAR: build/libs/cosmicdungeon-1.5.1.jar
SHA256: 05bdac83f19032171dcbb9c14ccaaa3dde2158b0c5cd9979169676dcd811f28a

No GameTest/runtime/client/server launch, deployment, push, active config or world edit occurred.
Datagen is inapplicable: existing item/renderer/texture, with no generated models, tags, recipes or registries changed.
The tracked 1.5.0 JAR, pre-existing generated-cache changes and private source workbooks were preserved.

## Persistence and compatibility

The existing cosmicdungeon_player_currency_v1 save gains optional death_currency schema 1.
Older saves without that field load an empty extension; present malformed/future shapes fail closed.
The player cosmicdungeon root gains death_currency_life_v1; the existing class-clone root preserves it.
Existing balances, capacity overrides, trade/repair/shop plans, world IDs and registry IDs remain.
No spawner/block-entity/preset migration is required; no placed content was edited.
Common code has no client imports and introduces no packet or protocol version.

Before eventual deployment, stop the TEST server and take one complete world/config/mod backup.
Update matching client/server jars only through the existing authorized deployment workflow.
To roll back after running this version, restore that complete pre-update backup with its matching jar.
Do not downgrade the jar while retaining new account data, or restore account/player/dimension files separately:
an older jar cannot preserve active logical-drop ownership. Offline fixtures do not certify a live upgrade.

## Cumulative licensed TEST steps

1. On a world copy, test balances 19, 20, 50 and 12,345; confirm losses 0, 1, 1 and 246 and one visible drop.
2. Cancel death through the installed event hook; confirm no debit. Repeat with keepInventory enabled.
3. Race two players to one drop; confirm one full credit and no inventory stack. Repeat with original owner.
4. Give the collector insufficient capacity, then sufficient room; confirm intact drop then full pickup.
5. Test hopper carts/hoppers, mobs, merge candidates, containers, crafting/anvils and vendor/trade paths.
6. Unload/reload the chunk and restart; verify UUID/value, saved age/lifespan, owner and run metadata.
7. Compare expiry with ordinary loot, including installed expiry hooks; test fire, lava, void and portals.
8. Interrupt saves around intent/debit/pickup/despawn; verify recovery and /currency report difference 0.
9. Die with open trade/repair/shop custody; confirm cancellation, protected returns and one death debit.
10. Reset a dimension with loaded and unloaded drops; confirm only that dimension's remaining value sinks.
11. Exercise failed resets, unavailable dimensions and simulated write failures; verify holds preserve evidence.
12. Repeat life-token/recovery checks on dedicated and integrated servers; inspect actual mixin application.
13. Measure save latency and recovery budgets with representative active drops; check menus/HUD resync.

## Scope and next work

M08 moves to implemented_unverified. M03 remains partial for Inn/travel entitlement, legacy physical-currency
pickup migration, long-run receipt retention and licensed native acceptance.
Tracker totals: 40 implemented_unverified,25 partial_D1,9 preserved_verification_pending,27 deferred_D2_plus (101 IDs).
See [remaining work](D1_REMAINING.md). Batch 27: Beluzon's Inn binding and Chop travel recovery; await Cameron.

No required new PNG. Optional outstanding art: tamsin_d1_map.png, 512x256, winding route to Base Camp,
signed -JHW; the drawn fallback remains. Existing attunement_trace.png is reused.

Future improvement: measure active-drop checkpoint/save latency on TEST and tune the existing server budgets.

## Source evidence

Debloated workbook and Q&A hashes remain unchanged. All five relevant Google file metadata refreshes
matched the prior local snapshots; no older document replaced a newer one.
Economy Internal 17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28, modified 2026-08-18T21:01:23.864Z,
death specification in mirrored Markdown lines 489â€“597 and corresponding native document snapshot.
Private validation/evidence: Google Docs and Sheet/Audit/D1_Batch_26_2026-09-19/.
Closed audits and the Batches23-25 report remain historical snapshots.

## Exact files in this checkpoint

- src/main/java/net/goui/cosmicdungeon/economy/DeathCurrencyRecord.java
- src/main/java/net/goui/cosmicdungeon/economy/DeathCurrencyState.java
- src/main/java/net/goui/cosmicdungeon/economy/DeathCurrencyService.java
- src/main/java/net/goui/cosmicdungeon/economy/PlayerCurrencyData.java
- src/main/java/net/goui/cosmicdungeon/economy/CurrencyService.java
- src/main/java/net/goui/cosmicdungeon/economy/CurrencyPickupEvents.java
- src/main/java/net/goui/cosmicdungeon/economy/D1EconomyConfig.java
- src/main/java/net/goui/cosmicdungeon/economy/EconomyLedger.java
- src/main/java/net/goui/cosmicdungeon/economy/CurrencyAudit.java
- src/main/java/net/goui/cosmicdungeon/item/identity/ItemMovementRules.java
- src/main/java/net/goui/cosmicdungeon/transaction/InventoryTransactionGuard.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonWorldSnapshotService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/package-info.java
- src/main/java/net/goui/cosmicdungeon/mixin/DeathCurrencyPlayerMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/DeathCurrencyItemMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/DeathCurrencyEntityMixin.java
- src/main/resources/cosmicdungeon.mixins.json
- src/test/java/net/goui/cosmicdungeon/economy/DeathCurrencyChecks.java
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java
- docs/config-examples/CosmicDungeon.config
- docs/ai/D1_BATCH_26.md
- docs/ai/D1_REMAINING.md
- docs/ai/tasks/d1-canon-config-20260916.md
- docs/ai/D1_IMPLEMENTATION_20260916.md
- docs/releases/fragments/d1-batch-26-death-currency.md
