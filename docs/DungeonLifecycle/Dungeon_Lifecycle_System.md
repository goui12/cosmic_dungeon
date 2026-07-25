# Dungeon Lifecycle System (Developer) — 1.5

## Runtime model

Dungeon lifecycle is built around snapshot-driven recovery, run-state tracking, and a fixed pool of ten logical instance slots. Each slot has a primary physical dimension and a reserved Nether companion; Dungeon 1 uses both while the other current dungeon profiles use only the primary dimension.

## What developers should expect

- Dungeon runs track participant and progression state separate from static world layout.
- A class-selector start leases the first free slot, refreshes it from the selected template, copies template rift linkage, and teleports the party into the physical instance. Multiple groups may run the same dungeon profile concurrently.
- Active run records persist their slot and physical dimension ids. Logout and server restart do not abandon the run; members reconnect in place when they logged out inside the instance and may otherwise return through a lifecycle-aware rift or their bound Farrow's Chop.
- Farrow's Chop visits to Main Village persist separate dungeon and outside inventory snapshots. Lifecycle cleanup retains the outside snapshot for a member who is away when the run ends, preventing dungeon items from escaping through village storage.
- Template-bound rifts resolve to the member's active instance. Developers outside a lifecycle retain literal template access; ordinary players without a matching lifecycle are rejected. Non-dungeon destinations remain literal.
- The first class-selector ready player is treated as the run **Group Leader**.
- Group Leader kicks remove the target from the active run record immediately, restore that player from their pre-run snapshot, clear temporary run side data such as bloom/Plant Flags tracking for that member, and teleport them to the main overworld spawn. Kicked players therefore do not block completion, abandonment, or reset registration.
- Reset operations rely on snapshot data per dungeon.
- Completion refreshes the leased slot from its template before releasing it. A slot whose refresh permanently fails is quarantined by its failed run record rather than reused.
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

## Persistence and upgrades

Run SavedData keeps the existing `cosmicdungeon_dungeon_runs` id and adds an optional `instance_slot` field. Older records decode with slot `0`; on server start, active/resetting legacy records are copied into an available fixed slot and updated only after a successful refresh. Back up the entire world before upgrading. Rollback to a jar without instance support should use that backup because older code does not understand physical instance dimensions.

The feature does not change Cosmic Spawner block-entity NBT, preset NBT/JSON, registry ids, door/key codecs, region codecs, rift destination records, currency, faction, progression, achievement, or class storage. Template block entities—including placed Cosmic Spawners—are copied with the template dimension data, so no spawner migration or manual recreation is required.

## Dungeon AFK handling

Active dungeon runs now include transient AFK tracking for dungeoneers. See [Dungeon AFK Handling](./Dungeon_AFK_Handling.md) for the 15-minute threshold and Group Leader prompt behavior, and see [Dungeon Group Split](./Dungeon_Group_Split.md) for mob-kill Trace payouts that exclude AFK, cross-world, and distant members.
