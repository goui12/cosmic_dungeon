# Class Restrictions & Inventory (Developer) — 1.4.8

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
