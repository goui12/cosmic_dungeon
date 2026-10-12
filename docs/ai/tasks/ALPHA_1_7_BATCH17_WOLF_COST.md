# Batch 17 wolf workload evidence

Status: workload implemented; execution results pending CI. No local GameTest, server or client was launched.

## Scope and baseline

`BogatyrIntegrationGameTests.packWorkloads` uses the existing native fixture and real registered, tamed player wolves at pack sizes **1, 30 and 120**. These are enrolled entities returned by the actual owner lookup, not arrays standing in for wolf behavior. All entities remain in the fixture's already ticking chunk; the fixture removes its entities and restores blocks afterward.

The baseline is the currently installed native `OwnerHurtByTargetGoal.canUse()` quiet-defense check for the same actual wolves. The Patrol sample executes production `BogatyrModes.apply(wolf, false)` plus that same check. This compares the managed Patrol overhead with the native defense work it accompanies, not with an invented older release measurement. Settled Stay samples call the same production pre-tick mode update and the installed `StandGround.tick()`; no comparable native Stay command exists, so its time is reported separately without a claimed speedup.

Each state receives 20 warmup sweeps and three samples of 100 full-pack sweeps. Baseline/Patrol order alternates between samples. Logs include each raw elapsed sample and median microseconds per whole-pack sweep. Timings are diagnostics; scheduling/JIT noise must not make CI flaky, and no arbitrary wall-time threshold is asserted.

The Feed Pack sample runs `BogatyrFeed.plan` on the same enrolled pack and real player inventory. Five warmups precede three samples of ten plans. Dense healthy-adult pairing exercises the cell map, eligibility and stock projection; a separate injured-pack execution verifies the successful debit and resulting health on real entities.

## Deterministic acceptance

- Every requested pack size is present in the actual loaded owner directory; no hidden cap.
- Every measured wolf retains native owner defense and has no retired proactive target scan goal.
- Quiet Patrol and settled Stay produce zero retired custom scan/decision/path/node counts through the existing opt-in probe.
- Patrol leaves quiet wolves standing; settled Stay preserves every exact anchor, seating and completed navigation.
- Feed projections do not mutate food or love state, do not reuse a wolf in multiple pairs, and respect the ten-input limit.
- Injured wolves receive food first. Native execution consumes exactly one input per successfully healed one-point wound, up to ten.
- Existing failure-sensitive gameplay tests remain registered separately; the workload does not replace hostile-damage, actual breeding, save/reload or ally-protection coverage.

## Interpretation and limits

These synchronous sweeps measure the actual selected code paths in a headless native Minecraft environment. They **do not advance world time** and are **not live ticks, TPS, maximum supported player count, a complete entity-AI benchmark, or a terrain/pathfinding benchmark**. Entity construction, disk saves, networking, rendering, collision, ordinary movement and full server tick scheduling are outside the timed regions. Stay is settled, and Feed Pack uses a dense healthy pack, followed by injured-pack execution; sparse/no-mate terrain and active recall still require licensed gameplay checks.

No runtime dependency, heap, parallelism, tick budget or scan budget changes are introduced. The probe remains disabled in ordinary play and is always released in `finally`. Bounded diagnostics print only in the explicitly registered CI workload.

## Results

Exact results are retained in the successful Integration Gate and publisher WOLF_INTEGRATION_COST log lines, linked by the main Batch17 completion receipt. The receipt records each1/30/120 row and source/run identity. This source document specifies the benchmark and does not pre-claim a run result from compilation.

Licensed UI/co-op acceptance: **TESTING**. With one and multiple owners, exercise Patrol/Stay/Feed Pack at 1/30/120 wolves on representative dungeon terrain, including recall around obstacles, changed classes, unavailable owners and run reset. Expect correct owner/run separation, fixed Stay anchors, successful food costs only and no lasting packs after reset. Record actual client/server observations separately from this isolated kernel cost sample.

Recommendation: retain these deterministic bounds and optional timing lines as a reproducible regression baseline; profile full native movement on representative licensed terrain only if observed play reveals a material problem. No performance-budget increase is justified by this isolated benchmark.
