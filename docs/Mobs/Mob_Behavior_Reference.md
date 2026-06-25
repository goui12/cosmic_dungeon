# Mobs & AI Behavior Reference (Developer) — 1.5

## Registered custom mobs

- magma_glob
- stone_warden
- goblin_ambusher
- metalmancer_golem
- crystal_creeper
- cthonian_gnawling

## Behavior expectations

Each mob uses custom goal stacks and render/model pipelines. Encounter authors should treat these as behavior contracts:

- Stone Warden: heavy melee pressure profile.
- Goblin Ambusher: fast engage pressure profile.
- Crystal Creeper: swell/explosion behavior and amethyst interaction goal.
- Cthonian Gnawling: latch-style pressure behavior.
- Metalmancer Golem: owner-follow/protect pattern (summoned utility companion).

## Spawner integration notes

- Cosmic spawner presets can equip entities by slot and configure per-slot drop chance (default `0.0` on new presets).
- Boss one-shot and mob cap flags alter encounter repetition behavior.
- Delay controls affect cadence and should be tuned with progression gates.
