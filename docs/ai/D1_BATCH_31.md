# D1 Batch31: durable wealth review

2026-09-19. Authorized: Batch31, validation, remaining-batch plan and final local commit; stop before32.
Branch: feature/d1-canon-config-20260916.
Verified parent30: bf091702f67deb48e2f57061df028cadaac2204f.
Code implementation is complete for M02; licensed native gameplay acceptance remains pending.

## Finished

B31-1: First threshold evidence now lives in an optional inbox inside the existing account save.
Every journaled committed balance path captures it, including credits, paired trades/repairs,
commerce and administrative adjustments. The two-row transfer rollback restores the inbox
together with the ledger. Repeated later crossings remain in the ledger without replacing
the first evidence. The old transient chat path no longer controls first-notification state.

Developer delivery runs only after account write/readback succeeds, with a global message
budget and per-connection cursor. Offline developers do not consume notices. Reconnect or
server restart replays unacknowledged notices; chat delivery itself never acknowledges one.
A failed persistence check advances no delivery cursor, and a failed send retains that notice.

B31-2: Connected developers and the direct server console can inspect, acknowledge, resolve
or reopen a review. Expected revision, actor and exact action/note prevent stale overwrites
and make the last identical uncertain-write retry a no-op. Each new decision writes before/
after review images into the existing account ledger with zero currency change. Resolution
acknowledges and closes; acknowledgment alone leaves review open; reopening creates a fresh
delivery sequence. Original first evidence remains intact.

B31-3: Bounded startup/config-change work observes old offline balances, old notification
markers and surviving final-review rows. Committed transactions also observe their BEFORE
balance so an immediate debit/reset cannot erase an old cap before the background pass.
Unknown historical earnings are labeled legacy_balance or legacy_marker with observation
time/current balance; surviving exact final rows use legacy_final. Nothing truncates balances,
changes explicit capacity overrides, creates income or freezes legitimate spending.

## Sources and scope

The debloated MASTER and current Q&A match their sealed baseline hashes. Direct user instructions
remain highest authority; no Q&A answer contradicts the selected wealth rules. Both applicable
live Docs were revalidated UNCHANGED before edits and their relevant sections were read:

| Source | Document ID | Modified UTC |
| --- | --- | --- |
| Attunement Fragment Economy (Internal) | 17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28 | 2026-08-18T21:01:23.864Z |
| Gear Trading and Vendor Sales 2.0 (Internal) | 1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc | 2026-08-18T22:35:34.052Z |

Canonical thresholds remain500,000 /80,000,000 /100,000,000 Trace. The delivery cadence/budget
are developer engineering defaults, not claimed quotations from Dad's tables.
Private evidence, hashes, before-images, sources and logs:
Google Docs and Sheet/Audit/D1_Batch_31_2026-09-19/.
Legacy physical-currency pickup and receipt retention remain in Batch32; this pass closes
three related M02 code gaps rather than migrating world item entities.

## Commands and configuration

- /currency review pending [afterSequence]
- /currency review <UUID> [after <threshold>]
- /currency review <UUID> ack <threshold> <revision>
- /currency review <UUID> resolve <threshold> <revision> <note>
- /currency review <UUID> reopen <threshold> <revision> <note>

Pages contain at most16 entries; identifiers and revisions appear in each entry.
Resolve/reopen require a nonblank note of at most512 characters. These review commands require
the actual connected developer source or direct server console, rechecked at mutation.
Command blocks, RCON and /execute impersonation are not accepted review actors. Other existing
currency commands retain their previous access policy. No client packet or network version changed.

Two new values in CosmicDungeon.config, section Economy:
wealthReviewIntervalTicks=100 (20..24000); wealthReviewWorkPerInterval=8 (1..128).
The budget separately limits legacy owners processed and total developer messages per interval.
No per-tick player-file read or world/entity/chunk scan was added. Startup/config changes build
a one-time owner queue; pending notices use ordered indexes. First evidence and compact latest
decisions grow per account/threshold; full decision history uses the existing bounded ledger archive.
Index rebuilding and native save latency still need the cumulative licensed performance baseline.

A new configured numeric threshold already exceeded becomes an observation. Existing numeric
thresholds retain first evidence and prior disposition even if policy labels change; the current
maximum is included in final-review presentation/counts. Explicitly reopen if a policy change
requires renewed review. The maximum threshold does not itself alter account capacity.
The example config adds only these two defaults. Vendor prices and active configs were unchanged.

## Saved-data compatibility and failure handling

The existing cosmicdungeon_player_currency_v1 ID now has optional wealth_review schema1.
Old saves load an empty extension; legacy wealth_crossings and compact final_reviews remain.
NBT/JSON round trips preserve integral values. Strict schema, owner, sequence, crossing,
decision and legacy-marker validation refuses malformed evidence instead of silently discarding it.
Unknown names in legacy observations remain unknown; no player file is opened to invent one.

Notices, balances and decisions share one verified account image. Full economic decisions
remain in the existing outbox/pages; review decisions do not create a separate currency store.
An uncertain command save reports that the decision requires verification; retry the identical
command. A stale revision requires another inspection. Source-backed code TODOs retain the
native runtime acceptance work. Chat can repeat after interruption and is not exactly-once delivery.

Before upgrading TEST, back up the complete matching world, playerdata, level.dat, data and
dimensions. Settle pending transactions before downgrade; revert using the matching full backup.
Older mod versions do not understand the new optional review field and may discard it on save.
Never restore only one older account/player/world file or erase review/custody records to force recovery.
No live world migration, spawner/preset/chest/equipment edit or active config change was performed.

## Validation

8,597 offline assertions passed: the previous8,457 plus135 new wealth checks and5 config checks.
Two .config round trips and Java21 offline build passed. The wealth checks include19 native
compressed account images across old-cap/decision saves and six restart-cut sequences.
They exercise offline delivery, persistence/send failure, reconnect, acknowledgment suppression,
global fairness/budget, stale revisions, exact decision retry, legacy cap/override/marker/final
evidence, immediate reset, paired-ledger rollback, full-outbox rejection, defensive copies,
malformed state and changed maximum classification.

The first run exposed integer-type narrowing during existing JSON codec fixtures; the reader
was corrected without weakening those tests. New tests also reject fractional sequence truncation.
Final review also caught review-only decisions creating a legacy observation that their inbox
snapshot would overwrite. Review decisions now leave observation to its separate transaction;
three additional checks cover changed-threshold ordering. The final passing run, including all
code and TODO updates, is checks-final.log.

Source JSON, local documentation links, baseline hashes, config semantics and whitespace checks
are recorded in FINAL_VALIDATION.json. These are production-rule/codec/delivery-model tests and
code review, not actual ServerPlayer command, network, filesystem-failure or multiplayer tests.
Datagen was unnecessary: no registry/generated-resource change. No clean, GameTest, client/server
launch, deployment or push. The tracked1.5.0 jar and pre-existing generated-cache edit remain intact.

## Cumulative licensed TEST acceptance

1. Back up TEST. With no developer online, cross each threshold through reward and trade paths.
   Restart, join as Goui12/developer, and verify UUID/name/type/amount/balances/time against ledger.
2. Reconnect before acknowledging: pending notices may repeat once per connection. Acknowledge the
   shown revision, restart, and confirm that notice no longer sends; the review remains open.
3. Resolve with a note, repeat the identical command, and confirm only one decision ledger row.
   Reopen, confirm fresh delivery and unchanged original evidence. Try stale competing revisions.
4. Attempt commands as an ordinary player, command block, RCON, impersonated source and disconnected/
   demoted developer. Check both dedicated and integrated server behavior without client-only loads.
5. Use copied old saves with already-over-cap money, explicit overrides, spent old markers and
   existing final rows. Test an immediate debit/reset before polling. Verify no confiscation,
   fabricated historical crossing or lost prior evidence; normal spending remains possible.
6. Interrupt native account writes and archive handoff on a backed-up TEST fixture. Delivery must
   wait for verified persistence; identical decision retry must reconcile. Inspect corrupt-state
   rejection and restore only matching complete backups.
7. Change review cadence/budget and threshold amounts while stopped. Confirm bounded global
   delivery, new observations, retained prior dispositions and final-review counts after restart.
8. Regress reward, trade, repair, vendor, death and First Trace transactions; their balances,
   supply reconciliation, reservations and item custody must remain unchanged by notifications.

## Remaining batches and graphics

M02 moves to implemented_unverified:44 implemented,21 partial D1,9 preserved-content verification,
27 deferred D2+=101. [Remaining work](D1_REMAINING.md) now has seven estimated batches32-38,
followed by the separate cumulative licensed TEST phase. Update their count and each summary
after every completion, as recorded in AGENTS.md. Await Cameron before Batch32.

No new required PNG. Optional existing request: tamsin_d1_map.png,512x256, a winding route
to Base Camp signed -JHW, in src/main/resources/assets/cosmicdungeon/textures/gui/.
The fallback already works; authored vanilla equipment is preserved.
A future improvement is the separately planned proof-backed account receipt retention policy.

## Exact files changed

- AGENTS.md
- docs/ai/D1_BATCH_31.md
- docs/ai/D1_IMPLEMENTATION_20260916.md
- docs/ai/D1_REMAINING.md
- docs/ai/tasks/d1-canon-config-20260916.md
- docs/config-examples/CosmicDungeon.config
- docs/releases/fragments/d1-batch-31-wealth-review-inbox.md
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/package-info.java
- src/main/java/net/goui/cosmicdungeon/economy/CurrencyAudit.java
- src/main/java/net/goui/cosmicdungeon/economy/D1EconomyConfig.java
- src/main/java/net/goui/cosmicdungeon/economy/EconomyLedger.java
- src/main/java/net/goui/cosmicdungeon/economy/EconomyLedgerEvents.java
- src/main/java/net/goui/cosmicdungeon/economy/PlayerCurrencyData.java
- src/main/java/net/goui/cosmicdungeon/economy/WealthReviewCommands.java
- src/main/java/net/goui/cosmicdungeon/economy/WealthReviewDelivery.java
- src/main/java/net/goui/cosmicdungeon/economy/WealthReviewEvents.java
- src/main/java/net/goui/cosmicdungeon/economy/WealthReviewState.java
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java
- src/test/java/net/goui/cosmicdungeon/economy/WealthReviewChecks.java
