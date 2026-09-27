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

Dragoon is the server-authoritative vanilla anvil/repair-support class. `AccessPolicy.allowClassGatedVanillaUse` allows Dragoons to use anvils, denies non-Dragoons with a clear message, and preserves developer bypass. Theurgist brewing stand access remains a separate restriction and is unchanged. The custom Dragoon Repair Affinity UI is live as a player-to-player Dragoon service; see [Dragoon Repair System](Dragoon_Repair_System.md).

## Personal slot chests

For new Dungeon 1 runs, each occupied slot's class chests belong to the player assigned
that slot at entry. This applies to the starting-room schematic and all five later chest
groups. Players of the same class still have separate ownership.

Nearby chests display their owner's name. A non-owner receives a red explanation instead
of a menu. The server checks the UUID when opening and while the menu remains open;
owned chests also reject automated insertion/extraction. Developer status does not bypass
another player's slot ownership. Existing class restrictions still apply to the owner.

Ownership persists through saving, chunk reloads and restarts. Leaving the party does not
shift ownership to someone else. New runs bind fresh pasted chests to their own roster.
Name labels fit within one chest column and use ordinary depth testing, with no floating
entities or extra polling.

Chest items, names, quantities and item components remain authored as-is. Optional
CosmicSlotOwner metadata is separate from inventory data; only that metadata is synchronized
for labels. Existing unbound/template chests retain their previous class/developer rules.
Already-started runs from before this update need a new run to receive these assignments;
the update does not guess retroactive ownership or modify source schematic files.

See the [task and validation notes](../ai/tasks/slot-chest-ownership-20260926.md).
