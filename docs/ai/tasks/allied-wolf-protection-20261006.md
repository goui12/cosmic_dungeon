# Batch12: allied-wolf protection

Task branch: feature/allied-wolf-protection-20261006. Version1.6.2-alpha.1.
Exclusive hotspots: shared companion combat/effect boundary and native GameTest registration.
Parent includes approved workflow optimization PR229 and the corrected Batch11 outsider-trade handoff PR228.
One shared existing predicate now includes tamed player Bogatyr wolves via saved ownership, preserving
offline-owner protection and keeping mercenary ownership independent. Existing native projectile,
damage, instant splash, timed effect and lingering-cloud guards consume that predicate.
No new entity scans, AI ticks, packets, registries, saved-data fields or dependencies.
No migration or datagen needed. Existing authored spawners/chests/inventories remain untouched.

## Validation
Pending full Java21 build/CI and exact release verification.
Three new native GameTests cover owner/teammate/mercenary/companion damage, firework AOE,
wolf armor durability, hostile and environmental damage, wild-wolf behavior, actual flaming
tipped-arrow flight, splash/lingering instant and timed harm, and beneficial healing.
The separate prior-batch outsider-hold regression is also included. No existing test weakened.
Manual dedicated multiplayer QA remains pending:
1. Hit an owned Bogatyr wolf with melee, flaming/tipped arrows and allied potions; health/armor/effects remain safe.
2. Let a hostile mob strike/shoot/poison the wolf; damage/effects still apply, then verify allied healing.
3. Repeat with a mercenary wolf and an offline Bogatyr owner; confirm normal equipment feeding/armor interaction.
4. Start Adventure during an outside-party trade; ordinary refunds and Ready state remain correct.

## Sources and boundaries
Read cached Wolf Internal document10-3IgopUqHKyPHuZDKlpa64JMYXmq-_8GgKtQFhLX3c.
Existing read-only Google authorization was verified expired at2026-10-06T15:34:03Z earlier this session;
no repeated consent or fresh semantic audit claimed. Explicit Cameron friendly-fire requirements govern.
Older five-wolf/permanent-pack text is already superseded by settled user decisions, handled in later batches.
Access/teleport/spawner/door/currency/progression/network/persistence logic is unchanged by the wolf fix.
The reviewed trade correction uses existing server-authoritative custody; no item recreation.
Future improvement: complete the already planned wolf command/lifecycle/performance batches.

## Exact files
- gradle.properties
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryWolves.java
- src/main/java/net/goui/cosmicdungeon/mercenary/AlliedWolfGameTests.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
- docs/releases/1.6.2-alpha.1.md
- docs/releases/fragments/batch12-allied-wolves.md
- docs/ai/tasks/allied-wolf-protection-20261006.md
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
- docs/ai/D1_REMAINING.md
