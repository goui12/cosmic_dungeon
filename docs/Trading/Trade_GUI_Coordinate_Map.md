# Trade GUI Coordinate Map

Coordinate origin is the upper-left corner of `Trade_Window.png`. Coordinates are zero-based texture pixels. Slot `x`/`y` values are the 16x16 item-render anchor positions visible inside each 18x18 slot frame.

## 1. Asset inventory

- trade background filename/path: `src/main/resources/assets/cosmicdungeon/textures/gui/Trade_Window.png`
  - texture width/height: 256 x 256
  - non-transparent drawn bounds: x=47, y=18, width=177, height=190; inclusive pixels x=47-223, y=18-207
- accept button texture path(s): `src/main/resources/assets/cosmicdungeon/textures/gui/gui_accept.png`
  - texture width/height: 16 x 16
  - non-transparent drawn bounds: x=1, y=1, width=14, height=14; inclusive pixels x=1-14, y=1-14
- deny button texture path(s): `src/main/resources/assets/cosmicdungeon/textures/gui/gui_deny.png`
  - texture width/height: 16 x 16
  - non-transparent drawn bounds: x=1, y=1, width=14, height=14; inclusive pixels x=1-14, y=1-14
- disabled/grayscale accept texture path(s), if present: none found under the searched texture folders.
- hover texture path(s), if present: none found under the searched texture folders.
- currency item texture paths:
  - `src/main/resources/assets/cosmicdungeon/textures/item/attunement_anchor.png`: 16 x 16
  - `src/main/resources/assets/cosmicdungeon/textures/item/attunement_crown.png`: 16 x 16
  - `src/main/resources/assets/cosmicdungeon/textures/item/attunement_mark.png`: 16 x 16
  - `src/main/resources/assets/cosmicdungeon/textures/item/attunement_seal.png`: 16 x 16
  - `src/main/resources/assets/cosmicdungeon/textures/item/attunement_trace.png`: 16 x 16

## 2. Trade screen dimensions

- imageWidth: 256
- imageHeight: 256

## 3. Other player section

- text anchor for `Trading with: <name>`: not represented in the PNG; no exact pixel anchor can be derived from the provided assets.
- other currency row/icon coordinates: not represented as a distinct currency row/icon in the PNG; currency should be rendered from `ModItems` item stacks when implemented.
- other currency count/text coordinates: not represented in the PNG; no exact pixel anchor can be derived from the provided assets.
- other offer slot coordinates, 9 slots total:
  - slot 0: x=56, y=28, bounds x=54-72, y=26-44
  - slot 1: x=74, y=28, bounds x=72-90, y=26-44
  - slot 2: x=92, y=28, bounds x=90-108, y=26-44
  - slot 3: x=110, y=28, bounds x=108-126, y=26-44
  - slot 4: x=128, y=28, bounds x=126-144, y=26-44
  - slot 5: x=146, y=28, bounds x=144-162, y=26-44
  - slot 6: x=164, y=28, bounds x=162-180, y=26-44
  - slot 7: x=182, y=28, bounds x=180-198, y=26-44
  - slot 8: x=200, y=28, bounds x=198-216, y=26-44
- other accept status icon bounds: not represented in the PNG; no exact status-icon bounds can be derived from the provided assets.
- other deny/cancel/status bounds, if present: not present in the PNG.

## 4. Own player section

- player preview rectangle bounds: not represented in the PNG; no exact preview rectangle can be derived from the provided assets.
- own name text anchor: not represented in the PNG; no exact pixel anchor can be derived from the provided assets.
- own currency row/icon coordinates: not represented as a distinct currency row/icon in the PNG; currency should be rendered from `ModItems` item stacks when implemented.
- own currency count/text coordinates: not represented in the PNG; no exact pixel anchor can be derived from the provided assets.
- own offer slot coordinates, 9 slots total:
  - slot 0: x=55, y=89, bounds x=53-71, y=87-105
  - slot 1: x=73, y=89, bounds x=71-89, y=87-105
  - slot 2: x=91, y=89, bounds x=89-107, y=87-105
  - slot 3: x=109, y=89, bounds x=107-125, y=87-105
  - slot 4: x=127, y=89, bounds x=125-143, y=87-105
  - slot 5: x=145, y=89, bounds x=143-161, y=87-105
  - slot 6: x=163, y=89, bounds x=161-179, y=87-105
  - slot 7: x=181, y=89, bounds x=179-197, y=87-105
  - slot 8: x=199, y=89, bounds x=197-215, y=87-105
- own accept button bounds: not placed in the background PNG. The available `gui_accept.png` control art is 16 x 16 with non-transparent pixels x=1-14, y=1-14 relative to its own texture.
- own deny button bounds: not placed in the background PNG. The available `gui_deny.png` control art is 16 x 16 with non-transparent pixels x=1-14, y=1-14 relative to its own texture.

## 5. Player inventory section

Main inventory 27 slots:

| slot purpose | container index | x | y |
| --- | ---: | ---: | ---: |
| main inventory row 0 col 0 | 18 | 55 | 126 |
| main inventory row 0 col 1 | 19 | 73 | 126 |
| main inventory row 0 col 2 | 20 | 91 | 126 |
| main inventory row 0 col 3 | 21 | 109 | 126 |
| main inventory row 0 col 4 | 22 | 127 | 126 |
| main inventory row 0 col 5 | 23 | 145 | 126 |
| main inventory row 0 col 6 | 24 | 163 | 126 |
| main inventory row 0 col 7 | 25 | 181 | 126 |
| main inventory row 0 col 8 | 26 | 199 | 126 |
| main inventory row 1 col 0 | 27 | 55 | 144 |
| main inventory row 1 col 1 | 28 | 73 | 144 |
| main inventory row 1 col 2 | 29 | 91 | 144 |
| main inventory row 1 col 3 | 30 | 109 | 144 |
| main inventory row 1 col 4 | 31 | 127 | 144 |
| main inventory row 1 col 5 | 32 | 145 | 144 |
| main inventory row 1 col 6 | 33 | 163 | 144 |
| main inventory row 1 col 7 | 34 | 181 | 144 |
| main inventory row 1 col 8 | 35 | 199 | 144 |
| main inventory row 2 col 0 | 36 | 55 | 162 |
| main inventory row 2 col 1 | 37 | 73 | 162 |
| main inventory row 2 col 2 | 38 | 91 | 162 |
| main inventory row 2 col 3 | 39 | 109 | 162 |
| main inventory row 2 col 4 | 40 | 127 | 162 |
| main inventory row 2 col 5 | 41 | 145 | 162 |
| main inventory row 2 col 6 | 42 | 163 | 162 |
| main inventory row 2 col 7 | 43 | 181 | 162 |
| main inventory row 2 col 8 | 44 | 199 | 162 |

Hotbar 9 slots:

| slot purpose | container index | x | y |
| --- | ---: | ---: | ---: |
| hotbar col 0 | 45 | 55 | 184 |
| hotbar col 1 | 46 | 73 | 184 |
| hotbar col 2 | 47 | 91 | 184 |
| hotbar col 3 | 48 | 109 | 184 |
| hotbar col 4 | 49 | 127 | 184 |
| hotbar col 5 | 50 | 145 | 184 |
| hotbar col 6 | 51 | 163 | 184 |
| hotbar col 7 | 52 | 181 | 184 |
| hotbar col 8 | 53 | 199 | 184 |

## 6. Hover/click regions

- clickable own currency denomination bounds: not represented in the PNG; no exact bounds can be derived from the provided assets.
- read-only other currency denomination bounds: not represented in the PNG; no exact bounds can be derived from the provided assets.
- accept hover border bounds: no separate hover texture found; button placement is not represented in the background PNG. The accept art itself is 16 x 16.
- deny hover border bounds: no separate hover texture found; button placement is not represented in the background PNG. The deny art itself is 16 x 16.
- denomination hover border bounds: no separate denomination hover art found and no denomination region is represented in the background PNG.

## 7. Implementation recommendation

- TradeMenu imageWidth/imageHeight: use 256 x 256 for this asset.
- TradeMenu slot constants:
  - other offer start: x=56, y=28, spacing=18, count=9
  - own offer start: x=55, y=89, spacing=18, count=9
  - player inventory start: x=55, y=126, spacing=18, rows=3, cols=9
  - hotbar start: x=55, y=184, spacing=18, count=9
  - if the menu uses custom trade slots before player inventory, a natural menu-index layout is other offer slots 0-8, own offer slots 9-17, player inventory slots 18-44, hotbar slots 45-53.
- TradeScreen texture ResourceLocations:
  - background: `cosmicdungeon:textures/gui/Trade_Window.png`
  - accept button: `cosmicdungeon:textures/gui/gui_accept.png`
  - deny button: `cosmicdungeon:textures/gui/gui_deny.png`
  - currency icons: render `ItemStack`s from `ModItems` rather than referencing item texture paths directly.
- Button hover rendering: draw hover/selected borders procedurally around the placed controls unless dedicated hover PNGs are added later; no separate hover textures are present in the searched assets.
