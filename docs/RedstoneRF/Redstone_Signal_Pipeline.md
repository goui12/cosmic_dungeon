# Redstone RF Signal Pipeline (Developer) — 1.4.8

This guide explains how dungeon signal TX/RX behavior should be authored and validated.

## Feature intent

Redstone RF links world interactions (TX) to dungeon actions (RX) through named channels. It is designed for deterministic encounter orchestration.

## Pipeline behavior

1. A trigger source emits TX event on a channel.
2. Channel routes to one or more RX endpoints.
3. RX endpoints execute bound actions (doors, gates, waves, scene events).

## Channel design rules

- Use stable channel naming by dungeon and room scope.
- Avoid sharing a channel across unrelated progression branches.
- Treat fan-out channels as high-risk and validate ordering expectations.

## Runtime expectations

- TX creation alone has no gameplay effect until bound.
- RX with no bound action is inert.
- Graph validation should be considered mandatory before enabling a dungeon.

## Command subset

- `/rf tx create <channel>`
- `/rf tx bind <channel> <triggerId>`
- `/rf rx create <channel>`
- `/rf rx bind <channel> <actionId>`
- `/rf graph validate <dungeonId>`

## Debug strategy

- Trigger single-event sources first (buttons/plates) before timed systems.
- Validate each channel in isolation before introducing fan-out.
- If actions appear duplicated, check for overlapping TX bindings.

## Integration notes

- Keep channel ownership mapped to progression gates where possible.
- Use spawner arming/disarming as explicit RX actions for encounter pacing.
