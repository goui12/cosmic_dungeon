# Cosmic Dungeon 1.7.13 Alpha 1

Active skills now have saved Minecraft Controls bindings: Alt+1 Patrol, Alt+2 Stay, Alt+3 Feed Pack, and Alt+4 Resurrect by default. Rebind or unbind them under Cosmic Dungeon. The actual binding appears in gray beside each skill and in its foreground tooltip; narrow rows use two lines.

Shortcuts work from the world or player inventory, once per physical press. Chat, other menus, carried items, dragging and ambiguous matching bindings block skill activation. Resurrect opens member selection without choosing a target or bypassing cooldown. Potion crafting remains manual.

Uses the existing protocol30 action services and native options persistence; no world/player save changes. Includes input, capability-routing and dynamic-label regression tests. Licensed Controls, layout and multiplayer acceptance remain TESTING. Native Controls can highlight shared base keys even when modifier chords differ.
Alpha publication only; no TEST-server or local-client deployment.
