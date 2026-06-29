# In-Game Commands (Developer Reference) — 1.5

## Command roots registered by the mod

- `/world`
- `/creative`
- `/survival`
- `/metalmancer`
- `/more`
- `/door` (lock/info/count/pass_limit/reset_count/key)
- `/heal`
- `/fly`
- `/flyspeed`
- `/fullbright`
- `/day`
- `/night`
- `/riftdestination`
- `/shake`
- `/spawner`
- includes `/spawner keybind <1-5> <preset_name>` for preset hotkey assignment
- `/rift`
- `/region`
- `/rank`
- `/developer`
- `/dungeoneer`
- `/dungeoneer kick <player>`
- `/classselectordestination`
- `/currency`
- `/faction`
- `/progression`
- `/achievement`
- `/plantflags`
- `/vendor`
- `/trade`
- `/classitem`

## High-use command groups

### Spawner
`/spawner help|set|name|boss|cap|equip|enchant|drop|drops|showlabels|delay|preset|info|reset`

### Region (actual command tree)

- `/region wand`
- `/region new <name>`
- `/region create <name>`
- `/region new <name> copy <source>`
- `/region create <name> copy <source>`
- `/region look <name>`
- `/region look all` (nearby-region outline toggle; outlines include an x-ray overlay so hidden edges remain visible through walls while still respecting normal nearby/render-distance behavior)
- `/region info <name>`
- `/region here` (shows `/region info`-style output for all regions at your current position)
- `/region parent <region> <newParent>` (`none`/`null` clears parent)
- `/region delete <name>`
- `/region list`
- `/region flags <name>`
- `/region flags <name> <flag> <allow|deny|clear>`
- `/region flags <name> inherit <flags|exceptions> <on|off>`
- `/region flags <name> exceptions <place|break>`
- `/region flags <name> exceptions <place|break> <torch|ladder|water> <allow|deny|clear>` (`water` only for `place`)
- `/region flag list`
- `/region flag <flag> <allow|deny>` (applies to effective region at your location)

**Default behavior note:** newly created regions now default `interact=allow`.

### Door/Key
`/door lock|info|count|pass_limit|reset_count|key info|key duplicate`

### Rift
`/rift list|delete ...` and `/riftdestination ...` for destination mapping workflows.

### Class selector
`/classselectordestination ...` for per-slot, fallback, and max-player routing controls.

### Rank/authority
`/rank ...` for rank/password workflow across developer and dungeoneer authority boundaries.

## Notes for operators

- Many workflows require a player source (console-only invocations can fail for player-targeted UIs).
- Several commands are look-target dependent within short range.
- Failed syntax for `/spawner` falls back to help output with unknown syntax warning.


### Trade (1.5 player-to-player foundation)
`/trade <player>`
`/trade accept <player>`
`/trade deny <player>`
`/trade cancel`

Authority: normal players can send, accept, deny, and cancel their own trades. No developer/admin rank is required.

Notes:
- `/trade <player>` sends a pending invite to an online target and remains the typed fallback without a distance requirement.
- The **Trade Request** Controls keybind (default `CAPS LOCK`, category **Cosmic Dungeon**) sends the same invite path when the client is looking at another player within 3 blocks; the server revalidates online target, same dimension, close range, approximate look direction, active trade state, and cooldown before inviting.
- Incoming invites include clickable `[Accept Trade]` and `[Deny Trade]` chat buttons that run the command fallbacks.
- Pending invites expire after 30 seconds and requesters have a 3-second cooldown between successful invite sends.
- Players cannot start or accept another trade while already in an active trade.
- If the hotkey has no valid look target, the client shows: `Look at a player within 3 blocks to request a trade.`
- Trade cancellation, menu close, and logout/disconnect return offered items safely. Other-player offer slots are read-only.
- The trade menu uses the custom `trade_window.png` container layout: 9 read-only partner offer slots, 9 lockable own offer slots, 27 player inventory slots, and 9 hotbar slots. Shift-clicking never takes partner offer items and only moves real item stacks between the player's inventory/hotbar and their own offer slots before that player accepts.
- Active trades keep the invite chat flow separate from the GUI flow: `[Accept Trade]` / `[Deny Trade]` only answer pending requests, while the GUI accept button first marks the current offer accepted/ready and then, after both players are ready, confirms/finalizes the same offer. The GUI deny button cancels the active trade, returns offered items, discards currency offers, closes both screens, and notifies both players.
- The trade screen receives server-authoritative, view-specific state sync for `Trading with: <name>`, each player's displayed name, Trace balances, Trace offers, ready/confirm status, and status messages; item offers still sync through the menu slots. Own item/currency offer changes before acceptance reset both players' ready/confirm state, and accepted own offers are locked against further item/currency edits until cancel.

### Class item attunement (1.5 foundation)
`/classitem attune <class_name> <dungeon_number> <tier_number> <trace_value>`
`/classitem clear`

Authority: developer-only through `AccessPolicy.requireDeveloperOrConsole`, but the action must be run by an in-game developer because it edits the main-hand item. Console receives a clean failure message.

Notes:
- Attunes the held vanilla or customized item as CosmicDungeon class gear without changing the item's display name, enchantments, trims, lore, durability, count, or unrelated data.
- Class names use existing playable `ClassKeys` ids case-insensitively, excluding `none`; stored metadata is canonical lowercase such as `judicator`.
- Dungeon accepts `d1`, `D1`, or `1` and stores integer `1`; tier must be `1` through `10`; Trace must be zero or greater.
- Attunement is stored in persistent data components: `cosmicdungeon:class_attunement`, `cosmicdungeon:class_item_dungeon`, `cosmicdungeon:class_item_tier`, and `cosmicdungeon:class_item_trace_value`.
- Hover tooltips append the attuned class display name at the bottom in that class color, bold and italic. Normal items show no extra line.
- Class-attuned equipment is server-restricted to the matching selected class: guarded tools, weapons, shields, bows/crossbows/tridents, maces, and armor/equippable combat gear deny use or wear with `Only a <Class> can use/wear that!` when the player has another class or no class.
- Intrinsic class-bound Metalmancer utility items, including the Satchel of Samples and Metalmancer-only items/staffs, use the same central class item access policy; explicit stack attunement still takes priority if present.
- Class-attuned banners are intentionally not guarded equipment, so Plant Flags banners remain placeable and can still be inspected by the Plant Flags system. Dungeon, tier, and Trace metadata do not affect equipment use restrictions.
- Vendor sell value uses valid class-item attunement metadata as the source of truth: any sold attuned item, including armor, sells for its stored Trace value.
- Vendor sales value each sellable stack independently; matching armor pieces do not receive special vendor sale treatment.

### Currency (Attunement Fragment economy foundation)
`/currency balance [player]`
`/currency add <player> <trace|mark|seal|crown|anchor> <amount>`
`/currency remove <player> <trace|mark|seal|crown|anchor> <amount>`
`/currency set <player> <trace|mark|seal|crown|anchor> <amount>`
`/currency clear <player>`
`/currency capacity <player> <traceAmount>`
`/currency value`
`/currency value inventory`

Authority: self-balance and value-check commands are open to players; other-player/admin mutations require developer or console authority through AccessPolicy.

Pickup behavior: Attunement currency item entities auto-store into balance on pickup, with all-or-nothing capacity checks (no partial deposit).

### Faction (1.5 foundation)
`/faction get <player> <faction>`
`/faction set <player> <faction> <value>`
`/faction add <player> <faction> <delta>`
`/faction list <player>`

Authority: players can read their own faction values. Reading or mutating another player's faction values requires developer or console authority through AccessPolicy.



### Progression (1.5 long-term progression foundation)
`/progression get <player>`
`/progression d1 complete <player> <lesserBlooms>`
`/progression lesser add <player> <amount>`
`/progression lesser set <player> <amount>`
`/progression cavern add <player> <amount>`
`/progression cavern set <player> <amount>`
`/progression village <player> <true|false>`

Authority: players can read their own progression. Reading another player's progression and all mutations require developer or console authority through AccessPolicy.


### Achievement (1.5 advancement foundation)
`/achievement grant <player> <achievementId>`
`/achievement counters <player>`
`/achievement counters reset <player>`
`/achievement idol return <player>`
`/achievement idol provide <provider> <receiver>`
`/achievement vitalexchange <provider> <receiver> <item>`

Authority: developer or console only through `AccessPolicy.requireDeveloperOrConsole`.

Notes:
- Uses `CosmicAdvancementUtil` with default criterion `triggered`.
- Intended as debug/admin tooling while gameplay triggers are implemented incrementally.
- Binding Idol debug hooks: use `idol return` and `idol provide` to increment idol counters and validate threshold advancement grants.


### Plant Flags (1.5 D1 foundation)
`/plantflags status`
`/plantflags reset`
`/plantflags setregion pos1`
`/plantflags setregion pos2`
`/plantflags complete-debug`

Authority: `status` is public. `reset`, `setregion`, and `complete-debug` require developer or console authority via `AccessPolicy.requireDeveloperOrConsole`.

Notes:
- Tracks per-run/session planting state. Offline run members do not block completion; online members in the same run must plant matching class-attuned banners.
- Completion grants `cosmicdungeon:achievements/plant_flags` to online eligible players and broadcasts placeholder JHW summon text.


### Vendor (1.5 progression + faction access)
`/vendor list`
`/vendor reload`
`/vendor profile <profileId>`
`/vendor assign <profileId>`
`/vendor clear`
`/vendor info`
`/vendor spawn <profileId>`
`/vendor access <profileId>`

Authority: developer or console only via `AccessPolicy.requireDeveloperOrConsole`.

Profile ID usability:
- Commands that accept `<profileId>` resolve exact full IDs first, such as `cosmicdungeon:d1/brewing_store`.
- They also accept the short last-path alias, such as `brewing_store`, `general_supply_vendor`, `weapon_supplier`, or `save_teleport_npc`.
- If multiple loaded profiles ever share the same short alias, commands fail with an ambiguity message instead of guessing.
- `/vendor list` shows the short alias first and the full datapack `ResourceLocation` as secondary detail.

Scope boundary:
- loads datapack vendor profiles from `data/cosmicdungeon/vendor_profiles/*.json`
- supports villager vendor assignment + vendor menu
- `/vendor access` explains whether the executing player can access a vendor profile and why

## Changelog

- **1.5:** Added command coverage for currency, class item attunement, faction, progression, achievements, Plant Flags, vendors, and trading.
