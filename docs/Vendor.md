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
- Offer costs come from the vendor profile's `cost.amount` plus `cost.denomination`. For example, an offer configured as amount `8` and denomination `trace` displays as `8 Trace`, while amount `3` and denomination `mark` displays as `3 Mark`.
- Costs are not shown as normalized balance strings in offer rows; they use the raw configured profile denomination so the price matches the profile authoring intent.
- Locked offers are visibly marked and cannot be clicked.
- If the vendor has more offers than fit on one page, use the arrow buttons or mouse wheel to cycle pages.

## Selling your items

- The right pane is labeled **Your Items**.
- Only stacks this vendor can buy are shown. Sellability is based on the vendor pricing group and the same pricing rules used by `VendorPricingService.getSellValue(...)`.
- Items are valued independently: each sellable stack contributes its own stored or configured sell value to the payout.
- Complete armor sets do not receive special vendor sale treatment; selecting or selling all four matching class-attuned armor pieces pays only the sum of the individual piece values.
- Hovering a listed stack shows its normal item tooltip.
- Click a sellable item stack to select it; click it again to deselect it. Selected stacks are outlined with a white border, and multiple stacks can be selected at once.

## Sell Selected

- **Sell Selected** sells only the stacks you have selected in **Your Items**.
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
