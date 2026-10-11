# Cosmic Dungeon 1.7 Alpha series and 1.8 Beta plan

Approved design baseline: Cameron's October 10, 2026 requests and clarifications, through 20:09 PDT.
Status: Batches1-9 completed; Batch9 1.7.9-alpha.1 published and delivered to stopped TEST. Batch10 IN PROGRESS; Batch11-20 PLANNED. From Batch10 onward, Alpha builds finish at CurseForge under Cameron's October11 instruction; Beta/official builds alone receive sFTP. See current batch reports and exact controller receipts.
The earlier 15-26 controller remains COMPLETE. Initialize a separately identified 1.7 series only after an implementation/start instruction; archive old receipts and never reuse their batch IDs as new evidence.

## Release and completion contract

- Batches 1-19 each produce one published Alpha. Batch1 uses1.7.1-alpha.2 because candidate1 failed native fixture tests before upload; its old tag remains immutable. Batches2-19 use1.7.2-alpha.1 through1.7.19-alpha.1. Batch20 produces1.8.0-beta.1.
- Each card includes implementation, its relevant tests, review, full clean/native CI, a unique source tag, CurseForge runtime plus matching companion, and channel-appropriate completion (Alpha publication only; Beta/official stopped TEST sFTP). No feature is considered implemented merely because its interface appears.
- Fix failures before publishing a card. Never reuse a published version/tag for different bytes. Beta is an audit/release batch, not a place to conceal unfinished features.
- Preserve Java21, server authority, authored chest stacks/loadouts/renamed rockets, existing worlds/spawner IDs and unknown save data. No automated local game/GameTest/client/server launch, restart, or server.properties edits.
- After CurseForge, Alpha builds save compact completion and stop: no sFTP or server checks. Beta/official builds update only TEST through pinned sFTP after current shutdown evidence, then complete. Stop at the authorized boundary. No local client/helper installs or additional post-publication hash/readback loops.
- Use the local canon backup at C:/Users/Cameron/Documents/mod_development/cosmic_dungeon/Google Docs and Sheet/. No Google connector, OAuth or live refresh gate. Read relevant local lore/recipes; preserve recorded provenance and coverage limits.
- Each batch report records requirement IDs, changed surfaces, build/CI/release/TEST evidence, actual manual acceptance versus TESTING, every failed GameTest and disposition, outstanding TODOs, and one useful AI recommendation for that batch (or explicitly none justified). Final Beta consolidates these without erasing failures that later passed.
- Common automated gates do not prove GUI readability or licensed multiplayer behavior. List concrete manual checks and retain their real status.
- Normally one cooperative worker with disjoint writes. The explicitly requested category-art batch may use multiple image workers with separate assets; one coordinator owns integration. Do not change runtime dependencies/resources silently.

## Settled specification

### Inventory, panels and tooltips

Retain a shared layout: avatar/armor/offhand at left, inventory/hotbar central, reclaimed crafting area for class controls, separate movable Skills panel where applicable. Remove the entire2x2 crafting/result/arrow/title/recipe-book presentation and prevent hidden slots receiving input, while retaining native menu/slot identities and existing crafting guards. Preserve class-specific storage such as any existing Metalmancer extra row; do not enable inactive classes.

Every class inventory uses its existing chest emblem and chest's main color, with sparse lore-based background art. Reuse textures/entity/chest/emblem_<class>.png and the existing chest palette; do not alter chest contents or original emblems. Bogatyr evokes a bear-pelt northern-Russian mountain hunter leading wolves; Theurgist brewing in Circe's hut; Venefex extracting snake-fang venom in jungle; Dragoon spearing a water monster with a lightning trident; Judicator follows local judging/lore themes. Read the remaining class lore locally. Text contrast, slot recognition and playability outrank art detail.

Skills empty body is mostly transparent; entries/title remain readable and more solid, border slightly more visible than body. Preserve movement/reset preferences. Theurgist and Venefex use inventory actions and have no separate Skills window after their new controls are available. Class-name hyperlinks open that class's H-guide, underline and turn blue on hover.

All actual item, skill, party and editor/help tooltips render above all open mod panels. Use the topmost hovered control to avoid ghost/multiple tooltips. Party inspection is hover-based rather than requiring a click. Show contribution YES/NO, shared-resource use/contribution and player/mercenary resurrection cooldowns.

### Sharing, crafting and potion stacks

Group creators receive the consent prompt immediately; joiners receive it in Tamsin. YES/NO remains changeable. Tamsin progression controls remain reachable without Back, including recruit/mercenary subpages. Online consenting same-party members may contribute regardless of distance. Revalidate membership and consent when a transaction executes.

Update eligible inventories through change-driven and bounded periodic reconciliation, not unbounded every-tick global scans. When personally affordable, use the crafter's own recipe materials. Otherwise use the eligible shared pool, drawing from least-recently-donated eligible players in that order and combining inventories as needed. Aim for even contributions; scarcity must not require identical ingredients from every donor. Serialize ordering and update donation history only on successful debit.

Notify each donor: "You have contributed a(n) <item_name> to <player_name>'s <skill_name>." Record exact quantities/item IDs, donor, recipient and skill. Tooltip summaries show shared resources used/contributed; retain own use separately so future metrics are interpretable.

Remove old random-potion skills. Theurgist receives positive vanilla-brewable splash recipes; Venefex receives negative vanilla-brewable splash recipes. Both may craft mixed-effect recipes such as Turtle Master as DRINKABLE potions; neither may craft their splash variant. Do not expose mixed lingering variants as a workaround. Existing potions of every type remain tradable regardless of brewing-role restrictions. Luck has no native brewing recipe; do not invent one.

Recipes consume the complete applicable vanilla chain: actual water bottle/base/effect/modifier ingredients and gunpowder for splash outputs. Resolve valid routes and supported intermediate potions from current recipes rather than assuming every potion needs wart. One click yields one potion. Preserve vanilla fuel accounting: one fuel item supplies20 brewing-operation charges, each simulated recipe step consumes one; retain unused charges per crafter across normal save/reload/relog/death boundaries. Fuel transactions and ingredient/output changes commit together. Show a fuel-charge graphic and remaining charges LEFT of the potion tiles; fuel is not a renamed Brewing Supplies resource.

Craft... in the former crafting area expands a potion palette. Aim for two rows; wrap/reflow and snap to the right of the inventory as space changes, using additional rows if needed to keep tiles visible and interactive at supported GUI scales. Show only legal level/duration variants; preserve normal potion tooltips.
- Unavailable: red0, light-red background; clicking does nothing.
- Own materials suffice: green background, count craftable from own inventory.
- Group help needed: normal background, blue count craftable with eligible shared materials.
Counts are alternative recipe capacities, not reservations that promise every tile can be crafted simultaneously. Preserve these tooltip explanations:
"this item can be crafted using materials in your own inventory."
"this item can be crafted using group shared materials in other player's inventories."

Normal click adds exactly one potion to the cursor stack, including repeated crafting of normally nonstackable potions. Shift-click crafts exactly one into inventory, preferring hotbar slots. Allow bounded stack growth with identical full components only; incompatible/full destinations do not consume anything. Correct splitting, dragging, shift movement, throwing/drinking one at a time, bottle returns, trade, drops, death/resurrection, reconnect and save behavior must exist before exposing cursor crafting. No potion hotkeys: crafting requires opening inventory.

Remove Requests entirely and retire Brewing Supplies only when replacement crafting and free resurrection work. Retire Kibble when old Bogatyr abilities are removed. Preserve compatibility/inert legacy data as necessary and settle pending physical custody; never rewrite authored chest stacks or invent item refunds from obsolete virtual balances.

### Resurrection, ownership, trade and wolves

Player Resurrect is available immediately, free, with a180-second per-caster cooldown charged on successful revival. Place Resurrect... in Theurgist inventory, with teammate picker and established accept/decline, latest-death location, protection and recovered-inventory behavior. Cancelled/stale/failed attempts cost no cooldown. Preserve cooldown across death/relog; prevent duplicate/racing revivals. Existing mercenary unlock/rules remain unless a new explicit instruction changes them. Party hover and death screen show each relevant player/mercenary Theurgist's remaining cooldown or readiness.

All players can manually drop gear, including class ammunition, and it drops on death; retain owner-only pickup for manually/death-dropped player gear. Ground drops do not silently transfer ownership. Authorized trades transfer custody appropriately. Trade every inventory item except explicitly class-attuned non-potion items; all potions remain tradable. Remove pricing/class/ability/quest-tag blanket eligibility barriers while retaining transaction, ownership and anti-duplication validation. Offered currency reduces available displayed balance immediately and restores on cancel/close/failure.

Bogatyr has only Patrol and Stay, mutually exclusive; Patrol is the default native-like defense of master/pack. Stay recalls wolves to sit within5 blocks of master, then holds their individual positions even if master moves. Suppress retaliation and clear prior targets/aggro so returning to Patrol does not resume old fights. Allies cannot harm wolves, apply hostile effects or make them sit, but can feed/breed them. Stay temporarily allows breeding, then returns wolves to their held positions/sitting state after success or bounded mate-search failure. Preserve unlimited player packs and end them with the dungeon; do not alter the separate mercenary5-dog rule.

Feed Pack consumes at most10 actual items, healing injured wolves before breeding healthy adults. Prefer bones/rotten flesh, then raw meat, then remaining meat by player food value. Bones are accepted only as ability inputs; ordinary bones/taming/item properties remain unchanged. Preserve authored eggs, normal taming and breeding after removing paid Summon and every old skill/mode. Validate pack ownership, food availability and actual successful consumption.

Every active non-potion skill/action has a rebindable Minecraft Controls entry. Show the player's actual key/chord beside its name in gray and include name, description and hotkey in its tooltip. Respect modal/text input and key conflicts. Passive progression is not converted into an active action.

### Spawner editor and interactions

Approved GUI: Developer-only /spawner edit on looked-at spawner; icon sections, searchable entity/name/captain selection; six equipment slots; creative-style searchable/filterable item picker, copy actual held components, selected-item bottom preview and return-to-editor save; enchanted-book left+1/right-1/shift-left normal max/shift-right remove; Normal versus bounded Advanced/Unsafe; per-slot drop percentages; separate intrinsic/default loot and custom independent item/count/chance rows; boss one-shot, cap, explicit min/max delay; rotating visual-only preview; named preset browse/save/load/delete; preserve five preset shortcuts.

Approved flag set: Silent, No AI, Invulnerable, Persistent/no natural despawn, Always Show Name. Normal enchantments enforce item compatibility, max level and mutually exclusive rules; Advanced permits deliberate exceptions within bounded supported levels. Draft/apply/cancel, unsaved-change handling, target/rank validation and concurrent-edit protection are required. Preserve held-item rightclick equipment-copy behavior; previews never spawn real world mobs.

Detailed hover information is intrinsic to actual Developer rank; remove showlabels opt-in commands and deny detailed data to Dungeoneers server-side. A wrong-tool warning requires at least20 ticks of continuous breaking of the same spawner; reset on release/retarget, avoiding combat-click spam.

Preserve placed/unloaded spawners, IDs, unknown data and untouched native fields. Current preset export omits native SpawnCount/MaxNearbyEntities/RequiredPlayerRange/weighted potentials; expand compatibly only where complete editor/preset round trips require it, with old-shape and unknown-field tests. Every persistence-changing card carries its own preservation tests; later integration is not permission to discard fields early. Describe conditional loot honestly.

### Metrics and help

Write server-local Dungeon Instance Metrics/<instance-UUID>.xml and totals. Use schema versions, stable server/run/player/party UUIDs, class/name snapshots, dungeon/difficulty, UTC timing/outcome, kills by mob registry ID plus total kills, deaths, spawners broken and existing relevant run statistics. Add resource quantities and donor/recipient/skill events. Retain enough identity/history to retrieve a player's last10 runs or lifetime zombie/all-mob dungeon kills later. Record incomplete/aborted runs truthfully; finalize/totalize each run exactly once, recover safely after crashes and allow totals reconstruction. Bounded buffering/atomic snapshots; no synchronous disk write per hot-loop event. No SQL database, Discord connection or scoreboard implementation is requested now.

Before Beta, create category-specific help artwork with subagents, then rewrite the player-facing guide and H interface. Window uses about80% of available width/height; title anchored top, navigation left, info pane expands and wraps. Reuse existing pages/navigation, update all current character/gameplay instructions, use inline hyperlinks instead of info-pane action buttons, readable type hierarchy/bold/font variation, and hoverable read-only demonstration inventory tiles with actual item tooltips. These tiles cannot grant/move items.

Use16:9 master backgrounds with responsive cropping, preserving text contrast; Minecraft window ratios vary. Reuse class emblems/palette/lore. Cover each existing page category through an explicit asset manifest, including distinct class categories. Do not enable currently inactive class pages merely because artwork exists. Keep canonical help content in its content/document sources; update AGENTS and related workflow references to require current character guidance without duplicating the entire manual into the operational contract.

## Batch cards

All cards are PLANNED. Dependencies are cumulative in the listed order; the scope below names the extra risks beyond common gates.

| Batch | Release | Deliverable |
|---|---|---|
|1|1.7.1-alpha.2|Baseline wolf-test repair, global tooltip layering and hover party inspection|
|2|1.7.2-alpha.1|Shared inventory framework, class-emblem artwork and Skills transparency|
|3|1.7.3-alpha.1|Gear drop/pickup rules, unrestricted ordinary-item/potion trades and available balance|
|4|1.7.4-alpha.1|Tamsin forward progression and resource-sharing consent|
|5|1.7.5-alpha.1|Dungeon instance XML recorder and durable server totals|
|6|1.7.6-alpha.1|Shared-inventory transactions, fair donation order, messages and resource metrics|
|7|1.7.7-alpha.1|Vanilla recipe resolution, potion-role rules and persistent fuel charges|
|8|1.7.8-alpha.1|Safe crafted-potion stacks and full item lifecycle|
|9|1.7.9-alpha.1|Free immediate player resurrection and party/death cooldown displays|
|10|1.7.10-alpha.1|Theurgist/Venefex inventory Craft UI and retirement of old potion/resource UI|
|11|1.7.11-alpha.1|Bogatyr Patrol/Stay replacement, old skill/mode and Kibble retirement|
|12|1.7.12-alpha.1|Feed Pack, temporary Stay breeding and ally interaction guarantees|
|13|1.7.13-alpha.1|Rebindable active-skill hotkeys and complete skill tooltips|
|14|1.7.14-alpha.1|Spawner editor core, preview/flags, intrinsic labels and delayed tool warning|
|15|1.7.15-alpha.1|Spawner equipment and enchantment editors|
|16|1.7.16-alpha.1|Spawner loot editor, preset library and complete compatibility coverage|
|17|1.7.17-alpha.1|Cross-system integration, recovery, metrics and performance acceptance|
|18|1.7.18-alpha.1|Category-specific H-guide artwork using subagents|
|19|1.7.19-alpha.1|Responsive H-guide rebuild, complete player documentation and character links|
|20|1.8.0-beta.1|Final code/request audit, reports and Beta release|

### 1. Baseline and tooltip/hover foundation
Repair the two recorded native wolf failures by determining fixture versus gameplay cause, preserving genuine hostile damage and allied benefits. Resolve/integrate PR245's local-canon policy baseline before the new series proceeds. Fix final tooltip ordering for native items and mod overlays; switch party inspection to hover while retaining authorized data access.
Surfaces: AlliedWolfGameTests, relevant wolf protection code only if faulty, SkillsPanelClient/component, party inspection/HUD, render integration.
Accept: real projectile/effect recipients verified, controlled environment, no weakened assertions; item and panel tips above overlays across GUI scales, no ghost hover while dragging/modal.

### 2. Inventory structure and themed art
Implement the common layout and remove crafting visuals/input, retain storage/menu identities, add class-emblem/lore backgrounds and class-name guide links. Adjust movable Skills transparency/border and preserve reset behavior. This card includes its inventory artwork; help category art remains18.
Accept: normal/shift/drag/hotbar/armor/offhand behavior, existing extra inventory row, recipe controls absent, readably themed active classes, no chest-content edits. Validate in-game scales and fallback screens.

### 3. Ownership and trades
Repair no-drop ammunition/gear restrictions. Limit non-potion trade exclusion to explicit attunement; all potions and other ordinary inventory items trade regardless of vendor price/class tags. Update custody transfer and remaining spendable/available balance for offered trace.
Accept: manual/death drops and owner-only pickup, correct traded ownership, all denominations/refunds, simultaneous offers, cancel/disconnect/leader-start, no duplication or loss, legacy protected-item compatibility.

### 4. Tamsin consent and progression
Keep forward/ready/start navigation available on relevant group subpages. Prompt leader at creation and members at join; offer YES/NO changes, expose hover state. Define consent lifecycle and invalidate removed/offline/different-party donors at execution.
Accept: leader advances without Back, every membership path prompts once appropriately, no hidden default opt-in, toggles propagate, mercenary readiness rules preserved.

### 5. Run recorder
Introduce versioned per-instance XML and totals with stable identity/history, lifecycle checkpoints, per-player/per-mob statistics and resource-event fields for6. Integrate existing kill attribution rather than inventing different kill credit.
Accept: last10 run lookup from data, zombie/overall totals, separate simultaneous instances, restart/aborted/completed/reset paths, exactly-once totals and reconstructability. Measure bounded disk/memory cost.

### 6. Shared transaction service
Add bounded inventory availability updates, server recipe/resource debit transactions, least-recent donor ordering, multi-inventory material selection, consent/party revalidation, notifications and metrics after successful commit.
Accept: split ingredients across donors; fair order under repeated crafts; consent withdrawal, death/relog/disconnect, stale counts, simultaneous clicks, output-full failures and restart recovery consume nothing incorrectly. Integrate resource hover counters through5.

### 7. Recipes and fuel
Resolve full valid vanilla chains and role catalogs, legal stronger/extended variants and mixed-effect drinkable-only outputs for both roles. Implement per-crafter fuel charges20per fuel unit and per-step consumption/persistence atomically with6. Provide model data for the left-side fuel graphic.
Accept: real water bottles/ingredients, intermediate route correctness, no fictional Luck recipe, no mixed splash/lingering bypass, exact quantity/fuel projections, canceled crafts preserve fuel/materials, restart retains remaining charges.

### 8. Potion stack lifecycle
Implement bounded identical-component crafted stacks and output-placement helpers before public cursor crafting. Honor all-potion trading and existing ordinary potions; avoid global unsafe merging of unlike effects.
Accept: split/merge/drag/hotbar/offhand, drinking/throwing consumes one, bottle return/remainders, drop/pickup/trade/death/resurrection/save/relog, full cursor/inventory and partial-transfer boundaries without duplicates or loss.

### 9. Resurrection and cooldowns
Move a working Resurrect... action into Theurgist inventory, free/immediate with180-second successful-cast cooldown; preserve accepted-offer revival semantics. Add each player's/mercenary's cooldown to party hover and death screen, including meaningful unavailable states.
Accept: picker targets, accept/decline/races/timeout/death changes, latest death and inventory recovery, cooldown persistence, mercenary unlock behavior preserved. This precedes10 so hiding Skills/removing Brewing Supplies never strands resurrection.

### 10. Craft palette and legacy retirement
Connect2/6/7/8 to both class inventories: Craft... responsive tiles, variant controls, green/blue/red states and true potion tooltips, left fuel-charge graphic, one-click cursor stacking and one-at-a-time hotbar-first Shift crafting. Suppress both classes' Skills window; retain9's Resurrect action.
Remove Requests, random potion skills and Brewing Supplies after dependencies are replaced; settle pending custody compatibly.
Accept: tile counts/role legality/own versus group debits, donor messages, cursor mismatches/full destinations, rapid/repeated input, changing inventories, GUI scaling/wrapping, no potion keybind path.

### 11. Bogatyr two-mode replacement
Replace all old skills/modes with Patrol/Stay and remove Kibble use/UI. Implement recall/sit anchors, old-aggro clearing, retaliation suppression and re-entry to native defense. Preserve acquisition through existing eggs/taming/breeding and run-only pack lifecycle.
Accept: mutually exclusive modes/default Patrol, moving master leaves Stay anchors fixed, threats before/during/after Stay, relog/unload/reset, mercenary pack rules untouched. Retire saved old state compatibly.

### 12. Pack feeding and breeding
Add Feed Pack with maximum10 actual food inputs in approved priority; ability-only bones; injured-first care, then eligible adult breeding. Permit ally breeding and temporary Stay mating with return/sit after success or timeout. Maintain protection from ally attack/sit/negative effects.
Accept: mixed inventories/limited stock, full health/ineligible/no mate, ownership and newborn pack membership, movement/target cleanup, bounded search, no consumed food without intended action.

### 13. Active-skill controls
Register rebindable active skills/modes, including Feed Pack/Patrol/Stay/Resurrect as applicable. Show actual gray key/chord beside names and in complete hover descriptions. Potion crafting remains manual inventory interaction.
Accept: modifier/custom/unbound/conflicting keys, saved bindings, correct class/permission/cooldown dispatch, no action while typing or behind an incompatible modal, labels update without stale defaults.

### 14. Spawner editor foundation
Add /spawner edit, icon navigation and server draft/apply/cancel sessions with concurrent-edit handling; entity/name/captain, boss/cap/min-max delay, five approved generic flags and visual-only preview. Preserve rightclick copying and all existing data touched by these fields.
Make Developer hover intrinsic, remove showlabels commands, prohibit Dungeoneer detail; add continuous20-tick wrong-tool warning.
Accept: rank/range/target changes and forged packets; cancel no mutations; old/unloaded data; preview no spawn; no combat-tap warning, proper reset on release/retarget.

### 15. Equipment/enchantments
Add slot-specific creative-style item selection/search, component-preserving held copies and bottom item preview. Add interactive enchantment books and Normal/Advanced validation as specified.
Accept: six slots, item names/lore/components preserved, click/shift levels/removal, max/compatibility/conflict validation server-side, no inventory-item grants or accidental live changes before Apply.

### 16. Drops and presets
Add gear percentages, intrinsic/default/custom independent loot rules and honest conditional-loot presentation. Add named preset browsing/save/load/delete and five existing shortcut integrations. Complete compatible export coverage required for faithful editor round trips.
Accept: stable independent rule IDs including same-item rows, zero/full/conditional chances, bounded counts, named file validation, old/current/unknown fields, unloaded blocks, preset apply/cancel and all newly introduced fields. No authored spawner replacement.

### 17. Integration and resource cost
Review end-to-end party consent -> resource debit -> fuel/output -> cursor/inventory/trade -> death/resurrection -> run finalization. Cover class changes, parallel parties, full inventories, disconnect/restart/reset and spawner editor concurrency/old worlds.
Measure meaningful1/30/120-wolf workloads and bounded inventory/metrics sync against baseline; do not silently increase runtime dependencies or resource budgets.
Accept: all release gates pass; previous failures have evidence-backed disposition; concrete licensed UI/co-op checks documented as passed or TESTING, never invented.

### 18. Help artwork
Use subagents with disjoint category asset ownership to produce an explicit manifest and background for each existing help category, including class categories. Read local lore, reuse emblems, compose16:9 masters with quiet text zones and responsive cropping.
Accept: category coverage, file/texture/JAR size budget, legible representative text overlays at supported ratios/scales, no artwork that implies unavailable content. Coordinator alone changes shared asset mapping.

### 19. H interface and player guide
Rebuild fixed384x240 H menu to responsive80%-screen layout, preserve navigation/content and update every affected class/system guide. Add rich inline links/type hierarchy, word wrapping, read-only demonstration item tiles and inventory-to-class deep links/hover styling. Integrate18 art.
Update related player documentation, AGENTS and workflow references so character guidance remains canonical/current. Finalize clear instructions for crafting/fuel/sharing/stacks/trade/ownership/resurrection/wolves/hotkeys and developer editor where appropriate.
Accept: resizing/GUI scale, anchored panes/title, scrolling/hyperlink/back navigation, item/keybind tooltips on top, no fake-slot item transfer, content agrees with shipped mechanics, readable text over every background.

### 20. Final Beta and audit
Review the actual code and all original requirements plus October10 clarifications. Build/release1.8.0-beta.1 from validated merged main with matching companion and stopped TEST delivery. No new feature scope in this card.
Deliver a requirement-by-requirement implemented/partial/not-implemented matrix with source/test/manual evidence; a per-batch summary and AI future recommendation; failed GameTests including first occurrence/cause/fix/retest or unresolved disposition; outstanding TODOs ranked by impact; compatibility/performance notes and remaining licensed acceptance.
Unresolved required tests or blocking defects block Beta publication. Any gameplay fix discovered here returns through a tested Alpha with a unique version and documented plan adjustment before Beta retry; never reuse a published version. Do not call an untested UI requirement complete. Stable remains a separate decision after licensed Beta acceptance.

## Request traceability

| Requirement | Batches |
|---|---|
|All tooltips above panels; hover party detail|1,9,13,19|
|Inventory crafting removal, class emblems/lore art, readability, transparent Skills|2,10|
|Tamsin forward flow; creator/joiner consent and hover state|4,6|
|Resources used/contributed; XML run history and server totals|5,6,17|
|Drop any gear; protected owner pickup; broad trading; offered trace balance; all potions tradable|3,8|
|Real own/shared potion materials; complete vanilla recipes/fuel and graphic|6,7,10|
|Positive Theurgist/negative Venefex; both drinkable-only mixed effects|7,10|
|Craft palette counts/colors/wrapping/tooltips/cursor stacks/Shift hotbar priority|8,10|
|Remove Requests/random potion skills/Brewing Supplies; no two-class Skills panels|9,10|
|Immediate free player Resurrect;3-minute cooldown; player/mercenary death/party displays|9|
|Bogatyr old skills/modes/Kibble removal; Patrol/Stay; forgotten aggro; fixed Stay positions|11|
|Feed Pack10food; ability-only bones; ally breeding/protection; temporary Stay breeding|12|
|Rebindable non-potion active skills, actual gray hotkeys and complete tooltip text|13|
|Intrinsic Developer spawner hover/no Dungeoneer detail;1second continuous warning|14|
|Full approved spawner GUI/equipment/enchantments/flags/loot/preview/presets|14,15,16|
|Per-category art with subagents;80% H guide/rich text/item demos/class links/docs|2,18,19|
|One Alpha per batch; final1.8.0Beta audit/recommendations/failed tests/TODOs|1-20|

## Baseline evidence and planning limits

Source baseline: d5dd3f75a882ed365a4958c55491339d7a3a57bc (prior Batch26 main merge).
Local-canon policy change:20f9dd421e8705ee39960f6d0c1ed4ce8149e2e5, PR245.
Integration Gate38101339624 passed clean build but failed:
- cosmicdungeon:allied_wolf_projectiles — Hostile flaming tipped-arrow tick still wounds and applies side effects on tick0.
- cosmicdungeon:allied_wolf_potions — Native allied healing and beneficial lingering effects still apply on tick0.

Read-only investigation suggests immediate entity-section visibility and uncontrolled projectile weather as possible fixture causes; neither is proven or fixed. Record them as OPEN until Batch1 establishes and tests the cause. A later passing run alone does not erase this history.
Current planning work does not change game code, publish artifacts, deploy TEST, initialize the new queue, or claim manual gameplay acceptance.
