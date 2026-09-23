# D1 Batch 28 - inventory handoff and startup rollback

Completed in code on 2026-09-19; native runtime acceptance remains pending.
Authorization: Batch28, checks, final local Git commit, then STOP before29.
Baseline: 7b05883d8c2e5fd4fa6a37b51848e68932f007d3 on feature/d1-canon-config-20260916.

## Behavior

- A frozen D1 cleanup decision now precedes escrow retirement. It records owner/run/outcome,
  exact replacement/stored inventory images, old escrow and old/new Chop entitlement.
  Success keeps the current inventory and stores the other one. Failure restores outside
  belongings; a player already using the outside inventory keeps its current contents.
- Online source inventory is verified on disk before committing an exact-before decision.
  Offline owners receive an independent durable cleanup record. Their run may reset only
  after the stash, Raw entitlement and escrow retirement are verified; login then applies
  that owner's saved outcome. A missing/changed source holds for review.
- Native player receipts cover inventory, equipment, class-root state, position, rotation,
  dimension and respawn, including the integrated owner's level.dat copy. The saved receipt
  prevents a second delivery. Completed-run watermarks survive later claims and protect
  against an older world cleanup overwriting an owner's acknowledged completion.
- /d1 claim processes the oldest stored run, within the existing configured stack budget.
  It simulates ordinary-slot insertion before touching inventory, preserves components,
  honors stack limits, and journals exact before/after/remainder images. The remainder
  is retired only after the matching player receipt is verified. Full inventory leaves
  belongings stored; more pending runs can be claimed with another command.
- Kicks/link-dead removal retire membership through the same decision. Reset and both
  direct dimension-restore paths wait for durable handoffs. Trade/repair/death/commerce
  recovery remains ahead of cleanup; Chop synchronization and protected-return commands
  respect pending handoffs. Watson refuses a hand-in while item recovery is pending.
- Entry records the entire roster's full pre-entry inventories and Chop ownership beside
  the registered run before binding/clearing/teleporting anyone. That startup marker is
  removed only after every owner entered and native saves verify. An incomplete marker
  rolls the roster back on restart. Full pre-entry images include the carried Raw Chop;
  rollback restores its original entitlement and keeps personally selected classes.
- Pre-registration paste failure changes no player inventory. The unused physical slot
  is refreshed before reuse. All36 paste operations and six logical-slot mappings remain.
  Actual authored schematic placement is still M81/runtime verification.
- Fixed a public nested-record codec initialization cycle exposed by the new startup test.
  Invalid/duplicate saved records and future handoff versions fail closed without overwriting
  their original files.

## Authority and configuration

The debloated Master workbook and supplied September16 Q&A remain authoritative.
D20 defines successful inventory retention and failed-run outside restoration; D24 defines
Raw return entitlement. D01/D02/D25 retain the approved single-Chop/campfire round trip.
Tamsin's August19 document requires a locked roster and cancellation on failed preparation.
The July notes and older Chop proposals do not override the later supplied answers.

Five relevant Google file metadata/body hashes were revalidated unchanged:
- Tamsin Internal: 1-FcHP73pFytPfoM2KhUPa6tt_2licsgWmWokto4YzE4, 2026-08-19T22:35:48.808Z.
- Notes7.7.26: 10fv5JCue39bZq8ENdDdz7QtJdJKH67f8oCIzGcAyc2Y, 2026-07-12T16:12:42.442Z.
- Campfire Internal: 1fyiehjysrKWM0RilTxXpccmEQzdqc65Wmz0XuQRrUio, 2026-07-07T22:42:25.125Z.
- Chop Internal: 18McISr9dcvMGp3-VjLDVsISosKBfkaidUTPzHxlw1Pc, 2026-07-07T18:23:05.554Z.
- Beatrix inventory: 1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I, 2026-08-19T21:23:31.822Z.

No new gameplay modifiers or vendor prices. CosmicDungeon.config's existing
ItemProtection.recoveryStacksPerClaim controls claim work (default32, range1-128);
its source/example comment now describes both commands. All configuration values
and all_vendors_prices.config are unchanged. No active config was edited.

## Saved data, compatibility and rollback

Existing IDs remain:
- cosmicdungeon_pending_dungeon_recovery: optional handoffs and completed_runs maps.
- cosmicdungeon_dungeon_runs: optional startup map; old active runs have no startup marker
  and are not automatically treated as interrupted entry.
- cosmicdungeon_d1_stored_inventory_v1: existing pending/last_stashed_run fields unchanged.
- Owner root: additive dungeon_inventory_handoff_v1 receipt and
  dungeon_inventory_completed_run_v1 watermark. Existing clone handling preserves the root.

Legacy pending recovery records load unchanged but are held because they contain no proof
whether delivery already occurred. Do not automatically replay/delete them or invent a
new ownership token. Review a complete consistent backup before a separately scoped
adoption tool. Older malformed/duplicate records likewise retain evidence for review.

Before upgrading TEST, back up the complete stopped world: level.dat, all playerdata,
SavedData, dimensions/entities and matching mod/config versions. Never roll back only a
player file or only a world data file. Older jars do not understand pending handoffs;
restore the complete matching backup to downgrade safely. No actual save was migrated.

No item/entity/block registry, network protocol, placed spawner, preset, authored chest,
NPC, door, key, rift definition, world file or client asset changed. Spawner migration
is not required. Common code uses server authority and no client-only initialization.
Protocol5 is unchanged; use matching intended mod jars for licensed TEST as usual.

## Validation

- Java21: gradlew.bat d1OfflineChecks build --offline --console=plain, exit0.
- 3,836 offline checks plus two configuration round trips;683 new handoff/startup checks.
- Fixtures exercise production outcome selection, codecs, receipts, immutable images,
  legacy loading/holds, wrong owners, conflicting evidence, partial claim remainders,
  all36 class/blank paste-plan mappings and11 save cuts across10 cleanup/claim scenarios.
- Native compressed-NBT fixtures verify journal/player state survives reload. These are
  offline save-cut models, not live ServerPlayer, WorldEdit or actual filesystem crash tests.
- 1,963 source JSON files parse; Git whitespace checks pass.
- Debloated workbook/Q&A hashes and old tracked1.5.0 JAR/cache evidence preserved.
- No datagen: no generated models/tags/recipes/loot/advancements changed.
- No runtime/GameTest/client/server launch, deployment, push or active-world/config edit.

The first test compile caught a mutable lambda capture in the new fixture, corrected.
The next run exposed the record/outer-codec initialization cycle, fixed in production.
All existing assertions remain enabled; final validation passed after these repairs.

Synchronous verified writes occur only at requested entry/cleanup/claim/recovery boundaries,
with at most the existing six-person roster. Stored claims use an owner/run index and
the existing stack budget. No new idle full-world scan or per-tick disk loop was added.
Native save latency and large nested-item performance still need measurement on TEST.

## Remaining licensed TEST steps

1. Back up a stopped TEST world; stage the matching jar only after deployment authorization.
2. Enter with3-6 players and all six D1 classes. Check all36 authored paste targets, blank slots,
   exact inventory snapshots, selected classes, safe entry and instance isolation.
3. Force each paste/registration/teleport/save boundary to fail on a disposable copy.
   Restart: verify full-roster rollback, exact pre-entry Raw token/components, retained class
   selections, no duplicate inventory, no orphan slot and no trapped participant.
4. Complete/fail D1 with armor, offhand, custom/enchantment/container components and full
   inventories. Test ordinary dungeon inventory and temporarily active Village inventory.
5. Pause after outcome selection and disconnect a member before cleanup. Reset/restart,
   then rejoin: recover once, claim stored overflow repeatedly and verify Raw entitlement.
6. Interrupt decision/stash/ownership/escrow/player/receipt/ack writes. Repeat login/claim;
   prove no duplication or loss, verify dedicated and integrated saves and respawn position.
7. Exercise kick/link-dead removal, two simultaneous instances and pending trade/repair/death
   work. Direct restore must refuse unresolved ownership. Legacy ambiguous evidence must hold.
8. Verify instance objectives reset while lifetime counters remain; measure entry/cleanup
   save pauses and reconnect behavior. Native reward transaction acceptance remains M93.

## Remaining work and graphics

M43/M102 normal implementation is complete, runtime-unverified. M03/M93 still need the earlier
Watson outcome transaction: physical Bloom consumption, lifetime kills/Blooms/completions,
progression/faction projections and cleanup trigger must share a durable decision. Batch28
starts after that outcome; it does not make those earlier writes atomic. Detailed code TODOs
remain in D1WatsonService, DungeonInventoryHandoffs, ClassSelectorEntryService and package-info.

All101 IDs remain tracked:43 implemented_unverified,22 partial_D1,9 preserved_verification_pending,
27 deferred_D2_plus. Next proposed Batch29 is the Watson outcome transaction; await Cameron.

No new required PNG. Existing optional tamsin_d1_map.png is512x256, a winding route to Base Camp
signed -JHW, under src/main/resources/assets/cosmicdungeon/textures/gui/. A fallback already works.

Private evidence: Google Docs and Sheet/Audit/D1_Batch_28_2026-09-19/.
See [remaining tracker](D1_REMAINING.md) and [Batch27](D1_BATCH_27.md).
The final local commit follows these notes and staged review; its hash is in Git history.

## Exact changed files

- docs/ai/D1_BATCH_28.md
- docs/ai/D1_IMPLEMENTATION_20260916.md
- docs/ai/D1_REMAINING.md
- docs/ai/tasks/d1-canon-config-20260916.md
- docs/config-examples/CosmicDungeon.config
- docs/releases/fragments/d1-batch-28-inventory-handoff.md
- src/main/java/net/goui/cosmicdungeon/Config.java
- src/main/java/net/goui/cosmicdungeon/block/custom/ClassSelectorEntryService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInventoryHandoffs.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleEvents.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonRunRegistryData.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonTravelRouter.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonWorldSnapshotService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/FarrowsChopTravelService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/InventoryHandoffPlan.java
- src/main/java/net/goui/cosmicdungeon/dungeon/PendingDungeonRecoveryData.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1StoredInventoryData.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1WatsonService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/package-info.java
- src/main/java/net/goui/cosmicdungeon/item/identity/ProtectedItemRecovery.java
- src/main/java/net/goui/cosmicdungeon/transaction/InventoryTransactionGuard.java
- src/main/java/net/goui/cosmicdungeon/transaction/PlayerSaveProof.java
- src/test/java/net/goui/cosmicdungeon/dungeon/InventoryHandoffChecks.java
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java

Built1.5.1 JAR SHA256: e6340756735dd18f57b2d91a0b6e18a9978326c6089ed2346d0ef7012788081c (untracked build output; not committed).
