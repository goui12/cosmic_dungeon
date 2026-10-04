# Testing batch 6 - Instance difficulty and completion tiers

Base: Batch 5 / 3e6561b9 / draft PR #204. Branch: feature/testing-fixes-batch6-20261003.
Two approved action items implemented. Five implementation batches (7-11) remain.

## Behavior

The leader cycles Easy, Hard, Insane and Ridiculous with the Difficulty button at
Tamsin. Hard is the default and means the existing unmodified baseline. All members
see the selected tier in the group menu and HUD. Only a current leader request in a
valid Tamsin session can change it; malformed/stale, nonleader, queued and preparing
requests fail. A change clears readiness, requiring a fresh check. HUD actions cannot
change difficulty. Each adventure freezes the server's settings before its worlds load.

| Tier | Enemy health/damage and wave count | Delay time | Harmful duration | Blindness per damage | Armor wear |
|---|---:|---:|---:|---|---:|
| Easy | 0.5x | 2x | 0.5x | None | 1x |
| Hard | 1x | 1x | 1x | None | 1x |
| Insane | 1.5x | 2/3x | 1.5x | 0.1%, 100 ticks | 1x |
| Ridiculous | 2x | 0.5x | 2x | None | 1.1x |

Every column has independent per-tier settings in the existing common config's
DungeonDifficulty section; health, damage and count are separate keys as well.
Defaults apply to newly selected runs. Changing config never alters an active
run's frozen profile. Server settings are authoritative; client config grants no power.

Health uses one named native attribute modifier for hostile enemies, preserving
their health fraction and preventing reload stacking. Damage scales enemy attacks
(including projectiles with an enemy owner) within the same instance dimensions.
Player attacks, pets, friendly NPC health, outside dimensions and template worlds
retain their behavior. Player harmful effects/armor/blindness require active run
membership and the correct dimension; developer/spectator exemptions remain.
Beneficial effects and infinite effect durations are preserved. Blindness lasts its
configured 100 ticks independently of harmful-duration scaling. Armor wear uses
fractional stochastic rounding before native enchantment and durability handling,
so 10% extra wear does not become one extra point on every small hit.

Cosmic spawners scale transient wave counts, rounding up to at least one for enabled
waves. Disabled counts stay disabled; bosses remain single-spawn and the authored
living-mob cap still limits the whole entity/passenger graph. Countdown speed changes
without rewriting min/max delays, counts, presets, equipment, drop chances or loot.
The fractional countdown phase is transient; a chunk reload can shift timing by
less than one authored delay unit. Normal saved countdown progress is retained.
Both overworld-like and Nether instance dimensions use their own run profile.

Successful Dungeon 1 cleanup records the chosen tier plus all lower tiers, then
verifies that history before acknowledging the inventory handoff. Four generated
advancements form Easy -> Hard -> Insane -> Ridiculous. Failed, forfeited, kicked,
link-dead and startup-aborted handoffs grant none. Offline owners keep the frozen
tier and receive advancement awards on login. Existing NORMAL completion records
qualify for Hard and Easy; other/unknown dungeon tiers do not invent achievements.
Existing completed timestamps remain intact on replay. No currency/XP rewards added.

## Persistence and compatibility

- RunRecord gains optional difficulty_profile in the existing runs SavedData.
  Missing old fields load immutable legacy Hard settings, independent of config;
  all run copies, reservation activation, member removal and round trips retain it.
- InventoryHandoffPlan gains optional difficulty in its existing compound/version.
  Missing legacy fields mean Hard; malformed tier/profile data fails closed.
  All inventory images, ownership data, receipts and existing keys remain intact.
- Completion-record shape and existing progress rows are unchanged. Native UUID
  codecs are self-contained so direct record initialization is also safe.
- The enemy health multiplier uses native attribute persistence. No new registry
  entry, spawner data version, preset field or world recreation is required.
- Protocol 9 becomes 10 because the bounded existing party View adds the tier.
  Both client and server must run this matching build.
- Old saves upgrade automatically through defaults on load and normal save.
  Keep world backups for rollback; use a matching pre-update world backup when
  reverting code after new difficulty runs. Never manually rebuild spawners.
- No server.properties, live config, authored chest, loot, class skill or template
  edits. No new dependency, world scan, thread or per-tick network stream.
  Runtime lookup is bounded by existing instance leases; completion writes occur
  only at the established durable cleanup boundary.

## Validation

Java 21 build and 134 native JUnit tests passed, with zero failures/errors/skips.
All 2,001 source JSON files parse. d1OfflineChecks and its config round trips passed.
Server datagen succeeded and wrote exactly four new advancement JSONs; existing
generated content was unchanged. Client datagen does not apply. Scoped diff and
documentation links were checked. No clean or dedicated/GameTest server launched.

New checks cover defaults and configurable profiles, corrupt/legacy codecs,
reservation/active/copy isolation, count/boss guards, fractional countdown/armor,
harmful-effect copying, cumulative/legacy completion, non-success exclusion,
offline handoff recovery and leader/revision/queue/packet boundaries. Native spawner
reload testing verifies existing fields and authored knobs remain intact.
Prior class, ownership, commerce, death, forfeit, spawner and instance checks remain.

Live simultaneous-instance gameplay and visual acceptance are NOT performed.
The Batch 5 development client was open initially and left untouched; it closed
during this pass. A fresh process check controls the Batch 6 development-client
launch. Startup, installed TEST/client/feed provenance and build/test logs are
recorded separately in the final local handoff.

## Pending gameplay testing

1. At Tamsin choose each tier. Confirm only the leader changes it, changing it
   clears everyone's readiness, queued/preparing groups cannot change it, and
   menu/HUD show the same choice. Start two groups on different tiers.
2. Compare the same enemies and spawners across those instances: health, attack
   damage, first/later wave timing and counts. Check a one-shot boss, living cap,
   blocked placement/recovery, unchanged loot, harmful potion duration and armor.
   Confirm spawn/template/village players remain unaffected. Reconnect/reload and
   verify scaling does not stack. The rare blindness check needs repeated damage.
3. Successfully finish a higher tier and verify that tier plus lower awards.
   Repeat with a member offline and reconnect. Easy must not award Hard; failed
   and forfeited runs award none. Check existing inventory/Chop recovery and
   hidden locked descriptions on the new advancements.

## Exact files

- `docs/NPCs/Tamsin_Vane.md`
- `docs/ai/D1_REMAINING.md`
- `docs/ai/TESTING_BATCH_6_20261003.md`
- `docs/ai/TESTING_ROLLOUT_20261003.md`
- `docs/ai/tasks/testing-fixes-batch6-20261003.md`
- `docs/config-examples/CosmicDungeon.config`
- `docs/releases/fragments/testing-fixes-batch6-20261003.md`
- `src/generated/resources_server/data/cosmicdungeon/advancement/achievements/dungeon_1_easy.json`
- `src/generated/resources_server/data/cosmicdungeon/advancement/achievements/dungeon_1_hard.json`
- `src/generated/resources_server/data/cosmicdungeon/advancement/achievements/dungeon_1_insane.json`
- `src/generated/resources_server/data/cosmicdungeon/advancement/achievements/dungeon_1_ridiculous.json`
- `src/main/java/net/goui/cosmicdungeon/Config.java`
- `src/main/java/net/goui/cosmicdungeon/block/custom/ClassSelectorEntryService.java`
- `src/main/java/net/goui/cosmicdungeon/block/entity/CosmicSpawnerBlockEntity.java`
- `src/main/java/net/goui/cosmicdungeon/block/entity/CosmicSpawnerRuntime.java`
- `src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyHud.java`
- `src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyPanel.java`
- `src/main/java/net/goui/cosmicdungeon/datagen/ModAdvancementProvider.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonCompletionAwards.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonDifficulty.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonDifficultyConfig.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonDifficultyEvents.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceWorlds.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInventoryHandoffs.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleService.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonRunProgressData.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonRunRegistryData.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/InventoryHandoffPlan.java`
- `src/main/java/net/goui/cosmicdungeon/mixin/DungeonDifficultyEffectMixin.java`
- `src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java`
- `src/main/java/net/goui/cosmicdungeon/network/PartyPayloads.java`
- `src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyHudService.java`
- `src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyLobby.java`
- `src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyService.java`
- `src/main/resources/assets/cosmicdungeon/lang/en_us.json`
- `src/main/resources/cosmicdungeon.mixins.json`
- `src/test/java/net/goui/cosmicdungeon/achievement/AdvancementCatalogTest.java`
- `src/test/java/net/goui/cosmicdungeon/block/entity/CosmicSpawnerPolicyTest.java`
- `src/test/java/net/goui/cosmicdungeon/dungeon/DungeonDifficultyTest.java`
- `src/test/java/net/goui/cosmicdungeon/npc/tamsin/PartyDifficultyTest.java`

## Next improvement

Batch 7 implements persistent class weapon skills, damage/class bonuses and potion-use progression.
