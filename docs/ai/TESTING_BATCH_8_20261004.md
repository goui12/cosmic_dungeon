# Testing batch 8 — mercenary hiring and entry

Base: Batch 7 / 9e7eda71 / draft PR206. Branch: feature/testing-fixes-batch8-20261004.
Two approved action items implemented. Three implementation batches (9–11) remain.
See [approved rollout](TESTING_ROLLOUT_20261003.md) and [task card](tasks/testing-fixes-batch8-20261004.md).

## Behavior

At Tamsin, create/join a group, then choose Mercenary and a currently playable Dungeon 1 class.
One reservation per human; one shared roster slot, included in invite acceptance, capacity
changes, ready checks and six-slot entry. Only the hirer can release it. Changes clear
readiness; queued/preparing parties cannot change hires. Leaving releases that member's
reservation. Server restart clears the transient lobby without charging anyone.

mercenaries.hireTrace defaults to 500 Trace. Each reservation freezes its displayed price.
The server checks all hirers' available balances, records contracts in the existing run
registry, and persists account holds after registering startup. Paste, chest, spawn or
teleport failure aborts entry and cancels the holds. The existing verified startup receipt
decides payment: only successful starts debit the group, once, in the same currency image.
A rejected ledger row rolls back all group changes. Existing ledger/archive accounting
is reused. Startup recovery settles saved pending holds from the run receipt before
instance recovery removes incomplete startup markers. Pending successful payments retry
at the existing one-second boundary, with no work when no reservation is pending.

Humans keep their ordered slots; hires follow them in reservation order. All 36 schematic
operations use the complete class roster and distinct mercenary UUID chest ownership.
The starter chest locations come directly from those paste operations, with no world scan.
Each hire spawns at its configured slot destination. Armor, shield and class-supported
weapon use native equipment slots; spare stacks/ammunition/potions remain in its persisted
54-slot supplies inventory. Stable authored chest order selects the first compatible item.
Whole stacks retain names, counts, enchantments, damage, components and payloads. No item
generation, rebalance or ownership rewrite occurs. Foreign/malformed attunement fails entry.
Missing/wrong-owner/wrong-class chests, excess supplies or
refused spawning cancel entry without a fee. These guard limits fail closed.

New cosmicdungeon:mercenary entities use an existing vanilla humanoid model/texture and
armor/held-item layers. They have persistent contract/run identity, native equipment and
a supplies inventory. Admission verifies the exact registered contract, dimension, UUID
and current hirer membership. Cleanup discards loaded hires by known IDs; stale unloaded
copies cannot rejoin. These are stationary entry/equipment NPCs in this batch. Movement,
combat, collection and brewing remain Batch 9; full no-drop death retention, ten-minute
respawn and stacked health/countdown HUD remain Batch 10.

## Persistence, compatibility and boundaries

The existing run codec adds optional mercenaries=[]; all state/copy transitions retain it.
The existing account codec adds optional mercenary_fees={}; balance/ledger/repair/trade/
death/wealth fields and save IDs remain intact. Pending fee indexes rebuild on load and
validate total held debit against balances. Contract fields and fee receipts are validated.
New entities save mercenary_contract, mercenary_run and mercenary_supplies alongside native
equipment. No spawner, door/key, rift destination, preset or authored chest-stack schema
changes; no migration/recreation is needed for placed spawners. Existing class chest owner
fields already support distinct UUIDs and require no migration.

Deploy the same protocol-11 JAR to client/server. Common code has no client-only imports.
Actions require current Tamsin session, proximity, onboarding, roster revision, active-run
exclusion and server class/capacity validation. Developer/spectator admission stays excluded.
Actual players alone remain run members for completion, inventory escrow and kill rewards;
mercenaries do not impersonate players. New gameplay AI/reward attribution is deferred.
World backup before upgrade remains required operational practice. An older binary does
not understand new mercenary entities/receipts: rollback a matched world/account backup,
or finish all new runs first; never downgrade across pending fee reservations.

Maximum three hires fit the six-occupant party. No runtime dependency, heap change,
world/entity scan, pathfinding or new per-tick packet stream. Group views retain existing
delta synchronization. Fee settlement uses the existing bounded ledger and account saves.
Terminal hire receipts follow the account store's existing retained receipt policy;
long-term archival can join its already queued terminal-receipt archive improvement.

## Verification

Java 21 build and 164 native NeoForge JUnit tests passed, zero failures/errors/skips.
Sixteen new tests cover roster limits/invite races/stale actions, readiness and ownership,
frozen fees, disabled classes, slot mapping, exact stack preservation and foreign binding,
old saves/native codec round trips, account holds, group failure/replay/cancellation,
second-ledger-row failure rollback, packet round trip and entity admission boundaries.
d1OfflineChecks passed, including 129 config checks and both config round trips.
All 2,001 source JSON files and 56 changed-document links passed validation.

No generated resources changed: existing vanilla model/texture layers are reused and no
recipe, loot table, tag, model JSON or advancement is introduced. Datagen is not applicable.
No clean, dedicated-server/GameTest launch, world entry or licensed gameplay QA performed.
Build/startup success does not prove rendering, actual authored schematics, interactions
or crash recovery in a running world. Those remain the testing steps below.

## Pending licensed TEST checks

1. At Tamsin, reserve/release a hire and use a maximum of two with one human. Check one-hire
   limit, full-party invitations, readiness reset, stale/queued actions and unchanged balance.
2. With enough Trace, start with two humans and different hires. Check distinct rooms,
   class-matching personal chests, exact authored equipment/supplies, no access to someone
   else's slot, and one frozen-price debit per hirer. Insufficient funds, missing chest or
   failed entry must produce no charge and preserve every human's original inventory.
3. Restart TEST through the existing operator workflow: active identities/equipment must
   restore without a second fee; incomplete entry must release holds and recover inventories.
   Check forfeit/completion/kick cleanup and normal no-hire entry. Leave death/AI acceptance
   for batches 9–10; keep cumulative A/B/C and batches 4–7 gameplay checks pending.

## Exact files
- [docs/ai/D1_REMAINING.md](D1_REMAINING.md)
- [docs/ai/TESTING_BATCH_8_20261004.md](TESTING_BATCH_8_20261004.md)
- [docs/ai/TESTING_ROLLOUT_20261003.md](TESTING_ROLLOUT_20261003.md)
- [docs/ai/tasks/testing-fixes-batch8-20261004.md](tasks/testing-fixes-batch8-20261004.md)
- [docs/config-examples/CosmicDungeon.config](../config-examples/CosmicDungeon.config)
- [docs/releases/fragments/testing-fixes-batch8-20261004.md](../releases/fragments/testing-fixes-batch8-20261004.md)
- [src/main/java/net/goui/cosmicdungeon/Config.java](../../src/main/java/net/goui/cosmicdungeon/Config.java)
- [src/main/java/net/goui/cosmicdungeon/CosmicDungeonMod.java](../../src/main/java/net/goui/cosmicdungeon/CosmicDungeonMod.java)
- [src/main/java/net/goui/cosmicdungeon/block/custom/ClassSelectorEntryService.java](../../src/main/java/net/goui/cosmicdungeon/block/custom/ClassSelectorEntryService.java)
- [src/main/java/net/goui/cosmicdungeon/block/entity/ClassLockedChestBlockEntity.java](../../src/main/java/net/goui/cosmicdungeon/block/entity/ClassLockedChestBlockEntity.java)
- [src/main/java/net/goui/cosmicdungeon/client/CosmicDungeonClient.java](../../src/main/java/net/goui/cosmicdungeon/client/CosmicDungeonClient.java)
- [src/main/java/net/goui/cosmicdungeon/client/render/MercenaryRenderer.java](../../src/main/java/net/goui/cosmicdungeon/client/render/MercenaryRenderer.java)
- [src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyPanel.java](../../src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyPanel.java)
- [src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInventoryHandoffs.java](../../src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInventoryHandoffs.java)
- [src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleEvents.java](../../src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleEvents.java)
- [src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleService.java](../../src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleService.java)
- [src/main/java/net/goui/cosmicdungeon/dungeon/DungeonRunRegistryData.java](../../src/main/java/net/goui/cosmicdungeon/dungeon/DungeonRunRegistryData.java)
- [src/main/java/net/goui/cosmicdungeon/dungeon/DungeonSlotChestBindings.java](../../src/main/java/net/goui/cosmicdungeon/dungeon/DungeonSlotChestBindings.java)
- [src/main/java/net/goui/cosmicdungeon/dungeon/DungeonStartupSchematicPipeline.java](../../src/main/java/net/goui/cosmicdungeon/dungeon/DungeonStartupSchematicPipeline.java)
- [src/main/java/net/goui/cosmicdungeon/economy/MercenaryFee.java](../../src/main/java/net/goui/cosmicdungeon/economy/MercenaryFee.java)
- [src/main/java/net/goui/cosmicdungeon/economy/PlayerCurrencyData.java](../../src/main/java/net/goui/cosmicdungeon/economy/PlayerCurrencyData.java)
- [src/main/java/net/goui/cosmicdungeon/entity/ModEntities.java](../../src/main/java/net/goui/cosmicdungeon/entity/ModEntities.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryConfig.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryConfig.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryContract.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryContract.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEntity.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEntity.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEntry.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEntry.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEquipment.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEquipment.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryLifecycle.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryLifecycle.java)
- [src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java](../../src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java)
- [src/main/java/net/goui/cosmicdungeon/network/PartyPayloads.java](../../src/main/java/net/goui/cosmicdungeon/network/PartyPayloads.java)
- [src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyHudService.java](../../src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyHudService.java)
- [src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyLobby.java](../../src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyLobby.java)
- [src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyService.java](../../src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyService.java)
- [src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryEntryTest.java](../../src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryEntryTest.java)

## Remaining implementation batches

9. Follow/teleport and ally-safe combat; permitted collection/brewing; timed fallback healing.
10. Stacked mercenary HUD; equipment-preserving no-drop death and ten-minute respawn.
11. Server-wide lifetime statistics; teal pause-menu button and selectable leaderboards.

Cumulative licensed gameplay QA is a separate phase. Batch 9 awaits Cameron's instruction.
