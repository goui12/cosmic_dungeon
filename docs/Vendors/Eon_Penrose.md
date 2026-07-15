# Eon Penrose — Brewing Store

Eon Penrose runs the Dungeon 1 Brewing Store profile `cosmicdungeon:d1/brewing_store`.

## Access

- Vendor type: `brewing_store`.
- Requires Dungeon 1 village access.
- Keeps the existing Dungeon 1 NPC unlock gate from the live profile: `requiredNpcSystem: D1`, `requiredNpcTier: 3`.
- Purchases are server-authoritative. Class-locked rows may appear locked in the GUI, and the server rechecks the class gate when a buy request arrives.

## Live Dungeon 1 stock

All live offers sell one item per click. Vendor-result `maxStackSize` is live in the D1 profile: ingredients, bottles, and potions use max stack 20; Brewing Stand and Cauldron use max stack 1. Prices below are per item.

### Ingredients

| Item | Cost | Class gate |
| --- | ---: | --- |
| Nether Wart | 2 Marks | General |
| Sugar | 1 Trace | General |
| Spider Eye | 8 Trace | General |
| Fermented Spider Eye | 9 Trace | General |
| Magma Cream | 7 Trace | General |
| Glistering Melon Slice | 2 Marks | General |
| Blaze Powder | 5 Trace | General |

### Brewing Equipment

| Item | Cost | Class gate |
| --- | ---: | --- |
| Glass Bottle | 5 Trace | General |
| Brewing Stand | 8 Marks | Theurgist only |
| Cauldron | 5 Marks | Theurgist only |

### Potions

| Item | Cost | Class gate |
| --- | ---: | --- |
| Potion of Night Vision | 3 Marks | General |
| Potion of Fire Resistance | 4 Marks | General |
| Potion of Healing | 2 Marks | Theurgist or Judicator |
| Potion of Healing II | 4 Marks | Theurgist only |
| Potion of Regeneration | 4 Marks | Theurgist only |
| Splash Potion of Healing | 3 Marks | Theurgist or Judicator |
| Lingering Potion of Healing | 5 Marks | Theurgist only |

## Dungeon 2 planned stock, not live in D1

These offers are planning notes only until a real Dungeon 2 progression gate exists and is enforced server-authoritatively. They must not be placed into the live D1 profile by accident.

| Planned item | Intended max stack | Planned cost |
| --- | ---: | ---: |
| Potion of Strength | 20 | 4 Marks each |
| Extended Water Breathing | 20 | 4 Marks each |
| Potion of Invisibility | 20 | 5 Marks each |
| Splash Potion of Poison | 20 | 3 Marks each |
| Splash Potion of Invisibility | 20 | 6 Marks each |
| Lingering Potion of Regeneration | 20 | 7 Marks each |

## Related lore

For dungeoneer-facing backstory without price tables, see [Eon Penrose NPC lore](../NPCs/Eon_Penrose.md).
