# Batch 3 - mercenary recovery and transactions

Authorized after verification of Batch 2. Dependent branch feature/mercenary-recovery-20261005, based on Batch 2 source1b46cedb; release1.5.5-alpha.1. Stop after Batch 3.

## Scope and integration ownership

Own mercenary revival/regeneration services, entity combat/recovery hooks and configuration; PlayerCurrencyData's existing account-operation/ledger integration; party HUD/payloads and server actions; ModNetwork and client dispatch; delegated GameTest registration. Single writer, no concurrent hotspot edits.

New hires default to 50 Trace; revival is half the immutable original contract fee (integer Trace, rounded down for custom odd prices). Existing contracts retain their historical paid terms. Existing configuration overrides remain readable; deployment will set the known ADMIN/TEST hire price to the explicitly requested50 without touching server.properties.

One donation covers the entire displayed revival. Active participants anywhere in the same run, including its linked dimensions, may donate; the hirer must also be inside the run and eligible. Safe return remains pending until the hirer is back in the companion death dimension. Both help confirmation and donation recheck that the hirer still lacks available funds and is transaction-ready. Ownership, contract, run, exact death UUID and deadline, account holds and cleanup state are rechecked server-side. A stale button or chat request cannot target a later death/run. Chat asks are limited to one per death per30seconds and request/save-retry maps are capped at30.

Payment and entitlement are one terminal operation in the existing account save. Verified flush precedes paid recovery; a failed flush waits for retry without a second debit. Existing operation codecs preserve these acknowledged records and prior receipts. The original dormant UUID, supplies, armor and flags are resumed through the existing bounded companion-loading and safe-return logic; no replacement entity, duplicate equipment, paid currency transfer or separate ledger.

Healing uses the existing10-tick AI decision cadence, with15seconds quiet and0.5health per subsequent second. Valid hostile targets and successful incoming/outgoing wounds reset it. No offline catch-up; reload and revival restart the quiet delay. Native healing clamps to maximum. No new entity scans, dependencies or background I/O.

## Compatibility and source evidence

Network protocol14 adds bounded recovery controls and the insufficient-funds prompt. Dedicated common paths contain no client imports. No registry IDs, save versions or spawner/preset formats change. MercenaryRest adds optional death UUID, defaulting to zero for existing dormant saves; all old fields are retained and native saves resave automatically. Every new death gets a fresh UUID, including two deaths in one tick. Account operation shape remains unchanged; old account/run/entity codecs are tested. Preserve a whole-world backup before rolling back to older binaries; older code ignores the new optional identity and cannot provide the new paid-recovery guarantees. No authored class-chest edits, rift/door/key changes, class-access changes or progression changes.

Google read-only metadata refresh on2026-10-05 failed because the saved authorization expired. Tamsin/currency cached hashes and failure are recorded privately in Audit/BATCH3_RECOVERY_SOURCE_REFRESH_20261005.json. Explicit Cameron requirements govern; no fresh canon alignment claimed.

## Validation and QA

Local Java 21 build passed: 290 native tests and 2 loading-helper tests, zero failures; 11 publisher checks and 2001 source JSON files passed. CI clean build and GameTests are required before publication. Diff checks passed. Datagen is unrelated (code, protocol and prose only). No local GameTest/server/client launch authorized or performed.

Manual QA pending:
1. At Tamsin, hire for50Trace; start a dungeon and verify the actual debit.
2. Let the hire die. Inventory shows Revive (25 Trace), including minimum GUI/recipe-book layouts. Pay once; the same named/equipped companion returns safely and only25Trace is charged.
3. With fewer than25available Trace, click Revive then Yes. Group chat names that hire; a funded ally can donate once. Test donors in the linked Nether and a funded hirer after the prompt; two near-simultaneous donors, repeated clicks, outsiders and an old request after another death cannot double-charge or revive the wrong death.
4. Relog/restart during waiting recovery and verify the persisted entitlement and unchanged equipment. Test full/unsafe return area and normal free ten-minute recovery.
5. Damage a hire, wait15seconds, then observe half a health point per second. New combat interrupts it; maximum health is never exceeded.

Six batches remain: shared skills; Theurgist potions; Bogatyr skill; Pyroclast fireworks; Dragoon lightning; Theurgist resurrection. Future improvement: complete licensed CurseForge companion update QA once moderation finishes.

## Exact changed source files

- `docs/ai/CURSEFORGE_AND_MERCENARY_BATCHES_20261004.md`
- `docs/ai/D1_REMAINING.md`
- `docs/ai/tasks/mercenary-recovery-20261005.md`
- `docs/releases/1.5.5-alpha.1.md`
- `docs/releases/fragments/mercenary-recovery-20261005.md`
- `gradle.properties`
- `src/main/java/net/goui/cosmicdungeon/client/ModNetworkClient.java`
- `src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyHud.java`
- `src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java`
- `src/main/java/net/goui/cosmicdungeon/economy/MercenaryRevivePayment.java`
- `src/main/java/net/goui/cosmicdungeon/economy/PlayerCurrencyData.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryBrain.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryConfig.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEntity.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryRecoveryGameTests.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryRegeneration.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryRespawns.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryRest.java`
- `src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryRevival.java`
- `src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java`
- `src/main/java/net/goui/cosmicdungeon/network/PartyPayloads.java`
- `src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyHudService.java`
- `src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyLobby.java`
- `src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyService.java`
- `src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java`
- `src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryRecoveryTest.java`
