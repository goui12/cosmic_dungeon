# Batch 5 - Theurgist potions

Authorized by Cameron after Batch 4. Branch feature/theurgist-potions-20261005,
based on a5a747b9e700ef888180c9be41cdc5de53a1303d. Version 1.5.7-alpha.1.
Stop before Batch 6. Four remain: Bogatyr, Pyroclast, Dragoon, Theurgist resurrection.

## Behavior and balance

Free stock replaces the AI's material recipe path. All new stock is native splash effect
potions. Healing, Strength and Swiftness positive stock; Harming and Poison negative stock.
At most six per category from replenishment, inside the existing 54-slot inventory.
Collection keeps useful equipment/ammunition/effect splashes but no brewing inputs,
bones, spawn eggs, drinkables or lingering potions. Existing legacy contents are preserved.
Authored class chests and brewing stand stacks/components are never rewritten.

Base portable stock interval uses existing brewTicks (default 400 ticks / 20 seconds).
A nearby permitted, visible stand can add an immediate batch on a separate cooldown at
half that base. One positive and one negative bottle per batch; no offline catch-up.
The respective skill divides base intervals by 1 + 0.06*(level-1), minimum 40 ticks.
Throwing uses the existing per-potion configured base with the same scaling/floor,
separate positive/negative category cooldowns, a 20-tick global gap and 10-tick preparation.
Two 8-particle native flashes signal preparation/release; no new assets or packet type.

Tier II positive probability: level 1 = 5%, level 10 = 45%, level 25+ = 95%, linearly
interpolated between these anchors. Negative stock remains ordinary Harming/Poison.
Useful healing is preferred, then enemy attacks, then player/other ally buffs.
Out of combat, support targets dungeoneers. Full-health healing, stronger/unexpired buffs
and native immunities are skipped. Revalidate target/range/sight/inventory/contract at release.
Positive-at-feet implements the explicit splash-only requirement; optional clarification
was unanswered. This is an interpretation, not approval to add drinkables.

## Integration and persistence

Single writer owns mercenary potion services, the entity's transient prepared-cast state,
existing skill-effect integration, one native effect mixin and the existing GameTest suite.
No CosmicDungeonMod, registry ID, networking/protocol or saved-schema change.
Stock/casting cooldowns reuse the existing bounded MercenaryTimers map. Old keys/fields
remain readable; no migration or registry/spawner/template/chest recreation required.
Existing skill levels retain Batch 4 death/reload/reset behavior.

Poison attribution follows native effect copy, upgrade and hidden-effect restoration.
A transient dose holds owner UUID/run and the original projectile's shared credit tag,
so multiple victims/ticks and mixed instant/timed damage still earn once per potion.
Only actual health/absorption loss and the existing active-run server guards can award.
A stronger/longer player-applied replacement clears old active attribution.
No global target cache, extra world scan, thread, disk flush or polling loop.
Pending timed-effect attribution is deliberately transient across unload/restart:
native effects persist, previously earned levels persist, but an uncredited old dose
cannot claim new XP after reload. This avoids persistent per-projectile ledgers.
Per-recipient ally protection remains; native heal/harm inversion is honored.

Access/class ownership, container locks, unopened loot tables, useful equipment, copied
chest receipts, run eligibility and friend/wolf filtering remain in their existing paths.
No changes to doors/keys, currency, trades, rifts, travel, achievements or authored spawners.
Shared server-authoritative jar; no client-only initialization or client XP write action.
No new runtime dependencies or heap changes. Existing candidate search stays bounded
at 48 nearby entities plus owner/self; casting and replenishment have hard cadence floors.

## Validation and remaining QA

Local Java21 build: 309 native tests and 2 helper tests, zero failures/errors/skips.
Legacy material-consumption/acquisition expectations were updated only where explicitly
superseded; original authored-stack, permission and capacity assertions remain.
Eight added native tests cover chance/cadence, stock bounds, splash catalog, materials,
buff refresh, one shared credit and timer persistence. Two new CI GameTests exercise
native free-stock production and effect-copy/replacement/hidden-restoration attribution.
Clean build, all 25 registered GameTests and release artifact validation are CI gates.
No local game, GameTest server, launcher or TEST server launch is authorized.
Datagen is not applicable: implementation, mixin registration and documentation only.
Google metadata refresh failed with expired saved OAuth; private
Audit/BATCH5_POTIONS_SOURCE_REFRESH_20261005.json records cached IDs/hashes and failure.
Explicit Cameron requirements govern; no fresh canon alignment claim.

1. Hire Theurgist with no ingredients: observe splash stock, stand brewing without
   consuming either inventory's materials, and aura preparation/release.
2. Out of combat, verify healing only when hurt and Strength/Speed at feet; in combat,
   check enemy attacks plus ally/mercenary/wolf protection and undead inversion.
3. Compare level 1, 10 and 25 positive quality/frequency; verify one potion hitting
   multiple targets or repeated poison ticks grants at most one success.
4. Check immune/full-health/stronger-buff cases, blocked sight and moving targets;
   death/revival and relog retain earned skills, while a new dungeon resets them.
5. Recheck prior paid/donated revival, equipment copies, native brewing/player potions,
   recipe-book HUD and other classes. Licensed multiplayer/visual acceptance remains pending.

Future improvement: add a bounded pending-dose save representation only if gameplay
testing establishes a need for unearned poison credit to survive entity unloads.

## Exact files changed

- `docs/ai/CURSEFORGE_AND_MERCENARY_BATCHES_20261004.md`
- `docs/ai/D1_REMAINING.md`
- `docs/ai/tasks/theurgist-potions-20261005.md`
- `docs/releases/1.5.7-alpha.1.md`
- `docs/releases/fragments/theurgist-potions-20261005.md`
- `gradle.properties`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryBrain.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryBrewing.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryConfig.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEntity.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryInventory.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryPotions.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenarySkillEffects.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryPotionBalance.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryPotionCasting.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryPotionCredit.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryPotionGameTests.java`
- `src/main/java/net/goui/cosmicdungeon/mixin/MercenaryPotionEffectMixin.java`
- `src/main/resources/cosmicdungeon.mixins.json`
- `src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java`
- `src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryCopyLootTest.java`
- `src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryEntryTest.java`
- `src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryRolesTest.java`
- `src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryPotionsTest.java`
