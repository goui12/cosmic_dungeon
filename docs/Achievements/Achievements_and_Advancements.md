# Achievements & Advancements — 1.5

Cosmic Dungeon achievements use generated advancements plus server-side helper services for incremental gameplay hooks.

## Current foundations

- Advancement IDs are centralized and generated through datagen.
- Developer commands can grant achievements and inspect/reset persisted counters.
- Binding Idol counters track returns through an idol and players who provide idols to others.
- Vital Exchange hooks grant achievements when the expected healing/support items are provided to a Deadeye receiver.
- Plant Flags tracks D1 run-scoped banner planting and grants the Plant Flags advancement when eligible online members complete the requirement.
- D1 environmental tracker services exist for region-based achievement work.

## Operator workflow

Use [Commands: Achievement](../commands/In_Game_Commands.md#achievement-15-advancement-foundation) for debug grants and counter inspection. Use [Commands: Plant Flags](../commands/In_Game_Commands.md#plant-flags-15-d1-foundation) for Plant Flags region setup and diagnostics.

## Related topics

- [Class Restrictions & Inventory](../Classes/Class_Restrictions_and_Inventory.md) for class-attuned Plant Flags banners.
- [Progression, Factions & Unlocks](../Progression/Progression_Factions_and_Unlocks.md) for D1 unlock context.

## Changelog

- **1.5:** Added generated achievement scaffolding, counter persistence, Binding Idol hooks, Vital Exchange hooks, Plant Flags tracking, and D1 environmental tracker foundations.
