# D1 protected overflow recovery

Protected equipment no longer spills from a full cursor/menu close or forced armor unequip.
Any remainder stays in the owner's player save. Make room and use /d1 recover.

Recovery follows its dungeon inventory context: success retains it, failure removes only that
run's gear, and outside belongings cannot be imported into an active dungeon.
The claim processing budget is in CosmicDungeon.config under ItemProtection.

Optional player-save data; no new textures, item IDs or network version. Open-interface crash
and cross-file trade/repair transaction recovery remain separate work. Runtime TEST is pending.
