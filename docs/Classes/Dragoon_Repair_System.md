# Dragoon Repair System Design Notes

## Current implemented behavior

- **Dragoon is the current anvil/repair-support class.** Server-side vanilla-use access allows Dragoons to use anvils and denies non-Dragoons unless they have developer bypass.
- **Repair Affinity is live** as a player-to-player Dragoon UI started with `/repair <player>` and accepted, denied, or cancelled with the `/repair` command family.
- The customer owns the damaged-item slot, keeps the repaired item, and may offer an optional account-currency labor fee. The Dragoon supplies matching repair materials, and final repair validation is server-authoritative.
- Elias sells Dragoon-themed repair materials used by the live player-to-player Repair Affinity flow; this is not a direct Elias shop repair service.
- Repair Affinity state is in memory only and does not add saved-data migration.
- The Theurgist brewing stand restriction remains separate and unchanged.

## Design direction / future work

Future repair work may add a direct Elias shop repair service, broader vendor pricing hooks, progression hooks, or D2 Deadeye exceptions, but those systems are design-only unless source code implements them.

## Related docs

- [Dragoon Help Guide](Dragoon_Help_Guide.md)
- [Dragoon Repair Affinity](Dragoon_Repair_Affinity.md)
- [Class Restrictions and Inventory](Class_Restrictions_and_Inventory.md)
