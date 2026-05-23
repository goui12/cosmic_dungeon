# Class Restrictions & Inventory (Developer) — 1.4.8

## Class system behavior

Class service stores per-player class identity and applies class-based item restrictions.

- Class IDs include bogatyr, deadeye, dragoon, judicator, metalmancer, pyroclast, theurgist, venefex.
- Class-restricted item tags are enforced by class restriction events.
- Extra inventory and satchel systems provide class-adjacent storage behaviors.

## Developer-facing implications

- Class-locked chests are bound to a specific class key and reject mismatched players.
- Tag-driven restrictions mean item behavior is data-pack sensitive.
- Extra inventory UI/menu interactions should be included in class QA scenarios.

## Related systems

- Class selector block configuration.
- Dungeoneer commands and rank permissions.
- Metalmancer-specific commands and actions.
