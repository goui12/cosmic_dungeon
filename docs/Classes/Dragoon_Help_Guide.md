# Dragoon Help Guide

## Class ID

- `dragoon`

## Availability

- Selectable class ID exists.
- Current selector availability: not separately documented as disabled.

## Overview

Dragoon is a melee-forward class with an innate chain-lightning passive that occasionally turns a weapon hit into a short-range mob-clearing burst.

## Abilities

### Innate passive: Chain Lightning

- Every Dragoon hit against an enemy mob has a **3% chance** to trigger chain lightning.
- When triggered, the lightning uses the same final on-hit damage that the Dragoon was already going to deliver. For example, a 3-damage sword hit chains for 3 damage per chained mob.
- The chain can hit up to **7 mobs total** in a full **360-degree** search around the Dragoon.
- Eligible chained mobs must be within **3 blocks** of the Dragoon.
- The lightning bounces through the available mobs in a randomized nearby order and plays the `dragoon_lightning_particle` visual between each bounce.
- Chain-lightning damage does not recursively trigger additional chain lightning.

## Benefits

- Access to Dragoon-attuned equipment, when such equipment is authored.
- Access to Dragoon class-locked chests.
- Server-authoritative anvil access for vanilla repair/support interactions.

## Incentives

- Occasional burst cleave while staying focused on melee combat.

## Team roles

- TBD.

## Class restrictions and access

- Can use/wear equipment attuned to `dragoon`.
- Can open Dragoon class-locked chests.
- Can use anvils; non-Dragoons are denied unless they have developer bypass.
- Future custom repair-affinity UI is design-only and is not implemented yet. See [Dragoon Repair System](Dragoon_Repair_System.md).

## Known gaps / TBD

- Define intended party role.
- Define player-facing incentives.
- Define any class-specific commands, resource systems, or progression hooks.
