# Potion of Companionship (Teleportation)

The Potion of Companionship is a light-pink utility potion that helps dungeon parties regroup during an active dungeon lifecycle.

## Behavior

- Uses vanilla-style potion drinking timing, animation, sound, and an empty glass bottle return.
- Can only be consumed by players who are part of an active dungeon run created through the class selector lifecycle.
- Opens a client GUI titled `Teleport to a Dungeoneer:` after drinking.
- Lists only online companions from the same active dungeon run, excluding the drinker.
- Selecting a listed dungeoneer teleports the drinker to that player.
- Applies a visible five-minute `Teleport Cooldown` mob effect to the drinker.

## Failure Messages

- If the player is not in an active dungeon group, the potion is not consumed and shows: `You’re not part of an active dungeon group`.
- If the player is cooling down, the potion is not consumed and shows: `Teleportation on cooldown: _____ minutes ___ Seconds`.

## Availability

- Creative inventory: vanilla `Food & Drinks` tab and Cosmic Dungeon `Dungeon Items` tab.
- Vendor: Beluzon Everly, the D1 Save Teleport NPC, sells one potion for one Seal.

## Travel-service planning boundary

Broader travel services are tracked as future planning in [Travel Services Design Notes](../Teleportation/Travel_Services_Design_Notes.md). The live mechanic documented here remains the Potion of Companionship dungeon-party teleport with cooldown.
