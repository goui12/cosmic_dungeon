# Region Protection Guide (Developer) — 1.5

This guide reflects the **actual implemented region system and command tree**.

## What regions are

Regions are axis-aligned cuboids stored per world save in `cosmicdungeon_regions`. Each region has:
- `name`
- `dimensionId`
- `min`/`max` corners
- optional `parent`
- a `flags` map
- `createdOrder` (used as a tie-breaker)

When multiple regions overlap, effective behavior is resolved by parent/child specificity and deterministic ordering in code (not by a user-set `priority` command).

## Effective region resolution

At a position, the region system:
1. Finds all regions containing the position in the same dimension.
2. Prefers regions that are descendants of other matched regions (deepest nested candidate survives).
3. Falls back to deterministic tie-breaks using volume and creation order.

Use `/region info`, `/region list`, and `/region look` to inspect what exists and where it applies.

## New-region defaults (important)

When a new region is created (via `/region new` or `/region create`):
- `interact` is pre-seeded to **allow** (`true`).
- Other standard flags are unset and therefore resolve by inheritance/default behavior.

This means `/region flag interact allow` is now the default state for newly created regions.

## Region commands (actual)

### Root
- `/region`

### Wand / selection
- `/region wand`
  - Gives the Region Wand used for setting selection points (Pos1/Pos2).

### Create
- `/region new <name>`
- `/region create <name>`
- `/region new <name> copy <source>`
- `/region create <name> copy <source>`
  - Creates from your current wand selection.
  - `copy <source>` copies the source region flag map.

### Visualization
- `/region look <name>`
  - Toggle rendering for one named region.
- `/region look all`
  - Toggle rendering for nearby regions.
  - Region outlines now render in an x-ray overlay pass so edges remain visible even when buried behind solid blocks, while still using normal nearby/render-distance filtering for large or far-away regions.

### Inspection / structure
- `/region info <name>`
- `/region here`
  - Shows `/region info`-style output for all region(s) at your current player position.
- `/region list`
- `/region delete <name>`
- `/region parent <region> <newParent>`
  - `newParent` can be a region name, `none`, or `null` to clear parent.

### Flag UI + explicit flag edits on named region
- `/region flags <name>`
  - Opens clickable flag UI for that region.
- `/region flags <name> <flag> <allow|deny|clear>`
- `/region flags <name> inherit <flags|exceptions> <on|off>`
- `/region flags <name> exceptions <place|break>`
- `/region flags <name> exceptions <place|break> <torch|ladder|water> <allow|deny|clear>`
  - `water` exception exists only for `place` scope.

### Quick edit effective region at your current position
- `/region flag list`
- `/region flag <flag> <allow|deny>`
  - Applies to the effective region where you stand.

## Known flag keys

Standard region flags:
- `place`
- `break`
- `interact`
- `explode`
- `mobgrief`
- `spread`
- `burn`

Exception flags are stored as:
- `place.ex.torch`, `place.ex.ladder`, `place.ex.water`
- `break.ex.torch`, `break.ex.ladder`

Inheritance control keys:
- `__inherit_flags`
- `__inherit_exceptions`

## Practical workflow (recommended)

1. `/region wand`
2. Select Pos1/Pos2 with wand.
3. `/region create <name>`
4. Optionally assign parent: `/region parent <child> <parent>`
5. Configure policy in `/region flags <name>`.
6. Validate with `/region look <name>`, `/region info <name>`, `/region list`.

## Important corrections vs old docs

- There is no `/region define`, `/region pos1`, `/region pos2`, `/region commit`, `/region mode`, `/region priority`, or `/region inspect` command in the current code.
- Priority/mode-based semantics from older docs are obsolete; actual control is flag + parent/inheritance driven.
