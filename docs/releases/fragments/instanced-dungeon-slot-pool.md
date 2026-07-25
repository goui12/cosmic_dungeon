# Instanced dungeon slot pool

- Added ten persistent logical dungeon slots so simultaneous groups—including groups running the same dungeon profile—receive isolated physical copies. Dungeon 1's primary and Nether worlds share one logical slot.
- Active slot assignments persist server restart. Completion refreshes the slot from its template before release; failed refreshes quarantine the slot.
- Dungeon rifts and the developer `/world` route now resolve template destinations against active lifecycle membership. Developers outside a run retain template access, while ordinary non-members cannot enter templates or unrelated instances.
- Beatrix's Campfire now binds cooked Farrow's Chop to the owner's exact active-instance location while sending them to Main Village. Eating the owner-bound chop returns there after server-side run, membership, dimension, and safety validation.
- Existing dungeon-run SavedData gains an optional slot field with startup migration for active legacy records. Rift destination, region, door/key, Cosmic Spawner block-entity/preset, currency, faction, progression, class, and achievement schemas remain unchanged. Back up the full world before upgrading; no placed spawner recreation is required.
