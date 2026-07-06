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

## Vendor mob shells

- [`/vendor spawn <profileId>`](../commands/In_Game_Commands.md#vendor-15-progression--faction-access) remains backward-compatible and spawns a villager by default. Operators can now pass a mob type, such as `/vendor spawn <profileId> horse`, to use another mob as the vendor shell. See [Vendor entity shells](../Vendor.md#vendor-entity-shells).
- Vendor shells now render the profile display name above the mob in bold bright neon green, are always marked invulnerable, and have mob AI disabled so they stand still and do not roam.
- The interaction and purchase/sell validation paths are still server-authoritative and keyed to the assigned entity id/profile, so [vendor access gates](../Vendor.md#access-gates), [class restrictions](../Classes/Class_Restrictions_and_Inventory.md), [teleportation/rifts](../Rifts/Rift_System_Guide.md), and [Cosmic Mob Spawners](../Spawners/Spawner_Systems.md) do not share state with this command change.
- This feature only stores the existing `cosmicdungeon.vendor_profile_id` string on vendor entities and does not modify server-data-storage methods for block entities or existing entity systems such as mob spawners, doors/keys, rifts/RD, class selector data, teleportation, or access-policy records. Updating from 1.5.0 to 1.5.1 requires no migration for those systems: keep a normal world backup, deploy the 1.5.1 jar, and respawn/reassign only the vendors you want to visually change from villager shells to another mob type.

## D1 Gritch vendor profile

- Added [`cosmicdungeon:d1/d1_nether_gritch_of_the_barter_pit`](../Vendor.md#d1-vendor-profiles), displayed as **Gritch of the Barter Pit**, with two buy offers: one golden carrot for `1000` Trace and one glistering melon slice for `1000` Trace.
- This is a datapack vendor-profile addition under the existing vendor reload/list/spawn path. It does not change access-policy records, class systems, teleportation/rifts, Cosmic Mob Spawner entity or block-entity storage, doors/keys, or server-data-storage methods. Updating from 1.5.0 to 1.5.1 requires no data migration for those systems: keep a normal world backup, deploy the 1.5.1 jar, and use `/vendor reload` or restart the server so the new profile is loaded.


## Infinite Dispenser mob eggs

- [Infinite Dispensers](../Blocks/Block_Interaction_Reference.md#infinite-dispenser) now recognize every `SpawnEggItem` as shootable even when the generated `cosmicdungeon:infinite_shootables` item tag exists for other projectiles. This lets operators fire vanilla eggs such as zombie spawn eggs and modded mob eggs without maintaining duplicate tag entries.
- The spawn-egg path is server-authoritative and reuses the existing block-entity inventory/menu path, so it does not add packets, registries, saved fields, or storage migrations. [Cosmic Mob Spawners](../Spawners/Spawner_Systems.md), doors/keys, [Rifts/RD](../Rifts/Rift_System_Guide.md), [classes](../Classes/Class_Selector_System.md), teleportation, and access-policy records are unchanged. To update safely from 1.5.0 to 1.5.1, keep a normal world backup, deploy the 1.5.1 jar, and leave existing block entities in place; no data fixer or manual conversion is required.


## Cosmic Spawner intrinsic-drop refactor

- [Cosmic Mob Spawners](../Spawners/Spawner_Systems.md#intrinsic-loot-table-drops-and-per-spawner-rules) now display active mob loot-table drops in [`/spawner info`](../Spawner_Commands_Features.md#intrinsic-drops-in-spawner-info), including base spawners that only have `SpawnerEntityId` and no custom preset. The display comes from active server loot-table resources/registry data, not a hardcoded vanilla list.
- Developers can set final per-spawner intrinsic chances with `/spawner drop intrinsic <item> <0.0-1.0>`, remove them with the preferred `/spawner drop intrinsic default <item>`, or use the legacy `/spawner drop intrinsic clear <item>` alias. Chat rows expose `[+]`, `[-]`, and `[Default]` controls for the same server-authorized command path.
- Per-spawner rules can override default loot-table items or add custom intrinsic items such as `minecraft:pink_wool` to a zombie spawner. These rules do not modify global vanilla/datapack loot tables.
- Configured intrinsic rules are copied onto spawned entities and applied server-side on death, so custom drops and overrides continue to work for boss one-shot spawns after the spawner block self-destructs. Older already-spawned mobs without entity-attached rules still fall back to the existing `cosmic_spawner_<x>_<y>_<z>` block lookup when possible.
- Existing 1.5.0 placed/configured Cosmic Spawners are preserved. Missing `CosmicSpawnerDataVersion` is treated as legacy 1.5.0 data, loaded lazily, and saved with the current version after normal chunk save. Old `SpawnerPreset/intrinsicDrops` entries are preserved as final configured chances and re-evaluated against active loot tables as overrides or custom-added rows.
- Preset JSON files in `<server_root>/cosmicdungeon/spawner_presets/` are read safely across the 1.5.1 transition. Older `formatVersion: 1` files are logged for upgrade on next save, and nested legacy `spawnerPresetNbt` intrinsic drops remain supported. Keep normal world backups before updating, but developers do not need to redo placed Cosmic Spawners.
- Complex/conditional loot-table chances may display as `complex`, `conditional`, `rare/player/looting`, or similar rather than fake exact percentages. Use explicit per-spawner overrides when an exact final chance is required.

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
- [`/spawner info`](../Spawner_Commands_Features.md#spawner-info-display) now shows the configured custom mob name near the top of the panel as `Mob Name`, matching names set by `/spawner name set <display name>` and using `unnamed` when cleared. This is a chat-output-only refactor and does not change Cosmic Mob Spawner block-entity storage, preset JSON formats, entity save data, rift/RD data, door/key data, class selector data, teleportation data, or access-policy records. Updating from 1.5.0 to 1.5.1 requires no spawner data migration for this display fix; keep normal backups and deploy the 1.5.1 jar.
- [Cosmic Mob Spawners](../Spawners/Spawner_Systems.md) now tag every newly spawned mob with the shared `cosmic_spawner_<x>_<y>_<z>` marker used by Group Split, including base-entity-only spawners with no optional preset, boss one-shot flag, or mob cap.
- These 1.5.1 trade GUI/finalization fixes are runtime menu/network behavior and do not change currency saved-data schemas. This update also keeps runtime/server-player currency and runtime SpawnData state only; it does not add required saved fields or invalidate existing entity/block-entity storage for [Cosmic Mob Spawners](../Spawners/Spawner_Systems.md), [Rifts](../Rifts/Rift_System_Guide.md), doors/keys, [classes](../Classes/Class_Selector_System.md), teleportation, or access-policy data. Updating from 1.5.0 to 1.5.1 requires no data migration for those systems; keep normal world backups, deploy the 1.5.1 jar, and let existing placed spawners tick naturally to tag future spawns.

## Help Menu Classes Subpage

- Added a [Help Menu Classes Subpage](../Classes/Help_Menu_Classes.md) under the in-game **H** help menu with four visible class buttons, up/down paging through sixteen class slots, class text pages, and left-arrow navigation back to the previous help page.
- Metalmancer and Deadeye are visible but disabled for unreleased Dungeon 2 content; eight placeholder slots are disabled as **COMING SOON!** until their class guides are defined.
- Help-menu class metadata now lives in a small client-side model, while editable titles and guide copy use the standard `en_us.json` language file.
- This is a client-only GUI/content change. It does not change server-data-storage methods for entity/block-entity data such as [Cosmic Mob Spawners](../Spawners/Spawner_Systems.md), doors/keys, [Rifts/RD](../Rifts/Rift_System_Guide.md), [classes](../Classes/Class_Selector_System.md), teleportation, or access-policy records. Updating from 1.5.0 to 1.5.1 requires no migration for those systems: keep a normal world backup, deploy the 1.5.1 jar, and let clients open the updated H menu.

## Cosmic Spawner summary HUD

- [`/spawner showlabels`](../Spawner_Commands_Features.md#spawner-showlabels) now enables a developer-only bottom-right HUD panel while looking at a [Cosmic Mob Spawner](../Spawners/Spawner_Systems.md), replacing the old teal in-world mob-type label. The panel shows mob type, configured mob name, per-spawner cap, and delay range, then disappears when the developer looks away. Developers can tune the panel locally through client config values for position, opacity, and pixel offsets.
- This change reuses the existing developer authorization and client sync path for the showlabels toggle. It does not change server-data-storage methods for entity/block-entity data such as Cosmic Mob Spawners, doors/keys, [Rifts/RD](../Rifts/Rift_System_Guide.md), [classes](../Classes/Class_Selector_System.md), teleportation, or access-policy records. Updating from 1.5.0 to 1.5.1 requires no migration for these systems: keep a normal world backup, deploy the 1.5.1 jar, and let clients receive the updated HUD behavior after `/spawner showlabels` is enabled.

## Phase 6 Cosmic Spawner Intrinsic Drop Counts

Cosmic Mob Spawner intrinsic drops now use lossless rule rows with a stable rule id, item id, final chance, stack count, and rule kind. Use `/spawner drop intrinsic <namespace:item> <chance 0.0-1.0> [quantity]`; omitted quantity defaults to `1`, and counts are clamped to the safe 1-64 range.

Multiple independent rules for the same item are supported. For example, potato rules at `1.0 1`, `0.5 2`, and `0.1 5` roll separately, so one kill can drop 1, 3, 6, or 8 potatoes depending on which rows succeed. Chance always applies to that configured stack, not to the combined item total.

`/spawner info` shows configured rows as `Chance: <percent> [+] [-] Count: <n> [+] [-] [Default]`. The chance and count buttons target the stable rule id for that row, so duplicate item rows are safe. Clicking `[Default]` on a configured row removes only that rule; `/spawner drop intrinsic default <item>` and the backward-compatible `/spawner drop intrinsic clear <item>` remove all configured rules for that item and restore vanilla/datapack default behavior. `/spawner drop intrinsic add <item> <chance> [quantity]` always adds a new independent rule, while `/spawner drop intrinsic <item> <chance> [quantity]` upserts by item/count for convenience.

Save migration is lazy and backward-compatible: 1.5.0 data, block-entity data versions 150/151, preset versions 2/3, preset file formats 1/2, old `intrinsicDrops` maps, and Phase-5 `intrinsicDropRules` keyed by item id are read as one count-1 rule per old item chance. New block entities save as data version 152, presets save as preset version 4, and preset JSON saves as format version 3 while preserving the full `spawnerPresetNbt` rule list. The legacy `intrinsicDrops` mirror remains compatibility-only and cannot represent duplicate rows.

## Phase 7 Cosmic Spawner Spawn Defaults

Cosmic Mob Spawners now apply a small server-side spawn-default pass only to mobs carrying that spawner's `cosmic_spawner_<x>_<y>_<z>` marker. Vanilla Wardens spawned by Cosmic Spawners receive a 1200-tick `minecraft:dig_cooldown` brain memory so they do not immediately dig after spawning.

Vanilla `minecraft:slime` and `minecraft:magma_cube` spawned by Cosmic Spawners are raised to the max standard size 4 using the entity API so health and dimensions refresh correctly. Larger entities are not shrunk, and naturally spawned mobs are unaffected.

This phase does not change Cosmic Spawner block-entity storage, preset JSON/NBT storage, intrinsic drop rule storage, rift/RD data, door/key data, access policy, class, teleportation, or dungeon reset data. The only new marker is per spawned entity: `cosmicdungeon:spawner_spawn_defaults_applied_version`, used to avoid reapplying the defaults forever.
