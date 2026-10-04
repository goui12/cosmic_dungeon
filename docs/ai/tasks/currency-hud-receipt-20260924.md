# Currency HUD receipt timer

Cameron's explicit runtime feedback on 2026-09-24: show the gameplay currency bar for
about five seconds after receiving currency, then hide it; always show inventory balance.

Scope: client CurrencyBalanceClient and CurrencyBalanceOverlay, pure OOP
CurrencyHudVisibility, its deterministic clock tests, this record and a release fragment.
No central integration hotspot changes. Parent branch includes the tested GameTest
registry/world-entry repair and the menu branding work. Preserve all unrelated changes.

Behavior: accepted owner/revision-validated increases in total balance start/restart
five seconds of monotonic real time. Initial login balance is a baseline. Equal totals,
spending, reserved/spendable changes and opening menus do not restart the timer.
Disconnect/login clears both visibility and baseline; same-session respawn retains the
account snapshot. Inventory and class-chest render paths keep their existing visibility.
The timer also expires while other screens are open or the game is paused.

Uses existing server-confirmed balance snapshots, so notification starts on receipt of
the next snapshot (existing poll interval). It does not invent transaction events that
are not present in the protocol: a gain offset by spending before a snapshot may not
produce a net increase. No server polling, packet schemas, currency/account mutation,
saved-data, chest contents, registries, dependencies, or migration changes.

Validation completed: Java 21 build and menuBrandingChecks passed. Native JUnit ran
five CurrencyHudVisibilityTest methods (including 5,016 existing owner/revision/balance
checks) and one GameTestSerializationTest method (93 codec checks), all with zero
failures or skips. Menu/loading checks passed 34 + 8 assertions. Scoped diff reviewed;
existing inventory/class-chest screen rendering is unchanged.
No datagen or JSON changes. Do not interrupt his active world; allow save/quit before restart.
Evidence: sibling CosmicDungeon_AI/backups/currency-hud-receipt-20260924.

## Completion and remaining manual checks

Exact files: CurrencyBalanceClient.java, CurrencyBalanceOverlay.java and new
CurrencyHudVisibility.java under src/main/java/net/goui/cosmicdungeon/client/economy;
new src/test/java/net/goui/cosmicdungeon/client/economy/CurrencyHudVisibilityTest.java;
this task record; docs/releases/fragments/currency-hud-receipt-20260924.md.

No JSON or generated resources changed, so JSON validation and datagen are not
applicable. Build used the non-destructive build target under tracked-JAR protection.
No dedicated server or GameTest server was launched; those retain separate authorization.
No saved-data formats changed and no migration is required. Existing account authority,
owner/revision checks and networking are unchanged. No common/server client-class imports,
access-policy, classes, teleportation, spawners, doors, vendor/trade transactions,
progression, achievements, entity/block-entity persistence or authored chest-content edits.

The prior client saved and exited cleanly at 22:25:35 local time. Relaunch current build
under standing runClient authorization. Automated results do not establish visual or
multiplayer acceptance. Cameron should verify:
1. Login with an existing balance: no receipt flash; inventory balance remains visible.
2. Earn currency: gameplay bar shows about five seconds then hides.
3. Earn again before expiry: timer restarts; spending alone does not reopen/extend it.
4. Open inventory past expiry and close: inventory balance stays visible; HUD stays hidden.
5. Save/rejoin and test inventory/recipe-book GUI scales; later perform licensed multiplayer QA.

Separate world-entry follow-up: the registry serialization exception has not recurred,
but the prior session logged a client chunk-readiness timeout before allowing entry.
Track any further terrain-loading hang separately; do not claim this warning resolved.
No push or deployment performed. Preserve unrelated staged historical-JAR deletion and
generated caches/logs. Final local checkpoint follows validation and scoped staging.
Possible future improvement: an optional brief fade after the five-second display.
