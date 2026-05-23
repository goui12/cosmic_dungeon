# Dungeon Flow and Locks (Developer) — 1.4.8

This guide documents progression gates, checkpoints, and lock sequencing behavior.

## Feature intent

Progression systems ensure players complete objectives in designed order while preserving recoverability during failures/resets.

## Flow behavior

### Gates

- Gates define progression boundaries.
- Gate requirements are objective-based, not location-only.
- Force-open should be reserved for operations and recovery.

### Checkpoints

- Checkpoints persist session recovery anchors.
- Checkpoint placement should align with major encounter boundaries.

### Reset interactions

- Soft reset keeps authored dungeon topology but clears transient session state.
- Hard reset additionally clears deeper runtime maps across linked systems.

## Command subset

- `/progress gate create <dungeonId> <gateId>`
- `/progress gate require <gateId> <objectiveId>`
- `/progress gate open <gateId>`
- `/progress checkpoint set <dungeonId> <checkpointId>`
- `/dungeon reset <id> [soft|hard]`

## Design guidance

- Prefer short objective chains with explicit player feedback points.
- Couple gate unlocks to observable world events when possible.
- Keep emergency recovery paths documented for live operators.
