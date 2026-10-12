# Help artwork, Batch18

The authoritative page-to-theme mapping is
`src/main/resources/assets/cosmicdungeon/help_artwork.json`.
It explicitly covers all33 reachable pages using23themes. NPC/service pairs share
their own scene; root/Get Started/General/References use the guide theme. Six active
classes each have distinct art and directly reference their existing chest emblem.
No inactive class is added to navigation. Unknown page IDs use the neutral guide.

## Deliverables and provenance

- `masters/*.jpg`: exact1664x936 (16:9) painterly masters, outside the resource/JAR tree.
- `src/main/resources/assets/cosmicdungeon/textures/gui/help/*.png`:480x270 RGB runtime textures.
- `provenance/*.json`: complete built-in imagegen prompts, local canon references,
  actual visual findings and exclusions. Generated prompts mention requested sizes;
  this packaging contract specifies the final distributed sizes.
- `qa/*`: offline contact sheet and representative text/crop mockups. These are
  design checks with a DejaVu Sans font proxy, not Minecraft screenshots.

Three disjoint category workers used one built-in imagegen call per distinct asset;
the scholar scene required targeted correction. Generated1672x941 images receive a
center crop of at most8horizontal/5vertical pixels to exact1664x936, then JPEG95
with chroma subsampling disabled. Runtime PNGs are deterministic Lanczos reductions
from the committed masters. No emblems were regenerated, recolored or edited.
No private source backup is copied into this directory. Local mirror timestamps
are provenance, not a claim of online freshness.

## Integration contract for Batch19

`HelpMenuArtwork` loads this one mapping as immutable metadata; it does not decode,
preload, upload or render textures. The existing H screen is deliberately integrated
in Batch19 together with its responsive layout. Batch18 changes no navigation,
mechanics, packets, saves, recipes, class access or runtime dependencies.

Use `forPage(currentPage.id())`, then `crop(targetWidth,targetHeight)` for centered
cover UVs. Crop does not stretch. Keep key glyphs/text outside the raster art.
Class emblems remain separate original32x32 sprites. Draw the background first,
then the required`0xC0000000` black scrim within text panes, then text/emblems.
The whole-image pixel check proves at least4.5:1 contrast for#B0B0B0 and text of
higher luminance under that scrim; it does not certify arbitrary saturated links.
Check every actual link color and native font in Batch19.

4:3,16:9 and21:9 are representative window ratios; crop math also covers square,
32:9, the current384x240 canvas and GUI scale1-4. Decorative edge details may crop.
Navigation/content geometry and usability at minimum sizes remain Batch19 work.

## Budget and maintenance

Hard regression caps: total runtime PNGs4MiB; all23 decoded RGBA surfaces13MiB.
Final480x270 textures have at most11,923,200bytes (11.3716MiB) of RGBA surfaces
before driver overhead if every theme is eventually cached. One theme is518,400bytes.
No textures are loaded by this batch's metadata preparation; there is no server
work, ticking, extra packet, new runtime dependency or preload cache.

The baseline GUI texture surfaces totaled37,909,952bytes; baseline runtime JAR was
26,053,980bytes. A512x288 prototype exceeded the4MiB file cap;480x270 preserves the
aspect/style while reducing file and potential texture cost. High-resolution masters,
prompts and preview images are never bundled in the runtime JAR. Exact final byte
counts are recorded in`provenance/pack-measurements.json` and the batch receipt.

Run`python scripts/prepare_help_artwork.py` to verify pixels against masters with
Pillow, or add`--write` to intentionally regenerate. Java tests verify reachable
page coverage, unchanged emblem paths, PNG dimensions/contrast/budgets and cover
UVs in normal local/full CI builds without adding a runtime imaging dependency.

Actual licensed UI acceptance remains TESTING: inspect every category, original
emblem, text/link and scrolling pane after Batch19 integration across GUI scales,
resizing and resource-pack/native-font overrides. Offline art checks do not prove it.
