# D1 Batch29: recoverable Watson outcomes

Watson's six-Bloom hand-in now records exact inputs and rewards before consuming items.
Owner receipts and receipt-protected lifetime, progression and faction saves recover interrupted
outcomes once. Final inventory cleanup/reset waits for the matching settled result.
New Lesser Bloom statistics commit only on success; immediate harvest faction stays unchanged.
Old totals and uncertain legacy outcomes remain preserved for review.
JohnWatson.outcomeRecoveryPollTicks in CosmicDungeon.config controls bounded recovery polling.

Existing saved-data IDs gain optional fields; no item/spawner/preset/network identity changes.
7,951 offline checks, two config round trips and Java21 build passed. Native gameplay and
licensed multiplayer acceptance remain pending. No deployment or new required texture.
Details: ../../ai/D1_BATCH_29.md.
