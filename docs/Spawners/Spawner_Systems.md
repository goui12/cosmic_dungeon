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
- `/spawner name set <name>` / `clear` — the configured mob name is shown near the top of `/spawner info` as `Mob Name`.
- `/spawner boss [true|false]`
- `/spawner cap <amount>`
- `/spawner equip <slot> fromhand`
- `/spawner equip clear <slot>|all`
- `/spawner enchant ...`
- `/spawner drop <slot> <0..1>`
- `/spawner drops`
- `/spawner showlabels` — toggles the developer-only bottom-right summary HUD while looking at a Cosmic Mob Spawner
- `/spawner delay <ticks>`
- `/spawner info`
- `/spawner reset`

## Runtime mob tagging and 1.5.0 compatibility

Cosmic Mob Spawner tagging is applied to the pending vanilla `SpawnData` during server ticks, not by adding required saved fields to the block entity. Existing 1.5.0 spawners therefore keep their configured base entity, optional preset, boss one-shot flag, mob cap, delays, spawn ranges, and player ranges when opened in 1.5.1. No manual conversion is required for spawners that were placed and configured before this update.

The marker prefix is intentionally shared with [Dungeon Group Split](../DungeonLifecycle/Dungeon_Group_Split.md), so base-entity-only spawners created with `/spawner set <entity_type>` and later cleared with `/spawner reset` do not bypass dungeon kill payouts once they spawn new mobs.


## Intrinsic loot-table drops and per-spawner rules

Cosmic Spawners now separate **equipment drops** from **intrinsic mob loot-table drops**. Equipment drop chances still use the configured `CosmicSpawnerPreset.Slot` values and vanilla mob equipment slots. Intrinsic drops are the mob loot-table items shown and managed through [`/spawner info`](../Spawner_Commands_Features.md#intrinsic-drops-in-spawner-info).

Runtime model:

- The active default display is resolved from the server's active entity type and loot-table resources/registry. It is not a hardcoded global list of vanilla mob drops, so datapack or modded loot-table replacements can be reflected in the operator display where they can be inspected safely.
- Per-spawner intrinsic rules are local final-chance overrides/additions stored in the Cosmic Spawner preset. They do **not** modify global vanilla or datapack loot tables.
- If a configured item exists in the active default loot table, the rule is displayed as an override. If it does not, the rule is displayed as a custom-added intrinsic drop.
- On spawned mobs, configured intrinsic rules are copied to entity persistent data under a namespaced Cosmic Dungeon key. Death/drop handling prefers the entity-attached rules and falls back to the `cosmic_spawner_<x>_<y>_<z>` block lookup for older already-spawned mobs.
- Boss one-shot spawners therefore keep their configured intrinsic drops after the spawner block self-destructs; the spawned entity carries the rules needed at death time.
- Drop behavior is server-authoritative: overridden generated drops for matching item IDs are removed, then the spawner-specific final chance is rolled independently and successful rolls create normal `ItemEntity` drops at the dead mob's position.

Complex loot tables can include conditions, functions, looting bonuses, weights, and killed-by-player checks. The display uses approximate/simple percentages only when safe; otherwise it labels rows as `complex`, `conditional`, `rare/player/looting`, or similar.

## 1.5.0 compatibility and lazy migration

Existing 1.5.0 Cosmic Spawner block entities are treated as legacy data when the `CosmicSpawnerDataVersion` key is missing. Loading preserves fields such as `SpawnerEntityId`, `SpawnerPreset`, boss one-shot flags, mob cap, and vanilla `BaseSpawner` fields such as delay, spawn range, player range, and spawn data. Saving after load writes the current version key.

Existing preset NBT fields remain supported, including `presetVersion`, entity type, custom name, illager captain variant, equipment slots, equipment drop chances, and legacy `SpawnerPreset/intrinsicDrops` float entries. Legacy intrinsic entries are loaded as configured final chances, displayed against the active loot table as overrides or custom additions, and saved in the new rule structure while also mirroring the legacy child during the 1.5.1 transition.

Preset JSON files under `<server_root>/cosmicdungeon/spawner_presets/` keep designer-authored values. Older `formatVersion: 1` files are read safely and logged for upgrade on next save; the reader does not destructively rewrite preset files during load.

## Edge behavior

- Non-spawner targets are rejected.
- Missing block entities are rejected.
- Preset auto-creation occurs on first mutating command if absent.
