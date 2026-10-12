# Approved follow-up batches21–25

Cameron2026-10-12 authorized completion of20 followed by21–25. Source: Cosmic_Dungeon_Batches_21-25.md, created2026-10-12T12:19:45Z, approved by Cameron and read in full. These cards continue the current1.7 series; the archived15–26 queue remains complete. Finish20's corrective Alpha and merged-main1.8.0-beta.1/stopped TEST first.

One coordinator and one cooperative worker, disjoint writes; coordinator alone owns shared transactions/persistence/network/controller/releases. One focused task branch/PR per card. Preserve old formats/unknown data, registry IDs, authored chest contents and current resource/dependency budgets. Java21 local build, relevant regressions/JSON/diff and full clean/native CI including GameTests; no local game/client/GameTest/server launch. Implementation validation is automated; licensed manual play stays pending and outside this queue.

Alpha publication only: no sFTP/state checks/deployment downloads. Any separately authorized later Beta/official delivery follows stopped TEST policy. No stable-release or manual-testing batch. Do not reserve/reuse future version numbers. For each push retain/report all recommendations and every unimplemented genuine Codex note with priority/source/disposition; fresh pending review is not no-findings evidence.

### 21. Save and recovery hardening
Correctness first; extend deterministic interruption/replay coverage on existing transaction/recovery foundations. Reuse Batch20 Raw/Cooked Chop admission, recipient escrow, registry-before-acknowledgement and delayed-owner recovery cases; add uncovered persistent ordinary/Chop trade, shared crafting and cursor delivery boundaries, plus associated resurrection/cooldown custody handoff.
Use isolated temporary saves/CI fixtures, durable before/after images and actual saved-state reloads, rather than only in-memory helpers. If genuine process interruption is required, confine it to isolated CI; never kill/restart Cameron's server/game. Distinguish deterministic failure injection from process-crash proof.
Accept: pre/post durable decision, registry/account acknowledgement and owner delivery cannot duplicate/lose items/currency/fuel, repeat cooldown/metrics or overwrite another owner. Repeated recovery and both/reversed owner return orders. Incomplete/unsupported/conflicting legacy custody stays held with a named actionable reason, with original bytes preserved; no deletion or guessed ownership. Discovered defects receive compatible load/rollback notes and regressions.
Likely: TradeTransactions/TradeCommitPlan/ChopTradeCustody/PlayerCurrencyData; SharedCrafting/transaction plans; PotionCursorCustody; existing resurrection/native recovery guards. Coordinator owns all common persistence/transaction source changes.

### 22. Stay-mode performance
Origin P2 PR258 r4243537947 at7040a9cc3d17450077a9d211a7862fa2a9bcec84. Measure repeated Stay target-goal traversal directly, then reduce redundant work through current mode/lifecycle foundations using transition invalidation/bounded equivalent guards. Preserve protection from native/external target writes while Stay is active.
Accept: repeatable before/after counts for1/30/120 wolves explicitly include target-goal traversal; threat clearing/non-retaliation across Patrol/Stay, owner/run/epoch/reload; fresh incoming defense after Patrol, stale/allied targets rejected. No speculative cadence/heap/dependency/worker increase. If safe design retains necessary traversal, report that measured limitation honestly.
Does not add outgoing owner-attack assistance. P1 PR262 r4244287961 at59b87afcdc13aaf836d01ed6c47c5c38e256d48c remains disputed/unimplemented; preserve incoming-defense behavior pending Cameron's separate choice.

### 23. Brewing ingredient tooltips
Explain selected valid server-resolved brewing route ingredients alongside step/fuel cost, reusing bounded stock/quote snapshots. Distinguish existing potion inputs, consumed intermediate stages and newly required ingredients so multi-step routes are not double-counted; own/shared availability and actual debit rules agree.
Accept correct quantities for base/stronger/extended/multi-step routes; existing crafted inputs and saved fuel change remaining cost correctly. No fictional Luck recipe or mixed splash/lingering bypass. Empty/unavailable/stale quotes explain state; availability promises no reservation. Automated wrapping/model/payload bound checks; no recipe search/full quote in every render frame.
Update HelpMenuContent and affected player/class guides. Native visual acceptance remains Cameron's manual work.

### 24. Six-player crafting concurrency
Depends on23. Repeatable automated six consenting members in one eligible party with six simultaneous palette subscriptions and a smaller baseline. Changing stock, refresh invalidation and overlapping queued crafts preserve normal serialized server transactions. Check another party's isolation. Include23's actual final route-detail payloads.
Accept environment/fixture/sample size and before/after quote counts, inventory scans, packets/bytes and available timing distribution. Exact resource/fuel totals, donor fairness, sequence handling, full destinations and consent/dimension/disconnect rejection. Optimize only demonstrated bottleneck and rerun same workload, with no silent budget increase or transaction weakening.
Simulated viewers do not prove native GUI latency, whole-world TPS or terrain performance.

### 25. Metrics history retention
Measure XML/event-segment storage growth for completed/aborted/resource-heavy runs. Document live/history/archive/reconstruction policy; default retain-all. No purge schedule, reduced retention or database migration.
Focused copied/restored segment reconstruction/preservation checks using current formats/readers. Only if measurements justify a command: explicit opt-in, lossless, offline, refusing active/unresolved journals, verified restore and concrete destination/policy before implementation. Measurements/documented retain-all policy may complete this card without speculative archival code.
Accept stable run/owner/event identity, exact totals, last10 lookup/full resource history and no replay double counting. Missing/partial/conflicting segments fail visibly without invented totals/deleted originals. Measured bytes/run/event and projection assumptions. No automatic deletion/new retention limit.

## Outside this set
Licensed inventory/guide GUI scales, font/link/tooltips/modal/drag and Creative/Extra checks; multiplayer trade/potion/death/resurrection journeys; wolf stairs/doors/crowds/obstructed mating/return/terrain; real keyboard/native Controls; authored-world/modded component/enchantment/conditional-loot/narrow editor acceptance remain manual. Optional stalled-recall/return/key-conflict hints require actual demonstrated need. Stable is separately user-owned after licensed Beta acceptance. Preserve baseline measured bounds; terrain profiling stays conditional.
