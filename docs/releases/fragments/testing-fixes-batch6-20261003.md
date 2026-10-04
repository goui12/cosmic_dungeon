# Batch 6 - Difficulty and completion tiers

Leaders choose Easy, Hard, Insane or Ridiculous at Tamsin. Each adventure freezes
its own configurable health, damage, wave count/delay, harmful-effect duration,
blindness and armor-wear settings. Changing difficulty clears readiness.
Bosses remain single-spawn; authored mob caps, spawner/preset data and loot rules stay intact.

Successful Dungeon 1 completion grants the selected tier and all lower tiers.
Offline recovery retains the chosen tier. Existing NORMAL completion records map
to Hard and Easy; old runs and handoffs load with the unchanged baseline.
Protocol 10 requires matching client/server builds. Optional persisted profile/tier
fields are backward compatible; no spawner migration or server.properties change.
