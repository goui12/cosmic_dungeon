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
- `/spawner flag boss [true|false]`
- `/spawner cap <amount>`
- `/spawner flag <persistent|name_visible|silent|glowing|no_ai|no_gravity> <true|false>`
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
- `/spawner info`
- `/spawner reset`

## Auto-complete and syntax tips
- Brigadier tab completion is available for:
  - entity IDs, item IDs, enchantment IDs
  - equipment slots
  - root subcommands
- Running a partial parent command such as `/spawner drop` or `/spawner equip` prints command-specific syntax hints.
- Unknown subcommand text (example: `/spawner dropp`) now shows an error plus the full syntax guide in chat.

## `/spawner drops` display
- Shows **equipment drops configured by spawner commands**.
- Shows **intrinsic mob drops** (currently includes skeleton-focused intrinsic rows).
- Equipment entries include clickable `[+]`/`[-]` controls for chance tuning.
- Intrinsic rows are displayed as informational rows with non-mutating controls.
