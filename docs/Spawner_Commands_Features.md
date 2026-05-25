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
- Developer-only global toggle.
- `/spawner showlabels` switches whether the cyan/teal spawner entity label above Cosmic Spawners is visible.
- When enabled, labels are shown to all online developers and to developers who join later.
- Non-developers never receive this label view mode.
