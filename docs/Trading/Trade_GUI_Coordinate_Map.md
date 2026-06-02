# Trade GUI Coordinate Map

Coordinate origin is the upper-left corner of `trade_window.png`. Coordinates are zero-based texture pixels. Slot `x`/`y` values are the 16x16 item-render anchor positions visible inside each 18x18 slot frame.

## 1. Asset inventory

- trade background filename/path: `src/main/resources/assets/cosmicdungeon/textures/gui/container/trade_window.png`
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

- clickable own currency denomination bounds: not represented in the PNG; current procedural fallback renders five 16x16 fake `ItemStack` controls in a vertical left-gutter column at x=8 and y=76,94,112,130,148 relative to the screen origin, ordered Anchor, Crown, Seal, Mark, Trace.
- read-only other currency denomination bounds: not represented in the PNG; current procedural fallback renders five 16x16 fake `ItemStack` displays in a vertical left-gutter column at x=8 and y=17,35,53,71,89 relative to the screen origin, ordered Anchor, Crown, Seal, Mark, Trace.
- offered currency summary bounds: not represented in the PNG; current procedural layer renders compact 16x16 fake `ItemStack` summaries horizontally near the offer boxes at x=56,74,92,110,128 y=47 for the partner and x=56,74,92,110,128 y=108 for the local player, ordered Anchor, Crown, Seal, Mark, Trace. Tooltip hitboxes cover each full horizontal row from x=56 through x=145 and y=47-62 or y=108-123.
- other accept status indicator bounds: x=228, y=28, width=16, height=16; status-only, never clickable, and rendered dim/off until the other player accepts.
- own accept button bounds: x=228, y=89, width=16, height=16; left-click sends ready first, then confirm/finalize after both players are ready.
- own deny button bounds: x=228, y=107, width=16, height=16; left-click sends cancel.
- player preview bounds: x1=28, y1=72, x2=52, y2=124; rendered with `InventoryScreen.renderEntityInInventoryFollowsMouse` so it stays left of the offer and inventory slots without covering the vertical currency controls.
- hover borders: no separate hover textures found; the screen draws procedural 1px/2px borders around the own accept/deny buttons and own denomination controls only. The other accept status indicator never receives a hover border.

## 7. Current implementation

- TradeMenu/TradeScreen imageWidth/imageHeight: 256 x 256 for this asset.
- TradeMenu slot constants:
  - other offer start: x=56, y=28, spacing=18, count=9
  - own offer start: x=55, y=89, spacing=18, count=9
  - player inventory start: x=55, y=126, spacing=18, rows=3, cols=9
  - hotbar start: x=55, y=184, spacing=18, count=9
  - implemented container-index layout: other offer slots 0-8, own offer slots 9-17, player inventory slots 18-44, hotbar slots 45-53.
  - other offer slots are display-only; they reject placement, pickup, removal, and shift-click transfers.
  - own offer slots reject placement/pickup/click changes while the local player's offer is ready/locked; legal item changes before acceptance reset both players' ready/confirm state.
  - server-to-client trade state sync drives names, Trace balances, Trace offers, ready/confirm status, and status text; item offers remain synchronized through the container slots.
  - local and partner balances are normalized from server-synced Trace totals into Anchor/Crown/Seal/Mark/Trace fake `ItemStack` displays in the left gutter; only the local balance column has hover borders and click handling, and its bounds stay left of the offer slots so denomination clicks do not consume slot clicks.
  - local denomination clicks send `C2S_AdjustCurrencyOffer` with a denomination id plus signed delta count: left-click adds 1, right-click removes 1, and Shift changes the delta magnitude to 10. While the local offer is accepted/ready these controls are locked client-side and rejected server-side; otherwise the server validates the denomination, converts through `CurrencyDenomination`, clamps the resulting Trace offer to `[0, player balance]`, resets ready/confirm state on changes, and syncs both players.
  - offered currency summaries are fake horizontal `ItemStack` rows only; they are not real slots and only show tooltips containing formatted offered amounts.
  - the default vanilla title/inventory labels and vanilla inventory background are suppressed; the PNG background is the GUI. The screen uses the standard container render path so the PNG background renders first, real slots/items render next, and custom currency controls, button art, synced partner/self names, compact ready/finalize status, and hover borders render above the slots.
  - the local player preview uses the same 1.21.10 rectangle API pattern as `ExtraInventoryScreen` and follows the mouse.
  - the own accept icon uses the existing two-phase server-authoritative model: first click sends ready for the current offer, and once both sides are ready the same icon sends confirm/finalize. The deny icon sends cancel, returns offers through the server session, and closes both screens.
  - the other accept icon is only a synced status indicator: dim/off when the partner has not accepted and on/outlined when the partner has accepted. It has tooltip text but no click behavior.
- TradeScreen texture ResourceLocations:
  - background: `cosmicdungeon:textures/gui/container/trade_window.png`
  - accept button: `cosmicdungeon:textures/gui/gui_accept.png`
  - deny button: `cosmicdungeon:textures/gui/gui_deny.png`
  - currency icons: render `ItemStack`s from `ModItems` rather than referencing item texture paths directly.
- Button and denomination hover rendering: draw hover/selected borders procedurally around the placed own-side controls unless dedicated hover PNGs are added later; no separate hover textures are present in the searched assets.
## 8. Look-at-player HUD trade prompt

- `TradeLookPromptOverlay` is a client-only visual hint, not part of `trade_window.png` and not a clickable world menu.
- It reuses `TradeRequestKeybindClient.getLookTradeTarget` so the prompt only appears when the crosshair is on another player within 3 blocks, never for self, and not merely because another player is nearby.
- The prompt is hidden whenever a screen is open or the debug overlay is visible.
- The overlay draws a small translucent box centered above the hotbar with `<OtherPlayerName>` and `Press CAPS LOCK to request trade`. It sends no packets; only pressing the registered CAPS LOCK trade-request key can send the look-target trade request packet.
