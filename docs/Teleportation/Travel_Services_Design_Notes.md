# Travel Services Design Notes

## Current implemented travel systems

- Potion of Companionship supports dungeon-party target teleportation with server-side validation and cooldown.
- Rift systems are documented separately as the implemented rift/RD travel mechanics.

## Future planning

Broader travel-service notes are future expansion planning unless source code explicitly implements them. Do not present future NPC travel vendors, faction travel perks, or hub route services as live player mechanics in the in-game help menu.

## Related docs

- [Potion of Companionship Teleportation](../Potions/Potion_of_Companionship_teleportation.md)
- [Rift System Guide](../Rifts/Rift_System_Guide.md)


## Farrow's Chop return-home destination

Cooking Raw Farrow's Chop inside an active dungeon uses the normal rift destination `main_village` for the trip out and binds the cooked item to the owner's exact instance location. Eating the bound chop performs the validated return trip. The Village destination is auto-created only if missing, stored in existing rift destination saved data, and never overwrites developer edits. Use `/rd info main_village` to inspect it and `/rd move main_village` to update it in-game.
