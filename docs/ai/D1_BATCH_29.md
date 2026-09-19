# D1 Batch29: Watson outcome recovery

2026-09-19. Authorized: Batch29 only, validation and final local commit; stop before30.
Branch: feature/d1-canon-config-20260916.
Verified parent: 77d257f101f818597f64dc687a3451d9f151fbe1 (Batch28).
Implementation candidate; native gameplay and licensed multiplayer remain untested.

## Finished

Watson now records one immutable decision for the gathered instance before consuming any
physical Blooms. The decision contains the participating UUIDs, exact before/after inventories,
personal Tax evidence, actual successful-run statistics, rounded contributions and configured
faction bonus. It uses the existing D1 objective store and shared native save/readback helpers.

Each distinct registered Bloom costs one item across the roster. Duplicate stacks, unrelated
items, components, armor/offhand slots and full inventories are preserved. All six identities
produce success; an incomplete set produces failure and consumes the available distinct inputs,
as in the previous hand-in. Developers and other instances remain excluded by Watson gathering.

Owner inventory, Tax eligibility and the owner receipt save together. The world acknowledges
each input only after its native player-file readback succeeds. Lifetime, progression and faction
apply their own per-owner receipt in the same file as the reward. Independent file writes can
resume without repeating awards. Online scoreboards are projections of the saved lifetime totals.

Only a settled outcome authorizes its matching final inventory cleanup. Kick, link-dead removal,
abandonment, reset retry and direct instance restore cannot replace or bypass that outcome.
Login and a bounded poll resume pending inputs; missing owners reconnect individually.
Players with an unsettled input are disconnected with a recovery message, retaining evidence.
The existing Batch28 cleanup/claim journal then handles inventories, Raw entitlement and reset.
After verified run retirement, full outcome images retire into compact per-owner cursors.

Q&A D20 also corrects Lesser Bloom statistics: new harvests stay in the instance until success.
A successful run adds actual newly tracked Lesser Blooms to lifetime statistics; rounded counts
add to NPC unlock contributions. Immediate harvest faction remains, and successful rounding adds
its frozen configured bonus once. Failure/exit grants no completion, kill or new Lesser statistics.
Existing lifetime totals are never cleared or retrospectively reduced.

## Sources and authority

Debloated MASTER and Q&A workbook hashes match the Batch28 baseline.
Current direct instructions and Q&A D11/D20/D23 override older conflicting prose.
Four live Google Doc metadata/body checks returned UNCHANGED before code edits:

| Source | Document ID | Modified UTC |
| --- | --- | --- |
| Watson Internal | 1e1po-TpjWQpTJ6ueIfnpTDNRZ1za3J7434Y4nFXvSmo | 2026-07-10T20:39:40.976Z |
| Bloom/NPC unlocks | 1x59OaQfNB1UNYXgLySdq5BBktq9brgcBBbqLeJYpBvU | 2026-08-20T20:25:49.056Z |
| NPC/Vendor Faction | 11Cwgha2loiAQfMJwKLWEC_dD3jir3VybyfFvrNZBUyY | 2026-08-18T21:07:27.966Z |
| Tamsin Tax | 1dIuaeMFMZaaWo7AS1zQ51kXSbdPkb9tmUm864MBs2Q0 | 2026-08-19T22:32:29.955Z |

Private revisions, workbook cells, hashes, before-images and validation receipts:
Google Docs and Sheet/Audit/D1_Batch_29_2026-09-19/.
Private source bodies remain outside Git.

## Save compatibility and recovery

No saved-data ID, item/block/entity ID, networking protocol or spawner/preset format changes.
No active world, authored chest, equipment, NPC placement or server configuration was edited.

Optional additions to existing files:

- cosmicdungeon_d1_objectives_v1: watson_outcomes and watson_receipts; owner index rebuilt on load.
- cosmicdungeon_d1_lifetime_v1: watson_receipts beside existing lifetime totals.
- cosmicdungeon_player_progression_v1: watson_receipts beside existing unlocks and unrelated fields.
- cosmicdungeon_player_factions_v1: watson_receipts beside existing faction values.
- Existing clone-preserved player class root: d1_watson_receipt_v1.
- Existing run counts: lesser_success:<UUID> tracks harvests made under the corrected policy.

Old absent extensions load empty. New malformed/foreign/overlapping/stale records fail closed.
Ordinary objective reset cannot erase a pending outcome. Each projection keeps only its latest
Watson cursor per UUID; full pending inventory images retire after verified run retirement.

Older watson_outcome flags have no proof of which inputs/rewards were saved and stay held for
complete-backup review. Historical Lesser totals may include failed runs; there is no evidence
to subtract those safely. An upgraded active run only adds its new lesser_success counts to
lifetime, preserving older already-credited harvests. Those legacy-review tasks remain code TODOs.

Before a future TEST upgrade, back up the complete stopped world, playerdata, data, level.dat,
instance mappings and server configs together. Test a copy with matching jars. Do not delete
receipts, copy individual old player files over newer world data, or force-clear held outcomes.
A downgrade must restore the matching complete backup: an older jar may ignore/drop new fields.
No actual migration, restore, deployment or server restart was performed in this batch.

## Configuration and cost

CosmicDungeon.config adds JohnWatson.outcomeRecoveryPollTicks, default100, range20-1200.
One pending outcome is considered per poll; each decision has at most six participating owners.
Offline missing inputs cause no periodic disk writes. Normal settlement performs synchronous,
verified owner/projection writes only at hand-in/recovery boundaries. Real save latency and
integrated/dedicated behavior need the licensed TEST measurements; no zero-cost claim is made.

The reviewable config example contains that one new default. Existing active overrides and
all_vendors_prices.config are unchanged. No new runtime dependency, client effect, forced chunk
load, full-world scan, heap increase or network message family was introduced.

Single-writer hotspots: objective/lifetime/progression/faction codecs, inventory/currency gates,
Watson interaction, player login and dungeon cleanup/reset. Existing save-proof and inventory
codecs are reused. Access policy and gathering checks stay server-side. Other transaction types
must settle before input capture; a pending Watson outcome blocks new transfers/travel/entry.

## Validation

- Java21 offline d1OfflineChecks plus build passed.
- 7,951 offline assertions: 4,112 Watson checks, 3 new config checks and the previous 3,836.
- Two .config round trips passed.
- Watson fixtures exercise 168 combinations of success/failure, interruption and reward-file
  write order, reopening native compressed NBT at each cut. They call production plans,
  projection methods and codecs; they are not a running Minecraft server.
- Coverage includes six full inventories/offhand Blooms, duplicate identifiers/components,
  success-only grants, actual versus rounded Lesser totals, Tax timing/payment preservation,
  saturation, old save shapes, wrong/stale receipts, pending objective reset, terminal cursors,
  and attempts to substitute a different cleanup reason.
- All1,963 source JSON files parse; whitespace/diff and document links checked.
- Datagen is inapplicable: no generated model/tag/recipe/loot/advancement/resource change.
- No client, server, GameTest, GUI, actual crash, native runtime or licensed multiplayer was launched.

## Licensed TEST steps remaining

1. With3-6 dungeoneers, collect six distinct Blooms across main inventory/offhand. Add duplicates
   and a named vanilla flower. Hand in once; verify exactly one of each real identity is removed.
2. Give players0/1/5/6 Lesser harvests and different hostile kills. Success adds actual new lifetime
   harvests/kills once, rounded unlock contributions and only the rounding faction bonus.
3. Repeat with five distinct Blooms and through failure/exit. Verify no new permanent statistics,
   the original inventory return, immediate harvest faction retained and instance counters reset.
4. On isolated backed-up TEST copies, interrupt native writes at decision, every owner/ack,
   each reward projection, readiness and cleanup/retirement. Include dedicated and integrated
   owner level.dat, disk-save failure, logout, staggered reconnect and replayed interaction.
5. While outcome recovery is pending, try kick, AFK removal, Chop travel, trade/vendor/repair,
   another entry and direct reset. No path may replace the committed result or move its inputs.
6. Verify personal camp-before-success Tax eligibility, existing payment preservation, permanent
   reward/unlock projection, companion kill attribution and other-instance isolation.
7. Load representative pre-extension and ambiguous legacy saves on copies. Preserve historical
   totals and held evidence; verify authored Watson/objective/physical progression bindings.
   Measure six-player native save latency and ensure stored outside inventory remains claimable.

## Remaining work and PNGs

All101 audit IDs remain:43 implemented/runtime-unverified,22 partial D1,9 preserved/verification
pending,27 deferred D2+. M03/M93 now have the Watson transaction implemented; their broader
legacy review and authored-world/runtime obligations remain. Detailed deferred notes stay in code.
Current list: [D1_REMAINING.md](D1_REMAINING.md).

No new required PNG. Existing optional tamsin_d1_map.png is512x256: winding route to Base Camp,
signed -JHW, under src/main/resources/assets/cosmicdungeon/textures/gui/. A drawn fallback exists.

Next proposed Batch30: legacy currency/Chop/item identity recovery, scoped from the tracker.
Wait for Cameron; Batch30 is not started. A useful later improvement is operator review tooling
for ambiguous legacy records without fabricating historical rewards.

The final local Git commit follows these notes and allowlist/staged review. Verify its hash from
Git history. A local commit is not a push or deployment.

## Exact files changed

- docs/ai/D1_BATCH_29.md
- docs/ai/D1_IMPLEMENTATION_20260916.md
- docs/ai/D1_REMAINING.md
- docs/ai/tasks/d1-canon-config-20260916.md
- docs/config-examples/CosmicDungeon.config
- docs/releases/fragments/d1-batch-29-watson-outcome.md
- src/main/java/net/goui/cosmicdungeon/Config.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInventoryHandoffs.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleEvents.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonTravelRouter.java
- src/main/java/net/goui/cosmicdungeon/dungeon/FarrowsChopTravelService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1LesserBloomEvents.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1LifetimeData.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1Members.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1RunData.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1WatsonRecovery.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1WatsonService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/WatsonOutcome.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/WatsonReceipt.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/package-info.java
- src/main/java/net/goui/cosmicdungeon/economy/CurrencyService.java
- src/main/java/net/goui/cosmicdungeon/faction/PlayerFactionData.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/TamsinTaxProgress.java
- src/main/java/net/goui/cosmicdungeon/progression/PlayerProgressionData.java
- src/main/java/net/goui/cosmicdungeon/transaction/InventoryTransactionGuard.java
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/WatsonOutcomeChecks.java
