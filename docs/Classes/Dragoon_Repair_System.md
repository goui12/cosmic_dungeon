# Dragoon Repair System Design Notes

## Current implemented behavior

- **Dragoon is the current anvil/repair-support class.** Server-side vanilla-use access allows Dragoons to use anvils and denies non-Dragoons unless they have developer bypass.
- The prerequisite custom repair-material items now exist as registered items: `cosmicdungeon:leather_patch`, `cosmicdungeon:chain_link`, and `cosmicdungeon:netherite_repair_fragment`. They are grouped with vanilla repair materials in the `cosmicdungeon:dragoon_repair_materials` item tag for future repair logic.
- This implementation does **not** include a custom Dragoon Repair Affinity UI, repair station, vendor pricing hook, or additional saved data.
- The Theurgist brewing stand restriction remains separate and unchanged.

## Design direction / future work

Dragoon repair design should build on the class identity that Dragoons stabilize and restore dungeon-worn gear. Future work may add a dedicated repair-affinity interface, vendor interactions, or progression hooks, but those systems must be implemented server-authoritatively before appearing in player help as live mechanics.

## Related docs

- [Dragoon Help Guide](Dragoon_Help_Guide.md)
- [Class Restrictions and Inventory](Class_Restrictions_and_Inventory.md)
