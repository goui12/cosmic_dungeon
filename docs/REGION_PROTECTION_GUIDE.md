# Region Protection System Guide

This guide covers how to use, operate, and maintain the Cosmic Dungeon region protection system.

## Goals and behavior model

The system is built around WorldGuard-like concepts:
- Cuboid region selection via a wand.
- Region creation/deletion/listing/inspection.
- Overlap resolution via an **effective region** choice.
- Parent/child inheritance for flags and exceptions.
- Explicit allow/deny/clear semantics.

## Core concepts

### Region
A region stores:
- `name`
- `dimensionId`
- `min`/`max` corners
- `flags` map
- optional `parent`
- `createdOrder`

### Effective region in overlaps
When multiple regions overlap, the server resolves a single effective region via `RegionRegistryData.effectiveRegionFromList(...)`.
Parenting and creation order are then used for inheritance/value resolution.

### Flag value sources
Flag UI surfaces source metadata:
- `DEFAULT`
- `INHERITED`
- `OVERRIDDEN`

This helps debug unexpected behavior quickly.

## Player/dev command guide

## Selection and creation
- `/region wand`
  - Gives you the region wand.
- `/region new <name>`
- `/region create <name>`
  - Creates region from your current wand selection.
- `/region new <name> copy <sourceRegion>`
- `/region create <name> copy <sourceRegion>`
  - Creates region from selection and copies all flag values from `<sourceRegion>`.
  - Cuboid is still from your selection; only flags are copied.

## Inspection and management
- `/region list`
- `/region info <name>`
- `/region delete <name>`
- `/region parent <region> <newParent>`
  - Use `none` as parent to clear.

## Visual debugging
- `/region look all`
- `/region look <name>`

## Flag control
### Named region controls
- `/region flags <name>`
- `/region flags <name> <flag> <allow|deny|clear>`
- `/region flags <name> inherit <flags|exceptions> <on|off>`
- `/region flags <name> exceptions <place|break>`
- `/region flags <name> exceptions <place|break> <ex> <allow|deny|clear>`

### Current-region shortcut
- `/region flag list`
- `/region flag <flag> <allow|deny>`

## Current built-in flags
- `place`
- `break`
- `interact`
- `explode`
- `mobgrief`
- `spread`
- `burn`

## Current built-in exceptions
- `place`: `torch`, `ladder`, `water`
- `break`: `torch`, `ladder`

## Tips / tricks

- Prefer a clear region hierarchy (large parent, smaller children).
- Keep inheritance ON unless you need explicit local overrides.
- Use `clear` to return to inherited/default behavior.
- Use `/region flags <name>` often; source pills (`DEFAULT/INHERITED/OVERRIDDEN`) are your best debugging signal.
- For repetitive setups, use `copy` at creation time to bootstrap flags quickly.

## Known limitations / bugs to address

1. Some declared flags may not yet be fully enforced in event handlers.
   - Example: `mobgrief` exists in command/UI surface and storage, but audit should confirm dedicated enforcement paths.
2. Copy command currently copies only flags (by design), not parent, members, owners, priority, or geometry.
3. Region names are treated as single tokens for create/copy path (`word` argument), so spaces are not supported.
4. Protection hardening should continue to be tested against edge interactions (buckets, entity interactions, special blocks, modded placement patterns).

## Developer maintenance checklist

- When adding a new flag:
  1. Add it to command suggestions/validation.
  2. Add UI row display.
  3. Add runtime enforcement in protection events.
  4. Add docs update here and in IN_GAME_COMMANDS.

- When adding a new exception:
  1. Add exception key list in command.
  2. Add classification tags/logic in protection event helpers.
  3. Add corresponding UI controls.
  4. Add docs update.

- Add regression tests where practical for:
  - inheritance on/off
  - override clear semantics
  - overlap/effective selection
  - create-with-copy flow

