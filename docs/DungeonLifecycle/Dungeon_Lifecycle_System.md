# Dungeon Lifecycle System (Developer) — 1.5

## Runtime model

Dungeon lifecycle is built around snapshot-driven recovery and run-state tracking. The lifecycle stack is implemented through dungeon definitions, run registry data, run progress data, snapshot save/restore, recovery data, and lifecycle service/event hooks.

## What developers should expect

- Dungeon runs track participant and progression state separate from static world layout.
- The first class-selector ready player is treated as the run **Group Leader**.
- Group Leader kicks remove the target from the active run record immediately, restore that player from their pre-run snapshot, clear temporary run side data such as bloom/Plant Flags tracking for that member, and teleport them to the main overworld spawn. Kicked players therefore do not block completion, abandonment, or reset registration.
- Reset operations rely on snapshot data per dungeon.
- Reset attempts can abort if active players remain in dungeon spaces.
- Post-reset cleanup attempts to remove non-player entities and force chunk/cache drain before restore completes.

## Operational consequences

- Snapshot save cadence is a build/ops decision: take a stable snapshot after approved layout edits.
- Do not initiate hard recovery while test players are still inside instance dimensions.
- If restore aborts, rerun after clearing occupants and forced chunk activity.

## Relevant commands

- `/world ...` (dimension / dungeon target routing)
- `/dungeoneer ...` and `/developer ...` (rank-gated operational command surfaces)
- `/region ...`, `/rift ...`, `/spawner ...`, `/door ...` (subsystem state manipulated as part of lifecycle rehearsals)

## Dungeon AFK handling

Active dungeon runs now include transient AFK tracking for dungeoneers. See [Dungeon AFK Handling](./Dungeon_AFK_Handling.md) for the 15-minute threshold and Group Leader prompt behavior, and see [Dungeon Group Split](./Dungeon_Group_Split.md) for mob-kill Trace payouts that exclude AFK, cross-world, and distant members.
