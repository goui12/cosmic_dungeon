# Batch 4 - shared mercenary skills

Authorized by Cameron after Batch 3. Branch feature/mercenary-skills-20261005 is based on
aec14b26cc3f5fc9b6a72bd66535b2c8d96a547d (Batch 3). Version 1.5.6-alpha.1. Stop before Batch 5.

## Scope and integration ownership

Single writer owns new mercenary skill services, successful summon/potion hooks, existing
splash/cloud redirects, PartyPayloads/ModNetwork and inventory HUD presentation.
Reuse D1RunData counts; no new SavedData type or entity progression copy.
Existing player ClassSkills behavior remains unchanged.

Skills: Positive Potions and Negative Potions (Theurgist), Wolves (Bogatyr), Fireworks
(Pyroclast), Chain Lightning (Dragoon). Other classes gain no invented special skill.
Start at1; total needed for level L is L*(L-1)/2. Each verified success adds one.
Arithmetic saturates at the existing integer storage limit; level lookup is bounded binary
search. Level50 needs1225 successes. No configurable balance cap or runtime dependency.

One existing counter per run/contract/skill survives native entity death, rest, safe revival,
chunk unload and restart. Missing old counters mean level1. Distinct run IDs reset progress,
even if the same contract is reused. Existing run retirement clearRun removes the counters.
No lifecycle service, registry ID, save version, codec field, spawner or authored chest changes.
Old binaries preserve ordinary count keys but cannot display/earn the new skills.

Only active, admitted, living companions with an eligible hirer earn on the server.
Sealed/completed runs, cleanup handoffs, wrong class/contract/dimension and dormant entities
are rejected. Announcements reach online, non-exited run members in linked run dimensions.
Existing delta HUD synchronization sends at most two bounded skill rows per companion.
Protocol15 requires matching client/server. No C2S skill-write operation exists.

Current integrations award successful native wolf creation, actual splash/cloud healing,
effective beneficial buff application/extension, and immediate negative potion damage.
Per-projectile/cloud persistent flags prevent multiple targets/effects/repeated cloud
applications awarding more than one success per category. Rejected effects do not consume
the first real success. Negative timed debuffs are not credited merely for being attempted
or applied: damage-over-time attribution belongs with Batch5's potion rewrite.
Fireworks/Chain Lightning start at1 until their success sources arrive in Batches7/8.
No skill-based cooldown/quality/cap/damage changes precede their planned batch.

Storage adds at most two integers per admitted Theurgist and one per other supported hire.
No global target cache, extra entity scan, polling, chunk load, per-action disk flush, timer
or thread is introduced. Existing bounded HUD sync and normal saves perform delivery.

## Source and validation

Relevant Google metadata refresh failed because saved read-only OAuth expired; private
Audit/BATCH4_SKILLS_SOURCE_REFRESH_20261005.json records IDs and cached hashes.
Explicit Cameron progression/HUD/reset requirements govern. No fresh canon alignment claim.

Local Java21 build passed: 301 native tests and 2 loading-helper tests,
zero failures/skips. 11 publishing checks, 2001 source JSON files and diff checks passed.
Clean build and all 23 registered GameTests are required in CI before immutable tagging/publication.
The final review fast-path preserves ordinary player potion work without extra effect copies;
this final edit pass is rebuilt before commit.
Datagen is not applicable: code, protocol and documentation only.
No local Minecraft, dedicated-server/GameTest or launcher start is authorized.

## Manual QA pending

1. Start a fresh dungeon. Hover Theurgist/Bogatyr/Pyroclast/Dragoon HUD entries in inventory:
   skills show level1 and0/1; normal and compact recipe-book layouts remain usable.
2. Observe successful potion healing/buffing or a wolf summon: first success reaches2 and
   announces once to the group. Two further successes reach3; total10 reach5. Full-health
   healing, immune/failed effects and a single potion hitting multiple allies cannot multiply XP.
3. Kill and revive the same named companion; skill levels and original gear remain.
   Relog/unload and reload the server normally; progress remains within that run.
4. Beat or forfeit the dungeon, then start a new instance: skills return to1. Another group's
   progress is independent, and old projectiles/resting companions cannot credit the new run.
5. Recheck Batch3 paid/donated revival controls, regeneration and Batch2 ally-wolf protection.
   Firework/lightning progression and class level bonuses await their later batches.

Five batches remain:5 Theurgist potions;6 Bogatyr skill;7 Pyroclast fireworks;
8 Dragoon lightning;9 Theurgist resurrection.
Future improvement: connect damage-over-time potion credit during the planned Theurgist rewrite.

## Exact source files changed

- `docs/ai/CURSEFORGE_AND_MERCENARY_BATCHES_20261004.md`
- `docs/ai/D1_REMAINING.md`
- `docs/ai/tasks/mercenary-skills-20261005.md`
- `docs/releases/1.5.6-alpha.1.md`
- `docs/releases/fragments/mercenary-skills-20261005.md`
- `gradle.properties`
- `src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyHud.java`
- `src/main/java/net/goui/cosmicdungeon/client/screen/MercenaryHudLayout.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryRespawns.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenarySkill.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenarySkillEffects.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenarySkills.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenarySkillsGameTests.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryWolves.java`
- `src/main/java/net/goui/cosmicdungeon/mixin/SkillCloudMixin.java`
- `src/main/java/net/goui/cosmicdungeon/mixin/SkillSplashPotionMixin.java`
- `src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java`
- `src/main/java/net/goui/cosmicdungeon/network/PartyPayloads.java`
- `src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java`
- `src/test/java/net/goui/cosmicdungeon/mercenary/MercenarySkillsTest.java`
