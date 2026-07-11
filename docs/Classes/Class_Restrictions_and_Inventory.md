# Class Restrictions & Inventory (Developer) — 1.5

## Class system behavior

Class service stores per-player class identity and applies class-based item restrictions.

- Class IDs include bogatyr, deadeye, dragoon, judicator, metalmancer, pyroclast, theurgist, venefex.
- Class-attuned equipment and intrinsic class-bound utility items are enforced through the central class item access policy.
- Extra inventory and satchel systems provide Metalmancer storage behaviors; the Satchel of Samples is intrinsically class-bound to Metalmancer without dungeon, tier, or Trace metadata.

## Developer-facing implications

- Class-locked chests are bound to a specific class key and reject mismatched players.
- Stack attunement metadata takes priority over intrinsic item binding when both are present; dungeon, tier, and Trace metadata remain separate from use permission.
- Extra inventory UI/menu interactions should be included in class QA scenarios.

## Related systems

- Class selector block configuration.
- Dungeoneer commands and rank permissions.
- Metalmancer-specific commands and actions.

## Class-attuned equipment

Developer-authored class gear stores class, dungeon, tier, and Trace value metadata on the item stack. The stored class controls who can use or wear guarded equipment; dungeon, tier, and Trace value remain progression/economy metadata and do not change access permission. Class-attuned banners are excluded from guarded-equipment restrictions so D1 Plant Flags banners remain placeable.

## Related systems

- [Economy & Currency](../Economy/Economy_and_Currency.md) for Trace value context.
- [Vendor](../Vendor.md) for sell-value behavior.
- [Achievements & Advancements](../Achievements/Achievements_and_Advancements.md) for Plant Flags banner tracking.

## Changelog

- **1.5:** Added class-item attunement metadata, dynamic class tooltips, server-side equipment restrictions, Metalmancer policy unification, Plant Flags banner carve-out, and class-attuned vendor sell values.


## Dragoon anvil access

Dragoon is the server-authoritative vanilla anvil/repair-support class. `AccessPolicy.allowClassGatedVanillaUse` allows Dragoons to use anvils, denies non-Dragoons with a clear message, and preserves developer bypass. Theurgist brewing stand access remains a separate restriction and is unchanged. No custom Dragoon Repair Affinity UI is implemented yet; see [Dragoon Repair System](Dragoon_Repair_System.md).
