# Batches32-33: final implementation checkpoint

Completed2026-09-19 on feature/d1-canon-config-20260916.
Cameron authorized both batches; final local commit follows all validation/notes/staged
review. STOP before34. Read the actual commit hash from Git history after that final step.
Verified parent: fcccd96a20c55d162db0f07cf7a1318fa7c689d5 (Batch31).

[Batch32 detail](D1_BATCH_32.md) | [Batch33 detail](D1_BATCH_33.md) |
[Item/source mappings](D1_BATCH_33_MAPPINGS.md) | [Remaining plan](D1_REMAINING.md)

32 closes the unsafe legacy denomination deposit/discard path and makes review explicit.
Exact retired-run intervals prevent mob-reward replay after safe receipt compaction.
33 fixes ammunition identity/alias checks, adds guarded held-stack adoption and verifies
all23 named-loot enchantment signatures. Six authored class loadouts remain preserved.

## Validation and compatibility

10,617 offline checks passed:8,597 baseline +1,400 Batch32 +620 Batch33. Includes99 config
checks plus two separate config round trips. Four new compressed account images exercise
retirement and retained old receipts; earlier suites also pass.
Java21 offline build passed;1,963 source JSON files parse; source hashes/diff/link checks pass.
No gameplay/GameTest/client/server launch, active-world/config edits, datagen, deployment or push.
Datagen is inapplicable because there are no new resources, registrations or asset definitions.
Original tracked1.5.0 jar, generated-cache edit and earlier build/log artifacts remain preserved.

Built candidate1.5.1 SHA256:
085abdb7423c3e584e3299576278a272cffcd2f8169077ae026b8dab8984fd1b

Only the account's optional retired_reward_runs schema1 is new. Old balances/history remain;
no automatic world migration runs. Existing ability/provenance components and item/spawner/
network IDs stay unchanged. Revert tested save upgrades only with a complete matching backup.
Developer authoring remains an explicit, exact-stack, temporary-undo operation, not crash recovery.

Native command/permission/menu/anvil/pickup/container behavior,36 startup pastes, actual
spawner mappings, mixed legacy saves, save failures and multiplayer still require cumulative TEST.
Ambiguous historical entitlement is preserved; no bulk conversion or compensation is certified.
Audit dispositions remain44 implemented/runtime-unverified,21partial D1,9preserved,27deferred=101.

## Exact files changed

- `docs/ai/D1_BATCHES_32-33.md`
- `docs/ai/D1_BATCH_32.md`
- `docs/ai/D1_BATCH_33.md`
- `docs/ai/D1_BATCH_33_MAPPINGS.md`
- `docs/ai/D1_IMPLEMENTATION_20260916.md`
- `docs/ai/D1_REMAINING.md`
- `docs/ai/tasks/d1-canon-config-20260916.md`
- `docs/releases/fragments/d1-batches-32-33-legacy-currency-item-adoption.md`
- `src/main/java/net/goui/cosmicdungeon/command/CurrencyCommand.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/d1/package-info.java`
- `src/main/java/net/goui/cosmicdungeon/economy/CurrencyPickupEvents.java`
- `src/main/java/net/goui/cosmicdungeon/economy/LegacyCurrencyPolicy.java`
- `src/main/java/net/goui/cosmicdungeon/economy/LegacyCurrencyReview.java`
- `src/main/java/net/goui/cosmicdungeon/economy/PlayerCurrencyData.java`
- `src/main/java/net/goui/cosmicdungeon/economy/RetiredRewardRuns.java`
- `src/main/java/net/goui/cosmicdungeon/economy/pricing/ItemTransferRules.java`
- `src/main/java/net/goui/cosmicdungeon/item/identity/D1ItemAdoption.java`
- `src/main/java/net/goui/cosmicdungeon/item/identity/D1ItemAuthoring.java`
- `src/main/java/net/goui/cosmicdungeon/item/identity/D1LootSignatures.java`
- `src/main/java/net/goui/cosmicdungeon/item/identity/ItemMovementRules.java`
- `src/main/java/net/goui/cosmicdungeon/item/identity/ItemProvenanceService.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1AbilityIdentity.java`
- `src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1AmmunitionCatalog.java`
- `src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java`
- `src/test/java/net/goui/cosmicdungeon/economy/LegacyCurrencyChecks.java`
- `src/test/java/net/goui/cosmicdungeon/item/identity/D1AdoptionChecks.java`


The shared ItemMovementRules and D1OfflineChecks files include both batches.
No private Google source bodies, OAuth material, world files, build outputs or logs belong
in this commit. The pre-existing tracked generated cache remains intentionally uncommitted.

## Five planned batches remain

| Batch | Summary |
| --- | --- |
| 34 | Mob rewards and stable NPC/profile bindings |
| 35 | Vendor pricing, conversions, catalogue and calculator checks |
| 36 | Final D1 class effects and ammunition review |
| 37 | Achievements, progression and travel gates |
| 38 | Help, balance displays, performance bounds and cumulative-test handoff |

This estimate excludes the subsequent licensed gameplay-testing phase and all deferred D2+ work.
No new required PNG. Optional tamsin_d1_map.png,512x256, remains a winding Base Camp map
signed -JHW under src/main/resources/assets/cosmicdungeon/textures/gui/; the fallback already works.
A future improvement is a read-only native TEST exporter for exact legacy item and save evidence.
