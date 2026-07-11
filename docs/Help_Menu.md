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

## Migration

No server saved data or gameplay storage structures are changed by the help menu framework. No 1.5.0 to 1.5.1 server data migration is required.
