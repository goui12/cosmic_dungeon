# Region Protection Guide (Developer) — 1.4.8

This guide documents region behavior and practical setup patterns for world developers.

## Feature intent

Region protection controls where players can move, interact, fight, and bypass encounter logic. Region mode and priority are the two most important behavior levers.

## Region behavior model

### Boundary resolution

- Regions are axis-aligned cuboids with committed corner pairs.
- Overlap is allowed and resolved by priority.
- Higher priority region policies supersede lower priority policies.

### Mode expectations

- `entry`: onboarding/briefing behavior; typically low restrictions.
- `combat`: active encounter space; combat and trigger logic expected.
- `safe`: no-combat recovery or staging area.
- `utility`: maintenance/logic space, typically inaccessible to players.

### Transition behavior

Crossing boundaries can trigger:
- encounter activation,
- progression milestone logging,
- fallback teleport if crossing blocked edges.

## Developer workflow

1. Define outer containment region first.
2. Add internal encounter regions with higher priorities.
3. Carve utility lanes last with strict policy.
4. Inspect every overlap and record intended winner.

## Region command subset

- `/region define <dungeonId> <regionId>`
- `/region pos1`
- `/region pos2`
- `/region commit <dungeonId> <regionId>`
- `/region mode <regionId> <entry|combat|safe|utility>`
- `/region priority <regionId> <value>`
- `/region inspect <regionId>`

## Common failure patterns

- **Misordered priorities** causing safe zones to lose to combat zones.
- **Uncommitted selections** resulting in no active protection behavior.
- **Overwide utility regions** shadowing player-intended spaces.

## Integration notes

- Pair region transitions with progression checkpoints at choke points.
- Keep Redstone RX action volumes inside deterministic region modes.
- Avoid placing critical gates exactly on boundary edges where players can oscillate states.
