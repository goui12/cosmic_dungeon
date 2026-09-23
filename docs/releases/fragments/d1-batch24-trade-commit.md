# D1 Batch24 - paired trade commit and recovery

Trade uses the existing shared account reservations and an immutable paired item decision.
Both owners' exact offer/cursor custody is saved before committing both account balances.
After the decision is verified, each owner receives the correct offer and their own cursor,
then a player-file receipt is saved/read back before acknowledgement. Replays preserve later
balances and cannot award another offer. Uncommitted restart reservations cancel; committed
ones remain recoverable. Offline participants hold run cleanup until their receipt is settled.
First Trade eligibility is recorded permanently in the same player save as completed custody.

Optional trade_plans in the existing currency payload and trade_receipt_v1/first_trade_completed_v1
in the existing player root. No IDs removed, no spawner/world/packet changes. Full completed
trade images remain available for Batch25 durable archival. Old ambiguous records stay held.
2,002 offline checks, two config round trips and Java21 build passed.114 new account/native-NBT
interruption checks cover11 save boundaries. Five existing GameTest scenarios retain their
assertions using the shared account engine; no GameTest or gameplay runtime was launched.
Datagen not applicable; no new PNGs. Licensed multiplayer/native-save QA remains pending.
