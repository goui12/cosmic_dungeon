# Update 1.4.9 Notes

## Fixes

- Fixed Cosmic Mob Spawner preset defaults so armor and hand equipment drop chances now start at `0.0` instead of `0.085`.
- This affects newly created/initialized spawner presets (for example when first using `/spawner equip ...` on a spawner without an existing preset).

## Operator impact

- Spawner-authored mobs will no longer unexpectedly drop equipped armor by default.
- If you want equipment to drop, explicitly set chance via `/spawner drop <slot> <0.0-1.0>`, `/spawner drop armor <0.0-1.0>`, `/spawner drop hands <0.0-1.0>`, or `/spawner drop all <0.0-1.0>`.

## Features

- Cosmic Mob Spawner now supports direct equipment authoring by right-click: right-click the spawner with any equippable item (armor, mainhand weapon/tool, or offhand-compatible item) to copy that exact custom item stack into the spawner preset equipment slot.
- This preserves custom names, enchantments, and other item components/NBT without requiring separate `/spawner enchant ...` steps.
