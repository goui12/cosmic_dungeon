# Batch17 — mercenary potion roles and efficient delivery

## Behavior and compatibility
Separate existing potion AI into positive-only Theurgist and negative-only Venefex.
Keep physical legacy stock/counters and unknown saved fields; prevent opposite/mixed
stock collection or casting. Only Venefex uses0/1/3/6/10 cumulative triangular progression.
Credit actual native effective applications once per thrown potion across recipients/ticks;
poison/wither require actual damage. Miss/immune/no-op effects earn nothing.
Keep other mercenary level curves, material-free production, scaled quality/cadence,
friendly protection and Theurgist Positive Potions10/3-minute resurrection unchanged.

No registry IDs, world/spawner/preset/network schema or authored chest content changes.
No migration/datagen needed. Existing run-counter storage remains authoritative.
One worker owns mercenary source/tests; coordinator owns process/scripts/docs/CI/deployment.
No new runtime dependency or unbounded polling/scanning.

## Process
AGENTS5359→1680 words; installed skill705→522 words, validated/saved separately.
Controller contract fingerprint explicitly rebound to this user-authorized edit;
ownership/scope/evidence validation code unchanged. WORK_PROTOCOL condensed.
deploy-mod now requires an exact published receipt/runtime and updates only stopped TEST.
Pinned SSH, DPAPI, identity, latest-session shutdown/recheck, staging byte count and
write-ahead rename journal remain. Uncertain acknowledgement blocks recovery/retry.
No client access or artifact hashes after CurseForge; no routine feed publication.
build-local legacy feed is explicit opt-in.

## Validation and delivery evidence
Required: Java21 build, source JSON, Python safeguards, diff check and full clean/native
CI with existing49 registered GameTests (potion-stock includes the new native scenarios).
PowerShell fault-injection guards cover shutdown/restart ambiguity, release identity,
successful swap and lost acknowledgements before/after activation.
Native coverage checks real splash effects/credit, two-target one-credit, no-op/immunity,
undead inversion, poison actual ticks, run reset and retained revival progress.
Unit coverage checks role collection/stock, triangular thresholds/HUD and legacy/unknown
counter round trips. Exact final results/source/tag/CI/CurseForge IDs/sFTP transaction are
recorded in CosmicDungeon_AI/BatchRunner/receipts/batch-17.json and linked evidence.
This source document does not assert a future release/deployment already happened.
SetB remains on the task branch until18. No client/server/GameTest launch or restart.

## Pending licensed testing
1. Hire Theurgist and Venefex: only their assigned potion skill appears; new Venefex starts0.
2. Injure allies and fight hostiles: Theurgist benefits allies only, Venefex harms enemies only,
   no progress for misses/immune/full-health/no-op casts; multi-target hit counts once.
3. Reach Venefex1/3/6/10 successes, revive/relog: levels1/2/3/4 persist; new run resets0.
4. Existing Theurgist at Positive Potions10 still offers native death-menu resurrection
   with3-minute cooldown; old identities/counters survive load and no stock is rewritten.
5. Install/update through CurseForge and join TEST manually with the matching version.

Possible follow-up: reusable task-card summaries can reduce future historical reads further.

## Exact changed repository files
- AGENTS.md
- docs/CURSEFORGE_RELEASES.md
- docs/LOCAL_AI_WORKFLOW.md
- docs/ai/D1_REMAINING.md
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
- docs/ai/tasks/mercenary-potion-roles-20261007.md
- docs/releases/1.6.7-alpha.1.md
- docs/releases/fragments/mercenary-potion-roles-20261007.md
- gradle.properties
- scripts/build-local.ps1
- scripts/deploy-mod.ps1
- scripts/deploy-mod.safe.ps1
- scripts/deploy-server-core.ps1
- scripts/tests/test_server_deployment.ps1
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryBrewing.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryInventory.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryPotionCasting.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryPotionGameTests.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryPotionRoleGameTests.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryPotions.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenarySkill.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenarySkillEffects.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenarySkills.java
- src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryPotionsTest.java
- src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryRolesTest.java
- src/test/java/net/goui/cosmicdungeon/mercenary/MercenarySkillsTest.java
