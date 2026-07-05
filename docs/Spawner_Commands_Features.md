# Spawner Commands Features

This document covers the full `/spawner` command surface for Cosmic Dungeon operators.

## Core behavior
- Right-clicking a Cosmic Mob Spawner with an equippable item now applies that exact `ItemStack` (name, lore, enchantments, components/NBT) to the matching mob equipment slot in the spawner preset.
- Commands target the **Cosmic Mob Spawner block you are currently looking at** (within 5 blocks).
- Existing spawners placed before this feature update are supported.
- Default preset equipment drop chance is `0.0` for all armor and hand slots (unless explicitly changed with `/spawner drop ...`).

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
- `/spawner drops`
- `/spawner delay <ticks>`
- `/spawner showlabels`
- `/spawner info`
- `/spawner preset save <preset_name>`
- `/spawner preset load <preset_name>`
- `/spawner preset delete <preset_name>`
- `/spawner preset reload`
- `/spawner reset`

## Auto-complete and syntax tips
- Brigadier tab completion is available for:
  - entity IDs, item IDs, enchantment IDs
  - equipment slots
  - root subcommands
- Running a partial parent command such as `/spawner drop` or `/spawner equip` prints command-specific syntax hints.
- Unknown subcommand text (example: `/spawner dropp`) now shows an error plus the full syntax guide in chat.

## `/spawner info` display
- The top of the panel now shows both the entity type (`Mob`) and the custom mob name (`Mob Name`) set by `/spawner name set <display name>`, or `unnamed` when no name is configured.
- Primary operator panel with color-coded sections for mob, equipment, coordinates, runtime properties, and drops.
- Equipment rows now display item identifier, custom-name state, and enchantment list (from real item NBT/components).
- Drop entries include clickable `[+]`/`[-]` controls for each equipment slot. Clicking adjusts the value and immediately re-runs the info panel.
- Intrinsic drops are shown from values present in spawner preset NBT overrides (no fake defaults), with clickable `[+]`/`[-]` controls that also refresh the panel.

## `/spawner drops`
- Kept as a compatibility alias that opens the same `/spawner info` panel.


## Preset file workflow
- Presets are stored as editable JSON files in `<server_root>/cosmicdungeon/spawner_presets/`.
- `preset_name` becomes the file name, e.g. `/spawner preset save Skeleton_Master` writes `Skeleton_Master.json`.
- Names cannot contain spaces; only `A-Z`, `a-z`, `0-9`, `_`, and `-` are accepted.
- `/spawner preset load <preset_name>` applies entity, boss one-shot, cap, delay, and nested `spawnerPresetNbt` data to the targeted spawner.
- `/spawner preset delete <preset_name>` removes that JSON file.
- `/spawner preset reload` ensures the directory exists and seeds examples if empty.
- If the directory is empty, three examples are auto-generated: `Skeleton_Master.json`, `Warden_Trial.json`, and `Pillager_Captain_Elite.json`.
- Dev reminder: when future cosmic spawner fields are added/removed, update preset read/write parsing to keep files in sync.


## `/spawner showlabels`

- `/spawner showlabels` now switches the developer-only **Cosmic Spawner summary HUD** instead of rendering a teal nameplate above spawners.
- When enabled, online developers who look directly at a Cosmic Mob Spawner see a bottom-right screen panel with mob type, configured mob name, per-spawner cap, and delay range.
- Looking away from the spawner, opening another screen, or disabling the toggle hides the panel.
- Developers can tune the HUD locally in the client config with `spawnerHud.position`, `spawnerHud.opacity`, `spawnerHud.horizontalOffset`, and `spawnerHud.verticalOffset` for different screen layouts.
- Non-developers never receive this HUD mode, and the command remains server-authorized through the existing developer access checks.
- This is a client HUD/render refactor only. It does not add required saved fields or change Cosmic Mob Spawner block-entity storage, preset JSON formats, entity save data, rift/RD data, door/key data, class selector data, teleportation data, or access-policy records. Updating from 1.5.0 to 1.5.1 requires no data migration for this display change.

## Spawner preset hotkeys
- Five client keybinds are available under **Cosmic Dungeon** in Controls.
- Defaults: `NUMPAD 1`..`NUMPAD 5`.
- Assign each slot with `/spawner keybind <1-5> <preset_name>`.
- Pressing the matching key loads the assigned preset onto the Cosmic Spawner you are currently looking at (within 5 blocks).
- `/spawner preset load <preset_name>` is unchanged and still supported.
