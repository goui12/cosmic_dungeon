# Menu branding and preview

Status: connected for development-client preview. Visual/audio acceptance remains pending.
The supplied art, text and soundtrack are authoritative; no gameplay/canon decision is involved.

## Run the preview

From the repository root with Java 21, run `./gradlew.bat runClient`, or use the Gradle
runClient task in IntelliJ. Refresh the Gradle project first if using an existing generated
IDE Client configuration. The early-loading theme is already prepared for this checkout.

The `prepareCosmicLoadingTheme` task runs before the development client and on IDE sync.
It copies the theme and three existing PNGs into `run/config/fml/`, then sets only
`earlyLoadingScreenTheme = "cosmicdungeon"` in `run/config/fml.toml`.
Modified existing files receive unique `.before-cosmic-*.bak` backups beside them.
Unrelated FML settings are preserved. Nothing is launched by this preparation task.

## Connected behavior

- Title: a dedicated client renderer displays the complete 1024 x 256 title and 512 x 64
  subtitle, scales them proportionally, and positions them above the native menu buttons.
  A narrow client-only mixin replaces just TitleScreen's logo draw call.
- Background: processResources copies the six authored faces to native Minecraft panorama
  paths. Native rotation and the Panorama Speed setting remain in control. The authored
  copies stay in the cosmicdungeon namespace; no PNG is altered by the integration.
- Splashes: processResources aliases the authored ten-line text file to the native path.
  Minecraft selects the phrase and honors Hide Splash Texts. Its date-specific holiday
  phrases remain native behavior.
- Music: only the `minecraft:music.menu` playlist is replaced, with a streamed reference
  to `cosmicdungeon:music/cd_menu_theme`. A client event handler shortens the menu delay
  to about one second under the default frequency setting, retains native Music/Master
  controls, and stops the menu track on world login. The Constant music-frequency option
  retains its native five-second spacing. In-world music definitions are unchanged.
- Startup: the FML 10.0.32 theme replaces Mojang's startup logo with Cosmic Dungeon and uses
  the authored progress bars. The approved silver-bordered cosmic title appears against
  the existing muted lavender background. The inherited NeoForge fox, version and window icon remain, with an added
  "Powered by NeoForge" label. NeoForge carries this theme through the initial resource-load
  transition. Later resource-pack reload overlays retain Minecraft's existing behavior.

The external early-loading theme is development configuration, not part of mod resource
discovery. The built mod JAR contains the menu integration; a future installed-instance
deployment also needs the theme files and FML setting before launch. This pass does not
deploy to CurseForge or a server.

## Panorama preparation

Original screenshots: 1025 x 941. Remove exactly 42 pixels from each side, leaving
x=42, y=0, width=941, height=941. Preserve top/bottom. Every retained pixel was compared
against its source. Uniformly resize to 1024 x 1024 with System.Drawing HighQualityBicubic
and TileFlipXY edge handling. No color, exposure, rotation, warping or generated-content edits.

| Face | Source direction |
| --- | --- |
| panorama_0.png | North |
| panorama_1.png | East |
| panorama_2.png | South |
| panorama_3.png | West |
| panorama_4.png | Up |
| panorama_5.png | Down |

Cameron confirmed capture FOV=90 and explicitly requested activating these prepared images
for inspection. The offline cubemap preview shows some mismatched floor/ceiling joins.
Its cause is not established, and seamlessness has not passed. Originals, backup ZIP,
941-square masters and the diagnostic preview remain in the desktop asset kit.

## Authored resource inventory

Paths relative to `src/main/resources/assets/cosmicdungeon/`:

| Asset | Format / size |
| --- | --- |
| textures/gui/title/cd_minecraft.png | 1024 x 256 PNG |
| textures/gui/title/cd_edition.png | 512 x 64 PNG |
| textures/gui/loading/cd_progress_bar_bg.png | 40 x 20 PNG |
| textures/gui/loading/cd_progress_bar_fg.png | 40 x 20 PNG |
| texts/splashes.txt | Ten UTF-8 phrases |
| sounds/music/cd_menu_theme.ogg | Stereo Vorbis, 44.1 kHz, 3:41 |
| textures/gui/title/background/panorama_0.png through panorama_5.png | Six 1024 x 1024 PNGs |

See [asset provenance](menu_branding_assets.json) for current hashes and the preserved original logo hashes.

## Validation and manual acceptance

Java 21 `build menuBrandingChecks --offline --console=plain` passed. The 29 offline checks
use the installed FML ThemeLoader to parse the theme and check NeoForge credit retention,
native resource aliases, PNG dimensions, soundtrack selection/streaming, and theme copies.
All 1,996 source JSON files parsed. The built JAR contains the expected native aliases.
The native TitleScreen bytecode has exactly one matching logo-render invocation.
A runClient dry run confirms theme preparation precedes launch. No game was launched.

Do these checks in the client:

1. Watch startup: Cosmic Dungeon logo and authored progress bars; NeoForge fox/credit remains.
2. Inspect the title/subtitle and buttons at windowed/fullscreen sizes and your usual GUI scales.
3. Let the panorama turn; check wall seams and any visible floor/ceiling joins.
4. Reopen the title screen to inspect splash selection; test the Hide Splash Texts option.
5. Listen for your song, adjust Music/Master, enter a world, then disconnect.
   Confirm the menu song stops on login and returns in the menu.
6. Reload resources and return to the title screen; check for missing textures or audio.

No saved-data/schema migration, registry IDs, common entry point, networking, authorization,
or gameplay systems change. Dedicated servers do not load the client mixin or music handler.
No datagen is applicable to these hand-authored images/audio/text or build-time aliases.
GameTests were not launched under the local workflow; rendering and audio require the preview.

## Development rollback

Revert this task's source changes, restore the saved `fml.toml.before-cosmic-*.bak` as
`run/config/fml.toml`, and remove only this task's `theme-cosmicdungeon.json` and
`fml/cosmicdungeon/` copies. Do not restore a whole config directory or change world files.
While this integration is active, the preparation task intentionally reselects its theme
before development launches.

Next improvement: use the native preview to tune logo scale and settle panorama alignment.


## Approved logo refresh - 2026-09-24

The approved wider silver/blue/violet title and matching edition artwork replace the two
old dark PNGs byte-for-byte. Title remains 1024 x 256; edition remains 512 x 64, with real
transparency. Existing renderer geometry and theme settings are retained. The preparation
task copies the new title into the early-loading theme. Desktop source originals are kept.
Originals and transfer evidence: sibling CosmicDungeon_AI/backups/menu-logo-refresh-20260924.
Cameron supplied screenshots of the earlier menu/loading integration; the refreshed images
still require his native runClient check for contrast, size and button/splash overlap.

Refresh validation: Java21 build and all 29 branding checks passed; 1,996 source JSON files
parsed, all 12 authored assets match the built JAR, and the loading title matches the approved
PNG. Previous loading title backup verified. FML config/theme bytes and unrelated Git changes
are preserved. No datagen, game, server or GameTest was run for this image replacement.
