# Known Bugs (Developer Tracking) — 1.4.8

Only issues evidenced directly from implementation behavior, explicit TODO markers, or command/runtime guard rails are listed.

## Active known bugs

1. **Crystal Creeper amethyst-eat interaction uses vanilla amethyst item mapping placeholder**
   - Evidence: TODO in creeper eat-goal indicates current item mapping is placeholder.
   - Developer impact: custom amethyst variants may not participate as expected in that behavior path.

2. **Metalmancer action set is partially stubbed**
   - Evidence: TODO stubs for ore projectile spawn, vortex chaining, golem recall, and ore-based healing.
   - Developer impact: designers should not assume those action branches are production-complete.

3. **Metalmancer summoning staff tier handling is explicitly incomplete**
   - Evidence: TODO indicates future tier expansion path not yet implemented.
   - Developer impact: tier expectations beyond implemented branches can fail silently or behave as fallback.

4. **Spawner intrinsic drop control is display-only for intrinsic rows**
   - Evidence: intrinsic drop UI buttons are inert display elements.
   - Developer impact: intrinsic drop rows in `/spawner drops` are informational, not editable through those controls.

5. **Rift deletion/list operations are dimension-sensitive and can be misread as global**
   - Evidence: command feedback warns chat delete actions are current-dimension scoped.
   - Developer impact: operators can remove/look up wrong target set if dimension context is not verified.

6. **Class selector configuration can fail by distance and slot bounds during remote ops**
   - Evidence: repeated guard failures for distance, slot bounds, and missing block entity.
   - Developer impact: automation scripts must move operator avatar into valid proximity before applying config.

7. **Dungeon snapshot restore can abort when occupancy/chunk drain preconditions are not met**
   - Evidence: lifecycle snapshot service has explicit abort branches for occupants, missing paths, and stalled unload drains.
   - Developer impact: hard reset rehearsals can intermittently fail if prep conditions are not strictly enforced.
