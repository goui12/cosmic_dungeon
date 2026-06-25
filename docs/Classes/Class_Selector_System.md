# Class Selector Block System (Developer) — 1.5

## System behavior

Class selector is a block+block-entity+menu+screen workflow that assigns class and can route players through configured destinations.

- Selector supports per-slot destination routing.
- Selector supports fallback destination.
- Selector supports max-player configuration for dungeon entry targeting.
- Teleport utility and ready-state tick manager coordinate handoff behavior.
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
