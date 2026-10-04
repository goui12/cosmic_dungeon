# Tamsin input and mercenary pickup crash fixes

Cameron's October 4 live TEST report adds two corrections after Batch11. Branch
`feature/tamsin-input-mercenary-crash-20261004` is chained on curated Batch11
`8932b788f05a5638e97126a0382cd7ec6621a25c` / PR210.
[Task card](tasks/tamsin-input-mercenary-crash-20261004.md).
Zero numbered implementation batches remain; these are reported gameplay fixes.

## Findings and changes

The TEST crash at 12:02:56 UTC identifies Mercenary theurgist in run19 and a
ConcurrentModificationException in EntitySection.getEntities through MercenaryBrain.nearby
and MercenaryCollection.collect. Pickup discarded an item inside native section traversal.
The query now snapshots only its bounded candidates, ends iteration, then invokes consumers.
The same fix protects all mercenary classes and both nearby-query callers. The pickup cap
remains24 candidates/four transfers per40ticks; combat remains48 candidates per10ticks.
New entities created by a consumer wait until a later query. Existing alive/permission checks
still run before transfer. There is no full-world scan or asynchronous entity mutation.

Tamsin inherited AbstractContainerScreen's inventory-key closing behavior. Its screen now
consumes E and the configured inventory binding before that fallback, forwards key handling
to the focused widget, and leaves native character events intact. Lowercase/uppercase E can
be typed in the group name. Escape and the explicit Close button keep their existing behavior.

Crash report and post-crash log are preserved outside Git under
`Google Docs and Sheet/Audit/Testing_Fixes_20261004/`.
The log records all dimensions saved, then secondary errors after modules closed. This is
not proof that the hosting panel has completed stopping its server process. No world repair,
entity deletion, configuration change, server control or restart was performed.

## Compatibility and boundaries

No saved-data, entity-NBT, packet, registry, preset or dependency changes; no migration required.
Existing mercenary identity, gear, inventory, timers and run state remain intact. The server
continues enforcing ownership, protected pickups and transfer capacity. No access policy,
class rules, doors/keys, rewards, reset/teleport behavior or currency changes are introduced.
The snapshot temporarily holds at most24/48 existing references at the existing cadence.
Live tick/frame cost and multiplayer acceptance have not been measured.

## Validation

Java21 Gradle build passes:222 native NeoForge JUnit tests, zero failures/errors/skips.
Six new tests cover the native iterator crash reproduction, safe post-query removals,
bounded callbacks/new-entity deferral, nonpositive bounds, unfocused E, and focused E/e
character input plus backspace in the actual native EditBox/ClassSelectorScreen.
All2001 source JSON files parse; scoped staged diff/whitespace checks pass.
No datagen applies. No destructive clean or dedicated/GameTest runtime was launched;
the existing historical JAR deletion, unrelated generated caches and logs remain excluded.

## Deployment and pending gameplay acceptance

Source checkpoint and latest-built publication use the normal pipeline. Installation,
current-test advancement and fresh development-client handoff await confirmed stopped TEST
and a freshly verified closed client. The active client was observed closing during repair;
do not force-close a subsequently reopened game. Cameron controls the hosting panel.

1. At Tamsin type a name such as `Emerald Seekers`, including uppercase E; the dialog remains
   open, editing works, unfocused E does nothing, and Escape/Close still exit normally.
2. Start a dungeon with a Theurgist mercenary and nearby permitted dropped stacks. Pickup
   succeeds without a server crash; full bags, pickup delays and foreign bindings stay protected.
   Repeat with multiple stacks and another class; no duplicate or lost items.
3. Check the inherited curated leaderboard: eight choices, default Dungeons completed,
   mixed-mode Blocks traveled, small GUI scrolling and online/offline player rank pages.

Possible future improvement: a licensed dedicated-server pickup acceptance fixture.

## Exact files (7)

- `src/main/java/net/goui/cosmicdungeon/client/screen/ClassSelectorScreen.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryBrain.java`
- `src/test/java/net/goui/cosmicdungeon/client/screen/TamsinKeyboardTest.java`
- `src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryQueryTest.java`
- `docs/ai/tasks/tamsin-input-mercenary-crash-20261004.md`
- `docs/ai/TAMSIN_MERCENARY_FIXES_20261004.md`
- `docs/releases/fragments/tamsin-input-mercenary-crash-20261004.md`
