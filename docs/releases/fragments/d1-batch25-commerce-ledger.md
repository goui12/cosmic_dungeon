# D1 Batch25: vendor transaction recovery and economy ledger

Retail purchases, vendor sales (including explicitly confirmed zero-price surrender) and
direct shop repairs now retain exact input/output components, a shared-account decision
and a verified owner-file receipt. Daily stock shares the delivered-item save; legacy stock
seeds first use. Chop purchase/sale ownership is applied through an idempotent saved comparison.

Account outbox rows archive into native-NBT pages of128 records with staging/readback and
conflicting-evidence rejection. Reports separate generation, sinks, gross transfers and
administrative adjustments, show32 recent UTC days, cap rejection, median/percentiles and
supply difference. Maximum-balance crossing evidence persists without preventing debits.

New Economy settings in CosmicDungeon.config: ledgerFlushIntervalTicks=1200 and
ledgerRowsPerFlush=256. Vendor prices and other defaults are unchanged.

Existing currency save ID/balances/overrides remain; operations/ledger and owner custody,
receipt and stock fields are additive. Back up the entire world before upgrading; a rollback
must restore the matching complete backup, not individual player/account/ledger files.
No spawner, world-authored content, registry ID, network protocol or texture changes.

Validation: Batches23-25 combined passed2,426 offline checks, two config round trips,
Java21 build,1,963 JSON parses and diff checks. Datagen not applicable to these batches.
Native Minecraft save failures, menus/multiplayer, integrated-owner behavior and latency
remain untested. Death-drop supply and later Inn/travel boundaries remain source-backed TODOs.
