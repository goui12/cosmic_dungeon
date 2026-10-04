# Class selector presentation

## Request and ownership

Cameron requested fitted collision/selection, a readable sword/shield/brewing-stand item
icon and corrected held presentation, plus a magical placed-block effect. Source:
2026-09-24 screenshots and explicit local implementation request. This task owns the
selector block's shape override, dedicated shape/targeting helpers, one client mixin
registration and the selector entry in ModModelProvider. No competing task writes found.
No registry, class policy, Tamsin, teleport/ready, network-handler or persistence hotspot
changes are required. Direct cosmetic requirements do not change any canon gameplay rule.

## Result

- Read the bundled Blockbench model once, cache a common-side 53-box VoxelShape, and use
  it for outline and collision. Rotated solids are conservatively stair-stepped at half
  a model pixel. The placed geometry/texture and two-block height remain unchanged.
  A resource pack can change visuals but does not redefine server collision.
- A small client-only GameRenderer return hook delegates to ClassSelectorTargeting.
  It checks the root blocks whose overhangs intersect the ordinary view ray, including
  the upper trident and negative-X shield edge. Native reach and nearer wall/entity
  hits limit the ray. Work is capped at 64 ray cells and 27 candidate offsets per cell
  (the bundled model needs 8 offsets). No world/entity scans, chunks loaded, or added
  input packets. Extreme custom reaches exceeding 64 traversed cells retain native
  behavior beyond the bounded supplemental search.
- Above-root interaction points are mapped inside vanilla's +/-1-from-root-center
  hit-coordinate envelope only after real-surface reach/occlusion checks. Server root
  reach, protection, Developer/Dungeoneer role and Tamsin agreement checks remain native
  and unchanged. No server validation hook or packet/schema override.
- A new transparent 64x64 sword/shield/brewing-stand icon uses item/generated via existing
  client datagen. Native flat-item transforms replace the oversized in-hand block model.
- Hovering emits two native soul/enchantment particles per eight client ticks (five per
  second), capped globally to one focused selector. Decreased particles halves that;
  Minimal disables it. Briefly looking away does not reset the rate limit.
  Screens/pause/unload stop emission. No new particle registry,
  assets for effects, dynamic lighting, sounds, server tick work or runtime dependency.

## Validation

Java 21 runClientData completed; exactly two selector JSON outputs changed/appeared
besides ignored/pre-existing hash caches. runServerData is not applicable.
Java 21 build and menuBrandingChecks completed successfully. Six new native JUnit tests
passed for solids/gaps/collision parity, upper and side targeting, native coordinate
bounds, reach/wall/entity occlusion, bounded traversal and icon/model packaging.
Existing five HUD/balance methods (5,016 balance checks), one registry serialization
method (93 checks), and 34 menu + 8 loading checks also pass. No tests skipped.
1,997 source/generated JSON files parsed; four packaged selector assets match source.
All existing selector interaction/menu/destination methods are byte-preserved.
git diff --check is the final formatting gate.

The empty lookup-fixture targeting benchmark averaged 2.83 microseconds over 5,000 rays;
this is a bounded offline baseline, not a native frame-time/GPU measurement.
The initial compile attempt caught an incorrect ParticleStatus import; corrected to
the checked-in 1.21.10 package net.minecraft.server.level, then datagen/build passed.
No clean was run under tracked historical-JAR protection. No GameTest/dedicated server
was started; those retain separate authorization. Built tests do not establish GUI QA.

## Compatibility and native handoff

No new block/item IDs, blockstate properties, block-entity data, world writes or migration.
Existing placed selectors obtain the fitted shape after loading this build. Shared
collision code has no client imports; the targeting hook and aura are client-only.
Authored chest stacks, inventories/currency, vendors/trades, classes, Tamsin agreement,
rifts/teleports, spawners, doors/keys, achievements/factions and entity persistence are
unchanged. The cached physical shape and successful clicking still need native QA.

A healthy prior client (PID8160) was running single player during validation. Cameron
was asked to Save and Quit then Quit Game so runClient can launch the new build without
interrupting his world or creating a duplicate. Startup/visual acceptance is pending.

Manual checks:
1. Compare hotbar/inventory, dropped item, and both held hands: upright readable icon,
   transparent background and ordinary item size.
2. Walk around the pedestal and aim at sword/shield/trident tips. Outline follows solids,
   empty gaps remain empty, and upper/side clicks reach the same selector.
3. Put a solid block between player and selector; the hidden selector must not be picked.
   Confirm ordinary reach, Developer settings, and Dungeoneer/Tamsin denial/accepted flow.
4. Aim at the selector for blue soul sparks/enchantment motes; look away/open a screen.
   Check Decreased and Minimal particle settings.
5. Reload an existing placed selector and repeat. Dedicated-server comparison remains
   future authorized TEST; this is a local development-client handoff.

No push or deployment. Preserve unrelated staged historical-JAR deletion, generated
cache edits, build output and logs. Future improvement: tune the hover effect's density
or color after Cameron's native preview.

## Exact files

- src/main/java/net/goui/cosmicdungeon/block/custom/D1_Class_Selector_Block.java
- src/main/java/net/goui/cosmicdungeon/datagen/ModModelProvider.java
- src/main/resources/cosmicdungeon.mixins.json
- src/generated/resources_client/assets/cosmicdungeon/items/class_selector_block.json
- src/main/java/net/goui/cosmicdungeon/block/custom/ClassSelectorShape.java
- src/main/java/net/goui/cosmicdungeon/block/custom/ClassSelectorTargeting.java
- src/main/java/net/goui/cosmicdungeon/mixin/client/ClassSelectorTargetingMixin.java
- src/main/java/net/goui/cosmicdungeon/client/ClassSelectorAmbience.java
- src/test/java/net/goui/cosmicdungeon/block/custom/ClassSelectorPresentationTest.java
- src/main/resources/assets/cosmicdungeon/textures/item/class_selector_block.png
- src/generated/resources_client/assets/cosmicdungeon/models/item/class_selector_block.json
- docs/ai/tasks/class-selector-presentation-20260924.md
- docs/releases/fragments/class-selector-presentation-20260924.md

## Icon provenance

Generated with the built-in image-generation tool, then nearest-neighbor downsampled
to 64x64 while retaining alpha; no block texture was repainted.
Final texture: src/main/resources/assets/cosmicdungeon/textures/item/class_selector_block.png
SHA256: c39d53423de71451e1d91a8b132246009b12d5a7a9158654661dc1899ba1d668
Original generator output: exec-b45f7118-d655-4cfc-b4a8-4d392f4c2872.png
Full generation prompt:

Use case: stylized-concept. Asset type: one Minecraft Java inventory item sprite for the Cosmic Dungeon Class Selector block. Create a simple, unmistakable sword / shield / brewing stand emblem, a compact unified object, not three separate icons. Large central dark charcoal kite shield with steel edging; a stout steel sword rising diagonally behind its upper-left edge; a small recognizable brass Minecraft brewing stand with two bright cyan-violet potion bottles at the lower-right in front. Cool haunted cyan and violet glints at a few corners, readable bright highlights on dark materials. Authentic low-resolution pixel-art item texture, designed on a 64 by 64 pixel grid, hard pixel edges, bold small silhouette and only a few clusters of shading. Square canvas, centered object occupying 88% of width and height with clear margins, front-facing 2D sprite with very slight isometric depth. Genuine transparent alpha background, including the openings between objects; no background color, no checkerboard painted into pixels, no floor, no scene, no shadow beyond silhouette, no framing badge, no words, no text, no watermark. No photorealistic surfaces or fine filigree. The design must remain recognizable when viewed at 32 pixels in a Minecraft hotbar. Sword points upward-left. Output PNG with transparency.
