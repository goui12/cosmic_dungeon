# Farrow's Chop Teleportation

Farrow's Chop is the restored Village return-home food associated with Beatrix Farrow.

## Live behavior

- Raw Farrow's Chop is cooked through Beatrix's Campfire.
- While the player is inside an active dungeon instance, cooking Raw Farrow's Chop on Beatrix's Campfire records that player's exact instance dimension, coordinates, rotation, and run id, then returns the player to Main Village.
- Eating the bound Farrow's Chop returns its owner to that exact remembered location only while the same lifecycle remains active and the owner remains a member.
- Cooking consumes the raw item only after the Main Village teleport succeeds. Eating consumes the cooked item only after the validated dungeon return succeeds, unless creative-mode rules preserve the stack.
- Failed destination lookups or unsafe teleport attempts do not consume the chop.
- The successful campfire trip to Main Village grants **Nostalgia Bait**.

## Design notes

Bound chops are owner- and run-specific. Transferred, stale, cross-run, unsafe, or unbound chops cannot bypass lifecycle membership or teleport authorization.

For lower-level implementation and operator travel-service notes, see [Travel Services Design Notes](./Travel_Services_Design_Notes.md) and [Farrow's Chop and Beatrix's Campfire](../Travel/Farrows_Chop_and_Beatrixs_Campfire.md).
