# Tamsin input and mercenary pickup crash — October 4

Authorized by Cameron during live TEST play. Branch feature/tamsin-input-mercenary-crash-20261004, chained on curated Batch11 source 8932b788 / PR210. Separate scoped task and draft PR; no merge.

Tamsin must never close on E/the inventory binding, with or without text focus. Preserve focused-widget editing, character delivery, Escape and the Close button. The server crash at 2026-10-04 12:02:56 UTC is ConcurrentModificationException in EntitySection.getEntities via MercenaryBrain.nearby and MercenaryCollection.collect; item.discard mutates the collection inside traversal. Snapshot at most the existing 24 pickup / 48 combat candidates before invoking consumers.

Expected files: ClassSelectorScreen, MercenaryBrain, native input/entity-section regression tests, this task card, one release fragment and a focused verification report. No central registration, persistence, packet-format or exclusive transaction changes. Authored gear, protected pickups, four-transfer limits, cadence and existing save formats remain intact; no migration or datagen applies.

Validation: Java21 build and native JUnit without world/server launch; reproduce native entity removal crash, bounded and mutation-safe traversal, actual EditBox E/e input, focused editing and unfocused E. JSON/diff checks, scoped commit/push/draft PR. Match final client/TEST jars and publish current-test only after closed-client/stopped-server deployment guards. An active client will not be closed or duplicated. Cameron controls TEST restart. Live keyboard/pickup acceptance remains pending.

Crash evidence is preserved, ignored by Git, under Google Docs and Sheet/Audit/Testing_Fixes_20261004/crash-2026-10-04_12.02.56-server.txt.
