# Elias Centvin Vendor Notes

## Current profile

Elias Centvin is represented by the existing D1 weapon-supplier vendor profile:

- Profile id: `cosmicdungeon:d1/weapon_supplier`
- Data path: `src/main/resources/data/cosmicdungeon/vendor_profiles/d1/weapon_supplier.json`
- Display name: `Elias Centvin`
- Vendor type: `weapon_supplier`
- Access gates: village access, `requiredNpcSystem: "D1"`, and `requiredNpcTier: 2`

Because D1 NPC tier 2 currently derives from 10 cumulative Lesser Blooms, Elias's current profile matches the design note that vendor access begins at 10 Lesser Blooms through village/NPC progression. The runtime gate is server-authoritative through vendor access and progression services.

## Current stock

Elias currently sells straightforward weapon/tool supplies encoded directly in the vendor profile: stone and diamond swords, stone and diamond pickaxes, bow, crossbow, arrows, and shield. Prices are live only because they are encoded in the profile JSON.

## Dragoon repair-material design note

Design notes call for Dragoon-only repair materials through Elias. The custom material items now exist (`cosmicdungeon:leather_patch`, `cosmicdungeon:chain_link`, and `cosmicdungeon:netherite_repair_fragment`), but those offers are **not** currently added to Elias's profile because the active vendor schema still has no class-restricted offer field. Do not add Dragoon-only repair materials as normal public offers until a complete server-authoritative offer class gate is implemented and documented.

## NPC/vendor faction pricing design note

NPC/vendor faction pricing multipliers are design-only for Elias at this time. Current code supports profile-level faction access and offer-level faction tier locks tied to the profile faction id, but it does not implement dynamic faction price multipliers.

## Related docs

- [Vendor](../Vendor.md)
- [Pricing Master List](../Economy/Pricing_Master_List.md)
- [NPC/Vendor Faction](../Factions/NPC_Vendor_Faction.md)
- [Progression, Factions & Unlocks](../Progression/Progression_Factions_and_Unlocks.md)

## Repair Affinity materials now consumed by Dragoon UI

Elias's Dragoon repair material theme now feeds the live player-to-player Repair Affinity system: Dragoons consume Leather Patches, Chain Links, Netherite Repair Fragments, and vanilla material ingots/gems from their inventory when repairing another player's supported damaged gear. This is not a direct Elias shop repair service, and the customer labor fee remains optional player-negotiated account currency in the Repair Affinity UI.
