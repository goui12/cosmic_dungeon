# Update 1.5.0 Notes (Initial Implementation)

## Attunement Fragment economy account foundation

This is the first 1.5.0 implementation pass and introduces backend account plumbing only.
No vendors, purchase GUI, or gameplay sinks/sources are included in this update.

### Currency model

Legal-tender denominations (non-vanilla currency):

- TRACE
- MARK
- SEAL
- CROWN
- ANCHOR

Exchange ratio is decimal by tier:

- 10 Trace = 1 Mark
- 10 Mark = 1 Seal
- 10 Seal = 1 Crown
- 10 Crown = 1 Anchor

All balances are stored internally as total `Trace` using `long`.

### Persistence and account data

- Added persistent player economy data via overworld `SavedData`.
- Balance data persists across server restarts.
- Per-player capacity override support is included.
- Default account capacity is currently `10,000 Trace`.

### Commands

Added `/currency` root command:

- `/currency balance`
- `/currency balance <player>`
- `/currency add <player> <denomination> <amount>`
- `/currency remove <player> <denomination> <amount>`
- `/currency set <player> <denomination> <amount>`
- `/currency clear <player>`
- `/currency capacity <player> <traceAmount>`
- `/currency value`
- `/currency value inventory`

Permission model:

- Any player can run self-balance.
- Other-player balance checks and all admin mutations require developer/console authority.

### Scope boundary

This update intentionally does **not** include:

- vendors
- economy GUI/menu screens
- world sinks/sources beyond pickup auto-storage

These are planned for future 1.5.x follow-up work.


## 1.5.0 Follow-up: Attunement fragment item tender + auto-storage

- Added five legal-tender item forms: `attunement_trace`, `attunement_mark`, `attunement_seal`, `attunement_crown`, and `attunement_anchor`.
- These items are drop-compatible as world entities but auto-deposit into player currency balance on successful pickup.
- Pickup uses all-or-nothing capacity checks: if full capacity blocks the full stack value, no partial deposit occurs and the entity remains in-world.
- Capacity-denied pickup sends a short, rate-limited actionbar denial message.
- No crafting recipes were added for these tender items.


## 1.5.0 Follow-up: Vendor pricing foundation (no vendors yet)

- Added a new pricing package: `net.goui.cosmicdungeon.economy.pricing`.
- Added foundational types: `VendorValueCategory`, `VendorPrice`, `GearSetDefinition`, and `VendorPricingService`.
- Added starter isolated hardcoded set definitions (structured for later JSON migration under `data/cosmicdungeon/vendor_prices/*.json`).
- Included starter pricing for the D2 T1 Judicator chainmail set:
  - `visor_of_the_resolute`
  - `cuirass_of_purpose`
  - `chausses_of_the_pledge`
  - `sabatons_of_the_unheard_oath`
- Starter values: 10 Trace per individual piece, 100 Trace for complete set detection.
- Added `/currency value` to report the held item sell value/debug source.
- Added `/currency value inventory` to report detected complete sets and notable inventory values.
- Scope boundary remains unchanged: no vendor NPCs, no vendor GUI, no item removal in pricing evaluation.
