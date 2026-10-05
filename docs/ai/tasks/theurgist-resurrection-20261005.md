# Batch 9: Theurgist resurrection, 2026-10-05

Authorized implementation: Batch 9. Branch feature/theurgist-resurrection-20261005,
stacked on Batch 8 source 5a4e0fce53c2286b1764a4c396827dc01022c535 / PR225.
Version 1.5.12-alpha.1. Cameron's latest explicit decision is Positive Potions level 10;
this supersedes the original level-50 requirement. Negative Potions does not unlock it.

## Behavior and integration

Living Theurgists at Positive Potions 10 offer resurrection to a dead online member in
the same active dungeon run and physical dimension. The online hirer may be dead; combat
AI's living-hirer rule is unchanged. Dormant/unloaded mercenaries, exited/offline/spectator/
developer players, sealed outcomes and cleanup/reset runs cannot offer or accept.

The native final-death hook records exact fractional coordinates, yaw/pitch and a UUID.
Cancelled death creates no record. The death screen displays one current eligible offer,
updating as the existing bounded party HUD snapshot changes. Acceptance carries run,
mercenary and death identity; the server checks all three, active ownership/membership,
cooldown and the existing inventory-transaction recovery guard again. A short per-player
request throttle bounds repeated forged requests; no global request/offer cache is added.

Before respawn, the death is consumed and the mercenary cooldown saved with the existing
verified D1RunData persistence boundary. An uncertain save fails closed. Native PlayerList
respawn performs inventory/class/currency clone hooks. A synchronous ThreadLocal supplies
the exact destination at the respawn-position HEAD and is removed in finally; ordinary
respawns retain their normal behavior. No bed/anchor charge is consumed or future bed
setting overwritten. Original drops stay dropped; no inventory or currency refund/copy.

Protection lasts 100 overworld game ticks, applied during native clone and confirmed on
return. A damage invulnerability event preserves other invulnerability sources and stops
overriding after expiry. Cooldown is 3,600 game ticks (three minutes at normal TPS), tracked
per mercenary in the run even while resting/unloaded. Paused/offline servers do not advance
game time. Inventory HUD shows Ready or remaining m:ss once unlocked.

Exclusive hotspots owned: native death/respawn mixins, PartyPayloads codec, ModNetwork
registration/protocol, existing per-run serialized value keys, GameTest registration and
gradle.properties version. No other active writer. No core mod/registry/class-chest edits.

## Compatibility and regression boundaries

Reuse existing D1RunData values; no save identifier/schema or entity NBT field is renamed.
At most one latest death per run member and one cooldown per contract. Empty legacy values
mean no offer and ready cooldown; malformed values fail closed. Normal respawn clears
the latest death, reload retains it, and run cleanup resets all new values. A death from
before this update has no exact recorded offer; normal respawn/forfeit remains available.
Full world backups remain the rollback source; older binaries ignore these extra keys.

Protocol 15 advances to 16 for the extended HUD row and bounded resurrection request.
Client and dedicated server share the same runtime JAR; common code imports no client
classes. No new entity scans, forced chunk loads, polling loop or runtime dependency.
Existing delta HUD cadence is retained; cooldown changes at most once per second.

Access/class/teleport guards, native clone, death currency and inventory recovery were
inspected. Their services are reused unchanged. No edits to authored class-chest contents,
spawners/presets, doors/keys, rifts/RD, vendors/trades or advancement rewards. No spawner
migration or manual world rebuilding is required. Datagen is inapplicable: no generated
model/tag/recipe/loot/advancement resource changed.

## Validation

Java 21 local build passed with all 336 native unit tests and two loading-helper tests.
Initial CI passed its clean build and 32/33 GameTests; the new native respawn fixture
needed an in-memory network channel for clone hooks, because NeoForge FakePlayer has none.
The fixture now supplies a private channel; no gameplay gate or assertion is bypassed.
Seven new tests cover exact death/state persistence, old fields, malformed values,
single-use/replaced death tokens, cooldown expiry/reset, protection expiry and packet/HUD
round trips. Two additional native CI GameTests cover Positive 9/10 versus Negative 50,
dead hirer, dormant/offline/reset/sealed states, cancelled/final death, exact native respawn,
retained class/bed data, unchanged drops, duplicate requests and protection expiry.
Clean CI build plus all 33 GameTests, publisher/JSON/diff checks and PR review are required
before tagging; final source, results, receipt and installed hashes are recorded in the PR
and operational checkpoint. No local game, GameTest server, client or launcher is started.

## Pending licensed TEST QA

1. At Positive 9 (even Negative 50), die: no offer. At Positive 10, die with a living
   Theurgist, including your own: accept and check exact position, class and five seconds
   of protection. Confirm original drops are not duplicated.
2. Kill/unload the Theurgist or disconnect its hirer while the death screen is open:
   the offer disappears. Check another eligible Theurgist can offer.
3. Check inventory hover counts from 3:00 to Ready; death/revival/relog preserve the timer.
   A new dungeon resets skills/cooldown. Compare normal Respawn and Forfeit with acceptance
   queued close together; only one life transition should occur.
4. Check death-screen layout at supported GUI scales and co-op with two dead dungeoneers.
   A single Theurgist can accept only one resurrection before its cooldown.

Future improvement: licensed co-op testing can guide clearer feedback when eligibility
changes between seeing an offer and clicking it.

One planned implementation batch remains after this task: **10**, the top-right 20x18
Tamsin "i" control replaying first-time lore/map and the completed-set **1.6.0-beta**.
The detailed plan preserves agreement, class, group/readiness and progression and requires
the existing version parser/workflow to accept the exact requested beta string. Stop before
Batch 10 implementation; completed-set main merge/Beta publication follows that batch.
Cumulative licensed beta acceptance remains separate from automated validation.

## Source alignment

Relevant Google metadata/body refresh attempted 2026-10-05; existing read-only OAuth is
expired. Private Audit/BATCH9_RESURRECTION_SOURCE_REFRESH_20261005.json records the two
Theurgist source IDs, cached hashes and refresh failure. Explicit current user requirements
govern. No fresh Google semantic-alignment claim.

## Exact files

- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryResurrection.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryResurrectionState.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryResurrectionGameTests.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryRespawns.java
- src/main/java/net/goui/cosmicdungeon/mixin/DeathCurrencyPlayerMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/InnRespawnMixin.java
- src/main/java/net/goui/cosmicdungeon/network/PartyPayloads.java
- src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java
- src/main/java/net/goui/cosmicdungeon/client/DungeonDeathScreen.java
- src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyHud.java
- src/main/java/net/goui/cosmicdungeon/client/screen/MercenaryHudLayout.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryResurrectionTest.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
- gradle.properties
- docs/ai/CURSEFORGE_AND_MERCENARY_BATCHES_20261004.md
- docs/ai/D1_REMAINING.md
- docs/releases/1.5.12-alpha.1.md
- docs/releases/fragments/theurgist-resurrection-20261005.md
- docs/ai/tasks/theurgist-resurrection-20261005.md
