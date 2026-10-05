# Release and mercenary rollout, 2026-10-04

Stage 1 and Batches 1-9 are authorized. Batches 1-8 are validated, published and installed;
manual gameplay QA remains. Batch 9 implements Theurgist resurrection at Positive Potions
level 10 (Cameron's explicit 2026-10-05 decision). Batch 10 is queued for Tamsin lore/map
replay and the completed-set **1.6.0-beta** release. Stop after Batch 9 until Batch 10
implementation is requested. Earlier D1 sets retain their cumulative licensed multiplayer QA.

## Stage 1: public 1.5.2 Beta and publishing

- Use project 1326805, current development source, Minecraft 1.21.10/NeoForge 21.10.64/Java 21.
- Make mod_version authoritative, increment patch for every new distributed test update,
  use alpha for individual batches, beta after sets, and stable after full beta testing.
- Update AGENTS workflow: merge completed source sets into main via PR; publish versioned
  client builds to CurseForge with both runtime and loading-screen artifacts retained.
- Token is kept in the existing Windows DPAPI store and GitHub secret CURSEFORGE_API_TOKEN.
- Loading helper needs a separate project ID for CurseForge app updates. Additional-file
  archiving alone does not satisfy client updater migration. Preserve the transition feed
  until companion installation, theme activation and updates are verified.
- No gameplay, network/save format, authored chest, registry or world changes in this stage.

## 1. Menus and identities (implemented; manual QA pending, 1.5.3-alpha.1)

- Leader's Tamsin menu stays open after Ready so Start Adventure is immediately available.
- Random mercenary skins and approximately 100 names with a seventeenth-century English
  feel; each complete displayed name is at most nine characters.
- Group HUD uses only the random name, omitting both owner and the word mercenary.

## 2. Wolves and equipment drops (implemented; 20 CI GameTests passed, installed, manual QA pending, 1.5.4-alpha.1)

- Dungeoneer Bogatyr has no wolf cap: its wolfpack must never report full.
- Separate player wolf ownership/counting from mercenary ownership/counting.
- Allow players to equip mercenary wolves with wolf armor.
- Keep naturally generated mob armor but independently control its drop chance. Preserve
  authored spawner loot and deliberately equipped/player-given items when defining scope.

## 3. Mercenary recovery and transactions (implemented; installed, 1.5.5-alpha.1)

- Hiring costs 50 Trace. A dead mercenary gets an inventory Revive (25 Trace) button to
  bypass the countdown. Server revalidates ownership/death state/payment atomically.
- Insufficient funds: "You do not have the funds to revive the mercenary. Would you like
  to ask the group for help?" Yes sends clickable group chat donation for that named
  mercenary. Prevent double charges/revives and stale requests.
- After 15 seconds out of combat regenerate 0.5 health points per second until full.

## 4. Shared mercenary skills (implemented foundation, 1.5.6-alpha.1)

- Start at level 1; next level needs current-level additional successful events: 1,2,3,...
  Level 5 requires 10 total successes. Failed/immune effects do not count.
- Group chat announces level-ups; inventory group HUD hover shows class-specific skills.
- Levels survive mercenary death/revival, but reset with the next dungeon group instance
  after completion or forfeit. Reuse existing run identity, persistence and lifecycle.
- Reuse D1RunData counters keyed by run, contract and skill, including unloaded/resting HUD rows.
- Current success hooks: actual wolf creation; successful splash/cloud healing, useful positive
  buff changes, and immediate negative potion damage. Each potion/cloud contributes at most
  one success per category across targets/effects. No XP for immune/failed/unchanged effects.
- Batches 5-9 add the remaining class behavior and success hooks, including damage-over-time
  attribution in the potion rewrite, firework detonation and completed lightning casts.
  Fireworks/Chain Lightning display level 1 until their later mechanics exist.
- No player skill balance change or automatic skill-based combat scaling in this foundation.

## 5. Theurgist potions (implemented, 1.5.7-alpha.1)

- Positive Potions gains success for buffing/healing an ally; Negative Potions for potion
  damage to an enemy. One success reaches level 2; two more reach 3; three more reach 4.
- Brew only splash-effect potions, positive and negative. Brewing stays instant at stands;
  skill levels increase acquisition/brewing cadence and throwing cadence.
- Mercenaries spend no crafting/summoning materials; remove the need to loot brewing
  supplies, bones, wolves and similar supplies. Preserve useful equipment behavior.
- Brief aura flashes when preparing and using a splash potion.
- Out of combat, periodically offer strength, speed and healing to dungeoneers. Tier II
  quality probability grows with Positive Potions, appreciably around 10 and almost
  always around 25; frequency also improves. Prefer effective healing/buffs over waste.
- Goals include positive single-target potions placed/thrown on the ground for players,
  negative splash potions at enemies and positive splash potions at allies. This implementation follows the explicit splash-only
  requirement and aims at the player's feet. Optional clarification received no answer;
  this is a documented interpretation, not a separately approved drinkable feature.

## 6. Bogatyr mercenary skill (implemented, 1.5.8-alpha.1)

- Baseline remains one wolf every two minutes, up to five mercenary wolves.
- Summon interval is ceil(2400 / (1 + 0.06*(level-1))) ticks, with a 400-tick floor.
- Cap is 5 + floor((level-1)/2): six at level 3, seven at 5. Counts include unloaded wolves.
- Commands rotate through five roster entries per existing decision; larger loaded packs
  finish dismissal through their existing two-second validation. No expanded world scans.
- Preserve protection from allied aggression, damage and harmful potion effects.
- Player Bogatyr remains uncapped regardless of mercenary level/count.

## 7. Pyroclast fireworks (implemented, 1.5.9-alpha.1)

- Active player and mercenary Pyroclast rockets damage enemies exclusively, including native
  unnamed rockets. New damage multiplier defaults to 10: Cinderbite/native 120 HP and
  Cindermaul 150 HP at the center before falloff, defense and existing player skill bonuses.
- Preserve existing radius, obstruction checks and candidate budget; no terrain damage.
  Protect every player, mercenary, owned companion, friendly NPC and neutral animal.
- Mercenaries start with five virtual rockets, use no materials, and prefer nearby clusters.
  Maximum firing rate is one per two seconds, sharing the existing attack clock.
- Restock one per 600 active ticks at level 1. Interval ceil(600/(1+.06*(level-1))),
  floor 100 ticks. Capacity 5+floor((level-1)/2): six at 3, seven at 5.
- A burst damaging at least one enemy earns one Fireworks success, independent of target count.
  Existing group announcements, HUD skill tooltips and triangular thresholds apply.
- Optional entity stock/timer data preserves death/revival/reload; legacy saves start with
  five, a new dungeon resets levels/stock. No offline backlog or item/chest rewriting.
- Launch/run and one-detonation stamps prevent stale or repeated rockets from earning or
  regaining vanilla damage. Reuse server authority; no new packet or registry IDs.

## 8. Dragoon chain lightning (implemented, 1.5.11-alpha.1)

- Start at 50 successful damaging hits. Threshold is max(5, 51-level), one fewer per
  level until level 46. Native melee and attributed projectile hits count, including
  lethal hits and absorbed damage; misses, blocked attacks and friendly damage do not.
- Every actual secondary lightning hit adds one to the bank. No recursive event casts:
  existing AI decisions spend at most one charge per 20 active ticks, without offline backlog.
- A cast damaging an enemy earns one skill success regardless of target count: one cast
  reaches level 2, two further casts level 3, three further level 4; level 5 totals ten.
- Reuse configured Dragoon damage multiplier, radius, target/candidate limits, line of sight
  and arc particles. Exclude the last primary victim, allies, pets and neutral animals.
  Chain damage does not compound back into the stored primary-hit power.
- Optional entity charge/power/victim/cooldown fields retain death/revival/reload; legacy
  saves start empty. Existing run skill counters/HUD/chat remain; new dungeon resets both.
- Require a living active contract and online hirer in the same run; cleanup/offline state
  cannot cast. Failed casts earn no XP or hit credit and refund their charge.
- Player Dragoon chance, damage, repair and authored equipment remain unchanged.
  See [Batch 8 report](tasks/dragoon-lightning-20261005.md) for boundaries and manual QA.

## 9. Theurgist resurrection

- At **Positive Potions level 10**, a living Theurgist mercenary can resurrect a dead
  dungeoneer through the death menu: "Accept Resurrection from <Name>". This explicit
  2026-10-05 decision replaces the earlier level-50 requirement; Negative Potions is irrelevant.
- Reappear at the exact death location with five seconds of invincibility. Reuse death
  inventory/forfeit state; no duplicate drops, inventory or race with normal respawn.
- Mercenary resurrection cooldown is three minutes and appears in its group HUD hover
  once unlocked. Validate alive/same group/same instance/cooldown server-side on acceptance.

## 10. Tamsin lore/map replay and 1.6.0-beta (planned)

- Add an "i" button at the top right of the group selector, adjacent to Refresh/Close.
  Match the existing Refresh dimensions: 20x18. Tooltip: replay Tamsin's introduction.
- Replay the same first-time agreement/lore and map presentation using the existing
  TamsinFlow and ClassSelectorScreen. Returning from replay restores the group selector.
  Do not erase accepted onboarding, selected class, readiness, group membership or progress.
- Own selector/Tamsin conversation routing and session-state tests in a separate chained
  task. Expected files: ClassSelectorScreen, ClassSelectorMenu, TamsinFlow, TamsinService,
  related presentation/flow tests; no new artwork, world content or registry IDs.
- Verify first-time and returning players, repeated replay, refresh/close, resized GUI,
  group leader/member sessions, invitations/readiness, and normal Start Adventure.
- Verify the Positive Potions 9/10 boundary, resurrection eligibility, cooldown and HUD
  delivered by Batch 9 as part of beta acceptance.
- Exact release requested: **1.6.0-beta**. Extend the existing version parser, publisher
  tests, and any workflow/AGENTS constraints to accept this form while preserving
  numbered alpha/beta compatibility. Do not distribute another version under the same tag.
- After clean validation and review, merge the completed source set into main through
  validated PRs, then publish matching runtime/loading-screen artifacts to CurseForge.
  Install each eligible stopped TEST / closed ADMIN target and verify both feeds.
  No automatic client launch or server restart.

## Release acceptance

After all ten batches: verify multiplayer payments/donations, friendly fire and potion
targeting, growth/reset/reload, wolf ownership, death-menu races, Tamsin replay, performance
and both CurseForge client updates. Merge the completed source set into main and publish
**1.6.0-beta**. Stable follows full licensed beta acceptance, which is a separate gate.
Two implementation batches remain at this checkpoint: active Batch 9 and planned Batch 10.
