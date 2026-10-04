# Dungeon forfeit vote ? 2026-09-25

Authority: Cameron requests an entry message, /ff voting, at least two-thirds agreement,
failed dungeon outcome, loss of dungeon loot, and restoration of pre-entry belongings.

Branch: feature/dungeon-forfeit-20260925, based on e92e4adf.
Live Google authorization refresh failed; no current canon refresh is claimed.
No broader canon reconciliation or disconnect fix is part of this feature.

## Behavior

- Successful dungeon entry sends: type /ff to forfeit the dungeon instance.
- /ff opens a 60-second ballot or privately reopens its choices. Opening does not vote.
- Click Yes/No, or type /ff yes or /ff no. Each member casts one vote.
- Required Yes votes for rosters of 1?6 are 1, 2, 2, 3, 4, 4.
- Offline members on the run roster count. Membership changes cancel the ballot;
  a new ballot uses the new roster. Outsiders, duplicate votes, expired ballots and
  old clickable ballot IDs cannot contribute votes. Ballots are isolated per server/run.
- Passing calls the existing ABANDONED failure path for this leased instance only.
  Pending transaction/outcome recovery remains authoritative; no success reward is granted.
- Existing inventory handoffs restore the outside snapshot and discard dungeon inventory.
  If Farrow's Chop has switched the player outside, the existing separated outside
  inventory is preserved; outside-world changes are not rolled back.
- Offline owners retain durable recovery decisions for reconnect. Living owners are
  evacuated through the existing safe destination. A dead owner must respawn for cleanup.
- A vote rejected, expired, cancelled, or unable to start safe cleanup makes no inventory changes.
- Votes are temporary and clear at server stop; accepted failure uses existing durable run state.
- /world reset is template maintenance and is not a player forfeit command.

## Scope and integration ownership

New files: DungeonForfeitBallot.java, DungeonForfeitService.java, ForfeitCommand.java,
DungeonForfeitTest.java, this task card and the matching release fragment.

Existing files: D1Command.java (command registration), DungeonLifecycleEvents.java
(once-per-second vote expiry and server-stop cleanup), ClassSelectorEntryService.java
(notice after verified startup), DungeonLifecycleService.java (guarded failure entry point).

Exclusive hotspots owned: command registration; lifecycle/reset entry point;
verified selector-entry notification. No other task owns these surfaces in this pass.

No IDs, saved-data schemas, NBT keys, network payloads, protocol version, config values,
authored inventories, spawner data or registry definitions change. No migration.
No generated assets/data change; client/server datagen is not applicable.
The shared jar uses only common/server APIs. No new dependency or heap requirement.
Expiry scans only active ballots once per second; no entity/chunk scan or tick disk I/O.
Inventory cleanup's existing verified saves and world-reset costs are unchanged.

## Regression boundaries

Access: any actual active member may vote; no Developer rank or leader privilege required.
Classes, spawners, doors/keys, vendors, trades and currency rules are not changed.
Inventory/transaction and Watson guards remain authoritative before failure cleanup.
Existing safe teleport, protected-item recovery, companion preservation, temporary
progression cleanup and offline handoff are reused. No achievement is granted by voting.

## Validation and handoff

Passed: Java 21 Gradle build; 42 JUnit methods (8 new), including 683 native
inventory-handoff checks; all 1,997 source JSON files parse; git diff --check.
Reviewed the four integration diffs: only 15 added lines in existing Java files.
The initial candidate main JAR SHA256 is
fcca71fd9946a85ec2cc70bc7915a04789c0d655ea5837784fe2d92b90f6cfd1.
Final commit/build provenance and deployment receipts live in the external task cache.
Native dedicated/integrated gameplay and multi-client testing remain unperformed.
No GameTest server launch is authorized. Preserve the unrelated staged historical JAR
deletion and generated caches; do not run destructive clean.
No server/client swap until Cameron confirms TEST stopped and the installed client closed.

Manual QA:
1. Enter solo carrying recognizable outside items; confirm the entry notice. Acquire
   dungeon loot, use /ff, then No: stay in the dungeon with unchanged inventory.
2. Open another vote and Yes: failed exit, original outside gear restored, dungeon loot
   gone, no completion reward. Repeat after death/respawn and with a carried cursor stack.
3. With 3 members, one Yes must not finish; two Yes must fail only their instance.
4. Repeat with an offline member; reconnect after acceptance and verify original items
   exactly once. Test expiry, old buttons, repeat votes, and a second simultaneous run.
5. Exercise Farrow's Chop outside-inventory separation and pending custody recovery.

This requested feature has zero further implementation batches once validation completes.
Licensed multiplayer inventory/persistence acceptance remains required.
Possible follow-up: a configurable ballot duration, if Cameron wants one.
