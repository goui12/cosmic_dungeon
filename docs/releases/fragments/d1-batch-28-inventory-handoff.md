# D1 inventory handoff and startup rollback

Dungeon cleanup now preserves a saved owner decision before retiring inventory escrow.
Success retains loot and stores outside belongings; failed runs recover outside inventory.
Stored-item claims preserve exact overflow and use native player receipts against duplication.
Incomplete entry retains full pre-entry inventories and rolls back the party after restart.

Existing save IDs remain; optional journal/startup fields and player receipts require a complete
world backup before upgrade and a matching complete backup for downgrade. Older unreceipted
recovery records are held for review. No authored worlds, spawners or item assets changed.

Validated with3,836 offline checks, two config round trips and Java21 build.
Native gameplay, save interruption and licensed multiplayer testing remain pending.
Watson's earlier Bloom/reward outcome transaction remains separate follow-up work.
See docs/ai/D1_BATCH_28.md for compatibility and exact TEST steps.
