# Testing batch 7 - Persistent class skills

Base: Batch 6 / 3d1ecce5 / draft PR205. Branch: feature/testing-fixes-batch7-20261004.
Three approved action items implemented; four implementation batches (8-11) remain.

## Behavior and balance

Class skills live in the existing world-level player progression store, keyed by player
UUID and class/skill. They survive death, forfeit, logout and switching away/back without
transferring levels between classes. New/legacy players begin at zero; old aggregate kills
cannot invent weapon XP. See the [approved balance table](CLASS_SKILL_BALANCE_20261003.md).

An eligible active adventure member earns 10 XP for a direct qualifying hostile weapon
kill. No training from players, friendly NPCs, companions, administrative/spectator hits,
environmental finishing damage, sealed outcomes or outside/template worlds. Existing
kill-credit payouts/statistics are unchanged. Dragoon chain damage already inherits the
initial final damage and receives neither a second skill multiplier nor direct-weapon XP.

Default maximum level is 50. Level L costs 250 + 50*L to advance; total XP at levels
10/25/50 is 4,750/21,250/73,750. All new values have separate server config keys in
ClassSkills. Lowering a cap stops extra earning without deleting existing XP. Bonuses
scale linearly and multiply after the existing class damage calculation, once per hit.

- Swords/maces: +25% at cap; bows: +20%, except Venefex +15%.
- Dragoon trident: +20% damage and +5 percentage points chain chance, bounded at 100%.
- Pyroclast crossbow/rocket: +15% damage; no range, radius or target-count increase.
- Judicator/Theurgist support arrows: +25% healing; Venefex class-arrow debuffs: +25% duration.
- Theurgist potions: +25% instant effect amount and timed effect duration. Native intensity,
  distance, armor, immunity and effect-veto behavior remains; no whole amplifier jump.
- Deadeye mappings/draw reduction are prepared behind existing class availability.
  Metalmancer resonance mapping remains dormant: direct ore/vortex attacks are existing
  TODOs, and this batch does not invent those mechanics or award XP for golem kills.

Bows/crossbows capture actual weapon, class, owner and run before launch. Tridents/potions
capture at first server admission. Reload retains identity and potion action budget;
missing old snapshots receive no inferred credit. A changed owner, class, dimension or
run invalidates the snapshot. Switching held weapons cannot relabel a projectile kill.

Effective potion healing or a new/improved beneficial buff earns 2 XP per eligible
recipient/effect. Equal-amplifier refreshes, empty throws and overhealing earn none;
regeneration at full health also earns none. Helpful D1 support arrows train bow skill.
Default interval is 60 server ticks per recipient/effect. One potion's complete lifetime,
including its lingering cloud, shares a 10-XP budget across recipients/effects/kills.
Native potion creation, components, consumption and brewing remain unchanged.

Levels are inspectable using the existing /progression get <player> command, preserving
self/developer read restrictions. A level-up emits one server message. No new screen,
client XP request, packet, network protocol change, polling loop, runtime dependency,
world scan or per-hit disk write. Existing protocol 10 remains; deploy matching JARs.

## Save compatibility and boundaries

The existing cosmicdungeon_player_progression_v1 SavedData gains optional class_skill_xp.
Absent legacy data defaults empty. Keys are limited to the 16 approved class/skill pairs;
XP is nonnegative and bounded. Invalid skill data fails closed through existing SavedDataProof.
Existing progression fields, flags and Watson receipts retain their codecs and values.
New entity persistent fields carry firing identity/action spending, a bounded recipient
cooldown map (64 effects maximum), a kill receipt and two support-arrow healing factors.
All defaults preserve old entity behavior; no backfilled XP or altered registry IDs.
Normal server autosaves/stops persist XP; an abnormal crash can lose recent unsaved XP.
Use a matching world backup when reverting to code predating skill storage.

No spawner/preset schema, authored chest/equipment, class selection/unlock, loot, template,
server.properties, teleport, door/key, vendor/trade/currency or inventory handoff changes.
Class item ownership/use guards remain authoritative. Code review covered those boundaries;
this is not a claim of live multiplayer or visual acceptance.

## Validation

148 native JUnit tests passed, zero failures/errors/skips; 14 new tests exercise real
progression codecs/legacy data, independent classes/players/weapons, preserved existing
fields, malformed data, exact curve/cap edges, native weapon identities, projectile snapshot
reload/owner/class/run rejection, environmental attribution, non-melee weapon punches and potion budget/cooldown rules.
The native harness loads the new mixins. An initial cloud-hook target was corrected to
1.21.10 serverTick; the sword fixture supplies the native tag absent from JUnit bootstrap.
Existing tests were retained. d1OfflineChecks, including 129 config checks and two round
trips, passed. All 2,001 source JSON files parse. Scoped diff and doc links checked.
Final Java21 build and installed TEST/client/feed provenance are recorded in the private
handoff receipt. No clean, GameTest/dedicated-server launch or world entry performed.
Datagen is not applicable: no generated models/tags/recipes/loot/advancements changed.

The eight offline Items and Armor documents were reviewed for weapon mapping, with IDs
from the balance table and hashes/metadata in the ignored mirror Audit folder. No live
Google freshness claim. Utility tools, later rods and unavailable classes remain deferred.
Completed diagnostic logs were losslessly compressed with hashes; rollback JARs and
source documents retained. Source commit excludes existing unrelated JAR deletion,
generated caches and logs. No AGENTS edits were necessary.

## Pending gameplay testing

1. Start an adventure as a Dungeoneer. Compare /progression get <your player> before/after
   a melee, bow, trident and crossbow kill. Each actual weapon earns 10 XP in its own class.
   Switch held items while a projectile flies; environmental and wolf/golem kills give no
   weapon XP. Check existing currency payout and item ownership restrictions.
2. Earn progress, die/forfeit, reconnect, switch class at Tamsin and switch back. Each class
   retains its own levels. Compare baseline/trained damage, support-arrow healing, Venefex
   durations and Dragoon chain chance using controlled TEST config; restore test tuning.
3. As Theurgist drink/splash/linger healing and buffs. Injured allies/new useful buffs earn
   XP, empty throws/full-health healing/equal buffs do not. Several recipients and cloud
   reapplications never exceed 10 XP total. Verify buff upgrades, cooldown, cloud reload,
   native effect vetoes and skill/difficulty modifiers applying once each.

## Exact files

- `docs/ai/CLASS_SKILL_BALANCE_20261003.md`
- `docs/ai/D1_REMAINING.md`
- `docs/ai/TESTING_BATCH_7_20261004.md`
- `docs/ai/TESTING_ROLLOUT_20261003.md`
- `docs/ai/tasks/testing-fixes-batch7-20261004.md`
- `docs/config-examples/CosmicDungeon.config`
- `docs/releases/fragments/testing-fixes-batch7-20261004.md`
- `src/main/java/net/goui/cosmicdungeon/Config.java`
- `src/main/java/net/goui/cosmicdungeon/command/ProgressionCommand.java`
- `src/main/java/net/goui/cosmicdungeon/effect/D1TunedMobEffect.java`
- `src/main/java/net/goui/cosmicdungeon/mixin/D1CrossbowMixin.java`
- `src/main/java/net/goui/cosmicdungeon/mixin/SkillCloudMixin.java`
- `src/main/java/net/goui/cosmicdungeon/mixin/SkillDrinkPotionMixin.java`
- `src/main/java/net/goui/cosmicdungeon/mixin/SkillLingeringPotionMixin.java`
- `src/main/java/net/goui/cosmicdungeon/mixin/SkillProjectileWeaponMixin.java`
- `src/main/java/net/goui/cosmicdungeon/mixin/SkillSplashPotionMixin.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1ArrowAbilities.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/dragoon/DragoonPassiveEvents.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/skill/ClassSkillConfig.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/skill/ClassSkillEvents.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/skill/ClassSkillRules.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/skill/ClassSkills.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/skill/SkillAttackSnapshot.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/skill/SkillPotions.java`
- `src/main/java/net/goui/cosmicdungeon/progression/PlayerProgressionData.java`
- `src/main/resources/cosmicdungeon.mixins.json`
- `src/test/java/net/goui/cosmicdungeon/progression/ClassSkillProgressionTest.java`

## Next improvement

Batch 8 adds mercenary hiring/payment/party slots and their authored starter-room equipment.
