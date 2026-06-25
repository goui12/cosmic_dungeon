# Update 1.5 Notes

Update 1.5 is a player-facing systems update focused on Attunement currency, vendors, trading, class-attuned gear, D1 progression, and achievement foundations. These notes are intentionally short; use the linked guides for the full behavior, command, and authoring details.

## Added

- **Attunement Fragment currency accounts** with Trace/Mark/Seal/Crown/Anchor denominations, auto-storing tender items, balance commands, capacity controls, and inventory value checks. See [Economy & Currency](../Economy/Economy_and_Currency.md) and [Commands: Currency](../commands/In_Game_Commands.md#currency-attunement-fragment-economy-foundation).
- **Vendor NPC shops** with assigned villager shells, profile-loaded offers, buy/sell GUI flows, access gating, command aliases, and colored feedback. See [Vendor](../Vendor.md) and [Commands: Vendor](../commands/In_Game_Commands.md#vendor-15-progression--faction-access).
- **Player-to-player trading** with invites, clickable accept/deny chat, server-authoritative item/currency offers, the CAPS LOCK look-target request keybind, and a custom trade window. See [Trading Guide](../Trading/Trading_Guide.md) and [Trade GUI Coordinate Map](../Trading/Trade_GUI_Coordinate_Map.md).
- **Class item attunement** for developer-authored class gear, tooltip identity, equipment-use restrictions, and vendor sell values. See [Class Restrictions & Inventory](../Classes/Class_Restrictions_and_Inventory.md) and [Commands: Class item attunement](../commands/In_Game_Commands.md#class-item-attunement-15-foundation).
- **Faction, long-term progression, and D1 unlock data** including JHW standing, Lesser Bloom/Cavern Residue counters, village access, and D1/D2 NPC unlock tiers. See [Progression, Factions & Unlocks](../Progression/Progression_Factions_and_Unlocks.md).
- **Achievement/advancement foundations** for generated Cosmic Dungeon advancements, debug grants, Binding Idol counters, Vital Exchange hooks, Plant Flags tracking, and D1 environmental trackers. See [Achievements & Advancements](../Achievements/Achievements_and_Advancements.md).
- **Lesser Bloom and Cavern Residue content** as real registered D1 collectible/content entries for future progression, vendor, and achievement hooks. See [Progression, Factions & Unlocks](../Progression/Progression_Factions_and_Unlocks.md#d1-collectibles-and-unlocks).

## Changed

- Vendor pricing now uses class-attuned item metadata as the source of truth for individual sell values instead of early hardcoded seed values. See [Vendor: Pricing and buyback](../Vendor.md#pricing-and-buyback-authoring-notes).
- D1 progression terminology now uses **Lesser Bloom** instead of placeholder Torch Flower naming. See [Progression, Factions & Unlocks](../Progression/Progression_Factions_and_Unlocks.md#d1-collectibles-and-unlocks).
- Vendor access checks now run through one centralized evaluator for village, NPC-tier, progression-flag, and faction-gate outcomes. See [Vendor: Access gates](../Vendor.md#access-gates).
- Cosmic Dungeon controls now share one client keybind category for spawner preset and trade-request bindings. See [Commands: Trade](../commands/In_Game_Commands.md#trade-15-player-to-player-foundation).

## Fixed / hardened

- Vendor purchases and buyback now validate range, access, profile/offer state, balance, capacity, and inventory delivery before committing so currency and items do not partially disappear on failure. See [Vendor](../Vendor.md).
- Trade finalization now preflights both inventories and currency capacity, rolls back currency on failure, returns offered items safely, and cleans up on disconnect/menu close. See [Trading Guide](../Trading/Trading_Guide.md#safety-and-server-authority).
- Class-attuned equipment restrictions are enforced server-side for guarded tools, weapons, shields, bows, tridents, maces, and armor/equippable gear. See [Class Restrictions & Inventory](../Classes/Class_Restrictions_and_Inventory.md#class-attuned-equipment).
- Vendor profile and command alias handling now reports invalid or ambiguous IDs clearly instead of silently choosing a profile. See [Vendor](../Vendor.md#vendor-profile-authoring).

## Documentation map

Start with the renamed [Developer Documentation 1.5](../Developer_Documentation_1.5.md), then use the topic pages linked above. Older 1.4.8/1.4.9 prep notes remain only as historical release material.
