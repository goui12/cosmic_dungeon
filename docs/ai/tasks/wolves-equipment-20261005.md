# Batch 2 - wolves and equipment drops

Authorized by Cameron on 2026-10-04 PDT. Branch feature/wolves-equipment-20261005,
stacked on Batch 1 feature/mercenary-identities-20261004. Release 1.5.4-alpha.1.
Cameron subsequently authorized verification of Batch 2 and implementation of Batch 3.
Batches 4-9 remain planned, not authorized by this implementation request.

## Scope and integration ownership

Own BogatyrWolfEvents, BogatyrCompanions, the two egg mixins and the cap checks in
BogatyrRecovery. Own Config, mercenary wolf armor interactions and the focused Mob
equipment-generation mixin. Own the existing DungeonInstanceGameTests registration
for one delegated equipment integration test and its serialization expectation.
No concurrent writer. Recovery remains exact and rate/budget limited; only the cap
rejections change. Player pets still cost eggs/bones and use existing breeding rules.
Mercenary identity/rosters remain run-bound, independent and capped at five.

Natural armor receives the server-configured drop chance at vanilla's actual random
armor-generation call, only if the slot still has the vanilla default chance.
Preset equipment is applied later and retains authored rates. No death-time scanning,
new per-tick work, extra dependencies or changes to weapons/loot tables.
Existing mobs retain saved drop chances; later native pickup/player equip can override
the chance normally. Zero is checked before Looting in vanilla 1.21.10 death drops.

Wolf armor admission verifies the saved mercenary bond, active D1 run, hirer, physical
owner, dimension, active contributing group member, class/item ownership restrictions,
non-dismissal, adult living tamed wolf and empty body slot. It consumes one item,
preserving components and ownership. It does not grant other players sit/shear control.

## Compatibility and boundaries

No registry IDs, packets, save fields, NBT keys, spawner presets or storage versions
change. No world migration required. The old Bogatyr.maxWolves config key remains
readable but ignored; new Loot.naturalArmorDropChance defaults to zero.
No authored class-chest changes. Access/class-item policies, friendly damage/effects,
currency, doors/keys, rifts and progression remain intact.
Player packs are intentionally uncapped by request; larger packs still have native
per-entity cost. Existing shared/bounded threat and recovery budgets remain unchanged.

Google source metadata refresh was attempted on 2026-10-05 UTC and failed because
the saved read-only OAuth authorization expired. Cached Wolf Internal/Bogatyr source
hashes and failure are recorded privately in Audit/BATCH2_WOLVES_SOURCE_REFRESH_20261005.json.
Current explicit Cameron instructions control this batch; no fresh canon audit claimed.

## Validation and remaining QA

Passed locally: Java 21 build, 278 native tests, two helper tests, 2,001 source JSON
files and diff checks. Publisher safeguard suite: 11 checks passed.
Initial CI 37258306527 passed clean build but exposed an incorrect random fixture:
zero continuation rolls stopped vanilla armor generation. The fixture now enables
generation once and supplies non-stopping continuation rolls; all armor, authored
equipment and drop-rate assertions remain. CI clean build/GameTests must pass before publication; local game/server launches are
not authorized. Exact results and deployment hashes belong in the private checkpoint.
No datagen applicable: only code and hand-maintained mixin configuration change.

Manual QA pending:
1. As player Bogatyr, create more than five wolves with eggs; tame/breed additional
   healthy wolves, then relog/recall. No full-pack denial; original pets remain.
2. Hire a Bogatyr: its own five summons neither consume player slots nor enter the
   permanent-pet recovery roster. Friendly hits and harmful potions remain blocked.
3. Have hirer and another active group member equip separate summoned adult wolves:
   one armor consumed, exact appearance/damage preserved, no replacement or duplication.
   Outsiders cannot contribute; existing armor and owner-only removal remain intact.
4. New hard-mode random armor remains visible but does not drop at default zero.
   Verify authored Cosmic Spawner drop settings and picked-up equipment still work.

Seven batches remain: recovery/payments; shared skills; Theurgist potions; Bogatyr
skills; Pyroclast fireworks; Dragoon lightning; Theurgist resurrection.
Future improvement: verify both CurseForge client components after companion approval.

## Exact source files

- `docs/ai/CURSEFORGE_AND_MERCENARY_BATCHES_20261004.md`
- `docs/ai/D1_REMAINING.md`
- `docs/ai/tasks/wolves-equipment-20261005.md`
- `docs/releases/1.5.4-alpha.1.md`
- `docs/releases/fragments/wolves-equipment-20261005.md`
- `gradle.properties`
- `src/main/java/net/goui/cosmicdungeon/Config.java`
- `src/main/java/net/goui/cosmicdungeon/combat/NaturalArmorDrops.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java`
- `src/main/java/net/goui/cosmicdungeon/gametest/WolfEquipmentGameTests.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryWolfArmor.java`
- `src/main/java/net/goui/cosmicdungeon/mixin/BogatyrEggOffspringMixin.java`
- `src/main/java/net/goui/cosmicdungeon/mixin/BogatyrEggSpawnMixin.java`
- `src/main/java/net/goui/cosmicdungeon/mixin/NaturalArmorDropsMixin.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrCompanions.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrRecovery.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrWolfEvents.java`
- `src/main/resources/cosmicdungeon.mixins.json`
- `src/test/java/net/goui/cosmicdungeon/combat/NaturalArmorDropsTest.java`
- `src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java`
- `src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryTestWolf.java`
- `src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryWolfArmorTest.java`
- `src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryWolvesTest.java`
