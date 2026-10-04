# Tamsin artwork installation

## Scope and authority
Cameron supplied tamsin_villager.png and tamsin_plains.png in
C:/Users/Cameron/Documents/BlockBench/NPC/Tamsin and requested installation, assignment,
build, GitHub commit, and runClient. Branch feature/tamsin-artwork-20260924 follows cb43e760.

Exclusive integration ownership: one attachment-registration delegate in CosmicDungeonMod,
TamsinService bind/unbind callbacks, client mixin list, and new appearance attachment registry.
No competing agent edits. Existing central networking/identity/save services remain authoritative.

## Behavior
- Both original 64x64 PNGs are installed byte-for-byte beneath
  assets/cosmicdungeon/textures/entity/tamsin/.
- Only a villager with the current or not-yet-adopted legacy Tamsin UUID binding receives
  the server-derived cosmetic attachment. Display names do not identify the NPC.
- Entity join rebuilds appearance on load; explicit bind updates a loaded NPC;
  unbind removes appearance through indexed UUID lookup across loaded dimensions.
- A nonpersistent NeoForge boolean attachment syncs only on changes and initial tracking.
  Ordinary villagers do not receive a stored default merely by being rendered.
- Client extraction captures a boolean into the native render state. Two client-only
  mixins select the body texture and replace biome/profession/level clothing with the
  supplied outfit. Normal invisibility, lighting, animation, and outlines remain native.
- Non-villager legacy bindings retain their original model; the supplied art is a
  villager UV map. Existing stationary placement produces villagers.

## Compatibility and cost
One new cosmetic attachment registry ID: cosmicdungeon:tamsin_appearance. No serializer,
new save fields, registry renames, player-data rewrite, or migration. Existing Tamsin
bindings, agreements, immunity, NPC replacement, inventory, currency and class rules remain.
Client and server should run matching builds for the attachment registry and sync.
No client classes referenced by common appearance code. No gameplay action trusts this flag.
No polling, world scans, forced chunk loads, custom packet system, or dependencies.
Uses the existing body plus clothing draw passes; checks are constant time per villager.
No datagen: two hand-authored PNGs and rendering code only. No clean/GameTest/dedicated-server
launch, deployment, or merge. Preserve the unrelated staged historical JAR deletion and caches.

## Sources
Local 1.21.10 / NeoForge 21.10.64 renderer, layer, render-state, and attachment sources.
https://docs.neoforged.net/docs/1.21.10/datastorage/attachments/
https://docs.neoforged.net/docs/1.21.10/entities/renderer/
Cached Tamsin Vane (Internal), ID 1-FcHP73pFytPfoM2KhUPa6tt_2licsgWmWokto4YzE4,
version46 modified2026-08-19T22:35:48.808Z, reviewed in this session. Live metadata refresh
attempt failed: saved local Google authorization expired. No fresh-canon sync is claimed.
User's explicit artwork instruction supplies the visual design authority.

## Validation
Java21 .\\gradlew.bat build --offline --console=plain passed. All27 native JUnit tests
passed (four new appearance tests), zero failures/errors/skips. Existing placement,
immunity, selector, HUD and world-entry serialization tests pass. New coverage checks
current/legacy/retired/unbound UUID selection, read-only lookups, change-only sync,
attachment nonpersistence, and clearing reused render states. All1997 source JSON parse.
Both64x64 PNG CRCs, original hashes and packaged JAR bytes verified. Scoped diff check
passed; final staged review and commit follow this note. No datagen needed.
runClient wrapper18580 started successfully. At09:43:58 both event hooks registered,
09:43:59 both client mixins applied, and09:44:01 sound/atlases initialized. Minecraft
window is responding with no startup ERROR or uncaught exception in the bounded log.
No gameplay input automated; visual fit and dedicated-server testing remain manual.
Initial built JAR SHA256: ed45805c1bcbe785888f0e27e42f598bbc393b85ab46955d93881b6e4b022670.
Final documentation build and normal feature-branch push follow; no main merge.

## Manual acceptance
1. Load the existing world and inspect the bound Tamsin from front, sides, and back.
   Check face/clothing alignment and that the robe is not covered by vanilla profession art.
2. Place Tamsin through a selector's Developer [Tamsin] link; verify the same custom appearance.
   A repeated click must reuse the nearby NPC. Conversation and immunity should still work.
3. Check an ordinary villager and one renamed Tamsin Vane: their normal skin must remain.
4. Save/rejoin and leave/reenter tracking range: Tamsin's custom appearance must return.
5. In a disposable setup, unbind Tamsin: the retained villager returns to normal textures;
   bind again to restore the artwork. Dedicated multiplayer appearance remains separate QA.

## Files and evidence
Exact allowlist and asset hash manifest are saved in:
C:/Users/Cameron/Documents/mod_development/CosmicDungeon_AI/backups/tamsin-artwork-20260924/
Original art is backed up there before moving into the repository.
Possible future improvement: add a separately approved map/compass accessory model.

## Exact file allowlist
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/TamsinAppearance.java
- src/main/java/net/goui/cosmicdungeon/client/TamsinAppearanceRender.java
- src/main/java/net/goui/cosmicdungeon/mixin/client/TamsinVillagerRendererMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/client/TamsinClothingMixin.java
- src/test/java/net/goui/cosmicdungeon/npc/tamsin/TamsinAppearanceTest.java
- src/test/java/net/goui/cosmicdungeon/client/TamsinAppearanceRenderTest.java
- docs/releases/fragments/tamsin-artwork-20260924.md
- docs/ai/tasks/tamsin-artwork-20260924.md
- src/main/java/net/goui/cosmicdungeon/CosmicDungeonMod.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/TamsinService.java
- src/main/resources/cosmicdungeon.mixins.json
- src/main/resources/assets/cosmicdungeon/textures/entity/tamsin/tamsin_villager.png
- src/main/resources/assets/cosmicdungeon/textures/entity/tamsin/tamsin_plains.png
