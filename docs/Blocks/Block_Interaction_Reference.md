# Blocks & Interaction Contracts (Developer) — 1.5

## Dungeon-control blocks

- `class_selector_block`: class selection + destination routing UI.
- `cosmic_mob_spawner`: configurable encounter spawner with preset workflow.
- `redstone_transmitter` / `redstone_receiver`: RF bus channel event endpoints.
- `cosmic_rift` / `cosmic_rift_tile`: rift placement and destination routing anchors.
- `infinite_dispenser`: dispenser behavior with non-depleting content semantics.
- Class-locked chest variants: class-gated loot access surfaces.

## Region/control utility blocks

- `region_ghost_glass`: visual helper for region look tooling; no item registration.
- `barrier_block`: indestructible/no-loot barrier contract.

## Encounter/theme blocks

- Spectral bloom set and potted spectral blooms.
- Full colored amethyst family (block, budding, bud tiers, cluster, lit).

## Operational expectation

- Control blocks should be placed in protected build layers to prevent accidental tampering.
- No-item helper blocks (like region ghost glass) are tooling artifacts, not player loot content.
