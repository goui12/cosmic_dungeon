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
- `/vendor`
- `/trade`
- `/classitem`

## High-use command groups

### Spawner
`/spawner help|set|name|boss|cap|equip|enchant|drop|drops|showlabels [true|false]|delay|preset|keybind|info|reset`

See [Spawner Commands Features](../Spawner_Commands_Features.md) and [Spawner Systems](../Spawners/Spawner_Systems.md) for the full operator workflow.

Common examples:

```mcfunction
/spawner info
/spawner drop intrinsic minecraft:potato 1.0
/spawner drop intrinsic default minecraft:potato
/spawner drop intrinsic minecraft:pink_wool 1.0
/spawner drop intrinsic default minecraft:pink_wool
```

Intrinsic drop commands set final per-spawner mob loot-table chances. Default loot-table items become overrides; items that are not in the active mob loot table become custom-added intrinsic drops. `/spawner drop intrinsic clear <item>` remains supported as a legacy alias for `/spawner drop intrinsic default <item>`.

`/spawner showlabels [true|false]` controls the executing developer's personal, server-authorized Cosmic Spawner summary HUD. Without an argument it toggles only that player; `true` and `false` explicitly set that same personal preference. Other developers are not changed, and non-developers cannot enable the HUD through command attempts or client config. When enabled, that developer can locally toggle HUD rows for Mob Type, Mob Name, Coordinates, Boss One-Shot, Boss Spawned, Cap, Delay, Spawn Count, Spawn Range, Required Player Range, Max Nearby Entities, Preset Present, and Equipment while looking at a Cosmic Mob Spawner; loot tables are intentionally not shown in the HUD. This HUD is display-only and does not migrate or rewrite placed spawner or preset file formats.

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
- `/region quest <quest-name> status`
- `/region quest <quest-name> reset`
- `/region quest <quest-name> setregion pos1`
- `/region quest <quest-name> setregion pos2`
- `/region quest <quest-name> complete-debug`

**Default behavior note:** newly created regions now default `interact=allow`; dungeoneers can use buttons/levers and break cobwebs, Cosmic Mob Spawners, Lesser Blooms, and torches in protected regions. Ladder placement remains denied by default until an operator enables `place` exception `ladder` for a specific region.

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


### Region Quest Reactions (1.5.1 location quest foundation)
`/region quest plant_flags status`
`/region quest plant_flags reset`
`/region quest plant_flags setregion pos1`
`/region quest plant_flags setregion pos2`
`/region quest plant_flags complete-debug`

Authority: the `/region` root remains developer-or-console scoped. Player invocations require developer rank; console remains allowed for non-player subcommands, while `setregion` requires an in-world player position.

Notes:
- Quest names are single command words. Use `plant_flags` exactly, not `plant flags`, so quest IDs cannot be mistaken for later arguments.
- Tracks per-run/session planting state. Offline run members do not block completion; online members in the same run must plant matching class-attuned banners inside the configured quest cuboid.
- Completion grants `cosmicdungeon:achievements/plant_flags` to online eligible players and broadcasts placeholder JHW summon text.
- The old `/plantflags` command root is removed so Plant Flags uses the same `/region quest <quest-name> ...` shape as future location-based quest reactions. New region-backed quests should add a `RegionQuestHandler` and register it through the small region quest registry instead of adding Plant Flags-specific branches to the command parser.


### Vendor (1.5 progression + faction access)
`/vendor list`
`/vendor reload`
`/vendor profile <profileId>`
`/vendor assign <profileId>`
`/vendor clear`
`/vendor info`
`/vendor spawn <profileId>`
`/vendor spawn <profileId> <mobType>`
`/vendor access <profileId>`

Authority: developer or console only via `AccessPolicy.requireDeveloperOrConsole`.

Profile ID usability:
- Commands that accept `<profileId>` resolve exact full IDs first, such as `cosmicdungeon:d1/brewing_store`.
- They also accept the short last-path alias, such as `brewing_store`, `general_supply_vendor`, `weapon_supplier`, or `save_teleport_npc`.
- If multiple loaded profiles ever share the same short alias, commands fail with an ambiguity message instead of guessing.
- `/vendor list` shows the short alias first and the full datapack `ResourceLocation` as secondary detail.

Scope boundary:
- loads datapack vendor profiles from `data/cosmicdungeon/vendor_profiles/*.json`
- supports default villager vendor spawning plus optional assigned mob vendor shells, for example `/vendor spawn <profileId> horse`
- `/vendor access` explains whether the executing player can access a vendor profile and why

## Changelog

- **1.5.1:** Added default protected-region dungeoneer allowances for buttons/levers, cobwebs, Cosmic Mob Spawners, Lesser Blooms, and torches while keeping ladder placement denied by default for explicit per-region enablement.
- **1.5.1:** Moved Plant Flags operator tools from `/plantflags` to `/region quest plant_flags ...` as the first standardized location-based quest reaction command.
- **1.5:** Added command coverage for currency, class item attunement, faction, progression, achievements, Plant Flags, vendors, and trading.

### Dungeon AFK prompt commands

AFK kick prompt clicks use internal `/dungeoneer afk-kick yes <uuid>` and `/dungeoneer afk-kick no <uuid>` command fallbacks. Players should normally use the clickable `[YES]` / `[NO]` text documented in [Dungeon AFK Handling](../DungeonLifecycle/Dungeon_AFK_Handling.md); manual `/dungeoneer kick <player>` remains the normal explicit Group Leader kick command.

## Phase 6 Cosmic Spawner Intrinsic Drop Counts

Cosmic Mob Spawner intrinsic drops now use lossless rule rows with a stable rule id, item id, final chance, stack count, and rule kind. Use `/spawner drop intrinsic <namespace:item> <chance 0.0-1.0> [quantity]`; omitted quantity defaults to `1`, and counts are clamped to the safe 1-64 range.

Multiple independent rules for the same item are supported. For example, potato rules at `1.0 1`, `0.5 2`, and `0.1 5` roll separately, so one kill can drop 1, 3, 6, or 8 potatoes depending on which rows succeed. Chance always applies to that configured stack, not to the combined item total.

`/spawner info` shows configured rows as `Chance: <percent> [+] [-] Count: <n> [+] [-] [Default]`. The chance and count buttons target the stable rule id for that row, so duplicate item rows are safe. Clicking `[Default]` on a configured row removes only that rule; `/spawner drop intrinsic default <item>` and the backward-compatible `/spawner drop intrinsic clear <item>` remove all configured rules for that item and restore vanilla/datapack default behavior. `/spawner drop intrinsic add <item> <chance> [quantity]` always adds a new independent rule, while `/spawner drop intrinsic <item> <chance> [quantity]` upserts by item/count for convenience.

Save migration is lazy and backward-compatible: 1.5.0 data, block-entity data versions 150/151, preset versions 2/3, preset file formats 1/2, old `intrinsicDrops` maps, and Phase-5 `intrinsicDropRules` keyed by item id are read as one count-1 rule per old item chance. New block entities save as data version 152, presets save as preset version 4, and preset JSON saves as format version 3 while preserving the full `spawnerPresetNbt` rule list. The legacy `intrinsicDrops` mirror remains compatibility-only and cannot represent duplicate rows.
