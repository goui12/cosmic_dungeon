# D1 remaining work

Updated 2026-09-19 after Batch27. Cameron authorized Batch27 only, then a local commit and STOP.
Batch26 was committed as a11d98983c815ba2a26a3d7150afbcbe53531e2f before this pass.
Gameplay/multiplayer testing remains deferred for a cumulative pass; static/build checks continue.
All101 original audit IDs remain intact. Code implementation is not runtime acceptance.
Detailed current dispositions: Google Docs and Sheet/Audit/D1_Batch_27_2026-09-19/implementation_findings.json.
Latest completion and manual QA: [Batch27](D1_BATCH_27.md). Prior: [Batch26](D1_BATCH_26.md).
Historical completion: [Batches23-25](D1_BATCHES_23-25.md).

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
- M72: Extend trusted identity adoption to reviewed legacy containers and drop definitions
- M03: Complete final inventory handoff and legacy currency boundaries; Inn/travel decisions implemented
- M79: Complete remaining achievement and reward bindings

## Other D1 follow-ups (20)

- M02: Finish threshold-delivery recovery, legacy cap review and operator disposition
- M05: Replace health-derived payouts with approved mob reward definitions
- M09: Verify native balance-display resync and stale-packet behavior
- M12: Complete production-cost/conversion review and named-drop runtime pricing acceptance
- M17: Correct the Beatrix/Beluzon profile roles and unlock mapping
- M20: Review ambiguous legacy Chops without deleting counts, components or ownership evidence
- M41: Establish stable NPC identity and global spawn/personal-access separation
- M43: Finish final lifecycle handoff and native Chop travel acceptance; journey journal implemented
- M55: Implement exact restorative/vision/marking arrow identities and tier effects
- M58: Add the approved Venefex attack, debuff and movement mechanics
- M60: Reconcile Dragoon trident enchantments and chain lightning with bounded targeting
- M63: Align Pyroclast rockets and launchers with exact payload/damage rules
- M81: Reconcile every D1 quest, region, key and startup-room binding
- M93: Implement physical progression-item retention under the approved exit policy
- M101: Validate personal travel gates and existing destination isolation
- M102: Journal end-of-run inventory handoff and verify startup/class-room rollback
- M104: Put explicit budgets around new AI, auras, snapshots and client effects
- M105: Update help, commands and player terminology only alongside approved behavior
- M106: Validate the retained item catalogue and repair broken references
- M107: Use the linked calculator as a checked pricing reference, not a runtime web dependency

## Implemented; runtime acceptance pending (41)

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

28. Final D1 inventory handoff and startup rollback: M102/M43/M03; success/failure/offline cleanup receipts.
    Keep legacy Chop review (M20) and existing placed NPC identity (M17) visible as separate follow-ups.

STOP after the validated Batch27 local commit. Batch28 is not started or authorized in this pass.
Remaining24 partial D1 IDs stay listed above; later batch sizing is not new authorization.
The queued breakpoint remains: "finish what you're doing and stop".
Gameplay/multiplayer testing is deferred to a cumulative pass. Focused code/build checks continue.
Deployment/game launches and actual world migrations still require their existing authorization.

Before runtime release: authored world/NPC/objective bindings, actual menu/mixin behavior,
old saves, failure resets, packet replay, receipt recovery and licensed multiplayer QA.
