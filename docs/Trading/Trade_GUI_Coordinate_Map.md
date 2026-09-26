# Trade GUI layout

The screen uses a 300 x 238 logical-pixel panel, fitting the normal minimum 320 x 240 GUI.
Coordinates are relative to its upper-left corner. Shared anchors live in TradeScreenLayout;
TradeMenu and TradeScreen use the same positions. No slot indexes or transfer rules changed.

| Area | X | Y | Details |
| --- | ---: | ---: | --- |
| Your account | 8 | 4 | Exact inventory account renderer; icon/integer row and total/available tooltips |
| Partner offer title | 9 | 38 | Name, with full name/account balance available on hover |
| Partner item offer | 9 | 49 | 9 slots, 18px spacing; indexes 0-8; read-only |
| Partner currency offer | 9 | 69 | Anchor, Crown, Seal, Mark, Trace; borderless integers |
| Your offer title | 9 | 92 | Accepted/finalized state at right |
| Your item offer | 9 | 103 | 9 slots, 18px spacing; indexes 9-17 |
| Your currency offer | 9 | 123 | Same denomination row; click icon/count to adjust |
| Inventory label | 9 | 148 | Separate from offered currency |
| Main inventory | 9 | 158 | 3 x 9, 18px spacing; indexes 18-44 |
| Hotbar | 9 | 216 | 9 slots; indexes 45-53 |
| Partner accepted indicator | 274 | 49 | 16 x 16; read-only |
| Accept/finalize | 274 | 103 | 16 x 16; existing two-phase confirmation |
| Cancel | 274 | 123 | 16 x 16; existing protected return path |
| Player preview | 181 | 163 | Ends at 214,232; follows mouse |
| Trade status | 219 | 148 | Wrapped in 72px; full status on hover |

Each currency cell uses a 16px icon, text at icon x+18/y+4, and 4px trailing space,
matching the inventory account row. Counts use BalanceDisplayView.denominations and
Long.toString: no abbreviation, editable textbox, textbox border or item-count decoration.
Rows are recalculated when offered amounts change and on screen initialization.
Only your offer row is interactive: left +1, right -1, Shift +/-10. The server still
validates/clamps offers and blocks changes after acceptance. Your account remains read-only.

The original 256-square trade_window.png is retained unchanged and supplies the slot-frame
sample at 54,26 (18 x 18). Procedural panels provide the additional separation without
stretching the original artwork. Existing accept/deny and denomination assets are reused.

Account updates reuse the owner-only balance synchronization already used by inventory;
no packet, save, registry, transaction, authentication or world-data changes.
Partner and self offer totals remain driven by the existing session-scoped trade payload.
The other player's existing balance is retained in their title hover, not mixed into offers.

The look-at-player CAPS LOCK trade-request prompt is unchanged.
Manual QA: two licensed players inspect both rows, hover every denomination, adjust +/-1
and Shift +/-10, move/shift-click items, accept/finalize or cancel, and verify balances and
returned items. Inspect GUI scales including 320 x 240, long names and multi-digit amounts.
