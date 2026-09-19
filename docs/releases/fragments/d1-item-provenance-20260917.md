# D1 named equipment identity and transaction eligibility

Named vanilla loot previously lacked trusted identities, and ordinary names could not
safely support the documented payouts. Add 23 D1 catalogue IDs with exact base items,
explicit developer adoption preserving every existing stack component, and generic
server-issued vendor equipment provenance.

Equipment sale/trade now requires trusted origin; malformed identities, class/bound
restrictions and invalid repair markers are rejected. No-sale and no-trade are checked
for their respective operation at insertion and final validation. Old unclassified
equipment remains intact but needs reviewed adoption before these transactions.

Named final payouts are configured in all_vendors_prices.config from the 2026-08-28
loot table, including the 900-Trace Recovered Spyglass. They include enchantment value;
illegal enchantment sets are still rejected. Named loot is not vendor retail stock.

Protocol 4 and a new optional item_provenance component require matching client/server
jars. No save ID or spawner schema changes, no world rewrite, no new PNG/datagen output.
Java 21 build, 727 offline checks and two config round trips pass. Real item/menu/world
round trips remain licensed TEST acceptance work. No deployment/runtime occurred.

Legacy adoption, no-drop routes and the durable Tamsin Tax removal/achievement journal
remain detailed TODOs; this batch does not activate a tax payment path.
