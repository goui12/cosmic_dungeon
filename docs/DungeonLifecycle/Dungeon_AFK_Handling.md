# Dungeon AFK Handling

Dungeon AFK handling watches active dungeon members for server-observable player activity. Movement, camera changes, block/item/entity interactions, chat messages, commands, and server-side network actions that result in those events refresh the player's activity timer.

## AFK threshold and messages

- A dungeoneer is flagged AFK after more than 15 minutes without player input while they are part of an active dungeon run.
- Every other online member in the same run receives: `Player: <name> has been AFK for 15 minutes. They will no longer receive Group Split until they return`.
- The Group Leader also receives a clickable prompt: `Player: <name> has been AFK for 15 minutes. Would you like to kick them from the dungeon? [YES] [NO]`.
- Clicking `[YES]` runs the same server-authoritative kick path as `/dungeoneer kick <player>` after revalidating that the target is still online, still AFK, still in the same active run, and that the clicker is still the Group Leader.
- Clicking `[NO]` intentionally does nothing.

## Return from AFK

When the server observes activity from an AFK dungeoneer, the AFK flag is cleared and all online members in the run receive: `Player: <name> is no longer afk. Group split is reactivated for this player`.

## Group Split integration note

Group Split payout logic is implemented in 1.5.1. The AFK service flag is honored by [Dungeon Group Split](./Dungeon_Group_Split.md), so AFK dungeoneers are excluded from both the payout divisor and the final Trace grant until activity clears the flag.

## Persistence and update safety

AFK state is transient runtime state only. It is not written to entity data, block-entity data, dungeon run save data, rift data, door/key data, access-policy data, or Cosmic Mob Spawner storage. Updating a server from 1.5.0 to 1.5.1 does not require migration of existing dungeon runs, spawners, doors/keys, rifts/RD entries, class selector data, or region/access records for this feature.
