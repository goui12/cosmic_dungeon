# D1 Batch31: durable wealth review

First wealth-threshold notifications now persist with their account transaction instead of
depending on an online developer. Bounded delivery resumes after verified saves and reconnects.
Developer/direct-console reviews can be acknowledged, resolved or reopened with a checked
revision and an audited note. Repeating the same uncertain-write decision is idempotent.
Legacy cap balances, old notification markers and existing final-review evidence remain
reviewable without confiscating money or fabricating historical earning transactions.

CosmicDungeon.config adds Economy.wealthReviewIntervalTicks (100) and
Economy.wealthReviewWorkPerInterval (8). Existing thresholds and vendor prices are unchanged.
An optional schema1 account inbox preserves prior save shapes; use complete backups when
upgrading or reverting. Licensed multiplayer/save-failure acceptance remains pending.

Java21 offline build,8,597 checks and two config round trips passed. No deployment, runtime
launch or required new PNG. See docs/ai/D1_BATCH_31.md and docs/ai/D1_REMAINING.md.
