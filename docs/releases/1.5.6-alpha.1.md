# Cosmic Dungeon 1.5.6-alpha.1

Batch 4: shared mercenary skills.

- Class-specific skill levels appear when hovering a companion in the inventory group HUD.
- Skills start at level 1 and need 1, then 2, then 3 more successful actions to level up.
  Level 5 therefore requires 10 successes. Group chat announces each level gained.
- Progress survives companion death, paid/free recovery, unload and server reload.
  A new dungeon instance starts each companion at level 1.
- Existing successful wolf summons and splash/cloud healing, effective buffs and immediate
  potion damage now earn progress. Failed, immune or unchanged effects earn nothing.
  A potion contributes at most one success per category regardless of affected targets.
- Later batches connect the remaining class mechanics and their level-based bonuses.

Requires matching Cosmic Dungeon client/server versions (network protocol 15).
Install the matching optional loading-screen helper on clients only.
Existing worlds load with no manual save migration. Existing authored items are unchanged.
