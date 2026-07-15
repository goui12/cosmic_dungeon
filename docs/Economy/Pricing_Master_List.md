# Pricing Master List Notes

## Current implemented pricing sources

- Currency balances are stored as total Trace and displayed through Trace denominations.
- Vendor buy offers are defined in vendor profile JSON.
- Vendor buyback values can come from class-attuned item trace metadata and profile buyback rules.
- Player-to-player trading uses the same account balance but does not enforce vendor price tables.

## Design/future pricing notes

The pricing master list is a balancing reference for future vendor stock, buyback, faction pricing, and repair-cost work. Values in this document are not live unless encoded in vendor profile data, item metadata, or server-side pricing code. Keep this as a developer/world-designer reference and do not dump the full list into the player H help menu.

## Related docs

- [Economy and Currency](Economy_and_Currency.md)
- [Vendor](../Vendor.md)
- [Elias Centvin](../Vendors/Elias_Centvin.md)

## Eon Penrose Brewing Store pricing

Eon Penrose's live Dungeon 1 Brewing Store prices are encoded in `cosmicdungeon:d1/brewing_store`. All live purchases are one item per click and costs are per item.

| Stock | Cost | Live status | Class gate |
| --- | ---: | --- | --- |
| Nether Wart | 2 Marks | D1 live | General |
| Sugar | 1 Trace | D1 live | General |
| Spider Eye | 8 Trace | D1 live | General |
| Fermented Spider Eye | 9 Trace | D1 live | General |
| Magma Cream | 7 Trace | D1 live | General |
| Glistering Melon Slice | 2 Marks | D1 live | General |
| Blaze Powder | 5 Trace | D1 live | General |
| Glass Bottle | 5 Trace | D1 live | General |
| Brewing Stand | 8 Marks | D1 live | Theurgist only |
| Cauldron | 5 Marks | D1 live | Theurgist only |
| Potion of Night Vision | 3 Marks | D1 live | General |
| Potion of Fire Resistance | 4 Marks | D1 live | General |
| Potion of Healing | 2 Marks | D1 live | Theurgist or Judicator |
| Potion of Healing II | 4 Marks | D1 live | Theurgist only |
| Potion of Regeneration | 4 Marks | D1 live | Theurgist only |
| Splash Potion of Healing | 3 Marks | D1 live | Theurgist or Judicator |
| Lingering Potion of Healing | 5 Marks | D1 live | Theurgist only |
| Potion of Strength | 4 Marks each | D2 planned only | TBD |
| Extended Water Breathing | 4 Marks each | D2 planned only | TBD |
| Potion of Invisibility | 5 Marks each | D2 planned only | TBD |
| Splash Potion of Poison | 3 Marks each | D2 planned only | TBD |
| Splash Potion of Invisibility | 6 Marks each | D2 planned only | TBD |
| Lingering Potion of Regeneration | 7 Marks each | D2 planned only | TBD |

## Dragoon repair material prices

Design source prices for Elias Dragoon-only repair materials are: Leather Patch 2 Trace; Gold Ingot 4 Trace; Copper Ingot 8 Trace where copper gear/material support exists; Chain Link 1 Mark 2 Trace; Iron Ingot 1 Mark 6 Trace; Diamond 1 Mark 8 Trace; Netherite Repair Fragment 2 Marks 2 Trace. Repair Affinity itself does not enforce a mandatory labor fee; customer labor payment is optional account currency selected in the UI.
