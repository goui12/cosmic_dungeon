# Progression, Factions & Unlocks — 1.5

This guide covers long-term player progression data, faction standing, D1 collectible unlocks, and vendor-access gates.

## Factions

The 1.5 faction foundation includes the JHW baseline faction with a 0-500 scale and tier ladder from Hostile through Ally. Players can read their own standing; developer/console authority is required for other-player reads and mutations.

## D1 collectibles and unlocks

- **Lesser Bloom** is the canonical Dungeon 1 flower collectible.
- **Cavern Residue** is a registered item/block used by progression and future content hooks.
- D1 completion currently expects at least 3 Lesser Blooms.
- Village access unlocks from D1 completion.
- D1 NPC unlock tiers derive from cumulative Lesser Blooms: tier 1 at 5, tier 2 at 10, tier 3 at 15, and tier 4 at 20.

## Vendor access gates

Vendor profiles can require village access, a D1/D2 NPC tier, faction standing, or future progression flags. Use [Vendor: Access gates](../Vendor.md#access-gates) for vendor-specific behavior and [Commands: Vendor](../commands/In_Game_Commands.md#vendor-15-progression--faction-access) for diagnostics.

## Related topics

- [Achievements & Advancements](../Achievements/Achievements_and_Advancements.md) for progression-adjacent achievement hooks.
- [Vendor](../Vendor.md) for player-facing shop lock reasons.
- [Commands: Progression](../commands/In_Game_Commands.md#progression-15-long-term-progression-foundation) and [Commands: Faction](../commands/In_Game_Commands.md#faction-15-foundation) for command syntax.

## Changelog

- **1.5:** Added JHW faction data, Lesser Bloom/Cavern Residue progression counters, village access, D1/D2 NPC unlock tiers, and centralized vendor gate inputs.

## Faction design note links

See [Faction General](../Factions/Faction_General.md) and [NPC/Vendor Faction](../Factions/NPC_Vendor_Faction.md) for current-vs-future faction documentation. These notes do not imply unimplemented NPC faction pricing, stock, or travel services are live.
