# Authored class-chest correction

Cameron reiterated on2026-09-20: class-chest items are outside AI work.
The previous implementation incorrectly interpreted source alignment as permission
to mark his authored repair materials. This correction removes that behavior.

Parent64cb93298d039cfdf1ace0bc4a8675647dd9a066 on feature/d1-canon-config-20260916.
Final local commit follows validation, completion notes and staged-diff review.

## Changes

- Restore ClassLockedChestBlockEntity.createMenu to its pre-readiness implementation.
- Delete ClassChestRepairSupplies and ChestRepairSupplyRules. No replacement mutation hook.
- Remove ChestRepairSupplyChecks and its runner entry because the entire feature was withdrawn.
- Preserve the requested shift-click implementation, Judicator Lux access and NPC/Watson changes.
- Add the author-owned boundary to AGENTS.md and the source TODO register.
- Remove authored rocket inspection and loadout reconciliation from the AI backlog.
- Reopen R01/M23 as repair-service compatibility; current marker-based validation remains.
  Detailed code TODO requires a solution that never rewrites the authored inputs.

No live world or class-chest contents were opened, inspected or edited during this correction.
No item payload, name, count, component, registry ID, config, resource or packet was changed.
Only the unintended runtime rewrite path was removed; existing repair-component data is
not stripped from any saved item, because it may have been authored or issued legitimately.

## Validation

Java21 offline d1OfflineChecks plus build passed.
16,565 checks in45 groups and two config round trips passed.
The count is310 below the preceding checkpoint because the deleted feature's310 checks
were removed with that feature. Remaining assertions were not weakened.
All1,964 source JSON parse; git diff --check passes.
The built jar contains neither deleted helper and retains ClassChestTransfers.
Chest block-entity source matches the pre-readiness checkpoint exactly.
Shift-click and NPC implementation files remain unchanged.
The two relevant Repair2.0/Dragoon document revisions and artifact hashes were revalidated
unchanged; Cameron's instruction takes precedence over the earlier interpretation.
Local Debloat/Q&A sources remain authoritative and unchanged.

No datagen applies because resources did not change. No new required PNG.
No saved-data/schema migration, client requirement or access-policy change.
No game/GameTest launch, deployment, push or live acceptance test.
Candidate1.5.1 SHA256: cdd4b408ad2eba3c6543cba7a5a72ccfcdd5ee5c36e991e170e56821227b8bce

## Remaining

Five readiness areas; numbered batches scheduled0:
R01 repair-service compatibility; R04 approved recipes; R05 balance displays/treasure decision;
R07 legacy registered-item visuals; R08 native world/NPC/instance acceptance.
R03 and class-chest loadout content are author-owned, not unfinished AI tasks.

Current101 dispositions:45 implemented-unverified,20 partial D1,1 preserved-verification-pending,
8 author-owned/outside AI work,27 deferred D2+.
The historical audit IDs and source evidence remain; they do not authorize chest content work.

Manual acceptance of requested chest behavior still needs ordinary/shift transfers and full
inventory handling in the licensed TEST pass. Do not audit/rewrite the loadouts as part of it.
Repair compatibility must be fixed in its own code path before claiming it works.

A later repair-service fix should recognize approved inputs without changing their metadata.

## Exact changed files

- AGENTS.md
- docs/ai/D1_AUTHORED_CHEST_CORRECTION_2026-09-20.md
- docs/ai/D1_CUMULATIVE_TEST_HANDOFF.md
- docs/ai/D1_IN_GAME_NPC_PLACEMENT_2026-09-20.md
- docs/ai/D1_READINESS_FIXES_2026-09-20.md
- docs/ai/D1_REMAINING.md
- docs/releases/fragments/d1-preserve-authored-chest-items.md
- src/main/java/net/goui/cosmicdungeon/block/entity/ClassLockedChestBlockEntity.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/package-info.java
- src/main/java/net/goui/cosmicdungeon/playerclass/dragoon/repair/ChestRepairSupplyRules.java (deleted)
- src/main/java/net/goui/cosmicdungeon/playerclass/dragoon/repair/ClassChestRepairSupplies.java (deleted)
- src/main/java/net/goui/cosmicdungeon/playerclass/dragoon/repair/RepairComponents.java
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java
- src/test/java/net/goui/cosmicdungeon/playerclass/dragoon/repair/ChestRepairSupplyChecks.java (deleted)
