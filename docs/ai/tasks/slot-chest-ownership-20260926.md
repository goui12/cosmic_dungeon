# Slot chest ownership - 2026-09-26

Cameron requests that every slot schematic's class chest belong to the player assigned
that slot at entry, show that player's name overhead, and explain denied access.

Branch: feature/slot-chest-ownership-20260926; base4d413f7c/PR198.
Exclusive surfaces: ClassSelectorEntryService startup integration,
DungeonStartupSchematicPipeline, ClassLockedChestBlockEntity persistence/access.
No concurrent writer or registry/network registration change.

Bind all class chests from each of the36 startup paste requests to the corresponding
locked roster UUID/name. Use actual clipboard chest positions, its origin and WorldEdit
rotation; do not guess nearest slot or recompute ownership from a shrinking live roster.
Bind prepared physical instances only, before admitting players. Preserve authored items,
components, quantities and schematic files. Cache source chest positions per batch.
A repeated slot/class still has its own UUID. Blank slots fail closed if they contain a chest.

Add optional CosmicSlotOwner block-entity metadata and native update tags containing owner
information only. No existing items/fields are renamed. Old unbound chests load as before;
new runs acquire ownership during paste. Do not guess owners for already-started old runs.
Malformed ownership remains locked. Chunk reload/restart preserves UUID ownership.
Other players, including developers inside another player's owned chest, are denied.
Existing developer/class rules remain for unbound authoring chests.

The block interaction, inherited createMenu route and stillValid share the same server
ownership check. Owned chest automation cannot insert/extract. Render nearby, depth-tested
name captions through the existing chest renderer, without entities, per-tick world scans,
new custom packets, dependencies, texture assets or changes to chest content.

Expected files:
- block/custom/ClassSelectorEntryService.java
- dungeon/DungeonStartupSchematicPipeline.java
- dungeon/DungeonSlotChestBindings.java (new)
- block/custom/ClassLockedChestBlock.java
- block/entity/ClassLockedChestBlockEntity.java
- block/entity/ClassChestOwnership.java (new)
- client/render/blockentity/ClassLockedChestRenderer.java
- assets/cosmicdungeon/lang/en_us.json
- focused ownership and paste-coordinate tests
- docs/Classes/Class_Restrictions_and_Inventory.md
- this task card and unique release fragment.

Validation: Java21 build, focused ownership/round-trip/slot-rotation tests, source JSON,
diff review and native development-client startup. No datagen applies to Java/native
update tags/hand-authored translations. Do not run clean or an unlicensed GameTest server.
Preserve unrelated staged historical-JAR deletion and generated caches/logs.
Source commit/push and latest-built publication follow the standing agreed-fix workflow.
TEST replacement requires current stopped-server/closed-installed-client evidence.

Manual QA: two players of the same class; own vs other starting and later slot chests;
floating labels at close range, stacked chest readability; shift-click/currency display;
disconnect/rejoin/chunk reload/restart; separate concurrent runs and a fresh subsequent run;
unchanged unbound authoring chests and no item/component changes. Legacy active runs
need a new entry to acquire this feature. No world/spawner migration or configuration edit.

## Completed validation

Java21 (installed21.0.12.101) build passed; all77 JUnit tests passed with zero failures,
errors or skips. Eight new tests cover UUID identity, stable entry-slot mapping with
duplicate classes, all four WorldEdit rotations/origin offsets, legacy and malformed
ownership, native chest save round trips, authored item/component preservation, automation
gates and ownership-only client updates. All1,997 source JSON files parse and diff checks pass.

The native development client launched to a responding Minecraft window (PID13044).
Bounded current startup logs show completed resource loading and no task-specific or
fatal startup error. No world was entered; actual labels and multiplayer interaction
remain manual acceptance. GameTest/dedicated server launches were not authorized.
No datagen applies; no generated runtime JSON was changed by this task.

The TEST log remains identical to the previous confirmed shutdown snapshot:
BE22896B122DAE7BB2DF8AA26655752002E6938952541E0E8C9879CFFCC8444C.
The installed client was closed; development client launch is separate.
Final source commit/push, committed build, safe replacement and current-test verification
follow the existing authorized pipeline. No server restart, world reset, schematic edit,
server.properties/authentication change or unrelated mod replacement is authorized.

No planned implementation batches remain. Native labels/owner access and licensed
multiplayer testing remain. Possible future improvement: an owner caption in the open
chest title if Cameron requests it.
