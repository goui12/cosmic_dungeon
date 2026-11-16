# Class skills Batch 5 — Independent anti-farming protections

Cameron authorized Batch 5, then pause. Baseline f278b06daa2a718e21f5ed5b603e87051fec5885 is completed Batch 4. Branch feature/class-skills-b5-20261116 continues the unmerged six-batch series in the Goggins development checkout.

## Delivery

Version 1.8.13-beta.1 is an unpublished source beta. Development files and GitHub update only. No TEST, local client, CurseForge, publication tags or installs before Batch 6 completes. One batch remains: party UI, integration and official release. [Approved plan](CLASS_SKILLS_BATCHES_20261115.md).

## Behavior

Spawner counters are shared within the dungeon party and keyed by run, actual dimension and the existing canonical Cosmic Spawner position tag. Real accepted deaths count once; canceled and duplicate death callbacks do not. At 30 the killer's hirer privately receives the approved warning, and at 40 the refusal. All mercenaries and their owned wolves refuse that source. Fresh blocked encounters can repeat the refusal at most once per five seconds per hirer; repeated checks of the same target do not spam chat.

Melee, projectile, firework, charged lightning/Smite and harmful potion paths recheck source eligibility at damage/effect execution. Targets selected before the fortieth death do not bypass the restriction. Delayed poison/wither carries mercenary ownership through native damage and kill callbacks, including active and hidden effect save/reload; restoring origin does not award duplicate potion progression.

Actual player damage against that spawner resets its counter only with recent voluntary horizontal walking. At least one block of grounded movement with directional input in the prior ten seconds is required; no riding, gliding, water motion or large teleport jumps. Looking, jumping in place, pet damage and walking alone do not qualify. Damage received while actively walking/fighting does not invalidate participation. Any accepted group-player death resets all source counters through an epoch; relog and mercenary death/respawn do not.

Healing observes each player independently. Accepted repeated injuries no more than ten online seconds apart start a 30-second warning and 40-second refusal while horizontal displacement stays below one block. A single stale wound cannot lock healing. Once blocked, quiet time does not clear it: at least one block of X/Z displacement does. Observation time pauses offline; attacking in place has no healing-recovery signal. Per-recipient native potion filtering prevents splash bypass while other allies remain eligible. Player healing, max-health buffs and resurrection are unaffected.

Exact dialogue is retained from the [approved plan](CLASS_SKILLS_BATCHES_20261115.md), including “Your sword arm looks fine to me. I prescribe using it!”

## Compatibility and cost

Additive versioned keys use existing D1RunData maps. Existing clearRun cleanup resets the new state; unrelated/unknown map keys survive. No registry IDs, network payloads, world data versions, placed-spawner NBT, preset files or authored stacks change. Canonical native tags are read through the existing parser without loading their source chunks. Old saves begin with empty counters. Unrecognized healing encodings are retained separately while a recoverable blocked observation is installed.

Victim-local accepted-death and last-wound markers avoid a global dead-UUID cache. Projectile origin uses additive persistent metadata. Poison/wither active and hidden origins are snapshotted before native Entity serializes NeoForgeData, then restored only against matching native effect duration/amplifier entries after load. Native effect serialization itself remains unchanged. Older builds ignore additive metadata; downgrade-running the series is not an anti-farming enforcement guarantee.

Work is event driven with one observation per active player per second, bounded native effect chains and saved counters only for encountered sources. No world/entity scan, synchronous save, runtime dependency, extra thread, packet timer loop or spawner migration. Existing ten-tick mercenary decisions and bounded wolf roster commands remain.

## Validation and review

Required: Java21 local Gradle build, all source JSON, scoped diff/link checks, independent review and exact-head full Integration Gate clean build/native GameTests. No datagen is relevant because no generated resources change. The native suite covers actual threshold deaths, cancellation/deduplication, damage paths, delayed hidden-effect reload, saved run counters, rest/relog boundaries, per-recipient healing splashes and independent movement/combat recovery. The pure state tests cover 30/40-second boundaries, damage gaps, horizontal distance, offline pause, malformed storage and clock overflow.

One cooperative worker owns the isolated observation model and its tests plus native regression fixture; root owns persistence/event integration and current guides. Review corrected a duplicate threshold refusal, hidden-effect origin reload, and native save ordering before final validation. Exact changed paths, test counts, review and CI receipts live in operational batch05 evidence.

Offline Theurgist codex was checked for relevant healing lore only: source1NLiq_L2_CQub_0hdCqAxeN5D5dL7bwP7FlqlQf9rqg4 revision21, modified2025-10-17, local check2026-09-16; no conflicting anti-farm mechanics. No online refresh, chest audit or full canon semantic audit is claimed; explicit approved mechanics govern.

After Batch6 delivery, test private warning/refusal visibility with two human players, two mercenaries and owned wolves; cross the same-spawner30/40 thresholds; clear only by walking plus combat or a player death. Test repeated stationary damage30/40seconds, splash on an eligible neighbor, horizontal recovery, standing attacks, relog, mercenary revival and a new dungeon. Native GUI/co-op acceptance remains TESTING. All17 P2 entries remain in the [active ledger](BATCH_1_7_20_REVIEW_NOTES_20261012.md).
