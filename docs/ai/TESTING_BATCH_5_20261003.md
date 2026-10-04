# Testing batch 5 — Maximum capacity and mobile group HUD

Base05685591 / Batch4 PR203. Branch feature/testing-fixes-batch5-20261003.
Three accepted areas implemented; six implementation batches6–11 remain.

## Behavior

1. The leader selects a maximum of1–6 with the minus/plus controls beside the group
   count at Tamsin. This is a ceiling, never a required total: one, two or three
   members may start with maximum6 after every member personally confirms.
   The selector's saved MaxPlayers provides the initial default; leaders choose
   their own group's maximum independently. Reducing below the existing roster,
   nonleader changes, stale requests and changes during queued/preparing entry fail.
   Capacity changes reset readiness and acceptance rechecks pending invitations.
2. Ready at Tamsin closes her screen. Group members can move beyond selector range
   within the same starting dimension while keeping readiness. The leader begins
   the check and starts the adventure at Tamsin. Not Ready withdraws only that
   member, preserves teammates' confirmations and cancels any queue/countdown.
   Re-readying never automatically restarts entry; the leader must start again.
   Class/roster changes, disconnects and dimension changes invalidate preparation.
3. Group status remains at top left after the menu closes and during inventory.
   Ready/Not Ready and Leave Group HUD buttons appear only on player inventory
   screens. Invitation HUD identifies the inviter and Dungeon1; Join Group appears
   in inventory. Pending onboarding stays pending until agreement/class selection
   is complete in the starting dimension. Group-less, uninvited players see no HUD.
   Survival and Creative player inventory screens are supported; other menus do
   not gain group buttons. Active-run rosters remain read-only and cannot use
   these buttons to join, change class or leave a dungeon run.

The full roster uses a sidebar outside inventory slots. Narrow sidebars truncate
labels with complete text on hover. With the recipe book open, a compact top-left
summary exposes the full roster on hover; buttons sit below the inventory/book,
and currency has separate space. World currency receipts stack below the group HUD.
No recipe-book policy, slots, item movement or currency transaction behavior changes.

## Authorization and compatibility

Server-side checks validate the requesting member, revision, leader privileges,
1–6 capacity, onboarding, current class, starting dimension, preparation phase and
active-run exclusion. NPC operations still require a valid nearby Tamsin session.
Inventory HUD requests use the existing Action payload's reserved containerId -1,
a five-action allowlist and cooldown. The server requires the player's inventory
menu (no other open container); actual inventory-screen visibility is client UI state
and cannot grant additional gameplay authority. Wire record shapes are unchanged.
Protocol8 becomes9 so client/server must use matching builds.

Existing selector MaxPlayers, destination/slot NBT, saved classes, run records and
registry IDs remain unchanged. Legacy minimumPartySize stays readable in old configs
but no longer imposes a minimum. No migration, world rewrite or server config edit.
Groups and HUD caches are transient. Active-run HUD reads the existing roster;
a restored/reconnected run can fall back to the Dungeon1 title when its temporary
group name is unavailable. Existing dungeon startup/rollback/inventory handoff owns entry.

HUD synchronization visits only known participants/invitees and tracked active
players at the existing lobby poll interval; identical snapshots are not resent.
At most six roster members are sent, with no LFG candidate list in the HUD.
No new runtime dependency, world scan, thread or per-tick network stream.
Access policy, class restrictions and teleport checks remain server-authoritative.
Spawner/door/key/vendor/advancement/authored-chest content logic is unchanged.

## Validation

Java21 compilation and native tests passed:117 JUnit tests, zero failures/errors/skips.
Ten new tests cover maximum ranges/privilege/revision, pending capacity changes,
personal unready and queued countdown cancellation, preparation locking, dimension
boundaries, GUI bounds with/without the recipe book and HUD Action/View codec round trips.
Existing solo/minimum fixtures were updated to the approved maximum semantics;
legacy config readability, class confirmation, queue ownership, invite tokens,
expiration, group identity, roster locking and six-slot schematic checks remain.
d1OfflineChecks passed, including config round trips and adjacent lifecycle/commerce
regressions. Full build, JSON/diff and deployment/feed receipts are in the local handoff.
No datagen applies: Java UI/runtime and documentation/config comments only.
No destructive clean or dedicated/GameTest launch. Gameplay acceptance remains unperformed.

## Pending licensed gameplay testing

1. At Tamsin create a group, set maximum6 and start with fewer members; repeat solo.
   Verify only the leader changes capacity, it cannot shrink below the roster,
   and invitations cannot exceed the selected maximum.
2. Begin a ready check. Click Ready and confirm Tamsin closes. Walk beyond selector
   range in spawn; verify readiness remains. Toggle Not Ready in inventory:
   teammates remain ready and a countdown cancels. Re-ready, then have the leader
   explicitly start at Tamsin. Check disconnect, dimension/class change and stale clicks.
3. Invite a player while their menu is closed and while inventory is already open.
   Verify inviter/Dungeon1 HUD and Join Group, expiry/disband cleanup, then grouped
   HUD/controls through inventory open/close, GUI scales320/426/640, recipe-book
   open/closed and currency display. No buttons on world HUD or unrelated containers.
   Verify read-only active-run HUD, no late join, no class switch and no lobby leave
   shortcut around the established forfeit flow.

Earlier batches' cumulative gameplay testing remains pending. These are testing
tasks, not questions or requests to reconfirm accepted decisions.

## Exact files

- `src/main/java/net/goui/cosmicdungeon/Config.java`
- `src/main/java/net/goui/cosmicdungeon/block/custom/ClassSelectorEntryService.java`
- `src/main/java/net/goui/cosmicdungeon/block/entity/ClassSelectorBlockEntity.java`
- `src/main/java/net/goui/cosmicdungeon/client/ModNetworkClient.java`
- `src/main/java/net/goui/cosmicdungeon/client/economy/CurrencyBalanceOverlay.java`
- `src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyHud.java`
- `src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyHudLayout.java`
- `src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyPanel.java`
- `src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyPresentation.java`
- `src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java`
- `src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyLobby.java`
- `src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyService.java`
- `src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyHudService.java`
- `src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyRules.java`
- `src/test/java/net/goui/cosmicdungeon/client/screen/D1PartyPresentationTest.java`
- `src/test/java/net/goui/cosmicdungeon/client/screen/D1PartyHudLayoutTest.java`
- `src/test/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyChecks.java`
- `src/test/java/net/goui/cosmicdungeon/npc/tamsin/NamedPartyRecruitmentTest.java`
- `src/test/java/net/goui/cosmicdungeon/npc/tamsin/SoloPartyMinimumTest.java`
- `src/test/java/net/goui/cosmicdungeon/npc/tamsin/MobilePartyReadinessTest.java`
- `docs/NPCs/Tamsin_Vane.md`
- `docs/ai/TESTING_ROLLOUT_20261003.md`
- `docs/ai/D1_REMAINING.md`
- `docs/ai/TESTING_BATCH_5_20261003.md`
- `docs/ai/tasks/testing-fixes-batch5-20261003.md`
- `docs/releases/fragments/testing-fixes-batch5-20261003.md`
- `docs/config-examples/CosmicDungeon.config`

## Next improvement

Batch6 adds the approved per-instance difficulty factors and cumulative completion tiers.
