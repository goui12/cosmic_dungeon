# Batch19 - Skills panel foundation

Scope: approved card19, feature/skills-panel-foundation-20261007,1.6.9-alpha.1.
Baseline is merged Set B main7c58a5c5d4639e4533d9663db0675e3a43f9a3d4.
Stop after CurseForge publication and stopped TEST sFTP; do not begin20.

## Behavior and boundaries

Reusable client Skills model/state/preferences/renderer and shared geometry provide
per-class stored normalized placement, header dragging, minimization with resource
display, equal-sized action rows and wheel/visible draggable scrollbar. Layout
reflows/clamps at viewport changes; Reset Skill Panel UI clears saved placements.
Bogatyr/Theurgist use existing guide-only action slots. Other classes gain no invented
abilities; absent authoritative resource counts remain unavailable. Future dead-player
actions can use the same bounded row list/scrollbar. No future resource spending,
Requests transactions or wolf-command gameplay is implemented.

Skills owns only its mouse capture; carrying an inventory item bypasses new captures.
Shared regions preserve ordinary inventory slots at vanilla supported minimum sizes.
Extremely small nonstandard viewports use a visible compact Skills fallback; the
inventory itself is never moved. HUD lobby/active health scrolling remains independent;
readiness, mercenary detail and revival actions keep their existing server validation.
Fixed-right Requests and above-XP resource rectangles are reserved for their later
content providers, not populated with fake controls or amounts.

The blue square/white i opens the exact existing H class guide and returns to its
parent inventory. Recipe-book buttons, remembered visibility, toggles and ghost
previews are suppressed across recipe-book menus without changing saved book
preferences, server recipes or CraftingPolicy. No new packets, world schema,
migrations, runtime dependencies, authored chest changes or game/server launches.

## Validation and review

Java21 full local build, source JSON/diff checks and exact full PR/release clean CI
with native GameTests are required. Regression coverage includes shared geometry,
drag/capture/release/resize/minimize/reset, scroll overflow and per-class preferences,
plus existing exact H-guide directory identities. Root/worker review covers native
API usage, input priority, tooltip ownership, storage bounds and dedicated-server
separation. Operational BatchRunner evidence records actual results and source IDs;
this report does not pre-claim publication or played acceptance.

## Licensed manual QA - TESTING

1. On Bogatyr and Theurgist, open inventory at supported GUI scales/resolutions.
   Verify Skills/group/account do not cover item slots by default, headers/resource
   remain visible when minimized, and blue i opens the matching H page; H/Esc returns.
2. Drag/release outside panel bounds, reopen inventory, switch class, resize/change
   GUI scale, then use Cosmic Dungeon Settings > Reset Skill Panel UI. Placement
   stays visible, per-class preferences restore, and reset returns default placement.
3. Drag inventory stacks through panel bounds: no panel movement/action or captured
   item drag. Move Skills over group HUD/account; only the upper panel owns input
   and its tooltips. Scroll each overflowing pane by wheel and scrollbar.
4. Inspect invited/lobby/queued/active groups with six members and mercenaries:
   ready/leave/join/revive, queue/difficulty/status/health/effect tooltips and member
   inspection remain usable. Server checks still reject stale/unauthorized actions.
5. Open inventory, crafting and furnace-family menus with remembered-open books:
   no book/button/ghost recipe; underlying permitted crafting still works.
6. Confirm clients obtain the runtime/helper through CurseForge. No local client
   installation, startup or licensed acceptance is inferred from build/deployment.

## Delivery and remaining work

Unique Alpha tag from reviewed task source, one CurseForge publisher/runtime+helper,
then exact release runtime to stopped TEST through pinned sFTP. Preserve source,
publication receipts and upload acknowledgements without post-publication hashes.
Complete compact controller receipt; leave task PR unmerged until completed Set C.
Seven cards remain20–26: resources, Requests, player Theurgist, wolf commands/lifecycle,
core modes, advanced modes/performance, final integration/Beta.

## Exact changed files

- docs/ai/D1_REMAINING.md
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
- docs/ai/tasks/skills-panel-foundation-20261007.md
- docs/releases/1.6.9-alpha.1.md
- docs/releases/fragments/batch-19-skills-panel.md
- gradle.properties
- src/main/java/net/goui/cosmicdungeon/client/DungeonInventoryRecipeBook.java
- src/main/java/net/goui/cosmicdungeon/client/HelpMenuKeybindClient.java
- src/main/java/net/goui/cosmicdungeon/client/economy/CurrencyBalanceOverlay.java
- src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyHud.java
- src/main/java/net/goui/cosmicdungeon/client/screen/HelpMenuContent.java
- src/main/java/net/goui/cosmicdungeon/client/screen/HelpMenuScreen.java
- src/main/java/net/goui/cosmicdungeon/client/screen/settings/CosmicDungeonOptionsIntegration.java
- src/main/java/net/goui/cosmicdungeon/client/screen/settings/CosmicDungeonOptionsScreen.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/PanelScroll.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/SharedInventoryLayout.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/SkillsPanelClient.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/SkillsPanelComponent.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/SkillsPanelModel.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/SkillsPanelPreferences.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/SkillsPanelState.java
- src/main/java/net/goui/cosmicdungeon/mixin/client/InventoryRecipeBookComponentMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/client/InventoryRecipeBookScreenMixin.java
- src/test/java/net/goui/cosmicdungeon/client/screen/ClassGuideNavigationTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/PanelScrollTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/SharedInventoryLayoutTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/SkillsPanelModelTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/SkillsPanelPreferencesTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/SkillsPanelStateTest.java
