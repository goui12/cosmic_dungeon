# Update 1.4.9 Notes

## Fixes

- Fixed Cosmic Mob Spawner preset defaults so armor and hand equipment drop chances now start at `0.0` instead of `0.085`.
- This affects newly created/initialized spawner presets (for example when first using `/spawner equip ...` on a spawner without an existing preset).

## Operator impact

- Spawner-authored mobs will no longer unexpectedly drop equipped armor by default.
- If you want equipment to drop, explicitly set chance via `/spawner drop <slot> <0.0-1.0>`, `/spawner drop armor <0.0-1.0>`, `/spawner drop hands <0.0-1.0>`, or `/spawner drop all <0.0-1.0>`.

## Features

- Removed unsupported mob-behavior toggle flags from `/spawner`: `persistent`, `silent`, `glowing`, `no_ai`, `no_gravity`, and `name_visible` are no longer accepted.
- Boss toggle moved to `/spawner boss [true|false]` (replacing `/spawner flag boss ...`).
- Preset structured data load/save now strips those removed behavior flags so stale values are cleaned on rewrite.
- Added `/spawner showlabels` as a developer-only global toggle for the cyan/teal spawner label text above Cosmic Spawners.

- Simplified Cosmic Mob Spawner command aliases: `/spawner flag cap <amount>` is now `/spawner cap <amount>`, and `/spawner set entity <namespace:entity>` is now `/spawner set <namespace:entity>`.
- Cosmic Mob Spawner now supports direct equipment authoring by right-click: right-click the spawner with any equippable item (armor, mainhand weapon/tool, or offhand-compatible item) to copy that exact custom item stack into the spawner preset equipment slot.
- This preserves custom names, enchantments, and other item components/NBT without requiring separate `/spawner enchant ...` steps.

- `/spawner info` has been expanded into the primary readable inspector: colored sections, mob identity, coordinates, runtime properties, full equipment listing, and enchantment/NBT-derived item naming details.
- Drop tuning is now in-chat via clickable `[+]` / `[-]` controls directly inside `/spawner info`, and each click immediately refreshes the info panel for rapid incremental editing.
- Intrinsic drop overrides now show exact values stored in spawner NBT (no placeholder/default text), with clickable `[+]` / `[-]` controls in the same info panel.

- Added file-based preset commands for Cosmic Spawner authoring:
  - `/spawner preset save <preset_name>`
  - `/spawner preset load <preset_name>`
  - `/spawner preset delete <preset_name>`
  - `/spawner preset reload`
- Preset files are JSON under `<server_root>/cosmicdungeon/spawner_presets/` and can be edited offline for developer workflows.
- Added auto-generated sample presets to help reverse-engineer the schema (`Skeleton_Master`, `Warden_Trial`, `Pillager_Captain_Elite`).

## Region updates

- New region creation now seeds `/region` flag `interact` to **allow** by default (`interact=true`), so fresh regions are immediately interaction-permissive unless you override them.
- Added `/region here` to inspect the region(s) at your current position using the same output format as `/region info`.
- Region documentation has been rewritten to match the implemented command tree and behavior model.
- Region look outlines now render with an additional depth-disabled overlay pass, so region edges remain visible through walls without changing the existing nearby/render-distance behavior of `/region look all`.
- Corrected docs now describe the real `/region` subcommands (`wand`, `create/new`, `look`, `info`, `here`, `parent`, `delete`, `list`, `flags`, `flag`) and remove outdated priority/mode-era command references.

- Added five configurable Cosmic Dungeon keybinds for Cosmic Spawner preset loading (Controls category: **Cosmic Dungeon**).
- Default keybinds are `NUMPAD 1` through `NUMPAD 5`.
- Assign presets per slot with `/spawner keybind <1-5> <preset_name>`.
- Pressing a configured key now loads that assigned preset onto the `cosmic_mob_spawner` the player is looking at within 5 blocks.
- Existing `/spawner preset load <preset_name>` remains available.
