# Dungeon 1 startup schematic pipeline

## Summary

Dungeon 1 startup now executes six ordered schematic groups with six logical player slots per group, for exactly 36 server-side paste operations before run registration and teleportation.

## Developer or operator behavior

- Parties of one through six preserve class-selector ready order.
- Occupied slots use their selected class; every unoccupied slot uses the required `blankslot` schematic for each group.
- The batch targets only the prepared physical primary instance and never the reusable template.
- Run registration and teleportation require an explicit 36-operation success. An unexpected mid-batch failure leaves the physical instance unregistered and available for the next preparation refresh, though partial physical changes may remain until that refresh.

## Exact systems affected

- Class-selector Dungeon 1 startup orchestration.
- Server-controlled WorldEdit schematic planning and execution.
- Dungeon instance GameTests for the pure startup plan.

## Saved-data and migration effects

No SavedData, NBT, codec, registry, network, snapshot, or schematic format changed. Existing 1.5.0 and 1.5.1 worlds remain storage-compatible and require no migration.

## Server/client and security implications

The prepared physical `ServerLevel` is the sole WorldEdit target. The executor is server-authoritative and introduces no player actor, local WorldEdit session, player clipboard, movement mutation, or client-only dependency in the shared jar.

## Automated validation performed

- Pure GameTests cover one-, four-, and six-player plans, duplicate classes, definition integrity, unsupported party counts, and defensive blank entries.
- Java 21 clean build, all 17 required GameTests, repository JSON validation, and diff checks passed.

## Remaining manual Minecraft QA

Verify one-, intermediate-, and six-player runs; duplicate classes; all four rotations; paste-air behavior; all 36 physical-instance placements; and that the reusable template remains unchanged.

## Documentation updated

- `docs/Classes/Class_Selector_System.md`
- `docs/DungeonLifecycle/Dungeon_Lifecycle_System.md`
