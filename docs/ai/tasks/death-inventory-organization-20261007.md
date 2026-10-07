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

Java 21 local build (364 tests), 34 Python safeguards and 2,001 JSON files passed. PR, merged-main and release clean CI
passed all 49 native GameTests; automatic review completed without findings. Native fixtures use real
ServerPlayer death/respawn and explicit Survival capacity; no assertion was removed or weakened.
Coverage includes cancellation/duplicate death, save/load/legacy defaults, latest-death replacement/retirement,
partial/merged pickups, occupied targets, provenance, cursor/external-menu custody, recovered armor and
physical pickup following the existing duplicate-resurrection race check.
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

- docs/ai/D1_REMAINING.md
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
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
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryResurrectionGameTests.java
- src/main/java/net/goui/cosmicdungeon/mixin/DeathCurrencyPlayerMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/DeathInventoryItemMixin.java
- src/main/resources/cosmicdungeon.mixins.json
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/DeathInventoryLayoutTest.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java

## Final runtime and deployment evidence

Runtime/tag source: ad91c1296fe1820ec82a0e454f346dd2d4059d2e. PR234 merged Set A into main with the same tested tree.
PR CI 37580829518, merged-main CI 37581180762 and release CI 37581500010 passed all 49 native GameTests.
Local Java 21 build (364 tests), 34 Python safeguards, 2,001 source JSON files and diff checks passed.
Automatic review completed without findings; later native-fixture-only corrections were reviewed and fully retested.
CurseForge main 9086976 and loading companion 9086975 submitted; moderation/licensed app delivery remain unverified.
Exact CI main SHA256: a8badbe279ad3bcd2a6144af811b24aa2bc5426c8be5321499a4c0dc4d427142.
Matching helper SHA256: c87228f2170c442805e2411dc3d81184b28801203335607f0f1fa36a08348d65.
Fresh independently observed stopped TEST and closed ADMIN received the exact CI runtime; ADMIN helper also matches.
Installed hashes and latest-built/current-test feeds match; deployment journals resolved.
No local client/GameTest/server launch, forced close, restart or server.properties change.
Compatible additive layout data stays in existing D1RunData; no registry/spawner/preset/network schema migration or datagen.
Licensed multiplayer/visual interaction and CurseForge companion app acceptance remain pending.

Queue paused after 16 at Cameron's request; 10 batches (17-26) remain. Batch 17 has not been claimed.
