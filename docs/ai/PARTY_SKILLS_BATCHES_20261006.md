# New batch round - decisions updated 2026-10-06

Status: Batches10-19 are implemented, validated and published; Batch19 runtime1.6.9-alpha.1 is installed on TEST.
All original and follow-up questions are settled. Cameron resumed for Batch20 on2026-10-07; stop after20. Set C continues with1.6.10-alpha.1.
Batch10 merged through PR227: main e57b27bb78e592fca521e05bc78e305f23c55c2f; exact1.6.0-beta published.
Batch11 starts from that merged baseline on feature/party-trade-readiness-20261006; validated/published1.6.1-alpha.1. TEST deployment backlog was resolved before14; current delivery is CurseForge plus TEST sFTP; local client delivery is not a batch gate.
Source: Cameron's original request, reviewed wolfpack mockup, and October6 08:16PDT clarification.
This queue records requirements; implementation status is maintained in task reports and D1_REMAINING.
Google source metadata refresh attempted2026-10-06T15:34:03Z: existing read-only authorization expired. Cached Tamsin/TamsinTax plus Cameron explicit settled corrections used; no fresh semantic audit claimed.
All earlier questions below the latest DECISIONS checkpoint are historical; do not re-ask settled decisions.

## Numbering and release sequence
Queued Batch10: Tamsin20x18 top-right lore/map replay i; preserve actual class/group/ready/progress.
Support exact1.6.0-beta, validate, merge completed prior set via PR and publish both jars.
Original round had16 batches11-26 plus queued10. Batches10-19 completed, including Set B Beta and the generic Skills foundation. Batch20 implements class-resource balances/recycling/bars/tags. After its release/deployment,6 remain21-26. Stop after20.
The independent generic Skills UI warrants its own Batch19; prior19-25 become20-26.
SetA11-16 party/combat/HUD/death recovery; SetB17-18 mercenary roles; SetC19-26 UI/resources/classes/wolves.
Alpha micro revisions between implementation batches; validated set main merge/Beta after each set.
Do not reuse tags, preassign conflicting versions, merge test-builds or claim companion app verification without evidence.
No automatic game/client/GameTest-server launch or restart. Current delivery is CurseForge plus stopped TEST sFTP only; no local-client copy or post-publication hashes.
No unresolved gameplay questions remain. Batch16 and validated Set A Beta completed; Approach A remains narrowly approved.

## Confirmed common rules
- Recycling is1 tagged item=1 resource. Consume only headroom to600; never waste donor/self items at cap.
- Companionship is1 heart per5sec per protected player, never multiplied by number of guarding wolves.
- Inventory recovery only reorders actually recovered items; no recreation, overwritten new contents or old-death snapshot recovery.
- Universal group HUD and universal inventory Requests column, anchored right of inventory.
- Scrollable content supports mouse wheel AND a visible clickable/draggable scrollbar; event ownership prevents scrolling multiple panes.
- Generic reusable inventory Skills panel changes content with class; Bogatyr wolf controls only appear for Bogatyr.
- Skills panel independently draggable/movable with saved client placement and a minimizable header.
- Minecraft Menu/Settings/Cosmic Dungeon has exact action "Reset Skill Panel UI" restoring default visible placement.
- Remove recipe book entirely from its GUI surfaces; preserve underlying crafting authorization/recipes.
- Default resource presentation aligns with above-XP world bar; inventory Skills panel can then be moved independently.
- Faint red spawner visual respects occluding walls; preserve authored spawner data.
- Fireworks retain obstruction/access/protection rules and break only cosmic spawners within5blocks; no other terrain.
- Player wolves remain uncapped initially. If measured resource cost warrants it, Cameron authorizes a30-wolf cap.
  This conditional authorization replaces older absolute never-cap guidance. Do not impose cap blindly or delete existing excess pets.
- Expensive wolf target/strategy decisions should be around1/sec, staggered/shared by owner; cheap boundary checks and explicit mode switches stay responsive.
  This does not mean stopping ordinary movement/attacks for a second or sending client/server polling for every wolf.
- Regroup is ONLY living currently loaded owned wolves in the current dungeon. No dead wolf revival, archive recovery,
  cross-dimension retrieval or chunk-loading search. Do not confuse unloaded wolves with dead wolves.
- Do not remove ordinary Minecraft save/load or erase existing companion records based on this Regroup scope.
  Cameron explicitly ends packs with the dungeon; Batch23 removes cross-run preservation with safe old-data handling.

## Set A - party and combat reliability
11. Trade and readiness
CORRECTION: There is no timer-removal task. Opening/using/closing/cancelling/completing a trade must not unready the party.
Preserve Ready state while awaiting Start Adventure. A validated leader Start cancels/refunds unfinished trades before entry.
Keep mutual trade acceptance and existing transaction safety. Resolve final-confirm/start races without duplicate loss/gain.
Remove active-dungeon readiness text in every HUD form; mercenaries never display ready/not-ready. Show random name + class.
Expected: TradeSessionData/TradeCustody/TradeTransactions, InventoryTransactionGuard, Tamsin D1PartyService/Lobby/HudService,
ClassSelectorReadyManager/call sites, client D1PartyHud/MercenaryHudLayout and related tests/payload if needed.
Hotspots: trade/currency transaction boundaries, party readiness/entry, menu sessions/network.
Tests: both-ready trade through completion/cancel; start during open/accepted/finalizing trade; full inventory returns;
currency/drop custody, disconnect/death/class-change invalidations still work; roster/leader/membership remain authoritative.
12. Player wolf friendly-fire protection (complete;1.6.2-alpha.1)
Protect player-owned Bogatyr wolves and mercenary wolves against allied melee/projectiles/AOE/harmful effects.
Preserve hostile damage/healing. Source gap verified: MercenaryWolves.protectedCompanion covers mercenary entity/marked wolves;
BogatyrThreats prevents player-wolf outbound friendly damage, but lacks equivalent inbound guard.
Expected: CompanionAllies/BogatyrThreats/BogatyrWolfEvents and mercenary damage/projectile/effect guards.
No30cap change in this narrow bug-fix batch; performance/cap belongs to later command/AI work.
13. Cosmic spawner visual, pickaxe warning and firework destruction (complete;1.6.3-alpha.1)
Faint wall-occluded red appearance when blocked, no red through-wall silhouette/fill.
Large throttled "You need a pickaxe to break that!" on wrong-tool attempt.
Both player/mercenary Pyroclast fireworks break eligible cosmic spawners within5blocks, respecting existing obstruction/protection.
Expected: CosmicSpawnerRenderer/CosmicMobSpawnerBlock and firework explosion services; preserve access/reset/drop lifecycle.
No planned NBT/preset/schema change, no planned spawner migration; authored placements/presets must survive updates.
14. Complete group HUD (complete;1.6.4-alpha.1)
All human members including self plus mercenaries: current/maxHP, name/class, small positive/negative effect icons under HP.
Explicit dead/offline/unloaded states; no stale health presented as current. World view read-only, inventory supports mouse.
Expected: D1PartyHud/Layout/MercenaryHudLayout, D1PartyHudService, PartyPayloads, ModNetwork.
Bounded delta synchronization and appropriate protocol update; GUI scales/window sizes/long effect lists.
15. Member run-stat inspection
Inventory HUD human row opens current-run name/class/HP/ability cooldowns/effects.
Proposed useful additional fields: resources and run kills/damage/healing/deaths; do not substitute lifetime counters.
Reuse class ability state and existing run data, authorized same-party detail responses, reset on next run.
Expected: client detail UI, party snapshot/detail packets, ability readers, minimal run counters if required.
16. Death inventory organization
Record latest real uncancelled death layout including inventory/hotbar/armor/offhand. Replace prior snapshot on each new death.
Reorder only actual recovered items on resurrection and return/pickup at latest death location.
Missing/stolen/destroyed items remain missing; never recreate or reclaim another player's items.
Preserve occupied slots/new items, handle partial/merged stacks/components, equipment re-equip only valid recovered contents.
Old-death drops remain physical items normally, but new death replaces automatic recovery/layout history.
Expected: native death/respawn/pickup, inventory handoffs/transaction guard, run store and latest-death token.
Tests: cancelled/double deaths, resurrection race, partial pickup, occupied slots, reload/forfeit/trade interactions.
Validated Beta checkpoint A after16.

## Set B - mercenary roles
17. Theurgist/Venefex potion role split
Theurgist mercenary positive-only; remove its negative behavior/visible skill. Add negative-only Venefex mercenary.
Venefex confirmed cumulative successes L1=1,L2=3,L3=6,L4=10, then triangular L*(L+1)/2.
This means0 before first successful potion; adjust new-profile presentation without globally shifting existing mercenary skills.
No XP for misses/immune/non-effective effects, one successful cast event rather than unlimited multi-target credit.
Retain material-free mercenary crafting, friendly protection, skill cadence/quality scaling.
Theurgist MERCENARY resurrection remains Positive Potions10,3-minute cooldown, native death-menu acceptance.
Preserve legacy save IDs/counters compatibly; do not convert hired identities or discard unknown fields.
Expected: MercenarySkill/Skills/Potions/Casting/Balance/Credit/Brewing and profiles/HUD/tests.
18. Judicator mercenary
Confirmed cumulative kill totals1,3,6,12,24 (subsequent doubling). Combat0 before first credited hostile kill.
Each successful DIRECT attack rolls L% chance (probability cannot exceed100%) for L HP damage to hostiles within2blocks.
Level4:4% for4HP/two hearts. Secondary AOE cannot recursively proc itself; ordinary valid kill credit still applies.
Exclude all allies; preserve death levels and new-run reset. No class-chest equipment edits.
Expected: mercenary profile/combat/progression, bounded area effects and tests.
Validated Beta checkpoint B after18.

## Set C - shared Skills UI, resources, requests and class gameplay
19. Generic Skills panel and shared inventory layout (new separate foundation)
Reusable per-class panel; independently draggable with stored client layout, minimizing keeps header/resource display.
Initial class content slots for Bogatyr/Theurgist; no invented abilities for other classes.
Equal-sized action buttons; support additional dead-player buttons and overflow through wheel + visible draggable scrollbar.
Default visible position, viewport/GUI-scale resize handling, mouse capture/release, no interference with item dragging.
Add Cosmic Dungeon settings "Reset Skill Panel UI" button. Restore default visible panel even after resolution change.
Remove recipe book entirely; coordinate universal group HUD, fixed-right Requests region, class info shortcut/resource panels.
Class-help blue square/white i opens exact class H guide. No server gameplay permission may depend on UI visibility/disabled controls.
Expected: inventory/client event integration and layout, new reusable panel component/client settings, existing CosmicDungeon
settings screens, DungeonInventoryRecipeBook/mixins, HelpMenuContent/Screen navigation. Network only if genuinely needed.
Layout-only persistence is client-side; no new world schema.
20. Brewing Supplies/Kibble resource ledger, bars, recycling and datagen
Both start0 each dungeon; regenerate1/sec while online in active run, retain through death/relog, reset next run.
Maximum600; stop accumulation/recycle at cap, no offline catchup. Each resource belongs to its respective player class.
Herb-styled higher-definition Brewing Supplies bar aboveXP in world; inventory Skills content includes current/600 and requested styled hover.
Kibble count updates <=1sec in expanded/minimized panel.
Request Supplies and Recycle tooltips use user's yellow/blue-bold resource wording; inputs strictly tagged items.
Datagen brewing tag EXACT: sugar,rabbit_foot,glistering_melon_slice,spider_eye,blaze_powder,golden_carrot,ghast_tear,
pufferfish,magma_cream,turtle_helmet,phantom_membrane,breeze_rod,stone,cobweb,fermented_spider_eye,slime_block.
Kibble tag EXACT: rotten_flesh,beef,porkchop,mutton,chicken,rabbit. NO fish/cooked meat.
H guide lists exact convertible supplies from authoritative tags; runServerData and inspect generated diff.
Expected: class-resource service/storage, generated item tags/provider, resource bars/panel, HelpMenuContent, payload/ModNetwork,
transaction guards. Additive old-save defaults and round-trip/reset tests.
21. Group supply requests
Universal Requests column on inventory right. Multiple boxed cards, wheel + clickable draggable slider,
Accept/Deny per card plus Accept All/Deny All. Hover requester name/class, item icons/counts and exact credited supply yield.
Server checks actual same-run/group/class/life/resource cap and donor inventory at execution, safe with trade/death/logout.
Consume eligible items only after consent; cap-limited no-waste transfer, deterministic bulk processing, no stale/double charges.
Proposed bounded deduplicated request per requester/resource/recipient; unrelated valid requests still coexist.
Expected: requests service/UI/payload, shared inventory layout and existing item transaction/run lifecycle.
22. PLAYER Theurgist potion crafting and resurrection
Craft a potion [20 brewing supplies]: random positive tierI splash potion.
Craft an epic potion [40 brewing supplies]: random positive tierII splash potion.
All skill-produced potions are physical splash potions; skill crafting spends resource only, no stand/bottle/fuel requirement.
Full inventory/failed item delivery must not lose supplies. Normal pool: night vision,invisibility,fire resistance,swiftness,healing,regeneration,strength,luck.
Epic pool: only actual tierII variants from that list (swiftness,healing,regeneration,strength).
Positive potions thrown by a Theurgist must not affect hostile mobs, including heal/harm inversion on undead.
Allow beneficial effects on valid allies/ally pets/mercenaries; preserve other classes' intended behavior.
Theurgist creates no negative potions through skills, but CAN manually brew negative potions at a brewing stand.
Player resurrection has NO cooldown,120 Brewing Supplies, anywhere in same active dungeon, living Theurgist.
On teammate death notify living Theurgist to open inventory. Add one "Resurrect <player>" button per dead teammate.
If nobody needs resurrection show disabled "Resurrect..." placeholder; insufficient120 also disables/rejects the action.
Theurgist clicking creates offer in dead player's death menu; acceptance uses latest-death position/token only.
Latest death supersedes old offer and old layout; normal respawn/forfeit/cleanup invalidates offer.
Implementation default: debit120 exactly once on successful accepted resurrection, revalidate caster alive/resource/current run;
declined/stale/failed offers cost nothing. Prevent duplicate competing caster/mercenary offers and double native respawn.
No additional player skill-level gate was requested; available as Theurgist class action once affordability/eligibility hold.
Reuse native respawn/invincibility/death-token foundations and do not alter the separate mercenary cooldown/unlock.
Expected: player Theurgist service/potion source attribution, Skills content, death notifications/offers, death UI,
shared latest-death recovery/transaction/resource ledger, payload/network and tests.
23. Bogatyr Skills content and paid wolf commands
Bogatyr-only Wolfpack content reuses movable generic Skills panel; minimize keeps title/Kibble. All10 buttons equal size.
Breed5/eligible adult into love mode, partial affordable including1; real pathing/mating, own pups tamed, cooldown/juvenile/no-op safeguards.
Summon30/tamed wolf, disabled below30 or applicable measured30cap; no cost on failed placement.
Regroup1/living loaded wolf, require affordability for entire affected pack; same current dungeon, safe teleports, no revive/archive/chunk loads.
Heal5/injured living loaded wolf, partial affordable, disabled below5/no injured wolves; default full heal and lowest health first.
All exact affected counts/costs previewed and server-revalidated. No charges for failed/noneligible actions.
RUN-ONLY lifecycle: pack ends with dungeon completion/forfeit/reset; remove legacy cross-run archive preservation/retrieval.
Do not discard normal active-run save/load; unloaded ended-run wolves must be rejected/removed on later load.
Prevent reset blockers from retired archives, safely retire old ended-run records without duplicate delivery, preserve active-run wolves until cleanup.
Expected: current loaded ownership roster, Bogatyr care/commands, archive/reset lifecycle and compatible cleanup,
Skills UI/resources, minimal payload hooks. This replaces prior cross-dungeon pet preservation.
Conditional30cap if needed must limit new admissions/births without deleting existing excess wolves; keep mercenary counts independent.
24. Core wolf modes
Defensive: protect master; react when wolves/master attacked.
Stand Ground: immediate sit/stop path/drop target+retaliation, remain sitting even attacked; new mode stands up.
Aggressive: attack hostiles nearest master and work outward.
Exclusive selected border and immediate command response; expensive target/path decisions bounded/staggered around1sec.
Expected: BogatyrThreats/goal controls/behavior rules, owner mode state and panel payload.
25. Advanced wolf modes
Strategic: ranged first, then descending maxHP, deterministic ties.
Search and Rescue: allies<=3 hearts, interpose between attackers and player, prioritize actual attackers/redirect aggro.
Companionship1heart/5sec once per protected player, not per dog; use existing effect identity appropriately.
Danger Close: moving16block radius around master, constrain paths and stop/return if master moves outside boundary.
Within allowed boundary finish current kill before assisting; then prioritize threats to master.
Cheap leash/mode responsiveness separate from slower expensive decisions; no per-wolf world scans or packet polling.
Compare baseline/large-pack server cost; conditional cap30 already authorized if optimization alone inadequate.
26. Integration and final set Beta
Co-op tests covering all class panels, drag/reset/scroll/GUI scales, requests/crafting/cap races, death inventory and resurrect offers,
large packs/mode transitions/friendly fire, latest-death/reset/relog/old save compatibility.
Complete validated PR main merge and matching CurseForge runtime/helper Beta; independently eligible target deployment and feed receipts.
Licensed full beta gameplay verification remains required before stable.

## Final answers recorded
A. Packs end with the dungeon. No surviving-wolf preservation across resets; implement compatible lifecycle cleanup in23.
B. Normal splash pool: night vision,invisibility,fire resistance,swiftness,healing,regeneration,strength,luck.
Epic skill selects only real tierII variants from that list. No unresolved potion-pool question.
Cameron explicitly authorized Batch13 on2026-10-06. Stop after13; prior conditional continuation is superseded.

## Automated/manual verification and planning receipt
All actual code batches follow AGENTS: narrow task/PR, exclusive hotspot ownership, Java21 build, CI clean build/native GameTests,
relevant JSON/diff checks, server/client datagen only when required, review comments and exact release provenance.
UI manual QA remains distinct from compilation; dedicated-server source must not import client-only panel code.
Resource/request/resurrection tests: cap/concurrency/duplicates/death/disconnect/forfeit/latest death, no lost or generated items.
Wolf tests: loaded-only regroup, dead exclusion, hostile/friendly effects, stand-ground under attack, mode change response and budget.
This is the approved implementation queue. Each task records its actual validation and rollout separately.
