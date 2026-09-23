# Menu branding assets

Status: assets imported for later client integration. These files do not yet replace
Minecraft's title screen, splash selection, music, or early loading theme.

Cameron's desktop kit is the authoritative source for this import. All six original
screenshots remain intact. A ZIP backup of all 12 user inputs and pixel-exact 941 x 941
crop masters remain in the desktop kit's timestamped preparation folder/backup.

## Panorama preparation

Remove exactly 42 pixels from the left and 42 from the right of each 1025 x 941 image.
Crop rectangle: x=42, y=0, width=941, height=941. No top/bottom cropping.
Every retained pixel was compared with its corresponding original pixel.
Uniformly resize the square to 1024 x 1024 with System.Drawing HighQualityBicubic and
TileFlipXY edge handling. Final imagery has no color, exposure, or generated-content edits.

| Face | Source direction |
| --- | --- |
| panorama_0.png | North |
| panorama_1.png | East |
| panorama_2.png | South |
| panorama_3.png | West |
| panorama_4.png | Up |
| panorama_5.png | Down |

The user confirmed 90-degree capture FOV. The mapping follows Minecraft's native
panorama capture order when the initial yaw faces north. Up/down orientation is
preserved exactly as supplied, with no speculative rotations.
An offline cubemap preview was rendered and inspected. Visible mismatched joins remain,
especially at the floor/ceiling boundaries; seamlessness has NOT passed. The preview alone
does not identify the cause. Preserve the requested crop and investigate capture position,
effective projection, and pole orientation before enabling this panorama. The diagnostic
preview is brightened 2.5x for inspection only; no imported image has an exposure adjustment.
Native-client acceptance of all 12 joins remains pending.

## Imported files

All paths below are relative to src/main/resources/assets/cosmicdungeon/.

| Asset | Format / size |
| --- | --- |
| textures/gui/title/cd_minecraft.png | 1024 x 256 |
| textures/gui/title/cd_edition.png | 512 x 64 |
| textures/gui/loading/cd_progress_bar_bg.png | 40 x 20 |
| textures/gui/loading/cd_progress_bar_fg.png | 40 x 20 |
| texts/splashes.txt | UTF-8 text |
| sounds/music/cd_menu_theme.ogg | Stereo Ogg Vorbis |
| textures/gui/title/background/panorama_0.png | 1024 x 1024 |
| textures/gui/title/background/panorama_1.png | 1024 x 1024 |
| textures/gui/title/background/panorama_2.png | 1024 x 1024 |
| textures/gui/title/background/panorama_3.png | 1024 x 1024 |
| textures/gui/title/background/panorama_4.png | 1024 x 1024 |
| textures/gui/title/background/panorama_5.png | 1024 x 1024 |

The ten splash phrases are preserved byte-for-byte, including their original filename.
The cd_ prefix is retained on authored title, loading-bar, and music files.
Vanilla examples, the original music sample, and NeoForge credit artwork are not imported.

## Integration boundaries

This step adds client assets only. Existing help-menu art is unchanged.
No Java classes, event handlers, registries, sound definitions, packets, client configs,
world data, schemas, or migrations are changed. Dedicated-server behavior is unchanged.
The images/music will remain unused until a dedicated client integration task wires them.
That task must retain the NeoForge fox/icon and deliver early-loading theme files before
startup. Gameplay systems, authored chest stacks, and all persistence are outside scope.

## Verification

See menu_branding_assets.json for exact file hashes and Ogg integrity results.
PNG dimensions and copy hashes passed; 941 x 941 crop pixels matched every source pixel.
The original-input backup was CRC-tested and compared byte-for-byte.
Ogg page checks verify file integrity, not audible playback quality.
The task record lists build/JSON/package results and outstanding manual acceptance.

Manual acceptance after wiring: launch the licensed client, inspect all panorama joins,
confirm both logos at supported GUI scales, test splash selection and Music/Master sliders,
and enter/leave a world to check music lifecycle and the early-loading transition.
No client/server/GameTest was launched by this asset import. Datagen is unrelated to these
hand-authored PNG, Ogg, and text assets.
