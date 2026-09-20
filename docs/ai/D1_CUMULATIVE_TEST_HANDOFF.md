# D1 cumulative licensed TEST handoff after Batch 38

Prepared only; every native acceptance item below is **NOT RUN**.
The current numbered implementation/review plan has no further batch scheduled.
This is not release acceptance: all101 audit IDs retain their explicit disposition.
M09 additionally records missing dedicated HUD/inventory/class-chest balance panels.
The remaining19 partial IDs and9 preservation checks are not silently marked complete;
27 D2+ implementation entries remain TODO-only. M70/M71 future-class authored-loadout
verification also stays outside D1 despite its preserved-content status.

[Batch38 changes](D1_BATCH_38.md) | [remaining-work register](D1_REMAINING.md) |
[Batch37 bindings](D1_BATCH_37_BINDINGS.md) |
[Batch33 item mappings](D1_BATCH_33_MAPPINGS.md)

## Setup and evidence

1. Obtain separate authorization for TEST deployment/launch. This document performs neither.
2. Back up the full TEST world/player data, mod/config files and authored preset files together.
   Preserve an untouched copy for old-save/rollback comparison and ambiguous legacy decisions.
3. Verify the existing Cosmic Dungeon ADMINISTRATIVE ACCESS ONLY client and licensed Goui12 login.
   Use TEST only; Cameron controls stop/start in Akliz. Preserve server.properties and all other mods.
4. Select the exact Batch38 candidate hash from its report, verify the same CosmicDungeon jar
   hash on client/server, and preserve the deployment rollback manifest. Protocol6 requires matching
   jars. Server1.21.10/NeoForge21.10.64; no authentication/EULA bypass or Dev-client substitute.
5. Record jar/hash, config diffs, world backup identity, player UUIDs/classes, run IDs, instance
   dimensions, timestamps and actual/expected results. Redact credentials; use bounded captures.
6. Complete authoring checks on the copy first. Unknown NPCs, chests, keys, drops and destinations
   are review items, not permission to replace authored content or auto-convert old inventory.

## Acceptance order

| Pass | Short steps and expected result |
| --- | --- |
| 1. Old worlds and bindings | Use Batch37 binding checklist. Load representative old spawners, weighted potentials, presets, doors, keys and rifts. Compare saved fields and equipment before/after restart. No placed-spawner recreation or guessed coordinates. |
| 2. D1 onboarding | With fresh and returning players, test Tamsin Yes/No, interrupted map/class selection, all six classes, three/six-player bounds, invitations/merges, personal readiness, full queue, disconnect during preparation and entry failure recovery. No charge for entry; -JHW map route remains visible. |
| 3. Authored equipment | Compare all retained D1 chest quantities and23 named drop signatures using Batch33 mappings. Test owner/class/tier restrictions, old aliases, preview/apply/undo, full inventory, cursor and nested/unloaded storage. Unknown provenance stays intact for review. D2 gear is excluded. |
| 4. Two simultaneous runs | Use separate instance IDs. Split/rejoin/AFK/disconnect players; verify objectives, flags, bells and spectral Blooms never cross runs. Complete/fail/reset one run while the other continues. Next run starts fresh; lifetime contributions, completion totals, vendor unlocks and account money persist. |
| 5. Watson and cleanup | Test missing/duplicate six physical Blooms, full group near Watson, successful/failed outcome, repeated interaction, reconnect, full inventory, outside-active Chop ownership and interrupted save boundaries. One durable outcome controls reward projections, Tax eligibility and final cleanup without duplicate grants. |
| 6. Account and rewards | Check12,345 Trace denominations, reward eligibility/radius, typed encounter payouts, remainder rotation, caps/threshold notices, over-cap historical balances and paired ledger supply. Cross-class/restart reads show one UUID account. Do not infer historical money or Watson grants. |
| 7. Death currency | Test owner/other-player recovery, cap rejection without loss, ordinary despawn/fire/void, portal/dimension/reset cases and save interruption. Logical drops cannot merge or enter containers; legacy physical coins remain review evidence. Reconcile account plus drop supply. |
| 8. Vendors and prices | Visit each unlocked NPC with different personal progress/faction/classes. Check quote lines, rejection reasons, zero-value surrender, account cap, exact payment, independent stock/morning restock, blocked sales and full inventory. Vendor purchase prices remain independent of retail specialization. |
| 9. Batch35 crafting and conversions | Exercise the actual server recipes/data packs, not just the reviewed vanilla subset: fuel, yield, bucket/bottle remainders, milk drinking, brewing, disabled inputs, enchantment/curses and existing caps. Check general-crafting permissions. Add a review case for every additional approved recipe before release. |
| 10. Trade and repair | Both players ready/confirm, alter offers, close menus, move away, die/disconnect and interrupt each documented save boundary. Recover every item/payment once; fee/cap/material checks remain server-side. Check repair timing/units/health and component ownership. |
| 11. Batch38 menu synchronization | Keep vendor/trade/repair open while rewards/admin capacity changes affect accounts. Compare /currency balance. Close/reopen rapidly, reconnect and switch menus; old/reused-container packets must not repaint a fresh opening. Resize at several GUI scales. No unchanged-state packet stream or selection loss on balance refresh. |
| 12. Inn and Chop | Test bond once, approved two-half beds, home fallback/stronger respawn, protected First Heart and bed edits. Cook Raw Chop at a lit campfire, go to Village, return, destroy campfire, fill inventory and interrupt handoffs. Resolve exact ownership/escrow evidence; no bulk legacy migration. |
| 13. Batch37 travel | Verify actual named Village aliases and destination permissions. Deny wrong-run/class, dead/outside, pending custody and reset bypasses. Companionship pays on drinking, rejects stale/unsafe targets, retains cooldown after failed travel and selects only valid same-run peers. Cover cancelled native teleports and both instance slots. |
| 14. Bogatyr | Tame/feed/breed/grow, cap including unloaded companions, call/recover/archive one pet, unload/restart/reset, expiry, targeting and aura ownership. Preserve names/equipment/ownership. Future tier totem auras remain disabled; actual high-density CPU cost still needs measurement. |
| 15. Batch36 D1 combat | Exercise eighteen ammo identities, ordinary ammo controls, restorative teammates with friendly fire off, shields, undead/spider immunity, stronger/equal overlapping effects, cancellation/reload, Venefex formulas, successful trident-only chain, rocket payload/team/self/wall/multishot and candidate saturation. |
| 16. Achievements and lore | Test name-only displays, six distinct bells/timing, qualifying candles/Withers, actual disc playback/stop, Plant Flags disconnect grace, journal attribution, fire/rest triggers, one-time World Spawn Elytra and six simultaneous Piglin-head characters. Tax does not reveal eligible payment items early. Check NPC lore timing. |
| 17. Spawner index native hooks | Compare capped/uncapped and one-shot behavior. Let tagged mobs wander, die/revive, unload/reload and cross visible/hidden chunk transitions. Edit Tags through /tag and /data on the world copy. Cancel/reject duplicate entity admission. Disabled/completed spawners remain inactive. Confirm index cleanup across level unload/server stop. |
| 18. Performance acceptance | On the same backed-up scenario/settings, record baseline and candidate server MSPT, client frame time, loaded entities, memory after repeated unloads and packet rate. Include many spawners, six-player parties, wolves, candles, brewing and outside-active inventories. Use existing on-demand diagnostics; no permanent agent or extra heap. Budgeted preset maintenance must make progress fairly; actual latency/allocation claims require these results. |
| 19. Recovery and rollback | Repeat approved save interruptions/restarts with all paired world/player/config data restored together. Reconcile items, account supply and receipts before continuing. Batch38 adds no save schema, but earlier batches do; a jar-only downgrade is not a safe rollback of the whole D1 pass. |

## Explicit follow-ups outside the finished numbered plan

- M09: dedicated HUD, ordinary inventory and class-chest account panels remain absent.
  Their source calls for the same five denominations/UUID balance, not a second account.
  Design client layout and cost before requesting the repository's required client-work approval.
- M03/M10/M17/M20/M72: old unreceipted/ambiguous data needs a consistent-backup review.
  Do not manufacture proof, auto-adopt every old item or grant inferred historical rewards.
- M12/M106: native/custom recipe coverage and three source links to the deleted Master Item Sheet
  remain review tasks. The source workbooks were not edited.
- M104: profile snapshot serialization before a dirty-tracking change; count-only tracking misses
  mutable components, damage and cursor changes and can undermine recovery.
- Every D2+ item remains source-backed TODO-only. Deferred work is not quietly enabled for TEST.

## All101 audit dispositions

Statuses below are code-review dispositions, never native test results.
See the code TODO register at src/main/java/net/goui/cosmicdungeon/dungeon/d1/package-info.java
and the private Batch38 implementation_findings.json for exact source-backed remaining requirements.

| ID | Disposition | Work item |
| --- | --- | --- |
| M01 | implemented_unverified | Raise the default account cap and define migration of overrides |
| M02 | implemented_unverified | Implement wealth alerts and economic review state |
| M03 | partial_D1 | Add an idempotent transaction ledger and supply reconciliation |
| M04 | implemented_unverified | Distinguish automatic-reward overflow from transaction overflow |
| M05 | partial_D1 | Replace health-derived payouts with approved mob reward definitions |
| M06 | implemented_unverified | Use canonical 60-block and dead-player eligibility |
| M07 | implemented_unverified | Preserve and rotate split remainders |
| M08 | implemented_unverified | Implement the canonical death debit and protected logical drop |
| M09 | partial_D1 | Audit all balance displays without creating extra stores |
| M10 | partial_D1 | Enforce sale/transfer provenance before pricing |
| M11 | implemented_unverified | Support the approved universal purchase catalogue |
| M12 | partial_D1 | Implement authoritative base, enchantment and production-cost pricing |
| M13 | implemented_unverified | Rebuild shop inventories and costs from the approved vendor lists |
| M14 | implemented_unverified | Replace lifetime caps with per-player morning stock where required |
| M15 | implemented_unverified | Add the distinct NPC/vendor faction and transaction-time effects |
| M16 | implemented_unverified | Wire Lesser Bloom gains and attributable NPC-death penalties |
| M17 | partial_D1 | Correct the Beatrix/Beluzon profile roles and unlock mapping |
| M18 | implemented_unverified | Wire persistent acquisition and canonical completion rounding |
| M19 | implemented_unverified | Complete the Torch Flower success and permanent-reward integration |
| M20 | partial_D1 | Choose and migrate one Chop/campfire/travel state machine |
| M21 | implemented_unverified | Reconcile temporary Village travel with the persistent currency account |
| M22 | implemented_unverified | Replace the old material/gear whitelist with the revised repair matrix |
| M23 | implemented_unverified | Require marked repair components and update Elias's catalogue |
| M24 | implemented_unverified | Implement the timed two-party repair protocol |
| M25 | implemented_unverified | Make repair reservation/cancellation and ownership recovery durable |
| M26 | implemented_unverified | Implement direct shop repair and revised price formulas |
| M27 | deferred_D2_plus | Finish magnet projectile and chain-attack behavior |
| M28 | deferred_D2_plus | Finish recall and active reforging |
| M29 | deferred_D2_plus | Make summon time a channel, and reconcile tier-specific regeneration |
| M30 | implemented_unverified | Remove the global instant-brewing override and implement approved class behavior |
| M36 | implemented_unverified | Implement Tamsin’s persistent map and first-entry conversation |
| M37 | implemented_unverified | Replace incidental ready-order grouping with the specified party/invitation flow |
| M38 | deferred_D2_plus | Enable Deadeye and Metalmancer at their approved D2 progression point |
| M39 | implemented_unverified | Build The Tamsin Tax as a durable, zero-payout turn-in |
| M40 | implemented_unverified | Implement Beluzon’s Inn bed binding and protected Heart relationship |
| M41 | partial_D1 | Establish stable NPC identity and global spawn/personal-access separation |
| M42 | implemented_unverified | Add the campfire placement, ownership, lifetime and interaction rules |
| M43 | implemented_unverified | Make Chop return entitlement and full-inventory failure owner-safe |
| M44 | implemented_unverified | Implement Bogatyr companion ownership, cap and lifecycle |
| M45 | deferred_D2_plus | Implement the three scapula recruitment relics and assigned boss drops |
| M46 | deferred_D2_plus | Implement all three pack totem auras without same-class stacking |
| M47 | deferred_D2_plus | Implement four wolf armor tiers, recipes, repairs and totem synergies |
| M48 | deferred_D2_plus | Unify Metalmancer’s four-tier staff and golem statistics |
| M49 | deferred_D2_plus | Implement the complete ore/rest/heal exchange, not only doubled idle income |
| M50 | deferred_D2_plus | Implement the individual Metalmancer equipment modifiers and stacking policy |
| M51 | deferred_D2_plus | Validate held equipment and action context on every Metalmancer packet |
| M52 | deferred_D2_plus | Make one-golem ownership and link state robust across unload/restart |
| M53 | deferred_D2_plus | Finish bag tier capacities and safe inventory/ore persistence |
| M54 | deferred_D2_plus | Separate standard brewing from the proposed class-specific brewing station |
| M55 | partial_D1 | Implement exact restorative/vision/marking arrow identities and tier effects |
| M56 | deferred_D2_plus | Implement the approved Thanatropic Conduit / Binding Idol lifecycle |
| M57 | deferred_D2_plus | Deliver the Theurgist story arc without leaking internal epilogue knowledge |
| M58 | partial_D1 | Add the approved Venefex attack, debuff and movement mechanics |
| M59 | deferred_D2_plus | Add Deadeye zoom, draw and range-based damage progression |
| M60 | partial_D1 | Reconcile Dragoon trident enchantments and chain lightning with bounded targeting |
| M61 | implemented_unverified | Add Dragoon passive health-for-durability repair under the revised exclusions |
| M62 | implemented_unverified | Match Pyroclast transmutation to the explicit tool interaction |
| M63 | partial_D1 | Align Pyroclast rockets and launchers with exact payload/damage rules |
| M64 | preserved_verification_pending | Reconcile every Bogatyr loadout and named-item row |
| M65 | preserved_verification_pending | Reconcile every Dragoon loadout and named-item row |
| M66 | preserved_verification_pending | Reconcile every Judicator loadout and named-item row |
| M67 | preserved_verification_pending | Reconcile every Pyroclast loadout and named-item row |
| M68 | preserved_verification_pending | Reconcile every Theurgist loadout and named-item row |
| M69 | preserved_verification_pending | Reconcile every Venefex loadout and named-item row |
| M70 | preserved_verification_pending | Reconcile every Metalmancer loadout and named-item row |
| M71 | preserved_verification_pending | Reconcile every Deadeye loadout and named-item row |
| M72 | partial_D1 | Create a canonical named-item factory/catalogue with provenance |
| M73 | deferred_D2_plus | Populate all player-facing Codex and item descriptions with reveal controls |
| M74 | deferred_D2_plus | Correct Vital Exchange I–IV to real transfer events and the right tier identities |
| M75 | deferred_D2_plus | Wire Binding Idol provider/return milestones into actual gameplay |
| M76 | implemented_unverified | Require distinct successful bell actions for Synchronous Peal |
| M77 | implemented_unverified | Make Sixfold Vigil lighting and variant credit match authored state |
| M78 | implemented_unverified | Track actual jukebox insertion/playback, not merely held-disc interaction |
| M79 | implemented_unverified | Complete remaining achievement and reward bindings |
| M80 | implemented_unverified | Connect Plant Flags completion to the intended JHW response |
| M81 | partial_D1 | Reconcile every D1 quest, region, key and startup-room binding |
| M89 | deferred_D2_plus | Build the remaining D2 rooms, encounters and progression hand-off |
| M90 | deferred_D2_plus | Implement D3’s water/pressure puzzles and Dagon–Hydra state sequence |
| M91 | deferred_D2_plus | Implement D4’s portal maze, reflection encounters and Web completion |
| M92 | deferred_D2_plus | Implement D5’s trap construction, boss endings and persistent outcomes |
| M93 | partial_D1 | Implement physical progression-item retention under the approved exit policy |
| M95 | implemented_unverified | Disable JHW faction gating to match the retained NPC table |
| M96 | deferred_D2_plus | Create a checked-in approved language/codex corpus and lint inconsistent examples |
| M97 | deferred_D2_plus | Implement Webbound Priest journal drops and staged reading |
| M101 | partial_D1 | Validate personal travel gates and existing destination isolation |
| M102 | implemented_unverified | Make startup preparation failure-safe and verify every class room paste |
| M103 | preserved_verification_pending | Preserve existing spawner data when retained encounters or drops change |
| M104 | partial_D1 | Put explicit budgets around new AI, auras, snapshots and client effects |
| M105 | partial_D1 | Update help, commands and player terminology only alongside approved behavior |
| M106 | partial_D1 | Validate the retained item catalogue and repair broken references |
| M107 | implemented_unverified | Use the linked calculator as a checked pricing reference, not a runtime web dependency |
| M108 | deferred_D2_plus | Keep explicit future proposals and incompatible candidates outside automatic implementation |
| M110 | implemented_unverified | Apply the retained name-only achievement presentation rule |
| M111 | implemented_unverified | Make the First Trace reward and achievement one successful idempotent operation |
| M112 | implemented_unverified | Close the unrestricted Metalmancer class-toggle and ore-set command path |
| M113 | implemented_unverified | Protect mutable door and destination authoring commands consistently |
| M114 | implemented_unverified | Enforce item transfer eligibility at trade insertion and final confirmation |
| M115 | implemented_unverified | Make trade item delivery and cancellation durable and owner-safe |
| M116 | implemented_unverified | Enforce managed-equipment repair restrictions in ordinary interfaces |
| M117 | implemented_unverified | Round selected repair percentage once and preserve the approved over-repair choice |
| M118 | implemented_unverified | Separate a legitimate zero-Trace surrender from missing-price rejection |
