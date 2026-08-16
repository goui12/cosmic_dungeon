# Class Selector Block System (Developer) — 1.5

## System behavior

Class selector is a block+block-entity+menu+screen workflow that assigns class and can route players through configured destinations.

- Selector supports per-slot destination routing.
- Selector supports fallback destination.
- Selector supports max-player configuration for dungeon entry targeting.
- Teleport utility and ready-state tick manager coordinate handoff behavior.
- Dungeon 1 parties may contain one through six players. Ready order remains authoritative: startup constructs six logical class slots in that order and fills every unoccupied slot with the literal `blankslot` class token.
- Dungeon 1 startup builds six ordered schematic groups with six logical-slot operations each. All 36 operations must complete against the prepared physical instance before the run is registered or any party member is teleported.
- The first player to ready up is announced as the **Group Leader** for that dungeon run. The run keeps this same leader by using the first UUID in the ordered ready list.
- During an active dungeon run, the Group Leader can use `/dungeoneer kick <player>` to remove another run member immediately. Kicked members are restored from their pre-run snapshot, cleared from run lifecycle/progression side data, teleported to the main overworld spawn, and no longer count for reset/completion registration.
- Metalmancer and Deadeye remain visible in the selector list but are temporarily disabled: their buttons render with the normal disabled-button shading and cannot be clicked. Server-side selection normalization also rejects those class IDs while they are disabled.

## Developer interactions

- Configure selector in proximity; configuration commands enforce distance checks.
- Slot index validity is constrained by selector capacity.
- Destination identifiers must be valid registered destinations.

## Command subset

- `/classselectordestination help`
- `/classselectordestination set <slot> <destination>`
- `/classselectordestination clear <slot>`
- `/classselectordestination fallback set <destination>`
- `/classselectordestination fallback clear`
- `/classselectordestination maxplayers <n>`

## Related dungeon lifecycle systems

- [Dungeon AFK Handling](../DungeonLifecycle/Dungeon_AFK_Handling.md) documents the active-run AFK timer and Group Leader kick prompt that applies after class selector runs begin.
