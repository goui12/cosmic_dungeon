# D1 in-game NPC placement and unique identities

- Vendor spawn/assign replaces the prior NPC for that stable profile; stale unloaded
  copies remain retired across normal saves/restarts. Failed insertion retains the old NPC.
- Beluzon defaults to a native Creaking. Explicit Tamsin binding replaces the prior Tamsin.
- Watson's in-game template placement maps into each D1 instance with its own identity.
  Reauthoring retires old-position copies while preserving run objectives and lifetime data.
- Adds independent NPC identity SavedData; existing role, placement and spawner formats remain.
- No world coordinate configuration, new art, registry IDs or network changes.

[Implementation, exact files, backup notes and pending TEST steps](../../ai/D1_IN_GAME_NPC_PLACEMENT_2026-09-20.md).
