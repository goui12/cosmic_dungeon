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

## Original deployment and pending gameplay acceptance

Original source ff65c7f9 was installed on TEST and the ADMIN client with identical SHA256
CBB67548CE9FA76CD288514B2067D2FAE53461A3BD2031736219FE8DF07E6772. current-test publication and
updater verification completed. Development-client launch and one retry failed in the native
NVIDIA driver before mod initialization. Cameron subsequently launched the licensed client
and resumed testing. The new follow-up below requires a fresh stopped/closed handoff.

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


## Friendly-fire follow-up and second shutdown investigation

Incoming friendly damage was missing from the existing outgoing mercenary guard. The existing
CompanionAllies classification now protects mercenaries from all players, friendly NPCs and
owned companions, without needing the hirer online/in the active run. Native direct/indirect
projectile and cloud ownership is resolved, while orphaned marked mercenary projectiles remain
unable to regain damage. Ordinary hostile attacks and unowned environmental damage still apply.

Pre-impact filtering prevents friendly non-potion projectile damage and impact side effects.
Incoming player projectiles are not discarded, preserving recoverable ammunition. Permitted
restorative D1 arrows still reach their existing native-wound-suppressing handler. Splash/cloud
mixins reuse the updated potion helper to filter harmful effects per recipient. The native
applicability event also vetoes source-attributed friendly debuffs before they can create delayed
source-less poison damage. Beneficial healing/buffs remain allowed. No general immunity, saved
format change, dependency, polling loop, scan or packet was added. No migration/datagen applies.
Authored equipment and chest stacks remain untouched. No central registrations/mixins changed.

Java21 full build and d1OfflineChecks passed:230 native JUnit tests, zero failures/errors/skips;
all2001 source JSON files parse and scoped whitespace checks pass. Eight added native tests
use actual mercenary/entity/event classes without a level/server launch; existing companion
classification tests cover ServerPlayer and NPC types. Live player attacks, restorative class
arrows, splashes, lingering clouds, hostile combat and fire/knockback still need licensed QA.

The second reported shutdown has no new crash report. Retrieved latest/debug logs end at
12:58:00.951UTC (05:58PDT) with Stopping the server; the client records disconnect and subsequent
connection timeout. No crash stack, watchdog, memory error or fatal mod exception appears there.
This does not establish who/what initiated shutdown or prove the earlier crash recurred. The
hosting-panel console/exit reason is the missing evidence. No speculative crash fix was made.
Logs remain private in Google Docs and Sheet/Audit/Testing_Fixes_20261004/test-*-second-crash.log.

This follow-up changes exactly six files: MercenaryBrain.java, MercenaryPotions.java,
MercenaryFriendlyFireTest.java, this report, its linked task card and the same release fragment.
It stays on PR211 with zero numbered implementation batches remaining. The existing installed
ff65c7f9 build/current-test remains until a fresh stopped-TEST/closed-client deployment.

Pending testing: hit the mercenary with melee, a damaging arrow and poison/harming potions;
health/effects must be unchanged. Check healing potions and restorative D1 arrows still help.
Stand with pets/mercenaries during combat; friendly attacks must not hurt allies, while hostile
mobs/environment can still hurt mercenaries. Repeat the original Tamsin typing and pickup checks.
Future improvement: add this reciprocal combat matrix to the licensed dedicated-server QA fixture.
