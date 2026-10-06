# Batch11 task and review report - 2026-10-06

## Scope and ownership
Authorized by Cameron after final wolf lifecycle/potion answers. Implement11, stop before12.
Base: merged Batch10 e57b27bb78e592fca521e05bc78e305f23c55c2f; branch feature/party-trade-readiness-20261006.
Version1.6.1-alpha.1. Exclusive hotspots: D1 party queue/start, trade custody entrypoint, PartyPayloads/ModNetwork and native test registration. No concurrent writers.

## Behavior and design
- Readiness remains independent of inventory/menu sessions. Existing code had no direct trade-to-unready call; native regressions exercise actual trade opening, item/payment offers, cancellation and completion against the ready group.
- Validate current leader/revision/all-ready before touching any trade. Return unfinished offers/cursor to original owners through existing custody/recovery, then submit queue. Cancellation is idempotent; final confirmation cannot transfer after a cancelled session. Already completed trades remain completed.
- Reject invite/accept while either participant's party is QUEUED/PREPARING. Clear pending invitations at valid Start. Trading is permitted in assembly/readiness and again after entry. Trade expiry/adventure countdown unchanged.
- Full inventory recovery stays queued; no overwritten stacks, dropped synthetic refunds or invented currency. A held recovery blocks entry while preserving readiness for retry.
- Explicit AI identity in bounded Member packet; active mercenary class comes from authoritative contract even dead/unloaded or skill-free. Human readiness totals exclude AI and same-name lookup never selects an AI row.
- Hide Ready/Not Ready in all active HUD forms, always omit AI readiness, show random name/class without owner. Resurrection ability cooldown text remains useful and is preserved.

## Validation
Java21 local Gradle build passed. Native JUnit: 349; loading asset tests: 2; JSON parsed: 2001. Diff checks passed.
Three new native GameTests cover unfinished accepted trade/cursor/refunds/stale and unauthorized Start, completed native trade, and full-inventory recovery.
Clean CI build/native GameTests and GitHub review pending. No local GameTest server/client launch.

## Data, performance, security and boundaries
No registry, saved-data/NBT/schema/preset or authored-spawner changes; no migration. No datagen-managed resource changes; datagen not applicable.
Protocol16->17 requires matched client/server mod versions. Existing payload row/string limits retained; no client-controlled readiness grants.
Server-thread start revalidates actor and revision before bounded <=6-member trade handoff; no new polling/world searches, dependencies or heap changes.
Access/class/starting dimension validations remain in D1PartyService before handoff; entry inventory/teleport guards unchanged.
Existing trade transactions, currency reserves, item custody and native disconnect/death hooks retained. Doors/keys, mobs, faction/progression, wolf lifecycle and authored class chests unchanged.

## Remaining licensed manual QA
1. Ready leader/member; trade items and Trace, cancel then complete another trade. Both stay ready, including returning to Tamsin after closing trade.
2. Leader starts while member trades with a third player outside the group. Both trade menus close, original offers/cursor return once, Trace stays correct, group enters. Try a full inventory refund and then free a slot.
3. Start and final confirm close together: exactly one completed exchange or one cancellation, never duplicate/lost items/Trace. Invalid/nonleader/stale requests cannot cancel unrelated trades.
4. World/inventory/recipe-book compact HUD: active humans show no readiness; living/dead/unloaded mercenaries show name/class, no owner or party-ready status. Check GUI scales/long names and cooldown tooltips.
5. Batch10 top-right i: lore Yes/map/Continue or No returns to current selector without changing agreement/class/group/readiness; refresh/resize remain safe.
No manual client interaction has been claimed.

## Rollout
Batch10 merged/released successfully; ADMIN received exact beta main/helper. TEST was observed running/paused on2026-10-06 and remains prior revision until shutdown is verified. Server properties were read only; no restart.
Batch11 alpha publishing/install receipts will be appended after clean CI/review. Companion app-managed delivery remains unverified.
15 batches remain after11:12 wolf friendly fire;13 spawner visuals/warning/rockets;14 party HP/effects;15 run stats;16 inventory layout;17 potion mercenary split;18 Judicator;19 Skills UI;20 resources/tags;21 requests;22 player Theurgist;23 run-only wolves/commands;24 core modes;25 advanced modes/performance;26 integration/Beta.
Future improvement: Batch14 replaces the remaining compact roster with complete HP/effect rows.

## Exact files
- docs/ai/D1_REMAINING.md
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
- docs/ai/tasks/party-trade-readiness-20261006.md
- docs/releases/1.6.1-alpha.1.md
- docs/releases/fragments/batch11-party-trade-readiness-20261006.md
- gradle.properties
- src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyHud.java
- src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyPanel.java
- src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyPresentation.java
- src/main/java/net/goui/cosmicdungeon/client/screen/MercenaryHudLayout.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryRespawns.java
- src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java
- src/main/java/net/goui/cosmicdungeon/network/PartyPayloads.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyHudService.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyLobby.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyService.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyTrades.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/PartyTradeGameTests.java
- src/main/java/net/goui/cosmicdungeon/trade/TradeSessionData.java
- src/test/java/net/goui/cosmicdungeon/client/screen/D1PartyPresentationTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/PartyIdentityCodecTest.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
- src/test/java/net/goui/cosmicdungeon/npc/tamsin/PartyTradePolicyTest.java
