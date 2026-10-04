# Batch 7 - Class weapon skills

Authorized: Cameron, 2026-10-04. Base: Batch 6 / 3d1ecce5 / PR205.
Branch: feature/testing-fixes-batch7-20261004. Three action items only.

Implement independent persistent class/weapon XP, approved combat/class bonuses,
and meaningful Theurgist potion support progression from CLASS_SKILL_BALANCE_20261003.md.
Use the eight offline Items and Armor documents; preserve deferred mechanics and gear.

Single writer owns PlayerProgressionData serialization, Config integration, combat hooks,
class-projectile attribution and mixin registration. No other active task owns these.
Expected directories: progression/, playerclass/skill/, playerclass/d1/, mixin/, command/,
config example, tests and relevant docs. No registry IDs, chest stacks, spawner format,
world templates, teleports, currency transactions or class availability changes.

Existing progression SavedData gains an optional bounded per-player class-skill map;
missing legacy data means zero XP. No retrospective credit. Normal server saves preserve
progress through death/forfeit/class switch. Validate old/current codecs and round trips.
Server-only awards and attack snapshots; no client XP mutations. Avoid packet/tick scans.

Validate actual attack identity, pet/environmental exclusion, lower/upper XP bounds,
class isolation, effects/healing effectiveness and per-action/cooldown caps; native
JUnit, offline checks, all source JSON, diff check, Java21 build. Datagen only if generated
resources change. Manual: combat/projectile swapping, class switch/death/reconnect,
potions with overheal/equal buffs/multiple recipients and bonus limits.

Complete scoped commit/push/chained draft PR and authorized safe TEST/client pipeline.
Do not force-close the Batch6 client or launch duplicates. Stop before Batch8.

Completed: 27 scoped files; 148 native JUnit tests, offline checks and Java21 build passed.
All 2,001 source JSON parse. No datagen applies. Report: ../TESTING_BATCH_7_20261004.md.
Native cloud hook corrected and tags initialized in the sword test fixture. Final review
excludes punching with held bows/potions from those skills. Existing tests preserved.
The Batch6 dev client has closed without intervention; process guards control final launch.
Final source/deployment/feed hashes and startup evidence are recorded in the private handoff.
