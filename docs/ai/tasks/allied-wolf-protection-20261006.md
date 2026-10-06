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
Java21 build,349 JUnit tests, loading assets checks,2,001 JSON and diff checks passed.
CI37504494392 passed all40 native GameTests and34 Python guard/publisher checks.
Corrected source full CI37505395568 and release CI37505674000 passed all40 native GameTests.
Both CurseForge uploads accepted; final narrative checkpoint uses the approved proof verifier.
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

## Review correction
Automated review of3051105 found ambiguous network wording in the new release note.
Clarified protocol17 is unchanged only relative to1.6.1-alpha.1; prior beta/protocol16 needs
matched client/server upgrades. This follow-up changes release/report text only. All runtime
code and the successful40 native tests remain unchanged, and the conservative release-note
boundary deliberately requests full CI again. No review finding was ignored.

## Release and deployment completion
Version1.6.2-alpha.1; tagv1.6.2-alpha.1; sourcebe96c8c3f1311797251788f92a62f315e74df625; PR230.
CurseForge main project1326805 file9082469; loading companion project1727305 file9082467.
Runtime SHA256:ce692f143a679e38d1b4ac0b8e48bf710c5a721bb2a056567cd7a08600dd18f7
Loading SHA256:2dbb34a6fe2aadaf4609966209377ebda24a5d37a4da02e0774a1bfaf35fc6e2
ADMIN has the exact published runtime/helper installed. TEST remains running/paused as of2026-10-06T17:48:21.4237164Z; its1.5.12-alpha.1 jar is unchanged. current-test stays at the prior matched build.
No server restart, client launch or server.properties write. Upload acceptance is distinct from
public moderation and licensed CurseForge-app delivery verification.
349JUnit+2loading tests,34Python checks,2,001JSON and all40 nativeGameTests passed.
Actual source reviewed at3051105538600689ee8b31d5dcc548f084a0dfd6; protocol-note follow-up manually verified against
the recorded review finding and passed complete CI. No gameplay finding remains unaddressed.
14 planned batches remain13-26. Stop after12 because the10-minute conditional extension was not met.
