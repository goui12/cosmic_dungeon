# Batch 8 — mercenary entry

Authorized 2026-10-04; chained on Batch 7 / PR206. Two items: hiring/payment/roster slots; own starter room and actual authored equipment. One hire per member, one slot, configurable 500 Trace charged only for a successful start. Tamsin controls use existing server-validated sessions. Movement, combat, brewing, death recovery and stacked health HUD remain batches 9–10.

Exclusive owner: this task. Hotspots: party payload codecs/protocol; entity registration/attributes/client renderer; Config; dungeon startup/slot chest bindings/lifecycle; run and currency persistence. Expected directories: npc/tamsin, client/screen, network, dungeon, mercenary, economy, entity; related tests/docs/release fragment. No other writer. No authored stack rewriting, spawner changes, server.properties or runtime dependencies.

Validation: Java21 build and native JUnit capacity, payload, persistence/payment and exact-stack tests; all source JSON, offline checks, diff/link checks. Datagen only if generated resources are affected. Manual TEST QA: hire/release/full roster, failed/successful entry payment, room/chest equipment and restart. No dedicated/GameTest launch authorized. Preserve unrelated existing work and historical staged JAR deletion.
