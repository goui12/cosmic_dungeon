# D1 Batch23 - trade custody and restrictions

Trade offers and each owner's cursor now live beside their inventory in the player save.
Native saves capture the actual open menu. Closing, dying, disconnecting and dimension changes
return originals through owner-local recovery; full inventories do not spill offered items.
The server rechecks complete accepted images, participants, inventory scope, provenance and
approved prices. Ready locks all menu edits, including shift-click. Legacy independent world-file
copies remain readable and held for evidence review, never automatically awarded/deleted.

Optional trade_custody_v1 player-root field; existing registry IDs and protocol5 unchanged.
No spawner/world format changes. Datagen not applicable. 80 new native NBT custody checks;
1,888 cumulative offline checks, two config round trips and Java21 build passed.
Batch24 completes cross-file trade commit. Licensed dedicated/integrated menu/lifecycle and
interrupted-save acceptance remain pending; no runtime or deployment performed.
