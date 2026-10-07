# Batch 14 - Complete group HUD
Authorized October 6: TEST stopped; complete Batch 13 deployment then Batch 14 only.
Base fb195f1eb929c980c46711f0816c06f1c5c7161e; branch feature/party-health-effects-20261006.
Batch 13 exact published build installed on stopped TEST; ADMIN matched; current-test a94958b1.
Ownership: PartyPayloads/ModNetwork, party snapshot/mercenary status, client HUD/layout, native GameTest registration.
Changes: server-authoritative human and mercenary vitals/effects; active-run cards; no gameplay ability or persistence changes; reviewed trade Start recovery guard corrected.
Protocol 18 requires both ends updated. No datagen or migration.
Validation passed: Java21 build355JUnit+2loading assets; full Integration Gate37541085447 and release CI37541580285 passed all45 native GameTests.34Python and2,001JSON checks passed. One inherited trade retry finding addressed, verified and resolved; correction manually reviewed and fresh full CI passed.
Manual QA: full party including self/mercs; damage/heal/effect add/remove; death/relog/outside dungeon; GUI scales and scrollbar; recipe-book compact hover; revive/skills regression.
Canon: same-day Google OAuth expiry at 15:34Z, cached sources plus explicit approved user requirements; no fresh semantic audit claimed.

Review follow-up owns TradeSessionData, D1PartyTrades and PartyTradeGameTests: retain cancelled-session peer recovery across valid Start retries in the existing transient lobby lifetime. No new persistence. Extend native hold regression with repeated attempts, offline peer, recovery and a later unrelated outsider trade. Full build/CI rerun required for changed inputs.

## Completed release and deployment
Version1.6.4-alpha.1; tagv1.6.4-alpha.1; runtime source0804dc40bdd555aa9889e1fadc9fb4b4b34327cb; [PR232](https://github.com/goui12/cosmic_dungeon/pull/232).
CurseForge main project1326805 file9084607; companion project1727305 file9084606.
Runtime SHA256:0fa8f2c67d073b46ae9446e0ad60f423ac4442a109e02f234a182b29f54265e9
Loading SHA256:302db56fcb18a0dc50a1ae95bbcfc37d4e1067a7e9b446b2203f300112852a6d
The exact CI runtime is installed on stopped TEST and closed ADMIN; ADMIN also has the matching helper.
Both installed hashes were independently verified before publishing current-test; latest-built also uses the CI artifact.
Shutdown evidence:[06Oct2026 22:06:07.475] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Stopping the server, checked2026-10-06T22:40:51.4151507Z.
server.properties hash preserved:FDD602AB63E58A8F27D38BCD351975E28A2AB1065FCFBAF03DF36193934058FB. No restart or game launch.
CurseForge accepted uploads; public moderation/app availability and licensed companion auto-update acceptance remain unverified.

## Behavior and regression boundaries
All active-run human members including self and companions have health/effect cards. Positive and negative icons use native sprites.
Unavailable states carry no stale health/effects; no cross-run/outside-party detail request or client-authoritative gameplay.
World rows use bounded columns; overflow effects show +. Inventory cards show up to64 effects/member with wheel plus draggable scrollbar,
and explicit overflow beyond the wire limit. Icon hover gives name/level; row hover preserves full names/companion skills/recovery.
Recipe-book-open layout remains the compact header with health in hover; close it for full cards. Batch19 removes that book.
Existing readiness, mercenary revival and resurrection interactions are preserved; no gameplay ability changes. Reviewed trade recovery now blocks every Start retry until the affected outside peer recovers; no transaction record or item format changes.
No world/schema/registry ID, authored spawner/preset/chest, inventory, currency or access-policy mutation. No migration/datagen.

## Manual licensed acceptance still needed
1. Enter with multiple humans and mercenaries. Verify self and every member's name/class/HP; damage/heal and apply/remove positive/negative effects.
2. Kill/revive, disconnect/rejoin, and leave the dungeon dimension; verify Dead/Offline/Unloaded states replace stale HP and active readiness stays absent.
3. Test minimum/default GUI scales, long effect lists, mouse wheel and scrollbar drag. Hover full names/effects/mercenary skills; verify slots, currency, recipe book and revive footer stay usable.

## Exact task files
- docs/ai/tasks/party-health-effects-20261006.md
- docs/releases/1.6.4-alpha.1.md
- gradle.properties
- src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyHud.java
- src/main/java/net/goui/cosmicdungeon/client/screen/PartyHealthHud.java
- src/main/java/net/goui/cosmicdungeon/client/screen/PartyHealthLayout.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryRespawns.java
- src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java
- src/main/java/net/goui/cosmicdungeon/network/PartyPayloads.java
- src/main/java/net/goui/cosmicdungeon/network/PartyVitals.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyHudService.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyTrades.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/PartyHealthGameTests.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/PartyTradeGameTests.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/PartyVitalsSnapshot.java
- src/main/java/net/goui/cosmicdungeon/trade/TradeSessionData.java
- src/test/java/net/goui/cosmicdungeon/client/screen/PartyHealthLayoutTest.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
- src/test/java/net/goui/cosmicdungeon/network/PartyVitalsTest.java
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
- docs/ai/D1_REMAINING.md

## Workflow and next work
Reused the checkout and validated delivery helpers; consolidated reads/notes; no unrelated database audit/datagen or local game launches.
Full runtime/release tests retained. Final narrative-only proof must match the successful ancestor build inputs and pass34Python verifier checks.
12 batches remain15-26. Stop after14; next15 adds human row current-run inspection. Completed SetA merges/Beta after16.
Potential later improvement: share final panel placement with Batch19/21 so Skills/Requests and roster scroll capture remain coordinated.
