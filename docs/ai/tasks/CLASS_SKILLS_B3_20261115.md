# Class skills Batch 3 — Charged primary combat

Cameron authorized Batch 3, then pause. Baseline467b31e11d67e906e5fe5c4772378331de446460 is completed Batch 2. Branch feature/class-skills-b3-20261115 continues the unmerged series in C:\\Users\\Cameron\\Documents\\mod_development\\cosmic_dungeon_skills_20261115.

## Delivery

1.8.11-beta.1 is an unpublished source beta. Update Goggins development source and GitHub only. No TEST, local client, CurseForge, release tags or distribution before Batch 6 finishes. Three cards remain: shared buffs, independent anti-farming, then party UI/integration/official release. [Approved plan](CLASS_SKILLS_BATCHES_20261115.md).

## Behavior

- Players and mercenaries share a single bounded100-hit Chain Lightning bar. Effective hostile trident melee or owned thrown-trident hits charge it; denied, neutral, allied, wrong-weapon and secondary ability damage cannot. No random proc or overflow bank remains.
- G, the remappable Primary class skill, arms a full player bar. The next valid trident strike attempts the chain. Mercenaries automatically arm at100 and discharge on their next valid strike. Current radius, candidate/target limits, line of sight and triggering-hit damage multiplier remain. A wholly ineffective secondary chain preserves its full armed bar; success consumes once and awards one existing Chain Lightning proficiency success.
- Ten accepted undead deaths fill Smite. Player G and mercenary automatic use damage only hostile undead. Each effective activation consumes once and advances proficiency once regardless of target count. Ranks unlock at0/10/30/60 successful activations: Gravehammer3blocks/2.5HP, Bonegrinder4/2.5, Deathpurge5/5, Last Rites6/5.
- No-damage Smite preserves charge and shows exactly: Smite only works on undead! You did not damage any mobs
- Native accepted-death tail prevents canceled or duplicate deaths from charging. Administrative kills are excluded. Secondary kills and later environmental finishes attributed to secondary damage do not charge; an effective primary hit replaces that provenance. Existing kill-credit and legacy Combat history remain intact.
- Shared inventory primary buttons display charge, skill/rank, state and actual remapped italic key; successful input gives a short overlay. The server revalidates actor/class/run, current charge and request revision. Generic bindings stay G/Z/X/C; unused slots remain disabled. The final world charge HUD and party UI are Batch6.

## Compatibility and cost

Additive version1 keys live in the existing D1 run store, keyed by player or mercenary contract and signature skill. Bars/arming survive save/load, death recovery, reconnect and class switching inside the same run. Other classes cannot spend them; normal run cleanup removes them. Invalid values fail closed and unknown run fields survive. Old mercenary entity lightning fields remain readable/writable unchanged, but their secondary-inclusive bank is never imported into the new direct-hit bar. Legacy Combat counters and resurrection are preserved. No data-version or wire-codec change is needed; protocol37 from Batch2 remains.

Old binaries ignore the additive bars/provenance and restore their own old combat rules; saving downgraded newly registered Batch2 potions/effects is not supported. This series has not been installed. No authored chest, equipment, preset, spawner, loot or resource transactions are changed.

Charging is event driven with constant-size per-actor counters and no synchronous save. Damage queries retain bounded spatial/target limits; mercenary use shares the existing10-tick AI cadence. Delta snapshots use the existing once-per-second channel plus action acknowledgments. No global tick scan, dependency, thread or payload schema is added.

## Validation and review

Local Java21 Gradle build, unit regressions, JSON/link/diff checks and exact-head full Integration Gate clean build/native GameTests are required. Concrete counts, files, commit/PR and CI IDs are recorded in operational Batch3 evidence. No datagen inputs changed; none required. No local client/GameTest/server launch.

Unit checks cover charge limits, arming, invalid values, nested secondary scope restoration, current slot eligibility and all Smite ranks/thresholds. Native player fixtures exercise real melee/thrown-trident damage,100-hit/manual arming, refunds, accepted/canceled/admin deaths, exact empty-Smite overlay, undead-only area damage, proficiency cap, secondary provenance and native SavedData round trips/class/run/death boundaries. Existing mercenary native tests now cover the100-hit next-strike flow, protected targets/LOS/offline hirer, all four Smite ranks, denied/absorption-only casts, no self-recharge and preserved legacy counters.

One cooperative worker implemented the bounded Smite service/mercenary path, reviewed shared root code and synchronized relevant player guidance. Review corrected mercenary nontrident charging and delayed secondary kill attribution; both have regressions. No unresolved source findings remain after those corrections. Actual native GUI/render/co-op acceptance remains TESTING.

After Batch6 delivery: at two GUI scales inspect/remap G and inventory Primary labels, fill99/100 trident hits and verify G arms rather than immediately firing, then strike with nearby/blocked/immune targets. Kill9/10 undead as Judicator, try Smite without undead and verify exact message/refund, then damage a mixed group and inspect rank progression. Observe mercenary equivalents, reconnect/death/class change and a fresh dungeon; verify allies stay protected. All15 active P2 recommendations remain in the [active ledger](BATCH_1_7_20_REVIEW_NOTES_20261012.md).

## Changed file groups

Shared bars/secondary provenance, death mixin and skill actions; Dragoon shared lightning and player events; mercenary Lightning/Judicator and existing AI wiring; primary inventory labels; native registrations and player/mercenary regression fixtures; unit tests; current class/player/help guides, beta version, release notes, plan and active ledger. Exact paths are in evidence/batch03/changed-files.json.
