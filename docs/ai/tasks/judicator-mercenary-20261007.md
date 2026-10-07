# Batch18 - Judicator mercenary and Set B Beta

## Behavior and boundaries
Combat starts at0 and advances on cumulative credited hostile kills1,3,6,12,24,
then doubling. Each successful direct attack rolls L% (maximum100%) for L HP burst
damage to hostiles within2blocks. Secondary burst damage never recursively triggers it;
ordinary valid kill credit remains. Allied players/mercenaries/pets are excluded.
Implementation defaults: direct melee and owned arrow impacts qualify; the radius is
centered on the struck enemy. Potion ticks/indirect secondary effects do not roll.
These details were not specified in the earlier confirmation; optional clarification
returned no selection, so these defaults preserve both existing weapon attack modes.

Reuse existing mercenary run counters, server permissions, HUD skill codec and native
kill lifecycle. Progress remains through death/revival and resets for a new run.
No authored class-chest items, registry IDs, network/world schema or existing fields
are rewritten. No migration/datagen/new runtime dependency is needed. Work is event-driven
with spatially bounded burst candidates; no new every-tick world scans or I/O.
Worker owns mercenary implementation/tests; coordinator owns native-suite registration,
release notes/version, state, Git, publication and deployment. No central overlapping writes.

## Validation and release
Required: Java21 local build/tests, source JSON, diff review and full Integration Gate
clean build/native GameTests. Native scenarios cover direct trigger selection, true
radius, friendly protection, nonrecursive secondary damage/kill credit, cancelled and
duplicate deaths, skill HUD/thresholds, death/revival and new-run reset.
Exact changed files and final evidence are recorded below and in BatchRunner/evidence/.
Set B includes the validated Batch17 potion-role split. This task PR merges the completed
set into main before unique1.6.8-beta.1 publication. Full main CI and release CI remain
required; exact source IDs/file IDs/TEST transaction are recorded by
BatchRunner/receipts/batch-18.json and linked evidence. No future outcome is inferred here.
After CurseForge, only stopped TEST receives the runtime via pinned sFTP. No local-client
install, game/server launch/restart or post-publication artifact hash audit.

## Pending licensed testing
1. Hire Judicator: Combat0; reach1,3,6,12,24 hostile kills and inspect levels1–5.
2. Fight clustered enemies with melee and arrows: eligible hits can cause nearby damage;
   allies/pets remain safe and distant enemies are unaffected. Level4 is4% for4HP.
3. Verify secondary kills progress Combat without chained bursts; misses/immune hits
   and allied/neutral deaths do not progress it.
4. Kill/revive and relog the mercenary: progress remains; a fresh dungeon starts0.
5. Recheck Theurgist/Venefex roles and licensed CurseForge update/join on TEST.

Eight cards remain19–26: Skills UI; resources; requests; player Theurgist; wolf commands
and lifecycle; core modes; advanced modes/performance; integration/final Beta. Stop after18.

## Exact changed repository files
- docs/ai/D1_REMAINING.md
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
- docs/ai/tasks/judicator-mercenary-20261007.md
- docs/releases/1.6.8-beta.1.md
- docs/releases/fragments/judicator-mercenary-20261007.md
- gradle.properties
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryJudicator.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryJudicatorGameTests.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEntity.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenarySkill.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenarySkills.java
- src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryJudicatorTest.java
- src/test/java/net/goui/cosmicdungeon/mercenary/MercenarySkillsTest.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
