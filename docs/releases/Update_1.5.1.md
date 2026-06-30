# Update 1.5.1 Notes

## Dungeon party leadership

- The first player to ready up through the class selector is now announced as the **Group Leader** for that dungeon run.
- Added `/dungeoneer kick <player>` for the active Group Leader to remove another member from the run immediately. Kicked players are restored from their pre-run state, returned to the main overworld spawn, and removed from lifecycle/progression tracking so they do not count against completion, abandonment, or dungeon reset registration.

## Added
- Added instant brewing stand ability for the Theurgist class. See [Theurgist Help Guide](../Classes/Theurgist_Help_Guide.md).

- Added support for Dragoon passive innate ability 3% chain lightning. See [Dragoon Help Guide](../Classes/Dragoon_Help_Guide.md).

- Added gunpowder crafting ability for Pyroclast class. See [Pyroclast Help Guide](../Classes/Pyroclast_Help_Guide.md).

## Fixed
- Fixed hitbox on class chests.

## Potion of Companionship

- Added the Potion of Companionship, a light-pink dungeon party teleport potion.
- Drinking it inside an active dungeon group opens a dungeoneer selection GUI containing only online players in the same active dungeon lifecycle.
- Selecting a dungeoneer teleports the drinker to that player and applies a visible five-minute Teleport Cooldown effect.
- Cooldown and non-dungeon attempts do not consume the potion and provide clear error messages.
- Added the potion to the Food & Drinks and Dungeon Items creative tabs.
- Added the potion to Beluzon Everly's Save Teleport NPC shop for one Seal.

## Dungeon AFK handling

- Added active dungeon AFK detection after more than 15 minutes without server-observable player input. See [Dungeon AFK Handling](../DungeonLifecycle/Dungeon_AFK_Handling.md).
- Other dungeoneers are warned that the AFK player will no longer receive Group Split until returning; Group Split payout enforcement remains a TODO for the future Group Split implementation.
- Group Leaders receive clickable `[YES]` / `[NO]` kick prompts for AFK members. `[YES]` reuses the existing Group Leader kick path and `[NO]` does nothing.
- AFK state is transient and does not modify saved entity/block-entity data, so 1.5.0 worlds can update to 1.5.1 without spawner, door/key, rift/RD, class selector, or access-policy migration for this feature.
