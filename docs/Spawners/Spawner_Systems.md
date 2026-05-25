# Spawner Systems (Developer) — 1.4.8

## Cosmic spawner runtime model

Spawner behavior is driven by `cosmic_mob_spawner` block entities and optional presets.

- Base entity type can be changed by command.
- Presets can define equipment by slot.
- Per-slot drop chances are configurable and default to `0.0` for all slots on new presets.
- Delay includes tick target and generated min/max range.
- Optional boss one-shot flag and per-spawner mob cap are available.

## Authoring workflow

1. Place `cosmic_mob_spawner`.
2. Look at spawner within command range.
3. Set entity baseline.
4. Configure equipment, enchants, and drop chances.
5. Set delay and cap rules.
6. Validate with info and drop summaries.

## Command subset

- `/spawner help`
- `/spawner set <entity_type>`
- `/spawner name set <name>` / `clear`
- `/spawner flag boss on|off`
- `/spawner cap <amount>`
- `/spawner equip <slot> fromhand`
- `/spawner equip clear <slot>|all`
- `/spawner enchant ...`
- `/spawner drop <slot> <0..1>`
- `/spawner drops`
- `/spawner delay <ticks>`
- `/spawner info`
- `/spawner reset`

## Edge behavior

- Non-spawner targets are rejected.
- Missing block entities are rejected.
- Preset auto-creation occurs on first mutating command if absent.
