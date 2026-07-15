# Elias Centvin Vendor Notes

## Current live profile

Elias Centvin is the live Dungeon 1 weapon supplier profile:

- Profile id: `cosmicdungeon:d1/weapon_supplier`
- Data path: `src/main/resources/data/cosmicdungeon/vendor_profiles/d1/weapon_supplier.json`
- Display name: `Elias Centvin`
- Vendor type: `weapon_supplier`
- Access gates: village access, `requiredNpcSystem: "D1"`, and `requiredNpcTier: 2`

D1 NPC tier 2 currently derives from cumulative Lesser Bloom/NPC progression, so Elias remains gated behind restored-village progress. This is separate from Watson's six spectral bloom quest.

## Live weapon, tool, and ammunition stock

All live offers sell one item per click at the listed per-item price. Stack caps are represented with vendor-result `maxStackSize` where the live vendor schema supports it.

| Item | Cost | Notes |
| --- | ---: | --- |
| Wooden Sword | 5 Marks | Live |
| Stone Sword | 8 Marks | Live |
| Golden Sword | 1 Seal, 6 Marks | Live |
| Copper Sword | 2 Seals, 5 Marks | Live where the target Minecraft/NeoForge item exists |
| Iron Sword | 4 Seals, 9 Marks | Live |
| Diamond Sword | 5 Seals, 7 Marks | Live |
| Netherite Sword | 6 Seals, 9 Marks | Live |
| Wooden Pickaxe | 6 Marks | Live |
| Stone Pickaxe | 1 Seal | Live |
| Golden Pickaxe | 1 Seal, 8 Marks | Live |
| Copper Pickaxe | 3 Seals, 1 Mark | Live where the target Minecraft/NeoForge item exists |
| Iron Pickaxe | 6 Seals, 2 Marks | Live |
| Diamond Pickaxe | 7 Seals, 2 Marks | Live |
| Netherite Pickaxe | 8 Seals, 8 Marks | Live |
| Shield | 2 Seals, 4 Marks | Live |
| Bow | 2 Seals, 2 Marks | Live |
| Crossbow | 3 Seals, 6 Marks | Live |
| Trident | 5 Seals, 8 Marks | Live |
| Arrow | 1 Trace | Live, vendor result max stack 64 |

## Live Dragoon-only repair-material stock

Elias now sells Dragoon-only repair materials through server-authoritative `requiredClasses: ["dragoon"]` offer gates.

| Item | Cost | Class gate |
| --- | ---: | --- |
| Leather Patch | 2 Trace | Dragoon only |
| Gold Ingot | 4 Trace | Dragoon only |
| Copper Ingot | 8 Trace | Dragoon only |
| Chain Link | 1 Mark, 2 Trace | Dragoon only |
| Iron Ingot | 1 Mark, 6 Trace | Dragoon only |
| Diamond | 1 Mark, 8 Trace | Dragoon only |
| Netherite Repair Fragment | 2 Marks, 2 Trace | Dragoon only |

## Direct shop repair services

Direct Elias shop repair services are still design/future. The live source supports vendor item offers and the separate Dragoon Repair Affinity flow; it does not implement a direct vendor repair-service offer type.

## NPC/vendor faction pricing design note

NPC/vendor faction pricing multipliers are design-only for Elias at this time. Current code supports profile-level faction access and offer-level faction tier locks tied to the profile faction id, but it does not implement dynamic faction price multipliers.

## Related docs

- [Vendor](../Vendor.md)
- [Pricing Master List](../Economy/Pricing_Master_List.md)
- [NPC/Vendor Faction](../Factions/NPC_Vendor_Faction.md)
- [Progression, Factions & Unlocks](../Progression/Progression_Factions_and_Unlocks.md)
