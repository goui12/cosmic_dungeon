# D1 Batch30: reviewed legacy Chop recovery

2026-09-19. Authorized: Batch30, validation and final local commit; stop before31.
Branch: feature/d1-canon-config-20260916.
Verified parent29: 06336504483e26b294ebedb8bea6934d4b4a1e2b.
Implementation candidate; licensed gameplay and native multiplayer acceptance remain pending.

## Finished

B30-1: In-game developers can inspect an online owner's old Chops, preview one ordinary
inventory slot and explicitly apply a safe orphan-to-Raw conversion. The preview expires
using the existing ItemProtection authoring lifetime from CosmicDungeon.config.
Apply rechecks developer access, both live sessions, interfaces/cursor, inventory journals,
the owner's full inventory, Ender Chest, position, ownership, run and stored-item evidence.
Changing any frozen owner image invalidates the preview.

The command preserves quantity, unrelated components and every other inventory slot.
An existing self-owned token remains identical. Only untagged single items receive a new
owner/token after explicit developer review. It removes old return coordinates and does
not teleport, grant another item, erase escrow or choose among duplicates. A current Raw
Chop is a no-op. No new config key, item ID, network packet or client-only dependency.

B30-2: Purchase and automatic adoption now respect old escrow, stored inventory and protected
returns. An in-memory owner index includes orphan escrow rows, with no table scan in polling.
Raw auto-adoption waits until outside a run/template/instance, without old return metadata.
Recognized Cooked recovery rejects conflicting owner/run evidence and retained run records.
Duplicate stacks, overstacks, Ender Chest copies, partial/foreign markers, other registered
token owners and pending Raw entitlements remain intact for complete-save review.

B30-3: Existing item-identity preview/apply/undo now checks a living connected developer,
empty cursor and the shared inventory-recovery gate. Previews belong to the actual player
session and original member/location run scope; an old token cannot cross reconnect or a
different dungeon instance. Existing restrictions, exact component matching, unopened loot
table protection and nearby-loaded-container limits remain in force.

These three changes are code-complete/runtime-unverified. M03/M20/M10/M72 remain broad partial
IDs because other legacy evidence, authored mappings and native acceptance are still pending.

## Sources and authority

Debloated MASTER and Q&A workbook hashes match the Batch29 baseline. Q&A D01/D02/D04/D24/D25
governs round-trip travel, one personal Chop, owner-only pickup and unused Cooked-to-Raw
recovery. It overrides the older one-way Powered Chop/campfire text. Authored vanilla
equipment is preserved under Cameron's direct instruction; display names confer no identity.

Four relevant live Google documents were revalidated UNCHANGED before code edits:

| Source | Document ID | Modified UTC |
| --- | --- | --- |
| Beatrix Farrow Inventory | 1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I | 2026-08-19T21:23:31.822Z |
| Farrow's Chop Internal | 18McISr9dcvMGp3-VjLDVsISosKBfkaidUTPzHxlw1Pc | 2026-07-07T18:23:05.554Z |
| Gear Trading and Vendor Sales 2.0 | 1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc | 2026-08-18T22:35:34.052Z |
| Dungeon Dropped Gear and Items | 1fX1UbC6cG_cnN2auDo_1ascDY24yjFy9pIfL4qe3-_g | 2026-08-28T16:49:24.607Z |

Private sources, exact Q&A cells, before-images, hashes and validation logs:
Google Docs and Sheet/Audit/D1_Batch_30_2026-09-19/. Source bodies remain outside Git.

## Persistence, security and compatibility

Recovery reuses the existing Chop travel reservation/decision, native player save proof,
ownership compare-and-set and reconnect protocol. There is no second transaction store.
An optional version1 review object in a local adopt/refresh plan records the developer,
slot, exact before/after item and ownership images. Its validator rejects unrelated item
changes, count changes, token rotation and inconsistent evidence. The player receipt keeps
this review until a later Chop operation replaces that receipt; it is not a permanent audit
archive. Export a full backup before administrative adoption.

Old plans without review remain valid. Existing SavedData IDs, owner/escrow shapes and item
IDs are unchanged; the escrow owner index rebuilds on load without a serialized extension.
No spawner, preset, chest, class equipment, active world or active server config was edited.
No world migration was run. Review does not scan/load unopened, unloaded or nested storage.

Back up the complete TEST save before upgrading/adopting, including playerdata, level.dat,
data and dimension folders. Settle pending journals before any downgrade. Restore a matching
complete backup if reverting; copying one older player/world file into newer journals can
create inconsistent evidence that intentionally stays held. Never delete custody or receipts
to bypass recovery.

Developer command permissions are checked again on preview/apply; command blocks/console
cannot impersonate the in-game reviewer. Other players cannot use the commands. Everything
runs on the server. Existing registry/network versions remain unchanged. Token-collision
lookups run only on deliberate developer review; ordinary recovery polling uses the existing
interval and constant-time owner index. Native latency still needs licensed measurement.

## Automated validation

8,457 offline checks passed, including506 new legacy-review checks and the existing94 config
checks. Two .config round trips and Java21 offline build passed. New fixtures exercise four
Raw/Cooked and untagged/self-owned cases across twelve compressed-NBT world/player/ownership
save boundaries each (48 scenarios), plus stale images, foreign/partial ownership, duplicate
slots/stacks, original-token preservation, exact components and legacy owner-index rebuilds.
The shared interruption helper was generalized to the plan's owner; its original assertions
remain. Early test-helper failures were corrected; final results are in checks-final.log.

These are offline production-rule/codec/native-NBT fixtures, not in-world ServerPlayer,
menu, command, mixed-save restore or multiplayer tests. Access/lifecycle integrations were
also code-reviewed. All source JSON and final whitespace/link checks are recorded in the
private FINAL_VALIDATION.json. Datagen was unnecessary because no generated resource changed.
No clean, GameTest, client/server launch, deployment or push occurred. Tracked1.5.0 jar and
the pre-existing generated-cache edit remain preserved.

## Licensed TEST acceptance still required

1. Back up TEST. As Goui12/developer, inspect a copied legacy fixture with
   /d1 chop inspect OwnerName. Record item components, owner/token and old return metadata.
2. With both players' interfaces closed and cursor empty, use
   /d1 chop preview OwnerName 5, then /d1 chop apply TOKEN.
   Confirm one Raw item, identical unrelated components/other slots and the same existing token.
3. Change inventory, Ender Chest, position, ownership, run or session after preview; Apply must
   refuse. Verify expired tokens, duplicate Apply, non-developer and console attempts.
4. Try duplicates, overstacks, foreign/partial markers, Ender Chest copies, retained runs,
   orphan escrow, stored returns and pending Raw delivery. Confirm no deletion or new grant.
5. Interrupt native dedicated/integrated saves at reservation, owner preparation, commit,
   ownership, owner receipt and acknowledgement. Reconnect, verify once-only conversion, and
   inspect retained review evidence. Review uncertain partial backups without forcing a reset.
6. Check new purchases/automatic Raw adoption wait for stored custody; normal owner-only
   dropped pickup, consumption, destruction and current travel still work after settlement.
7. Try /d1 item preview, apply and undo during Watson, Chop, cleanup, trade, commerce and
   repair recovery. They must refuse. After settlement, verify ordinary held/container adoption
   and exact undo; crossing a run/reconnect must invalidate the old preview.
8. Confirm full inventories and authored enchanted/nested components remain exact, unopened
   loot tables are not generated, and no instance/template/spawner data is rewritten.

## Remaining work and graphics

Counts remain43 implemented_unverified,22 partial_D1,9 preserved_verification_pending,
27 deferred_D2_plus=101. See [remaining work](D1_REMAINING.md) and the private disposition file.

Legacy physical denomination pickup still uses the older deposit/discard pair. A detailed
source-backed TODO now identifies entity merge/split, account/entity/player/chunk save order,
capacity, ownership and interruption requirements. Batch26's managed death-drop journal
does not automatically migrate those older physical items. Account receipt retention,
historical Watson evidence, duplicate/orphan Chop dispositions and reviewed template/chest/
drop/preset/unloaded item mappings remain open. No name-based or bulk migration was added.

No new required PNG. Optional existing request: tamsin_d1_map.png,512x256, winding route to
Base Camp signed -JHW, under src/main/resources/assets/cosmicdungeon/textures/gui/.
The fallback remains available; equipment uses authored vanilla items.

Next proposed31: legacy physical-currency recovery and account-cap/threshold evidence
(M03/M02), after live inspection and selecting concrete issues. Await Cameron; do not proceed.
A future improvement is a persistent, reviewed migration archive for complex orphan cases.

## Exact files changed

- src/main/java/net/goui/cosmicdungeon/command/D1Command.java
- src/main/java/net/goui/cosmicdungeon/dungeon/ChopOwnershipData.java
- src/main/java/net/goui/cosmicdungeon/dungeon/ChopOwnershipService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/ChopRecoveryCommand.java
- src/main/java/net/goui/cosmicdungeon/dungeon/ChopRecoveryReview.java
- src/main/java/net/goui/cosmicdungeon/dungeon/ChopTravelPlan.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInventoryEscrowData.java
- src/main/java/net/goui/cosmicdungeon/economy/CurrencyPickupEvents.java
- src/main/java/net/goui/cosmicdungeon/item/identity/D1ItemAdoption.java
- src/test/java/net/goui/cosmicdungeon/dungeon/ChopRecoveryChecks.java
- src/test/java/net/goui/cosmicdungeon/dungeon/ChopTravelChecks.java
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java
- docs/ai/D1_BATCH_30.md
- docs/ai/D1_REMAINING.md
- docs/ai/D1_IMPLEMENTATION_20260916.md
- docs/ai/tasks/d1-canon-config-20260916.md
- docs/releases/fragments/d1-batch-30-legacy-chop-review.md
