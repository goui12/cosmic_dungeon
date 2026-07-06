# Spawner Commands Features

This document covers the full `/spawner` command surface for Cosmic Dungeon operators. See also the [Cosmic Spawner runtime model](Spawners/Spawner_Systems.md) and the [1.5.1 release notes](releases/Update_1.5.1.md#cosmic-spawner-intrinsic-drop-refactor).

## Core behavior
- Right-clicking a Cosmic Mob Spawner with an equippable item applies that exact `ItemStack` (name, lore, enchantments, components/NBT) to the matching mob equipment slot in the spawner preset.
- Commands target the **Cosmic Mob Spawner block you are currently looking at** (within 5 blocks).
- Existing spawners placed before this feature update are supported, including base spawners that only have `SpawnerEntityId` and no preset.
- Default preset equipment drop chance is `0.0` for all armor and hand slots unless explicitly changed with `/spawner drop ...`.
- Intrinsic drops are mob loot-table drops, separate from equipment-slot drops.

## Full command syntax
- `/spawner help`
- `/spawner set <namespace:entity>`
- `/spawner name set <display name>`
- `/spawner name clear`
- `/spawner boss [true|false]`
- `/spawner cap <amount>`
- `/spawner equip <slot> <namespace:item>`
- `/spawner equip <slot> fromhand`
- `/spawner equip clear <slot>`
- `/spawner equip clear all`
- `/spawner enchant <slot> <namespace:enchantment> <level> [allowUnsafe]`
- `/spawner enchant remove <slot> <namespace:enchantment>`
- `/spawner enchant clear <slot>`
- `/spawner drop <slot> <0.0-1.0>`
- `/spawner drop armor <0.0-1.0>`
- `/spawner drop hands <0.0-1.0>`
- `/spawner drop all <0.0-1.0>`
- `/spawner drop intrinsic <namespace:item> <0.0-1.0>`
- `/spawner drop intrinsic default <namespace:item>` preferred reset/default command
- `/spawner drop intrinsic clear <namespace:item>` legacy alias for the same reset behavior
- `/spawner drops`
- `/spawner delay <ticks>`
- `/spawner showlabels`
- `/spawner info`
- `/spawner preset save <preset_name>`
- `/spawner preset load <preset_name>`
- `/spawner preset delete <preset_name>`
- `/spawner preset reload`
- `/spawner keybind <1-5> <preset_name>`
- `/spawner reset`

## Auto-complete and syntax tips
- Brigadier tab completion is available for entity IDs, item IDs, enchantment IDs, equipment slots, root subcommands, and preset names where supported.
- Running a partial parent command such as `/spawner drop` or `/spawner equip` prints command-specific syntax hints.
- Unknown subcommand text (example: `/spawner dropp`) shows an error plus the full syntax guide in chat.

## `/spawner info` display
- The top of the panel shows both the entity type (`Mob`) and the custom mob name (`Mob Name`) set by `/spawner name set <display name>`, or `unnamed` when no name is configured.
- The panel is server command output and includes color-coded sections for mob, equipment, coordinates, runtime properties, equipment drops, and intrinsic drops.
- Existing base spawners with only `SpawnerEntityId` still show the mob, coordinates, properties, and active intrinsic loot-table drops even when no preset exists.
- Equipment rows display item identifier, custom-name state, and enchantment list from real item NBT/components.
- Equipment drop entries include clickable `[+]`/`[-]` controls for each equipment slot. Clicking adjusts the value and immediately re-runs the info panel.

## Intrinsic drops in `/spawner info`
`/spawner info` and `/spawner drops` now show:

- **Default active loot-table drops** — items discovered from the configured mob's active server loot table/resource data, not from a hardcoded vanilla list.
- **Overrides** — per-spawner final chances for items that also appear in the default loot table.
- **Custom-added intrinsic drops** — per-spawner final chances for items that do not appear in that mob's default loot table, such as adding `minecraft:pink_wool` to a zombie spawner.

Example rows may look like:

- `minecraft:rotten_flesh: default ~66%  [+] [-] [Default]`
- `minecraft:iron_ingot: default rare/player/looting  [+] [-] [Default]`
- `minecraft:potato: default rare/player/looting, override 100%  [+] [-] [Default]`
- `minecraft:pink_wool: custom 25%  [+] [-] [Default]`

Minecraft loot tables can be conditional and include looting, killed-by-player checks, weights, functions, entity state checks, and datapack logic. Simple chances are displayed as approximate percentages. Complex entries are intentionally labeled `complex`, `conditional`, `rare/player/looting`, or similar rather than showing fake exact math.

## Intrinsic drop commands and chat controls
- `/spawner drop intrinsic <namespace:item> <0.0-1.0>` sets the **final spawner-specific intrinsic chance** for that item. If the item is in the active default loot table, it becomes an override. If it is not in the default table, it becomes a custom-added intrinsic drop.
- `/spawner drop intrinsic default <namespace:item>` removes the override/addition and returns that item to default loot-table behavior. This is the preferred reset command.
- `/spawner drop intrinsic clear <namespace:item>` remains supported as a legacy alias for default/reset.
- `[+]` increases the final spawner-specific chance by 5% and refreshes `/spawner info`.
- `[-]` decreases the final spawner-specific chance by 5% and refreshes `/spawner info`.
- `[Default]` removes the spawner-specific override/addition. On a default row it returns to vanilla/datapack behavior; on a custom-added row it removes that custom drop completely.
- When adjusting a default row with no override, `[+]`/`[-]` starts from the displayed numeric default when safely available. Complex/unknown defaults use safe fallback baselines (`[+]` starts at 5%, `[-]` clamps to 0%).

Examples:

```mcfunction
/spawner info
/spawner drop intrinsic minecraft:potato 1.0
/spawner drop intrinsic default minecraft:potato
/spawner drop intrinsic minecraft:pink_wool 0.25
/spawner drop intrinsic clear minecraft:pink_wool
```

## `/spawner drops`
- Kept as a compatibility alias that opens the same `/spawner info` panel.

## Preset file workflow
- Presets are stored as editable JSON files in `<server_root>/cosmicdungeon/spawner_presets/`.
- `preset_name` becomes the file name, e.g. `/spawner preset save Skeleton_Master` writes `Skeleton_Master.json`.
- Names cannot contain spaces; only `A-Z`, `a-z`, `0-9`, `_`, and `-` are accepted.
- `/spawner preset load <preset_name>` applies entity, boss one-shot, cap, delay, and nested `spawnerPresetNbt` data to the targeted spawner.
- Intrinsic drop rules are saved inside the nested spawner preset NBT. Legacy `intrinsicDrops` data is still read and mirrored during the 1.5.1 transition.
- `/spawner preset delete <preset_name>` removes that JSON file.
- `/spawner preset reload` ensures the directory exists and seeds examples if empty.
- If the directory is empty, three examples are auto-generated: `Skeleton_Master.json`, `Warden_Trial.json`, and `Pillager_Captain_Elite.json`.
- Dev reminder: when future cosmic spawner fields are added/removed, update preset read/write parsing to keep files in sync.

## `/spawner showlabels`

- `/spawner showlabels` switches the developer-only **Cosmic Spawner summary HUD** instead of rendering a teal nameplate above spawners.
- When enabled, online developers who look directly at a Cosmic Mob Spawner see a bottom-right screen panel with mob type, configured mob name, per-spawner cap, and delay range.
- Looking away from the spawner, opening another screen, or disabling the toggle hides the panel.
- Developers can tune the HUD locally in the client config with `spawnerHud.position`, `spawnerHud.opacity`, `spawnerHud.horizontalOffset`, and `spawnerHud.verticalOffset` for different screen layouts.
- Non-developers never receive this HUD mode, and the command remains server-authorized through the existing developer access checks.
- This HUD uses existing server/client sync for display only; intrinsic info and drop behavior remain server-authoritative.

## Spawner preset hotkeys
- Five client keybinds are available under **Cosmic Dungeon** in Controls.
- Defaults: `NUMPAD 1`..`NUMPAD 5`.
- Assign each slot with `/spawner keybind <1-5> <preset_name>`.
- Pressing the matching key loads the assigned preset onto the Cosmic Spawner you are currently looking at (within 5 blocks).
- `/spawner preset load <preset_name>` is unchanged and still supported.

## Phase 6 Cosmic Spawner Intrinsic Drop Counts

Cosmic Mob Spawner intrinsic drops now use lossless rule rows with a stable rule id, item id, final chance, stack count, and rule kind. Use `/spawner drop intrinsic <namespace:item> <chance 0.0-1.0> [quantity]`; omitted quantity defaults to `1`, and counts are clamped to the safe 1-64 range.

Multiple independent rules for the same item are supported. For example, potato rules at `1.0 1`, `0.5 2`, and `0.1 5` roll separately, so one kill can drop 1, 3, 6, or 8 potatoes depending on which rows succeed. Chance always applies to that configured stack, not to the combined item total.

`/spawner info` shows configured rows as `Chance: <percent> [+] [-] Count: <n> [+] [-] [Default]`. The chance and count buttons target the stable rule id for that row, so duplicate item rows are safe. Clicking `[Default]` on a configured row removes only that rule; `/spawner drop intrinsic default <item>` and the backward-compatible `/spawner drop intrinsic clear <item>` remove all configured rules for that item and restore vanilla/datapack default behavior. `/spawner drop intrinsic add <item> <chance> [quantity]` always adds a new independent rule, while `/spawner drop intrinsic <item> <chance> [quantity]` upserts by item/count for convenience.

Save migration is lazy and backward-compatible: 1.5.0 data, block-entity data versions 150/151, preset versions 2/3, preset file formats 1/2, old `intrinsicDrops` maps, and Phase-5 `intrinsicDropRules` keyed by item id are read as one count-1 rule per old item chance. New block entities save as data version 152, presets save as preset version 4, and preset JSON saves as format version 3 while preserving the full `spawnerPresetNbt` rule list. The legacy `intrinsicDrops` mirror remains compatibility-only and cannot represent duplicate rows.

## Phase 7 Cosmic Spawner Spawn Defaults

Cosmic Mob Spawners now apply a small server-side spawn-default pass only to mobs carrying that spawner's `cosmic_spawner_<x>_<y>_<z>` marker. Vanilla Wardens spawned by Cosmic Spawners receive a 1200-tick `minecraft:dig_cooldown` brain memory so they do not immediately dig after spawning.

Vanilla `minecraft:slime` and `minecraft:magma_cube` spawned by Cosmic Spawners are raised to the max standard size 4 using the entity API so health and dimensions refresh correctly. Larger entities are not shrunk, and naturally spawned mobs are unaffected.

This phase does not change Cosmic Spawner block-entity storage, preset JSON/NBT storage, intrinsic drop rule storage, rift/RD data, door/key data, access policy, class, teleportation, or dungeon reset data. The only new marker is per spawned entity: `cosmicdungeon:spawner_spawn_defaults_applied_version`, used to avoid reapplying the defaults forever.
