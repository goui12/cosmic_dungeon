# Cosmic Dungeon H guide

Press **H** for the current player guide. The screen occupies about 80% of the available scaled width and height, with an anchored title, left directory and wrapping information pane. It does not pause the game.

Use the directory or underlined inline links for navigation. **Back** / **Alt+Left** restores the previous guide page. **H** / **Esc** closes it. Wheel input scrolls the pane beneath the pointer; **Page Up / Page Down** scrolls the focused pane (information pane by default). **Tab / Shift+Tab** moves focus; **Enter** opens the focused link. The class-name link in inventory opens the corresponding class page.

The directory retains the existing six root entries: Get Started, Dungeons, Party, Classes, General and References. Its 33 reachable pages retain their IDs and hierarchy. Six active class pages are listed; Metalmancer and Deadeye remain unavailable and are not added to the player directory.

## Content and artwork

The canonical player-facing sources are [PLAYER_GUIDE.md](PLAYER_GUIDE.md), the class guides, and structured page/block content in `HelpMenuContent.java`. The H menu retains the full NPC backstories and distinguishes spectral Blooms from Lesser Blooms. Update these sources together when character controls, classes or system behavior changes.

Headings, paragraphs, bullets, command rows and notes wrap using the current pane width. Inline topic links navigate only within the guide. Underlined skill references show the player's saved binding on hover/focus; they never activate that skill. Commands are reference text and never execute through a link.

Read-only demonstration item tiles show actual item tooltips. They are illustrations, not container slots: clicking, dragging, number keys and shift-click cannot grant or transfer them. Examples use real registered items and potion components; an unbound sample key cannot open a door. Feed Pack's supplementary food list comes from the synchronized wolf-food tag plus bones and refreshes after tag/session changes. Retired Brewing Supplies, Requests and paid wolf-skill instructions are not injected.

Category artwork comes from the explicit help-art mapping and Batch 18's 16:9 masters. Rendering crops it responsively while preserving text contrast and class emblems. Artwork is atmosphere, not a promise of an unavailable class, recipe, encounter or location. See [artwork provenance](../art/help/README.md).

## Boundaries and references

The guide is client-only and read-only. It sends no gameplay actions, changes no inventory, and grants no class, currency, progression, teleport, vendor, door or spawner permission. No server save migration is needed.

The [player guide](PLAYER_GUIDE.md) covers party/readiness/sharing, gear/drop/trade rules, crafting and fuel, resurrection, every active class, hotkeys, progression, vendors, travel and achievements. [Class index](Classes/Help_Menu_Classes.md) links individual guides. Full NPC lore remains in the H directory and [NPC documentation](NPCs/).

Developer commands and spawner-authoring controls stay outside the H guide. Use the clearly labeled [Developer-only reference](PLAYER_GUIDE.md#developer-only-reference--outside-the-h-guide), [Spawner Systems](Spawners/Spawner_Systems.md) and [Spawner Commands and Features](Spawner_Commands_Features.md).

Automated layout/content checks do not prove licensed in-game visual acceptance. Test narrow and wide windows, GUI scales, long NPC pages, link focus/history, scrolling and topmost item tooltips in the real client when manual QA is authorized.
