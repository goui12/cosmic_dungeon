# Batch32: legacy currency and receipt retention

Completed in code and offline-validated on 2026-09-19. Cameron expanded the active Batch33
instruction to finish32 and33. Final local commit covers both; STOP before34.
Verified parent: `fcccd96a20c55d162db0f07cf7a1318fa7c689d5` (Batch31).
The private32 baseline explicitly records the uncommitted, validated33 work already present.

## Changes

- B32-1: removed the old physical-denomination deposit/discard pair. Unverified old
  Trace/Mark/Seal/Crown/Anchor stacks no longer automatically credit accounts or enter
  normal pickup. Current managed death drops retain their account-owned recovery path.
  Held legacy denominations share existing no-drop/private-storage and trade/sale guards.
  Pickup messaging is throttled; its cache clears on logout/server stop.
- B32-2: developer/direct-console `/currency legacy inspect <player>`,
  `entity <item>` and `receipts` provide read-only review. Inventory/Ender/nested inspection
  is limited to256 nonempty stacks, depth8 and32 findings; limitations are explicit.
  Entity inspection targets one already-loaded item. Receipt counts use existing indexes.
  Nominal item value is clearly distinguished from an approved unpaid entitlement.
- B32-3: compact exact retired-run intervals now accompany removal of finished-run
  mob receipts in the same account image. Closed-run reward replay is rejected for any
  owner, including after native save/reload. Holes remain open; no highest-run guess.
  First Trace/Inn receipts, transfers, operations, pending reservations, balances and
  historical ledger evidence remain intact. Paired/operation terminal IDs are not pruned.

## Authority and limits

Four relevant Google Docs refreshed unchanged:
Economy `17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28` (2026-08-18),
Trading `1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc` (2026-08-18),
Farrow inventory `1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I` (2026-08-19),
Chop `18McISr9dcvMGp3-VjLDVsISosKBfkaidUTPzHxlw1Pc` (2026-07-07).
Economy currency/denomination, reward-ID, ledger, supply and testing sections were read;
Trading was read in33; Farrow/Chop review confirmed existing restrictions/source conflicts.
The newer supplied Q&A D01/D02/D04/D24/D25 preserves the approved round trip over older
one-way/possession prose. No Chop entitlement or old-owner inference was changed.

The Economy explicitly makes denominations display-only and requires idempotent account
transactions. A surviving old item cannot prove that a prior merged/split entity never
credited an account. This pass therefore adds a safe hold and review route, **not automatic
legacy conversion or compensation**. No old item, account balance, historical statistic,
Chop ownership record or escrow was deleted or reassigned.

Legacy world entities retain native aging, damage and despawn. Blocking redemption is
not a preservation archive. Review complete copies before attempting recovery.
Offline/unloaded storage, earlier pruned receipt history, duplicate/foreign Chops,
orphan inventories and old unreceipted Watson outcomes remain explicit code TODOs.
Future terminal-receipt paging needs an exact online old-ID index before deletion;
existing full evidence stays in the account outbox or verified ledger pages.

## Validation

Combined32+33:10,617 offline checks, two config round trips, Java21 offline build passed.
32 adds1,400 production-policy/account checks and four compressed native account images.
Fixtures cover all five denominations, overflow/foreign IDs, interval gaps/merging/limits,
malformed/future schemas, before/after retirement saves, replay rejection, active receipts,
lifetime receipts, retained committed/cancelled/pending transfers, acknowledged operations,
supply reconciliation and unchanged balances. Existing506 Chop legacy checks also pass.
All1,963 source JSON files parse; source hashes and focused Markdown/diff checks pass.

Command: `gradlew.bat d1OfflineChecks build --offline --console=plain`, Java21.
No new config keys or modifier/price changes. Both generated examples match their exact
baseline bytes after semantic equality checks. Datagen is inapplicable: no registry,
item, recipe, tag, model, texture or resource additions. No runtime/GameTest/deployment/push.

## Saves, permissions and cumulative TEST

Optional `retired_reward_runs` schema1 is added to existing
`cosmicdungeon_player_currency_v1`. Missing old data loads an empty extension; malformed
or future intervals require recovery rather than guessing. Only verified run retirement
calls compaction; the guard and removed per-mob rows share one SavedData payload.
Save readback compares this extension as well as balances/other receipts.
There is no standalone bulk migration and no changed world/spawner/network/registry ID.

Old binaries do not understand the retired-run extension and could later omit it.
A rollback after TEST requires the matching complete backup and binaries, not simply
downgrading a live jar. Never restore account, run, player, entity or ledger files separately.

Review commands recheck the actual connected developer or direct server console.
No player-facing stamp/grant endpoint or runtime dependency was added.
Native command routing, pickup event order, containers and permission changes remain untested.

Licensed TEST steps:
1. On a complete copy, contact each unmarked legacy denomination with room/full account;
   confirm no credit/discard and verify the review message.
2. Check normal items and managed logical death drops, including cap rejection and two players.
3. Inspect inventory/Ender/bundle/shulker and one loaded entity; verify no changes and limits.
4. Check developer, non-developer, console and impersonated command-source access.
5. Retire a run and restart at save boundaries; replay an old mob ID and confirm no payout.
6. Leave a gap run active and confirm its valid rewards/receipts remain usable.
7. Verify First Trace, Inn, cancelled/committed commerce and pending reservations survive cleanup.
8. Review ambiguous Chop/Watson/physical-item evidence from consistent backups; never infer grants.
9. Measure retained account/ledger growth and save times during the cumulative performance pass.

Audit counts remain44 implemented/runtime-unverified,21 partial D1,9 preserved-content,
27 deferred D2+ =101. M03/M20 remain partial for explicit legacy/world/runtime follow-ups.
No new required PNG; optional Tamsin map512x256 remains.
A future improvement is an exact paged terminal-receipt index with native interruption tests.

Exact changed files: [combined checkpoint](D1_BATCHES_32-33.md).
Private evidence: `Google Docs and Sheet/Audit/D1_Batch_32_2026-09-19/`.
Remaining: [five planned batches34-38](D1_REMAINING.md), then cumulative licensed testing.
