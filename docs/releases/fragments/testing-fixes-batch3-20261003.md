# Testing batch 3

- Only Developers can configure Cosmic Spawner equipment or apply authored presets.
- Spawns use the fixed top/N/W/S/E/NE/NW/SW/SE positions. Actual mob/passenger bounds
  cannot intersect solid blocks; overlapping entities are allowed. Blocked spawners
  show a red outline and developer warning, then retry automatically.
- Fresh mobs wait for nearby player detection or a direct attack. Older fighting mobs
  cannot wake later reinforcements from a distance. Living idle/fighting mobs count
  toward the existing cap, and passenger groups cannot overflow it.
- Existing placed spawner/preset fields are preserved; no manual replacement or
  spawner-format migration is needed. Gameplay acceptance remains to be performed.
