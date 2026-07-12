# Cosmic Dungeon Help Menu

The H-key help menu is a client-only Minecraft GUI framework for player-facing Cosmic Dungeon reference material. It does not send packets, authorize gameplay, or migrate server data.

## Assets

The large help menu uses `src/main/resources/assets/cosmicdungeon/textures/gui/menu/background_large.png` as a fixed **384x240 GUI-pixel** background. The artwork is drawn centered and unstretched.

`src/main/resources/assets/cosmicdungeon/textures/gui/menu/background.png` is legacy/small **256x192** artwork and is not a fallback for the large help GUI because the canvases have different layouts.

Button assets used by the framework:

- `text_button.png`, `text_button_hover.png`, `text_button_selected.png`, `text_button_disabled.png`: 128x24 text buttons.
- `up_button.png`, `up_button_hover.png`, `down_button.png`, `down_button_hover.png`: 28x28 scroll controls.
- `title.png`: 128x32 Cosmic Dungeon brand/title art.

No PNG/image edits are required for this refactor.

## Layout and Scrolling

The screen is organized as a two-pane form:

- Left pane: scrollable, data-driven navigation list sourced from `HelpMenuContent`.
- Right pane: page title and scrollable rich-text content viewport.

Both panes clamp scroll offsets so the player cannot scroll into negative space or blank trailing space. Mouse wheel input scrolls the pane under the cursor, and scissor clipping keeps navigation and page content inside their viewports.

## Content Model

Help content is centralized in structured page/block data. Supported blocks include headings, paragraphs, bullets, command rows, tips/notes, and spacers. The renderer uses Minecraft font wrapping rather than manual split-on-space wrapping.

Only dungeoneer-facing information belongs in the in-game help menu. Operator, developer, authoring, debug, and world-builder commands should remain in markdown documentation instead of the player help UI.

## Client/Server Boundary

The help menu is client-only. It is opened and closed by the H key and closes with Esc. It does not pause the game. The UI never grants access, class capabilities, currency, progression, teleportation, vendor unlocks, faction state, achievements, rift behavior, door/key access, or spawner behavior. Those systems remain server-authoritative.

## Related player and operator references

The in-game H menu intentionally summarizes only dungeoneer-facing behavior. Use these markdown references for deeper, linkable documentation:

- Classes: [Help Menu Classes](Classes/Help_Menu_Classes.md), [Class Restrictions & Inventory](Classes/Class_Restrictions_and_Inventory.md), [Dragoon](Classes/Dragoon_Help_Guide.md), [Judicator](Classes/Judicator_Help_Guide.md), and [Dragoon Repair System design notes](Classes/Dragoon_Repair_System.md).
- Economy and trading: [Economy & Currency](Economy/Economy_and_Currency.md), [Trading Guide](Trading/Trading_Guide.md), [Vendor](Vendor.md), and [Pricing Master List](Economy/Pricing_Master_List.md).
- Progression and factions: [Progression, Factions & Unlocks](Progression/Progression_Factions_and_Unlocks.md), [Faction Notes](Factions/Faction_General.md), and [NPC/Vendor Faction Notes](Factions/NPC_Vendor_Faction.md).
- Travel and milestones: [Potion of Companionship teleportation](Potions/Potion_of_Companionship_teleportation.md), [Travel Services Design Notes](Teleportation/Travel_Services_Design_Notes.md), and [Achievements & Advancements](Achievements/Achievements_and_Advancements.md).
- Commands and release context: [In-Game Commands](commands/In_Game_Commands.md) and [Update 1.5.1](releases/Update_1.5.1.md).

## Migration

No server saved data or gameplay storage structures are changed by the help menu framework. No 1.5.0 to 1.5.1 server data migration is required.

## Prompt 03 dungeoneer content pass

The H help menu now includes concise, player-facing coverage for:

- Vendor identities: Naton Whitlock, Elias Centvin, Eon Penrose, and Beatrix Farrow.
- Eon Penrose's Brewing Store, including live Dungeon 1 stock categories and major class restrictions.
- Travel via Beatrix's Campfire and Farrow's Chop, including successful-return consumption and Nostalgia Bait.
- Village Souls / story NPC summaries for Tamsin Vane and John Hamish Watson.
- Achievement status notes that keep **Nostalgia Bait** live and mark **The Tamsin Tax** as coming soon until a real Tamsin payment interface exists.

The menu intentionally omits developer/world-designer commands, saved-data schema details, vendor profile authoring, rift destination authoring, spawner authoring, rank/debug commands, and other internal setup details.
