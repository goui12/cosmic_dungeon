# Development world-entry GameTest serialization repair

Cameron's observed test: both an old save and a new world hang on Loading terrain.
Live console captures the same ClassCastException while RegistrySynchronization encodes
TEST_INSTANCE: DirectGameTestInstance returns FunctionGameTestInstance.CODEC despite
being an unrelated subclass. Audio continues independently while configuration cannot finish.

Branch: fix/gametest-world-entry-20260924, based on the current menu-branding work.
Owned integration surfaces: GameTest function registration and two initialization calls in
CosmicDungeonMod.java. No other hotspot work is in progress in this session.
Scope: both existing test suites, one shared OOP function-registration adapter, a native
serialization regression fixture/task, this record and a release fragment.
All 18 existing test IDs, environments, bodies and timing metadata must remain intact.
No tests are disabled. Native function keys and native instances replace the unsafe cast.
Functions register only in development, matching NeoForge's GameTest event availability.
No gameplay, world/chest data, item/block IDs, packet schemas, security rules, or migrations
are changed. No extra per-tick work, runtime dependency, or persistent monitoring service.

Validation planned: Java21 build; native registry-payload encode/decode check; menu checks;
scoped diff review; launch runClient and have Cameron create/rejoin a test world while
capturing output. No datagen applies to this Java registration repair.
Dedicated/GameTest server execution and deployment remain separately authorized.
Existing staged historical-JAR deletion, generated cache edits and untracked output preserved.
Originals and evidence: sibling CosmicDungeon_AI/backups/gametest-world-entry-20260924.
Results and manual QA will be updated before the final local checkpoint.

Native regression runner: plain JavaExec cannot initialize SharedConstants without FML.
Use ModDevGradle unitTest with test-scoped JUnit 5.11.4 and its native loader integration.
No ephemeral server extension is used; no world or graphics context is created by the test.
All 18 native payloads must encode/decode with retained IDs, environments and metadata,
and each executable test function must exist in the native TEST_FUNCTION registry.
A negative fixture reproduces the original ClassCastException, so this check must detect it.
Reference: https://github.com/neoforged/ModDevGradle#unit-testing-with-junit

## Validation and handoff

- Java21 build + menuBrandingChecks: PASS, build-native-final.log (21 seconds).
- Native JUnit registry test: 1 test, 0 failures/skips, 93 assertions. Each of the
  18 retained tests has a registered executable function and a native registry payload
  that encodes/decodes without losing function identity or metadata. The negative
  incompatible-instance fixture reproduces the original ClassCastException.
- Existing d1OfflineChecks: PASS, existing-offline-checks.log (13 seconds). Their
  original standalone JavaExec entry point is retained. JUnit discovery selects
  *Test/*Tests classes; standalone *Checks programs continue using their own runners.
- Menu/loading checks: 34 + 8 PASS. No artwork/audio/panorama changes in this repair.
- Confirmed all 18 original GameTest bodies are unchanged. GameTest gameplay bodies
  were not executed by the serialization test; no dedicated/GameTest server was launched.
- Main JAR contains FunctionGameTestSuite but no obsolete DirectGameTestInstance
  classes, regression fixture, or JUnit classes.
- Test-only WorldEdit CUI dependency 4.0.2 matches run/mods/worldedit-mod-7.3.17.jar's
  embedded NeoForge protocol companion; no game/runtime dependency was added.
- Plain JavaExec first failed because no FML loader existed; native test setup then
  identified the missing test-only WorldEdit CUI companion and discovery of standalone
  Checks classes. Corrected the test harness; retained the native codec assertions.
- Datagen and changed-JSON checks: not applicable (no resource/data JSON changes).
  No destructive clean under the tracked historical-JAR rule. Scoped diff check passed.
- World data, authored chest items, gameplay transaction services, packet schemas,
  access controls, and existing registry IDs remain unchanged. No migration required.
  Function registration follows NeoForge's development-only GameTest event boundary.
- runClient relaunched after validation: PID34364 responding; graphics/audio initialized;
  no startup errors. Logs: logs/observed-client-20260923-221503/run-client.log under
  CosmicDungeon_AI. Preexisting 19 startup warnings remain outside this repair scope.
- Cameron's remaining manual test: create a disposable world, confirm terrain/control,
  return to menu and rejoin; verify menu music stops/returns. Actual world entry and
  audio/visual acceptance remain pending at this checkpoint.
- No world was entered by the assistant. The two clients stuck on registry configuration
  were force-stopped under Cameron's restart instruction; their Gradle failure exits
  are expected consequences of termination.
- Exact files in this repair:
  - build.gradle
  - src/main/java/net/goui/cosmicdungeon/CosmicDungeonMod.java
  - src/main/java/net/goui/cosmicdungeon/trade/TradeFinalizationGameTests.java
  - src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
  - src/main/java/net/goui/cosmicdungeon/gametest/FunctionGameTestSuite.java
  - src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
  - docs/ai/tasks/gametest-world-entry-20260924.md
  - docs/releases/fragments/gametest-world-entry-20260924.md

Review and local checkpoint follow these completed notes. No push/deployment requested.
Future improvement: add a dedicated world-entry smoke test when separately authorized.

Live follow-up before checkpoint: at22:16:03 the new capture reports Dev joined the game.
At22:16:57 PID34364 is responding with a Singleplayer window title and no ERROR events.
This verifies passage through the formerly failing registry-configuration stage.
Visual terrain/control, music behavior, return/rejoin and licensed multiplayer remain
separate acceptance checks. A missing tests datapack warning and offline-profile chat
public-key warning did not prevent this join; no authentication changes were made.
