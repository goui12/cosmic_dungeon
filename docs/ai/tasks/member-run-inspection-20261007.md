# Batch15 - Member run-stat inspection
Authorized October6 exclusive Work queue handoff; Batch15 claimed by work-20261007T0133-cameron-queue-a.
Base6d81b24e2e14fa9172566609bf3923b06c6d7ed3; task branch feature/member-run-inspection-20261007.

## Behavior
Inventory human rows open current-run member details. Server verifies viewer/subject membership,
run identity, active/non-exited state and inventory context; one bounded refresh per second while open.
Response run/UUID/sequence checks and a40tick freshness timeout prevent stale detail presentation.
Health/effects use the established capture; dead/offline/outside-run readings clear live abilities.
Existing Metalmancer cooldowns, ore and native item cooldown percentages are read without mutation.
Existing run kills are reused. Event counters show hostile damage, actual healing received and uncancelled
native deaths. New counters start at zero for pre-update activity; lifetime totals are never substituted.
Duplicate death callbacks are ignored. Existing run retirement clears the additive stat keys.
The detail screen supports wrapping, wheel and visible draggable scrollbar, preserves inventory drag custody,
and returns to the inventory. Recipe-book compact mode stays unchanged until Batch19.

## Ownership and compatibility
Exclusive network registration/payload/client dispatch, roster state and native death/health hooks.
Protocol19 requires matched client/server runtime. Existing D1RunData extensible counts/values own the new
keys; no new SavedData ID/schema, registry/spawner/preset migration, inventory/currency transaction or
authored chest edits. Default old fields read0; save/load and retirement are covered by native tests.
No runtime dependencies, heap changes, per-wolf/entity scans or detail polling outside an open panel.
Datagen not applicable to hand-authored mixin registration and Java UI/network changes.

## Validation and QA
Local Java21 build and full clean CI/native GameTests passed; packet identity/bounds and all existing tests retained.
Exact release CI, upload receipts and independently installed hashes verified. Native additions cover
outsider/stale-run/reset denial, offline state, actual heal/overheal/cancellation, hostile damage,
real/cancelled/duplicate death, old data defaults, save/load and next-run isolation.
Licensed acceptance pending:
1. Click self and teammate human rows; verify identity, health/effect changes and cooldown refresh.
2. Scroll/drag at small/default/large GUI scales; Back restores inventory and carried-item drag stays safe.
3. Relog, die, revive, leave/forfeit and start another run; verify unavailable/stale responses and fresh counters.
4. Confirm matching runtime/helper update delivery through the licensed CurseForge profile.

Google read-only OAuth was expired at the preceding same-day source verification. This approved queue
governs; no fresh Google sync or semantic audit is claimed. No game, local GameTest/server or client launch.
Potential improvement: integrate this view with the shared panel geometry in approved Batch19.
11 cards16-26 remain after Batch15 completion; SetA Beta checkpoint is after16. Continue automatically.

## Exact task files
- docs/ai/tasks/member-run-inspection-20261007.md
- docs/releases/1.6.5-alpha.1.md
- docs/releases/fragments/batch15-member-run-inspection.md
- gradle.properties
- src/main/java/net/goui/cosmicdungeon/client/ModNetworkClient.java
- src/main/java/net/goui/cosmicdungeon/client/screen/D1PartyHud.java
- src/main/java/net/goui/cosmicdungeon/client/screen/PartyHealthHud.java
- src/main/java/net/goui/cosmicdungeon/client/screen/PartyInspectionScreen.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/main/java/net/goui/cosmicdungeon/dungeon/d1/RunMemberStats.java
- src/main/java/net/goui/cosmicdungeon/mixin/DeathCurrencyPlayerMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/RunHealingMixin.java
- src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java
- src/main/java/net/goui/cosmicdungeon/network/PartyInspectionPayloads.java
- src/main/java/net/goui/cosmicdungeon/network/PartyPayloads.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/D1PartyHudService.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/PartyInspectionGameTests.java
- src/main/java/net/goui/cosmicdungeon/npc/tamsin/PartyInspectionService.java
- src/main/java/net/goui/cosmicdungeon/playerclass/metalmancer/MetalmancerActions.java
- src/main/resources/cosmicdungeon.mixins.json
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
- src/test/java/net/goui/cosmicdungeon/network/PartyInspectionPayloadsTest.java

## Final runtime and deployment evidence
Batch15 member current-run inspection is implemented, validated, published and installed as **1.6.5-alpha.1** through [PR233](https://github.com/goui12/cosmic_dungeon/pull/233).
Runtime/tag source: 54caa86fc988cc0e3c3d1e76ed550f8072c2d779. Full Integration Gate 37558694546 and release CI 37559317965 passed all 47 native GameTests; 359 local Java tests, 34 Python safeguards and 2,001 source JSON files passed.
Automatic review completed without findings on the exact runtime source.
CurseForge main 9085566 and loading companion 9085565 submitted; moderation/licensed profile delivery remain unverified.
Exact CI main SHA256 8d9c447c9c600ed34f19642d5de51836d099596f577c8830d9d33c636b47f50d; loading helper SHA256 c4150bb75ecdefdd5df832662b9be40a477d63f0253fd57921f02817f24ab1b7.
Fresh observed stopped TEST and closed ADMIN received the exact runtime; matching helper installed on ADMIN.
Read-only installed hashes match both targets and both latest-built/current-test feeds. Journals resolved.
No server.properties change, server restart, local GameTest/server, client launch or forced close.
Protocol19 requires matching client/server. Existing run storage was extended with compatible additive keys; no new schema/registry/preset migration or datagen.
Licensed visual/interaction/multiplayer QA and CurseForge companion acceptance remain pending.

The final narrative checkpoint uses only the approved Approach A allowlist and its verified ancestor fingerprint.
