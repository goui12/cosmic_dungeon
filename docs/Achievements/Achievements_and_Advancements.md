# Achievements and Advancements - 1.5.1

Cosmic Dungeon uses generated advancements and server-side gameplay services.
The visible catalog follows the repository's lore Docs and the Dungeon Crawl Master Sheet:
30 named achievements plus six Bloom records. Every displayed entry has a description
and an item icon tied to its subject.

## Advancement gallery

Open Advancements with the normal key or menu entry. The larger starfield window
shows achievement cards, earned status, chapter navigation and a detail panel.
Select a card to read its requirement; scroll over the detail panel for long text,
or over the card area to change pages. The layout adapts to the game's GUI scale.
Vanilla and other server-provided advancement roots remain available as chapters.
Secret advancements remain hidden until earned.

## Lore catalog

The catalog includes First Trace, The Tamsin Tax, Plant Flags, the ten Binding Idol
milestones, Tired, Not Broken, Vital Exchange I-IV, the four Sixfold Vigil records,
Cycle of Recorded Sound, Synchronous Peal, Nostalgia Bait, Wolves in Piglin Clothing,
Fire Escape, Librarian 1, Shulker Express and Stairway to Heaven.
The six existing Bloom records retain their shared discovery progress.

The descriptions explain the lore-defined requirements. Their presence does not
assert that all authored world bindings or later-dungeon gameplay have been tested.
See the [task audit](../ai/tasks/advancement-lore-gallery-20260925.md) for source
coverage and current validation limits.

## First Trace onboarding achievement

First Trace introduces Trace and deposits 5 Trace on first login when the player
does not already have the advancement. Trace is currency formed from fragments of
severed divine attunement. The existing advancement path `achievements/im_rich`
and its progress criterion remain unchanged for saved-player compatibility.

## Trade onboarding achievement

The old Handshake Protocol award is retired from the visible catalog because it
has no entry in the reviewed lore. Its internal `achievements/first_player_trade`
progress marker remains: a successful server-finalized [trade](../Trading/Trading_Guide.md)
records completion and retires the CAPS LOCK tutorial prompt for both participants.
It has no display, toast, chat announcement or reward. Existing completion remains valid.

BOOM!, Monster Compendium, Player Classes and the Pyroclast category are also removed
from the visible catalog. Their old IDs remain displayless compatibility records.
Gunpowder use no longer grants BOOM!; its ordinary gameplay behavior is unchanged.

## Nostalgia Bait

Return to the Main Village using Farrow's Powered Chop. Only a successful teleport
to `main_village` grants the advancement; failed or unsafe teleports do not.

## The Tamsin Tax

Personally discover the Base Camp, complete Dungeon 1 and give Tamsin Vane one
eligible named Dungeon 1 item. Currency is not an eligible payment.
The advancement is non-repeatable and hidden until earned. Its existing gameplay
gates and criterion are unchanged by the catalog/presentation update.

## Compatibility and authority

Retained advancement IDs, criteria, rewards, counters and Bloom progress are preserved.
No save migration is required. The client reads the server's advancement tree and
progress through the native listener and selected-tab packets; it cannot grant awards.
No new packet format, registry, saved-data field or runtime dependency is introduced.

## Operator workflow

Developer diagnostics are documented under
[Commands: Achievement](../commands/In_Game_Commands.md#achievement-15-advancement-foundation).
Plant Flags region setup remains under
[Commands: Region Quest Reactions](../commands/In_Game_Commands.md#region-quest-reactions-151-location-quest-foundation).

## Related topics

- [Class Restrictions and Inventory](../Classes/Class_Restrictions_and_Inventory.md)
- [Progression, Factions and Unlocks](../Progression/Progression_Factions_and_Unlocks.md)
- [Trading Guide](../Trading/Trading_Guide.md)
