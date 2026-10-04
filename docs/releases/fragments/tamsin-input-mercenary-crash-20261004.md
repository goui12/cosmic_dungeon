# Tamsin typing and mercenary pickup crash

Tamsin no longer closes on E or the inventory binding; group-name typing retains E/e, normal editing, Escape and Close. Mercenaries now finish their bounded nearby-entity query before picking up/removing items, fixing the reported server ConcurrentModificationException without changing pickup permissions, capacity or cadence. Java21 build and222 native tests pass, including the native crash reproduction and text-widget input. No saved-data/packet change, migration or datagen. Live TEST acceptance remains pending.
