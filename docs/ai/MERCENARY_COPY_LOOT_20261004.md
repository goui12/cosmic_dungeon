# Mercenary useful container copies

Requested during licensed TEST play on October 4, after the mercenary appeared to remove a barrel's contents and door key. [Task card](tasks/mercenary-copy-loot-20261004.md).

## Behavior

Mercenaries copy usable contents from permitted nearby containers and their assigned starter chests. Source counts, names, components and ownership are preserved. Players can still remove the originals. Unopened native loot tables wait for player opening so the mercenary cannot roll player loot/luck. Container extraction vetoes and class-chest ownership still apply.

Acquisition accepts class-usable weapons, armor/shields, attack ammunition the existing AI can fire, potions and native brewing inputs/reagents. Door keys and any lock-ID-bearing stacks are always rejected, including on the ground. Currency, food, torches, unusable rockets, wrong-class items and gear downgrades are ignored. Gear upgrades use native replacement rules; previous equipment goes into the mercenary's supplies only if it fits. Existing authored starter order remains stable and unused duplicate gear is not copied.

Ground items remain actual pickups rather than free copies. Existing acquired items, including any previously stolen key, are preserved; this change cannot infer their original container and does not retroactively return them.

## Persistence and bounds

MercenaryEntity adds optional mercenary_loot_version (1), mercenary_loot_blocked and mercenary_loot_claims fields. A receipt records dimension, block position and slot. Each mercenary can copy a slot once; consumption, container refilling, rest and reload do not reset it. Failed/full-inventory acquisitions record nothing. Different mercenaries have independent copies. Starter chest slots are reserved together to prevent reseeding.

Old saves without these fields load with empty history and retain existing supplies/equipment. Invalid versioned receipt data fails closed for future container copies. At 4096 receipts, further container copying stops without evicting history; ground pickups continue. Back up the world before an update. Rolling back to older code removes this protection on subsequent old-version saves; restore a matching backup for a full rollback.

Existing collection cadence (40 ticks), loaded-space scan, line-of-sight checks, four acquisitions per poll and bounded item snapshots remain. No runtime dependency, packet, client-only import, extra scan or networking traffic added. All acquisition remains server-side. Cosmic Spawner, door, preset, player progression, currency/vendor and teleport storage are unchanged; those systems require no migration.

## Verification and remaining QA

Java21 Gradle build passes with 243 native JUnit tests (zero failures, errors or skips), including 13 new container-copy tests. All 2001 JSON files under src parse, and git diff --check passes. Source/JAR receipts are recorded privately with the task evidence. Tests cover original stack preservation and player extraction, keys/unusable ground items, class/ownership checks, independent mercenaries, full bags, native upgrade decisions, starter copying, unrolled loot tables, extraction vetoes, old receipt-free saves, malformed versioned data, round trips and the receipt bound. Starter tests were updated to the user-requested useful-only behavior while retaining ownership guards.

No generated resource changes: datagen is not applicable. Build preserves the historical tracked JAR; destructive clean was not used. Native unit tests load NeoForge without launching a world/server. Local GameTest server and licensed gameplay were not run. Existing unrelated build/cache/log/config status remains outside this checkpoint.

Pending gameplay checks:
1. Start a dungeon with the Theurgist; confirm starter equipment and normal combat/brewing progression.
2. Approach a barrel containing usable supplies and a door key, then open it: all originals remain and the player can take/use the key.
3. Let ammunition/potions be consumed; revisit and reload without another copy of the same slot. A different mercenary gets its own copy.
4. Check equipment upgrades and a full inventory; verify friendly-fire protection and ordinary hostile damage still work.

The previous unexplained Akliz shutdown remains a separate unresolved investigation; this change is not claimed as its fix. TEST installation and live acceptance are recorded separately from build/source completion. A future improvement could expose a small mercenary supply summary if gameplay demonstrates a need.
