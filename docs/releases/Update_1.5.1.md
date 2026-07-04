# Update 1.5.1 Notes

## Dungeon party leadership

- The first player to ready up through the class selector is now announced as the **Group Leader** for that dungeon run.
- Added `/dungeoneer kick <player>` for the active Group Leader to remove another member from the run immediately. Kicked players are restored from their pre-run state, returned to the main overworld spawn, and removed from lifecycle/progression tracking so they do not count against completion, abandonment, or dungeon reset registration.

## Added
- Added instant brewing stand ability for the Theurgist class. See [Theurgist Help Guide](../Classes/Theurgist_Help_Guide.md).

- Added support for Dragoon passive innate ability 3% chain lightning. See [Dragoon Help Guide](../Classes/Dragoon_Help_Guide.md).

- Added gunpowder crafting ability for Pyroclast class. See [Pyroclast Help Guide](../Classes/Pyroclast_Help_Guide.md).

## Region quest reactions

- Moved Plant Flags operator tooling from `/plantflags ...` to `/region quest plant_flags ...`. See [In-Game Commands](../commands/In_Game_Commands.md#region-quest-reactions-151-location-quest-foundation) and [Achievements & Advancements](../Achievements/Achievements_and_Advancements.md).
- Quest identifiers for region-backed reactions are single words; use `plant_flags` exactly. This leaves room for future quests such as `JHW_Returns` without allowing multi-word quest names to be confused with command arguments.
- Region-backed quest commands now route through a small handler registry, so future quests can register their own `RegionQuestHandler` without adding quest-specific branches to `/region quest`.
- This refactor reuses the existing Plant Flags saved data format (`cosmicdungeon_plant_flags_v1`) and only changes the command surface. It does not add required saved fields or alter entity/block-entity storage for [Cosmic Mob Spawners](../Spawners/Spawner_Systems.md), [Rifts](../Rifts/Rift_System_Guide.md), doors/keys, class selector data, teleportation, or access-policy records. Updating from 1.5.0 to 1.5.1 requires no migration for those systems; keep a normal world backup, deploy the 1.5.1 jar, and run `/region quest plant_flags status` to verify the existing configured cuboid loaded.

## Trading prompt and achievement

- Player-to-player trades can now complete as item-only, currency-only, mixed item/currency, or no-currency item exchanges. The shared server-side finalization harness skips zero-value currency withdraw/deposit operations while preserving balance, capacity, inventory, and menu lifecycle validation, and registered GameTests cover item-only, currency-only, mixed, full-inventory, and currency-capacity-limit paths. See [Trading Guide](../Trading/Trading_Guide.md#trade-window).
- The trade GUI now spaces coin controls farther apart, treats coins as visual buttons without stack-count overlays, and displays both players' synced balances next to the trade panels. See [Trade GUI Coordinate Map](../Trading/Trade_GUI_Coordinate_Map.md#7-current-implementation).
- Fixed the player-to-player trade look prompt so the player name and `CAPS LOCK` instruction render over the prompt background instead of appearing as only a gray box. See [Trading Guide](../Trading/Trading_Guide.md#starting-a-trade).
- Added the **Handshake Protocol** achievement for completing one successful player-to-player trade. After the server finalizes a trade and grants this advancement, the client stops showing the CAPS LOCK onboarding prompt for that player. See [Achievements & Advancements](../Achievements/Achievements_and_Advancements.md#trade-onboarding-achievement).
- This change uses generated advancement data and a small login/finalization prompt-state sync. It does not modify server-data-storage methods for entity/block-entity data such as [Cosmic Mob Spawners](../Spawners/Spawner_Systems.md), doors/keys, [Rifts/RD](../Rifts/Rift_System_Guide.md), [classes](../Classes/Class_Selector_System.md), teleportation, or access-policy records. Updating from 1.5.0 to 1.5.1 requires no migration for those systems: keep a normal world backup, deploy the 1.5.1 jar, and let advancement progress sync on player login.

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
- Other dungeoneers are warned that the AFK player will no longer receive Group Split until returning; Group Split payout enforcement is active for dungeon mob kills, so AFK members are excluded from the divisor and payout.
- Group Leaders receive clickable `[YES]` / `[NO]` kick prompts for AFK members. `[YES]` reuses the existing Group Leader kick path and `[NO]` does nothing.
- AFK state is transient and does not modify saved entity/block-entity data, so 1.5.0 worlds can update to 1.5.1 without spawner, door/key, rift/RD, class selector, or access-policy migration for this feature.

## Dungeon Group Split

- Dungeon mob kills now pay [Trace Group Split](../DungeonLifecycle/Dungeon_Group_Split.md) from the mob max-HP heart value. Eligible dungeoneers split the pool evenly through the existing [currency account](../Economy/Economy_and_Currency.md) deposit path.
- Eligibility is server-side: active run membership, same world as the mob, within 100 blocks, online/non-spectator, and not AFK according to [Dungeon AFK Handling](../DungeonLifecycle/Dungeon_AFK_Handling.md).
- [Cosmic Mob Spawners](../Spawners/Spawner_Systems.md) now tag every newly spawned mob with the shared `cosmic_spawner_<x>_<y>_<z>` marker used by Group Split, including base-entity-only spawners with no optional preset, boss one-shot flag, or mob cap.
- These 1.5.1 trade GUI/finalization fixes are runtime menu/network behavior and do not change currency saved-data schemas. This update also keeps runtime/server-player currency and runtime SpawnData state only; it does not add required saved fields or invalidate existing entity/block-entity storage for [Cosmic Mob Spawners](../Spawners/Spawner_Systems.md), [Rifts](../Rifts/Rift_System_Guide.md), doors/keys, [classes](../Classes/Class_Selector_System.md), teleportation, or access-policy data. Updating from 1.5.0 to 1.5.1 requires no data migration for those systems; keep normal world backups, deploy the 1.5.1 jar, and let existing placed spawners tick naturally to tag future spawns.

## Help Menu Classes Subpage

- Added a [Help Menu Classes Subpage](../Classes/Help_Menu_Classes.md) under the in-game **H** help menu with four visible class buttons, up/down paging through sixteen class slots, class text pages, and left-arrow navigation back to the previous help page.
- Metalmancer and Deadeye are visible but disabled for unreleased Dungeon 2 content; eight placeholder slots are disabled as **COMING SOON!** until their class guides are defined.
- Help-menu class metadata now lives in a small client-side model, while editable titles and guide copy use the standard `en_us.json` language file.
- This is a client-only GUI/content change. It does not change server-data-storage methods for entity/block-entity data such as [Cosmic Mob Spawners](../Spawners/Spawner_Systems.md), doors/keys, [Rifts/RD](../Rifts/Rift_System_Guide.md), [classes](../Classes/Class_Selector_System.md), teleportation, or access-policy records. Updating from 1.5.0 to 1.5.1 requires no migration for those systems: keep a normal world backup, deploy the 1.5.1 jar, and let clients open the updated H menu.
