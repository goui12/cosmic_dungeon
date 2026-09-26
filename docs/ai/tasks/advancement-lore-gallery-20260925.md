# Lore advancement gallery - 2026-09-25

Cameron requests a lore-only advancement catalog, descriptions for every retained entry,
and a larger, more impressive starry advancement screen with meaningful icons.
His explicit description request supersedes the older sheet instruction for names only.

Branch: feature/advancement-lore-gallery-20260925, chained from 0f00c746 (PR197).
Owned integration surface: ModAdvancementProvider and CosmicAchievementIds.
No other active task owns these surfaces.

## Scope and sources

Read the full bodies of 17 relevant mirrored Docs and the relevant Debloat Achievements
and Questions and answers workbook cells, including workbook/tab metadata.
The current-body audit supports 30 named achievements plus six Bloom records.
Learn About Trace and First Trace are one achievement (Q&A D77); existing six custom
Blooms are retained (Q&A D23). Do not create an additional Learn About Trace award.
The current user request governs descriptions; older names-only instructions do not.

Remove Handshake Protocol, BOOM!, Monster Compendium, Player Classes and the Pyroclast
category from the visible catalog. No supporting lore entry was found in the 424-Doc
mirror scan or the reviewed workbook evidence. Retain five displayless impossible
compatibility IDs with their original criterion names. The first-trade marker still
retires the tutorial prompt; remove only the obsolete BOOM award hook.

Source freshness limitation: Google's saved authorization expired during the read-only
metadata-refresh attempt. This audit uses existing repository Docs/workbooks and does
not claim that newer cloud edits were verified. Private evidence stays in the ignored
Google Docs and Sheet/Audit/Advancement_Lore_2026-09-25 directory: reviewed-sources.json,
sheet-evidence.json, freshness.json and disposition.json. Source hashes/IDs and recorded
snapshot timestamps are preserved there; no credentials or private mirrors are staged.

## Exact change surfaces

- src/main/java/net/goui/cosmicdungeon/datagen/ModAdvancementProvider.java
- src/main/java/net/goui/cosmicdungeon/achievement/CosmicAchievementIds.java
- src/main/java/net/goui/cosmicdungeon/playerclass/pyroclast/PyroclastGunpowderEvents.java
- src/main/java/net/goui/cosmicdungeon/client/advancements/AdvancementGalleryEvents.java
- src/main/java/net/goui/cosmicdungeon/client/advancements/AdvancementGalleryLayout.java
- src/main/java/net/goui/cosmicdungeon/client/advancements/AdvancementStarfield.java
- src/main/java/net/goui/cosmicdungeon/client/advancements/CosmicAdvancementScreen.java
- src/main/resources/assets/cosmicdungeon/lang/en_us.json
- 42 JSON files under src/generated/resources_server/data/cosmicdungeon/advancement/
- src/test/java/net/goui/cosmicdungeon/achievement/AdvancementCatalogTest.java
- src/test/java/net/goui/cosmicdungeon/client/advancements/AdvancementGalleryLayoutTest.java
- docs/Achievements/Achievements_and_Advancements.md
- docs/Trading/Trading_Guide.md
- This task card and its matching release fragment.

## Design and boundaries

Use the native ClientAdvancements listener, progress and selected-tab packets.
A client-only screen-opening subscriber replaces only the exact vanilla screen;
specialized screens from other mods retain their behavior. Native roots remain chapters.
Respect displayless and secret entries. Paginate a bounded card grid; separate the
scrollable description panel. Native button navigation, narration and tooltips remain.
A fixed 120-star field uses existing GUI drawing and item assets; no animation, runtime
dependency, large texture or network polling. Rebuild the catalog only after advancement
callbacks and navigation, not every rendered frame.

No registry, saved-data schema, counter, reward, trade transaction, class restriction,
inventory, world binding, teleport/rift, spawner, door/key, entity or block-entity change.
No migration is required. Gunpowder behavior is unchanged except for retiring BOOM.
The gallery cannot grant progress or bypass server authority. Client imports stay in
the client-only subscriber/package. Dedicated/integrated runtime gameplay is not claimed
as tested by the build or code review.

## Validation and handoff

Java21 build passed; all 69 JUnit tests passed with zero failures, errors or skips.
Five new tests cover the lore catalog, descriptions/icons, old criterion compatibility,
secret Tax presentation and seven viewport layouts down to 320x240.
All 1,997 source JSON files parse; git diff --check passes.
runServerData completed: 42 advancement JSON changes, no unrelated generated resources.
runClientData is not applicable: icons already exist and language/GUI changes are authored.
No clean: preserve the pre-existing staged historical-JAR deletion. No licensed dedicated
or GameTest server launch was authorized, so neither is claimed as run.

The development client launched to a responding Minecraft window (PID36772); native loader
logs confirm the new client-only screen subscriber was registered without a gallery or fatal
startup error. No world was entered; visual/interaction acceptance remains manual.

TEST shutdown log still matches the prior stopped-server hash:
BE22896B122DAE7BB2DF8AA26655752002E6938952541E0E8C9879CFFCC8444C.
Build publication, client-startup evidence and deployment receipts are recorded in the
operational cache. Final safe deployment requires unchanged stopped-server/closed-installed-
client guards. Only the mod JAR is replaced; Cameron controls the server restart.

Manual QA remaining:
1. Open Advancements: verify starfield, larger window, icons and descriptions; use page/chapter
   controls and compare GUI scales. Check long titles and scrolling the detail panel.
2. Check retained earned progress and Bloom records. Confirm Handshake/BOOM/placeholders
   are absent and the Tax stays secret until earned.
3. Complete a normal trade: transfers remain correct and the old tutorial prompt stays retired.
   Check a normal lore achievement toast and reopen the gallery to verify updated progress.
4. Check vanilla/other-mod chapters, close/reopen, disconnect/rejoin and small-window navigation.

Zero additional implementation batches are planned for this request. Licensed visual and
multiplayer acceptance remains with Cameron. Possible future improvement: add a search
filter if the catalog becomes difficult to browse.
