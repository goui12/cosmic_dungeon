# Trade currency spacing - 2026-09-25

Cameron requests separated trade sections, the existing inventory account display while
trading, and plain integer counts beside each denomination in both offered-currency rows.
Keep hover details; borderless text means display labels, not editable amount fields.

Branch: fix/trade-currency-layout-20260925, based on 6974c5bf.
Exclusive ownership: TradeMenu presentation coordinates only. TradeScreenLayout is the
shared coordinate/denomination-row helper. No transaction, registry, network, SavedData,
migration, ModNetwork, ModMenus, or CosmicDungeonMod changes.
Files: TradeScreen.java, TradeMenu.java, TradeScreenLayout.java, CurrencyBalanceOverlay.java,
TradeScreenLayoutTest.java, docs/Trading/Trade_GUI_Coordinate_Map.md, this task and its release fragment.
Backup: ../CosmicDungeon_AI/backups/trade-currency-layout-20260925.

The 300 x 238 panel fits minimum normal GUI dimensions. Both nine-item offer rows retain
their indexes. Currency icons and full integer labels have separate bounded rows.
The account uses the exact inventory draw method and owner-only live account sync.
Existing offer clicks, hover, ready/finalize/cancel, avatar preview and protected transfer
rules remain. Partner balance is available on title hover. No new assets or datagen.

Validation: Java21 wrapper build passed; all 57 JUnit cases passed, including three new
layout/integer checks. All 1,997 source JSON files parsed and git diff --check passed.
The build wrapper publishes this candidate to latest-built. No datagen was applicable.
No destructive clean: preserve pre-existing staged historical JAR deletion and generated caches.
No GameTest/dedicated server launch is authorized. Licensed multiplayer appearance, controls,
acceptance/cancellation and item/balance recovery remain user QA. Active client is preserved.
Build wrapper publishes latest-built; current-test advances only after verified TEST/client deployment.
Direct user presentation request governs; live Google canon is not needed for this layout.
Potential future improvement: accept feedback from the two-player minimum-GUI-scale check.
