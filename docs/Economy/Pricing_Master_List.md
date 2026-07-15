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


## Elias Centvin live D1 weapon-supplier pricing

Elias Centvin's live `cosmicdungeon:d1/weapon_supplier` profile stores costs as Trace totals and sells one item per click.

| Stock | Cost | Live status | Gate |
| --- | ---: | --- | --- |
| Wooden Sword | 5 Marks | D1 live | General |
| Stone Sword | 8 Marks | D1 live | General |
| Golden Sword | 1 Seal, 6 Marks | D1 live | General |
| Copper Sword | 2 Seals, 5 Marks | D1 live | General |
| Iron Sword | 4 Seals, 9 Marks | D1 live | General |
| Diamond Sword | 5 Seals, 7 Marks | D1 live | General |
| Netherite Sword | 6 Seals, 9 Marks | D1 live | General |
| Wooden Pickaxe | 6 Marks | D1 live | General |
| Stone Pickaxe | 1 Seal | D1 live | General |
| Golden Pickaxe | 1 Seal, 8 Marks | D1 live | General |
| Copper Pickaxe | 3 Seals, 1 Mark | D1 live | General |
| Iron Pickaxe | 6 Seals, 2 Marks | D1 live | General |
| Diamond Pickaxe | 7 Seals, 2 Marks | D1 live | General |
| Netherite Pickaxe | 8 Seals, 8 Marks | D1 live | General |
| Shield | 2 Seals, 4 Marks | D1 live | General |
| Bow | 2 Seals, 2 Marks | D1 live | General |
| Crossbow | 3 Seals, 6 Marks | D1 live | General |
| Trident | 5 Seals, 8 Marks | D1 live | General |
| Arrow | 1 Trace | D1 live, max stack 64 | General |
| Leather Patch | 2 Trace | D1 live | Dragoon only |
| Gold Ingot | 4 Trace | D1 live | Dragoon only |
| Copper Ingot | 8 Trace | D1 live | Dragoon only |
| Chain Link | 1 Mark, 2 Trace | D1 live | Dragoon only |
| Iron Ingot | 1 Mark, 6 Trace | D1 live | Dragoon only |
| Diamond | 1 Mark, 8 Trace | D1 live | Dragoon only |
| Netherite Repair Fragment | 2 Marks, 2 Trace | D1 live | Dragoon only |

Direct Elias shop repair services remain design/future because the live vendor system does not implement a direct repair-service offer type.

## Naton Whitlock live D1 general supply pricing

| Stock | Cost | Live status |
| --- | ---: | --- |
| Flint and Steel | 3 Marks | D1 live |
| Torch | 1 Trace | D1 live, max stack 64 |
| Bread | 2 Trace | D1 live, max stack 64 |
| Baked Potato | 2 Trace | D1 live, max stack 64 |
| Cooked Chicken | 3 Trace | D1 live, max stack 64 |
| Apple | 1 Trace | D1 live, max stack 64 |
| Carrot | 1 Trace | D1 live, max stack 64 |
| Bucket | 5 Marks | D1 live |

## Beatrix Farrow live D1 food-vendor pricing

Beatrix Farrow's live `cosmicdungeon:d1/food_vendor` profile sells one item per click. Raw Farrow's Chop and Farrow's Chop are limited to one purchase per player.

| Stock | Cost | Live status |
| --- | ---: | --- |
| Potato | 1 Trace | D1 live, max stack 20 |
| Kelp | 1 Trace | D1 live, max stack 20 |
| Raw Beef | 2 Trace | D1 live, max stack 20 |
| Raw Chicken | 2 Trace | D1 live, max stack 20 |
| Raw Rabbit | 2 Trace | D1 live, max stack 20 |
| Raw Porkchop | 2 Trace | D1 live, max stack 20 |
| Raw Mutton | 2 Trace | D1 live, max stack 20 |
| Raw Cod | 2 Trace | D1 live, max stack 20 |
| Raw Salmon | 2 Trace | D1 live, max stack 20 |
| Raw Farrow's Chop | 2 Crowns | D1 live, max stack 1, one purchase per player |
| Farrow's Chop | 2 Crowns | D1 live, max stack 1, one purchase per player |
| Beatrix's Campfire | 5 Trace | D1 live, max stack 1 |

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

Live Elias Dragoon-only repair material prices are: Leather Patch 2 Trace; Gold Ingot 4 Trace; Copper Ingot 8 Trace; Chain Link 1 Mark 2 Trace; Iron Ingot 1 Mark 6 Trace; Diamond 1 Mark 8 Trace; Netherite Repair Fragment 2 Marks 2 Trace. Repair Affinity itself does not enforce a mandatory labor fee; customer labor payment is optional account currency selected in the UI.
