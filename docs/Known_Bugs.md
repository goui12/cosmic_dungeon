# Known Bugs (Developer Tracking) — 1.5

Only issues evidenced directly from implementation behavior, explicit TODO markers, or command/runtime guard rails are listed.

## Active known bugs

1. **Crystal Creeper amethyst-eat interaction uses vanilla amethyst item mapping placeholder**
   - Evidence: TODO in creeper eat-goal indicates current item mapping is placeholder.
   - Developer impact: custom amethyst variants may not participate as expected in that behavior path.

2. **Metalmancer action set is partially stubbed**
   - Evidence: TODO stubs for ore projectile spawn, vortex chaining, golem recall, and ore-based healing.
   - Developer impact: designers should not assume those action branches are production-complete.

3. **Metalmancer summoning staff tier handling is explicitly incomplete**
   - Evidence: TODO indicates future tier expansion path not yet implemented.
   - Developer impact: tier expectations beyond implemented branches can fail silently or behave as fallback.

4. **Spawner intrinsic loot-table chance display is approximate for complex tables**
   - Evidence: Minecraft loot tables can include conditions, functions, looting bonuses, killed-by-player checks, weighted alternatives, and datapack logic that are not always safely reducible to one exact percentage.
   - Developer impact: `/spawner info` lists displayable intrinsic items and marks complex rows as `complex`, `conditional`, `rare/player/looting`, or similar; operators should use explicit per-spawner overrides when they need exact final chances.

5. **Rift deletion/list operations are dimension-sensitive and can be misread as global**
   - Evidence: command feedback warns chat delete actions are current-dimension scoped.
   - Developer impact: operators can remove/look up wrong target set if dimension context is not verified.

6. **Class selector configuration can fail by distance and slot bounds during remote ops**
   - Evidence: repeated guard failures for distance, slot bounds, and missing block entity.
   - Developer impact: automation scripts must move operator avatar into valid proximity before applying config.

7. **Dungeon snapshot restore can abort when occupancy/chunk drain preconditions are not met**
   - Evidence: lifecycle snapshot service has explicit abort branches for occupants, missing paths, and stalled unload drains.
   - Developer impact: hard reset rehearsals can intermittently fail if prep conditions are not strictly enforced.

## Phase 6 Cosmic Spawner Intrinsic Drop Counts

Cosmic Mob Spawner intrinsic drops now use lossless rule rows with a stable rule id, item id, final chance, stack count, and rule kind. Use `/spawner drop intrinsic <namespace:item> <chance 0.0-1.0> [quantity]`; omitted quantity defaults to `1`, and counts are clamped to the safe 1-64 range.

Multiple independent rules for the same item are supported. For example, potato rules at `1.0 1`, `0.5 2`, and `0.1 5` roll separately, so one kill can drop 1, 3, 6, or 8 potatoes depending on which rows succeed. Chance always applies to that configured stack, not to the combined item total.

`/spawner info` shows configured rows as `Chance: <percent> [+] [-] Count: <n> [+] [-] [Default]`. The chance and count buttons target the stable rule id for that row, so duplicate item rows are safe. Clicking `[Default]` on a configured row removes only that rule; `/spawner drop intrinsic default <item>` and the backward-compatible `/spawner drop intrinsic clear <item>` remove all configured rules for that item and restore vanilla/datapack default behavior. `/spawner drop intrinsic add <item> <chance> [quantity]` always adds a new independent rule, while `/spawner drop intrinsic <item> <chance> [quantity]` upserts by item/count for convenience.

Save migration is lazy and backward-compatible: 1.5.0 data, block-entity data versions 150/151, preset versions 2/3, preset file formats 1/2, old `intrinsicDrops` maps, and Phase-5 `intrinsicDropRules` keyed by item id are read as one count-1 rule per old item chance. New block entities save as data version 152, presets save as preset version 4, and preset JSON saves as format version 3 while preserving the full `spawnerPresetNbt` rule list. The legacy `intrinsicDrops` mirror remains compatibility-only and cannot represent duplicate rows.

## Phase 7 Cosmic Spawner Spawn Defaults

Cosmic Mob Spawners now apply a small server-side spawn-default pass only to mobs carrying that spawner's `cosmic_spawner_<x>_<y>_<z>` marker. Vanilla Wardens spawned by Cosmic Spawners receive a 1200-tick `minecraft:dig_cooldown` brain memory so they do not immediately dig after spawning.

Vanilla `minecraft:slime` and `minecraft:magma_cube` spawned by Cosmic Spawners are raised to the max standard size 4 using the entity API so health and dimensions refresh correctly. Larger entities are not shrunk, and naturally spawned mobs are unaffected.

This phase does not change Cosmic Spawner block-entity storage, preset JSON/NBT storage, intrinsic drop rule storage, rift/RD data, door/key data, access policy, class, teleportation, or dungeon reset data. The only new marker is per spawned entity: `cosmicdungeon:spawner_spawn_defaults_applied_version`, used to avoid reapplying the defaults forever.
