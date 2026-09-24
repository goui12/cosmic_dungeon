# Selector Tamsin placement

## Request and scope
Cameron's 2026-09-24 request: add a third [Tamsin] choice beside the existing Developer
Rift Destination and Configure Dungeon Player Count links, for easy NPC placement.
Branch feature/selector-tamsin-placement-20260924 follows presentation checkpoint74837583.
Exclusive ownership: the selector Developer menu and Tamsin command-registration hook;
new feature logic stays in the object-oriented TamsinPlacement helper. No competing writes.

## Behavior
- [Tamsin] runs /d1 tamsin place <dimension> <selector coordinates>.
- Developer authorization is checked in the command tree and again in the server handler.
  Require a living, non-spectating Developer, same source/player dimension, a loaded
  selector, configured selector range, and the existing outside-Dungeon-1 placement rule.
  Coordinates and dimension are captured in the link; stale links from another dimension fail.
- Find a supported, dry, collision-free nearby position, including room for an adult villager.
  Prefer the player's side at selector height; allow one block above/below for uneven ground.
  At most36 candidates (eight immediate neighbors plus four cardinal positions two blocks away,
  at three heights); stop at first valid position. Check surrounding chunks before collision
  reads; no forced chunk loading, scheduled work, or world/entity scans.
- Spawn a named Tamsin Vane villager: visible name, no AI, invulnerable, silent, persistent,
  facing the Developer. Automatically bind her to the clicked selector.
- If the current living Tamsin is already bound and within range, report her position.
  Otherwise explicit placement uses the existing global identity replacement policy:
  successful insertion retires the old identity, including stale unloaded entities on rejoin.
  The hover tooltip explains replacement before the click.
- Reuse NpcIdentityService.spawn and its durable identity rollback. Stage the new binding
  before join validation; remove/restore it if insertion returns false or throws.
  Failed placement does not reset player agreements or replace the old Tamsin.
- No GUI resources, packets, registry IDs, dependencies, block models, data schemas, or
  migration. Existing bind/unbind/status commands, onboarding, tax, group and ready rules
  are unchanged. Nothing is spawned automatically during build/startup.

## Sources and limitations
Existing TamsinService, TamsinData, NpcIdentityService/Data, VendorCommand and D1WatsonService
provided native command, identity transaction, and stationary NPC patterns.
Local 1.21.10 sources confirm collision checks include entity/block/border obstruction and
hasChunksAt checks availability without requesting missing chunks.
Cached canon: Tamsin Vane (Internal), Google ID1-FcHP73pFytPfoM2KhUPa6tt_2licsgWmWokto4YzE4,
version46, modified2026-08-19T22:35:48.808Z; markdown SHA256
5df441ebb452776e44a07e94159ad1af13a4e28c98223a5f38db89931ba7d49f.
Relevant retained lore is a Starting Area entry broker with personal agreement/class setup.
Live metadata refresh was attempted with the authorized local read-only Google helper but
failed because local Google authorization expired. This is not a current-canon sync claim.
Cameron's explicit placement shortcut request supplies implementation authority; no lore
rule or previously resolved party-policy decision is changed.

## Validation
Java21 Gradle build passed. All16 native JUnit methods pass (zero failures/errors/skips):
four placement tests, six selector presentation, five currency HUD and one registry codec.
Placement tests cover preference/fallback, fully blocked36-candidate bound, early exit,
false/throwing insertion rollback, success, existing-binding restoration, and agreement
preservation. They also execute838 existing NPC identity replacement/save checks.
All1,997 JSON files under src parse. git diff --check passes.
Source review verifies the two original links and all existing TamsinService methods are
unchanged apart from the one command-registration delegation.
No datagen: no generated resources affected. No clean under historical-JAR protection.
No dedicated/GameTest server launched; native test bootstrap is not a gameplay server.
Final documentation build and scoped Git checkpoint follow this report.
Candidate JAR SHA256: 0ccf342bcdfef8e9a1e8a3d5a7af1890884326fdc919a0730caf3a09e2c06157.

## Persistence, boundaries, and handoff
Only an explicit Developer click creates an entity and writes the existing binding/identity
records. Accepted player UUIDs are untouched. No saved-data migration or backfill.
Common/server-safe Java; no client imports. Server authorization prevents command forgery
from granting Dungeoneers placement rights. No new packet protocol.
No authored chest contents, vendors/trades, inventories/currency, spawners, doors/keys,
rifts/destinations, class enforcement, progression/factions, or dungeon instance changes.

Current healthy single-player client PID8160 is still running the older build. Automatic
runClient handoff is deferred until it closes; no forced shutdown or duplicate client.
Native startup, clicking, collision placement, interaction, restart persistence and
multiplayer acceptance remain unperformed. Required user tests:
1. Developer: right-click selector, click [Tamsin]; named NPC appears beside it and faces you.
2. Click again: report existing Tamsin and do not duplicate. At a different selector,
   explicit placement replaces her through the existing global identity rule.
3. Block nearby spaces / put selector over void: clear failure, previous NPC retained.
4. As Dungeoneer: no placement option; forged place command rejected; NPC opens the normal
   agreement/map/class flow. Verify original Developer destination/player-count links.
5. Save/rejoin: NPC and binding persist and accepted agreement remains accepted.
6. Try stale chat after dimension change or walking out of range: reject placement.
Dedicated-server comparison remains separately authorized TEST.

Zero implementation batches remain for this standalone feature; native QA remains.
No change to the broader numbered D1 audit plan. No push/deployment.
Possible follow-up: a placement preview if Cameron wants precise control over which side.

## Exact changed files
- src/main/java/net/goui/cosmicdungeon/block/custom/D1_Class_Selector_Block.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/TamsinService.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/TamsinPlacement.java
- src/test/java/net/goui/cosmicdungeon/npc/tamsin/TamsinPlacementTest.java
- docs/ai/tasks/selector-tamsin-placement-20260924.md
- docs/releases/fragments/selector-tamsin-placement-20260924.md

External evidence/backups:
C:/Users/Cameron/Documents/mod_development/CosmicDungeon_AI/backups/selector-tamsin-placement-20260924
Preserve unrelated staged historical-JAR deletion, generated hash caches, build output/logs.
