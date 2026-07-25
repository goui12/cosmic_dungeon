# Farrow's Chop and Beatrix's Campfire

Beatrix Farrow's restored Village food-and-travel loop centers on a special campfire and a return-home meal.

## Player behavior

- Raw Farrow's Chop is cooked through Beatrix's Campfire, not through ordinary campfire recipes.
- Cooking Raw Farrow's Chop while inside the player's active dungeon binds the cooked chop to the exact location and returns the player to the Main Village destination, preferring `main_village`, then `village`, then `Main Village`.
- Dungeon and outside inventories remain separate for the round trip: cooking escrows the dungeon inventory, returning restores it, and ending the run while outside retains only the outside inventory. Currency transactions are unavailable during the outside visit.
- Eating that cooked chop returns its owner to the recorded active-instance position. The chop is consumed only after a successful server-side return.
- If the destination is missing, invalid, unloaded, unsafe, or blocked, the chop is not consumed and the player receives a failure message.
- A successful campfire trip grants **Nostalgia Bait**: "Cook Farrow's Chop to return to the Main Village."

Flavor line: _Smells like home. Costs like nostalgia._

## Destination contract

The campfire departure resolves normal rift destination data through `DefaultRiftDestinations`. The cooked item stores an owner-bound return component containing the active run id and exact physical instance location. Eating revalidates the active run, membership, dimension ownership, inventory escrow, and safety on the server; it never accepts a client-selected instance.

## Beatrix Campfire compatibility

Beatrix's Campfire uses the vanilla campfire block entity through NeoForge block-entity compatibility registration. It keeps normal campfire placement, lighting, extinguishing, waterlogging, smoke, cooking, breaking, and chunk-reload behavior while adding only the lit-campfire Raw Farrow's Chop conversion described above. The block and item models are generated/packaged so the inventory item uses the existing lit 3D campfire model rather than a missing flat item model.


## Beatrix vendor availability

Beatrix Farrow now has the live D1 vendor profile `cosmicdungeon:d1/food_vendor`. It sells Raw Farrow's Chop and Farrow's Chop for 2 Crowns each with a one-purchase-per-player cap, plus Beatrix's Campfire for 5 Trace and ordinary raw foods for camp preparation.
