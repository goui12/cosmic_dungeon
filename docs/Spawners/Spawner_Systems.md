# Spawner Systems (Developer) — 1.5

## Cosmic spawner runtime model

Spawner behavior is driven by `cosmic_mob_spawner` block entities and optional presets.

- Base entity type can be changed by command.
- Presets can define equipment by slot.
- Per-slot drop chances are configurable and default to `0.0` for all slots on new presets.
- Delay includes tick target and generated min/max range.
- Optional boss one-shot flag and per-spawner mob cap are available.
- Every mob spawned by a Cosmic Mob Spawner receives a stable `cosmic_spawner_<x>_<y>_<z>` entity tag at spawn time, even when the spawner uses only a base entity and has no preset, boss flag, or mob cap. This server-side marker is used by [Dungeon Group Split](../DungeonLifecycle/Dungeon_Group_Split.md) and per-spawner mob counting.

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
- `/spawner boss [true|false]`
- `/spawner cap <amount>`
- `/spawner equip <slot> fromhand`
- `/spawner equip clear <slot>|all`
- `/spawner enchant ...`
- `/spawner drop <slot> <0..1>`
- `/spawner drops`
- `/spawner showlabels`
- `/spawner delay <ticks>`
- `/spawner info`
- `/spawner reset`

## Runtime mob tagging and 1.5.0 compatibility

Cosmic Mob Spawner tagging is applied to the pending vanilla `SpawnData` during server ticks, not by adding required saved fields to the block entity. Existing 1.5.0 spawners therefore keep their configured base entity, optional preset, boss one-shot flag, mob cap, delays, spawn ranges, and player ranges when opened in 1.5.1. No manual conversion is required for spawners that were placed and configured before this update.

The marker prefix is intentionally shared with [Dungeon Group Split](../DungeonLifecycle/Dungeon_Group_Split.md), so base-entity-only spawners created with `/spawner set <entity_type>` and later cleared with `/spawner reset` do not bypass dungeon kill payouts once they spawn new mobs.

## Edge behavior

- Non-spawner targets are rejected.
- Missing block entities are rejected.
- Preset auto-creation occurs on first mutating command if absent.
