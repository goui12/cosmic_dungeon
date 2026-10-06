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
Clean CI run37494819836 passed build and all36 native GameTests; automated production review completed without findings. The fixture-only follow-up was manually reviewed. No local GameTest server/client launch.

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
Batch11 alpha publishing/install receipts are recorded below. Companion app-managed delivery remains unverified.
15 batches remain after11:12 wolf friendly fire;13 spawner visuals/warning/rockets;14 party HP/effects;15 run stats;16 inventory layout;17 potion mercenary split;18 Judicator;19 Skills UI;20 resources/tags;21 requests;22 player Theurgist;23 run-only wolves/commands;24 core modes;25 advanced modes/performance;26 integration/Beta.
Future improvement: Batch14 replaces the remaining compact roster with complete HP/effect rows.

## Exact files

- docs/ai/D1_REMAINING.md
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
- docs/ai/tasks/party-trade-readiness-20261006.md
- docs/ai/tasks/tamsin-replay-beta-20261006.md
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

## First clean CI follow-up
Run37493700745 passed clean build and34/36 native GameTests; two new fixture assertions failed (completed exchange and full-inventory recovery).
The completion fixture inserted uncatalogued items directly into offer containers; actual trade validation correctly rejected them.
Fixtures now use catalogued apples/bread through native shift-click and explicitly finite survival inventories, so creative overflow semantics cannot invalidate the full-inventory scenario.
All original final inventory/currency assertions remain; diagnostic actual values added. Production guards/transactions are unchanged.
Automated GitHub review completed on6ae97532a9b3df458510d5468b7bfdff1f67b811 with no findings. Follow-up is limited to this native fixture and report; manually reviewed.

## Verified completion and remaining deployment
- Runtime/source: 8b1c671ec0c94328976cc5db75ac9a2c2c4014f5; PR228 remains open for SetA's later merge checkpoint. Batch10 completed-set PR227 is merged to main.
- Exact release: 1.6.1-alpha.1, tag v1.6.1-alpha.1; CurseForge Release run 37495307261 succeeded on the same runtime source.
- CurseForge main file 9081954, helper file 9081952, both submitted. Main SHA256 031f388058d287d5fb458b9dda8fc5a4608f10510561e903fa738875d93aca7d; helper SHA256 737b930c64ef61cfab026f6d55b813042a698307f4b4aaa8228a13acef09238d.
- ADMIN client main/helper installed and hash-verified at 2026-10-06T16:29:39.5730401Z. No game/client launch.
- TEST was rechecked at 2026-10-06T16:31:19.1591779Z: latest log [06Oct2026 11:01:12.537] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Server empty for 60 seconds, pausing. It remains prior1.5.12-alpha.1; no server jar/properties were changed and no restart attempted.
- latest-built now points to the exact released CI artifact. current-test intentionally remains the prior matched deployment until stopped TEST receives the same alpha.
- Protocol17 rejects an outdated server/client combination; wait for the TEST update before multiplayer QA.
- Both release jars are archived on CurseForge/GitHub. Companion automatic app installation/moderation acceptance and licensed co-op/UI testing remain pending.
- Release CI reran publisher tests, source JSON, clean build and all36 native GameTests. No schema migration or datagen was required.
- Google metadata refresh remains blocked by expired authorization at2026-10-06T15:34:03Z; no fresh source semantic audit claim.
- Stop here:15 queued batches12-26; wolfpack reset cleanup and the approved splash-potion pools are recorded for23/22.

Final documentation-only checkpoint does not change the released runtime or reuse its tag.


## Follow-up review correction, 2026-10-06
PR229's cumulative review identified that endForAdventure checked only the party member's
recovery result after cancelling a trade with an outsider. It now captures the peer before
session removal and requires both participants' recovery checks. A native outsider-hold
regression verifies rejected Start preserves readiness, closes both trade sessions, and
retains exactly-once refunds. No currency/schema/protocol change or migration/datagen.
This correction remains on Batch11's PR228; the immutable1.6.1-alpha.1 release is not changed.
Distribute it with the next newly versioned Batch12 alpha after full validation.
Exclusive follow-up hotspots: trade custody handoff and native test registration.
