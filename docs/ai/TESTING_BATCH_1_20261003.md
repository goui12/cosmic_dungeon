# Testing fixes, batch 1 — implementation report

Branch: feature/testing-fixes-batch1-20261003, based on 256bd5dd / PR199.
This pass contains three gameplay changes. The remaining ten batches and accepted decisions
are in [the rollout](TESTING_ROLLOUT_20261003.md); the [skill table](CLASS_SKILL_BALANCE_20261003.md)
is a delegated draft, not implemented skill runtime.

## Result

- Attuned equipment can be tossed and dropped on death. Its first eligible collector owns
  it permanently; world-item, projectile, inventory/menu and equipment guards preserve ownership.
  Partial/failed pickup does not claim the uncollected remainder. Death items and Trace start
  in the death block with zero scatter velocity. Native gravity/fluid movement still applies.
- Group splits require an attributable run member's kill and use the existing immediate,
  receipt-protected payout. Player-owned companions and later environmental deaths share
  attribution; untouched environmental deaths do not pay. Existing D1 success-only lifetime
  accounting remains until the leaderboard batch. Weapon XP is not implemented in this pass.
- Bogatyr wolf eggs tame the actual spawned wolf, including egg-on-wolf offspring, while
  preserving the pack cap. Friendly targeting and damage are separately blocked for players,
  villagers, recognized friendly NPCs and owned pets/companions.

## Validation and limits

Java 21.0.12.101 Gradle build passed. All 89 JUnit tests passed (12 new, no skipped/failed
cases), including native item-component serialization and malformed-owner safeguards.
The complete existing d1OfflineChecks task passed. All 1,997 JSON files under src parsed;
scoped git diff --check passed. The initial compile caught incorrect golem package imports;
those were corrected before the successful checks.

No datagen applies: this changes Java and the established hand-authored mixin configuration.
No destructive clean was run because the historical tracked JAR has a preexisting staged
deletion. No GameTest/dedicated server launch was authorized or performed.

The build is not multiplayer acceptance. The committed rebuild, stopped TEST/client swap,
current-test publication, portable updater and development-client launch have separate
receipts under CosmicDungeon_AI; final handoff states their actual outcome.

## Persistence, boundaries and performance

Optional cosmicdungeon_attuned_owner_v1 item CUSTOM_DATA stores a UUID. Old eligible carried
gear is claimed on login/acquisition; old authored chests are not scanned or rewritten.
Optional cosmicdungeon_last_damaging_player_v1 entity metadata records the last actual player
damage. Native codecs preserve other fields. No SavedData version, registry ID, network
packet, spawner/preset, door/key, rift, world snapshot or server.properties format changes.
No destructive migration is required. Rollback preserves unknown custom fields, but an older
mod does not enforce the new ownership contract; use the coordinated deployment backups.

Checks remain server-authoritative and common code has no client-only dependencies.
Existing trade/private-storage/automation restrictions and death-currency journal remain.
Per-event checks and a single bounded login inventory pass replace no queues or global scans;
no dependencies, heap increases, tick-time disk/network work or permanent watchers were added.
Existing class availability, authored loadouts, loot and active configs are untouched.

## Licensed TEST acceptance still required

1. Claim an attuned item, toss it, then die: only its original owner can collect it, including
   another player of the same class. Relog/restart and check the owner persists.
2. Try ordinary/shift/partial/full inventory pickup and thrown-trident recovery; verify exact
   names, enchantments and quantities. Death gear and Trace initially occupy the death block.
3. Compare direct and wolf kills, a player-hit mob falling off a cliff, and an untouched
   environmental death. Eligible kills pay once immediately; the untouched death pays nothing.
4. Use a wolf egg on a block, water and a wolf; test the cap and egg consumption. Have another
   Dungeoneer/pet/NPC provoke it; verify no friendly targeting or damage and normal hostile combat.
5. Recheck wolf logout/archive/rejoin and existing dungeon death/forfeit inventory restoration.

Possible future improvement: expose concise kill-credit diagnostics to developers for faster
multiplayer reward investigations without adding player-facing chat noise.

## Exact changed files

- docs/ai/CLASS_SKILL_BALANCE_20261003.md
- docs/ai/D1_REMAINING.md
- docs/ai/TESTING_BATCH_1_20261003.md
- docs/ai/TESTING_ROLLOUT_20261003.md
- docs/ai/tasks/testing-fixes-batch1-20261003.md
- docs/releases/fragments/testing-fixes-batch1-20261003.md
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonGroupSplitEvents.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonGroupSplitService.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonKillCredit.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/D1KillStatistics.java
- src/main/java/net/goui/cosmicdungeon/economy/DeathCurrencyService.java
- src/main/java/net/goui/cosmicdungeon/economy/pricing/ItemTransferRules.java
- src/main/java/net/goui/cosmicdungeon/item/identity/AttunedInventoryEvents.java
- src/main/java/net/goui/cosmicdungeon/item/identity/ClassItemOwnership.java
- src/main/java/net/goui/cosmicdungeon/item/identity/ItemMovementGuard.java
- src/main/java/net/goui/cosmicdungeon/item/identity/ItemMovementRules.java
- src/main/java/net/goui/cosmicdungeon/item/identity/ProtectedItemLifecycle.java
- src/main/java/net/goui/cosmicdungeon/item/identity/WorldItemOwnership.java
- src/main/java/net/goui/cosmicdungeon/mixin/AttunedInventoryMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/BogatyrEggOffspringMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/BogatyrEggSpawnMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/OwnedItemPickupMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/OwnedProjectilePickupMixin.java
- src/main/java/net/goui/cosmicdungeon/playerclass/api/ClassItemEquipmentGuard.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrThreats.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrWolfEvents.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/CompanionAllies.java
- src/main/resources/cosmicdungeon.mixins.json
- src/test/java/net/goui/cosmicdungeon/dungeon/DungeonKillCreditTest.java
- src/test/java/net/goui/cosmicdungeon/item/identity/ClassItemOwnershipTest.java
- src/test/java/net/goui/cosmicdungeon/playerclass/bogatyr/CompanionAlliesTest.java
