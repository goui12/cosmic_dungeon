# D1 protected equipment lifecycle

Class-issued/no-drop equipment now stays in the vanilla inventory through death and respawn,
including vanishing-curse removal. Normal dungeon failure still restores the entry inventory.

Player-dropped equipment retains its owner. Other players, vanilla collecting mobs and hoppers
cannot take it. Dispensers/droppers cannot eject bound items. Ordinary droppable gear retains
environmental loss; unowned encounter loot and ordinary trap supplies remain usable.

No new save format, network version, item IDs or textures. Full cursor/scripted overflow recovery
continues in the next batch. Third-party automation adapters need explicit verification.

Java21 build/offline checks pass; licensed TEST gameplay and actual mixin application remain pending.
