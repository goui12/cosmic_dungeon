# Help Menu Classes Subpage

The in-game **H** help menu includes a [Classes](./Class_Selector_System.md) subpage for browsing player-class help without contacting the server.

## Navigation

- Press **H** to open the Cosmic Dungeon help menu, then select **Classes**.
- The top-left left-arrow returns from **Classes** to the main help page.
- The top-right up-arrow and bottom-right down-arrow cycle through the sixteen class slots four buttons at a time.
- Selecting an enabled class opens a text-only class guide page with its own top-left left-arrow back to **Classes**.

## Class slot behavior

- The first eight class slots are the currently-authored class entries: [Theurgist](./Theurgist_Help_Guide.md), [Pyroclast](./Pyroclast_Help_Guide.md), [Bogatyr](./Bogatyr_Help_Guide.md), [Dragoon](./Dragoon_Help_Guide.md), [Venefex](./Venefex_Help_Guide.md), [Judicator](./Judicator_Help_Guide.md), [Metalmancer](./Metalmancer_Help_Guide.md), and [Deadeye](./Deadeye_Help_Guide.md).
- **Metalmancer** and **Deadeye** render disabled because their Dungeon 2 class content is not released yet.
- The remaining eight slots render as disabled **COMING SOON!** buttons until their class guides are authored.

## Compatibility and update notes

This feature is client-side help-menu presentation only. It does not add packets, registries, saved fields, or server-authoritative state, and it does not modify storage for [Cosmic Mob Spawners](../Spawners/Spawner_Systems.md), [Rifts/RD](../Rifts/Rift_System_Guide.md), doors/keys, [class selector data](./Class_Selector_System.md), teleportation, or access-policy records.

Updating a 1.5.0 server to 1.5.1 for this help-menu feature requires no data migration: keep a normal world backup, deploy the 1.5.1 jar, and let clients open the updated H menu.

## Text and metadata maintenance

Help-menu page ordering, class release flags, and slot paging live in the small client-side `HelpMenuContent` model. Player-facing titles and body text use the normal Minecraft language file at `src/main/resources/assets/cosmicdungeon/lang/en_us.json`, which is the standard place for mod UI strings and future translations. Use dedicated data JSON files only when content must be data-driven like recipes, advancements, loot tables, tags, or datapack/server-loaded gameplay definitions; static client GUI copy should stay in language JSON so resource packs and translations can override it cleanly.
