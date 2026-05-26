# Update 1.5.0 Notes (Initial Implementation)

## Attunement Fragment economy account foundation

This is the first 1.5.0 implementation pass and introduces backend account plumbing only.
No vendors, purchase GUI, pickup hooks, or gameplay sinks/sources are included in this update.

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

Permission model:

- Any player can run self-balance.
- Other-player balance checks and all admin mutations require developer/console authority.

### Scope boundary

This update intentionally does **not** include:

- vendors
- economy GUI/menu screens
- currency item pickup handlers
- world drops or sinks

These are planned for future 1.5.x follow-up work.
