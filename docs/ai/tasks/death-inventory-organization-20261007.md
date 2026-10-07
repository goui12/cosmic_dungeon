# Batch16 - Latest-death inventory organization and Set A Beta

Authorized October6 exclusive Work queue; Batch16 resumed from the verified preflight/yield checkpoint on
`feature/death-inventory-organization-20261007`, base `36f04d8589b740ea6d1b58be3259a2a2b2124efc`.

## Behavior

The latest real, uncancelled D1 death records a bounded slot layout for the player's actual hotbar,
inventory, armor and offhand. The record is a matching/layout aid only: it never owns or recreates items.
Each real dropped item receives server-side entity provenance for the owning player, run, death token and
original inventory slot. Native item merges are permitted only for identical provenance, preventing a
latest-death stack from silently absorbing another player's, another death's or unrelated world items.

When the owner actually picks up a marked latest-death item, native pickup runs first. The organizer compares
the inventory immediately before and after that successful pickup and may move only the positive component-
identical count delta into the original slot. Pre-existing copies are not moved. Incompatible/newly occupied
slots are preserved, partial compatible stacks respect slot and item limits, and equipment destinations use
the actual inventory-menu Slot placement rule. Unsafe trade/repair/vendor/recovery/cursor states skip optional
organization without blocking or rolling back the native pickup.

Normal respawn or resurrection clears the resurrection offer token as before but not the latest-death layout,
so later physical pickup can still reorder the current death's items. A subsequent real death replaces the
layout/token. Repeated death preparation before respawn reuses the same token. Missing, destroyed, stolen or
older-death items remain missing/physical; no chunk search, world scan, item clone or remote reclaim is added.

## Compatibility, persistence and performance

Layout data is an additive bounded value inside existing `D1RunData`; old saves have no layout and remain
valid. The physical ItemEntity marker is transient world-entity provenance, not an ItemStack component,
registry ID, preset field or authored-item rewrite. Run retirement already removes the run-scoped layout.
No new packet/network protocol is required; protocol19 from Batch15 remains. No spawner/door/key/rift/currency
format changes or migration. Datagen is not applicable.

Work is event-driven only at confirmed death, actual native item merge and actual pickup. Inventory comparison
is bounded by the player's inventory size. No tick polling, chunk loading, entity/world scan, dependency, heap
or runtime telemetry change is introduced.

## Validation status and QA

Java21 compilation and the focused pure recovery-planner test pass at implementation checkpoint. Full local
build/Java tests, source JSON/diff checks, clean CI/native GameTests, automated review, merged-main Set A
Integration Gate, exact Beta release and independent target hash verification are required before completion.
No local GameTest/server, Minecraft client or server is launched.

Pending licensed acceptance:
1. Die with mixed hotbar/inventory/armor/offhand items; respawn or resurrect, pick up only some drops, and verify only recovered items return toward their latest-death slots.
2. Put new items in old slots before pickup and verify they are never overwritten; test partial/full stacks and equipment restrictions.
3. Let another player take/drop an item and leave another old-death item behind; verify neither is recreated or reclaimed automatically.
4. Exercise trade/repair/vendor/full-inventory conditions, relog and a second death; verify normal native pickup/custody and latest-death replacement.
5. Verify the complete Set A Beta through the licensed CurseForge profile.

Potential improvement: after licensed multiplayer acceptance, expose an optional visual hint when a recovered
item could not return to its former slot because the player intentionally occupied it.

## Exact task files

- docs/ai/tasks/death-inventory-organization-20261007.md
- docs/releases/1.6.6-beta.1.md
- docs/releases/fragments/batch16-death-inventory-organization.md
- gradle.properties
- src/main/java/net/goui/cosmicdungeon/dungeon/DeathInventoryGameTests.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/DeathInventoryLayout.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/DeathInventoryRecovery.java
- src/main/java/net/goui/cosmicdungeon/item/identity/ProtectedItemLifecycle.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryResurrection.java
- src/main/java/net/goui/cosmicdungeon/mixin/DeathCurrencyPlayerMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/DeathInventoryItemMixin.java
- src/main/resources/cosmicdungeon.mixins.json
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/DeathInventoryLayoutTest.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java

## Final runtime and deployment evidence

Pending full validation, review, merged-main Set A Beta publication and target verification.
