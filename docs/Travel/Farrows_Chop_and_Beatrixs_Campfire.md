# Farrow's Chop and Beatrix's Campfire

Beatrix Farrow's restored Village food-and-travel loop centers on a special campfire and a return-home meal.

## Player behavior

- Raw Farrow's Chop is cooked through Beatrix's Campfire, not through ordinary campfire recipes.
- Farrow's Chop is consumed only after a successful server-side teleport.
- A successful use returns the dungeoneer to the Main Village rift destination, preferring `main_village`, then `village`, then `Main Village`.
- If the destination is missing, invalid, unloaded, unsafe, or blocked, the chop is not consumed and the player receives a failure message.
- A successful return grants **Nostalgia Bait**: "Teleport back to the Main Village using Farrow's Chop."

Flavor line: _Smells like home. Costs like nostalgia._

## Destination contract

Farrow's Chop resolves the normal rift destination data through `DefaultRiftDestinations`, preferring `main_village`, then `village`, then `Main Village`. The destination lives in the existing `cosmicdungeon_rifts_v2` saved data alongside other rift destinations; no parallel registry or extra saved-data file is used. Server-start seeding creates `main_village` only if missing, and item use does not create or overwrite destinations.

## Beatrix Campfire compatibility

Beatrix's Campfire uses the vanilla campfire block entity through NeoForge block-entity compatibility registration. It keeps normal campfire placement, lighting, extinguishing, waterlogging, smoke, cooking, breaking, and chunk-reload behavior while adding only the lit-campfire Raw Farrow's Chop conversion described above. The block and item models are generated/packaged so the inventory item uses the existing lit 3D campfire model rather than a missing flat item model.


## Beatrix vendor availability

Beatrix Farrow now has the live D1 vendor profile `cosmicdungeon:d1/food_vendor`. It sells Raw Farrow's Chop and Farrow's Chop for 2 Crowns each with a one-purchase-per-player cap, plus Beatrix's Campfire for 5 Trace and ordinary raw foods for camp preparation.
