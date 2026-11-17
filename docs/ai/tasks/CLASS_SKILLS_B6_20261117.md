# Class-skills Batch 6 — party UI, final integration and official release

Baseline: `88cf9ef3fb0467dbafe9ee0f5310dee13d54e2ed` (Batch5, PR281, exact full CI43944376697). Current task: `feature/class-skills-b6-20261117`, release **1.8.13**. Cameron authorized this final card and official publication; no licensed acceptance is inferred from that authorization.

## Behavior and integration

Party labels retain white names, yellow classes and blue abbreviated signature levels for players and mercenaries. Active inventory/group details show charge count/armed state separately from five-section, five-minute class-buff cooldowns. Effect details show green name/amount, blue winning caster and yellow readable remaining duration. Missing historical caster data remains Unknown.

Bounded PartyVitals/PartySkills codecs carry server-time anchors and stable deadlines. Equality ignores transport sample time and elapsed cooldown ticks with the same deadline, so client countdowns do not cause repeated snapshots. Client clocks never authorize an action or enable the player's cooling slot before the server does. Protocol advances38→39; matching client/server mod versions are required.

Player Negative Potions earns once per useful harmful potion launch: real instant damage, new/stronger non-damaging debuff, or actual poison/wither damage. Each source shares credit across targets/ticks; class/run admission is captured at launch. Pending player DOT credit is conservative across native entity reload: restored effects do not invent/replay a training action. Fireworks earns once per effective native rocket; Wolves earns once per genuinely newly registered admitted owned wolf. Existing mercenary/weapon XP and positive-potion earning remain intact.

Root owns networking, native storage/progression integration, guides and releases. One cooperative worker owns client presentation/tests and the isolated FinalSkillIntegrationGameTests fixture. Source review caught and fixed weaker cosmetic potion source replacement after reload by comparing actual native pre/post effect strength and duration.

## Compatibility and resources

Existing player/run skill storage is reused. New effect-source metadata is additive in existing persistent entity data, preserves unknown fields, and does not reinterpret historical missing attribution. Native duration/attributes remain authoritative. No class-chest stacks, spawner IDs/presets, world dimensions, transactions or reset formats change; no manual migration/replacement is needed. Older code ignores the additional metadata; use a matching older client/server pair if rolling back. Existing source/CI receipts remain immutable.

No new runtime dependency, heap increase, world/chunk loading or per-tick world scan. Party rows/effects/abilities have bounded codecs; existing party sync and useful-action hooks carry deltas. Companion state uses existing loaded lookups and identity ledgers.

## Validation and acceptance

Required: Java21 full local Gradle build (without local clean), JSON/link/diff checks, full exact-head Integration Gate clean build and native GameTests, then merged-main full release tests. Three new native fixtures cover six-class player/merc snapshots, charge/buff separation, actual potion/rocket/wolf progression, rejected/duplicate/class-switch behavior, and native winning-caster save/load/unknown-field preservation. Presentation/codec/session tests cover colors, clocks, bounds and unchanged-delta equality. Existing full suites continue to cover co-op permissions, lifecycle/migrations, transactions, authored spawners and all prior five cards.

The active cooperative controller gains stable-version support only with explicit final-set official authorization. Its original evidence gates remain; original offline controller tests plus stable acceptance/rejection tests run before completion. Datagen is not relevant: no generated resources change.

Manual TESTING: at two GUI scales check all six class cards, full hover text/colors, remapped G/Z controls, local countdowns and charge/armed state; in licensed co-op check winning caster replacement, five-minute cooldowns, charged attacks, mercenary timing and both anti-farming warnings/recovery. Test death/relog/new-run transitions and CurseForge updates. [All18 remaining P2 recommendations and dispositions](BATCH_1_7_20_REVIEW_NOTES_20261012.md) remain visible; no human play is claimed.

## Publication and completion

Normal scoped source push, validated completed-set PR merge, unique annotated `v1.8.13` tag and one CurseForge Release publisher. Runtime and matching client-only loading companion publish together. Exact source/CI/publisher/file IDs and stopped TEST transfer acknowledgements are recorded in `class-skills-20261115/evidence/batch06` and its controller completion receipt. This report does not pre-claim those external results.

Only the runtime deploys to TEST after fresh stopped evidence through the existing pinned SFTP script. Never install local client/helper JARs, launch or stop games/servers, alter server.properties, or perform extra post-publication hashes/readbacks. After the final receipt, zero implementation cards remain; stop.

### Native validation correction

The first full CI run caught a native wolf reload edge: restoring health precedes the wolf taming-state maximum-health reset, briefly allowing current health above the new maximum. The party snapshot now bounds its displayed health to the current maximum without changing entity health. The packet validator remains strict, and native coverage explicitly checks both the bounded snapshot and unchanged entity health before continuing the saved-effect attribution assertions.

Mercenary roster rows use the same captured, bounded health values, preserving the outer-row/vitals consistency contract. Native six-class presentation coverage also reduces each mercenary maximum and verifies its status row remains valid without altering gameplay health.
