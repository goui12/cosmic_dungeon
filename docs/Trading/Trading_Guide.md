# Trading Guide — 1.5

Trading lets two players exchange item offers and Attunement Fragment currency through a server-authoritative trade session.

## Starting a trade

- Use `/trade <player>` to invite an online player.
- Incoming chat includes clickable **Accept Trade** and **Deny Trade** actions.
- The **Trade Request** keybind defaults to `CAPS LOCK` and sends the same invite path when looking at a nearby player. The look prompt is a client-only onboarding hint and stops appearing after the player completes one successful trade and receives the [first-trade achievement](../Achievements/Achievements_and_Advancements.md#trade-onboarding-achievement).
- Invites expire after 30 seconds and successful invite sends have a short cooldown.

## Trade window

- Partner offer slots are read-only.
- Your offer slots accept item stacks until you accept/lock your offer.
- Currency controls use Attunement denominations and show normalized offer summaries.
- The first accept locks your current offer; after both players are ready, accept confirms/finalizes.
- Deny/cancel returns offered items and discards uncommitted currency offers.

## Safety and server authority

The server validates online state, active-session state, inventory capacity, account balances, currency capacity, and menu lifecycle. Disconnects and menu closes clean up the session and return items where possible. Successful finalization grants the first-trade achievement to both participants server-side; clients only use the synced achievement state to hide the onboarding prompt and cannot bypass the server-authoritative trade checks.

## Related topics

- [Economy & Currency](../Economy/Economy_and_Currency.md) documents Trace and denominations.
- [Trade GUI Coordinate Map](./Trade_GUI_Coordinate_Map.md) documents the current texture and procedural layout coordinates.
- [Commands: Trade](../commands/In_Game_Commands.md#trade-15-player-to-player-foundation) lists command syntax and authority.
- [Achievements & Advancements](../Achievements/Achievements_and_Advancements.md#trade-onboarding-achievement) documents the first-trade achievement used to retire the prompt.

## Changelog

- **1.5.1:** Fixed the CAPS LOCK look prompt text rendering, and added the first-trade achievement so the prompt retires permanently after a successful player trade.
- **1.5:** Added trade invites, clickable chat responses, item/currency offer slots, ready/confirm flow, CAPS LOCK look-target request, server validation, and disconnect cleanup.
