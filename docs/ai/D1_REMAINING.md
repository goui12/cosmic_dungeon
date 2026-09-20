# D1 remaining work

Updated after Batch35, finalized2026-09-20 UTC (September19 on the PC).
Cameron authorized35 only, offline validation and final local commit; STOP before36.
Verified parent:91075b5cb86a92ab648387e40c578d364d46cc07.
Gameplay/multiplayer testing remains deferred for the cumulative licensed TEST pass.
All101 original audit IDs remain intact. Code implementation is not runtime acceptance.
Current dispositions: Google Docs and Sheet/Audit/D1_Batch_35_2026-09-19/implementation_findings.json.
Latest: [Batch35](D1_BATCH_35.md); previous [Batch34](D1_BATCH_34.md).

## Planned batches remaining: 3

These are estimated implementation/review batches, distinct from gameplay testing.
Concrete findings may change scope; native/world/performance acceptance remains in the
separate cumulative licensed TEST phase. This plan authorizes no launch or deployment.

| Batch | Work remaining | Audit IDs / boundary |
| --- | --- | --- |
| 36 | Final source/code review of D1 class effects and ammunition. | M55/M58/M60/M63; native effects remain untested |
| 37 | Achievement rewards, progression bindings and travel gates. | M79/M81/M93/M101; preserve instance/lifetime separation |
| 38 | Help, balance displays, performance bounds and cumulative-test handoff. | M09/M104/M105; include Batch35 recipe/crafting release gates; measure live performance later |

Remaining20 partial IDs can span these batches or explicit legacy/world TEST acceptance.
Nine preserved-content IDs still need native verification. The27 D2+ entries remain
TODO-only and excluded from this estimate. Consult dispositions before writing more code.

## Batch35 completed in code

B35-1: native Sweeping Edge maps to the retained config section and developer overrides.
B35-2: reviewed conversion ceilings account for yield, fuel, returned containers and cheaper inputs.
B35-3: disabled-input isolation, existing caps and player-trade separation preserved.
B35-4:182 source price fixtures,23 named-price reconciliations and1509 calculator formulas reviewed.
11,933 offline checks (1,139 new), two config round trips, Java21 build and1,963 source JSON passed.
M107 reference tooling is implemented/runtime-unverified. M12/M106 retain custom/native recipe,
crafting access, legacy/world and source-link checks explicitly recorded in code TODOs.
Counts45 implemented/20partial/9preserved/27deferred=101. No new required PNG.
No world, workbook or active config edits; no runtime launch, deployment or push.

## Batch34 completed in code

B34-1: native SpawnData/weighted potentials and authored NBT preserved when provenance is added.
B34-2: exact typed rewards, optional per-spawner config, ambiguous registrations blocked, NPCs excluded.
B34-3: ray-targeted developer binding, role/type checks, original-state evidence and safe clear.
B34-4: shared role-conflict guards and personal offer tiers isolated to their dungeon system.
10,794 offline checks (173 new reward/binding plus4 config), two config round trips,
Java21 offline build and1,963 source JSON checks passed. No new required PNG.
M05/M17/M41 remain partial for authored-world/legacy acceptance; M103 still needs native
spawner/preset loading verification. Counts44 implemented/21partial/9preserved/27deferred=101.
No world edits or assumed NPC/encounter mappings. Full native SpawnData codec needs
Minecraft registry bootstrap and is explicitly reserved for licensed TEST.

## Batches32 and33 completed in code

B32-1: unverified physical denominations no longer auto-credit/discard; existing items
remain review evidence. Managed logical death currency retains its current journal.
B32-2: bounded developer/direct-console legacy inventory/entity and receipt inspection.
B32-3: exact retired-run intervals preserve replay rejection when mob receipt rows compact;
active/lifetime/paired/operation receipts and all balances remain intact.
B33-1: eighteen D1 ammunition identities, canonical/legacy aliases and strict marker/signature checks.
B33-2: explicit held-ammunition preview/apply/undo; recognized legacy ammo shares no-drop guards.
B33-3: all23 named-drop enchantment signatures verified during new named adoption.
B33-4: six class loadout/source mappings, preserved quantities and explicit unknown world assignments.
Combined10,617 offline checks (1,400 new32;620 new33), two config round trips, Java21 build,
four new native account images and1,963 source JSON checks passed. No new required PNG.
Counts remain44 implemented/21partial D1/9preserved/27deferred=101. Runtime remains untested.
Legacy bulk conversion, unknown templates/spawners and historical grants were not guessed.

## Batch31 completed in code

B31-1: First threshold notices now share the authoritative account/ledger image. Bounded
developer delivery waits for save readback and replays unacknowledged notices after reconnect.
B31-2: Developer/direct-console acknowledgment, resolution and reopening use expected revisions,
exact retry detection and a zero-value ledger decision retaining the original evidence.
B31-3: Older balances/markers/final-review rows become honest observations, including before
an immediate debit/reset. Existing over-cap money and capacity overrides remain intact.
8,597 offline checks (135 wealth plus5 new config), two config round trips and Java21 build passed.
M02 is implemented/runtime-unverified. Counts44/21/9/27=101. No new required PNG.
Legacy physical currency remains scheduled for32; it was not folded into this M02 pass.

## Batch30 completed in code

B30-1: Developer inspect/preview/apply recovers one reviewed orphan Chop through the existing
save journal, retaining exact original item/ownership evidence in the owner receipt.
B30-2: Purchases and automatic recovery respect old escrow/stored belongings and conflicting
return/ownership data. Orphan escrow has a constant-time owner index.
B30-3: Item-adoption preview/apply/undo rechecks pending recovery, live session, empty cursor
and original dungeon scope. Unknown mappings and ambiguous items remain untouched.
8,457 offline checks (506 new), two config round trips and Java21 build passed.
M03/M20/M10/M72 remain partial for their broader legacy/world/runtime follow-ups.
Counts remain43/22/9/27=101. No new required PNG. See Batch30 for exact files and TEST steps.

## Batch29 completed in code

One saved Watson decision now controls exact physical Bloom inputs, Tax eligibility and
receipt-protected lifetime/progression/faction projections. Matching settled outcomes alone
authorize final cleanup; login/poll recovery handles interrupted inputs without repeated awards.
New Lesser harvest statistics commit only on success, while immediate harvest faction remains.
Old totals stay intact; old unreceipted Watson flags and uncertain historical counts require review.
7,951 offline checks (4,112 Watson plus3 new config), two config round trips and Java21 build passed.
M03/M93 retain their broader legacy/world/runtime follow-ups. Counts remain43/22/9/27=101.
No new required PNG. See Batch29 for save compatibility, configuration and licensed TEST steps.

## Batch28 completed in code

D1 success/failure/kick/offline cleanup now saves an owner decision before retiring run escrow.
Success stores the other inventory; failure restores outside belongings. Raw Chop entitlement
shares that decision. Player receipts and per-owner completed-run watermarks prevent replay.
Stored claims are bounded, component-preserving and recover their exact remainder.
Startup retains full pre-entry inventories and ownership until all members' entry saves verify;
an incomplete startup rolls back the roster after restart and retains personally chosen classes.
Reset/direct restore waits for durable handoffs. Legacy unreceipted copies remain held for review.
3,836 offline checks (683 new), two config round trips and Java21 build passed; runtime untested.
M43/M102 are implemented/runtime-unverified. M03/M93 retain Watson's earlier reward transaction.
Totals:43 implemented,22 partial D1,9 preserved,27 deferred=101. No new required PNG.

## Batch27 completed in code

Verified Inn bond/bed writes, both-half bed approval, per-bed dimensions, valid respawn priority,
and native protection/First Heart hooks. No authored world or NPC was changed.
Chop travel now has exact inventory/location/ownership/escrow decisions with owner save receipts.
Single Raw adoption and run-end Raw delivery use that journal; ambiguous legacy stacks remain intact.
Direct world restore, member removal and reset wait for unresolved journeys.
3,153 offline checks, two config round trips and Java21 build passed. Runtime testing remains pending.
M40 is implemented/runtime-unverified. M20/M43 retain legacy review and final lifecycle handoff;
M17 remains world identity verification. Totals:41 implemented,24 partial D1,9 preserved,27 deferred.
No new required PNG. See the Batch27 report for native TEST steps and migration/rollback limits.

## Batch26 completed in code

Canonical configured death loss, unique persisted intent, one recoverable logical Trace drop,
whole-amount pickup and ordinary destruction/expiry accounting now share the account save.
Recovery is bounded and does not force-load chunks. Dimension reset covers unloaded records.
Active-drop value joins supply reports; older account shapes load an empty extension.
2,549 offline checks, two config round trips and Java21 build passed;116 new death checks and7 config checks.
M08 is implemented/runtime-unverified; M03 retains Inn/travel, legacy physical-currency and native QA work.
No required PNG, new registry/network protocol, active config/world edit, launch or deployment.

## Batch25 completed in code

Vendor sales, retail purchases and direct repairs use an account decision plus owner-file
custody/receipt. Daily stock and Chop purchase/sale ownership recover with that decision.
A verified account outbox feeds fixed-size native-NBT ledger pages; exact items and quote
breakdowns persist. Reports separate generation, sinks, gross transfers and admin changes;
recent daily totals, median/percentiles, rejected cap value and final-review evidence persist.

Combined23-25: 2,426 offline checks, two config round trips and Java21 build passed.
Batch25 adds387 commerce/stock/interruption checks,34 ledger/report checks,3 config checks.
M118 normal implementation complete; M02/M03/M09 retain explicit follow-ups below.
No new required PNG, network/registry change, active-config edit or gameplay launch.

## Batch24 completed in code

One saved account decision now controls both item offers and both payments. Each owner recovers
once after a verified player-file receipt; unfinished reservations cancel on restart. Run resets
wait for unresolved offline participants. First Trade credit survives the owner save boundary.
2,002 offline checks plus two configuration round trips and Java21 build passed;114 new trade
commit/interruption checks model11 save boundaries. Existing five GameTest cases retain their
assertions and use the shared account engine; GameTests themselves were not launched.
M114/M115 normal D1 implementation complete; native runtime acceptance remains pending.

## Batch23 completed in code

Owner-local trade offers/cursors, exact accepted-offer checks, eligibility/pricing checks and
lifecycle cancellation now use player-local protected recovery. Legacy world-file copies remain
held for review. 1,888 offline checks and two configuration round trips plus Java21 build passed.
Cross-player account/item commit was completed in Batch24. No runtime launch/deployment/new PNG.

## Batches21 and22 completed in code

- B21: Owner-local target/cursor custody, exact component escrow and protected lifecycle returns.
- B22: Verified account/item commit decision, one-time participant recovery and cleanup/Chop guards.
- Repair completion and Ready expiry wake on exact ticks, without an idle-session scan.

1,808 offline checks plus two configuration round trips and Java21 build passed.
New checks:65 custody/player-save checks in21;126 commit/interrupted-save/timing checks in22.
The save fixtures model11 interruptions around the account/customer/provider save boundaries.
M24/M25 normal D1 implementation is complete; native runtime and licensed multiplayer acceptance
remain pending. Corrupt/ambiguous/older unreceipted saves retain evidence for developer review.
No actual game launch, deployment, world/spawner edits, active config edits or new PNGs.

## PNG requests

No new required item PNGs in this batch.
Optional: tamsin_d1_map.png, 512 x 256, a winding map ending at Base Camp, signed -JHW.
Place in src/main/resources/assets/cosmicdungeon/textures/gui/. A drawn fallback already works.
Conduits and the unmade D1 backpack remain explicitly deferred under Q&A D42/D48.

## Priority D1 items remaining (4)

- M10: Finish legacy item adoption and no-drop/world-container protections
- M72: Verify reviewed adoption against actual templates, containers and drop definitions
- M03: Verify legacy evidence and native replay guards; retain ambiguous historical grants
- M79: Complete remaining achievement and reward bindings

## Other D1 follow-ups (17)

- M05: Replace health-derived payouts with approved mob reward definitions
- M09: Verify native balance-display resync and stale-packet behavior
- M12: Complete production-cost/conversion review and named-drop runtime pricing acceptance
- M17: Correct the Beatrix/Beluzon profile roles and unlock mapping
- M20: Review duplicate/foreign Chops and orphan escrow; single-item preview/apply is implemented
- M41: Establish stable NPC identity and global spawn/personal-access separation
- M55: Implement exact restorative/vision/marking arrow identities and tier effects
- M58: Add the approved Venefex attack, debuff and movement mechanics
- M60: Reconcile Dragoon trident enchantments and chain lightning with bounded targeting
- M63: Align Pyroclast rockets and launchers with exact payload/damage rules
- M81: Reconcile every D1 quest, region, key and startup-room binding
- M93: Verify authored physical progression bindings and native outcome/cleanup recovery
- M101: Validate personal travel gates and existing destination isolation
- M104: Put explicit budgets around new AI, auras, snapshots and client effects
- M105: Update help, commands and player terminology only alongside approved behavior
- M106: Validate the retained item catalogue and repair broken references
- M107: Use the linked calculator as a checked pricing reference, not a runtime web dependency

## Implemented; runtime acceptance pending (44)

- M02: Durable wealth notifications, reviewed dispositions and legacy-cap observations

- M43: Durable Chop run-end entitlement and inventory handoff
- M102: Durable startup rollback, cleanup and stored-item claims

- M40: Verified Inn bond/bed persistence, respawn fallback and protected First Heart

- M08: Canonical death debit, recoverable logical drop and supply reconciliation

- M118: Exact consented zero-Trace surrender with durable item/account recovery

- M114: Exact trade custody and server-enforced item restrictions
- M115: Paired trade commit and owner-safe recovery

- M24: Timed two-party repair with exact target/component/payment reservations
- M25: Owner-local repair custody and verified decision/receipt recovery

- M44: Implement Bogatyr companion ownership, active cap, lifecycle and verified recall

- M76: Require distinct successful bell actions for Synchronous Peal
- M77: Make Sixfold Vigil lighting and variant credit match authored state

- M39: Implement personal Tamsin Tax eligibility, one-item payment and receipt recovery

- M01: Raise the default account cap and define migration of overrides
- M04: Distinguish automatic-reward overflow from transaction overflow
- M06: Use canonical 60-block and dead-player eligibility
- M07: Preserve and rotate split remainders
- M11: Support the approved universal purchase catalogue
- M13: Rebuild shop inventories and costs from the approved vendor lists
- M14: Replace lifetime caps with per-player morning stock where required
- M15: Add the distinct NPC/vendor faction and transaction-time effects
- M16: Wire Lesser Bloom gains and attributable NPC-death penalties
- M18: Wire persistent acquisition and canonical completion rounding
- M19: Complete the Torch Flower success and permanent-reward integration
- M21: Reconcile temporary Village travel with the persistent currency account
- M22: Replace the old material/gear whitelist with the revised repair matrix
- M23: Require marked repair components and update Elias's catalogue
- M26: Implement direct shop repair and revised price formulas
- M30: Remove the global instant-brewing override and implement approved class behavior
- M36: Implement Tamsin’s persistent map and first-entry conversation
- M37: Replace incidental ready-order grouping with the specified party/invitation flow
- M42: Add the campfire placement, ownership, lifetime and interaction rules
- M61: Add Dragoon passive health-for-durability repair under the revised exclusions
- M62: Match Pyroclast transmutation to the explicit tool interaction
- M78: Track actual jukebox insertion/playback, not merely held-disc interaction
- M80: Connect Plant Flags completion to the intended JHW response
- M95: Disable JHW faction gating to match the retained NPC table
- M110: Apply the retained name-only achievement presentation rule
- M111: Make the First Trace reward and achievement one successful idempotent operation
- M112: Close the unrestricted Metalmancer class-toggle and ore-set command path
- M113: Protect mutable door and destination authoring commands consistently
- M116: Enforce managed-equipment repair restrictions in ordinary interfaces
- M117: Round selected repair percentage once and preserve the approved over-repair choice

## Preserved authored content; verification pending (9)

- M64: Reconcile every Bogatyr loadout and named-item row
- M65: Reconcile every Dragoon loadout and named-item row
- M66: Reconcile every Judicator loadout and named-item row
- M67: Reconcile every Pyroclast loadout and named-item row
- M68: Reconcile every Theurgist loadout and named-item row
- M69: Reconcile every Venefex loadout and named-item row
- M70: Reconcile every Metalmancer loadout and named-item row
- M71: Reconcile every Deadeye loadout and named-item row
- M103: Preserve existing spawner data when retained encounters or drops change

## Deferred D2+; notes only (27)

- M27: Finish magnet projectile and chain-attack behavior
- M28: Finish recall and active reforging
- M29: Make summon time a channel, and reconcile tier-specific regeneration
- M38: Enable Deadeye and Metalmancer at their approved D2 progression point
- M45: Implement the three scapula recruitment relics and assigned boss drops
- M46: Implement all three pack totem auras without same-class stacking
- M47: Implement four wolf armor tiers, recipes, repairs and totem synergies
- M48: Unify Metalmancer’s four-tier staff and golem statistics
- M49: Implement the complete ore/rest/heal exchange, not only doubled idle income
- M50: Implement the individual Metalmancer equipment modifiers and stacking policy
- M51: Validate held equipment and action context on every Metalmancer packet
- M52: Make one-golem ownership and link state robust across unload/restart
- M53: Finish bag tier capacities and safe inventory/ore persistence
- M54: Separate standard brewing from the proposed class-specific brewing station
- M56: Implement the approved Thanatropic Conduit / Binding Idol lifecycle
- M57: Deliver the Theurgist story arc without leaking internal epilogue knowledge
- M59: Add Deadeye zoom, draw and range-based damage progression
- M73: Populate all player-facing Codex and item descriptions with reveal controls
- M74: Correct Vital Exchange I–IV to real transfer events and the right tier identities
- M75: Wire Binding Idol provider/return milestones into actual gameplay
- M89: Build the remaining D2 rooms, encounters and progression hand-off
- M90: Implement D3’s water/pressure puzzles and Dagon–Hydra state sequence
- M91: Implement D4’s portal maze, reflection encounters and Web completion
- M92: Implement D5’s trap construction, boss endings and persistent outcomes
- M96: Create a checked-in approved language/codex corpus and lint inconsistent examples
- M97: Implement Webbound Priest journal drops and staged reading
- M108: Keep explicit future proposals and incompatible candidates outside automatic implementation

## Next work (await Cameron)

34. Review authored mob reward registrations and stable NPC/profile bindings against current sources.
Do not guess spawner ownership or mutate live placements. Native world changes require explicit scope.
STOP after the validated32-33 local commit. Batch34 has not started.
The queued breakpoint remains: "finish what you're doing and stop".
No launch/GameTest/deployment/push or active-world/config edit occurred.

Before release: authored world/NPC/objective bindings, actual menus/mixins and all36 startup
pastes, legacy saves, full inventories/offline players, native save interruption and licensed multiplayer QA.
