# Personal slot chests

New Dungeon 1 runs bind the starting and later slot-schematic class chests to each
slot's original player UUID. Nearby chests show the owner's name; non-owners receive a
clear denial, including other players of the same class. Server menu checks and hopper
restrictions enforce ownership.

Authored contents remain unchanged. Optional ownership metadata survives save/reload and
restart; unbound legacy/template chests retain their existing behavior. Fresh runs acquire
the assignments automatically. Existing active runs are not retroactively reassigned.

See [task and validation notes](../../ai/tasks/slot-chest-ownership-20260926.md).
