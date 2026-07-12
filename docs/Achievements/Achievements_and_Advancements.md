# Achievements & Advancements — 1.5

Cosmic Dungeon achievements use generated advancements plus server-side helper services for incremental gameplay hooks.

## Current foundations

- Advancement IDs are centralized and generated through datagen.
- New players are granted the **First Trace** onboarding achievement on first login if they do not already have it; the reward deposits 5 Trace into their `/currency` balance and introduces Trace as currency for purchases, upgrades, and dungeon rewards. The internal advancement path remains `achievements/im_rich` for player advancement compatibility.
- Developer commands can grant achievements and inspect/reset persisted counters.
- Binding Idol counters track returns through an idol and players who provide idols to others.
- Vital Exchange hooks grant achievements when the expected healing/support items are provided to a Deadeye receiver.
- Plant Flags tracks D1 run-scoped banner planting and grants the Plant Flags advancement when eligible online members complete the requirement.
- D1 environmental tracker services exist for region-based achievement work.
- Successful player-to-player trade finalization grants the first-trade onboarding achievement to both participants.

## First Trace onboarding achievement

**First Trace** is the visible onboarding achievement for the first-login Trace grant. It keeps the existing `cosmicdungeon:achievements/im_rich` advancement id so existing player advancement files remain compatible, but player-facing title/copy now use First Trace. Trace represents stabilized fragments of severed divine attunement recovered from dungeon-bound or attuned beings. Freed NPCs gather Trace because it helps resist future binding, re-attunement, displacement, and enslavement.

## Trade onboarding achievement

**Handshake Protocol** tracks "You have traded with a player at least once." It is granted only after the server successfully finalizes a [player-to-player trade](../Trading/Trading_Guide.md), after item/currency capacity checks and transfers pass. The client receives a small synced prompt state on login and after the grant so the CAPS LOCK look prompt no longer appears for players who have already traded once. This stores advancement progress only; it does not add saved entity or block-entity fields.

## Operator workflow

Use [Commands: Achievement](../commands/In_Game_Commands.md#achievement-15-advancement-foundation) for debug grants and counter inspection. Use [Commands: Region Quest Reactions](../commands/In_Game_Commands.md#region-quest-reactions-151-location-quest-foundation) for Plant Flags region setup and diagnostics through `/region quest plant_flags ...`.

## Related topics

- [Class Restrictions & Inventory](../Classes/Class_Restrictions_and_Inventory.md) for class-attuned Plant Flags banners.
- [Progression, Factions & Unlocks](../Progression/Progression_Factions_and_Unlocks.md) for D1 unlock context.
- [Trading Guide](../Trading/Trading_Guide.md) for the trade finalization hook that grants Handshake Protocol.

## Changelog

- **1.5.1:** Added Handshake Protocol for first successful player trade and uses its synced state to retire the CAPS LOCK trade prompt.
- **1.5.1:** Standardized Plant Flags setup and diagnostics under `/region quest plant_flags ...` and added a small region quest handler registry for future location-based quest reactions.
- **1.5:** Added generated achievement scaffolding, counter persistence, Binding Idol hooks, Vital Exchange hooks, Plant Flags tracking, D1 environmental tracker foundations, and the First Trace currency onboarding achievement while preserving the legacy `achievements/im_rich` id.


## Nostalgia Bait

- **Title:** Nostalgia Bait
- **Description:** Teleport back to the Main Village using Farrow's Chop.
- **Trigger:** Granted only after a successful Farrow's Chop teleport to `main_village`. Failed destination lookups or unsafe teleports do not grant it.
