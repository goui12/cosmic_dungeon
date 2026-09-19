# D1 canon and configurable gameplay
Status: implementation in progress; not ready to deploy.
Branch: feature/d1-canon-config-20260916.
Baseline: 94e13b182bb8756935125af753940d1cbaee6b52.

Cameron's 2026-09-16 request authorizes completing Dungeon 1, shared server modifier
configuration, vendor price configuration, instance objectives and separate lifetime
statistics. The debloated workbook defines scope; use its linked documents. Current
direct instructions govern; compare applicable source revisions by modified date.
The supplied Q&A has embedded modified time 2026-09-16T20:42:01Z. Revalidated all 423
accessible linked Docs before implementation; record any later source supersession
and all interpretation choices in the private implementation audit.

Preserve authored vanilla chest contents, registry IDs, existing world/spawner data,
explicit account overrides, and all pre-existing tooling changes. Later dungeons and
explicitly deferred features get detailed source-backed TODOs, not new behavior.
No deployment, game launch, local GameTest runtime, heap increase, or release commit.

Ownership: one writer for config/bootstrap, D1 saved data/lifecycle, class and vendor
services, commands, network payloads, datagen, and relevant documentation.
Saved data additions use new IDs or optional fields. Existing item/block IDs remain.
UI naming must not invalidate placed class selector blocks.

Validation: Java 21 compile/build, offline rule/config and persistence fixtures,
datagen for generated assets, JSON and whitespace checks. Preserve tracked 1.5.0 JAR;
do not run clean in this checkout. Licensed multiplayer/world acceptance remains a
separate check on the TEST server when Cameron authorizes execution.

Implementation pass is ready for code review; Dungeon 1 remains incomplete and not release-ready.
See ../D1_IMPLEMENTATION_20260916.md for behavior, compatibility, config, setup and validation.
See source dungeon/d1/package-info.java for 73 source-linked follow-up entries plus shared
runtime/quote checks. The TODO register enables no deferred feature behavior.
Private audit: Google Docs and Sheet/Audit/D1_Implementation_2026-09-16.
All 101 findings have dispositions; 66 Q&A answers replace the old unanswered list;
five narrower questions remain. Older closed audits are preserved.

Resume 2026-09-17: same task/branch, single writer. Added vendor quote/session confirmation
and success-only lifetime kills. Hotspots: ModNetwork/payload dispatch, menu session state,
CurrencyService, Watson completion and optional lifetime save fields. Existing source and
world boundaries remain. See the implementation guide follow-up and new release fragment.

Batch 02, 2026-09-17: persistent Tamsin agreement, interface map/class flow, and
server quote price components. New optional-field Tamsin store; menu-bound class
packets; network compatibility version 2. Remaining tracker: ../D1_REMAINING.md.
Setup requires binding the authored NPC before enabling player entry on TEST.
No change to worlds, chests, configs in use, spawners or deferred dungeon behavior.


Batch 03, 2026-09-17: personal invitations, pre-entry roster ownership, readiness reset
and leader-controlled FIFO queue. Related fixes isolate waiting lobbies from run reset
and validate landing space after paste. Protocol 3; no new persistent store/schema.
Single-writer hotspots: ClassSelectorReadyManager/EntryService, ClassNet, ModNetwork,
ClassSelectorMenu/Screen, D1PartyLobby/Service/Panel, Config and D1Command.
Current source refresh and exact before/after receipts:
Google Docs and Sheet/Audit/D1_Batch_03_2026-09-17.
M37 implementation is complete; runtime acceptance and M102 crash recovery remain.


Batch 04, 2026-09-17: B04-1 trusted 23-item D1 identity/adoption; B04-2 configured
named payouts; B04-3 sale/trade provenance gates and trusted retail issuance.
Single-writer hotspots: ModDataComponents (one additive string component), D1Command,
ModNetwork compatibility version 4, VendorCatalog/VendorPricingService/VendorService,
TradeMenu/TradeSessionData. Existing ItemStack codecs preserve unrelated components.
No new SavedData, item registry, world/spawner migration or runtime packet family.
Broad M10/M12/M72/M114 remain partial; M39 tax/journal stays deferred with detailed code TODOs.
Current exact source/hash/test receipt: Google Docs and Sheet/Audit/D1_Batch_04_2026-09-17.

Batch 05, 2026-09-18: personal Tax eligibility, one-item snapshot receipt/recovery, and
Tax selection/confirmation are implemented; M39 moves to implemented/runtime-unverified.
M03/M115 cross-file account/trade recovery remains partial. Protocol 5. Additive player-root
NBT and hidden advancement; no item/world/spawner migration. Single-writer hotspots:
TamsinTax*, ClassSelectorMenu/Screen, ClassNet, ModNetwork/ModNetworkClient, Config,
D1WatsonService, D1ObjectiveBindings and ModAdvancementProvider. TEST needs authored
base_camp plus NPC bindings and actual save/restart/menu acceptance. Details and exact
receipt: Google Docs and Sheet/Audit/D1_Batch_05_2026-09-18. No deployment/game launch.

Batch 06 (2026-09-18): voluntary protected movement guards plus deliberate held/container
preview/apply/undo. Single writer owns movement policy, ServerPlayer/packet-handler mixins,
mixin configuration, item authoring commands and Config. No new save schema/packet/registry.
Continue batch after batch under Cameron's instruction; queued breakpoint phrase:
"finish what you're doing and stop". Finish current batch, validate/checkpoint, then stop.
No change to existing deployment, runtime launch or live world migration boundaries.


Batch 07, 2026-09-18: protected death/clone inventory; owner-only dropped equipment; vanilla
hopper/dispenser/dropper/mob guards. Same-player inventory and existing ItemEntity.Owner only.
M10/M114 remain partial pending cursor overflow and installed-capability verification.
No network/schema/registry changes. Batch 08 follows without approval unless Cameron queues
"finish what you're doing and stop"; then finish and checkpoint the current batch only.


Batch 08, 2026-09-18: cursor/menu/logout overflow; forced armor return; exact run-scoped recovery.
New optional player-root protected_item_returns_v1; existing clone root handles preservation.
No packet change. Added /d1 recover and central configurable per-command processing budget.
Cleanup receives explicit ending run IDs, closes menus before escrow removal, and applies success
or failure only to that run. Cross-file transaction and open-menu crash journals remain separate.
Next bounded batches cover the remaining D1 achievement, transaction and companion follow-ups.


Batch09: successful bell actions, bounded full-room candle scan and persistent shared credits.
M76/M77 authorized D1 portions implemented/unverified; Withers remain explicitly deferred.
New optional achievement_credits map in existing objectives save; no network change.
Config adds bell count and candle scan budgets. Next Batch10 continues remaining M79 bindings.


Batch10: marked canonical journals, actual hand/lectern opens, per-player reading and durable earned
Librarian credit. Stairway delivery/receipt share player save; validated bindings/status support
double chests. No world contents changed. Optional player receipt; protocol5 unchanged.
M79 remains partial for companion-disguise definition and authored/runtime verification.
Next Batch11 reviews Bogatyr ownership, configurable cap/duration and persistence boundaries.


2026-09-19 authorization checkpoint: complete Batch25, validate23-25, commit locally and STOP.
The temporary Batch23-only rollback was superseded; archived24/25 work was restored by hashes.
Batches23-25 validation passed2,426 offline checks/two config round trips and Java21 build.
No Batch26 implementation, runtime/GameTest/deployment or push. AGENTS now requires a final
local implementation commit after validation and completion notes. This is not a release.
Single writer owns account/vendor/trade/repair recovery, owner NBT, config and related tests/docs.
See ../D1_BATCHES_23-25.md and ../D1_REMAINING.md for final scope and remaining work.


2026-09-19 Batch26 checkpoint: Cameron authorized Batch26 only, followed by a local commit and stop.
Canonical death intents, debit/drop/pickup/despawn accounting and bounded projection recovery are
implemented. Hotspots: account SavedData/ledger, native player/item/entity hooks, inventory guards,
dimension reset boundary and server Economy config. Optional death_currency schema1 and player
death_currency_life_v1 preserve legacy saves; no registry/network/spawner change.
2,549 offline checks plus two config round trips and Java21 build passed. Runtime, native save
interruptions and licensed multiplayer acceptance remain pending. No launch/deployment/new PNG.
M08 implemented_unverified; M03 retains Inn/travel and legacy pickup follow-ups. Batch27 awaits Cameron.
See ../D1_BATCH_26.md for exact files, compatibility, backup/rollback and cumulative QA.


Batch27, 2026-09-19: Inn verified bond/bed persistence, approved-bed halves/dimensions, native
protection/Heart hooks; Chop full-inventory/location/ownership/escrow recovery and lossless legacy
holds. Single writer owns Inn/Chop SavedData, shared save proof and inventory/currency gates,
dungeon lifecycle/direct-restore blockers, common mixins and server config.
Existing save IDs gain optional fields; no registry/network/spawner change or authored-world edit.
3,153 offline checks + two config round trips + Java21 build passed; runtime deferred.
M40 implemented_unverified; M20/M43/M102 retain explicit legacy/final-handoff TODOs.
Final local commit after all notes and staged review, then STOP before Batch28. No push/deployment.
Details: ../D1_BATCH_27.md; remaining work: ../D1_REMAINING.md.


Batch28, 2026-09-19: Cameron authorized28, checks and a final local commit, then STOP before29.
Implemented immutable cleanup/claim decisions, native owner receipts, offline escrow/stash/Raw
handoffs and full pre-entry startup rollback. Single writer owns run/pending/stored SavedData,
inventory/Chop/lifecycle/reset/travel gates, entry coordinator and player save proof.
Existing save IDs gain optional maps/player keys; no registry/network/spawner migration.
3,836 offline checks (683 new), two config round trips, Java21 build and1,963 JSON checks passed.
M43/M102 implemented_unverified;43 implemented,22partial D1,9preserved,27deferred=101.
M03/M93 retain Watson outcome/Bloom/lifetime/progression/faction transaction work with code TODOs.
No runtime/GameTest/deployment/push, active config/world edit, datagen or required PNG.
Details: ../D1_BATCH_28.md. Final local commit after all notes/staged review, then STOP.

Batch29 (2026-09-19): authorized Batch29 only, focused checks, final local commit and STOP.
Verified parent28: 77d257f101f818597f64dc687a3451d9f151fbe1.
Single writer owns existing objective/lifetime/progression/faction save extensions, Watson
interaction, inventory/currency guards, login and final cleanup/reset integration. One saved
outcome captures physical Blooms, Tax proof and reward amounts; per-owner/native projection
receipts resume once before matching cleanup. New Lesser statistics are success-only; old
totals stay intact. Optional fields preserve old save IDs; no spawner, registry or protocol change.
7,951 offline checks (4,112 Watson,3 new config), two config round trips and Java21 build passed.
Four relevant live Docs unchanged; Q&A D11/D20/D23 overrides older thresholds/failure retention.
See ../D1_BATCH_29.md and ../D1_REMAINING.md; private evidence D1_Batch_29_2026-09-19.
Totals43 implemented/22 partial D1/9 preserved/27 deferred=101; native gameplay remains untested.
No new PNG, datagen, active config/world edit, launch, deployment or push. Final local commit
follows completion notes and staged allowlist review. Batch30 is proposed and awaits Cameron.

Batch30 (2026-09-19): authorized Batch30 only, checks, final local commit and STOP before31.
Verified parent29: 06336504483e26b294ebedb8bea6934d4b4a1e2b.
Single writer owns Chop command registration, existing ownership/escrow journal validation and
inventory issuance/adoption guards. One explicit orphan review preserves exact item images,
self-owned token and developer evidence; older custody blocks a conflicting new entitlement.
Item authoring rechecks pending recovery, live session/cursor and original run scope.
Optional review metadata preserves old plan shapes; no new store/registry/network/spawner ID.
8,457 offline checks (506 new), two config round trips and Java21 build passed.
Four relevant live Docs unchanged; current Q&A D01/D02/D04/D24/D25 overrides older Chop prose.
M03/M20/M10/M72 retain broader follow-ups; totals43 implemented/22 partial/9 preserved/27 deferred.
Detailed legacy physical-currency and ambiguous Chop TODOs remain in code; no migration ran.
See ../D1_BATCH_30.md and ../D1_REMAINING.md; private evidence D1_Batch_30_2026-09-19.
No new required PNG, datagen, active config/world edit, launch/GameTest, deployment or push.
Final local commit follows all notes and staged allowlist review. Batch31 awaits Cameron.

Batch31 (2026-09-19): authorized Batch31 only, checks, final local commit and STOP before32.
Verified parent30: bf091702f67deb48e2f57061df028cadaac2204f.
Single writer owns account CODEC/ledger, economy review commands/events and server economy config.
Optional wealth_review schema1 preserves old account IDs, balances, overrides and old markers.
First-threshold evidence shares every journaled balance decision; paired-row rollback includes it.
Explicit developer/direct-console decisions are revision checked, audited and idempotent.
Bounded legacy observation runs before new balance changes and in the background for offline owners.
Notifications remain pending until acknowledgment; chat may replay after reconnect or interruption.
Two relevant live Docs unchanged; debloated MASTER and Q&A hashes still match the sealed baseline.
8,597 offline checks (135 new wealth,5 new config), two config round trips and Java21 build passed.
M02 moves to implemented_unverified:44 implemented/21 partial D1/9 preserved/27 deferred=101.
Seven planned implementation/review batches remain (32-38), then cumulative licensed gameplay QA.
The count is an estimate distinct from audit IDs. AGENTS now requires the count and a short
summary of every remaining batch after each completion, as Cameron requested.
See ../D1_BATCH_31.md and ../D1_REMAINING.md; private evidence D1_Batch_31_2026-09-19.
No new required PNG, datagen, active config/world edit, launch/GameTest, deployment or push.
Final local commit follows notes and staged allowlist review. Batch32 awaits Cameron.


Batches32-33 (2026-09-19): Cameron expanded "Proceed with33" to complete both32 and33.
Verified parent31: fcccd96a20c55d162db0f07cf7a1318fa7c689d5. Final combined local commit then STOP before34.
Single writer owns currency pickup, account CODEC/receipt retirement, currency review commands,
item identity/adoption/movement/transfer guards and their focused offline checks.
Optional retired_reward_runs schema1 preserves old balances and receipt schemas; no new item,
network payload, spawner format or active-world/config edit. New held-ammo authoring adds only
the existing stable component; no authoring command or migration was executed.
Combined10,617 offline checks (1,400 new32,620 new33), two config round trips, four new native
account images, Java21 build and1,963 source JSON checks passed. Source artifacts verified.
Unknown legacy entitlements and world mappings remain detailed code TODOs, not guessed grants.
33 refreshed33Docs (18body reviews);32 refreshed four unchanged Docs. Workbooks unchanged.
Counts44 implemented/21partial D1/9preserved/27deferred=101. Five planned batches remain:
34 mobs/NPCs;35 pricing;36 D1 effects/ammo;37 progression/travel;38 displays/performance/QA handoff.
No runtime/GameTest/datagen/deployment/push or new required PNG. Optional Tamsin map512x256 remains.
See ../D1_BATCHES_32-33.md, ../D1_BATCH_32.md, ../D1_BATCH_33.md and ../D1_REMAINING.md.
Final local commit follows all completion notes and staged allowlist review. Await Cameron for34.

Batch34, 2026-09-19: Cameron authorized34 only; final local commit, STOP before35.
Single-writer hotspots: spawner load/provenance, server config/economy rewards, vendor
commands/identity/access, dependent Tamsin/Inn/direct-repair service guards and offline tests.
B34-1 preserves native SpawnData/potentials and pauses malformed tags without rewriting them.
B34-2 adds validated coordinate reward registrations; no inferred or guessed encounter payouts.
B34-3 binds the crosshair NPC with line of sight, preserves original state and refuses other roles.
B34-4 blocks legacy conflicting services and cross-dungeon offer-tier leakage.
No network/save/registry ID change; optional entity vendor_binding_before schema1 only on
future explicit assignment. No live bindings, spawners, worlds, presets or configs edited.
10,794 checks, two config round trips,1,963 source JSON and Java21 offline build passed.
Full native SpawnData/weighted preset and entity unload/restart acceptance remains licensed TEST.
Four planned batches remain; report and exact inventory: ../D1_BATCH_34.md.
