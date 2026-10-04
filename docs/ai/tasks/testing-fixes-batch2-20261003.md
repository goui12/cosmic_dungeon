# Testing fixes, batch 2 — 2026-10-03

Cameron explicitly authorized batch 2 after batch 1. This task owns exactly:
1. Trade order: Their Offer, My Offer, My Balance, My Inventory.
2. Advancement descriptions masked as ???????? until earned.
3. Death-screen forfeit and automatic living outside recovery when the vote passes.

Branch feature/testing-fixes-batch2-20261003, base ecd24c00 / draft PR200.
No main merge. Combined TEST/client handoff includes batch 1.

Expected edits: TradeScreen/Layout, CosmicAdvancementScreen, client death-screen event,
DungeonDeathRecovery, DungeonLifecycleService/ForfeitService, tests and task/release notes.
Exclusive integration: cleanup/respawn orchestration. Existing ballot, escrow, transaction
and inventory-handoff authorities remain. No network registration or save-format change.
No authored inventory, class-chest, world, server.properties or active config edits.
Death-screen commands revalidate run membership, vote expiry, threshold and duplicate votes.
Automatic native respawn requires a durably ending failed run and resolved other custody;
it uses the existing outside recovery destination before normal inventory restoration.

Validation: Java 21 build and native JUnit/receipt regression suite; source JSON and scoped
diff checks. No datagen applies to these Java presentation/lifecycle changes.
No clean/GameTest/dedicated launch. Native dev startup is separate from licensed gameplay QA.
Manual: minimum/normal GUI trade slots and denomination hitboxes; advancement details,
hover and narration before/after earning; solo and party death-screen votes, rejection,
expiry, one living/one dead, all dead, reconnect and exact pre-entry belongings.

After this batch, nine implementation batches remain; cumulative licensed testing is separate.
