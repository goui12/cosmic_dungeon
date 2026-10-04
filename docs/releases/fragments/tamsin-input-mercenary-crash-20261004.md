# Tamsin typing and mercenary pickup crash

Tamsin no longer closes on E or the inventory binding; group-name typing retains E/e, normal editing, Escape and Close. Mercenaries now finish their bounded nearby-entity query before picking up/removing items, fixing the reported server ConcurrentModificationException without changing pickup permissions, capacity or cadence. Java21 build and222 native tests pass, including the native crash reproduction and text-widget input. No saved-data/packet change, migration or datagen. Live TEST acceptance remains pending.

- Mercenaries now reject incoming friendly attacks and harmful friendly potion effects, including
  projectile/cloud ownership. Existing outgoing friendly protection remains; healing is preserved.
- Friendly projectile impacts are suppressed before damage/fire/effects without destroying
  recoverable player ammunition. Hostile and environmental hazards still apply.
