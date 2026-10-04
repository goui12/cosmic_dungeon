# Class selector nearby class label

## Request and implementation
Cameron requested a small selected-class label above the hovered class selector, visible
only close enough to highlight the block. Branch feature/selector-hover-class-20260924
follows149f280d. Five new files, no existing registration/network/registry hot spots changed.
The feature is a client-only object-oriented subscriber to the native
ExtractBlockOutlineRenderStateEvent, plus a small testable presentation helper.

The label is captured only for the actual highlighted selector with a BLOCK hit at its
root. It additionally requires a real fitted-shape intersection within the player's
blockInteractionRange, capped at5blocks even when another mod extends targeting. The
direction is normalized; a long vector cannot extend range. Misses, different native
targets, empty prop gaps, non-finite inputs, absent players/worlds, remote cameras,
Spectator, open screens and hidden HUD do not produce the label.
Native target selection supplies wall/entity occlusion; no extra world raycast or scan.

One frame-local callback draws Class: <selected class> at root centerY+2.3, above the
two-block model. Text is small, pale lavender, camera-facing, with shadow/dark backing,
normal depth testing and full-bright glyphs. It is not a through-wall overlay.
The renderer returns false to retain the existing fitted block outline. Native outline
pass selection prevents double drawing. Pose push/pop is paired in a finally block.
The callback captures text/font/width only, never a live level/player/block entity.

ClassData.getClassId reads the local player's existing server-synced class without
rewriting NBT. Existing playerclass translation keys are reused. None/unknown IDs render
Class: None. The next extracted frame reflects an accepted class change. Every viewer sees
their own class. No packets, requests, new sync cache, polling, tick work, assets, resource
files, runtime dependencies or saved-data migration. At most one small text draw and one
cached-shape clip per highlighted selector frame.

## Source review and validation
Repo AGENTS unchanged from149f280d; no nested src/docs rules. Existing renderer/overlay,
ClassData/ClassNet/client-login sync and selector-targeting patterns were inspected.
Local1.21.10 source confirms native outline extraction/custom renderer lifecycle,
camera-relative transforms, font depth mode, pass selection and callback return behavior.
Direct UI request supplies authority; no Google lore/gameplay policy changed. The prior
Google authorization expiry remains unresolved; no fresh canon-sync claim.

Java21 build passed with23 JUnit methods, zero failures/errors/skips. Four new tests cover:
- Actual matching block hit, MISS/other target/looking away/empty-space rejection.
- Shortened player reach, finite extended reach capped at5, zero and invalid direction.
- Upper trident's normalized packet location plus real geometry, translated world position.
- Canonical class translations and None fallback.
Previous19 selector/placement/protection/HUD/codec tests remain passing.
The first run exposed a test comparing native debug toString output to plain text; fixed
the test to read PlainTextContents.text and reran successfully. Production behavior was
not changed to satisfy this assertion.
No datagen or JSON validation needed: generated resources/JSON untouched.
Final documentation build, scoped staged diff/check and local checkpoint follow.
JAR SHA256: ec9eaccd74550b258dc2036f32d6b346fc78d07dff26f39e5527b3c2a0465d51.

## Runtime handoff and boundaries
runClient wrapper41724 launched client40872 after confirming no other client was running.
Window responds in Singleplayer. Native log confirms label event subscription07:43:31,
audio startup07:43:35 and user-driven world join07:44:03; no ERROR/uncaught exception.
No world input was automated by the assistant. This proves startup, not visual acceptance.
Manual checks:
1. Aim at a nearby selector: small personal class label appears above the props; native
   fitted outline remains. Check sword/shield/trident upper tip.
2. Look away, aim at a nearer wall/entity, walk beyond normal reach, open a screen or hide
   HUD: label disappears. Extended targeting must still not show it beyond5blocks.
3. Change class through the normal approved flow: label updates to the new class.
4. Check None, first/third person, dark surroundings, save/rejoin and dimension changes.
   Verify particles, Tamsin and selector interaction still behave normally.
5. Multiplayer viewers should each see their own class; dedicated TEST acceptance remains
   separate from this development-client handoff.

No changes to server authority, class enforcement, Tamsin agreement, ready/queue/teleports,
rifts/destinations, currency/inventory/vendor/trade, authored chest contents, spawners,
doors/keys, achievements/factions or saved entity/block-entity data. No migration/backfill.
No dedicated/GameTest server, clean, push, deployment or persistent watcher.
Zero implementation batches remain; native visual QA remains. Broader D1 audit plan unchanged.
Possible improvement: tune font size/height/color after Cameron's visual feedback.

## Exact changed files
- src/main/java/net/goui/cosmicdungeon/client/ClassSelectorHoverLabel.java
- src/main/java/net/goui/cosmicdungeon/client/ClassSelectorHoverSelection.java
- src/test/java/net/goui/cosmicdungeon/client/ClassSelectorHoverSelectionTest.java
- docs/ai/tasks/selector-hover-class-20260924.md
- docs/releases/fragments/selector-hover-class-20260924.md

Evidence: C:/Users/Cameron/Documents/mod_development/CosmicDungeon_AI/backups/selector-hover-class-20260924
Preserve unrelated staged historical-JAR deletion, generated caches, build outputs and logs.
