# Dungeon inventory recipe book - 2026-09-25

Cameron requests removal of the inventory recipe book for dungeoneers in survival mode
inside dungeon instances. This follows the completed map/trade/region TEST deployment.

Branch: fix/dungeon-inventory-recipe-book-20260925, chained from ae05c9fc (PR196).
Files: client/DungeonInventoryRecipeBook.java; mixin/client/InventoryRecipeBookScreenMixin.java;
mixin/client/InventoryRecipeBookComponentMixin.java; cosmicdungeon.mixins.json;
this task card and its matching release fragment.
Exclusive integration surface owned: the client list in cosmicdungeon.mixins.json.
No other active task or server integration surface is modified.

Reuse DungeonInstanceSlots.slotOf for both legacy fixed slots and unique run primary/nether
dimensions. Require exact SURVIVAL and the existing server-synchronized non-OP client state.
RankEnforcementEvents, RankCommand and DungeoneerCommand synchronize developer OP/dungeoneer
de-OP. This is a presentation hint, not an authorization change.
Metalmancer's ExtraInventoryScreen already has no recipe-book component.

Vanilla book visibility is locally suppressed at initialization and ticking; omit the button
and keyboard-focus widget. The native inventory remains centered. Rebuild widgets only when
the condition changes while the inventory is open. Preserve the saved open/filter preference;
do not call the settings mutator or send additional packets. Hide stale recipe ghost previews.
Normal inventories outside instances, developer inventories and other game modes retain
vanilla behavior. Crafting-table/furnace recipe books and all server CraftingPolicy checks remain.
No registry, item, menu slot, inventory, networking schema, saved-data or migration changes.
No datagen applies to client Java/mixin configuration changes.

Validation/handoff: Java21 build, existing JUnit suite, source JSON and diff checks;
native client startup to validate client mixin application. Gameplay QA: survival dungeoneer
inventory in primary/nether instances (book absent); outside/creative/developer unchanged;
remembered-open book, resize/GUI scale, mode/rank transitions, inventory drag/shift-click,
currency header and Metalmancer extra inventory. No dedicated/GameTest server launch.
TEST replacements only while server remains stopped and installed client is closed.
Zero additional planned implementation batches. Possible future improvement: consider
separate crafting-table recipe-book presentation if Cameron requests it.

Validation completed: Java21 build successful; all 64 existing JUnit tests passed without
failures/errors/skips; all 1,997 source JSON files valid; git diff --check passed.
The development client launched successfully to a responding Minecraft window (PID9876).
Its native loader applied both new mixins with no injection/application error; full GUI
behavior and licensed multiplayer acceptance remain manual checks. Unrelated existing
missing-item-texture warnings are outside this task. No world was entered by the agent.
TEST log snapshot still matches the earlier shutdown SHA256 BE22896B122DAE7BB2DF8AA26655752002E6938952541E0E8C9879CFFCC8444C.
Final source checkpoint, committed-receipt build and safe deployment follow. User controls
server restart. Keep original staged historical-JAR deletion and cache/build/log outputs.
