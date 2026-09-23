# Correction to this historical checkpoint

Cameron reiterated on2026-09-20 that class-chest items are outside AI work.
R01's automatic supply-marking implementation described below has been removed.
The previous interpretation of the newest-document instruction did not authorize
rewriting authored items. Repair compatibility is open for a service-only solution.
R03 rocket-content inspection is also outside scope. Requested shift-click and
Judicator Lux ability access remain implemented.
See [current correction](D1_AUTHORED_CHEST_CORRECTION_2026-09-20.md).

---

# D1 readiness fixes and answers

Date2026-09-20. Parent948cb6bd39030dfc6982a21c650e8a415035cdc0.
The20-tab debloated workbook and Q&A define scope. Eleven relevant retained Doc revisions/hashes
were revalidated unchanged. No removed sheet requirement was restored.

## Completed

### R01: repair supplies

The previous review correctly found incompatibility but overstated the validation as a bug.
Repair2.0 (1Gbcq7Piqg2uHO1smx5oHOyeGxoT9g93cH-_G8WvhOoo, August18) explicitly requires markers
and rejects ordinary substitutes. Cameron now asked to choose the current document rule,
superseding D08's no-marking answer for these run supplies.

A nondeveloper active D1 Dragoon opening a Dragoon class chest causes eligible plain material stacks
to become repair-only supplies in that run chest. IDs, quantities, names, lore and other allowed
components remain. This processes27 slots on opening, never per tick. Existing markers, protected
identities, provenance, damage, enchantments, arbitrary custom data, nested storage and oversized
stacks remain intact. Ordinary weapons never become repair kits. Global validation stays strict.

This is a deliberate supply-context rule, not proof of complete historical provenance:
eligible materials subsequently placed in such a chest are also prepared on its next authorized open.
No items or money are granted. Authored templates/developer opens are not processed.
The existing item-component codec persists the marker; no new save schema or ID was introduced.

### R02: Judicator Lux

The Camp3 chest list (1GY8_zURMNKZvxkV-PCkG82Rh1q7tErvFBWoSi3wvNWU, April4 21:12)
is newer than the overview (April4 14:39). Judicator now shares Lux identity and uses an independent
Judicator.lux_vitalis section. Defaultpower8HP = four hearts; healing living targets and damaging
undead through the existing magic path. Active-run/class/attunement gates remain.
Theurgist values remain independent. No item, texture or chest quantity changed.

### R06: shift-looting

Only the exact reviewed native ChestMenu backed by a valid class chest qualifies. All source slots
must belong to that chest and all36 destination slots must belong to this player.
Protected gear can shift out; the exception does not permit the reverse deposit.
Native merging/partial movement/full-inventory remainder behavior remains.

Existing physical denominations also shift out of class chests, but remain items awaiting legacy
review. No coin redemption, account credit or ordinary-chest credit path was added.

## Remaining5 follow-ups

### R03: rockets

Pyroclast Item Sheet rows12/23 retain Cinderbite/Cindermaul. Only the linked April4 chest document
mentions Cinderkiss/Cinderbight. Its entries give names/counts, without stars/damage/radius/equivalence.
The source spelling is Cinderbight, not Conderbright/Cinderbright.

| Name | Source detail | Treatment |
| --- | --- | --- |
| Cinderkiss | Named stacks/quantities | Retain authored vanilla payload |
| Cinderbight | Named stack, Camp1 quantity4 | Retain authored vanilla payload |
| Cinderbite | Four stars; six hearts maximum | Existing configured D1 effect |
| Cindermaul | Five stars;7.5 hearts maximum | Existing configured D1 effect |

No new item is necessary for a renamed rocket. Cameron owns those payloads; no AI inspection, conversion or alias work is scheduled.
No rocket or loadout edits occurred.

### R04: crafting

Existing guards reject repair-component misuse and restricted ordinary repairs. There is no general
approved-recipe gate. Recipe-book hiding and Adventure mode do not enforce inventory crafting.
Proposed server policy: recipe-ID allowlist, player class checks, explicit automation rules and
reload validation across2x2 inventory, crafting table and Crafter paths. Approved brewing, repair
and transmutation remain explicit services. Audit yield/fuel/byproducts/cheaper conversions.
No new crafting policy was installed in this question-and-fix pass.

### R05: balance UI and chest money

Reuse the existing five trade icons and live counts in a shared read-only renderer. A PNG can
decorate a background but cannot supply values, synchronization or click semantics. A compact
HUD and side panels on inventory/class-chest screens can read the same UUID account through
initial and bounded delta updates, with disconnect cleanup and GUI-scale compatibility.

Treasure claiming is separate. Dad defines denominations as account displays, not normal physical
money. Old coins may already have been credited. Any new chest-credit action needs an approved
unpaid reward identity and durable ledger claim. Never add the displayed personal balance again.
This pass permits physical class-chest transfer; account credit and balance panels remain pending.

### R07: legacy models

Older cosmicdungeon item definitions still point to missing PNGs. Four current compatibility IDs:
ebonsight, lux_vitalis, scintilla_vitalis, vielpiercer. Named minecraft:tipped_arrow and
minecraft:spectral_arrow stacks already use vanilla resources and are unaffected. D1 legacy
creative-tab listings are commented out, but old saves/commands can reference those IDs.

Change ModModelProvider's mapping, then runClientData regenerates definitions. Example:

    registerExternalItem(itemModels, ModItems.VIELPIERCER.get(),
            ResourceLocation.withDefaultNamespace("item/spectral_arrow"));

For tipped arrows preserve the appropriate vanilla tint handling as well as the model.
Keep every registry ID; do not edit generated JSON by hand. No resources changed in this pass.
The private review lists14 missing legacy references; future-only assets are not D1 requirements.
No new required PNG. Optional map:tamsin_d1_map.png,512x256, Base Camp route signed -JHW.

### R08: world bindings, last

A binding connects code to the authored world. Doc navigation coordinates alone do not establish
exact chest positions, saved regions, lock IDs, NPC profiles or encounter ownership.

| System | Verify on TEST copy | Failure if wrong |
| --- | --- | --- |
| Watson | Saved template location, instance mapping, flags and gathered roster | No spawn/hand-in |
| Regions/objectives | Base Camp, Camps4/5, Wither room, manor, Overworld achievements | Missing/wrong-area credit |
| Startup/class chests | Six classes,36 existing paste requests, rotations and quantities | Wrong/missing gear |
| NPCs | Stable identities, correct profiles, personal unlocks, Beluzon role/type | Wrong service/access |
| Doors/keys | Exact locks, matching keys, passage counts and instance isolation | Key fails/wrong door opens |
| Encounters | Preserved spawners/potentials, reward identities and drops | Wrong payout/progression |
| Travel | Village aliases, rifts, Chop campfire and safe returns | Wrong destination/lifecycle |
| Blooms/journals | Six distinct real Blooms and canonical journal metadata | Items fail qualification |

Start with a consistent world/player/config/preset backup and inspect before editing.
Read-only:/d1 watson status. Write only after reviewing location:/d1 watson set X Y Z
from the correct D1 template/instance. Existing objective, region and door diagnostics remain.
Run the existing binding checklist without replacing authored spawners or guessing coordinates.

Test one complete nondeveloper run, failure/reset and another run, then two simultaneous instances.
Run objectives reset; lifetime totals remain. No world was opened, rebound or deployed here.
A useful later improvement is a read-only missing-binding preflight before dungeon entry.

## Validation

Java21 offline d1OfflineChecks plus build:15,944 checks in44 groups passed,329 above Batch38.
Two config round trips; Judicator defaults/old-config correction/independent override checks passed.
All1,964 source JSON files valid; git diff --check passed.
Supply tests cover approved materials, identity/binding/marker/enchantment/damage exclusions,
ordinary weapons, and oversized counts. Shift tests distinguish pickup from reverse transfer.
Native ChestMenu direction and full-inventory behavior were traced in the exact local source.
Native GUIs, slots/prediction, chest saves, instance gates and combat still require licensed TEST.

No datagen applicable: no generated resources/models/tags/recipes/loot/advancements changed.
No GameTest, client/server launch, deployment, active config edit, push or bulk world migration.
Existing generated-cache change and tracked1.5.0 jar remain preserved/excluded.
No new protocol or save schema. Prepared run materials use the existing repair marker codec.
No client-only dependencies or per-tick scans were introduced.

Candidate JAR SHA256:d113c1542e0b287ba03f9354277abebdfa0711ba9a3cb40c59d071bcb27c4e1b.
The final local commit is the last repository-changing step; resolve its hash from Git history.

## Exact changed files

- docs/ai/D1_BATCH_33_MAPPINGS.md
- docs/ai/D1_CUMULATIVE_TEST_HANDOFF.md
- docs/ai/D1_READINESS_FIXES_2026-09-20.md
- docs/ai/D1_REMAINING.md
- docs/config-examples/CosmicDungeon.config
- docs/releases/fragments/d1-readiness-chest-supplies-and-lux.md
- src/main/java/net/goui/cosmicdungeon/block/entity/ClassLockedChestBlockEntity.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/package-info.java
- src/main/java/net/goui/cosmicdungeon/economy/CurrencyPickupEvents.java
- src/main/java/net/goui/cosmicdungeon/item/identity/ClassChestTransfers.java
- src/main/java/net/goui/cosmicdungeon/item/identity/ItemMovementGuard.java
- src/main/java/net/goui/cosmicdungeon/item/identity/ItemMovementPolicy.java
- src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1AbilityConfig.java
- src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1AmmunitionCatalog.java
- src/main/java/net/goui/cosmicdungeon/playerclass/dragoon/repair/ChestRepairSupplyRules.java
- src/main/java/net/goui/cosmicdungeon/playerclass/dragoon/repair/ClassChestRepairSupplies.java
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java
- src/test/java/net/goui/cosmicdungeon/item/identity/ItemMovementChecks.java
- src/test/java/net/goui/cosmicdungeon/playerclass/d1/D1CombatChecks.java
- src/test/java/net/goui/cosmicdungeon/playerclass/dragoon/repair/ChestRepairSupplyChecks.java
