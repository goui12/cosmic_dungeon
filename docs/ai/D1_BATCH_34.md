# Batch34: authored mob rewards and stable NPC bindings

Completed in code and offline-validated on 2026-09-19. Cameron authorized Batch34 only.
Verified parent: `a0174968d4af5b6a7dacb0cf0dd53c34c5447f1e`, branch
`feature/d1-canon-config-20260916`. Stop after the final local commit; Batch35 awaits Cameron.

## Changes

- B34-1: Cosmic Spawners load native SpawnData and weighted SpawnPotentials, previously
  omitted. Provenance decorates a copy of each selected spawn entry, retaining authored
  persistent reward data, other tags and equipment. The per-tick rebuild is removed.
  Existing full-bright rules, numeric knobs, preset formats and one-shot/cap fields remain.
  Malformed tag lists retain their evidence and pause spawning with a warning.
- B34-2: strict reward resolution validates one coordinate origin, one explicit override,
  native integer Trace values and duplicate registrations. Registered neutral/custom mobs
  can qualify without implementing Enemy; assigned vendors, Tamsin and Watson cannot.
  No health/name inference. Unknown or malformed rules yield zero and a deduplicated warning.
- B34-3: developer vendor assign/clear/info uses the nearest actual ray hit, clipped at
  solid blocks. Different profile assignments require an explicit clear first; Tamsin and
  Watson cannot be overwritten, and Beluzon requires a native Creaking. New assignments
  retain exact original name/visibility/invulnerability/AI state in optional entity NBT.
  Clear restores known original state; legacy assignments retain their unknown original
  flags. Persistence remains enabled. Unknown snapshots block clearing without mutation.
- B34-4: vendor interaction, purchase/sale, direct repair, Inn and Tamsin routes reject
  conflicting roles. Malformed assigned profiles cannot fall back to vanilla commerce.
  Offer tiers use the profile's D1/D2 system, never the greater of both player totals.
  Info reports UUID, dimension, native type, profile, prior-state evidence and personal access.

## Configuration and identity mapping

Two additive defaults in server `CosmicDungeon.config`:

| Section/key | Default | Meaning |
| --- | --- | --- |
| Economy.registeredSpawnerRewards | [] | Reviewed D1 coordinate reward registrations |
| NpcFaction.vendorBindingRange | 6.0 | Developer targeting distance in blocks |

A reviewed example is `cosmic_spawner_-12_64_300=miniboss`. It is syntax guidance,
not an approved world coordinate. Keys match existing spawner provenance across D1
instance copies. Resolution order: exactly one persistent entity override, then the
spawner table, then registered entity types. Existing category amounts stay configurable.
Two different provenance origins, duplicate matching entries, malformed overrides and
unknown categories fail closed. Explicit zero is valid. Floating NBT is not truncated.
No active server config or vendor price was changed; do not overwrite operator files
with exported examples. Actual encounter coordinates remain an authored-world review.

| NPC | Existing stable identity | Access/role retained |
| --- | --- | --- |
| Naton Whitlock | cosmicdungeon:d1/general_supply_vendor | Personal D1 tier1 plus Village access |
| Elias Centvin | cosmicdungeon:d1/weapon_supplier | Personal D1 tier2; retail and direct repair |
| Eon Penrose | cosmicdungeon:d1/brewing_store | Personal D1 tier3; approved brewing retail |
| Beatrix Farrow | cosmicdungeon:d1/food_vendor | Village access; Q&A D03's existing unlock override |
| Beluzon | cosmicdungeon:d1/save_teleport_npc | Native Creaking; Village access, one-time Inn bond |
| Tamsin Vane | Existing UUID-to-selector binding | Starting-area onboarding; no vendor role |
| John Hamish Watson | Existing run tag and saved UUID | Instance completion; no vendor role |
| Gritch | cosmicdungeon:d1/d1_nether_gritch_of_the_barter_pit | Legacy profile retained; role source unresolved |

Global placed-entity persistence remains separate from each player's service access.
No NPC is created by a personal unlock check or by failure to find an unloaded entity.
Old wrong-profile placements are never renamed/replaced by inference from display names.
Code TODOs retain the authored placement/coordinate review and Gritch's unresolved role.

## Source authority

The debloated20-tab MASTER and66-answer Q&A retain their sealed hashes:
MASTER `976fa26058f7ed624c8aa5aecc57e02caee008ff005591f05da40912e750dcff`;
Q&A `c59b81018151350e4df4ae686efd59a06cfdb6887167a1be2d61fc31f815ef8d`.
Eleven relevant Google Docs were refreshed read-only; all were unchanged.
Eight complete bodies and three relevant-section reviews are distinguished in private
SOURCE_REVIEW.json, with source artifact hashes and modification times.

Economy Internal (2026-08-18) requires one category/value per rewarded mob.
Vendor Info (2026-08-23) separates global presence from personal access.
Beluzon (2026-08-29) supplies the native Creaking/Inn-only identity.
Tamsin (2026-08-19) explicitly excludes vendor services.
Q&A D03 remains authoritative for the existing Beatrix unlock exception; D23/D56 keep
Watson per instance. D62/D67 allow the reviewed newest-doc choices. No balance, price,
chest quantity, equipment or encounter identity was guessed from obsolete sheet tabs.

## Validation and limits

`gradlew.bat d1OfflineChecks build --offline --console=plain`, Java21, passed:
**10,794 offline checks**, including173 new authored reward/binding checks and103 config
checks, plus two separate .config round trips. All1,963 source JSON files parse.
The final validation receipt records file/source hashes, config semantic comparison,
Markdown links, clean diff checks and preservation of unrelated tracked files.

New checks cover reward categories/priorities, zero/maximum/integer/malformed values,
duplicate/ambiguous origins, immutable native NBT and compressed disk round trip,
all original flag combinations, occupied/unknown role evidence, tier isolation and
ray geometry. First build compiled but a full SpawnData.CODEC fixture could not run
without Minecraft registry bootstrap. It was replaced with native compressed-NBT
evidence checks; full native codec/world behavior remains explicitly untested.
Final build and offline checks include all dependent service-route guards.

Datagen is not applicable: no new item, registry, model, texture, tag or recipe.
No gameplay, GameTest, client, server, deployment, push or active-world edit occurred.
No new required PNG. Optional existing `tamsin_d1_map.png`:512x256, winding route ending
at Base Camp, signed "-JHW"; its drawn fallback still works.

## Save compatibility and cumulative TEST acceptance

Registry IDs, network payloads, world/spawner save IDs and preset versions are unchanged.
The optional `cosmicdungeon.vendor_binding_before` schema1 compound is added only by a
future explicit developer assignment; old profile strings remain valid. Original name
components use the native registry-aware component codec. Actual commands were not run.
Do not downgrade after new authoring without a complete matching world backup.

Before release, use a complete world copy to:

1. Load/reload type-only, preset and weighted-potential spawners; compare tags, equipment,
   drops, knobs and boss one-shot state. Verify malformed tag evidence pauses safely.
2. Review each D1 encounter's actual coordinate and payout, including neutral/custom mobs;
   verify per-entity and config overrides, duplicate deaths, party splitting and restart.
3. Inspect existing NPC UUID/type/profile mappings; test adjacent mobs, solid-wall targeting,
   existing-role refusal and reviewed assign/clear across unload/restart. Never bulk-replace.
4. Compare personal access for two players sharing one NPC; test D1/D2 tier isolation,
   hostile buyback, stale menus/quotes and legacy conflicting roles.
5. Verify native Beluzon/First Heart behavior through the full night cycle and confirm
   existing Inn bonds and Tamsin/Watson bindings remain intact.

M05/M17/M41 remain partial for concrete authored-world/legacy acceptance; M103 remains
preserved-content verification pending. Counts remain44 implemented/runtime-unverified,
21partial D1,9preserved,27D2+ deferred =101. Their narrower code fixes above are complete.
[Four planned batches remain](D1_REMAINING.md):35 pricing,36 D1 class effects,
37 achievements/progression/travel,38 help/performance and cumulative-test handoff.

## Exact file inventory

Reviewed Batch34 commit allowlist (24 files):

- `docs/ai/D1_BATCH_34.md`
- `docs/ai/D1_IMPLEMENTATION_20260916.md`
- `docs/ai/D1_REMAINING.md`
- `docs/ai/tasks/d1-canon-config-20260916.md`
- `docs/config-examples/CosmicDungeon.config`
- `docs/releases/fragments/d1-batch-34-authored-rewards-npc-bindings.md`
- `src/main/java/net/goui/cosmicdungeon/Config.java`
- `src/main/java/net/goui/cosmicdungeon/block/entity/CosmicSpawnDataRules.java`
- `src/main/java/net/goui/cosmicdungeon/block/entity/CosmicSpawnerBlockEntity.java`
- `src/main/java/net/goui/cosmicdungeon/command/VendorCommand.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonGroupSplitService.java`
- `src/main/java/net/goui/cosmicdungeon/economy/D1EconomyConfig.java`
- `src/main/java/net/goui/cosmicdungeon/economy/D1MobRewardRules.java`
- `src/main/java/net/goui/cosmicdungeon/npc/inn/InnService.java`
- `src/main/java/net/goui/cosmicdungeon/npc/tamsin/TamsinService.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/dragoon/repair/DirectRepairService.java`
- `src/main/java/net/goui/cosmicdungeon/vendor/VendorAccessService.java`
- `src/main/java/net/goui/cosmicdungeon/vendor/VendorAssignmentService.java`
- `src/main/java/net/goui/cosmicdungeon/vendor/VendorBindingRules.java`
- `src/main/java/net/goui/cosmicdungeon/vendor/VendorInteractionEvents.java`
- `src/main/java/net/goui/cosmicdungeon/vendor/VendorMenuState.java`
- `src/main/java/net/goui/cosmicdungeon/vendor/VendorService.java`
- `src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1AuthoredBindingChecks.java`
- `src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java`
