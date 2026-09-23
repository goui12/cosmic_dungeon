# Durable D1 repair decisions and owner recovery

Repair completion now saves the paired payment and exact item decision in one currency SavedData
image before changing either owner's items. Owner-local receipts are saved with inventory/equipment
and queued returns, read back, then acknowledged. Interrupted undecided repairs return originals;
committed repairs finish once without repeated charge, component consumption or item delivery.
Full plan images are pruned after both acknowledgements; financial receipts remain immutable.

An additive optional repair_plans map extends the existing currency save. Missing fields in old
saves default empty; existing accounts/overrides/receipts remain intact. Participant receipts use
repair_receipt_v1 under the existing player root. No registry/network/item/spawner format changes.
Existing repairReadyTicks/repairStepTicks remain authoritative. Exact deadline wakeups avoid the
old up-to19-tick delay while idle sessions do no scanning. Pending invites still expire normally.

Login recovery precedes dungeon inventory replacement. Cleanup/reset waits for unresolved repair
ownership, and Chop inventory transitions stop on unresolved repair state. Quarantined records are
preserved; missing or manually restored participant evidence never causes guessed item recreation.
Single-writer hotspots: currency data/service, repair session/menu recovery/plan coordinator,
DungeonLifecycleService/Events and FarrowsChopTravelService transition guards, focused offline tests.

Validation:1808 offline checks plus2configuration round trips and Java21 build.126 new22checks use
native compressed account/customer/provider fixtures at11 interrupted-save cut points, plus exact
ownership/data preservation, single payment/result, acknowledgement/pruning, old optional fields,
invalid images and deadline boundaries. The21custody checks add65 more. All source JSON validated.
No datagen: no generated resources changed. No PNG requests, deployment, world migration or launch.

Cumulative licensed QA remains: native menu/mixin behavior; Ready/cancel/close/death/logout/full
inventory/dimension change; crash/restart and injected I/O failures at each save boundary; exact
player.dat plus integrated Data.Player behavior; offline peers; cleanup holds and Chop transitions.
Fixtures establish the tested application save-order behavior, not hardware power-loss guarantees.
Back up the full player/world/config set before eventual upgrades/rollback; older jars cannot
understand new custody/decision receipts. Legacy unreceipted copies require complete backup review.

M24/M25 normal D1 implementation is complete, runtime-unverified. M03 remains partial for the wider
economy, trade, vendor, direct-shop and death-drop ledger. Next improvement: extend shared receipts
and owner custody to trade. Work stops after22 until Cameron selects the next batch count.
