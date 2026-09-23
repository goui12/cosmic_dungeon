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
