# D1 Batches 23-25 completion

Cameron authorized completion through Batch25, combined validation, a final local Git commit,
and STOP before Batch26. The temporary 23-only rollback was superseded; saved 24/25 work was
restored from its hash-verified archive. Branch: feature/d1-canon-config-20260916.

Batch23: exact owner-local trade offer/cursor custody, item restrictions and protected returns.
Batch24: paired account/item decisions, per-owner saved receipts, replay-safe trade recovery,
offline-owner reset gates and persistent First Trade credit.
Batch25: retail, vendor sales and direct repairs use the same account/owner recovery protocol.
Daily stock shares the item-delivery receipt, and Chop sale/purchase ownership uses a verified
comparison. Whole items/components and quote breakdowns remain in the account outbox or
verified native-NBT ledger pages. Reports distinguish generation, sinks, gross transfers and
administrative changes, with daily totals, rejected cap value, median/percentiles and cap review.

Validation: 2,426 offline assertions plus two config-file round trips, Java21 offline build,
1,963 source JSON files, packaged-class checks, source/workbook hashes and Git diff checks.
The fixture runner uses NeoForge's native in-memory loaded-config wrapper; no active config
was loaded or rewritten. Earlier setup failures and their corrections remain in audit logs.
No datagen was needed for 23-25: these batches add no generated models, recipes or advancements.

New Economy defaults in CosmicDungeon.config: ledgerFlushIntervalTicks=1200 and
ledgerRowsPerFlush=256. All vendor prices and other gameplay defaults are unchanged.
The currency save ID, balances, overrides, item/registry IDs and network protocol remain.
New optional account operations/ledger and player custody/receipt/stock fields are additive.
Old balances establish the ledger baseline; old stock counts seed the first owner-local counter.
No spawner migration is required; placed spawners, presets, authored chests and worlds are untouched.
Before eventual deployment, back up the entire world. Roll back using the matching full backup;
individual account/player/Chop/ledger file restores cannot establish item ownership safely.

Server handlers recheck identity, permission, pricing and inventory before reserving payments.
No new client-only dependency or network packet was added in 23-25. Client/server menus, actual
save interruptions, dedicated/integrated behavior and save latency remain runtime-unverified.

Pending licensed TEST QA (when separately authorized):
1. Trade item-only, currency-only and mixed offers; cancel with a carried cursor/full inventory.
2. Disconnect/die at each trade save boundary; reconnect both owners and verify single delivery.
3. Purchase, sell, zero-price surrender and repair; verify components, payment and stock once.
4. Interrupt account/input/decision/delivery/ack saves; verify replay or preserved review hold.
5. Repeat Chop purchase/sale interruptions; check one owner token and no duplicate entitlement.
6. Cross morning/restart/clock rollback; compare vendor availability with saved counters.
7. Exercise capacity limits and rewards; compare /currency report and review <UUID> with ledger.
8. Attempt reset/travel while an offline owner has pending recovery; ensure inventory is retained.
9. Test actual integrated-owner level.dat and dedicated playerdata readback and measure save time.

M118 normal implementation is complete; M02 notification/review follow-ups, M03 death/legacy/
Inn/travel boundaries and M09 native display resync remain explicit. No death-drop behavior was
enabled. 39 findings are implemented/runtime-unverified,26 partial D1,9 preserved,27 deferred D2+.
All 101 original IDs remain in [remaining work](D1_REMAINING.md); deferred items retain source TODOs.

No new required PNGs. Optional tamsin_d1_map.png: 512x256, winding route to Base Camp, signed -JHW;
the existing fallback remains. A future improvement is indexed archival of compact terminal receipts.

Detailed source revisions, file hashes, build logs and restore evidence are in the ignored local
Google Docs and Sheet/Audit/D1_Batches_23-25_Final_2026-09-19 folder. Private source/audit files,
generated build binaries, logs and datagen caches are excluded from the implementation commit.
AGENTS now requires a local commit after validation and completion notes. This checkpoint also
saves the previously uncommitted D1 work/tooling; it does not push or deploy.

## Files changed across Batches 23-25

Paths below are relative to the repository, compared with the verified pre-23 checkpoint.

```text
AGENTS.md
docs/ai/D1_BATCHES_23-25.md
docs/ai/D1_IMPLEMENTATION_20260916.md
docs/ai/D1_REMAINING.md
docs/ai/tasks/d1-canon-config-20260916.md
docs/config-examples/CosmicDungeon.config
docs/releases/fragments/d1-batch23-trade-custody.md
docs/releases/fragments/d1-batch24-trade-commit.md
docs/releases/fragments/d1-batch25-commerce-ledger.md
src/main/java/net/goui/cosmicdungeon/achievement/TradeAchievementService.java
src/main/java/net/goui/cosmicdungeon/dungeon/ChopOwnershipData.java
src/main/java/net/goui/cosmicdungeon/dungeon/ChopOwnershipService.java
src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleEvents.java
src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleService.java
src/main/java/net/goui/cosmicdungeon/dungeon/FarrowsChopTravelService.java
src/main/java/net/goui/cosmicdungeon/dungeon/d1/package-info.java
src/main/java/net/goui/cosmicdungeon/economy/AccountOperation.java
src/main/java/net/goui/cosmicdungeon/economy/CurrencyAudit.java
src/main/java/net/goui/cosmicdungeon/economy/CurrencyService.java
src/main/java/net/goui/cosmicdungeon/economy/D1EconomyConfig.java
src/main/java/net/goui/cosmicdungeon/economy/EconomyLedger.java
src/main/java/net/goui/cosmicdungeon/economy/EconomyLedgerEvents.java
src/main/java/net/goui/cosmicdungeon/economy/EconomyReports.java
src/main/java/net/goui/cosmicdungeon/economy/PlayerCurrencyData.java
src/main/java/net/goui/cosmicdungeon/economy/pricing/ItemTransferRules.java
src/main/java/net/goui/cosmicdungeon/economy/pricing/VendorPricingService.java
src/main/java/net/goui/cosmicdungeon/mixin/RepairCustodySaveMixin.java
src/main/java/net/goui/cosmicdungeon/playerclass/dragoon/repair/DirectRepairService.java
src/main/java/net/goui/cosmicdungeon/playerclass/dragoon/repair/RepairTransactions.java
src/main/java/net/goui/cosmicdungeon/trade/TradeCommitPlan.java
src/main/java/net/goui/cosmicdungeon/trade/TradeCustody.java
src/main/java/net/goui/cosmicdungeon/trade/TradeCustodyImages.java
src/main/java/net/goui/cosmicdungeon/trade/TradeEvents.java
src/main/java/net/goui/cosmicdungeon/trade/TradeFinalizationGameTests.java
src/main/java/net/goui/cosmicdungeon/trade/TradeFinalizationService.java
src/main/java/net/goui/cosmicdungeon/trade/TradeMenu.java
src/main/java/net/goui/cosmicdungeon/trade/TradeRecoveryData.java
src/main/java/net/goui/cosmicdungeon/trade/TradeRecoveryEvents.java
src/main/java/net/goui/cosmicdungeon/trade/TradeSessionData.java
src/main/java/net/goui/cosmicdungeon/trade/TradeTransactions.java
src/main/java/net/goui/cosmicdungeon/transaction/InventoryTransactionGuard.java
src/main/java/net/goui/cosmicdungeon/vendor/CommerceCustodyImages.java
src/main/java/net/goui/cosmicdungeon/vendor/CommerceRecoveryEvents.java
src/main/java/net/goui/cosmicdungeon/vendor/CommerceTransactions.java
src/main/java/net/goui/cosmicdungeon/vendor/VendorMenuState.java
src/main/java/net/goui/cosmicdungeon/vendor/VendorPurchaseLimitData.java
src/main/java/net/goui/cosmicdungeon/vendor/VendorService.java
src/main/java/net/goui/cosmicdungeon/vendor/VendorStock.java
src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java
src/test/java/net/goui/cosmicdungeon/economy/EconomyLedgerChecks.java
src/test/java/net/goui/cosmicdungeon/trade/TradeCommitChecks.java
src/test/java/net/goui/cosmicdungeon/trade/TradeCustodyChecks.java
src/test/java/net/goui/cosmicdungeon/vendor/CommerceChecks.java
```
