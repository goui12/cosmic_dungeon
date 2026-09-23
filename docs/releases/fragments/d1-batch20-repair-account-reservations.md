# D1 repair payment reservations

Repair Ready now reserves the customer fee and the Dragoon receiving capacity without moving
Trace. Both balances and an immutable transaction receipt commit together in the existing
currency saved-data image. Replayed payments cannot overwrite newer balances. Unready,
cancellation and restart release unfinished repair reservations. Repair starts check the exact
server deadline; repeated Ready packets cannot extend it. Large fee deltas cannot overflow.

Dragoon.repairReadyTicks defaults to600 in CosmicDungeon.config. Existing balances, explicit
capacity overrides and over-cap saves are preserved. New optional transfers data defaults empty
for older saves. No registry, item, packet, world binding or spawner schema changed. Before any
future upgrade or rollback, back up the complete world/player/config set; older jars do not know
new reservation receipts. No migration or deployment was run against the actual worlds.

Single-writer hotspots: PlayerCurrencyData/CurrencyService, Dragoon repair session/menu/events/
finalizer, Config, offline checks. No datagen needed: no generated resources changed.

Validation:1617 offline checks plus2 config round trips and Java21 offline build. Includes87
reservation/receipt/quote regressions (old saves, current balances, concurrency, cap change,
zero fees, full64-bit transfers, cancellation/restart, conservation and replay). No game runtime.
Manual QA on a backed-up test world:1)Ready both clients, attempt spending/receiving held funds;
2)unready,close,disconnect,die,change dimension and exceed range;3)start exactly at deadline;
4)complete and replay packets,check exact items/components/account totals;5)restart while Ready;
6)lower cap during Ready and verify cancellation without payment;7)repeat on integrated server.

Limitation: item/cursor custody, exact component reservations and player-file crash recovery
remain M24/M25 follow-up work. Account receipts alone do not make those files atomic.
No new required PNG. Next improvement: coordinate exact inventory images with the paired receipt.
