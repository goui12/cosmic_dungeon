# Testing rollout — 2026-10-03

Cameron has supplied the behavior decisions. Work proceeds in batches of 2–3 changes.
The skill balance table is delegated for implementation without a further balance approval.
Use the offline Docs repository for class weapons; do not require live Google authorization.

## Numbered implementation batches

1. **Implemented; gameplay QA pending:** attuned ownership/death drops; kill credit/group split; Bogatyr wolves.
2. **Implemented; gameplay QA pending:** Trade ordering; hidden advancement descriptions; death-screen forfeit.
3. **Implemented; gameplay QA pending:** Developer-only spawner equipment; fixed placement/blocked indicator/radius removal; isolated wave aggro.
4. **Implemented; gameplay QA pending:** Tamsin create/name/leave groups; LFG and member invitations; move player class selection to Tamsin.
5. **Implemented; gameplay QA pending:** Leader maximum capacity; mobile ready check/group HUD; invitation HUD and inventory-only controls.
6. **Implemented; gameplay QA pending:** Per-instance difficulty with individual modifiers; cumulative completion advancement tiers.
7. **Implemented; gameplay QA pending:** Class weapon skill persistence/balance; damage and class bonuses; meaningful potion-use progression.
8. **Implemented; gameplay QA pending:** Mercenary hire/payment/roster slots; own starter room and actual chest equipment.
9. **Implemented; gameplay QA pending:** Mercenary follow/teleport and ally-safe combat; chest/ingredient collection and automatic brewing; timed fallback healing.
10. Mercenary HUD stacking; equipment-preserving no-drop death and ten-minute respawn.
11. Server-wide lifetime statistics; teal pause-menu button and scrollable selectable leaderboards.

Two implementation batches remain after batch 9; cumulative licensed gameplay QA is separate.
Pending gameplay checks are testing tasks, not questions or requests to reconfirm decisions.
The order may be adjusted for a concrete dependency without expanding a batch's scope.
Complete the current batch and hand off; the numbered queue is not permission to skip the
2–3-change limit or a subsequent requested stop.

## Accepted behavior

- Attuned gear: first eligible matching-class collector owns it forever. Droppable and lost
  onto the death block; only original owner collects. Bindings persist and never change owners.
- Actual player/companion kills pay immediately; last damaging player gets environmental kill
  credit, but environmental finishing damage grants no weapon kill XP.
- Skills persist through death/forfeit, separately by class; switching classes restores that
  class's own saved progress. Weapon map comes from offline class documentation.
- Trade: Their Offer, My Offer, My Balance, My Inventory. Preserve the liked presentation.
- Advancement descriptions: ???????? until earned on every presentation surface.
- Death-screen forfeit reuses the normal vote and restores dead members alive outside with
  pre-entry belongings; no respawn loop.
- Spawner equipment configuration is developers only. Spawn top then N,W,S,E,NE,NW,SW,SE.
  No block intersection; entity overlap allowed. Fixed one block, red while blocked, resumes
  when clear. Preserve existing spawner/preset data automatically.
- New waves wait near their spawner; approaching can activate them. Old fighting mobs cannot
  globally wake fresh reinforcements. All living spawned mobs count against the cap.
- Bogatyr egg wolves tame automatically; no attacks on Dungeoneers, villagers, friendly NPCs,
  mercenaries or pets.
- Tamsin: named groups, create/leave, LFG class advertising, invitations by all group members.
  Non-LFG friends can be invited. Only grouped players see recruiting/invite controls.
  Leader's selected size is a maximum, not a minimum.
- No dungeon joining or class changes during active runs.
- Ready closes Tamsin. Players may explore the spawn funzone until leader starts. Top-left
  group HUD remains during inventory; Ready/Not Ready and Leave Group buttons appear only
  with inventory open. Invitation HUD exposes Join Group while inventory is open.
- Difficulty is instance-local, not a server.properties change. Individual tunable factors:
  Easy 0.5, Hard 1, Insane 1.5, Ridiculous 2. Higher tiers shorten spawn delay; bosses stay
  single-spawn; loot quantities/drop chances unchanged. Easy shortens negative effects;
  Insane 0.1% blindness for 5 s per damage event; Ridiculous armor wear +10%.
  Winning also grants lower difficulty completion tiers.
- One mercenary per hirer, one party slot, configurable 500 Trace charged on successful start.
  Own starter slot/chest; equip actual authored contents. Follow hirer; support allies ~8 blocks.
- Latest mercenary decision supersedes the earlier timer-only/no-brewing proposal:
  collect brewing supplies and dropped items, access permitted chests and brew combinations
  from available ingredients. This does not authorize stealing another player's bound items
  or protected personal slot chest. A timed fallback produces healing potions every few minutes.
  Individual potion/production timers remain configurable.
- Mercenary never drops anything on death, retains equipment, respawns after 10 minutes.
  It may teleport when stuck/separated. Stacked top-left health/name/countdown HUD.
- Leaderboard retains server-wide activity including failed/forfeited runs, vanilla stats and
  new custom counters. Preserve known totals; never invent historical events not recorded.
- Agreed-fix pipeline: validate/build, scoped commit and push, stopped TEST/closed installed-client
  verified replacement, current-test publication and dad's updater verification. No restart.

## Balance reference

[Offline-source skill balance draft](CLASS_SKILL_BALANCE_20261003.md) defines delegated defaults.
Drafting this table does not mean the skill runtime is present in batch 1.
