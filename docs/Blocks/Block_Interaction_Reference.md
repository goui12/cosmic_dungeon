# Blocks & Interaction Contracts (Developer) — 1.5

## Dungeon-control blocks

- `class_selector_block`: class selection + destination routing UI.
- [`cosmic_mob_spawner`](../Spawners/Spawner_Systems.md): configurable encounter spawner with preset workflow.
- `redstone_transmitter` / `redstone_receiver`: RF bus channel event endpoints.
- [`cosmic_rift` / `cosmic_rift_tile`](../Rifts/Rift_System_Guide.md): rift placement and destination routing anchors.
- [`infinite_dispenser`](#infinite-dispenser): dispenser behavior with non-depleting content semantics.
- Class-locked chest variants: class-gated loot access surfaces.

## Region/control utility blocks

- `region_ghost_glass`: visual helper for region look tooling; no item registration.
- `barrier_block`: indestructible/no-loot barrier contract.

## Encounter/theme blocks

- Spectral bloom set and potted spectral blooms.
- Full colored amethyst family (block, budding, bud tiers, cluster, lit).

## Infinite Dispenser

The Infinite Dispenser keeps its normal nine-slot block-entity inventory but does not consume the selected stack when fired by redstone. It now treats any `SpawnEggItem` as shootable in code, so vanilla eggs such as zombie eggs and modded mob eggs can be launched without maintaining a duplicated generated registry list.

- Arrow, tipped-arrow, and spectral-arrow stacks still fire as non-pickup arrows.
- Spawn eggs spawn the configured mob at the block directly in front of the dispenser on the server side.
- The `cosmicdungeon:infinite_shootables` item tag remains available for future non-egg shootable entries, but eggs do not need to be listed there.
- This change does not add packets, registries, saved fields, or block-entity storage keys. Updating from 1.5.0 to 1.5.1 only requires a normal world backup and deploying the new jar; existing Infinite Dispensers, [Cosmic Mob Spawners](../Spawners/Spawner_Systems.md), doors/keys, [Rifts/RD](../Rifts/Rift_System_Guide.md), [class systems](../Classes/Class_Selector_System.md), teleportation, and access-policy records keep their existing data.

## Operational expectation

- Control blocks should be placed in protected build layers to prevent accidental tampering.
- No-item helper blocks (like region ghost glass) are tooling artifacts, not player loot content.
