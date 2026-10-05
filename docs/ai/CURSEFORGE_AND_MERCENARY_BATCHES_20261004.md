# Release and mercenary rollout, 2026-10-04

Stage 1 and Batches 1-6 are authorized. Batches 1-5 are validated, published and installed; manual gameplay QA remains. Batch 6 implements Bogatyr scaling and the publication approval gate; validation and delivery follow. Batches 7-9 are planned, not authorized. Earlier D1 sets retain their cumulative licensed multiplayer QA.

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

## 7. Pyroclast fireworks

- Player and mercenary fireworks deal very high damage exclusively to enemies: never
  players/allies, allied wolves, mercenaries or other friendly entities.
- Mercenaries prioritize fireworks for groups of enemies. Replenish one every 30 seconds,
  initially cap at five. Fireworks levels shorten restock time and every other level adds
  one capacity. Define bounded cadences/damage based on existing combat before implementation.

## 8. Dragoon chain lightning

- Initial trigger: 50 successful hits. Chain hits also count toward the next trigger.
- One successful chain cast reaches level 2, two further casts level 3, three further
  casts level 4; level 5 totals ten casts. Each level reduces the hit threshold.
- Count secondary hits without allowing recursive same-tick unbounded chains.

## 9. Theurgist resurrection

- At level 50, a living mercenary can resurrect a dead dungeoneer through the death menu:
  "Accept Resurrection from <Name>". Determine which of the Theurgist skill levels governs
  the threshold before this batch; do not silently infer combined/positive/negative level.
- Reappear at the exact death location with five seconds of invincibility. Reuse death
  inventory/forfeit state; no duplicate drops, inventory or race with normal respawn.
- Mercenary resurrection cooldown is three minutes and appears in its group HUD hover
  once unlocked. Validate alive/same group/same instance/cooldown server-side on acceptance.

## Release acceptance

After these nine batches: verify multiplayer payments/donations, friendly fire and potion
targeting, growth/reset/reload, wolf ownership, death-menu races, performance and both
CurseForge client updates. Merge the completed source set into main and publish beta.
Stable follows full beta acceptance. Three later gameplay batches remain (7-9); Batch 6 delivery and cumulative manual QA are pending.
