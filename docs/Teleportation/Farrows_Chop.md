# Farrow's Chop Teleportation

Farrow's Chop is the restored Village return-home food associated with Beatrix Farrow.

## Live behavior

- Raw Farrow's Chop is cooked through Beatrix's Campfire.
- Eating Farrow's Chop attempts to return the player to the Main Village.
- The item is consumed only after a successful server-side teleport, unless creative-mode rules preserve the stack.
- Failed destination lookups or unsafe teleport attempts do not consume the chop.
- A successful teleport grants **Nostalgia Bait**.

## Design notes

Farrow's Chop carries a faint interdimensional charge: it is food with a memory of home. Dungeoneer-facing help should describe the safe return behavior, not rift destination authoring or setup commands.

For lower-level implementation and operator travel-service notes, see [Travel Services Design Notes](./Travel_Services_Design_Notes.md) and [Farrow's Chop and Beatrix's Campfire](../Travel/Farrows_Chop_and_Beatrixs_Campfire.md).
