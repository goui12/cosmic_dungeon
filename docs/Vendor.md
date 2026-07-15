# Vendor

Vendors are assigned NPC shops that use the Cosmic Dungeon Attunement Fragment currency system. For currency denominations, commands, and account behavior, see [In-Game Commands](./commands/In_Game_Commands.md#currency-attunement-fragment-economy-foundation).

## Opening and using a vendor

- Right-click an assigned vendor NPC to open that vendor's GUI.
- Locked vendors do not open their shop and instead show the server-authoritative reason the vendor is unavailable.
- The vendor name stays near the top of the GUI.
- Your current currency balance appears in the bottom-right corner and updates after successful vendor transactions.

## Buying items

- The left pane is labeled **Vendor Selling:**.
- Each row shows the item stack, a shortened item name when space allows, the configured offer cost, and a **Buy** button.
- Offer costs come from the vendor profile's `cost.amount` plus `cost.denomination`, are converted to total Trace with the same denomination rules used by the server, and display as normalized abbreviations. For example, `{ "amount": 570, "denomination": "trace" }` displays as `5S 7M`, and `{ "amount": 3, "denomination": "mark" }` displays as `3M`.
- Locked offers are visibly marked and cannot be clicked.
- If the vendor has more offers than fit on one page, use the arrow buttons or mouse wheel to cycle pages.

## Selling your items

- The right pane is labeled **Player Inventory**.
- Only stacks this vendor can buy are shown. Sellability is based on the vendor pricing group and the same pricing rules used by `VendorPricingService.getSellValue(...)`.
- Items are valued independently: each sellable stack contributes its own stored or configured sell value to the payout.
- Complete armor sets do not receive special vendor sale treatment; selecting or selling all four matching class-attuned armor pieces pays only the sum of the individual piece values.
- Hovering a listed stack shows its normal item tooltip.
- Click a sellable item stack to select it; click it again to deselect it. Selected stacks are outlined with a white border, and multiple stacks can be selected at once.

## Sell Selected

- **Sell Selected** sells only the stacks you have selected in **Player Inventory**.
- The preview next to the button shows the current selected payout, such as `Selected: 40 Trace`.
- When nothing is selected, the preview shows `Selected: 0 Trace` and the button is disabled.
- The server recalculates the payout from individual stack values, rechecks the vendor, validates each inventory slot, ignores duplicate slot indexes safely, confirms every stack is still sellable, checks currency capacity, then removes items and deposits currency atomically.

## Sell All

- **Sell All** sells every stack in your inventory that this vendor can buy, including sellable stacks hidden behind the visible row limit.
- The preview next to the button shows the current full-inventory payout, such as `All: 100 Trace`.
- If there are no sellable items, the preview shows `All: 0 Trace` and the button is disabled.
- The server recalculates the full sell-all payout from the current inventory and vendor pricing group before removing anything.

## Currency overview

- Balances are stored server-side as total Trace.
- Higher denominations are converted through the currency model: Mark, Seal, Crown, and Anchor are worth increasing amounts of Trace.
- Vendor purchases withdraw from the player's stored balance, and vendor sales deposit payout into that same balance.
- Failed transactions do not partially remove items or partially deposit currency.

## Vendor entity shells

- `/vendor spawn <profileId>` still creates the default villager vendor shell.
- `/vendor spawn <profileId> <mobType>` creates another mob shell, such as `/vendor spawn d1/general_supply_vendor horse`. Short vanilla IDs are interpreted as `minecraft:<id>`; full modded entity IDs are also accepted.
- Assigned vendor shells show the vendor profile display name as a bold bright neon-green overhead name, are persistent, invulnerable, and have AI disabled so they stand still instead of roaming.
- Vendor opening and transaction validation are server-authoritative for any assigned mob shell, so client-side screen/network behavior continues to use the spawned entity id and cannot bypass distance, access-gate, profile, or buyback validation.

## Vendor profile authoring

Vendor profiles load from datapack JSON under `data/cosmicdungeon/vendor_profiles/<path>.json`. Profile IDs should be stable full `ResourceLocation` values; commands also accept unambiguous short aliases for common operator workflows. Invalid entries log clear errors rather than hard-crashing the server.

### Optional offer/result fields

- Buy offers may declare `maxPurchasesPerPlayer` as a positive integer. The server stores successful purchases in additive saved data (`cosmicdungeon_vendor_purchase_limits_v1`) keyed by player UUID, vendor profile id, and offer id. Existing worlds do not require manual conversion because this is a new, separate saved-data file. Failed purchases, failed delivery, and refunded purchases do not increment the count.
- Buy offers still parse the older `maxUses` field where present, but per-player limits use `maxPurchasesPerPlayer` so the semantics are explicit.
- Offer results may declare `maxStackSize` as a positive integer up to Minecraft's safe item-stack cap. This writes the vanilla `DataComponents.MAX_STACK_SIZE` component only onto the vendor-provided `ItemStack`; it does not globally change the item definition. `result.count` remains the number of items sold per click.

## D1 vendor profiles

- `cosmicdungeon:d1/brewing_store` opens **Eon Penrose**, a D1 tier-3 village-access Brewing Store with general brewing ingredients, Theurgist-only brewing equipment and advanced healing/regeneration stock, and Theurgist/Judicator shared healing offers. See [Eon Penrose](./Vendors/Eon_Penrose.md).
- `cosmicdungeon:d1/d1_nether_gritch_of_the_barter_pit` opens **Gritch of the Barter Pit**, a D1 tier-1 village-access vendor that sells one [golden carrot](https://minecraft.wiki/w/Golden_Carrot) for `1000` Trace and one [glistering melon slice](https://minecraft.wiki/w/Glistering_Melon_Slice) for `1000` Trace.
- Spawn or inspect this profile with the standard [vendor commands](./commands/In_Game_Commands.md#vendor-15-progression--faction-access), such as `/vendor spawn d1/d1_nether_gritch_of_the_barter_pit` or `/vendor profile d1_nether_gritch_of_the_barter_pit`.

## Access gates

Vendor access is evaluated centrally before a GUI opens. Profiles can require village access, NPC system/tier progress, faction standing, or future progression flags. Locked vendors show the server-authoritative denial reason instead of opening. Use `/vendor access <profileId>` to test the executing player against a profile. Offer rows can also be locked by supported offer-level gates; these rows are displayed as locked in the GUI and rechecked on the server purchase path.

## Pricing and buyback authoring notes

Buyback uses each eligible stack's individual sell value. Class-attuned items sell for their stored Trace value, including armor pieces; complete sets do not receive special set bonuses.

## Offer-level gates

Buy offers support the existing optional `requiredProgressionFlag`, `requiredNpcTier`, and `requiredFactionTier` fields. `requiredFactionTier` is evaluated against the profile-level `requiredFaction` id; if an offer declares a faction tier but the profile has no registered `requiredFaction`, the offer remains locked. Profiles that declare an invalid or unregistered `requiredFaction` id are rejected during vendor-profile loading. Buy offers also support optional `requiredClasses`, an array of canonical class ids from `ClassKeys`, such as `["theurgist"]` or `["theurgist", "judicator"]`. Missing or empty `requiredClasses` means general access. Profile loading rejects unknown class ids. Offer-level class gates are server-authoritative: locked rows are disabled in the GUI, and `VendorService.tryPurchase` rechecks `VendorMenuState.isOfferUnlocked` before currency is withdrawn or items are delivered.

## Related topics

- [Economy & Currency](./Economy/Economy_and_Currency.md)
- [Pricing Master List](./Economy/Pricing_Master_List.md)
- [Progression, Factions & Unlocks](./Progression/Progression_Factions_and_Unlocks.md)
- [NPC/Vendor Faction Notes](./Factions/NPC_Vendor_Faction.md)
- [Commands: Vendor](./commands/In_Game_Commands.md#vendor-15-progression--faction-access)

## Changelog

- **1.5.1:** Added `maxPurchasesPerPlayer`, vendor-result `maxStackSize`, and normalized purchase-cost display support for vendor profiles.
- **1.5.1:** Added server-authoritative offer-level `requiredClasses` gates and activated them for Eon Penrose brewing-store stock.
- **1.5.1:** Activated server-side evaluation for existing offer-level `requiredFactionTier` locks.
- **1.5.1:** Added the D1 Gritch of the Barter Pit vendor profile for expensive golden food purchases.
- **1.5.1:** Added optional mob-type vendor shells, bright neon-green friendly overhead names, and enforced invulnerable/no-AI vendor standing behavior.
- **1.5:** Added assigned vendor NPCs, profile-loaded offers, buy/sell GUI flows, centralized access gates, profile aliases, buyback, and atomic transaction validation.

## Elias Centvin and pricing design notes

Elias Centvin is documented as the current D1 weapon-supplier profile plus future design direction in [Elias Centvin](Vendors/Elias_Centvin.md). Pricing sources and future balancing references are tracked in [Pricing Master List](Economy/Pricing_Master_List.md). NPC faction pricing and stock overhauls are not live unless encoded in current vendor profiles or server-side vendor code.
