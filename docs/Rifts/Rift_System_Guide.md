# Rift System Guide (Developer) — 1.5

## System behavior

Rifts are authored as placer+tile constructs and registry-backed destinations.

Dungeon-template destinations are interpreted server-side. An active lifecycle member is routed to the corresponding physical dimension in their leased instance slot; a developer without an active lifecycle reaches the literal template; an ordinary non-member is denied template/instance access. Destinations in the Overworld and other non-dungeon worlds remain literal. Instance portal linkage is copied from the template at allocation and restored from the template before slot release.

- `cosmic_rift` acts as placement/control surface.
- `cosmic_rift_tile` acts as anchor tile membership for a rift structure.
- Rift destinations and lookup data are stored in registry data.

## Developer interactions

- Define named destinations with explicit dimension context.
- Use list and delete workflows for maintenance and audits.
- Deleting by UI/chat affordance is scoped to current dimension context.

## Command subset

- `/riftdestination ...`
- `/rift list`
- `/rift delete ...`

## Failure and edge behavior

- Unknown dimensions are rejected.
- Deleting from non-rift tiles is rejected.
- Duplicate rift names produce ambiguity errors.

## Travel-service planning boundary

Additional NPC or hub travel-service concepts belong in [Travel Services Design Notes](../Teleportation/Travel_Services_Design_Notes.md) and should not be treated as implemented rift behavior until source support exists.


## Default destination: main_village

On server start, Cosmic Dungeon ensures a default rift destination named `main_village` exists. If it is missing, it is added to the normal rift destination saved data at the Overworld shared spawn position. If it already exists, it is never overwritten, so developer edits persist.

Developers can inspect it with `/rd info main_village` and move it to their current dimension and block position with `/rd move main_village`. Moving the destination updates only the destination record; it does not delete portal links or rift blocks.
