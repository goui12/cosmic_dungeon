# D1 follow-up: confirmed vendor sales and successful-run kills

- Show server-issued final-stack quotes before vendor sales, including explicit zero-Trace surrender.
- Reject stale/replayed quotes and changed items, prices, vendor sessions, ownership or capacity.
- Exclude zero-value stacks from Sell All; keep missing/restricted-price items.
- Add configurable quote expiry under Economy in CosmicDungeon.config.
- Record hostile final blows within each D1 run; commit lifetime kills only on successful completion.
- Preserve saved IDs and existing totals through an optional successful_kills field.

Client/server jars must match. No new textures, resource generation, spawner migration,
authored-world edits or runtime dependencies. Build/offline checks do not establish
multiplayer or crash-atomic behavior. D1 remains incomplete; no deployment occurred.
See ../../ai/D1_IMPLEMENTATION_20260916.md for the September 17 handoff and manual checks.
