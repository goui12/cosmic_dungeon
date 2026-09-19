Latest checkpoint: **Batch34 (2026-09-19)** preserves authored spawn data, validates
per-spawner rewards, protects NPC/profile assignments and separates personal offer tiers.
**10,794 offline checks**, two config round trips,1,963 source JSON checks and Java21 build
passed. Runtime remains untested. See [Batch34](D1_BATCH_34.md) and
[four remaining batches](D1_REMAINING.md). Stop after the final local commit;35 awaits Cameron.
Older notes below are historical and do not override this checkpoint.

# Dungeon 1 implementation â€” 2026-09-16

Status: implementation candidate, not release-ready. No deployment or gameplay testing occurred.
Branch: feature/d1-canon-config-20260916; baseline 94e13b182bb8756935125af753940d1cbaee6b52.

Current checkpoint: [Batch27](D1_BATCH_27.md), 2026-09-19. Inn/First Heart implementation and
Chop travel recovery are complete in code; end-of-run inventory handoff and legacy review remain.
See [remaining work](D1_REMAINING.md) for current dispositions. Older sections retain implementation history.

## Authority and scope

Current direct instructions override the supplied Q&A, which overrides older conflicting source text.
The debloated MASTER defines scope together with its linked documents; modified time selects among applicable revisions.
The private local audit records source metadata, selected defaults, 101 finding dispositions and all 66 Q&A answers.
No private source bodies are committed. Dungeon 2â€“5 features remain detailed TODOs.
Authored vanilla equipment, chest contents, schematics, placed spawners and world saves were not rewritten.
Renaming the Java selector class to D1_Class_Selector_Block preserves its class_selector_block registry identity.

## Configuration

Both files are NeoForge SERVER configs, created in the active world's serverconfig directory.
They use TOML content with the requested .config extension.
Reviewable defaults: docs/config-examples/CosmicDungeon.config and docs/config-examples/all_vendors_prices.config.
These examples do not change a running server. Edit server values while stopped and restart normally.
Never copy defaults over established operator overrides.

CosmicDungeon.config separates Bogatyr, Dragoon, Judicator, Pyroclast, Theurgist, Venefex,
Deadeye, Metalmancer, JohnWatson, BeatrixFarrow, TamsinVane, Dungeon1, Achievements,
NpcFaction and Economy settings. Later class sections do not unlock those classes.
Death-currency settings are reserved: death debit/drop is not implemented.
all_vendors_prices.config supplies source-based vendor buy/retail, enchantment, repair and service prices.
Stock, personal unlocks and faction checks still apply. Beluzon's one-time 15-Trace service has no faction adjustment.
Known container/alchemy conversions cannot create higher vendor value merely by producing an empty container.

## Implemented D1 paths

- Six D1 classes; explicit Ready, valid selector session/range and 3â€“6-player entry checks.
- Six distinct Blooms, instance Lesser/achievement progress, Plant Flags and gathered Watson completion.
- Lifetime Blooms/Lessers/completions separate from run objectives; /d1 stats.
- Successful dungeon inventory retention and outside-inventory recovery via /d1 claim; failure restores entry inventory.
- Owned single-Chop travel, four-second lit-campfire cooking, no-space refusal and run-end Raw entitlement.
- Current vendor catalogues, separate NPC faction, per-player morning stock and source-driven prices.
- Marked vanilla repair components, timed two-party repair, direct Elias quotes and normal-save recovery.
- Bogatyr vanilla wolf tuning; Dragoon chain/passive repair; Theurgist brewing; D1 arrows and rockets.
- Registered mob rewards, 60-block sharing/death-position eligibility and rotating remainders.
- Beluzon bond/home fallback; real bell, disc, candle and configured location achievements.
- Server-side trade restrictions, normal-save escrow and owner-only overflow recovery.

These paths have build/static/offline evidence only, not tested multiplayer acceptance.

## Unfinished D1 work

Detailed source-backed TODOs: src/main/java/net/goui/cosmicdungeon/dungeon/d1/package-info.java
and adjacent feature code. The register contains notes only and enables no deferred behavior.

Tamsin's agreement/map (batch 02) and invitations/readiness/leader queue (batch 03) are implemented.
The tax is implemented in Batch 05; runtime acceptance remains pending. Batch 03 replaces the shared selector Ready pool and automatic start.
Current Q&A D24 prohibits party merge/split; do not implement older merge behavior over that answer.
A cross-file crash journal remains necessary for currency, inventory, trade, repair and Chop recovery.
Current SavedData escrow handles normal saves/reloads, not every power-loss ordering.
Death-currency loss/logical drops, complete supply reconciliation, immutable legacy loot/class-issued provenance,
durable item audit images remain unfinished; detailed quote breakdowns were added in batch 02;
server-quoted zero-Trace surrender is implemented in the September 17 follow-up.
Permanent wolf recovery across instance deletion, exact pup growth/target priority,
handheld journal reading, some objective bindings, full Inn piston/fluid protection
remain unfinished; successful-run lifetime kills were added September 17. Wither variants are explicitly deferred.
Later relics, auras, armor tiers, Deadeye, Metalmancer and Dungeon 2â€“5 content remain inactive.

## Required world setup (developer only)

Only on a backed-up TEST world after runtime work is authorized:

- In the D1 template, /d1 watson set <x> <y> <z>; verify /d1 watson status.
- /d1 objective bind <key> <x> <y> <z> supports stairway, journal_1, journal_2,
  journal_3, fire_start and fire_end. Template dimensions map to instances.
- Source defaults: journals (640,-60,60), (619,-1,119), (1637,98,4253) in D1;
  fire start (193,25,119) and end (206,28,99) in D1 Nether.
  Verify actual authored blocks; document coordinates are not proof of a world binding.
- Stairway has no guessed binding. Bind its authored uppermost chest.
- Outside the dungeon, /inn region <minimum> <maximum> approves the existing Inn region/beds.
  Inspect the authored Creaking service/Heart with established NPC tools; /inn status.
- Verify existing d1_spawn_area, woodland and Wither-room regions and vendor identities.
- Verify encounter reward defaults/overrides without replacing Cosmic Spawners.

Player recovery: /d1 claim outside active runs, /trade claim for eligible owner escrow,
and /repair claim. /d1 unready withdraws readiness. /home requires a valid Inn bond.

## Persistence and compatibility

New SavedData IDs:
cosmicdungeon_d1_objectives_v1, cosmicdungeon_d1_lifetime_v1,
cosmicdungeon_d1_stored_inventory_v1, cosmicdungeon_d1_watson_v1,
cosmicdungeon_d1_objective_locations_v1, cosmicdungeon_chop_owners_v1,
cosmicdungeon_inn_v1, cosmicdungeon_repair_recovery_v1, cosmicdungeon_trade_recovery_v1.

Existing currency, stock and run IDs remain. New receipt/day/cleanup fields are optional.
Existing explicit capacity overrides and balances are preserved, including over-cap balances.
Legacy stock anchors to its existing count/day before the next legitimate morning refill.
New item components are additive. Existing registry IDs, block-entity NBT and spawner formats remain;
no spawner migration is introduced. Legacy named/class-issued provenance is not fully migrated; batch 04 adds explicit trusted adoption.
After a world has been used, rollback requires its matching world/config backup, not only an older jar.

Selector Ready/repair payloads changed; batch 04 adds an item component and protocol 4.
Client and server require the same built jar.
Authority stays server-side; datagen does not prove dedicated-server classloading or packet safety.
No runtime mod dependency, heap increase or permanent monitor was added.

## Automated evidence

Java 21, NeoForge 21.10.64, Minecraft 1.21.10.
gradlew.bat d1OfflineChecks build --offline --console=plain:
11 objective checks, 24 saved-data/reward checks, 14 config checks and two TOML round trips.
runClientData and runServerData completed; a missing build-only cached config was downloaded normally.
No game client/server or GameTest server was launched.
The private audit records exit codes, JAR hash, JSON checks, changed files, registry-ID comparison,
generated resources, whitespace validation and baseline preservation.
clean was not used because this checkout tracks the 1.5.0 JAR.
No staging, commit, push, deployment or server configuration changes occurred.

## Multiplayer acceptance still required

1. Back up world/configs; load old accounts, stock, blocks and spawners without replacements.
2. Invite 3-6 players, begin readiness, personally confirm and have the leader queue; test cancellation and isolation.
3. Collect six distinct Blooms with active/inactive players; complete flags and resolve Watson exactly once.
4. Repeat success, failure, exit, disconnect and full inventory; verify escrow, resets and lifetime totals.
5. Test Chop travel, wrong owners, destroyed return campfires and full inventories.
6. Buy/sell across faction tiers, sunrise/time rollback, capacity and milk/honey/alchemy conversions.
7. Repair each percentage/kit; test disconnect/range/cancel and unmarked-component bypass rejection.
8. Trade allowed/blocked items, close menus with held stacks and reconnect; crash-journal acceptance remains separate.
9. Test every arrow/rocket on living/undead/shielded targets, durations and strongest-effect rules.
10. Tame/breed/heal wolves; unload/reload and verify cap/ownership; record unresolved reset behavior.
11. Bind Inn/Heart/beds; test a full night, payment once, stronger respawn and travel restrictions.
12. Exercise bells/discs/candles/journals/fire/stairway/shulker milestones and a second clean instance.
13. Compare client/server performance to baseline and inspect duplicate reward/entity/recovery events.

No new custom D1 item textures are required: equipment/components use existing vanilla items;
custom effects reuse vanilla sprites through datagen.
A useful next improvement is one shared durable transaction journal for inventory and currency recovery.

## Resumed 2026-09-17: vendor quotes and kill statistics

Vendor sales now use menu-local server quotes. The quote snapshots complete stacks and
prices, expires after Economy.vendorQuoteLifetimeTicks (default 600), and can be consumed
only once. Commit rechecks vendor identity/profile, life, range, access, stack components,
counts, current approved price, Chop ownership and complete account capacity.
Approved zero-Trace items are individually selectable and require a surrender warning.
Sell All excludes zero-value items; unpriced/restricted items cannot be surrendered.
Inventory paging exposes every selectable stack. Sale accounting logs the quote and vendor
identifiers, including zero payment. Separate base/enchantment/curse rows were added in batch 02. The shared
durable item/account journal remains a TODO. No claim of power-loss atomicity is made.

Q&A D20 kill statistics: direct hostile final blows by an active D1 member, including their
tame companion, accumulate per player/run. A successful Watson resolution commits them
once to lifetime totals. Failed/exited runs discard pending kills. This metric does not
change shared currency eligibility or pay rewards. The final-blow definition is the
implementation choice for the Q&A's unspecified kill metric; environmental/assist-only
kills are not counted. /d1 stats and distinct run/lifetime scoreboards display the totals.

Save compatibility: successful_kills is optional with zero default in the unchanged
cosmicdungeon_d1_lifetime_v1 store. Existing totals and IDs remain. Run counters reuse
the existing optional counts map; a dying entity gets an additive duplicate-credit marker.
No spawner data format, authored equipment, chest contents or world files changed.
Matching client/server jars are required for the new vendor quote/decision payloads.
No runtime dependencies or heap changes. Quotes are bounded to 41 inventory slots, one
per open menu; kill counting is event-driven without scans.

Validation: 79 offline checks (21 quote, 11 objective, 33 save/reward, 14 config), two
config round trips, Java 21 build and static validation. The first attempted quote
test used vanilla registry bootstrap, which needs FML; the quote model now accepts a
copy/equality strategy and is tested with mutable fixture stacks without launching FML.
Production uses ItemStack.copy and ItemStack.matches. Real ItemStack/menu/packet behavior
still needs licensed TEST acceptance. Datagen was unnecessary: no generated resource changed.

Manual TEST checks after coordinated deployment:
1. Quote a paid stack and a zero-value stack; inspect every page; cancel without removal.
2. Confirm once; replay/expire/change stacks, prices, NPC assignment, range or access.
3. Fill account capacity; verify rejection leaves all items and currency unchanged.
4. Reconnect, die or reopen the menu; old quote tokens must not transact.
5. Kill hostiles directly and with a tame wolf; ensure developers/other instances get no credit.
6. Complete, fail and exit separate runs; verify pending reset and success-only lifetime totals.
7. Load an old save and verify original Blooms/completions plus a zero initial kill total.

Full exact-file manifest and execution evidence:
Google Docs and Sheet/Audit/D1_Resume_2026-09-17.
Historical status at the first resume: Tamsin map/invitations/tax, durable cross-file journal/death
currency, legacy item provenance, permanent companion recovery, full Inn protection
and actual world bindings/multiplayer acceptance remain. No new item texture is needed.


## Batch 02: Tamsin entry and itemized vendor quotes (2026-09-17)

Current tracker: D1_REMAINING.md. Three scoped subissues completed in code; no runtime claim.

Tamsin binds to an existing living NPC by UUID and a nearby existing D1 selector.
First interaction offers Yes/No. No closes without recording acceptance. Yes records
per-player acceptance and shows a winding map ending at Base Camp, signed -JHW.
Continue opens the six-class selector; accepted returning players skip to selection
or Ready according to their current D1 class. Closing the map retains acceptance.
There is no new inventory map item, fee, faction gate, NPC spawn or equipment change.

Developer setup on TEST, after runtime authorization:
- Stand in the Starting Area dimension and run /d1 tamsin bind <npc UUID> <selector x y z>.
- The NPC must be alive, not a player, outside D1 templates/instances, and within
  TamsinVane.selectorInteractionRange of the existing loaded D1 selector.
- /d1 tamsin status lists the number of bindings; /d1 tamsin unbind <npc UUID> removes one,
  even if that NPC is unloaded or missing. Developers see setup guidance, not player class selection.
- Existing players must explicitly accept; a saved class never implies prior acceptance.
  Bind the authored Tamsin before deploying this gate or first entry will be unavailable.
- At batch 02, the compatibility Ready/countdown path remained; batch 03 replaces it.
  Tax eligibility and durable item surrender remain M39, dependent on trusted provenance.

The new overworld SavedData cosmicdungeon_d1_tamsin_v1 has optional accepted_players
and bindings fields. No existing save schema or registry ID changes. It is independent
of instance cleanup. Unbind/rebind does not erase player agreements. Normal restart
persistence is covered; a forced kill before save can lose a just-recorded agreement.

Server requests validate active container, conversation stage, accepted UUID, living
NPC identity/current binding, range, session expiry and absence of an active run.
Client selection/Ready packets include container IDs. Selector responses are ignored
by other containers. Batch 02 used protocol version 2; batch 03 requires version 3 on both client and server.
Work remains bounded to the open menu and direct UUID lookups; no new tick scans.

Vendor quotes show whole-stack base, enchantments, curses, cap/floor adjustment and
final Trace. Exact arithmetic rejects overflow. Existing price/config/default and
bucket-before-enchant/floor/cap ordering remain. Confirmation rechecks the breakdown,
so equal net totals cannot conceal changed price components. Quotes remain temporary;
the durable cross-file item/account journal is still M03/M118.

Optional artwork: 512 x 256 PNG at
src/main/resources/assets/cosmicdungeon/textures/gui/tamsin_d1_map.png.
Include the full winding route, Base Camp label and -JHW signature; it displays at
206 x 103 GUI pixels, so use large lettering. The schematic fallback works without it.
No required new item textures, item registrations or datagen outputs in this batch.

Manual acceptance: decline/accept/close/reopen/restart with two UUIDs; legacy class and
no-class cases; move away, expire, kill/unload/rebind NPC; replay old container actions;
attempt class/Ready during agreement/map and from another run; inspect all six classes.
Check map layout at normal GUI scales and optional-art reload. Inspect three quote
lines per page, zero surrender, curses/floors/caps and same-total config changes.
At batch 02 the group interface was unfinished; batch 03 adds it. Tax and durable crash recovery remain unfinished.

Batch 02 validation: Java 21 build exit 0; 318 offline checks (31 Tamsin flow/save/packet,
208 price-component fixtures, 21 quote, 11 objective, 33 save/reward, 14 config) plus two
config round trips. 1961 source JSON files valid; git diff --check passes. All new classes
are present in the candidate jar. Built-in test export re-emitted the gameplay example;
no operator/live config changed. No datagen required and no gameplay runtime was launched.
Receipt and exact hashes: Google Docs and Sheet/Audit/D1_Batch_02_2026-09-17/BUILD_RECORD.json.


## Batch 03: invitations, personal readiness and leader queue (2026-09-17)

M37 is implemented with runtime acceptance pending. The source is Tamsin Vane,
modified 2026-08-19T22:35:48.808Z, revalidated unchanged for this batch. Q&A D24 overrides
the older group-merging text. This batch adds no merge or subgroup-transfer operation.

The first eligible player accepting an invitation forms the sender's group. Only its
leader invites. First-time recipients can accept in chat and finish their personal
agreement/class selection; acceptance reserves no seat and does not extend expiry.
Full groups, unavailable inviters and preparation close pending invitations.
Players can leave before preparation; a leader leaving disbands the lobby, without
promoting another leader. These are explicit implementation choices for unspecified cases.

The leader starts a ready check for an eligible, nearby 3-6-player roster. Everyone
confirms their own readiness. The leader then submits the ready group to a FIFO queue;
the last Ready click does not submit it. Membership/class changes, withdrawal,
disconnect, loss of eligibility or leaving range cancel readiness and queue submission.
A previously invited eligible player joining a queued group also cancels its queue.
Preparation locks the roster. Post-entry membership still belongs to the existing run registry.

The queue starts its countdown only when an instance slot is available. Existing
snapshot, 36-operation startup schematic, inventory capture and teleport paths are
retained behind exact-roster checks. Landing space is checked after schematic paste.
A preparation failure returns the lobby to assembly instead of automatically retrying.
Finishing an active run no longer clears an unrelated lobby at the same selector.
Cross-file crash atomicity and failure-injection acceptance remain M102.

No new world binding is needed beyond batch 02's authored Tamsin/selector binding.
The existing MaxPlayers block-entity field and UI now set a capacity ceiling; entry
requires at least three, not exactly the ceiling. Existing selectors defaulting to
three remain three-player capacity until deliberately configured for more on TEST.
Existing destination names/slot ordering remain; the leader occupies the first slot
and later members retain their join order. No blocks, NBT IDs or schematics are migrated.

New CosmicDungeon.config keys under TamsinVane (implementation timing defaults):
- invitationLifetimeSeconds = 600
- invitationCooldownTicks = 100
- partyPollTicks = 20
- partyActionCooldownTicks = 4
Existing readyCountdownSeconds = 5, minimumPartySize = 3 and range settings still apply.
All values are server-controlled; the checked-in example is reviewable only.
Vendor prices and operator configs remain unchanged.

Protocol version 3 replaces the old selector Ready packet with bounded party action/view
payloads. Client and server must use the same jar. Server handlers check the active
container, stage, NPC/selector binding, range, accepted UUID, roster revision, leader
authority and exact class snapshot. Ready acknowledgments share one revision so
simultaneous confirmations work; stale membership/queue actions fail. Invite tokens
are recipient-bound and expire. Menu actions and outgoing invitations have cooldowns.

Groups, invitations, ready flags and queue position are intentionally temporary:
restart clears them and players regroup. Persistent agreements, active run saves,
outside inventory and lifetime counters retain their existing storage. No new SavedData
or registry ID is introduced. The server checks only online group/invite/viewer UUIDs
at the configured interval, sends changed views only and does not scan world entities.
Schematic preparation retains its existing synchronous cost; measure it on licensed TEST.

Manual TEST acceptance, after separate runtime authorization:
1. Bind Tamsin; use 3, 4 and 6 licensed players with selector capacity matching the test.
2. Invite a returning player and a first-time player; accept before onboarding, fill
   the group, expire/decline/replay tokens and verify nobody gains a reserved seat.
3. Try self-invites, nonleader invites, cross-group joins, another selector, invalid
   containers, stale revisions, spectators, developers and active-run members.
4. Begin readiness, confirm simultaneously and verify only the leader submits the queue.
5. Join/leave, change class, disconnect, die, move away and unready before preparation;
   everyone must confirm again. Verify leader departure disbands without a subgroup.
6. Queue two groups at one selector; exhaust/release slots, inspect FIFO/countdown,
   and complete another active run without disturbing either waiting lobby.
7. Fail preparation at snapshot/paste/registration/each teleport. Verify no duplicated
   inventory, stranded player, orphan run or consumed queue after cancellation.
8. Restart before entry; confirm lobbies clear while agreements/run/lifetime saves persist.
9. Inspect minimum supported GUI height, scaling, keyboard focus, roster/invite controls,
   map fallback and dedicated-server packet/classloading behavior.

Final Java 21 build passes: 468 offline checks (146 party, 31 Tamsin, 208 price
breakdown, 21 quote, 11 objective, 33 save/reward, 18 config) and two config round trips.
All 1961 source JSON files parse; whitespace checks pass. New classes are in the jar.
Only four new Tamsin timing defaults were added; existing gameplay/vendor values match.
Exact hashes are in Google Docs and Sheet/Audit/D1_Batch_03_2026-09-17/BUILD_RECORD.json.
No required new PNGs. Optional tamsin_d1_map.png remains 512 x 256 with a working fallback.
Next useful improvement: trusted item provenance shared by vendor/trade gates and Tamsin Tax.


## Batch 04: trusted D1 item identity and transfer pricing (2026-09-17)

The newest Dungeon Dropped Gear and Items source (2026-08-28) supplies 23 stable named
D1 identities, their exact vanilla base types and final vendor payouts. Gear Trading
2.0 prohibits inferring eligibility from display names. All nine applicable source
revisions were live-revalidated unchanged; workbook/Q&A hashes match the prior batch.

The additive cosmicdungeon:item_provenance component records schema 1, trusted origin,
optional named identity and exact vanilla base type. Only explicit developer adoption
and the server's configured vendor-delivery path write it. Existing quantities,
names, lore, enchantments, damage and other components are copied unchanged.
No custom item registration, equipment replacement, new PNG or datagen output is needed.

Developer authoring, on backed-up TEST only after runtime authorization:
- /d1 item inspect: inspect held provenance without changing the item.
- /d1 item named <identity>: adopt an existing correct vanilla stack as a catalogue identity.
  Example: /d1 item named recovered_spyglass while holding the developer-approved spyglass.
- /d1 item approve_loot: approve ordinary dungeon equipment without assigning a named identity.
Close other item interfaces first. Unknown IDs, wrong base types, already classified items,
class/bound/no-sale/no-trade/no-drop restrictions and repair components are rejected.
These commands deliberately require the developer role. They do not infer identity from
names, manufacture enchanted equipment, remove restrictions or edit chests/presets.
Review each authored stack against the source before approval; this is not a completed
bulk migration. No authoring command was executed in a world during this batch.

Guarded equipment and spyglasses now need trusted origin before vendor sale or player
trade. Unclassified legacy equipment remains usable under existing class rules and
retains all data, but cannot enter these transactions until reviewed/adopted.
Ordinary food/materials keep their previous paths. Valid marked repair components retain
their separate eligibility; invalid markers cannot pass as equipment. Existing owner,
class, quest and no-drop restrictions override a valid identity. No-sale applies to
vendor sale; no-trade applies to player trade. Raw Chop's established owner-checked
purchase path remains separate. World dropping/container transfer protection is still unfinished.

Configured vendor equipment gets generic retail provenance on its delivered copy before
capacity/withdrawal checks. Named dungeon loot cannot be issued through this retail path.
Vendor profiles, world stock, normal items, active configs and source documents are unchanged.
Trade offer slots, explicit shift-click and final confirmation use the same server policy.
Vendor quotes recheck complete components, eligibility and current prices at confirmation.

All 23 final named payouts are under NamedDungeon1.<identity>.purchaseTrace in the existing
all_vendors_prices.config. They include the authored enchantments; the quote displays
Listed / Ench: included and does not add the enchantments again. Actual enchantment
legality and compatibility are still checked. Durability and renaming do not change identity.
The existing conversion cap can lower a listed payout. -1 disables a named purchase;
0 retains explicit zero-value surrender confirmation. Existing price values are unchanged.
There are no new gameplay modifier values or separate configuration files.

Protocol version 4 requires matching client/server jars due the new item component.
No SavedData ID, spawner field/version, registry ID rename, NBT migration or world scan.
Unknown/malformed future component strings remain stored and fail closed at transaction
time; they are not silently discarded or reclassified. Keep matching world/config/jar
backups for rollback after real use. Existing spawner/preset and inventory codecs carry
normal stack components, but their actual old-world round trips remain runtime QA.

B04-1 catalogue/adoption, B04-2 named pricing and B04-3 transaction eligibility are
implemented. M10/M12/M72/M114 remain broadly partial because legacy coverage, no-drop
paths, conversion review and live acceptance remain. M39 Tamsin Tax is still a detailed
TODO: personal Base Camp then first-success eligibility, explicit 23-item allowlist,
one-item confirmation, no Trace/stock effects and once-per-UUID removal/achievement
recovery through a durable transaction journal. No deferred tax behavior was activated.

Validation: Java 21 build and 727 offline checks, including 233 new identity/codec/policy/
pricing checks and 44 config checks, plus two config round trips. The source's 23 rows
were compared directly with exported defaults. Real ItemStacks, menus and world migration
are not exercised by the pure fixtures. Exact files/hashes, logs and preservation checks:
Google Docs and Sheet/Audit/D1_Batch_04_2026-09-17/BUILD_RECORD.json.

Manual TEST acceptance before release:
1. Confirm old item counts/components remain intact and unclassified equipment is rejected.
2. Adopt a reviewed named stack; reject wrong types, forged/unknown identities and nondevelopers.
3. Rename/damage/repair it; verify identity, restrictions and exactly the configured payout survive.
4. Quote a sale, change identity/count/price or replay confirmation; reject stale transactions.
5. Test all 23 prices, disabled/zero settings, conversion caps and impossible enchantments.
6. Buy vendor equipment; verify retail origin, inventory-full rollback and no named retail stock.
7. Trade approved loot via click/shift/hotbar/drag; reject class, bound, no-drop and changed offers.
8. Verify no-sale-only and no-trade-only behavior separately and inspect ordinary repair kits.
9. Save/reload items in inventory, escrow, chests and existing spawner presets; check unknown schema.
10. Load matching jars on dedicated/integrated clients; inspect listed-price UI and performance.
No game runtime, deployment, commit or push occurred. No required new PNG; optional Tamsin map unchanged.
Next improvement: one durable item/achievement transaction journal for Tamsin Tax and related recovery.

## Batch 05 - 2026-09-18: The Tamsin Tax

Supersedes the older batch 04 Tax deferral above. Three scoped subissues are implemented:
B05-1 one-player surrender receipt/recovery, B05-2 personal eligibility, B05-3 payment UI.
The live Tax, Tamsin and named-loot documents were rechecked: all three unchanged.
Debloated workbook and Q&A hashes remain unchanged. Source choices and exact evidence
are in Google Docs and Sheet/Audit/D1_Batch_05_2026-09-18.

Personal eligibility lives in NeoForgeData.cosmicdungeon.tamsin_tax_v1, outside run_temp.
A living, nondeveloper, nonspectator D1 member must personally enter the configured radius
of the authored base_camp binding. A subsequent Watson success records the first qualifying
success. Camp discovery survives failure; eligibility survives later runs until payment.
Existing clone handling preserves this root; run reset removes only temporary fields.
Old saves without proof receive no inferred credit from party flags, loot or lifetime totals:
their first verified visit and subsequent successful run establish eligibility.

Setup on the licensed TEST world: use /d1 objective bind base_camp <x> <y> <z> at the
authored camp in its D1 template or instance; physical slots resolve to the template.
No default coordinate or guessed region. Existing /d1 tamsin bind links the authored
Starting Area NPC to its nearby D1 selector. No NPCs, regions, chests or spawners were edited.
CosmicDungeon.config / TamsinVane adds baseCampPollTicks=20,
baseCampDiscoveryRadius=4.0 and taxConfirmationSeconds=30. These numeric defaults are
implementation judgments, not quoted canon. Existing selector range and action cooldown
also govern the interaction. Active configuration files were not changed; examples updated.

Returning eligible players see the Tax menu before normal Tamsin onboarding. It names
every carried approved stack, including equipment/offhand, over bounded pages. Selection
uses server-issued identity plus exact base type and the frozen 23-entry Tax allowlist.
Class, acquisition by trading, renaming and durability do not change eligibility; ordinary
items and name-only lookalikes fail. Catalogue additions do not automatically expand Tax.
Tamsin's reminder does not reveal the required item category. Confirmation identifies the
item and warns that exactly one is permanently removed without any currency payment.
Cancel clears confirmation; Later returns to normal Tamsin flow. Reopening creates a new
menu. Tokens, container/NPC binding, distance, life state, expiry, eligibility, stack count
and all components are checked server-side. A changed stack consumes no item.

A successful confirmation stages item removal and a schema-one receipt together in the
same player NBT snapshot. The receipt stores UUID owner, random transaction ID, selected
slot, canonical identity, timestamp, quantity and complete before/after item images.
The current player file is saved and read back; receipt, inventory and equipment must match.
For integrated hosts, vanilla loads level.dat Data.Player first, so a full synchronous
world save also updates and verifies that authoritative copy before awarding. This may
pause a large single-player world once at payment; dedicated servers save only this player.
An uncertain save never triggers a blind refund/re-consumption: the paired state stays
in memory and the player is disconnected for normal save/reload reconciliation.
Serialization failure before any write safely restores the staged in-memory state.
A loaded valid receipt replays only the idempotent achievement, never the item removal.
Unknown/corrupt receipts stay stored and block another charge instead of guessing.

The same-file approach is scoped to zero-payout surrender. It is not a multi-owner
inventory/currency ledger and does not finish M03/M115. The award is a recoverable
projection, so a crash after the snapshot and before award may delay the achievement
until login. Arbitrary manual partial restores, disk corruption and cross-mod mutations
are outside this guarantee. Keep matching world/player/advancement backups for rollback.

Protocol 5 adds bounded Tax action/view payloads; use matching client/server jars.
The additive advancement cosmicdungeon:achievements/the_tamsin_tax is hidden until earned,
uses the existing vanilla paper icon, and retains the name-only presentation rule.
Server datagen generated it; no new item registry IDs, item textures or save-store IDs.
No required PNG. Optional tamsin_d1_map.png remains 512 x 256 with the existing fallback.

Validation: Java 21 build and 817 offline checks passed, including 86 Tax checks,
plus two config round trips. Server datagen generated the hidden advancement; all 1962
source JSON files parse and git diff --check passes. BUILD_RECORD.json holds exact hashes.
Fixtures model the storage boundaries; actual Minecraft menus, item serialization, file
replacement failures, integrated host reload and multiplayer behavior remain untested.
Manual TEST acceptance must cover:
1. Personal versus party discovery, success before/after camp, failed runs and reconnects.
2. All 23 identities, traded/damaged stacks, armor/offhand, clones and unknown provenance.
3. Cancel/Later, all menu pages, expiration, NPC unload/rebind, movement and wrong packets.
4. Changing selected count/components/slot; forged/replayed tokens; no loss on rejection.
5. One removal, no Trace/stock mutation, hidden-to-earned advancement and repeat refusal.
6. Hard restart before/after snapshot and award; receipt replay and no second removal.
7. Save/readback exceptions on dedicated and integrated hosts, including level.dat copies.
8. Existing world/spawner IDs, run objective resets and separate lifetime totals.
No gameplay launch, local GameTest, deployment, commit or push occurred.

## Batch 06 - 2026-09-18: Protected moves and deliberate item adoption

Three scoped subissues add voluntary drop/menu guards, held-stack authoring, and loaded
container authoring. They do not yet finish M10/M72/M114 lifecycle or migration work.
Source Gear Trading 2.0, named drops and Dragoon Repair 2.0 were revalidated unchanged.

Class-issued and explicit no-drop stacks cannot leave inventory through Q/drop-stack,
menu throw or outside-window drop. Bound/no-trade/class-issued stacks cannot be inserted
into shared containers, portable bundles, frames, armor stands or Allays by these paths.
Same-player inventory/ender chest, vanilla anvil/enchant and the existing owner-checked
Dragoon repair service retain deliberate slot placement. Protected quick-drag is rejected;
shift-click in unknown multi-container menus requires deliberate pickup/placement instead.
Taking an authored item out of a legacy container remains possible with ordinary pickup.
Ordinary dungeon loot stays droppable. Chop keeps its own drop/pickup ownership system.
Existing container/bundle contents are checked recursively with depth 8 / 256 stack budgets;
budget exhaustion blocks risky movement rather than interpreting unknown content as free.

The menu guard runs server-side after vanilla thread/menu/slot validation, before inventory
mutation or accepting client-predicted hashes. Rejection sends the authoritative inventory.
Keyboard dropping is intercepted before removal. No late ItemToss cancellation is used.
Developer authoring bypass remains explicit through AccessPolicy; normal creative status
alone is not permission. No new packets, registry IDs, persistent fields or spawner schema.
Protocol remains 5. Mixins are listed in the existing hand-authored mixin configuration.

Developer-only commands, with other item interfaces closed:
- /d1 item survey: read-only classification of carried equipment; no names infer identity.
- /d1 item preview <identity>: preview the exact held stack.
- /d1 item container <x> <y> <z> <zero-based-slot> <identity>: preview one loaded container slot.
- /d1 item apply <token>: apply only if target, slot and every stack component still match.
- /d1 item undo <token>: restore the exact original only while the post-image is unchanged.
Existing /d1 item named and approve_loot now create previews; they no longer mutate immediately.
The preview token is player-scoped, single-use per phase, and cleared on undo/logout/stop.
Range 16 blocks and lifetime 120 seconds are developer-configurable under ItemProtection
in CosmicDungeon.config. Undo is temporary, not a durable rollback journal; retain world backups.
The preview refuses unopened loot tables, so inspection does not generate or replace their loot.
No force-loading, bulk name matching, chest recreation, spawner migration or active config edit.

Next batch covers death/clone, menu-close/full-inventory cursor spills, scripted overflow,
owner-only drop pickup and automation. These remaining paths are explicit source TODOs.
Later legacy adoption needs reviewed world/template/drop mappings, including unloaded storage.
No required new PNG or datagen: behavior/commands/mixin configuration only.
Automated evidence and exact before/after files are in Audit/D1_Batch_06_2026-09-18.
Actual mixin application, UI prediction/resync, double-chest access, nested portable contents,
all click modes, changed-slot replay and clone/restart tests remain licensed TEST work.


## Batch 07 - protected death and owner pickup (2026-09-18)

Gear Trading and Vendor Sales 2.0 and Dungeon Dropped Gear were refreshed unchanged.
No-drop class gear survives vanishing-curse removal, death inventory dropping and normal respawn.
Exact vanilla components and equipment slots are retained. Normal keepInventory/spectator/end-return
inventory copying is not repeated. Dungeon failure/abandonment still replaces the inventory with
the outside snapshot; this batch introduces no alternate stash or retention entitlement.

Player-dropped equipment/provenanced items/bound tokens receive the vanilla persisted world Owner.
This implements the rule that equipment world drops cannot replace protected trading.
Existing explicit Chop owners win. Unowned encounter drops and ordinary food remain unchanged.
Other players, vanilla mob pickup overrides, hoppers, dispensers and droppers cannot bypass the
owner/binding. Droppable equipment can still burn or despawn. No new immunity, currency or item
identity is inferred. Automated trap supplies without protected contents remain functional.
A protected item in a dispenser/dropper or pushing hopper blocks that bounded transfer cycle.

No new save schema, network version, configurable balance modifier, item registration or PNG.
Third-party capability extraction and protected cursor/forced-unequip overflow remain explicit
follow-ups. No runtime claim is made for mixin application: TEST must verify death at each
equipment slot, keepInventory, clone, logout before respawn, owner pickup after chunk reload,
allay/piglin collection, hopper minecart and ordinary trap behavior.

Validation: Java21 offline checks/build; exact mapped 1.21.10/NeoForge hook signatures inspected.
Private batch receipts: Google Docs and Sheet/Audit/D1_Batch_07_2026-09-18.


## Batch 08 - exact overflow recovery (2026-09-18)

Protected cursor/menu input spills and forced wrong-class armor unequip return the exact remainder
to the owner. Serialization is checked before known menu/armor sources remove their items.
Room is used in the ordinary inventory first. Overflow is stored in the optional
cosmicdungeon.protected_item_returns_v1 compound beside vanilla Inventory/equipment in the
player save. Class clone preserves this root. Logout closes the menu before PlayerList saves.

Players make room, close their interface, then use /d1 recover. A single command decodes at most
ItemProtection.recoveryStacksPerClaim entries (default 32, range 1-128) in CosmicDungeon.config.
There is no background inventory serialization/poll. Claims preserve partial counts and all
components; unknown records are retained. Picking up more protected menu items is blocked while
recovery is pending so routine overflow is not an expandable storage interface.

Run ID 0 identifies outside belongings; a positive ID identifies its dungeon inventory.
Village escrow remains outside even while the player is tracked on the run roster.
Cleanup closes menus BEFORE removing escrow context, then explicitly releases successful D1
recovery or discards failed-run recovery. Outside items survive failure. Pending-reset claims,
wrong-run claims and outside claims inside a dungeon are rejected.

No packet/registry changes or PNG. The optional player field does not migrate old items.
This co-located inventory/recovery snapshot is not a cross-file currency/trade/repair journal.
Open-interface forced shutdown and world-source save ordering still need those separate journals
and licensed dedicated/integrated TEST acceptance. No gameplay was launched or deployed.
Private receipt: Google Docs and Sheet/Audit/D1_Batch_08_2026-09-18.


## Batch 09 - bell/candle actions and shared credit (2026-09-18)

Bells now count only after the mapped BellBlock ring method succeeds. Six unique physical bells
in Camp 5 must fall within the configurable rolling window. D70's first explicit two-second
instruction supplies 40 ticks; the later conflicting six-second phrase is not another active rule.
Achievements.synchronousPealBellCount defaults to six. No generic BLOCK_CHANGE can award a ring.

Sixfold Vigil scans the whole authored Wither-room binding rather than a cube around the last
changed candle. Round-robin jobs advance at most Achievements.candleScanBlocksPerTick coordinates
globally per tick (1024 default); final candidate candles also receive a small bounded validation.
candleScanMaxRoomBlocks caps an authored room at 1,048,576 by default. No room-position list or
chunk loading is created. Replaced/reset runs and changed bounds invalidate queued jobs.
All qualifying colors must still be lit on chiseled tuff together when credit is awarded.
Wither variants remain explicitly deferred under D71.

Bells, candles and music now capture eligible UUID recipients, including dead and briefly
disconnected members still enrolled in the dungeon. Developers, spectators, completed exits and
Village/outside escrow are excluded from new room credit. Online players must be physically in
one of that run's dimensions. Offline eligibility follows the retained roster and escrow state.
Earned achievement_credits are optional permanent fields inside cosmicdungeon_d1_objectives_v1,
co-saved with the run award marker. Cleanup clears counters/markers, not earned entitlements.
Login/respawn replay is idempotent and never consumes a separately saved receipt. Supported
shared advancement IDs are allow-listed; no prior recipients are invented for legacy awards.

No packet/registry changes, world binding edits, datagen or new PNG. Runtime mixin, multiplayer,
region and performance acceptance remains pending. Handheld journals, Stairway binding and
other M79 content continue in the next batch.
Private receipt: Google Docs and Sheet/Audit/D1_Batch_09_2026-09-18.


## Batch 10 - canonical journals and Stairway receipt (2026-09-18)

The three linked journal bodies were refreshed unchanged and extracted from native paragraphs.
Game-facing prose is packaged in data/cosmicdungeon/lore/d1_journals.json with checked body hashes.
Written books use vanilla assets, conservative 180-character pages, canonical titles and an
unspecified author. Words/punctuation are preserved; whitespace and page boundaries do not
define identity. Both raw and filtered content must match, together with a trusted schema/edition
marker under CUSTOM_DATA.cosmicdungeon_d1_journal. Rename or pickup alone cannot credit reading.

Developer commands:
- /d1 journal create <1|2|3> creates one canonical vanilla written book when inventory has space.
- /d1 journal preview <1|2|3> checks an unmarked held signed book against the selected canonical text.
- /d1 journal apply <token> changes only its marker after exact slot/dimension/scope/content checks.
- /d1 journal undo <token> restores the exact original within the authoring expiry.

Handheld credit follows an actual server book-open packet path, for the same currently held stack.
Bound lectern credit checks the trusted journal identity as well as the actual opening.
Each player reads all three distinct journals during one run. Personal earned Librarian entitlement
is co-saved with objective data and replays on login/respawn, surviving later run cleanup without
pre-populating new-instance reading counters. Readers cannot complete another player's progress.

Stairway gives one ordinary full-durability Elytra per UUID. Inventory delivery and the optional
cosmicdungeon.stairway_reward_v1 receipt share one verified vanilla player snapshot. Dedicated
player data and integrated-owner level.dat copies are checked before awarding the advancement.
Uncertain saves retain staged state and disconnect for reconciliation; no automatic redelivery.
A valid loaded receipt replays only the advancement. Legacy already-earned advancement saves get
no second item. A full inventory leaves the reward unclaimed; reopen the chest after making room.
The receipt does not exempt reward equipment from normal failed-run inventory loss.

Objective binding rejects non-chests for Stairway and non-lecterns for journal reading positions.
/d1 objective status reports source/default or configured positions and loaded type checks without
loading chunks. Either half of the bound double chest qualifies only through its real opened menu.
No unknown uppermost coordinate is fabricated and no world binding or chest content was changed.

No network/registry changes or new PNG. Datagen is unrelated to the standalone game lore JSON.
Actual book layout/opening, authored placements and dedicated/integrated save fault injection remain
licensed TEST acceptance. Wolves in Piglin Clothing remains a detailed functional-definition TODO
to reconcile during companion work. Private receipt: Google Docs and Sheet/Audit/D1_Batch_10_2026-09-18.

## Batch 11 - Bogatyr feeding and breeding (2026-09-18)

The refreshed Wolf Internal (2026-04-25) remains the newest companion mechanics source.
Bogatyr.pupGrowthFraction in CosmicDungeon.config defaults to 0.10, range 0-1.
Each meat item removes that fraction of remaining juvenile age, rounded up to one tick.
The exact growth adapter preserves vanilla forced-age breeding cooldown and growth particles.
Injured pups both heal and grow; adults heal before any later breeding feed. No food is consumed
for disabled growth on a healthy pup or an adult whose owner's saved pack roster is full.

Managed parents must both be full health, enrolled in the physical active D1 instance and retain
their saved owner UUIDs. New births use the first parent's owner/collar and check its saved cap,
including unloaded members. The owner need not be online. Child roster enrollment occurs on
accepted entity join instead of during a potentially canceled birth event. Rejoining the same
UUID does not occupy another slot. Existing over-cap companions are retained and counted;
lowering maxWolves blocks acquisitions without deleting pets. Owner transfer removes the exact
old run/owner entry when the wolf is enrolled under the new verified owner.

No new save fields, packets, item assets, datagen or PNG. Existing vanilla wolf armor is unchanged.
Cross-instance permanent recovery, configurable lifespan and threat prioritization remain next-batch
work. Dedicated/integrated TEST must exercise actual mixin application, simultaneous breeding,
owner disconnect/reconnect, unload/reload and third-party birth/join cancellation ordering.
Private receipt: Google Docs and Sheet/Audit/D1_Batch_11_2026-09-18.


## Batch 12 - permanent companion directory and reset preflight (2026-09-18)

Wolf Internal's permanent tamed bond now has separate storage:
cosmicdungeon_bogatyr_companions_v1.dat, containing exact wolf/owner UUID, associated run,
last observed dimension/position and a located flag. It contains no entity image and cannot
manufacture a replacement pet. Run objective cleanup never deletes this directory.
Owner indices are rebuilt from the save. Duplicate/malformed saved identities stop automatic
loading/mutation instead of silently replacing an unreadable directory with an empty one.

Actual entity addition enrolls offspring, after ordinary cancellation/duplicate checks.
Observed final KILLED removal frees its exact owner's slot; unload and dimension travel do not.
Managed wolf ticks use O(1) directory lookup and update only changed chunks. Save/removal hooks
record exact positions. There are no new entity scans, per-tick entity serialization or chunk loads.
Permanent living companions, including unloaded members and pets from previous runs, count
against maxWolves. Existing over-cap pets remain intact.

Before clearing an old roster, preserve every unknown UUID as an unresolved location hold.
The directory is explicitly saved and read back before that source roster may be discarded.
Both slot refresh and ordinary snapshot restore preflight ALL target dimensions before any
purge/filesystem replacement; a second guard protects the common preparation path.
A remaining companion or unverifiable directory save blocks world replacement.

Commands: /d1 wolves shows only the caller's roster. Developer/console /d1 wolves inspect shows
the current dimension's identities, owner, run and recorded location. Both displays are bounded
to 32 details and report the full count. No mutating forget/clone or bulk adoption command exists.

This is an intermediate safety foundation: resets with outstanding companions deliberately stop
until the next batch completes durable transfer/archive and owner recovery. Do not deploy this
checkpoint alone. Detailed source-backed TODOs describe unloaded/offline recovery, exact armor
and entity preservation, crash ordering, duration=0 permanence and deferred D2+ recruits/auras.
New independent save schema only; network5, world data, item registries and PNGs unchanged.
Actual mixin/load/save-failure/reset behavior remains licensed TEST work.
Private receipt: Google Docs and Sheet/Audit/D1_Batch_12_2026-09-18.


## Batch 13 - exact companion archive and main-world recovery (2026-09-18)

The companion directory now has optional archive records with four explicit stages:
prepared before source destruction; stored after verified reset; releasing before destination
creation; done only after the destination entity and completion receipt are verified on disk.
Older directory saves without archives still load. A completed receipt retains identity/control
flags and no stale recreatable armor/entity image.

Pending resets request only recorded unloaded chunks, using short-lived loading tickets and
asynchronous futures. Bogatyr.recoveryConcurrentChunks defaults to 2, recoveryChunkTimeoutTicks
to 200, and recoverySnapshotsPerTick to 2 globally. No forced chunks or blocking future waits.
Unknown locations hold the reset. Directory lookups use owner/dimension indices; there is no
whole-world entity enumeration or repeated per-tick inventory serialization.

Each living wolf is serialized once into native NBT before world replacement. Owner UUID,
wolf UUID, collar, variant, age, health, name and exact armor/components remain in the archive.
Passenger trees are rejected for review. The original is temporarily seated and protected;
interaction cannot change its equipment while held. Original AI/invulnerability flags are
preserved in both the archive and temporary source metadata for save-failure recovery.
Actual KILLED removal removes the archive as well as the roster slot.

The filesystem reset preflight accepts only a valid prepared archive for a still-matching source.
Completion acknowledges only the specific cleared dimensions and run. A wolf moved elsewhere
cannot become a recoverable copy merely because another world reset. Failed acknowledgements
retain/reset the archive state and queue another reset receipt attempt.

Owners use /d1 wolves recover after reset, in the main world and outside any active run.
One command returns one stored wolf, seated on a clear solid floor. Delivery preparation is
saved before entity creation. Full native entity NBT is then saved and read back from the exact
entity-region chunk before the done receipt is committed. The parser opens region files read-only
and supports gzip, zlib, uncompressed and LZ4, including external .mcc chunks. Missing/misplaced/
duplicate/truncated data cannot verify a delivery. Completion safely restores original control
flags even when the server restarts before the next ordinary entity save.

A pending delivery with no verified live UUID never automatically spawns another copy. Its exact
archive and fixed destination remain held for review. Unknown legacy locations likewise remain
visible through /d1 wolves and developer inspect. No mutating forget or speculative clone command
was added. Existing outside pets and stored pets returning to another D1 run remain the next
M44 scope, alongside duration=0 configuration and bounded ranged-threat priorities.

No new item/texture/packet registration or datagen. No world, player or live config was modified
during this coding session. Licensed dedicated/integrated tests must exercise the real lifecycle,
ticket/mixin hooks, rejection callbacks and every save/crash boundary before deployment.
Private receipt: Google Docs and Sheet/Audit/D1_Batch_13_2026-09-18.


## Batch 14 - Bogatyr threat priority and optional duration (2026-09-18)

Fresh source checks remain unchanged. Wolf Internal specifies owner defense and skeleton/ranged
priority; Q&A D27 explicitly permits a server duration override with zero meaning permanent.
Bogatyr.wolfDurationMinutes defaults to 0. A positive value counts loaded, non-archived companion
ticks; unload, archive and setting zero pause the counter. Existing elapsed active time is retained
when changing a positive duration. Counter arithmetic saturates and the counter lives in the
wolf's native NeoForgeData, so ordinary entity/archive saves preserve it.

On expiry, exact worn body armor is detached into a native ItemEntity assigned to that owner at
the wolf's location. No inventory/Chop escrow boundary is crossed. A rejected insertion restores
the armor and retains the companion for retry. The living wolf is released, its native wild
attribute defaults restored, and its exact directory/run roster entry retired. The default zero
setting performs no such retirement. Drops retain ordinary world hazards and reset cleanup.
expiryRetryTicks defaults to 20. maxWolves remains the one existing cap; no conflicting alias.

Managed wolves gain a dedicated owner-defense target goal. Eligible mobs actually targeting the
owner are ranked skeleton first, then ranged-capable, then other threats, with distance/UUID ties.
Native attack/alliance restrictions and self-defense remain. Unprovoked skeleton hunting is
suppressed for managed wolves, including when the owner is offline. Sitting and archive holds
remain passive. The goal clears only the target it applied when it stops.

Threat queries are spatial and share a per-owner UUID cache. threatPollTicks defaults to 20,
threatCandidateLimit to 64 inspected nearby mobs, and threatQueriesPerTick to 8 globally.
wolfFollowRange supplies the defense radius. Candidate selection is bounded, so dense encounters
need runtime acceptance. Cached UUIDs do not hold entity instances; owner logout and server stop
clear caches. Installed goals use transient weak identity and are reinstalled after reload.

No new packets, registry IDs, datagen or PNGs. Existing tamed-wolf kill statistics already resolve
the owner and are retained. Later-run recall and explicit review tools for uncertain recovery
remain M44 work. AI/pathing, actual armor-drop cancellation and dedicated/integrated save/crash
behavior remain licensed TEST acceptance. No game or live world was launched/changed.
Private receipt: Google Docs and Sheet/Audit/D1_Batch_14_2026-09-18.


## Batch 15 - companion identities across copied instances (2026-09-18)

Instance snapshots copy native entity region files without rewriting UUIDs. Newly enrolled wolves
and pups now receive a permanent bond UUID in native NeoForgeData. The directory and run roster
use that bond; the directory separately records entity_uuid plus dimension for physical lookup.
Live entity UUIDs are never changed in place. Old saves default entity_uuid to the original key,
and existing managed pets keep their legacy bond.

Exact archives validate the bond marker, or the native UUID for older unmarked images. Source
images retain the original physical UUID and exact equipment. After verified source destruction,
restoration assigns the bond UUID before entity insertion, avoiding collisions between copied
template wolves. Done receipts retain the bond/control flags and no recreatable equipment image.
Region save proofs continue to use the actual physical UUID.

Observed owner/physical/dimension changes must match the existing directory. Native dimension
removal permits the matching move; an archived delivery requires the exact target and transaction
marker. Unproven changes do not overwrite ownership. Persistent identity_holds protect conflicting
physical identities and block reset of their dimension. Developer review remains required before
clearing a hold; no silent rekeying or bulk pet adoption occurs.

New optional fields extend the existing companion save without changing its ID. Network5,
registries, world bindings and PNG requirements remain unchanged. 1,440 offline checks and build
pass. Actual copied-template taming, portals, add/remove hooks and interrupted saves require TEST.
No game/world/live-config operation occurred. Later-run recall remains the next implementation.
Private receipt: Google Docs and Sheet/Audit/D1_Batch_15_2026-09-18.


## Batch 16 - stored companions returning to D1 (2026-09-18)

Owners can use /d1 wolves call inside their active D1 run as a living, participating Bogatyr.
The existing /d1 wolves recover remains the main-world path outside active/resetting runs.
Each action returns at most one stored pet on clear, solid ground, seated. Pending deliveries
cannot be retargeted by repeating a command elsewhere. Help and roster output describe the paths.

Archives gain optional target_run (legacy default 0 for main-world recovery). D1 restoration
writes the current run/owner metadata before insertion, preserving permanent bond identity and
exact native equipment. Destination verification requires the same active run, owner membership
and dimension before a completion receipt. A reused physical slot is not the old run.

A rebuilt destination index covers RELEASING archives even before a wolf was inserted into the
level or its directory location updated. All reset entry points refuse outstanding deliveries.
Normal reset preflight resolves the exact destination entity and transaction, then archives that
observed wolf as the next source. Missing/mismatched deliveries stay held; the old archive survives.
This index does not depend on the owner still appearing in the run's current player list.

1,460 offline checks plus two config round trips and Java21 build passed. New checks cover native
save compatibility, reused slot rejection, destination index rebuild, death/completion cleanup
and re-archiving an observed interrupted delivery through a second run reset.
Actual class/command hooks, multiplayer, reset interruption and save failures remain TEST work.
No packet, item, texture, datagen or live world/config changes. Existing outside-wolf transfer and
reviewed uncertain/legacy recovery remain M44 implementation work.
Private receipt: Google Docs and Sheet/Audit/D1_Batch_16_2026-09-18.


## Batch 17 - existing main-world companion transfer (2026-09-18)

The owner /d1 wolves call path now accepts an existing recorded main-world companion. It loads
only the recorded source chunk, verifies actual owner/bond/native UUID, captures exact native
state, and saves a RESERVED receipt. After exact pinned source NBT is read back from disk, a
REMOVING receipt is saved before discarding the original. Destination creation remains prohibited
until both the live lookup and saved source chunk confirm absence.

The fixed source dimension/position/native UUID must still match the directory. A reservation
alone cannot authorize missing-source recovery. An armed removal can resume after restart by
checking that same saved source; it does not guess a new pet from a stale location. Verified
withdrawal yields one STORED archive, which then uses the existing destination delivery protocol.
If the owner has left D1 during withdrawal, main-world /d1 wolves recover can finish that pending
source transaction. No cross-party instance is searched or imported; direct withdrawal sources
are main-world recorded pets. Exact armor, collar, name, age and active-duration state persist.

Source steps share the global companion snapshot/transfer budget with reset preparation.
Bogatyr.companionCommandIntervalTicks defaults to 20, configurable 1-1200, and throttles per-owner
recall/recovery attempts. Logout and server stop clear transient command state.
Both new stages are saved in the existing optional archive map; no new ID/packet/PNG/datagen.
Actual source discard/save, cancellation, crash boundaries and performance require licensed TEST.
1,486 offline checks and two config round trips passed with the Java21 build.
Private receipt: Google Docs and Sheet/Audit/D1_Batch_17_2026-09-18.
The config check count is now measured rather than a manually maintained print literal.


## Batch 18 - companion rejection and explicit review controls (2026-09-18)

A synchronous false spawn result can return a prepared delivery to STORED only when the exact
candidate was never added, no matching live native UUID is observed, and its directory entry
remained unchanged. The rollback must itself verify on disk. Exceptions, observed entities or
uncertain receipt saves preserve RELEASING for review.

Developer/console /d1 wolves holds lists persistent identity holds; /d1 wolves review <bond>
reports owner, bond/native UUID, directory, archive stage, transaction and source/destination run.
These read-only commands do not request chunk loads or infer an absent pet from a directory.

In-game developers can preview /d1 wolves review <bond> preview, then apply its actor-bound token
through /d1 wolves review-apply <token>. This only re-reserves an actual living, marked, owned
main-world source in RESERVED/REMOVING. Exact directory/archive state and observed position must
still match the preview. Current real contents are captured; no older image is restored, and no
wolf is created or deleted. Copied identity holds and missing destinations cannot use this path.

Original and proposed native archive/entry images are written to unique prepared review files
before mutation, then a separate applied record after a verified directory commit. CREATE_NEW
prevents evidence overwrite. Files live under world/data/cosmicdungeon_companion_reviews/.
Bogatyr.recoveryReviewSeconds defaults to 120. Logout/server stop clear transient previews.

Damaged saves with missing prepared destinations or cloned identities remain protected for
full source/destination/backup investigation. Detailed TODOs forbid blind respawn, bulk rekey,
owner guessing or removing an offline pet's ownership slot.

The full latest Wolf Internal recheck identified a separate cap correction: five ACTIVE wolves
does not mean five lifetime bonds. Current stored archives still occupy cap slots. Batch19 must
separate active from stored counts and enforce the active cap on recall as well as taming/breeding.
No new PNG, registry or packet. Runtime and deployment remain untouched.

1,506 offline checks, two config round trips and Java21 build passed for Batch18.
Private receipt: Google Docs and Sheet/Audit/D1_Batch_18_2026-09-18.


## Batch 19 - active companion cap and owner selection (2026-09-18)

The latest Wolf Internal specifies five simultaneous ACTIVE companions. The prior implementation
also counted stored archives, which was too restrictive. A derived owner index now counts existing
wolves (including unloaded/unknown-location records), source preparations and pending destination
activations. Only verified STORED images release an active slot while retaining the lifetime bond.
The index updates on archive/owner/death/expiry transitions and rebuilds from existing save fields.

New tames and births use the active count. Stored recall checks for room before recording RELEASING,
so the reservation occupies its slot before spawning. Repeating that pending delivery does not
require another slot. Known canceled insertion returns to STORED and releases only its reservation.
Existing main-world transfers can proceed at the cap; if already above a lowered cap, the original
stays in place. No existing pet is deleted merely because an administrator lowers maxWolves.

The same maxWolves config key and default5 remain. No new save field or ID is needed.
The roster distinguishes active/stored/total and supports /d1 wolves <page>, 32 entries per page.
Owners can append a bond UUID to /d1 wolves call or recover to select one pet. Selection is filtered
through the caller's own roster before any source load/transfer. Help explains the active count.

1,527 offline checks and two config round trips pass with the Java21 build. Tests cover seven
lifetime bonds/five active wolves, reset storage, destination reservation, cancellation, repeated
receipts, death, expiry, owner change, unknown/unloaded identity and native save index rebuild.
Normal D1 Bogatyr mechanics are implemented; actual entity hooks, AI, loaded-world performance,
multiplayer and crash/failure behavior remain licensed TEST work. Damaged or ambiguous legacy saves
retain protected archives/holds for explicit backup investigation, with detailed code TODOs.
Private receipt: Google Docs and Sheet/Audit/D1_Batch_19_2026-09-18.


2026-09-19 Batch26 checkpoint: Cameron authorized Batch26 only, followed by a local commit and stop.
Canonical death intents, debit/drop/pickup/despawn accounting and bounded projection recovery are
implemented. Hotspots: account SavedData/ledger, native player/item/entity hooks, inventory guards,
dimension reset boundary and server Economy config. Optional death_currency schema1 and player
death_currency_life_v1 preserve legacy saves; no registry/network/spawner change.
2,549 offline checks plus two config round trips and Java21 build passed. Runtime, native save
interruptions and licensed multiplayer acceptance remain pending. No launch/deployment/new PNG.
M08 implemented_unverified; M03 retains Inn/travel and legacy pickup follow-ups. Batch27 awaits Cameron.
See D1_BATCH_26.md for exact files, compatibility, backup/rollback and cumulative QA.
