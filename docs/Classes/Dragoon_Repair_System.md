# Dragoon Repair System Design Notes

## Current implemented behavior

- **Dragoon is the current anvil/repair-support class.** Server-side vanilla-use access allows Dragoons to use anvils and denies non-Dragoons unless they have developer bypass.
- **Repair Affinity is live** as a player-to-player Dragoon UI started with `/repair <player>` and accepted, denied, or cancelled with the `/repair` command family.
- The customer places the damaged repairable item into the shared interface, owns that slot, keeps the repaired item, and may offer an optional account-currency labor fee. The implemented UI does **not** use a physical payment slot.
- The Dragoon supplies matching repair materials from inventory. The customer confirms readiness, the Dragoon confirms repair, and final repair validation is server-authoritative. On success only, Dragoon materials are consumed, durability is restored by selected repair units, and the account-currency labor fee transfers through the currency account service. Cancel, close, range break, death, logout, or failed validation paths do not consume/transfer materials or currency and return the repair item according to the current customer-ownership rules.
- Elias sells Dragoon-themed repair materials used by the live player-to-player Repair Affinity flow; this is not a direct Elias shop repair service.
- Repair Affinity state is in memory only and does not add saved-data migration.
- The Theurgist brewing stand restriction remains separate and unchanged.

## Design direction / future work

Future repair work may add a direct Elias shop repair service, broader vendor pricing hooks, progression hooks, or D2 Deadeye exceptions, but those systems are design-only unless source code implements them.

## Related docs

- [Dragoon Help Guide](Dragoon_Help_Guide.md)
- [Dragoon Repair Affinity](Dragoon_Repair_Affinity.md)
- [Class Restrictions and Inventory](Class_Restrictions_and_Inventory.md)
