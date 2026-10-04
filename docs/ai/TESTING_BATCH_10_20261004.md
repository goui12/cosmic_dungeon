# Testing batch 10 — retained mercenary death, respawn and HUD

Base: Batch 9 / 1b9fb4b00e9a0623f6f81c36d06ed2cdcb377809 / draft PR208.
Branch: feature/testing-fixes-batch10-20261004. Three approved action items implemented.
One implementation batch (11) remains; cumulative licensed gameplay acceptance is separate.
[Approved rollout](TESTING_ROLLOUT_20261003.md) | [Task card](tasks/testing-fixes-batch10-20261004.md).

## Behavior

Active-run party members see up to three mercenary panels below the top-left group HUD.
Each panel shows the mercenary name, hirer, health bar and health numbers or respawn countdown.
Mercenaries occupy their existing party slots without duplicate member rows. Currency moves
below the complete stack. Inventory panels use the existing narrow left margin with clipped
text and full hover text; an open recipe book uses the existing compact panel and detailed
hover tooltip. F1, debug-screen hiding and login/logout clearing retain the existing behavior.
An unloaded active entity is shown as outside the loaded area rather than inventing health.

Death does not invoke native loot/experience drops or remove the mercenary. The original
UUID-bearing entity becomes dormant: no AI, movement, collision, targeting, potion use,
base tick, portals or rendering (including armor/held equipment). Its equipped ItemStacks,
54 supply slots, production/cooldown clocks, contract and run identity remain in their native
storage. There is no second inventory image and no replacement entity to duplicate items.
The stored pre-rest flags are restored when it returns at full health, with fire, freezing,
air, transient effects and damage timers reset. The normal pre-death damage/durability and
totem rules still apply; death retention does not undo prior item consumption.

The ten-minute deadline is 12,000 overworld game ticks. This clock pauses while the server
is stopped; it continues while the hirer is offline or the death chunk is unloaded.
These are implementation defaults using existing server-time semantics, not a newly claimed
user decision. At expiry, the retained mercenary returns beside its living, eligible hirer
in the same active run and dimension. An offline/dead/departed hirer, pending cleanup,
missing original entity or blocked arrival cannot create another mercenary. The HUD shows
Awaiting safe return at zero until a valid return is possible.

Recovery reuses the existing 32-position safe teleport search and the bounded, expiring
Bogatyr companion-loading ticket budget. It does not block on chunk futures or add permanent
forced chunks. Each one-second pass rotates through at most 30 online players, considering
only their own existing contract. This prevents earlier players from permanently starving
later players. Run dismissal clears the locator and discards loaded originals; existing
join admission rejects stale unloaded entities after departure/cleanup.

## Persistence, networking and boundaries

Optional entity mercenary_rest and run mercenary_rests fields add a deadline, dimension,
position and six original flags. Old records default to no rest; existing registry/save IDs,
equipment/supplies/timer fields and human run membership are preserved. Every RunRecord
copy transition retains the locator. Native entity state is authoritative when its save and
the locator save finish in different orders; a loaded original repairs its locator and an
already active original clears a stale due locator. Missing originals fail closed and may
wait for native chunk recovery; this does not promise crash-atomic world saves.

Protocol advances from 11 to 12 because the existing party snapshot now carries up to three
bounded mercenary rows. Matching client/server jars are required. Existing party polling,
delta delivery, recipient selection and native entity tracking carry these updates; there
is no new client action, inventory transfer command, packet stream or SavedData subsystem.
Common server paths contain no client-only imports. Health is informational; eligibility,
death, elapsed time, cleanup and safe return are enforced by the server.

Spawner/preset formats, doors, keys, rifts, authored chest stacks, access policy, accounts,
vendor/trade transactions, progression and skill reward rules are unchanged. No spawner
migration or manual recreation is required. Take a stopped-world backup before first use.
Rollback after saving resting mercenaries requires the matching world backup: older jars
do not understand rest fields and cannot safely restore the saved dormant flags. Existing
pending hire-fee and Batch 8 rollback boundaries still apply.

Work is bounded and throttled: at most three HUD rows, 30 online-player recovery checks
per second, the existing shared load budget and at most 32 arrival candidates per attempt.
SavedData is marked dirty on transitions; normal autosave writes it. No per-tick disk/network
polling, world/entity scans, heap increases, dependencies or profiling agents were added.
Live multiplayer tick/frame cost remains unmeasured.

## Verification

Java 21 test/build passed: 195 native NeoForge JUnit tests, zero failures/errors/skips.
The 13 new tests cover deadline boundaries/overflow, native rest/run/SavedData round trips,
old records, every copy transition, foreign locators, cleanup of locator state, native
packet round trips and row limits, invalid values, countdown text and six-slot HUD geometry.
Native entity death/gear behavior and armor hiding were code-reviewed, not exercised in a
running world. Regression tests do not replace the manual checks below.
d1OfflineChecks passed, including 129 config checks and two configuration round trips.
Its incidental config-example ordering was restored to the verified pre-run bytes.
All source JSON, changed-document links and scoped whitespace are checked.

No generated resources changed; datagen is not applicable. No destructive clean,
GameTest/dedicated-server launch or world entry was performed. Existing unrelated staged
historical JAR deletion and generated-cache edits are excluded from this batch.

Batch 9 source matched origin and fresh installed-client/TEST hashes both matched
AA7947831594325147ED18476BA505F3EE67432F0190398411F76ABDB3CB8E49 before replacement.
The final operational handoff receipt is kept outside Git under
CosmicDungeon_AI/backups/testing-batch10-20261004/final-handoff.json; it records the exact
post-commit build, deployment manifest, current-test artifact, updater and client-startup
results. It is separate from gameplay acceptance and is produced only after those checks.
Two completed diagnostic/build copies were archived losslessly with verified hashes and
path receipts before deployment. Active rollback backups and authoritative sources remain intact.

## Pending licensed TEST acceptance

1. Hire one to three mercenaries, inspect the HUD in-world and in inventory at small/large
   GUI scales, with currency and recipe-book boundaries. Damage each hire: only its health
   changes. Kill it: no items/XP, body, armor or held item remains visible; countdown begins.
2. Check a full ten-minute return with actual named/damaged/component-bearing gear and
   supplies. Unload the death chunk, reconnect and restart the stopped TEST server mid-timer:
   clock resumes correctly, the same UUID/equipment returns once, and potion clocks remain.
3. At expiry test dead/offline hirers and blocked arrival positions, then restore eligibility.
   Forfeit/complete the run during rest: no late respawn or orphan entity. Repeat with two
   independent runs and Bogatyr recovery activity to check shared-budget isolation.

Possible future improvement: add an authorized native death/reload GameTest fixture.

## Remaining implementation batch

11. Server-wide lifetime statistics; teal pause-menu entry; scrollable selectable leaderboards.
This batch has not started. Cumulative gameplay QA for earlier batches remains pending testing.

## Exact files (20)

- [docs/ai/D1_REMAINING.md](D1_REMAINING.md)
- [docs/ai/TESTING_BATCH_10_20261004.md](TESTING_BATCH_10_20261004.md)
- [docs/ai/TESTING_ROLLOUT_20261003.md](TESTING_ROLLOUT_20261003.md)
- [docs/ai/tasks/testing-fixes-batch10-20261004.md](tasks/testing-fixes-batch10-20261004.md)
- [docs/releases/fragments/testing-fixes-batch10-20261004.md](../../docs/releases/fragments/testing-fixes-batch10-20261004.md)
- [src/main/java/net/goui/cosmicdungeon/client/render/MercenaryRenderer.java](../../src/main/java/net/goui/cosmicdungeon/client/render/MercenaryRenderer.java)
- [src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyHud.java](../../src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyHud.java)
- [src/main/java/net/goui/cosmicdungeon/client/screen/MercenaryHudLayout.java](../../src/main/java/net/goui/cosmicdungeon/client/screen/MercenaryHudLayout.java)
- [src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleEvents.java](../../src/main/java/net/goui/cosmicdungeon/dungeon/DungeonLifecycleEvents.java)
- [src/main/java/net/goui/cosmicdungeon/dungeon/DungeonRunRegistryData.java](../../src/main/java/net/goui/cosmicdungeon/dungeon/DungeonRunRegistryData.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryBrain.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryBrain.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEntity.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEntity.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryLifecycle.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryLifecycle.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryRespawns.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryRespawns.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryRest.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryRest.java)
- [src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java](../../src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java)
- [src/main/java/net/goui/cosmicdungeon/network/PartyPayloads.java](../../src/main/java/net/goui/cosmicdungeon/network/PartyPayloads.java)
- [src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyHudService.java](../../src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyHudService.java)
- [src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrRecovery.java](../../src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrRecovery.java)
- [src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryRestTest.java](../../src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryRestTest.java)
