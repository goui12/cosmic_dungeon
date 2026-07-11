# Rift System Guide (Developer) — 1.5

## System behavior

Rifts are authored as placer+tile constructs and registry-backed destinations.

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
