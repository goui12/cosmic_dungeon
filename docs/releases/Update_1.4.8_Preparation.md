# Update 1.4.8 Preparation (Developer Facing)

## Required documentation coverage check

Before release, ensure each subsystem page is reviewed by owners:

- Commands
- Dungeon lifecycle
- Regions
- Doors/keys
- Rifts
- Spawners
- RedstoneRF
- Class selector + class restrictions
- Mobs and block interaction contracts
- Known bugs

## Validation checklist

- Run full command drill for door, region, rift, class selector, and spawner operations.
- Verify lifecycle reset routine with no occupants present.
- Confirm dimension context before any destructive rift operation.
- Confirm class-locked chest and class-restricted item behavior by class.
- Confirm spawner presets persist expected entity, gear, and delay properties.

## Go / No-Go

**Go**
- Subsystem owners approved docs and runbooks.
- Known bug mitigations are explicitly documented in operational checklists.

**No-Go**
- Any owner reports doc/behavior mismatch in their subsystem.
- Reset rehearsal fails due to unresolved lifecycle precondition handling.
