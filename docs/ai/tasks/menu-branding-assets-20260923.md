# Menu branding asset preparation - 2026-09-23

Status: asset preparation and client integration complete; native preview acceptance pending.
Branch: feature/menu-branding-assets-20260923.
Initial scope: the 12 completed desktop assets, exact 42-pixel side crops, 1024-square exports,
provenance, and validation. Authorized client integration was then added as documented below.
Ownership: new assets/cosmicdungeon title/background/loading/text/music assets plus this
task's client documentation and unique release fragment. Client integration owns build.gradle, gradle/menu-branding.gradle, CosmicDungeonClient.java, and the client list in cosmicdungeon.mixins.json. Common entry point and all gameplay/network/registry hotspots remain untouched.

No saved-data, registry, network, security, or migration changes.
No Google canon decision is involved; Cameron's supplied art/text/audio governs this task.
Preserve pre-existing staged build-JAR removal, generated caches, logs, and build output.
No publishing request was made; keep this checkpoint local. A future commit/push request
must follow AGENTS.md and verify current remote ancestry before publishing.

Validation: exact crop pixel comparisons, source/copy/ZIP hashes, PNG dimensions, complete
Ogg page CRCs, source JSON validation, Java 21 build, packaged-resource hash checks, and
git diff --check. Local game/GameTest execution is not authorized by the current request.
Datagen is not applicable. Rendered panorama seams and audible playback remain pending.
Documentation: ../../client/Menu_Branding_Assets.md.

## Validation results

- Java 21 offline build passed. The first shell wrapper treated an SLF4J stderr warning
  as a terminating error; rerunning with normal native stderr handling passed.
- clean was not run because the local workflow protects tracked build artifacts.
  Existing build JARs were backed up before validation; the pre-existing staged removal
  of the historical 1.5.0 JAR was preserved exactly.
- 1994 source JSON files parsed successfully.
- All 12 imported resources matched their built JAR entries byte-for-byte.
- All imported PNG CRC/decompression checks passed; all 5,312,886 cropped pixels matched.
- Soundtrack: 221 seconds, stereo Vorbis, 44.1 kHz; all 2,427 Ogg page CRCs passed.
- Offline cubemap preview shows mismatched joins, especially floor/ceiling. No claim of
  seamlessness; capture position/projection/pole orientation remain to be diagnosed.
- No game, GameTest, deployment, datagen, common-code changes, or migration.
- Unrelated tracked/cache edits and pre-existing staged changes were verified unchanged.

Original import handoff: client integration was pending. The authorized follow-up below completes it.

## Authorized follow-up: connect assets for runClient

Cameron explicitly requested wiring these assets so he can runClient and inspect them.
Use the prepared panorama despite its known seams; this supersedes the earlier suggestion
to wait for seam acceptance. Preserve source imagery and orientation.

Implementation: dedicated client logo renderer and music event handler, narrow title-render
mixin, native panorama/splash resource aliases, music.menu resource override, and an OOP
Gradle task preparing the external FML theme before runClient/IDE launches.
Loading theme uses a muted lavender background for contrast with the supplied dark logo;
inherits NeoForge fox, version and icon, with an additional visible NeoForge credit.
Only the development run/config early-theme files are prepared; preserve unrelated fml.toml
settings and back up changed existing files. No installed client/server deployment or launch.

Expected changed directories: client/branding, mixin/client, src/test/client branding,
src/main/resources/assets/minecraft, src/main/loading-theme, gradle, and task documentation.
Automated gates: Java21 build, actual FML ThemeLoader parsing, alias/image/music packaging,
JSON validation, native mixin call-site inspection, runClient task ordering, and diff review.
Manual gates: startup/transition, title/GUI scale, all panorama seams, text/music, volume
controls, entering/leaving a world and resource reload. Cameron will run the preview.
No world/schema/registry/protocol/migration changes. Datagen is unrelated; generated aliases
are normal processResources copies, not NeoForge model/tag/recipe datagen.

## Integration validation

Java21 build and 29 offline checks passed, including the actual installed FML theme parser.
1996 source JSON files valid. Native aliases match the authored assets in the built JAR.
Native TitleScreen bytecode has exactly one matching logo-render call. Client mixin only.
runClient dry run orders theme preparation before launch. Existing FML settings preserved.
First build caught Groovy closure dispatch in the new task; ordinary class loop fixed it.
No test weakened. Build used without clean under tracked-artifact protection; JAR backed up.
No datagen/client/server/GameTest launch. No world/schema/registry/network/security changes.
No migration. All gameplay systems remain untouched and were not runtime tested.
Native panorama joins, logo contrast/layout, audio/sliders and login transitions remain QA.
No installed-instance/server deployment or push. One user preview pass remains.

Exact changed repository files:

- src/main/java/net/goui/cosmicdungeon/client/branding/CosmicMenuLogoRenderer.java
- src/main/java/net/goui/cosmicdungeon/mixin/client/TitleScreenBrandingMixin.java
- src/main/java/net/goui/cosmicdungeon/client/branding/CosmicMenuMusic.java
- src/main/resources/assets/minecraft/sounds.json
- src/main/loading-theme/theme-cosmicdungeon.json
- gradle/menu-branding.gradle
- build.gradle
- src/main/java/net/goui/cosmicdungeon/client/CosmicDungeonClient.java
- src/main/resources/cosmicdungeon.mixins.json
- src/test/java/net/goui/cosmicdungeon/client/branding/MenuBrandingChecks.java
- docs/client/Menu_Branding_Assets.md
- docs/client/menu_branding_assets.json
- docs/ai/tasks/menu-branding-assets-20260923.md
- docs/releases/fragments/menu-branding-assets-20260923.md

Manual QA: docs/client/Menu_Branding_Assets.md.
Future improvement: settle panorama alignment and title contrast using the native preview.


## Authorized follow-up: install approved brighter logos - 2026-09-24

Cameron approved the generated epic v2 title and edition exports and requested installation
for his runClient preview. Copy the approved bytes; retain 1024 x 256 and 512 x 64 RGBA PNGs.
No Java, Gradle, theme geometry or integration hotspot edits are needed. Existing OOP
renderer and theme-preparation task consume the replacements at their current paths.
Preserve desktop originals; back up repo PNGs, metadata and prior build JARs outside Git.
No saved-data, registry, networking, gameplay, security or migration effects; no datagen.
No client/server/GameTest launch or publishing request. Native appearance remains user QA.

Exact repository scope:

- src/main/resources/assets/cosmicdungeon/textures/gui/title/cd_minecraft.png
- src/main/resources/assets/cosmicdungeon/textures/gui/title/cd_edition.png
- docs/client/menu_branding_assets.json
- docs/client/Menu_Branding_Assets.md
- docs/ai/tasks/menu-branding-assets-20260923.md
- docs/releases/fragments/menu-branding-assets-20260923.md

Validation: Java21 build and all 29 existing branding checks passed; all 12 packaged asset
byte comparisons, 1,996 source JSON parses and git diff --check passed. Transferred PNG
SHA-256, dimensions, RGBA, chunk CRCs and decompression passed before replacement. Evidence and originals are in the sibling
CosmicDungeon_AI/backups/menu-logo-refresh-20260924 folder.

The prepared startup title matches the approved file; its previous version backup was verified.
FML config/theme bytes, unrelated worktree status and pre-existing staged diff are preserved.
Build used without clean under tracked-JAR protection; prior build JARs backed up.
New title native contrast/layout/GUI scales remain manual QA; no game or GameTest launched.
Next improvement: tune logo display scale after the requested native preview.
Final local checkpoint follows completed notes and exact six-file diff review; no push.


## Authorized follow-up: haunted background and darker lettering - 2026-09-24

Install the user-approved horror background; return to the original dark logo material,
widen the title/edition, and define their edges with irregular haunted lighting.
Same task/branch. Ownership includes the existing Gradle menu-branding setup, theme,
new isolated earlyLoading source set/service, artwork, focused checks and task docs.
No common entry point, gameplay, networking, registry, schema, world or security changes;
no migration or datagen. Preserve unrelated Git changes, desktop originals and prior JARs.
Only the development early loading provider/config changes; no installed-client/server
deployment, publishing, client launch or GameTest execution is authorized.

Use a separate OOP provider extending native DisplayWindow, with a delegating scheduler
to put cosmicBackground first before the first render task. Native controls/credits and
window/Minecraft handoff remain intact. Two FML10.0.32 private fields are isolated and
checked for compatibility. Additional image memory is bounded to about6MiB decoded.

Validation: pending Java21 build, actual FML parser and existing branding checks; add
background-copy/contrast/provider checks and scheduler first-frame/order/lifecycle checks.
Also verify client launch classpath and dry-run ordering, helper exclusion from the main
mod/server runtime, all JSON, packaged hashes, config preservation and scoped Git diff.
Manual: first frame, progress controls and credits, window resize, initial transition,
new wordmark readability/GUI scales/splash overlap. No runtime visual result claimed.
Exact file scope is appended after completion. Evidence: sibling CosmicDungeon_AI/
backups/haunted-loading-20260924. Future improvement: adjust contrast after native preview.


### Haunted-theme completion

Java21 build and 42 offline checks passed: 34 branding + 8 background ordering/scheduler.
The first configuration attempt used an obsolete additional-runtime setting; FML1.21.10
requires an ordinary runtime classpath. A separate empty brandedClient launch source set
now adds the helper only to client runtime and reuses main outputs/dependencies.
Generated client/server launch classpaths verify inclusion/exclusion; shared mod JAR omits
helper classes/service. runClient dry-run orders helper and theme preparation before launch.
1,996 source JSON files parsed; all 13 assets match the built mod JAR byte-for-byte.
Native earlydisplay is an automatic module; private-field access passes offline checks.
Native loading overlay accepts the DisplayWindow subclass and retains the framebuffer handoff.
Only earlyWindowProvider changed in existing fml.toml; theme selection already matched.
Prior config/theme/images/build JARs preserved in the scoped backup. Unrelated staged JAR
removal and tracked cache changes preserved. git diff --check passed. No datagen or migration.
No game/GL/server/GameTest execution, deployment or push; manual first-frame, resizing,
credits/progress, transition and wordmark readability QA remains Cameron runClient work.

Exact changed repository files for this pass:

- src/main/resources/assets/cosmicdungeon/textures/gui/title/cd_minecraft.png
- src/main/resources/assets/cosmicdungeon/textures/gui/title/cd_edition.png
- src/main/resources/assets/cosmicdungeon/textures/gui/loading/cd_loading_background.png
- src/main/loading-theme/theme-cosmicdungeon.json
- src/earlyLoading/java/net/goui/cosmicdungeon/loading/CosmicLoadingWindow.java
- src/earlyLoading/java/net/goui/cosmicdungeon/loading/CosmicLoadingScreenAccess.java
- src/earlyLoading/java/net/goui/cosmicdungeon/loading/CosmicLoadingScheduler.java
- src/earlyLoading/resources/META-INF/services/net.neoforged.neoforgespi.earlywindow.ImmediateWindowProvider
- src/test/java/net/goui/cosmicdungeon/client/branding/MenuBrandingChecks.java
- src/test/java/net/goui/cosmicdungeon/loading/LoadingBackgroundChecks.java
- gradle/menu-branding.gradle
- docs/client/Menu_Branding_Assets.md
- docs/client/menu_branding_assets.json
- docs/ai/tasks/menu-branding-assets-20260923.md
- docs/releases/fragments/menu-branding-assets-20260923.md

Future improvement: tune logo scale and background contrast after native preview.
Final local checkpoint follows complete notes and exact scoped diff review; no publishing.
