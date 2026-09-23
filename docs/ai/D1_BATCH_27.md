# D1 Batch 27 - Beluzon's Inn and Chop travel recovery

Completed in code on 2026-09-19; native runtime acceptance remains pending.
Authorization: Batch 27 only, final local commit after validation/notes, then stop before Batch 28.
Baseline: a11d98983c815ba2a26a3d7150afbcbe53531e2f on feature/d1-canon-config-20260916.

## Behavior

- Beluzon's one-time personal bond remains 15 Trace, with its permanent receipt in the existing account.
  Success requires account readback; an uncertain payment cannot silently unlock Inn services.
- Bed changes remain free. Both halves must be intact inside the configured Inn; claims normalize to
  the head and verify persistence before success. Existing bed coordinates gain their original dimension.
  Region edits preserve old bindings as evidence instead of deleting or relocating them.
- /home settles pending inventory work first. Another valid respawn remains preferred; an unavailable
  respawn world no longer prevents the Inn fallback. Active dungeon members retain dungeon handling.
- Native Inn protection covers piston sources/destinations/destruction, multi-block placement,
  fluid spread, burning/lava ignition, explosions, hostile block destruction and hostile spawning.
  A protected Heart cannot spawn a protector outside the cuboid. Operator commands and other mods'
  direct world writes are not blanket intercepted. No authored block, bed or NPC was changed.
- Chop travel freezes exact before/after full inventories, ownership, escrow and destination pose.
  A verified world reservation precedes owner custody; a verified commit precedes the swap.
  Owner receipt readback includes inventory, equipment, class-root custody, dimension, position
  and rotation, including the integrated-server owner's level.dat copy. Inventory snapshots
  use the server registry context, so registry-backed enchantment/holder components can serialize.
  The shared D1 entry snapshot and Chop-carry snapshot use the same checked writer.
- Reconnect cancels uncommitted reservations or finishes committed journeys. Matching receipts
  prevent repeated item delivery; world/ownership side effects reconcile independently.
  Mismatched or missing evidence holds for review instead of guessing a replacement inventory.
- Full outside inventories, invalid/missing campfires and unsafe destinations refuse before
  reservation. Failed saves disconnect the affected owner with saved recovery evidence retained.
- Pending journeys block member removal, run reset and direct dimension restore, including offline
  owners. The normal inventory/currency guards share that hold.
- One untagged held Raw Chop of count one can adopt through the journal. Run-end Raw delivery and
  stale Cooked conversion use the same receipt path. Cooking/conversion retain unrelated components.
  Duplicate, overstacked, foreign, unknown dropped-owner and old unbound Cooked copies remain intact.
  Ownership checks no longer stamp stacks, delete extras, or replace an entire overstack with one item.
- Ordinary outside-inventory synchronization marks escrow dirty only when its image changed.
  Idle Chop checks avoid full inventory serialization when no adoption/conversion is eligible.

## Authority and defaults

The debloated workbook remains the scope authority. Its supplied Q&A was last edited
2026-09-16T20:42:01Z. D01 selects campfire-bound round trips; D02 allows one Chop total;
D24 converts unused return tokens to Raw when runs end and prohibits party merge/split;
D25 uses an ordinary lit campfire. This preserves the already-approved implementation.
The older August 19 Beatrix inventory's one-way Powered Chop text does not supersede that
September answer. July campfire proposals do not re-enable extra sockets, group merge rules
or a separately purchased campfire item. This batch does not invent those deferred behaviors.
Inn defaults follow the August 29 Beluzon document, as Q&A D67 directs.

CosmicDungeon.config gains:
- Beluzon.bondQuoteSeconds = 30; Beluzon.bondInteractionRange = 8.
- BeatrixFarrow.chopCookRange = 6; BeatrixFarrow.chopRecoveryPollTicks = 20.
Existing chopCookTicks stays 80. The new ranges/cadence are implementation defaults, not quoted lore.
Prices remain in all_vendors_prices.config; its contents are unchanged.
No active server config was edited or replaced.

Nine relevant live Google file metadata checks matched the existing local artifacts.
Selected documents:
- NPC Beluzon Internal: 1FT6k2MFKgQf_tQ5UcBn0wmqJ9-yY_Wdpjna4USdVOZA, 2026-08-29T14:49:42.290Z.
- Beatrix Inventory: 1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I, 2026-08-19T21:23:31.822Z.
- Chop Internal: 18McISr9dcvMGp3-VjLDVsISosKBfkaidUTPzHxlw1Pc, 2026-07-07T18:23:05.554Z.
- Campfire Internal: 1fyiehjysrKWM0RilTxXpccmEQzdqc65Wmz0XuQRrUio, 2026-07-07T22:42:25.125Z.
Exact workbook rows, revisions and hashes remain in the private batch audit; source bodies are not committed.

## Validation

Java 21 d1OfflineChecks and build passed with --offline: 3,153 checks plus two config round trips.
This batch adds 140 Inn checks, 453 Chop checks, five native bytecode-target checks and six config checks.
The fixtures cover twelve persisted save cuts for each leave/return/adopt/refresh path, exact
counts/components/poses, repeated recovery, wrong owners/receipts, partial saved world images,
legacy loading, duplicate escrow rejection, invalid tokens/poses and dimension-restore blockers.
Inn checks include personal one-time payment, insufficient balance, region boundaries, legacy bed
dimensions and region edits. Native bytecode checks verify the exact fluid/lava/fire/Heart targets.

All 1,963 source JSON files parse; git diff --check passes. No client-only common imports were added.
The existing deliberate malformed-Bogatyr fixture logs an expected rejection and passes.
Gradle reports existing deprecation warnings. No heap, Gradle dependency or runtime setting changed.
These are offline serialization/decision fixtures and bytecode inspection, not live process-kill
experiments, Mixin application, native teleportation or multiplayer acceptance.

Built JAR: build/libs/cosmicdungeon-1.5.1.jar
SHA256: cdcf50917daff875421e76f053c19e59590ce569c13569285935319424547d4d

No GameTest/server/client launch, deployment, push, actual world migration or active config edit.
Datagen is inapplicable: no generated model/tag/recipe/advancement, new item or texture changed.
The tracked old 1.5.0 JAR and pre-existing generated-cache edit are preserved.
The workbook/Q&A and refreshed document/native-snapshot hashes still match.

## Persistence and rollback

No existing save or registry ID is renamed. cosmicdungeon_inn_v1 adds optional bed_dimensions;
old beds inherit the old Inn dimension before region edits can occur.
cosmicdungeon_dungeon_inventory_escrow_v1 adds optional transitions. Old entries load with no
pending journal; malformed/duplicate entries are held rather than silently collapsed.
cosmicdungeon_chop_owners_v1 retains its shape with stricter token/run validation.
Player cosmicdungeon data adds chop_travel_custody_v1 and one compact chop_travel_receipt_v1.
The existing full class-root clone path preserves these fields. Completed journal entries are
removed only after a verified owner receipt. There is no new network protocol or client registry.
Spawner/block-entity/preset storage is untouched and needs no migration.

Before eventual authorized TEST deployment, stop the server and back up the complete matching
world, player data, configs and jar. To roll back after running this version, restore that complete
backup with its matching jar. Do not restore only one player/ownership/escrow file or downgrade
the jar while a new travel journal remains: older code cannot honor the new custody fields.
A repeated recovery hold requires inspection of matching saved images on a copy, not deleting
the journal or manufacturing another token. Native power-loss and disk-failure acceptance is pending.

## Cumulative licensed TEST steps

1. Inspect the existing Creaking profile and Pale Oak pillar/Heart; bind the existing Inn cuboid.
2. Pay once with 15 Trace; repeat/reconnect/restart and change beds freely. Reject a balance of 14.
3. Claim either bed half; test a missing half, obstructed stand-up position and boundary-crossing bed.
4. Test valid bed/anchor/savepoint priority, missing/blocked respawn, unavailable world and /home during recovery.
5. Attempt piston push/pull across every face, fluids, lava ignition, fire, multi-place and explosions.
6. Run a full night beside the protected Heart; confirm it creates no hostile protector outside the region.
7. Cook one Raw Chop, visit Village, buy ordinary items and return; compare inventory/armor/offhand/components.
8. Refuse a full outside inventory, other owner's token, unsafe return and deleted campfire without item loss.
9. Test single Raw adoption; preserve duplicate/overstacked/old Cooked/Ender Chest/dropped-owner fixtures exactly.
10. Interrupt each owner/world/ownership save around both travel directions on dedicated and integrated servers.
11. Test receipt replay, disk-write failure and partial-backup mismatch; inspect retained evidence and reset holds.
12. Test offline owners, death outside D1 and run-end Raw delivery. Final cleanup handoff still belongs to Batch 28.
13. Measure save latency with representative inventories and verify actual Mixin application, menus and client sync.

## Remaining scope and graphics

M40 is implemented_unverified. Totals: 41 implemented_unverified, 24 partial_D1,
9 preserved_verification_pending and 27 deferred_D2_plus: all 101 original IDs remain.
M17 retains identity-safe inspection of existing placed vendors. M20 retains deliberate legacy
preview/adoption; ambiguous inventory is preserved, not declared migrated.
M43/M102 retain the final success/failure/offline cleanup handoff through PendingDungeonRecoveryData
and D1StoredInventoryData before ordinary escrow retirement. The travel journal does not claim
that all dungeon startup/reset inventory handling is now crash-safe. Detailed TODOs are in the code.
Next proposed Batch 28 addresses that final inventory handoff and startup rollback; await Cameron.

No required new PNG. Optional outstanding art: tamsin_d1_map.png, 512 x 256, winding route to
Base Camp signed -JHW; its drawn fallback remains available.
Future improvement: complete the final lifecycle handoff using verified destination receipts.

See [remaining work](D1_REMAINING.md) and the [implementation guide](D1_IMPLEMENTATION_20260916.md).
Private evidence: Google Docs and Sheet/Audit/D1_Batch_27_2026-09-19/.

## Exact files in this checkpoint

- src/main/java/net/goui/cosmicdungeon/Config.java
- src/main/java/net/goui/cosmicdungeon/npc/inn/InnData.java
- src/main/java/net/goui/cosmicdungeon/npc/inn/InnService.java
- src/main/java/net/goui/cosmicdungeon/npc/inn/InnEvents.java
- src/main/java/net/goui/cosmicdungeon/mixin/InnHeartMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/InnFluidMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/InnFireMixin.java
- src/main/java/net/goui/cosmicdungeon/transaction/SavedDataProof.java
- src/main/java/net/goui/cosmicdungeon/transaction/PlayerSaveProof.java
- src/main/java/net/goui/cosmicdungeon/transaction/InventoryTransactionGuard.java
- src/main/java/net/goui/cosmicdungeon/dungeon/ChopTravelPlan.java
- src/main/java/net/goui/cosmicdungeon/dungeon/ChopTravelRecovery.java
- src/main/java/net/goui/cosmicdungeon/dungeon/ChopOwnershipData.java
- src/main/java/net/goui/cosmicdungeon/dungeon/ChopOwnershipService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/ChopCampfireEvents.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInventoryEscrowData.java
- src/main/java/net/goui/cosmicdungeon/dungeon/FarrowsChopTravelService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonTravelRouter.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonWorldSnapshotService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/package-info.java
- src/main/java/net/goui/cosmicdungeon/economy/CurrencyService.java
- src/main/java/net/goui/cosmicdungeon/item/custom/FarrowsChopItem.java
- src/main/resources/cosmicdungeon.mixins.json
- src/test/java/net/goui/cosmicdungeon/dungeon/ChopTravelChecks.java
- src/test/java/net/goui/cosmicdungeon/npc/inn/InnChecks.java
- src/test/java/net/goui/cosmicdungeon/npc/inn/InnHookChecks.java
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java
- docs/config-examples/CosmicDungeon.config
- docs/ai/D1_BATCH_27.md
- docs/ai/D1_REMAINING.md
- docs/ai/D1_IMPLEMENTATION_20260916.md
- docs/ai/tasks/d1-canon-config-20260916.md
- docs/releases/fragments/d1-batch-27-inn-chop-recovery.md
