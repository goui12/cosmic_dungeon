# Testing batch 4 — Tamsin groups and class selection

Base: dbf9c3ec, batch 3. Branch: feature/testing-fixes-batch4-20261003.
Three accepted changes implemented; seven implementation batches (5–11) remain.

## Behavior

- Explicitly create a group at Tamsin with a trimmed 1–32-character name. Empty,
  control/formatting-character and oversized names are rejected on the server.
  Members leave independently; the leader disbands the group. Readiness is cleared
  on roster changes. Preparation still locks the roster.
- Ungrouped players opt into Looking for Group at Tamsin. Group members see four
  sorted candidates per page with current selected classes. Every member may invite
  candidates or any eligible online friend by name, including non-LFG friends.
  Ungrouped players receive no recruiting controls or candidate list.
  Listings clear on joining/creating a group, logout, ineligibility and server stop.
- Invitations bind to one group UUID and cannot be reused for a replacement group.
  Existing expiration, target tokens, capacity checks, anti-merge protection, pending
  onboarding, ready invalidation, personal confirmations and FIFO entry remain.
- Players select/change class through Tamsin. The selector block directs players
  to her while preserving developer configuration. Server selection rejects
  non-NPC sessions, invalid stages and active runs. Changing class cancels readiness.
  No active dungeon roster can be joined through these controls.

Batch 5 remains responsible for leader-selected maximum capacity, mobile readiness
and group/invitation HUDs. Existing configured minimum and near-Tamsin entry rules
remain in effect until that batch; creating a one-player group does not waive them.

## Compatibility and boundaries

No persistent world format, NBT key, registry ID, authored chest contents or config
is changed. Group names/LFG/invitation group IDs are transient server state; no
migration is required. Network protocol changes from 7 to 8 for the bounded
recruitment payload, requiring matching client/server jars.
This pass owns networking, menu/session, lobby and class-selection authorization.
No currency, inventory, vendor, spawner, door/key, advancement or dungeon restoration
logic changed. Existing native/offline regression suites cover adjacent systems;
their passage is not a live multiplayer acceptance claim.
No added dependency, world scan or per-tick network stream: existing throttled
viewer polling and equality-based delta sends remain; LFG cleanup visits only
advertised UUIDs and packets contain at most four candidate rows.

## Validation

Java 21 compile/test and d1OfflineChecks passed. 107 native JUnit tests, zero
failures/errors/skips, including six new recruitment/group/codec tests.
Existing invitation fixtures now explicitly create groups and assert member invitations,
matching the accepted behavior; token, expiry, capacity, queue and anti-merge tests remain.
Full Java 21 Gradle build passed; all 1,997 src JSON files and the scoped diff check
passed. Candidate jar SHA256: 72B2C87698E1E842FD32A066213756A88A3DF2D8365F042946F5CEB96EBB224C.
Final commit/deployment/publication receipts are recorded in the local task handoff. No datagen applies: this changes Java UI/server logic and docs,
with no generated resource changes. No destructive clean or GameTest/dedicated launch.
Unrelated historical-JAR staged deletion, generated caches and logs are preserved.

## Pending gameplay testing — not decisions or questions

1. At Tamsin, create a named group; verify a member can leave and the leader can
   disband. Blank/oversized names must fail. Reopen Tamsin and confirm the group name.
2. With two or more players, advertise LFG, verify the displayed class, invite from
   a nonleader, and invite a non-LFG friend by name. Accept/decline/expire invitations;
   leave/disband before acceptance and confirm no stale admission. Test multiple pages
   with five LFG players and verify ungrouped players cannot view recruitment controls.
3. Change class through Tamsin and verify readiness resets. The selector block must
   direct ordinary players to Tamsin; developer configuration remains available.
   Start an eligible run and confirm class changes and late joins remain blocked.
   Check GUI layout at normal scales and existing ready/queue behavior.

Earlier cumulative testing remains: A ownership/death drops, kill payouts and wolves;
B trade/advancements/death-forfeit restoration; C spawner permissions, placement,
blocking/recovery, independent activation and living-mob caps. None has been claimed
as licensed gameplay acceptance. Settled decisions remain recorded in the rollout.

## Exact files

- `AGENTS.md`
- `src/main/java/net/goui/cosmicdungeon/block/custom/ClassSelectorTeleportUtil.java`
- `src/main/java/net/goui/cosmicdungeon/block/custom/D1_Class_Selector_Block.java`
- `src/main/java/net/goui/cosmicdungeon/client/screen/ClassSelectorScreen.java`
- `src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyPanel.java`
- `src/main/java/net/goui/cosmicdungeon/menu/ClassSelectorMenu.java`
- `src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java`
- `src/main/java/net/goui/cosmicdungeon/network/PartyPayloads.java`
- `src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyLobby.java`
- `src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyService.java`
- `src/test/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyChecks.java`
- `src/test/java/net/goui/cosmicdungeon/npc/tamsin/SoloPartyMinimumTest.java`
- `src/test/java/net/goui/cosmicdungeon/npc/tamsin/NamedPartyRecruitmentTest.java`
- `docs/NPCs/Tamsin_Vane.md`
- `docs/ai/TESTING_ROLLOUT_20261003.md`
- `docs/ai/D1_REMAINING.md`
- `docs/ai/TESTING_BATCH_4_20261003.md`
- `docs/ai/tasks/testing-fixes-batch4-20261003.md`
- `docs/releases/fragments/testing-fixes-batch4-20261003.md`

## Next improvement

Batch 5 adds the approved capacity selection and inventory controls while letting
ready players move around the starting area.
