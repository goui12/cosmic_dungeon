# Batch21 - Group supply requests

Scope: approved card21, feature/group-supply-requests-20261007,1.6.11-alpha.1.
Baseline: Batch20 source d4a504ca4813c5fc5a987fd4c681825a20f9c7f7.
Stop after CurseForge publication and stopped TEST sFTP; do not begin22.

## Behavior and ownership

Theurgists/Bogatyrs request their class resource from actual living members of the
same active dungeon run. Run.orderedPlayers is the canonical active group; lobby
parties no longer exist after run entry. All classes can donate. Same-run dimensions
are allowed; outsiders, dead/offline players, spectators, developers, completed runs
and the applicable D1 seal cannot authorize transfer. Client controls are advisory.

Requests are ephemeral and deduplicated by run/requester/resource/donor. Other valid
requests coexist. Server state caps512 total requests,32 per visible inbox and64
ingredient stacks per card; creation and execution observe these bounds. One
online-player synchronization pass per second sends changed views only, using native
stack copies rather than inventory serialization. A5-tick per-player action gate
bounds consent work. Rejects acknowledge only the initiating client; affected changes
update others. No new runtime dependency, asset or datagen input is introduced.

The universal right inventory column contains boxed cards, named/classed requester
hover, native item/component tooltips, exact credited yield, Accept/Deny and Accept All/
Deny All. Wheel and a clickable draggable scrollbar cover arbitrary card heights.
Narrow creative lanes retain full button labels. Existing left Skills/HUD ownership,
item dragging, clipping and resize behavior remain coordinated. Request Supplies in
Skills is enabled by authoritative life/run/class/cap state; the H guide explains
consent, changed previews and lifecycle cancellation.

Actions bind run, revision and the captured card IDs. At execution the server checks
current membership/class/life, native inventory menu, empty cursor, transaction guards,
exact donor stacks and current recipient headroom. Individual and bulk denial never
consume items. Bulk uses visible creation order; later cards may consume only remaining
quoted stacks/counts, still tagged with identical components. New or changed stacks
require a fresh preview.1 tagged item grants1 resource, only to600. Consent is single
use, and duplicate/stale/wrong-owner actions cannot charge twice. Inventory actions
use a menu-safe guard so native closeContainer side effects do not close the screen.

Death and respawn invalidate requests while retaining monotonic generations within
the same connection. Logout removes its session and involving requests; membership/
completion is pruned on synchronization and rechecked at execution. These ephemeral
requests never survive restart/reconnect. Resource balances retain Batch20 lifecycle.

## Persistence, compatibility and failure behavior

Successful donor/recipient changes use a bounded pending-only schema1 SavedData file,
cosmicdungeon_supply_transfers_v1. This is a transfer decision journal, not another
balance store. The existing player class root adds supply_reservation_v1 while pending
and compact supply_receipt_v1 after settlement. No existing keys, registry IDs, tags,
authored chests/loadouts or spawner schemas change. Missing new keys/file are normal
old saves and need no migration; unknown existing fields are copied unchanged.

Ordering is world prepare proof, both native player reservation proofs, world commit
proof, each native owner image/receipt save and acknowledgement. Full owner reservations
bind the exact before/after images. Recovery decodes native inventory codecs and proves
that only the exact credited count of eligible same-component items disappears;
unrelated slots are identical. Resource images prove exact capped credit and preserve
other fields. Each owner settles once; an already saved receipt cannot debit/credit
again. Partial prepare cancels without changing inventory or resources. Completed
journals remove their images/owner indexes. Pending participants block conflicting
inventory operations/cleanup and death until settled.

Uncertain saves retain journal/reservations and disconnect affected sessions for a
fresh authoritative load; never refund against an uncertain write. Invalid/future
schemas, malformed projections, missing/mismatched proofs or changed eligibility
fail closed and preserve data for manual recovery. Do not delete unresolved journals,
restore only one participant, or roll back across pending transfers. Finish recovery
on this compatible release, then back up the world/player saves together before an
intentional rollback. Older releases ignore additive keys and lack these recovery
guards; rollback with a pending transfer is unsupported. Completed transfers require
no conversion. No background inventory normalization occurs.

## Validation

Required: Java21 full local Gradle build, source JSON/diff checks, full clean Integration
Gate and release native GameTests. Datagen is not applicable: no generated input/output
changes. Unit regressions cover strict journal/plan schemas and copy isolation,
unknown fields, prepare/commit/receipt prefixes, quoted cap-limited bulk, changed stack
components, bounded wire DTOs, UI layout/presentation/generation/input state.
Two isolated CI-only native fixtures cover real bound tags, donor/resource execution,
native payload codec, consent/denial/bulk/stale/lifecycle, absence of container-close
packets and native saved-player recovery for committed and partial-prepared decisions.
Registry serialization retains all previous IDs and includes the two new fixtures.
Actual counts, source/CI IDs and transfer/publication outcomes are recorded together
in BatchRunner receipts; this source report does not pre-claim those outcomes.

## Licensed manual QA - TESTING

1. Enter a three-player run with Theurgist/Bogatyr requesters and any donor class.
   Request supplies: multiple cards coexist. Hover names/classes and every item,
   including custom names/components; exact yield matches the capped resource gain.
2. At supported GUI scales and narrow creative inventory, scroll many/tall cards by
   wheel, track click and thumb drag. Check full labels, clipping, item dragging,
   left Skills/HUD drag/minimize/reset and no unexpected screen closure.
3. Deny one/all without changes. Accept one then replay/stale-click; only one charge.
   Accept All with limited supplies and requester headroom: visible order consumes
   only the shown eligible remainder. Untagged stacks/components are untouched.
4. Change inventory or open a trade/container between preview and accept: reject/
   refresh safely, then retry after closure. Test zero headroom and concurrent
   regeneration; no wasted items or permanently waiting controls.
5. Die/respawn, disconnect/rejoin, leave/complete the run and request again: prior
   consent disappears, counters persist independently, and fresh generations work.
   Outsiders/other runs cannot donate to those cards. Licensed co-op requires TEST.

## Delivery and remaining work

Unique Alpha tag, one CurseForge runtime/helper publisher, then exact runtime to
stopped TEST over pinned sFTP. Record one compact completion and pause after21.
No local-client JAR/helper install, post-publication hashes, restart or image handoff.
Leave Set C PRs unmerged until26. Five cards remain22-26: Theurgist crafting/
resurrection; paid wolf commands/run lifecycle; core modes; advanced modes/performance;
final integration/Beta.

## Exact changed files

- docs/ai/D1_REMAINING.md
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
- docs/ai/tasks/group-supply-requests-20261007.md
- docs/releases/1.6.11-alpha.1.md
- docs/releases/fragments/batch-21-group-supply-requests.md
- gradle.properties
- src/main/java/net/goui/cosmicdungeon/client/ModNetworkClient.java
- src/main/java/net/goui/cosmicdungeon/client/screen/ClassResourceHelp.java
- src/main/java/net/goui/cosmicdungeon/client/screen/requests/SupplyRequestAction.java
- src/main/java/net/goui/cosmicdungeon/client/screen/requests/SupplyRequestsClient.java
- src/main/java/net/goui/cosmicdungeon/client/screen/requests/SupplyRequestsComponent.java
- src/main/java/net/goui/cosmicdungeon/client/screen/requests/SupplyRequestsSnapshot.java
- src/main/java/net/goui/cosmicdungeon/client/screen/requests/SupplyRequestsState.java
- src/main/java/net/goui/cosmicdungeon/client/screen/settings/CosmicDungeonOptionsIntegration.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/ClassResourceClient.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/ClassResourcePresentation.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/SkillsPanelClient.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java
- src/main/java/net/goui/cosmicdungeon/network/SupplyRequestPayloads.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/ClassResourceService.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/SupplyRequestEvents.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/SupplyRequestGameTests.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/SupplyRequests.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/SupplyTransferData.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/SupplyTransferPlan.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/SupplyTransfers.java
- src/main/java/net/goui/cosmicdungeon/transaction/InventoryTransactionGuard.java
- src/test/java/net/goui/cosmicdungeon/client/screen/requests/SupplyRequestsLayoutTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/requests/SupplyRequestsPresentationTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/requests/SupplyRequestsSnapshotTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/requests/SupplyRequestsStateTest.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
- src/test/java/net/goui/cosmicdungeon/network/SupplyRequestPayloadsTest.java
- src/test/java/net/goui/cosmicdungeon/playerclass/resource/SupplyConsentPlanTest.java
- src/test/java/net/goui/cosmicdungeon/playerclass/resource/SupplyTransferPlanTest.java
