/*
 * D1 canon implementation follow-up register (2026-09-16).
 * Authority: direct Cameron instructions, current Q&A, then newest applicable
 * documents linked by the debloated MASTER. All runtime acceptance is pending.
 * Entries intentionally contain notes only; they do not enable deferred features.
 * See docs/ai/D1_IMPLEMENTATION_20260916.md for operating and validation limits.
 *
 * TODO(D1 multiplayer acceptance): Run only when authorized, using matching client
 * and TEST-server jars, world backups and the licensed player session. Validate
 * inventory, currency, death, reconnect, menus, effects, reset and old-save loading.
 * Datagen and offline fixtures do not prove multiplayer or crash-atomic behavior.
 *
 * TODO(M02, licensed TEST acceptance): Verify durable wealth alerts and economic review state
 * Current: Batch31 inbox shares each account/ledger decision; developer/direct-console ack,
 * resolve and reopen use audited expected revisions. Offline/reconnect delivery is bounded
 * and waits for native save readback. Legacy observations preserve balances, old markers and
 * original final evidence. /currency review pending and review <UUID> expose the durable state.
 * Required: verify actual save failures, offline developers, reconnect, concurrent reviewers,
 * stale permission/session attempts and debits at cap. Chat may repeat until acknowledgment;
 * never describe network delivery as exactly-once or reset lifetime first-threshold evidence.
 * Source: https://docs.google.com/document/d/17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28
 * Source modified: 2026-08-18T21:01:23.864Z
 *
 * TODO(M03, partial_D1): Add an idempotent transaction ledger and supply reconciliation
 * Current: Account reservations and owner-file receipts now cover repair, trade, vendor sales,
 * retail and direct repairs. Paged native-NBT evidence retains exact items and quote breakdowns;
 * a verified account outbox survives archive interruptions. Daily/lifetime account supply reconciles.
 * Batch26: logical death intent/debit/drop/pickup/despawn share the same account image; active
 * drops join supply totals. Only ordinary destruction or dimension reset is a sink.
 * Batch27: Inn fee/bed readback and committed Chop inventory/location/ownership recovery.
 * Batch28: saved D1 cleanup/claim receipts, offline escrow handoff and full-roster entry rollback.
 * Batch29: Watson's saved six-Bloom outcome precedes owner deductions and receipt-protected
 * lifetime/progression/faction projections; settled outcomes alone authorize final cleanup.
 * Required: older physical-currency pickup migration and native interrupted-save acceptance.
 * Q&A D20/D23 (2026-09-16) supersedes older failure-retained statistics/three-flower thresholds.
 * Legacy Watson flags and historical failed-run Lesser totals lack receipts: review complete
 * backups without replaying grants, subtracting guessed totals or changing completed-run history.
 * Preserve uncertain partial/manual-restored records for full-save review; never guess replacements.
 * Batch32: legacy physical denomination pickup is review-only, with no deposit/discard pair.
 * Bounded /currency legacy inspection exposes nominal items and retained receipt counts without
 * awarding money. Exact retired-run intervals replace only known finished-run mob receipt rows;
 * native account round trips reject later replay while preserving active-run and lifetime evidence.
 * TODO(M03, bounded account history): paired/operation terminal IDs remain retained after their
 * large item plans are acknowledged. Archive/prune only after an exact online replay index exists.
 * Never reconstruct previously pruned runs, old pickups or missing historical grants from balances.
 * Full ledger evidence remains archived; measure long-run save size during licensed TEST.
 * TODO(M02, legacy TEST fixtures): exercise pre-ledger over-cap balances, existing capacity
 * overrides, old delivery markers after spending, and immediate debit/reset before polling.
 * Batch31 labels these observations honestly and retains any surviving original final transaction.
 * Confirm complete-backup upgrade/rollback and failed native writes before runtime acceptance;
 * never infer original earning dates or restore mismatched player/account/world files.
 * Source: https://docs.google.com/document/d/17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28
 * Source modified: 2026-08-18T21:01:23.864Z
 * Source: https://docs.google.com/document/d/1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc
 * Source modified: 2026-08-18T22:35:34.052Z
 *
 * TODO(M05, partial_D1): Replace health-derived payouts with approved mob reward definitions
 * Current: Spawner-tagged D1 enemies use explicit registered reward defaults, optional encounter
 * overrides and zero for unknown/ambiguous entries.
 * Required: Verify each authored encounter's category and payout against newest linked encounter
 * Docs on a world copy. Preserve placed spawner NBT and presets; do not infer value from health or
 * replace spawners.
 * Source: https://docs.google.com/document/d/17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28
 * Source modified: 2026-08-18T21:01:23.864Z
 *
 * TODO(M08, implemented_unverified): licensed TEST acceptance for logical death currency.
 * Current: configurable threshold/floor/minimum; one UUID-bound logical drop owned by the account
 * save, carrying owner, amount, death transaction, run, timestamp and native item image.
 * Anyone with full receiving capacity can collect once. Native aging, expiry hooks, physics and
 * damage remain; no inventory/hopper/mob pickup, merging, or independent world-file currency copy.
 * Required: actual death cancellation, keepInventory, concurrent pickup, cap changes, unload,
 * restart, portals, ordinary expiry/fire/void, IO failures and occupied/failed/successful resets.
 * Verify native life-token/player saves on dedicated and integrated servers; restore all save
 * files together. Normal checkpoints preserve last saved age/position, not unsaved crash-time ticks.
 * Source: https://docs.google.com/document/d/17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28
 * Source modified: 2026-08-18T21:01:23.864Z
 *
 * TODO(M09, partial_D1): Audit all balance displays without creating extra stores
 * Batch38: commands/vendor/trade/repair still read the same UUID account and denomination formatter.
 * Open menus poll displayed scalar balances (Performance.menuBalancePollTicks, default20);
 * unchanged values send nothing. Native menu opening nonces reject stale/reused-container state,
 * including vendor results/quotes and trade/repair state. Caches are transient display snapshots.
 * Required: native reconnect, GUI-scale, external reward and capacity-change acceptance in TEST.
 * Source display rule also names HUD, ordinary inventory and class-chest surfaces: these do not
 * yet have a dedicated balance panel. Defer that client layout/performance design explicitly;
 * use /currency balance meanwhile. Do not create a second account or reset currency with a run.
 * Cameron2026-09-20 asks about reusing the existing trade denomination widgets: proposed
 * read-only shared renderer plus a delta-synchronized account view, not a PNG containing values.
 * The account balance is not loot. Physical chest-reward credit needs a separate reviewed
 * unpaid-reward identity and durable claim, never automatic conversion of old denomination stacks.
 * Source: https://docs.google.com/document/d/17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28
 * Source modified: 2026-08-18T21:01:23.864Z
 *
 * TODO(M10, partial_D1): Enforce sale/transfer provenance before pricing
 * Current: Transfer rules reject known bound/attuned/class-marked gear, quest items, Blooms, Chops
 * and recognized D1 ability ammunition.
 * Batch 04: trusted origin/identity is required for equipment sale and trade. Unknown or mismatched
 * provenance is rejected. Named prices never use display text. No-sale and no-trade apply to
 * their respective operations, while class/bound/no-drop restrictions continue to block both.
 * Required: reviewed adoption of legacy class-issued/loot stock, including unloaded containers.
 * Batch 06 blocks voluntary protected drops and unsafe menu/portable-container insertion before
 * mutation. Developer preview/apply/undo protects exact original held or loaded-container stacks.
 * Preserve quantities/components; finish death/clone, cursor spill, automated and owner-pickup routes.
 * Batch33: reviewed vanilla ammunition can receive one stable ability marker through held
 * preview/apply/undo; exact components/counts and D1 class metadata are retained. Unknown markers,
 * wrong bases/potions and foreign registered items cannot fall back to name-based abilities.
 * Legacy recognized ammunition now shares no-drop/private-storage protections. New named loot
 * adoption checks all23 approved enchantment signatures; existing receipts are not rewritten.
 * Unloaded/preset adoption still requires reviewed authoring mappings; never launder identity.
 * Six class source lists, uncertain names and drop-placement gaps are recorded in
 * docs/ai/D1_BATCH_33_MAPPINGS.md. Q&A D79 prohibits automatic chest or quantity edits.
 * Source: https://docs.google.com/document/d/1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc
 * Source modified: 2026-08-18T22:35:34.052Z
 * Source: https://docs.google.com/document/d/1aDUTh-_AmrB3kMHKeyTDQKdIeJHBtqdyp11FPBg3vmY
 * Source modified: 2026-08-23T20:33:27.547Z
 * Source: https://docs.google.com/document/d/1Gbcq7Piqg2uHO1smx5oHOyeGxoT9g93cH-_G8WvhOoo
 * Source modified: 2026-08-18T22:32:42.870Z
 *
 * TODO(M12, partial_D1): Implement authoritative base, enchantment and production-cost pricing
 * Current: 141 base/repair/potion price entries and 41 enchantment entries use newest Pricing
 * Master; returned-container caps close known milk/honey/alchemy value loops.
 * Batch 04: 23 named D1 final purchase prices, including the 900-Trace Spyglass, require
 * trusted identities and use the newer 2026-08-28 named-loot table. No double enchantment premium.
 * Required: audit remaining crafting, brewing and container conversions against production costs.
 * Ordinary renamed items never acquire named payouts; legacy equipment adoption remains M72.
 * Source: https://docs.google.com/document/d/1B3hQLrrOkZeRPG1tomd54rd7v7OKQ_PDNozHH-DIffY
 * Source modified: 2026-08-19T21:16:19.213Z
 * Source: https://docs.google.com/document/d/1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc
 * Source modified: 2026-08-18T22:35:34.052Z
 * Source: https://docs.google.com/document/d/1Gbcq7Piqg2uHO1smx5oHOyeGxoT9g93cH-_G8WvhOoo
 * Source modified: 2026-08-18T22:32:42.870Z
 *
 * TODO(M17, partial_D1): Correct the Beatrix/Beluzon profile roles and unlock mapping
 * Current: Beatrix handles food/Chops; native Beluzon provides the fixed Inn service without
 * ordinary buyback or faction discount.
 * Required: Verify legacy placed vendor profiles and native NPC registrations on a world copy;
 * remove conflicting service bindings only after identity-safe migration. Gritch's legacy profile
 * lacks a retained role Doc and needs world/source confirmation.
 * Source: https://docs.google.com/document/d/1aDUTh-_AmrB3kMHKeyTDQKdIeJHBtqdyp11FPBg3vmY
 * Source modified: 2026-08-23T20:33:27.547Z
 * Source: https://docs.google.com/document/d/1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I
 * Source modified: 2026-08-19T21:23:31.822Z
 *
 * TODO(M20, partial_D1): Lossless legacy Chop review
 * Current: Q&A D01/D02/D24/D25 (Sep16) selects one personal round-trip token and return-to-Raw.
 * Batch27 journals single held untagged Raw adoption; duplicate/overstacked/foreign/old Cooked
 * stacks remain intact. Cooking and Raw conversion retain unrelated custom components.
 * Required: Explicit developer preview/apply for ambiguous old stacks and orphan return targets.
 * Never shrink/delete extras or invent ownership from a display name; retain exact old escrow.
 * Test dropped and Ender Chest copies, menus, death outside the instance and full inventories.
 * Source: https://docs.google.com/document/d/1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I
 * Source modified: 2026-08-19T21:23:31.822Z
 * Source: https://docs.google.com/document/d/18McISr9dcvMGp3-VjLDVsISosKBfkaidUTPzHxlw1Pc
 * Source modified: 2026-07-07T18:23:05.554Z
 *
 * TODO(M24, partial_D1): Implement the timed two-party repair protocol
 * Current: Two-party Ready, range checks, channel timing, selected percentage and completion
 * validation.
 * Required: Replace logical availability checks with durable component/currency reservations and
 * quoted fingerprints. Prove cancellation, disconnect and concurrent-action behavior; join the
 * journal described in M25 rather than creating another transaction system.
 * Source: https://docs.google.com/document/d/1Gbcq7Piqg2uHO1smx5oHOyeGxoT9g93cH-_G8WvhOoo
 * Source modified: 2026-08-18T22:32:42.870Z
 *
 * TODO(M25, partial_D1): Make repair reservation/cancellation and ownership recovery durable
 * Current: Repair-owner escrow supports normal save/restart and full-inventory claims.
 * Required: Implement a durable reservation/commit journal across account, component, repaired item
 * and player saves. Recover exactly once at every crash boundary, including after debit and before
 * delivery. Normal SavedData escrow alone is not power-loss atomicity.
 * Source: https://docs.google.com/document/d/1Gbcq7Piqg2uHO1smx5oHOyeGxoT9g93cH-_G8WvhOoo
 * Source modified: 2026-08-18T22:32:42.870Z
 * Source: https://docs.google.com/document/d/1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc
 * Source modified: 2026-08-18T22:35:34.052Z
 *
 * TODO(M27, deferred_D2_plus): Finish magnet projectile and chain-attack behavior
 * Current: Metalmancer is unavailable in D1.
 * Required: Implement the newest magnet/recall/channel/regen rules only in the appropriate later
 * dungeon. Preserve staff/golem IDs and migrate any existing state; put every damage, range,
 * cooldown and ore modifier under Metalmancer in CosmicDungeon.config.
 * Source: https://docs.google.com/document/d/1hlDeYD6NQZGEn8UUzifas_KERzWvsf7D_-_KB33zHaQ
 * Source modified: 2025-10-26T23:53:41.488Z
 *
 * TODO(M28, deferred_D2_plus): Finish recall and active reforging
 * Current: Metalmancer is unavailable in D1.
 * Required: Implement the newest magnet/recall/channel/regen rules only in the appropriate later
 * dungeon. Preserve staff/golem IDs and migrate any existing state; put every damage, range,
 * cooldown and ore modifier under Metalmancer in CosmicDungeon.config.
 * Source: https://docs.google.com/document/d/1hlDeYD6NQZGEn8UUzifas_KERzWvsf7D_-_KB33zHaQ
 * Source modified: 2025-10-26T23:53:41.488Z
 * Source: https://docs.google.com/document/d/1bUZK8z3JtYpixhB8kBCwGT3SREqnef7z5sAZeYU-IsM
 * Source modified: 2025-10-16T01:24:44.970Z
 *
 * TODO(M29, deferred_D2_plus): Make summon time a channel, and reconcile tier-specific regeneration
 * Current: Metalmancer is unavailable in D1.
 * Required: Implement the newest magnet/recall/channel/regen rules only in the appropriate later
 * dungeon. Preserve staff/golem IDs and migrate any existing state; put every damage, range,
 * cooldown and ore modifier under Metalmancer in CosmicDungeon.config.
 * Source: https://docs.google.com/document/d/1hlDeYD6NQZGEn8UUzifas_KERzWvsf7D_-_KB33zHaQ
 * Source modified: 2025-10-26T23:53:41.488Z
 * Source: https://docs.google.com/document/d/1bUZK8z3JtYpixhB8kBCwGT3SREqnef7z5sAZeYU-IsM
 * Source modified: 2025-10-16T01:24:44.970Z
 *
 * TODO(M36, runtime acceptance): Verify Tamsin's first-entry conversation in TEST
 * Current: UUID-backed Yes/No acceptance, menu-only winding map/Base Camp/-JHW,
 * developer NPC-to-selector binding, six-class flow and returning-player skip are implemented.
 * Required: Bind the authored Starting Area NPC, verify decline/reopen/restart, session expiry,
 * movement, NPC unload/rebind and old/wrong-container packets with matching jars. Preserve
 * authored NPCs and chests. M37 and M39 now require licensed runtime acceptance.
 * Source: https://docs.google.com/document/d/1-FcHP73pFytPfoM2KhUPa6tt_2licsgWmWokto4YzE4
 * Source modified: 2026-08-19T22:35:48.808Z; live metadata rechecked 2026-09-17.
 *
 * TODO(M37, runtime acceptance): Verify invited groups and leader-controlled queue in TEST
 * Current: Explicit leader invitations, pending personal onboarding, roster-wide readiness reset,
 * personal confirmations, FIFO queue, countdown and locked preparation now replace shared Ready order.
 * Required: Exercise 3-6 players, first-time invitees, full/expired invitations, disconnects,
 * class/range/role changes, stale containers/revisions, occupied slots, and every preparation failure.
 * Ending another run must not clear groups waiting at the same selector. Party lobby state clears
 * on server stop; active run SavedData and instance recovery are unchanged. Q&A D24 forbids merges.
 * Source: https://docs.google.com/document/d/1-FcHP73pFytPfoM2KhUPa6tt_2licsgWmWokto4YzE4
 * Source modified: 2026-08-19T22:35:48.808Z; revalidated unchanged on 2026-09-17.
 *
 * TODO(M38, deferred_D2_plus): Enable Deadeye and Metalmancer at their approved D2 progression point
 * Current: Deadeye and Metalmancer remain unavailable in D1.
 * Required: Use approved D2 progression to unlock classes; define persistence and return-to-D1
 * policy from newest authority before enabling. Class packets must revalidate the server-side
 * unlock.
 * Source: https://docs.google.com/document/d/1Y1T-L7qRv3GWr11fcq9vuq_rO6yORnbqc1WmYplGVWg
 * Source modified: 2025-10-03T22:50:35.453Z
 * Source: https://docs.google.com/document/d/1oXHIKtIdWlQDwzf1JaHzBut08g4tytXs6rCYRY-89NY
 * Source modified: 2025-08-22T22:56:40.140Z
 * Source: https://docs.google.com/document/d/16Eq0yp7NTI57AK22s8vXrkoRYFp6BYrcdNjfbehWrcw
 * Source modified: 2026-01-18T15:54:24.799Z
 * Source: https://docs.google.com/document/d/1-FcHP73pFytPfoM2KhUPa6tt_2licsgWmWokto4YzE4
 * Source modified: 2026-08-19T22:35:48.808Z
 *
 * TODO(M39, licensed TEST acceptance): Verify The Tamsin Tax and world bindings.
 * Current: personal Base Camp discovery then qualifying success, frozen 23-item allowlist,
 * all-carried-item selection, expiring exact-stack confirmation, no payout/stock effects.
 * Removed item and receipt share one vanilla player snapshot; award replay never removes again.
 * Integrated owners also require verified level.dat Data.Player. Unknown receipts fail closed.
 * Required: bind the authored base_camp position and Starting Area NPC; test visit/success order,
 * old-save missing proof, clone, damaged/traded loot, cancellation, stale/replayed packets, full
 * inventory, disk errors and hard restart before/after every persistence/award boundary.
 * Shared cross-account currency/trade journaling is still M03/M115, not completed by this path.
 * Source: https://docs.google.com/document/d/1dIuaeMFMZaaWo7AS1zQ51kXSbdPkb9tmUm864MBs2Q0
 * Source modified: 2026-08-19T22:32:29.955Z; live rechecked 2026-09-18.
 * Source: https://docs.google.com/document/d/1fX1UbC6cG_cnN2auDo_1ascDY24yjFy9pIfL4qe3-_g
 * Source modified: 2026-08-28T16:49:24.607Z; live rechecked 2026-09-18.
 * Source: https://docs.google.com/document/d/1-FcHP73pFytPfoM2KhUPa6tt_2licsgWmWokto4YzE4
 * Source modified: 2026-08-19T22:35:48.808Z; live rechecked 2026-09-18.
 *
 * TODO(M40, implemented_unverified): Licensed Inn/First Heart acceptance
 * Current: Verified one-time 15-Trace bond, personal bed save and free bed changes; both halves
 * must be intact inside the Inn. Existing valid respawn wins; unavailable worlds may fall back.
 * Batch27 covers piston sources/destinations, fluids, burning/lava ignition, explosions and
 * hostile block destruction/spawning. Protected Hearts cannot spawn protectors outside the region.
 * Required: Bind/inspect the existing native Creaking, Pale Oak pillar, Heart and approved beds
 * on a backed-up TEST copy; run a full night and native respawn/save interruption checks.
 * No automatic world/bed/NPC replacement, mob-entry wall or developer-command rewrite.
 * Source: https://docs.google.com/document/d/1FT6k2MFKgQf_tQ5UcBn0wmqJ9-yY_Wdpjna4USdVOZA
 * Source modified: 2026-08-29T14:49:42.290Z
 * Source: https://docs.google.com/document/d/1EHoM1a5Yui1hSj8nC6MNDnfo99URDKAxdhv0IwidpXg
 * Source modified: 2026-07-10T21:40:00.498Z
 *
 * TODO(M41, partial_D1): Establish stable NPC identity and global spawn/personal-access separation
 * Current: Vendor menus bind actual NPC UUID, dimension and profile; Watson/Beluzon validate
 * identity.
 * Required: Catalogue remaining authored NPC entity/profile bindings and distinguish shared spawn
 * visibility from personal unlocks. Migrate persistent identities without duplicating or replacing
 * unrelated villagers/creakings; verify unload/restart.
 * Source: https://docs.google.com/document/d/1x59OaQfNB1UNYXgLySdq5BBktq9brgcBBbqLeJYpBvU
 * Source modified: 2026-08-20T20:25:49.056Z
 * Source: https://docs.google.com/document/d/11Cwgha2loiAQfMJwKLWEC_dD3jir3VybyfFvrNZBUyY
 * Source modified: 2026-08-18T21:07:27.966Z
 * Source: https://docs.google.com/document/d/1b0X-DFkmqheomsfC2ubajo4_iDQ9UlO9-YGPbwRryCE
 * Source modified: 2026-08-19T22:12:52.000Z
 * Source: https://docs.google.com/document/d/1FT6k2MFKgQf_tQ5UcBn0wmqJ9-yY_Wdpjna4USdVOZA
 * Source modified: 2026-08-29T14:49:42.290Z
 * Source: https://docs.google.com/document/d/1r-XtjgWAMHpYamn4_D2vmNTmHH0-23JP62Bac0tyq8E
 * Source modified: 2026-08-28T22:19:34.289Z
 * Source: https://docs.google.com/document/d/1LwIctAvogSULFfP79QTMhOZtBBQj-mKqaxA-t7jksTc
 * Source modified: 2026-08-19T20:44:48.175Z
 * Source: https://docs.google.com/document/d/1SjIUILpITN8k1NiwWZxPVmJV8FMNTboYA6bz09w8zMs
 * Source modified: 2026-08-17T23:08:52.568Z
 * Source: https://docs.google.com/document/d/1-FcHP73pFytPfoM2KhUPa6tt_2licsgWmWokto4YzE4
 * Source modified: 2026-08-19T22:35:48.808Z
 * Source: https://docs.google.com/document/d/1aDUTh-_AmrB3kMHKeyTDQKdIeJHBtqdyp11FPBg3vmY
 * Source modified: 2026-08-23T20:33:27.547Z
 *
 * TODO(M43, implemented_unverified): Licensed Chop lifecycle acceptance
 * Current: Batch27 freezes exact before/after inventory, position, ownership and escrow images.
 * A verified world decision plus owner custody/receipt recovers leave/return/adoption/Raw delivery.
 * No-space or invalid campfire refuses before reservation; uncertain saves retain evidence.
 * Pending journeys block member removal, reset and direct dimension restore, including offline owners.
 * Batch28 journals success/failure/offline inventory and Raw entitlement before retiring escrow;
 * player receipts and completed-run watermarks prevent repeat delivery. Required: M20 legacy review,
 * native dedicated/integrated interruption,
 * two-owner pickup, death outside D1, lost campfire and full-inventory acceptance are still pending.
 * Source: https://docs.google.com/document/d/1fyiehjysrKWM0RilTxXpccmEQzdqc65Wmz0XuQRrUio
 * Source modified: 2026-07-07T22:42:25.125Z
 * Source: https://docs.google.com/document/d/1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I
 * Source modified: 2026-08-19T21:23:31.822Z
 * Source: https://docs.google.com/document/d/18McISr9dcvMGp3-VjLDVsISosKBfkaidUTPzHxlw1Pc
 * Source modified: 2026-07-07T18:23:05.554Z
 *
 * TODO(M44, partial_D1): Implement Bogatyr companion ownership, cap and lifecycle
 * Current: Configured vanilla-wolf taming, five-companion roster, health/damage/movement, feeding,
 * breeding time, inherited collar and normal reload tuning.
 * Required: Preserve permanent tamed companions through instance teardown with owner/UUID-safe
 * recovery, or confirm the intended reset exception. Finish source skeleton/ranged-threat priority
 * and exact 10-percent pup-growth feeding; test breeding cap across unloads. Q&A D27 requests
 * configurable recruit duration with zero meaning permanent; later temporary relic recruits need
 * this setting under Bogatyr, not a hardcoded expiry.
 * Source: https://docs.google.com/document/d/1upubqZ3x53JaQqgI9rmNwKGOssxEIgReCTKKOohuwPw
 * Source modified: 2025-10-26T01:15:10.611Z
 * Source: https://docs.google.com/document/d/1JeaYFihitgr-A6TxWD151R9Ma9SMUVjyVVx--sNK4Ow
 * Source modified: 2025-10-18T17:54:18.154Z
 * Source: https://docs.google.com/document/d/10-3IgopUqHKyPHuZDKlpa64JMYXmq-_8GgKtQFhLX3c
 * Source modified: 2026-04-25T18:06:24.029Z
 *
 * TODO(M45, deferred_D2_plus): Implement the three scapula recruitment relics and assigned boss drops
 * Current: D1 uses vanilla wolves and authored vanilla equipment.
 * Required: Defer later scapula recruitment relics, boss drops and pack totem auras. Use newest
 * tier-specific sources; configure probability, duration (0 permanent), radius, strength, cap,
 * stacking, regeneration, bleed and lifesteal under Bogatyr. Do not register speculative D1
 * items/textures.
 * Source: https://docs.google.com/document/d/1t8nWA3bE9eGCYlm1zS-c5utR1Zn5g6vlRXBhJ3sAjnU
 * Source modified: 2026-01-20T18:28:26.008Z
 * Source: https://docs.google.com/document/d/1aUtDuhue7hlNhiPT43ep2KJZZuOildrnBZQaN9T6MOc
 * Source modified: 2026-01-20T21:30:15.083Z
 * Source: https://docs.google.com/document/d/12pWIGQ_L1qC6JF65FNErrqz-LB5z2rXiazotCokRfjU
 * Source modified: 2026-02-25T19:09:50.393Z
 * Source: https://docs.google.com/document/d/10-3IgopUqHKyPHuZDKlpa64JMYXmq-_8GgKtQFhLX3c
 * Source modified: 2026-04-25T18:06:24.029Z
 *
 * TODO(M46, deferred_D2_plus): Implement all three pack totem auras without same-class stacking
 * Current: D1 uses vanilla wolves and authored vanilla equipment.
 * Required: Defer later scapula recruitment relics, boss drops and pack totem auras. Use newest
 * tier-specific sources; configure probability, duration (0 permanent), radius, strength, cap,
 * stacking, regeneration, bleed and lifesteal under Bogatyr. Do not register speculative D1
 * items/textures.
 * Source: https://docs.google.com/document/d/1zA_EA-Dt0rDHwiV0w18Meruh7LbZ4i4cA887M04gt9Q
 * Source modified: 2026-01-20T21:32:06.246Z
 * Source: https://docs.google.com/document/d/12goYIx2eXkJqVSW5QAaW4YJqTbxu-i4jwyik0Xa9u8g
 * Source modified: 2026-01-20T21:31:49.895Z
 * Source: https://docs.google.com/document/d/12_DUdRY67snqD6w3nC4XjDFuzcXRvbld-zRTp49F-6k
 * Source modified: 2026-01-20T21:31:43.346Z
 * Source: https://docs.google.com/document/d/10-3IgopUqHKyPHuZDKlpa64JMYXmq-_8GgKtQFhLX3c
 * Source modified: 2026-04-25T18:06:24.029Z
 *
 * TODO(M47, deferred_D2_plus): Implement four wolf armor tiers, recipes, repairs and totem synergies
 * Current: Existing D1 vanilla dyed wolf armor and scutes are preserved.
 * Required: Defer custom wolf armor tiers, recipes, repairs and aura synergies to their documented
 * later dungeons. Resolve tables by modified date, configure modifiers centrally and preserve
 * existing item/armor components.
 * Source: https://docs.google.com/document/d/1B8zhyBafgg3hqztEtnNpwhAkNcf4YkBVwtwKSFjd86U
 * Source modified: 2025-10-26T01:48:10.187Z
 * Source: https://docs.google.com/document/d/1wYiddQdy_g4jt0LQLsvGoGo84KENclU0AagISUUVIB8
 * Source modified: 2025-10-26T01:29:13.116Z
 * Source: https://docs.google.com/document/d/1wg1IZRHb2fZ3hdw7xauYyncAGZKuHYbrRe8sHUYh4VQ
 * Source modified: 2025-10-26T01:41:47.878Z
 * Source: https://docs.google.com/document/d/1KHf_PdOdXy2cYkZwxJKZ2EmWA5JMyldPUPCWBMmOV0w
 * Source modified: 2025-10-26T01:35:28.476Z
 * Source: https://docs.google.com/document/d/1vcXgIrB0R7MRDDKsar-KLxir5ghmn9LhGl_zvHoa6mo
 * Source modified: 2026-01-23T17:34:06.948Z
 * Source: https://docs.google.com/document/d/1EBc7RDMA5Sm8TQ1uEG4kkPeiFRHBwOygAW_WLtGiUjg
 * Source modified: 2026-04-04T14:33:10.842Z
 *
 * TODO(M48, deferred_D2_plus): Unify Metalmancerâ€™s four-tier staff and golem statistics
 * Current: No later Metalmancer mechanics were enabled; privileged commands now require developer
 * access.
 * Required: Implement this finding's newest tier-specific staff/golem/ore/equipment/bag contract
 * only with its later dungeon. Use one persistent owner/UUID state, bounded AI, server-held-item
 * validation and safe inventory migration; expose all modifiers in the Metalmancer config section.
 * Source: https://docs.google.com/document/d/1X8UXqm_cHii5oPESESs3llBfXhGUIgkpXlEqAV0j6R0
 * Source modified: 2025-10-27T00:13:19.070Z
 * Source: https://docs.google.com/document/d/1rnxt8Vv_IdhH1ZiHkdS3Ix_oWkWSAijLs3QONigDTl4
 * Source modified: 2025-12-18T00:43:31.728Z
 * Source: https://docs.google.com/document/d/1LpgxY1w3ClENEwuqHOJlO_ulSUmK82D95_Aam9OS8nw
 * Source modified: 2025-10-27T01:43:12.851Z
 * Source: https://docs.google.com/document/d/1My3cokjeYyr-J20LkNeGJaCG11LfkSIUqfS_F0norD0
 * Source modified: 2025-10-27T00:18:48.776Z
 * Source: https://docs.google.com/document/d/1Q1LOvHt4MdJFAK3UDj90fT2HfThB3PCSFehGKGUrGP4
 * Source modified: 2025-10-27T01:33:12.564Z
 * Source: https://docs.google.com/document/d/1yUdXL9BE4CJSB-FifGKUlOSliMBscVotrZzHBv95pR8
 * Source modified: 2025-10-27T01:38:01.661Z
 * Source: https://docs.google.com/document/d/1bUZK8z3JtYpixhB8kBCwGT3SREqnef7z5sAZeYU-IsM
 * Source modified: 2025-10-16T01:24:44.970Z
 * Source: https://docs.google.com/document/d/1hlDeYD6NQZGEn8UUzifas_KERzWvsf7D_-_KB33zHaQ
 * Source modified: 2025-10-26T23:53:41.488Z
 * Source: https://docs.google.com/document/d/1LmPOc2w3SkDtcUtsqMHxnzpROlF5wTBX1U9s9A7Zq80
 * Source modified: 2025-10-27T01:42:04.482Z
 * Source: https://docs.google.com/document/d/1rfvBibxfBpOvbEEhPl3wOvYN6rCxC86Hs6PeXIXrY2U
 * Source modified: 2025-10-27T00:32:11.387Z
 *
 * TODO(M49, deferred_D2_plus): Implement the complete ore/rest/heal exchange, not only doubled idle income
 * Current: No later Metalmancer mechanics were enabled; privileged commands now require developer
 * access.
 * Required: Implement this finding's newest tier-specific staff/golem/ore/equipment/bag contract
 * only with its later dungeon. Use one persistent owner/UUID state, bounded AI, server-held-item
 * validation and safe inventory migration; expose all modifiers in the Metalmancer config section.
 * Source: https://docs.google.com/document/d/1rnxt8Vv_IdhH1ZiHkdS3Ix_oWkWSAijLs3QONigDTl4
 * Source modified: 2025-12-18T00:43:31.728Z
 * Source: https://docs.google.com/document/d/1LpgxY1w3ClENEwuqHOJlO_ulSUmK82D95_Aam9OS8nw
 * Source modified: 2025-10-27T01:43:12.851Z
 * Source: https://docs.google.com/document/d/1bUZK8z3JtYpixhB8kBCwGT3SREqnef7z5sAZeYU-IsM
 * Source modified: 2025-10-16T01:24:44.970Z
 * Source: https://docs.google.com/document/d/1hlDeYD6NQZGEn8UUzifas_KERzWvsf7D_-_KB33zHaQ
 * Source modified: 2025-10-26T23:53:41.488Z
 * Source: https://docs.google.com/document/d/19qNV4uIRmH_oOmIlwi-7-K-AWcroQx2XNfsPs9qoAFE
 * Source modified: 2026-01-18T15:52:49.084Z
 * Source: https://docs.google.com/document/d/1CftwLQAtZ6zQFV3EcO8t_S2odhfxiRcz1Age9db_lwU
 * Source modified: 2026-01-18T15:52:13.871Z
 * Source: https://docs.google.com/document/d/1LmPOc2w3SkDtcUtsqMHxnzpROlF5wTBX1U9s9A7Zq80
 * Source modified: 2025-10-27T01:42:04.482Z
 *
 * TODO(M50, deferred_D2_plus): Implement the individual Metalmancer equipment modifiers and stacking policy
 * Current: No later Metalmancer mechanics were enabled; privileged commands now require developer
 * access.
 * Required: Implement this finding's newest tier-specific staff/golem/ore/equipment/bag contract
 * only with its later dungeon. Use one persistent owner/UUID state, bounded AI, server-held-item
 * validation and safe inventory migration; expose all modifiers in the Metalmancer config section.
 * Source: https://docs.google.com/document/d/16CST2l99iOV2P1gc6YHbmpebcMqnZyEtYgH5qvO37ts
 * Source modified: 2025-10-27T00:44:43.444Z
 * Source: https://docs.google.com/document/d/1kSU5A7vsHjQ2OZmZ-KahXVOl9ot_hplIhhjs1nJUI0g
 * Source modified: 2025-10-27T00:27:24.875Z
 * Source: https://docs.google.com/document/d/1sEwVzt_w6az7w3gda74euXO3hTrKJzadPRFfKbFS5mU
 * Source modified: 2025-10-27T00:36:48.161Z
 * Source: https://docs.google.com/document/d/1IJ-mj-CWJdj4f-VJAnvHhrnZEmfOlcLBpERElqVU-Y0
 * Source modified: 2025-10-27T00:52:48.029Z
 * Source: https://docs.google.com/document/d/1My3cokjeYyr-J20LkNeGJaCG11LfkSIUqfS_F0norD0
 * Source modified: 2025-10-27T00:18:48.776Z
 * Source: https://docs.google.com/document/d/1HJbhncYLrLRbLTcWKGb-OUg9XnQY7LAf1yt0HxBmT_w
 * Source modified: 2025-10-27T00:58:30.213Z
 * Source: https://docs.google.com/document/d/1hlDeYD6NQZGEn8UUzifas_KERzWvsf7D_-_KB33zHaQ
 * Source modified: 2025-10-26T23:53:41.488Z
 * Source: https://docs.google.com/document/d/19qNV4uIRmH_oOmIlwi-7-K-AWcroQx2XNfsPs9qoAFE
 * Source modified: 2026-01-18T15:52:49.084Z
 * Source: https://docs.google.com/document/d/1CftwLQAtZ6zQFV3EcO8t_S2odhfxiRcz1Age9db_lwU
 * Source modified: 2026-01-18T15:52:13.871Z
 * Source: https://docs.google.com/document/d/1LmPOc2w3SkDtcUtsqMHxnzpROlF5wTBX1U9s9A7Zq80
 * Source modified: 2025-10-27T01:42:04.482Z
 * Source: https://docs.google.com/document/d/1rfvBibxfBpOvbEEhPl3wOvYN6rCxC86Hs6PeXIXrY2U
 * Source modified: 2025-10-27T00:32:11.387Z
 * Source: https://docs.google.com/document/d/10I0zjgEpMf-zYpcJuNQyaVXCidpJ5i8DzeBOrmnMABY
 * Source modified: 2025-10-27T00:40:34.616Z
 * Source: https://docs.google.com/document/d/1hYriqUpkrn20vhEq2jtl6WPVA_8snLwSOWUuOdMGNU8
 * Source modified: 2025-10-27T00:55:56.660Z
 * Source: https://docs.google.com/document/d/1jzkjrW0YyLL2fOA1xnEEv2Cl-FZrcYGzhzrIA1PhhQ4
 * Source modified: 2025-10-27T00:48:43.175Z
 * Source: https://docs.google.com/document/d/1OSi9TwcAu59-kAjTSNTvJzk41P3fABAItlR2BxMpXjY
 * Source modified: 2025-10-27T01:01:00.153Z
 *
 * TODO(M51, deferred_D2_plus): Validate held equipment and action context on every Metalmancer packet
 * Current: No later Metalmancer mechanics were enabled; privileged commands now require developer
 * access.
 * Required: Implement this finding's newest tier-specific staff/golem/ore/equipment/bag contract
 * only with its later dungeon. Use one persistent owner/UUID state, bounded AI, server-held-item
 * validation and safe inventory migration; expose all modifiers in the Metalmancer config section.
 * Source: https://docs.google.com/document/d/1bUZK8z3JtYpixhB8kBCwGT3SREqnef7z5sAZeYU-IsM
 * Source modified: 2025-10-16T01:24:44.970Z
 * Source: https://docs.google.com/document/d/1hlDeYD6NQZGEn8UUzifas_KERzWvsf7D_-_KB33zHaQ
 * Source modified: 2025-10-26T23:53:41.488Z
 * Source: https://docs.google.com/document/d/19qNV4uIRmH_oOmIlwi-7-K-AWcroQx2XNfsPs9qoAFE
 * Source modified: 2026-01-18T15:52:49.084Z
 * Source: https://docs.google.com/document/d/1CftwLQAtZ6zQFV3EcO8t_S2odhfxiRcz1Age9db_lwU
 * Source modified: 2026-01-18T15:52:13.871Z
 *
 * TODO(M52, deferred_D2_plus): Make one-golem ownership and link state robust across unload/restart
 * Current: No later Metalmancer mechanics were enabled; privileged commands now require developer
 * access.
 * Required: Implement this finding's newest tier-specific staff/golem/ore/equipment/bag contract
 * only with its later dungeon. Use one persistent owner/UUID state, bounded AI, server-held-item
 * validation and safe inventory migration; expose all modifiers in the Metalmancer config section.
 * Source: https://docs.google.com/document/d/1rnxt8Vv_IdhH1ZiHkdS3Ix_oWkWSAijLs3QONigDTl4
 * Source modified: 2025-12-18T00:43:31.728Z
 * Source: https://docs.google.com/document/d/1bUZK8z3JtYpixhB8kBCwGT3SREqnef7z5sAZeYU-IsM
 * Source modified: 2025-10-16T01:24:44.970Z
 * Source: https://docs.google.com/document/d/1hlDeYD6NQZGEn8UUzifas_KERzWvsf7D_-_KB33zHaQ
 * Source modified: 2025-10-26T23:53:41.488Z
 * Source: https://docs.google.com/document/d/10-3IgopUqHKyPHuZDKlpa64JMYXmq-_8GgKtQFhLX3c
 * Source modified: 2026-04-25T18:06:24.029Z
 *
 * TODO(M53, deferred_D2_plus): Finish bag tier capacities and safe inventory/ore persistence
 * Current: No later Metalmancer mechanics were enabled; privileged commands now require developer
 * access.
 * Required: Implement this finding's newest tier-specific staff/golem/ore/equipment/bag contract
 * only with its later dungeon. Use one persistent owner/UUID state, bounded AI, server-held-item
 * validation and safe inventory migration; expose all modifiers in the Metalmancer config section.
 * Source: https://docs.google.com/document/d/16Eq0yp7NTI57AK22s8vXrkoRYFp6BYrcdNjfbehWrcw
 * Source modified: 2026-01-18T15:54:24.799Z
 * Source: https://docs.google.com/document/d/1hlDeYD6NQZGEn8UUzifas_KERzWvsf7D_-_KB33zHaQ
 * Source modified: 2025-10-26T23:53:41.488Z
 * Source: https://docs.google.com/document/d/19qNV4uIRmH_oOmIlwi-7-K-AWcroQx2XNfsPs9qoAFE
 * Source modified: 2026-01-18T15:52:49.084Z
 * Source: https://docs.google.com/document/d/1CftwLQAtZ6zQFV3EcO8t_S2odhfxiRcz1Age9db_lwU
 * Source modified: 2026-01-18T15:52:13.871Z
 *
 * TODO(M54, deferred_D2_plus): Separate standard brewing from the proposed class-specific brewing station
 * Current: D1 retains gated vanilla brewing with configured Theurgist timing.
 * Required: Defer the proposed dedicated class brewing station and later recipes; preserve vanilla
 * D1 ingredients and keep production-cost checks local and source-versioned.
 * Source: https://docs.google.com/document/d/1fJU3nZTZi6iiK1EJS-RbEZ3sHtTdExLeBR3FaErXr3M
 * Source modified: 2026-04-05T13:48:38.690Z
 * Source: https://docs.google.com/document/d/1l9ox2pQUSPy0_J3h7ljPOaVOFtMFkoHq_rFSGK4iqeM
 * Source modified: 2026-04-05T14:27:08.267Z
 * Source: https://docs.google.com/document/d/1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es
 * Source modified: 2026-08-19T20:36:36.645Z
 * Source: https://docs.google.com/document/d/1LwIctAvogSULFfP79QTMhOZtBBQj-mKqaxA-t7jksTc
 * Source modified: 2026-08-19T20:44:48.175Z
 *
 * TODO(M55, partial_D1): Implement exact restorative/vision/marking arrow identities and tier effects
 * Current: Canonical D1 restorative, undead, vision and marking arrow effects; optional immutable
 * ability IDs and bounded compatibility recognition.
 * Required: Complete immutable authoring/migration of legacy vanilla ammo and test every impact,
 * shield, undead inversion, duration and class gate. Never change ordinary renamed arrows into
 * privileged ammunition without matching approved components.
 * Source: https://docs.google.com/document/d/1HnwmguP_4MjQyAdUlSeJuEPd3s1b33iFUR7TT_sjCrU
 * Source modified: 2026-03-14T21:35:01.908Z
 * Source: https://docs.google.com/document/d/11KIBXd995g2qjZhC65doSiK38g1d3N7V6ilBxkb93OM
 * Source modified: 2026-04-04T17:08:51.912Z
 * Source: https://docs.google.com/document/d/1bPkBSpDNZf8lr6ocdn0UGj2ZUJ9fzXX8xuVGRwcZYSg
 * Source modified: 2026-04-04T16:38:55.948Z
 * Source: https://docs.google.com/document/d/1MufAd5Ow8tBj04_NIoENDOZLc4IzzxofxZPHFVOAVgo
 * Source modified: 2026-03-14T21:31:47.537Z
 * Source: https://docs.google.com/document/d/1aX2vZ5tlPQh1-crhQBmMhDnVQfJpNiQ2EWcASkmgZFQ
 * Source modified: 2026-04-04T16:42:20.247Z
 * Source: https://docs.google.com/document/d/1u1Ou0Jmt2cacvF4mwdUvUriChBC_RYoOS8RwkL9nNpU
 * Source modified: 2026-03-14T21:38:40.664Z
 * Source: https://docs.google.com/document/d/1cY_czWEYbUEg_EQmaSANhOFe326gTDVKfL9XaEFGmQo
 * Source modified: 2026-04-04T14:39:30.851Z
 * Source: https://docs.google.com/document/d/1l9ox2pQUSPy0_J3h7ljPOaVOFtMFkoHq_rFSGK4iqeM
 * Source modified: 2026-04-05T14:27:08.267Z
 *
 * TODO(M56, deferred_D2_plus): Implement the approved Thanatropic Conduit / Binding Idol lifecycle
 * Current: D1 does not enable later conduit/revival/story content.
 * Required: Defer the documented later-tier Theurgist conduit and story lifecycle. Q&A D68 defines
 * downed as the death screen; a future revive of a respawned player must return them with recovered
 * gear and source consequences without duplication. Keep internal epilogue knowledge out of player
 * text.
 * Source: https://docs.google.com/document/d/1MLmsu-_Ntd1EtYtImWLlZWuM34Mg88-EbjchWB0BXM8
 * Source modified: 2026-04-01T17:42:13.081Z
 * Source: https://docs.google.com/document/d/16rRSJb6ll0xjkftfGKVetjubI08xYwU9WmLyJ0OtJ10
 * Source modified: 2026-04-01T17:35:52.938Z
 * Source: https://docs.google.com/document/d/1l9ox2pQUSPy0_J3h7ljPOaVOFtMFkoHq_rFSGK4iqeM
 * Source modified: 2026-04-05T14:27:08.267Z
 * Source: https://docs.google.com/document/d/1LYtsUEgswZE8bi9IaDrUeposrTTEL_xMt0eiXjzVMWk
 * Source modified: 2026-04-04T17:29:44.550Z
 *
 * TODO(M57, deferred_D2_plus): Deliver the Theurgist story arc without leaking internal epilogue knowledge
 * Current: D1 does not enable later conduit/revival/story content.
 * Required: Defer the documented later-tier Theurgist conduit and story lifecycle. Q&A D68 defines
 * downed as the death screen; a future revive of a respawned player must return them with recovered
 * gear and source consequences without duplication. Keep internal epilogue knowledge out of player
 * text.
 * Source: https://docs.google.com/document/d/1NLiq_L2_CQub_0hdCqAxeN5D5dL7bwP7FlqlQf9rqg4
 * Source modified: 2025-10-17T02:45:04.518Z
 * Source: https://docs.google.com/document/d/12mNX8Lc_kTdkL2fOPVsEffuW4zw4u_DzTmxtFVpmYHo
 * Source modified: 2025-10-17T02:40:34.923Z
 * Source: https://docs.google.com/document/d/1B6d-tK5wbYwa5dpWzlCAk90exral4k1ormCYgAG5he4
 * Source modified: 2025-10-12T00:23:14.661Z
 * Source: https://docs.google.com/document/d/1OCn7biuflB5sOvD9rdWJKvX7tXm27PZeJjRHH2eSO8k
 * Source modified: 2026-08-29T14:50:01.277Z
 * Source: https://docs.google.com/document/d/10DA4A5DyglUecj3ezDzmp6Kj2EtTwUpvkKNSHS3MT9M
 * Source modified: 2025-10-08T03:01:00.829Z
 * Source: https://docs.google.com/document/d/1bewOBnVPQw_jicVo_mZbfvIDGeVO3u96jEsh1WRU7iw
 * Source modified: 2025-10-17T02:43:33.093Z
 *
 * TODO(M58, partial_D1): Add the approved Venefex attack, debuff and movement mechanics
 * Current: D1 Venefex poison, weakness, slowness and Spicule effects are configurable and server-
 * authoritative.
 * Required: Confirm the unspecified per-distinct-debuff Spicule multiplier; current explicit config
 * default is 10 percent, cap 10. Test nonlethal poison, strongest family replacement, overlapping
 * effects and undead healing in multiplayer.
 * Source: https://docs.google.com/document/d/1yc3TyFu0HmD_P6TDO7I_z0SIYkiZwq7SVprj_zc8WcA
 * Source modified: 2026-04-04T20:51:06.663Z
 * Source: https://docs.google.com/document/d/1JXqPdwWxateRMGpAuoV1ub8asNL7iMeyrBtzqTuUwm8
 * Source modified: 2026-04-04T14:36:04.408Z
 *
 * TODO(M59, deferred_D2_plus): Add Deadeye zoom, draw and range-based damage progression
 * Current: Deadeye is unavailable in D1.
 * Required: Implement approved zoom/draw/range damage only at D2 unlock, with bounded client work
 * and server damage validation; keep tuning in the Deadeye config section.
 * Source: https://docs.google.com/document/d/1Y1T-L7qRv3GWr11fcq9vuq_rO6yORnbqc1WmYplGVWg
 * Source modified: 2025-10-03T22:50:35.453Z
 * Source: https://docs.google.com/document/d/1oXHIKtIdWlQDwzf1JaHzBut08g4tytXs6rCYRY-89NY
 * Source modified: 2025-08-22T22:56:40.140Z
 *
 * TODO(M60, partial_D1): Reconcile Dragoon trident enchantments and chain lightning with bounded targeting
 * Current: D1 trident chain uses 3-percent chance, bounded 32-block line of sight and target limit
 * 64; original victim is not hit twice.
 * Required: Validate the chosen line-of-sight approximation for source 'on screen' and exact
 * authored trident enchantments in multiplayer. All chosen radius/limit/damage defaults are
 * adjustable in Dragoon config.
 * Source: https://docs.google.com/document/d/1uG80jIWpLKZvTGCmbHStqJs565iqEvqhr6N5oYBIHOE
 * Source modified: 2026-08-28T17:05:24.471Z
 * Source: https://docs.google.com/document/d/19bB7G3MBAmokIZeURpq2bvBydZlg-MIZEce_tVR5pAk
 * Source modified: 2026-08-16T22:48:44.527Z
 *
 * TODO(M63, partial_D1): Align Pyroclast rockets and launchers with exact payload/damage rules
 * Current: Canonical D1 vanilla rockets use newest Cinderbite/Cindermaul payload and damage values,
 * bounded LOS blast and no terrain damage.
 * Required: Verify authored launcher/ammo components, class restrictions, self/friendly damage,
 * shield interaction and falloff in multiplayer; preserve vanilla chest contents.
 * Source: https://docs.google.com/document/d/1rW6TdP7S8L_KccGDUdrA_qotmmrP5LY435bx1KGE8Rs
 * Source modified: 2026-03-26T13:28:56.207Z
 * Source: https://docs.google.com/document/d/1-Rfil-stpQMbCIP6lHYks6y3WHeFdfM9uS6f3VuomVw
 * Source modified: 2026-03-26T13:33:39.916Z
 * Source: https://docs.google.com/document/d/1ZJeOBBCfaQ9D-jhYxrPsCAry-MbJVBAdW0CFmGpRh5I
 * Source modified: 2026-03-26T13:32:38.866Z
 * Source: https://docs.google.com/document/d/1vyElnGqiq5ZumdIu1cF51hA6HZn_eSB4z-l61dsZEss
 * Source modified: 2026-03-26T13:25:47.056Z
 * Source: https://docs.google.com/document/d/1A-TIYSaInjFJOm80KrgxSFErXow4JSHz2it-cvkY6xU
 * Source modified: 2026-03-26T13:27:20.195Z
 * Source: https://docs.google.com/document/d/1WUjS_D0DPlAWhhpjtU8hetK-QYY292GibxAyOtpYyy8
 * Source modified: 2026-03-26T13:31:55.612Z
 * Source: https://docs.google.com/document/d/1B1I0_k_r6fD32QzKp9sBgedlXbAv_JWZ5v145ydYAVw
 * Source modified: 2026-03-26T12:49:38.090Z
 * Source: https://docs.google.com/document/d/16FD3wxi-Uen_DRzItDHdSrSZvkGNwa_r-ZeUYiswxoE
 * Source modified: 2026-03-26T14:08:42.520Z
 *
 * Scope note (M64, author_owned_content): Preserve Bogatyr loadouts; no AI content task
 * Current: Authored vanilla equipment, chest contents and all existing IDs were preserved; no
 * automatic loadout rewrite.
 * Scope correction (Cameron, 2026-09-20): these contents are author-owned and outside AI work.
 * Do not inspect/reconcile/rebuild the loadout or alter names, counts, payloads or components.
 * Requested container access/display/shift-transfer code must preserve the authored stacks.
 * Source: https://docs.google.com/document/d/1lUy03lqDbeB4s_o5Nyvft40JMRj60pKMrY2jQ8Q0ncU
 * Source modified: 2026-04-04T20:46:47.612Z
 * Source: https://docs.google.com/document/d/1EBc7RDMA5Sm8TQ1uEG4kkPeiFRHBwOygAW_WLtGiUjg
 * Source modified: 2026-04-04T14:33:10.842Z
 * Source: https://docs.google.com/document/d/1JeaYFihitgr-A6TxWD151R9Ma9SMUVjyVVx--sNK4Ow
 * Source modified: 2025-10-18T17:54:18.154Z
 *
 * Scope note (M65, author_owned_content): Preserve Dragoon loadouts; no AI content task
 * Current: Authored vanilla equipment, chest contents and all existing IDs were preserved; no
 * automatic loadout rewrite.
 * Scope correction (Cameron, 2026-09-20): these contents are author-owned and outside AI work.
 * Do not inspect/reconcile/rebuild the loadout or alter names, counts, payloads or components.
 * Requested container access/display/shift-transfer code must preserve the authored stacks.
 * Source: https://docs.google.com/document/d/1E6YgHK0CEpirhbvAmy9Ihg9oQpr-p_YUymbUbDEqhVI
 * Source modified: 2026-04-04T20:26:31.382Z
 * Source: https://docs.google.com/document/d/1uG80jIWpLKZvTGCmbHStqJs565iqEvqhr6N5oYBIHOE
 * Source modified: 2026-08-28T17:05:24.471Z
 *
 * Scope note (M66, author_owned_content): Preserve Judicator loadouts; no AI content task
 * Current: Authored vanilla equipment, chest contents and all existing IDs were preserved; no
 * automatic loadout rewrite.
 * Scope correction (Cameron, 2026-09-20): these contents are author-owned and outside AI work.
 * Do not inspect/reconcile/rebuild the loadout or alter names, counts, payloads or components.
 * Requested container access/display/shift-transfer code must preserve the authored stacks.
 * Source: https://docs.google.com/document/d/1GY8_zURMNKZvxkV-PCkG82Rh1q7tErvFBWoSi3wvNWU
 * Source modified: 2026-04-04T21:12:05.781Z
 * Source: https://docs.google.com/document/d/1cY_czWEYbUEg_EQmaSANhOFe326gTDVKfL9XaEFGmQo
 * Source modified: 2026-04-04T14:39:30.851Z
 *
 * Scope note (M67, author_owned_content): Preserve Pyroclast loadouts; no AI content task
 * Current: Authored vanilla equipment, chest contents and all existing IDs were preserved; no
 * automatic loadout rewrite.
 * Scope correction (Cameron, 2026-09-20): these contents are author-owned and outside AI work.
 * Do not inspect/reconcile/rebuild the loadout or alter names, counts, payloads or components.
 * Requested container access/display/shift-transfer code must preserve the authored stacks.
 * Source: https://docs.google.com/document/d/1CQTFJrQyW8YNvU9pIaJZcFEHGSTrVjA7jQS0aTTYMNg
 * Source modified: 2026-04-04T21:16:03.857Z
 * Source: https://docs.google.com/document/d/16FD3wxi-Uen_DRzItDHdSrSZvkGNwa_r-ZeUYiswxoE
 * Source modified: 2026-03-26T14:08:42.520Z
 *
 * Scope note (M68, author_owned_content): Preserve Theurgist loadouts; no AI content task
 * Current: Authored vanilla equipment, chest contents and all existing IDs were preserved; no
 * automatic loadout rewrite.
 * Scope correction (Cameron, 2026-09-20): these contents are author-owned and outside AI work.
 * Do not inspect/reconcile/rebuild the loadout or alter names, counts, payloads or components.
 * Requested container access/display/shift-transfer code must preserve the authored stacks.
 * Source: https://docs.google.com/document/d/12VsVNQCmCmFy65ROmubdzaD4Vg1UTeLuNyBe9HhDyKo
 * Source modified: 2026-04-05T14:20:07.501Z
 * Source: https://docs.google.com/document/d/1l9ox2pQUSPy0_J3h7ljPOaVOFtMFkoHq_rFSGK4iqeM
 * Source modified: 2026-04-05T14:27:08.267Z
 *
 * Scope note (M69, author_owned_content): Preserve Venefex loadouts; no AI content task
 * Current: Authored vanilla equipment, chest contents and all existing IDs were preserved; no
 * automatic loadout rewrite.
 * Scope correction (Cameron, 2026-09-20): these contents are author-owned and outside AI work.
 * Do not inspect/reconcile/rebuild the loadout or alter names, counts, payloads or components.
 * Requested container access/display/shift-transfer code must preserve the authored stacks.
 * Source: https://docs.google.com/document/d/1yc3TyFu0HmD_P6TDO7I_z0SIYkiZwq7SVprj_zc8WcA
 * Source modified: 2026-04-04T20:51:06.663Z
 * Source: https://docs.google.com/document/d/1JXqPdwWxateRMGpAuoV1ub8asNL7iMeyrBtzqTuUwm8
 * Source modified: 2026-04-04T14:36:04.408Z
 *
 * Scope note (M70, author_owned_content): Preserve Metalmancer loadouts; no AI content task
 * Current: Authored vanilla equipment, chest contents and all existing IDs were preserved; no
 * automatic loadout rewrite.
 * Scope correction (Cameron, 2026-09-20): these contents are author-owned and outside AI work.
 * Do not inspect/reconcile/rebuild the loadout or alter names, counts, payloads or components.
 * Requested container access/display/shift-transfer code must preserve the authored stacks.
 * Source: https://docs.google.com/document/d/16Eq0yp7NTI57AK22s8vXrkoRYFp6BYrcdNjfbehWrcw
 * Source modified: 2026-01-18T15:54:24.799Z
 * Source: https://docs.google.com/document/d/1hlDeYD6NQZGEn8UUzifas_KERzWvsf7D_-_KB33zHaQ
 * Source modified: 2025-10-26T23:53:41.488Z
 *
 * Scope note (M71, author_owned_content): Preserve Deadeye loadouts; no AI content task
 * Current: Authored vanilla equipment, chest contents and all existing IDs were preserved; no
 * automatic loadout rewrite.
 * Scope correction (Cameron, 2026-09-20): these contents are author-owned and outside AI work.
 * Do not inspect/reconcile/rebuild the loadout or alter names, counts, payloads or components.
 * Requested container access/display/shift-transfer code must preserve the authored stacks.
 * Source: https://docs.google.com/document/d/1Y1T-L7qRv3GWr11fcq9vuq_rO6yORnbqc1WmYplGVWg
 * Source modified: 2025-10-03T22:50:35.453Z
 *
 * TODO(M72, partial_D1): Create a canonical named-item factory/catalogue with provenance
 * Batch 04: 23 named D1 identities and exact vanilla base types, immutable origin values,
 * held-stack developer adoption, and trusted vendor issuance are implemented. Named prices use
 * the 2026-08-28 final-item table in all_vendors_prices.config, without double enchantment value.
 * Required: review legacy template/container/spawner mappings, include unloaded storage and class-issued
 * stock outside class chests, and preserve counts/components. Class-chest item work is excluded
 * by Cameron's 2026-09-20 correction. Never infer identity from anvil text or auto-recreate chests.
 * Batch 05 implemented Tax with its own paired player snapshot. Batch 06 adds explicit one-slot
 * preview/apply/undo and a read-only inventory survey; no automatic legacy world migration.
 * Shared currency/trade journaling remains M03/M115.
 * Source: https://docs.google.com/document/d/1Gbcq7Piqg2uHO1smx5oHOyeGxoT9g93cH-_G8WvhOoo
 * Source modified: 2026-08-18T22:32:42.870Z
 * Source: https://docs.google.com/document/d/1fX1UbC6cG_cnN2auDo_1ascDY24yjFy9pIfL4qe3-_g
 * Source modified: 2026-08-28T16:49:24.607Z
 * Source: https://docs.google.com/document/d/1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc
 * Source modified: 2026-08-18T22:35:34.052Z
 * Source: https://docs.google.com/document/d/1K8HbgtOvRTYugr_X4GTquXqnWP0we87wmIz1WWEhPxo
 * Source modified: 2025-09-15T00:52:41.819Z
 * Source: https://docs.google.com/document/d/1B3hQLrrOkZeRPG1tomd54rd7v7OKQ_PDNozHH-DIffY
 * Source modified: 2026-08-19T21:16:19.213Z
 * Source: https://docs.google.com/document/d/19bB7G3MBAmokIZeURpq2bvBydZlg-MIZEce_tVR5pAk
 * Source modified: 2026-08-16T22:48:44.527Z
 *
 * TODO(M73, deferred_D2_plus): Populate all player-facing Codex and item descriptions with reveal controls
 * Current: Current D1 player help was updated alongside implemented behavior.
 * Required: Defer the full later-tier Codex and staged reveal corpus. Keep private internal
 * notes/epilogues out of player text; later descriptions must follow newest approved lore and reveal
 * gates.
 * Source: https://docs.google.com/document/d/1JeaYFihitgr-A6TxWD151R9Ma9SMUVjyVVx--sNK4Ow
 * Source modified: 2025-10-18T17:54:18.154Z
 * Source: https://docs.google.com/document/d/1NLiq_L2_CQub_0hdCqAxeN5D5dL7bwP7FlqlQf9rqg4
 * Source modified: 2025-10-17T02:45:04.518Z
 * Source: https://docs.google.com/document/d/1K8HbgtOvRTYugr_X4GTquXqnWP0we87wmIz1WWEhPxo
 * Source modified: 2025-09-15T00:52:41.819Z
 * Source: https://docs.google.com/document/d/1JvyunRb_cgCEVRGud91fCD3ZP83lxsbJcXbU_5-SimU
 * Source modified: 2026-08-29T00:32:32.934Z
 * Source: https://docs.google.com/document/d/1Ob5xTDxThhcXVcvW6u6KUJHUiptTkxNaU6vEx-GedD4
 * Source modified: 2026-05-09T20:36:55.170Z
 * Source: https://docs.google.com/document/d/1VwuK2NIRUTIxIMOFviWlVPZ_gz09yRjmYdSNlxxQkyY
 * Source modified: 2026-07-06T16:23:43.120Z
 * Source: https://docs.google.com/document/d/1B6d-tK5wbYwa5dpWzlCAk90exral4k1ormCYgAG5he4
 * Source modified: 2025-10-12T00:23:14.661Z
 * Source: https://docs.google.com/document/d/1OCn7biuflB5sOvD9rdWJKvX7tXm27PZeJjRHH2eSO8k
 * Source modified: 2026-08-29T14:50:01.277Z
 *
 * TODO(M74, deferred_D2_plus): Correct Vital Exchange Iâ€“IV to real transfer events and the right tier identities
 * Current: Incorrect D1 Vital Exchange hooks were removed; future Binding Idol milestones remain
 * inactive.
 * Required: Implement real later-tier transfer/provider/return events using canonical identities and
 * idempotent milestone receipts; current D1 vanilla Totems must not be silently converted into
 * conduits.
 * Source: https://docs.google.com/document/d/1ira4kzScYAYEjijiw_JZncI0jYcWOsMeklTgi1F18xg
 * Source modified: 2026-04-04T20:56:53.633Z
 * Source: https://docs.google.com/document/d/1l9ox2pQUSPy0_J3h7ljPOaVOFtMFkoHq_rFSGK4iqeM
 * Source modified: 2026-04-05T14:27:08.267Z
 *
 * TODO(M75, deferred_D2_plus): Wire Binding Idol provider/return milestones into actual gameplay
 * Current: Incorrect D1 Vital Exchange hooks were removed; future Binding Idol milestones remain
 * inactive.
 * Required: Implement real later-tier transfer/provider/return events using canonical identities and
 * idempotent milestone receipts; current D1 vanilla Totems must not be silently converted into
 * conduits.
 * Source: https://docs.google.com/document/d/1MLmsu-_Ntd1EtYtImWLlZWuM34Mg88-EbjchWB0BXM8
 * Source modified: 2026-04-01T17:42:13.081Z
 * Source: https://docs.google.com/document/d/16rRSJb6ll0xjkftfGKVetjubI08xYwU9WmLyJ0OtJ10
 * Source modified: 2026-04-01T17:35:52.938Z
 * Source: https://docs.google.com/document/d/1l9ox2pQUSPy0_J3h7ljPOaVOFtMFkoHq_rFSGK4iqeM
 * Source modified: 2026-04-05T14:27:08.267Z
 * Source: https://docs.google.com/document/d/1LYtsUEgswZE8bi9IaDrUeposrTTEL_xMt0eiXjzVMWk
 * Source modified: 2026-04-04T17:29:44.550Z
 *
 * TODO(M76, runtime acceptance): Batch09 counts only successful BellBlock rings, six distinct
 * positions per instance in a configurable rolling 40-tick window. Cameron's judgment instruction
 * resolves the contradictory D70 times to its first two-second value; developers may configure it.
 * Verify actual redstone/projectile rings, repeated bells and disconnected/dead party recipients.
 * Source: https://docs.google.com/document/d/1L_CmsTIWQ9TADh_1T18G_ukG61y1oExpH6tgzM9IKUw
 * Source modified: 2026-04-01T16:01:33.432Z
 *
 * TODO(M77, explicit deferred Wither variants + runtime acceptance): Base Sixfold Vigil is
 * implemented with any six dyed colors on chiseled tuff, full authored-room incremental scan,
 * final simultaneous lit-state validation and durable eligible-instance credit. Q&A D71 defers
 * Withers. Later implement After Dissolution / Lone Adversary / Twin Manifestation with zero /
 * one / two authoritative encounter summons and the same candle condition, not raw entity counts.
 * Verify flame-arrow lighting, extinguish/unload during scan, separate runs and configured budgets.
 * Source: https://docs.google.com/document/d/1YkiyPfomO7rnSenj4A9tySkh3e5mcKy4JNXSXv3ScR0
 * Source modified: 2026-03-29T14:36:37.731Z
 *
 * TODO(M79, remaining authored TEST acceptance): Batch10 implements marked canonical
 * handheld and lectern journals, per-player/run reading, persistent Librarian credit, and a
 * same-player one-Elytra receipt. Developer preview/apply/undo preserves matching legacy books.
 * Objective binding validates loaded chest/lectern types; either half of the bound double chest
 * qualifies after a real menu opens. Q&A D76 supplies one ordinary vanilla Elytra; no single-flight
 * behavior is inferred. Verify actual authored placements and dedicated/integrated save faults.
 * Batch37 corrects earlier audit inference using MASTER Achievements!B18/C18 and B20/C20:
 * Stairway is the World Spawn uppermost chest, with a schema2 run0 receipt; old positive-run
 * schema1 receipts remain valid. Bind the actual Overworld chest; old D1 bindings remain intact
 * but inactive. Wolves means six CHARACTERS wearing vanilla Piglin Heads simultaneously at Camp4.
 * Its per-run roster sample and permanent entitlement use existing D1RunData, not wolf counts.
 * Bind d1_camp_4 to reviewed camp bounds around source navigation coordinate 630 22 68.
 * Verify both instances, head removal/death/escrow/developer exclusions and restart replay.
 * No new equipment or PNG is needed; Wither variants and Vital Exchange D2 remain deferred.
 * Source: https://docs.google.com/document/d/1Bc1H58IC-VlS-ZyAm1YKLyHElAJ2EPMrB--M1IMe90o
 * Source modified: 2026-07-05T13:43:45.313Z
 * Source: https://docs.google.com/document/d/15r_3mHAj4hbIrcS9vUQ5BL57V88u6SX9JSgI-qzOA9c
 * Source modified: 2026-07-10T04:50:54.046Z
 * Source: https://docs.google.com/document/d/1pP6Mt-5TFdgEPpyswaIJyEDNexqTdmUomnChCdCXt0A
 * Source modified: 2026-07-06T17:01:12.599Z
 * Source: https://docs.google.com/document/d/1IuXRtjVl8Ew_CTedyrSTE0axlWycs0KOaIYXP0C0pqI
 * Source modified: 2026-08-19T22:07:37.956Z
 * Source: https://docs.google.com/document/d/1eqoSArvAllrAY5sk-uOdkQHwp5GvhsM53mGSIB8I5PI
 * Source modified: 2026-05-03T15:14:37.858Z
 * Source: https://docs.google.com/document/d/15y8tZg7KcmYyGKGsNvsxODiEtstEiLF9_1JK-j1A0kc
 * Source modified: 2026-08-10T22:12:18.939Z
 * Source: https://docs.google.com/document/d/1YkiyPfomO7rnSenj4A9tySkh3e5mcKy4JNXSXv3ScR0
 * Source modified: 2026-03-29T14:36:37.731Z
 * Source: https://docs.google.com/document/d/1PdXrct3swSjD0y3Twmct1NKZQ2N9GZQEFOWUddPYnsU
 * Source modified: 2026-08-10T22:04:51.200Z
 * Source: https://docs.google.com/document/d/1L_CmsTIWQ9TADh_1T18G_ukG61y1oExpH6tgzM9IKUw
 * Source modified: 2026-04-01T16:01:33.432Z
 * Source: https://docs.google.com/document/d/1dIuaeMFMZaaWo7AS1zQ51kXSbdPkb9tmUm864MBs2Q0
 * Source modified: 2026-08-19T22:32:29.955Z
 * Source: https://docs.google.com/document/d/1vhnYVmzSufbqtdugC2tIl0DAWRrq_nsl7k71AVCSa-w
 * Source modified: 2026-04-01T16:03:35.387Z
 * Source: https://docs.google.com/document/d/1ira4kzScYAYEjijiw_JZncI0jYcWOsMeklTgi1F18xg
 * Source modified: 2026-04-04T20:56:53.633Z
 * Source: https://docs.google.com/document/d/1xe-ikZsd0JoNlHZO_4b076AND7W6ow1ax4aWnVfuNPo
 * Source modified: 2026-08-15T21:56:15.190Z
 *
 * TODO(M81, partial_D1): Reconcile every D1 quest, region, key and startup-room binding
 * Current: Explicit Watson/objective authoring commands and bounded location/region checks support
 * D1.
 * Required: Verify every retained quest, room, key, region, reset and encounter binding against a
 * backed-up TEST world. Existing source coordinates are defaults, not proof that the actual world
 * matches. Preserve authored spawners, locks, rifts, schematics and unloaded chunks.
 * Batch37 binding handoff: docs/ai/D1_BATCH_37_BINDINGS.md maps startup, journals, fire path,
 * Camp4/5, Wither room, Manor, Plant Flags, Watson and World Spawn. Source chest lists contain
 * repeated entries; Q&A D79 preserves developer-authored quantities. Do not run broad adoption,
 * replace placed locks, or infer door/key IDs and cuboid bounds from navigation coordinates.
 * Source: https://docs.google.com/document/d/1x59OaQfNB1UNYXgLySdq5BBktq9brgcBBbqLeJYpBvU
 * Source modified: 2026-08-20T20:25:49.056Z
 * Source: https://docs.google.com/document/d/1Id13I12xr2XZgwzJX0ujnNvyIFu0AM87FUhc2qdmhJ4
 * Source modified: 2026-04-04T22:15:33.364Z
 * Source: https://docs.google.com/document/d/10fv5JCue39bZq8ENdDdz7QtJdJKH67f8oCIzGcAyc2Y
 * Source modified: 2026-07-12T16:12:42.442Z
 * Source: https://docs.google.com/document/d/1a5Xy46ahjqBSNPrcw6Tgl5HTulss9n5vqNrmCVScWjY
 * Source modified: 2026-07-18T14:48:43.831Z
 * Source: https://docs.google.com/document/d/1FIcIf82rCAEAhbEE2gbR7jdoFA0BWSIcnzeOcKlUG4M
 * Source modified: 2026-07-07T18:38:29.922Z
 * Source: https://docs.google.com/document/d/1VwuK2NIRUTIxIMOFviWlVPZ_gz09yRjmYdSNlxxQkyY
 * Source modified: 2026-07-06T16:23:43.120Z
 * Source: https://docs.google.com/document/d/1-FcHP73pFytPfoM2KhUPa6tt_2licsgWmWokto4YzE4
 * Source modified: 2026-08-19T22:35:48.808Z
 *
 * TODO(M89, deferred_D2_plus): Build the remaining D2 rooms, encounters and progression hand-off
 * Current: No new D2-D5 dungeon content was implemented.
 * Required: Keep the finding's linked rooms, encounters, puzzles, endings, persistent outcomes and
 * inter-dungeon progression as future work. Use newest source revisions when resumed; do not revive
 * deleted workbook scope or alter D1 to guess later mechanics.
 * Source: https://docs.google.com/document/d/1oXHIKtIdWlQDwzf1JaHzBut08g4tytXs6rCYRY-89NY
 * Source modified: 2025-08-22T22:56:40.140Z
 * Source: https://docs.google.com/document/d/16Eq0yp7NTI57AK22s8vXrkoRYFp6BYrcdNjfbehWrcw
 * Source modified: 2026-01-18T15:54:24.799Z
 * Source: https://docs.google.com/document/d/1Y1T-L7qRv3GWr11fcq9vuq_rO6yORnbqc1WmYplGVWg
 * Source modified: 2025-10-03T22:50:35.453Z
 *
 * TODO(M90, deferred_D2_plus): Implement D3â€™s water/pressure puzzles and Dagonâ€“Hydra state sequence
 * Current: No new D2-D5 dungeon content was implemented.
 * Required: Keep the finding's linked rooms, encounters, puzzles, endings, persistent outcomes and
 * inter-dungeon progression as future work. Use newest source revisions when resumed; do not revive
 * deleted workbook scope or alter D1 to guess later mechanics.
 * Source: https://docs.google.com/document/d/1u1xeNikxn02KkeFeFytocLguV869XPxW89KZW5jc1nM
 * Source modified: 2025-07-06T00:27:59.588Z
 * Source: https://docs.google.com/document/d/10DA4A5DyglUecj3ezDzmp6Kj2EtTwUpvkKNSHS3MT9M
 * Source modified: 2025-10-08T03:01:00.829Z
 *
 * TODO(M91, deferred_D2_plus): Implement D4â€™s portal maze, reflection encounters and Web completion
 * Current: No new D2-D5 dungeon content was implemented.
 * Required: Keep the finding's linked rooms, encounters, puzzles, endings, persistent outcomes and
 * inter-dungeon progression as future work. Use newest source revisions when resumed; do not revive
 * deleted workbook scope or alter D1 to guess later mechanics.
 * Source: https://docs.google.com/document/d/1WYUMIw4dH2z3LHeL-_n3hbJ7wCox_MRWnn12yXUbf5M
 * Source modified: 2026-08-29T12:55:53.787Z
 *
 * TODO(M92, deferred_D2_plus): Implement D5â€™s trap construction, boss endings and persistent outcomes
 * Current: No new D2-D5 dungeon content was implemented.
 * Required: Keep the finding's linked rooms, encounters, puzzles, endings, persistent outcomes and
 * inter-dungeon progression as future work. Use newest source revisions when resumed; do not revive
 * deleted workbook scope or alter D1 to guess later mechanics.
 * Source: https://docs.google.com/document/d/1fGIDiNrmvimau1CCoDvud_ufjxHFflWurKa_RD_AAEg
 * Source modified: 2025-07-15T00:58:00.373Z
 * Source: https://docs.google.com/document/d/1B6d-tK5wbYwa5dpWzlCAk90exral4k1ormCYgAG5he4
 * Source modified: 2025-10-12T00:23:14.661Z
 * Source: https://docs.google.com/document/d/1bewOBnVPQw_jicVo_mZbfvIDGeVO3u96jEsh1WRU7iw
 * Source modified: 2025-10-17T02:43:33.093Z
 *
 * TODO(M93, partial_D1): Implement physical progression-item retention under the approved exit policy
 * Current: Successful D1 inventory is retained, outside inventory remains claimable; failed/exited
 * runs restore the original inventory; instance objectives reset independently of lifetime counters.
 * Batch29: the preceding Watson outcome now freezes physical Bloom deductions, Tax eligibility,
 * success-only lifetime kills/actual Lesser Blooms, rounded contribution and faction bonus.
 * Owner and projection receipts recover interrupted saves; failure grants no permanent statistics.
 * Required: verify authored physical progression bindings, companion kill attribution, death,
 * full inventories, offline inputs and native interruption across outcome plus cleanup on TEST.
 * Q&A D20/D23 (2026-09-16) governs D1. Keep second-instance resets independent; D2+ stays deferred.
 * Batch37 routes D1 reset-rift exits through the saved cleanup handoff before any tile teleport.
 * Failed save must retain the player under lifecycle recovery; generic evacuation cannot bypass
 * RESETTING ownership. Native fault injection remains required. Existing run/lifetime codecs
 * and six-Bloom Watson receipts are unchanged; never subtract guessed old totals on upgrade.
 * Source: https://docs.google.com/document/d/1oXHIKtIdWlQDwzf1JaHzBut08g4tytXs6rCYRY-89NY
 * Source modified: 2025-08-22T22:56:40.140Z
 * Source: https://docs.google.com/document/d/1u1xeNikxn02KkeFeFytocLguV869XPxW89KZW5jc1nM
 * Source modified: 2025-07-06T00:27:59.588Z
 * Source: https://docs.google.com/document/d/1WYUMIw4dH2z3LHeL-_n3hbJ7wCox_MRWnn12yXUbf5M
 * Source modified: 2026-08-29T12:55:53.787Z
 * Source: https://docs.google.com/document/d/1fGIDiNrmvimau1CCoDvud_ufjxHFflWurKa_RD_AAEg
 * Source modified: 2025-07-15T00:58:00.373Z
 * Source: https://docs.google.com/document/d/1VwuK2NIRUTIxIMOFviWlVPZ_gz09yRjmYdSNlxxQkyY
 * Source modified: 2026-07-06T16:23:43.120Z
 *
 * TODO(M96, deferred_D2_plus): Create a checked-in approved language/codex corpus and lint inconsistent examples
 * Current: Private source bodies were not copied into Git.
 * Required: Defer the full checked language corpus, approved translations and later Webbound Priest
 * journal drops/read sequence. Revalidate newest sources before implementing; keep reveal state per
 * source and preserve existing book items.
 * Source: https://docs.google.com/document/d/1JvyunRb_cgCEVRGud91fCD3ZP83lxsbJcXbU_5-SimU
 * Source modified: 2026-08-29T00:32:32.934Z
 * Source: https://docs.google.com/document/d/1Ob5xTDxThhcXVcvW6u6KUJHUiptTkxNaU6vEx-GedD4
 * Source modified: 2026-05-09T20:36:55.170Z
 * Source: https://docs.google.com/document/d/1OCn7biuflB5sOvD9rdWJKvX7tXm27PZeJjRHH2eSO8k
 * Source modified: 2026-08-29T14:50:01.277Z
 * Source: https://docs.google.com/document/d/18COAtyZ3XOtm6aBZoWen8S4wJ3XAfD0N8igcQNAVByc
 * Source modified: 2026-07-06T16:20:40.052Z
 * Source: https://docs.google.com/document/d/1NA0B0KxsUE1yOmngBG9BDKBa85YvmaGwiWb6cyzGmf0
 * Source modified: 2026-07-06T16:53:50.165Z
 * Source: https://docs.google.com/document/d/1sBxIXgBR2jkHPwPfW1BxUT36J1DyGO8SNCpy3aTnjxU
 * Source modified: 2026-07-06T16:20:04.165Z
 *
 * TODO(M97, deferred_D2_plus): Implement Webbound Priest journal drops and staged reading
 * Current: Private source bodies were not copied into Git.
 * Required: Defer the full checked language corpus, approved translations and later Webbound Priest
 * journal drops/read sequence. Revalidate newest sources before implementing; keep reveal state per
 * source and preserve existing book items.
 * Source: https://docs.google.com/document/d/1OCn7biuflB5sOvD9rdWJKvX7tXm27PZeJjRHH2eSO8k
 * Source modified: 2026-08-29T14:50:01.277Z
 * Source: https://docs.google.com/document/d/18COAtyZ3XOtm6aBZoWen8S4wJ3XAfD0N8igcQNAVByc
 * Source modified: 2026-07-06T16:20:40.052Z
 * Source: https://docs.google.com/document/d/1NA0B0KxsUE1yOmngBG9BDKBa85YvmaGwiWb6cyzGmf0
 * Source modified: 2026-07-06T16:53:50.165Z
 * Source: https://docs.google.com/document/d/1sBxIXgBR2jkHPwPfW1BxUT36J1DyGO8SNCpy3aTnjxU
 * Source modified: 2026-07-06T16:20:04.165Z
 * Source: https://docs.google.com/document/d/1FT6k2MFKgQf_tQ5UcBn0wmqJ9-yY_Wdpjna4USdVOZA
 * Source modified: 2026-08-29T14:49:42.290Z
 *
 * TODO(M101, partial_D1): Validate personal travel gates and existing destination isolation
 * Current: Batch37 checks active run/roster/class, source physical ownership, completed exits,
 * startup/outcome/transaction holds, outside/orphan escrow and personal named-village access.
 * D1 reset rifts use the existing cleanup journal, while ordinary rifts cannot bypass Chop swaps.
 * Companionship selection uses original run ID and Overworld clock; both endpoints revalidate,
 * successful travel consumes selection, and target failure never removes the paid cooldown.
 * Required: Exercise locked Main Village aliases, actual old destination records, two slots,
 * death/logout/reconnect/restart, expired/cancelled selection and denied rift throttling.
 * Review custom-named village aliases against authored worlds; do not infer location from spawn,
 * because the legacy default main_village is seeded at World Spawn until explicitly authored.
 * Notes Teleport (2026-07-07) is future design: no new waypoint/recall/recipe system is enabled.
 * Preserve registered IDs, locations and original inventories; no partial-save automatic repair.
 * Source: https://docs.google.com/document/d/1x59OaQfNB1UNYXgLySdq5BBktq9brgcBBbqLeJYpBvU
 * Source modified: 2026-08-20T20:25:49.056Z
 * Source: https://docs.google.com/document/d/1FIcIf82rCAEAhbEE2gbR7jdoFA0BWSIcnzeOcKlUG4M
 * Source modified: 2026-07-07T18:38:29.922Z
 * Source: https://docs.google.com/document/d/1-FcHP73pFytPfoM2KhUPa6tt_2licsgWmWokto4YzE4
 * Source modified: 2026-08-19T22:35:48.808Z
 *
 * TODO(M102, implemented_unverified): Licensed startup and final inventory handoff acceptance
 * Current: Batch28 retains immutable cleanup/claim decisions in PendingDungeonRecoveryData,
 * exact player receipts, bounded stored claims and full pre-entry startup images in the run save.
 * Offline cleanup retires escrow only after its independent recovery/stash/Raw entitlement is
 * verified. Incomplete startup rolls back the entire roster after restart; committed entry retains
 * no rollback marker. Pre-registration paste failure touches no player inventory; refresh precedes reuse.
 * Required: interrupt native player/world saves and all 36 paste/class-room/teleport boundaries
 * on complete TEST copies. Verify offline success/failure, full inventories, custom components,
 * respawn/position, no duplicated loot and no trapped member. Pre-journal unreceipted records are
 * retained for review. No actual world preparation, game launch or destructive runtime test ran.
 * Source: https://docs.google.com/document/d/1-FcHP73pFytPfoM2KhUPa6tt_2licsgWmWokto4YzE4
 * Source modified: 2026-08-19T22:35:48.808Z
 * Source: https://docs.google.com/document/d/10fv5JCue39bZq8ENdDdz7QtJdJKH67f8oCIzGcAyc2Y
 * Source modified: 2026-07-12T16:12:42.442Z
 *
 * TODO(M103, preserved_verification_pending): Preserve existing spawner data when retained encounters or drops change
 * Current: Spawner IDs, serialized formats, presets and authored worlds were not changed; no spawner
 * migration is introduced.
 * Required: Verify representative old placed spawners and presets in TEST before release. Encounter
 * reward configuration must not require replacing spawners.
 *
 * TODO(M104, partial_D1): Put explicit budgets around new AI, auras, snapshots and client effects
 * Batch38: Cosmic Spawner caps read derived loaded-entity tag membership; three repeated full-level
 * scans are removed. Admission applies defaults/presets immediately; later maintenance is a fair
 * global queue capped by Performance.spawnerMaintenanceVisitsPerTick (default512). Native Tags,
 * NBT versions, presets, wandering-mob caps and one-shot decisions retain their formats/meaning.
 * Entity health/retag/load/remove hooks and level/server cleanup maintain the derived index.
 * Required: native hook and old-world acceptance plus before/after server MSPT, client frame time,
 * allocation/memory plateau and packet traffic under realistic D1 load. No zero-cost claim.
 * Outside-inventory snapshots remain bounded to once per20 ticks for eligible outside-active
 * players plus lifecycle captures. A future dirty optimization must catch in-place component,
 * damage and cursor changes and retain save-proof recovery; never skip solely on inventory count.
 * D2+ totem auras below remain TODO-only: tier radii10/12/14, durations20/30/60seconds,
 * cooldowns17/15/13minutes; highest overlapping Bogatyr tier wins, other-class stacking only.
 * Do not run them in D1. Current Q&A D25 any-lit-campfire travel overrides the older six-socket,
 * three-player,15minute/6hour enhanced-campfire proposal. No new campfire block is implied.
 * Source: https://docs.google.com/document/d/1fyiehjysrKWM0RilTxXpccmEQzdqc65Wmz0XuQRrUio
 * Source modified: 2026-07-07T22:42:25.125Z
 * Source: https://docs.google.com/document/d/1WYUMIw4dH2z3LHeL-_n3hbJ7wCox_MRWnn12yXUbf5M
 * Source modified: 2026-08-29T12:55:53.787Z
 * Source: https://docs.google.com/document/d/1fGIDiNrmvimau1CCoDvud_ufjxHFflWurKa_RD_AAEg
 * Source modified: 2025-07-15T00:58:00.373Z
 * Source: https://docs.google.com/document/d/1hlDeYD6NQZGEn8UUzifas_KERzWvsf7D_-_KB33zHaQ
 * Source modified: 2025-10-26T23:53:41.488Z
 * Source: https://docs.google.com/document/d/1zA_EA-Dt0rDHwiV0w18Meruh7LbZ4i4cA887M04gt9Q
 * Source modified: 2026-01-20T21:32:06.246Z
 * Source: https://docs.google.com/document/d/12goYIx2eXkJqVSW5QAaW4YJqTbxu-i4jwyik0Xa9u8g
 * Source modified: 2026-01-20T21:31:49.895Z
 * Source: https://docs.google.com/document/d/12_DUdRY67snqD6w3nC4XjDFuzcXRvbld-zRTp49F-6k
 * Source modified: 2026-01-20T21:31:43.346Z
 *
 * TODO(M105, partial_D1): Update help, commands and player terminology only alongside approved behavior
 * Batch38: help corrects invitation leadership, readiness, paid-at-drink Companionship cooldown,
 * logical death versus legacy physical currency, per-run/lifetime progress and class-specific
 * trident hits. Deferred classes and developer validation pages are absent from the D1 tree.
 * Beluzon's service and current achievement names are listed; Tamsin is not a retail vendor.
 * Required: read every translated menu/help page at supported GUI scales with matching protocol6
 * jars. Verify no clipping, stale balances, unsupported commands or unintended lore spoilers.
 * Existing long NPC backstories are preserved; this batch does not claim a fresh all-Doc audit.
 * Keep D2+ mechanics, secret eligible Tax item lists and developer-only commands out of help.
 * Source: https://docs.google.com/document/d/1K8HbgtOvRTYugr_X4GTquXqnWP0we87wmIz1WWEhPxo
 * Source modified: 2025-09-15T00:52:41.819Z
 * Source: https://docs.google.com/document/d/1FT6k2MFKgQf_tQ5UcBn0wmqJ9-yY_Wdpjna4USdVOZA
 * Source modified: 2026-08-29T14:49:42.290Z
 * Source: https://docs.google.com/document/d/1-FcHP73pFytPfoM2KhUPa6tt_2licsgWmWokto4YzE4
 * Source modified: 2026-08-19T22:35:48.808Z
 * Source: https://docs.google.com/document/d/1aDUTh-_AmrB3kMHKeyTDQKdIeJHBtqdyp11FPBg3vmY
 * Source modified: 2026-08-23T20:33:27.547Z
 *
 * TODO(M106, partial_D1): Validate the retained item catalogue and repair broken references
 * Current: Debloated workbook/source coverage and private catalogue audit are retained; changed
 * generated resources are validated.
 * Required: Resolve remaining same-level catalogue ambiguities against current documents and world-
 * authored items. Check runtime registry/resource references without resurrecting deleted tabs; keep
 * private sources out of Git.
 * Source: https://docs.google.com/document/d/1K8HbgtOvRTYugr_X4GTquXqnWP0we87wmIz1WWEhPxo
 * Source modified: 2025-09-15T00:52:41.819Z
 * Source: https://docs.google.com/document/d/1B3hQLrrOkZeRPG1tomd54rd7v7OKQ_PDNozHH-DIffY
 * Source modified: 2026-08-19T21:16:19.213Z
 * Source: https://docs.google.com/document/d/19bB7G3MBAmokIZeURpq2bvBydZlg-MIZEce_tVR5pAk
 * Source modified: 2026-08-16T22:48:44.527Z
 *
 * TODO(M107, partial_D1): Use the linked calculator as a checked pricing reference, not a runtime web dependency
 * Current: Pricing remains local/configured; capacity and whole-Trace rounding have offline checks.
 * Required: Add source-versioned calculator fixture cases for every manual/category exception. The
 * reference calculator is not a vendor catalogue or a runtime network dependency.
 * Source: https://docs.google.com/document/d/1B3hQLrrOkZeRPG1tomd54rd7v7OKQ_PDNozHH-DIffY
 * Source modified: 2026-08-19T21:16:19.213Z
 * Source: https://docs.google.com/document/d/17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28
 * Source modified: 2026-08-18T21:01:23.864Z
 *
 * TODO(M108, deferred_D2_plus): Keep explicit future proposals and incompatible candidates outside automatic implementation
 * Current: Explicit later proposals remain inactive.
 * Required: Preserve source-backed future requirements as TODOs only. Revalidate scope and newest
 * document revisions before later implementation; do not register incompatible speculative equipment
 * in D1.
 * Source: https://docs.google.com/document/d/1xT6KWQ_iLsygcQA-FwI0mH_79p0p4hv96l-aSlyf4rw
 * Source modified: 2026-07-07T23:53:47.006Z
 * Source: https://docs.google.com/document/d/10fv5JCue39bZq8ENdDdz7QtJdJKH67f8oCIzGcAyc2Y
 * Source modified: 2026-07-12T16:12:42.442Z
 * Source: https://docs.google.com/document/d/1a5Xy46ahjqBSNPrcw6Tgl5HTulss9n5vqNrmCVScWjY
 * Source modified: 2026-07-18T14:48:43.831Z
 * Source: https://docs.google.com/document/d/1VwuK2NIRUTIxIMOFviWlVPZ_gz09yRjmYdSNlxxQkyY
 * Source modified: 2026-07-06T16:23:43.120Z
 *
 * TODO(M114, partial_D1): Enforce item transfer eligibility at trade insertion and final confirmation
 * Batch 04: trade insertion, explicit shift-click and final confirmation use operation-specific
 * provenance/restriction checks. Invalid repair markers and unknown equipment origins are denied.
 * Required: finish reviewed legacy adoption and canonical no-drop/world-container restrictions
 * (M10/M72); test hotbar, drag, all menu packets and changed offers on licensed TEST. The offline
 * decision fixtures do not prove live ItemStack serialization, menu synchronization or crash recovery.
 * Source: https://docs.google.com/document/d/1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc
 * Source modified: 2026-08-18T22:35:34.052Z
 *
 * TODO(M115, partial_D1): Make trade item delivery and cancellation durable and owner-safe
 * Current: Normal-save trade escrow, owner-only overflow claims, run-isolated recovery and cleanup
 * prevent loose-item overflow.
 * Required: Join inventory delivery and escrow removal to the shared crash journal; prove
 * reconnect/restart and interrupted save recovery exactly once. Normal save escrow is not a
 * guarantee against process termination between independent file saves.
 * Source: https://docs.google.com/document/d/1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc
 * Source modified: 2026-08-18T22:35:34.052Z
 *
 * TODO(M118, partial_D1): Separate a legitimate zero-Trace surrender from missing-price rejection
 * Current: Approved zero prices require explicit server-quoted surrender. Missing/restricted prices
 * are rejected; Sell All excludes zero-value stacks. Quote IDs are single-use and expire. Stack
 * count/components, price, owner, vendor identity/range/access and capacity are revalidated.
 * Current: Server quotes now display base/enchantment/curse/cap-floor adjustment totals;
 * confirmation also rejects changed components when the final total is unchanged.
 * Required: Finish the shared durable transaction journal with complete item images.
 * Current account log records quote ID/vendor/zero payment;
 * it is not power-loss atomicity across independent account and player saves. Test UI in TEST.
 * Source: https://docs.google.com/document/d/1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc
 * Source modified: 2026-08-18T22:35:34.052Z
 * Source: https://docs.google.com/document/d/1B3hQLrrOkZeRPG1tomd54rd7v7OKQ_PDNozHH-DIffY
 * Source modified: 2026-08-19T21:16:19.213Z
 *
 * TODO(D1 vendor quote runtime acceptance): Server-owned stack snapshots, quote UUIDs, expiry
 * and single-use confirmation are implemented. On TEST, alter counts/components/prices, change
 * NPC assignment, disconnect, die and replay confirmations; verify inventory/account unchanged
 * on every rejection. Buyback does not mutate retail stock. Preserve the journal work in M03. Gear
 * Trading 1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc, 2026-08-18, and
 * Economy 17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28, 2026-08-18.
 */
package net.goui.cosmicdungeon.dungeon.d1;
