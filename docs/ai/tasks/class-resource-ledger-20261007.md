# Batch20 - Class resource ledger, recycling and bars

Scope: approved card20, feature/class-resource-ledger-20261007,1.6.10-alpha.1.
Baseline: Batch19 source56ca66f04d7cdf40e2943b31674161ef198abb46.
Stop after CurseForge publication and stopped TEST sFTP; do not begin21.

## Behavior and boundaries

Theurgist Brewing Supplies and Bogatyr Kibble begin at0 for each new run. The
server checks class, active run membership/dimension, completion exit and applicable
D1 seal; only the player's class resource regenerates1 per20 online server ticks
to600. Dead run members retain/accrue balances. Sessions retain no persisted clock:
logout discards timing, relog binds a new clock and adds no offline catch-up.
One bounded online-player pass per second performs in-memory deltas only.

Recycling validates the current run/resource/action generation, life, native inventory
menu, empty cursor and existing transaction guards at execution. It plans from the
actual bound item tag across native player inventory/equipment, validates all original
stacks before changing any, consumes1 item per resource only to remaining capacity,
and preserves untouched components/stacks. Unknown, stale and duplicate tokens cannot
credit items twice. Rejected current tokens receive a fresh authoritative acknowledgement.
Regeneration does not invalidate an action generation. A5-tick action gate bounds
save work; passive updates do no disk I/O.

Inventory and credited balance share one native player save. Serialization failure
rolls back before any disk operation. Uncertain save outcome retains the paired
state, blocks inventory replacement/cleanup on that session and disconnects for a
fresh authoritative player load; it never refunds items against an uncertain save.
No resource/credential/world/chest audit, helper server install or authored-stack
normalization runs in the background.

Generated brewing_supplies contains exactly sugar, rabbit_foot,
glistering_melon_slice, spider_eye, blaze_powder, golden_carrot, ghast_tear, pufferfish,
magma_cream, turtle_helmet, phantom_membrane, breeze_rod, stone, cobweb,
fermented_spider_eye and slime_block. Kibble contains exactly rotten_flesh, beef,
porkchop, mutton, chicken and rabbit. No fish/cooked meat enter the Kibble tag.
Server runServerData produces the two tags; no hand-authored duplicate exists.

Server snapshots update the world/expanded/minimized display at most1 second apart
when a balance changes. Native shaded herb and Kibble geometry avoids new assets or
dependencies. HUD placement observes native health/armor/air stack height. Rich
tooltips preserve requested yellow text and blue-bold resource names/counts.
The existing client-only gate wires packets/actions; common code loads no client types.
H-guide supply lists derive from the currently bound server tags, cache per session
and invalidate on tag reload. Existing page identities and authored guide text remain.
Request Supplies carries the agreed tooltip but is disabled pending card21.

## Compatibility and validation

Additive schema1 lives at NeoForgeData.cosmicdungeon.class_resources with run_id,
balances and revision. Old saves without the key initialize0; missing optional
balance/revision fields default0. Known balances are validated before conversion;
unknown fields are copied and unsupported/malformed schemas fail closed without
rewriting. New runs reset only known balances/token. Existing Clone events already
copy the whole class root and therefore retain balances through death.
No world migration, registry rename or unloaded-chunk conversion is needed.
An older release ignores this additive key; rollback loses the new UI/mechanics
while preserving unrelated NBT. Restore a pre-upgrade player backup if intentionally
discarding resource progress. Never replace player saves while a server is running.

Java21 runServerData, full local build, source JSON/diff checks, full clean Integration
Gate and release CI/native GameTests are required. Unit coverage exercises old saves,
round trips, unknown/future/corrupt data, cap overflow, online timing/reset, staged
recycling rollback and stale stacks, bounded payloads, generated-guide content,
rich tooltips, snapshot/action state and HUD geometry. Two isolated CI-only native
fixtures exercise bound tags, execution rejection/acknowledgement, actual inventory/
resource save pairing and death/relogin/new-run lifecycle. The existing registry
serialization test includes both fixture IDs. All prior tests remain registered.
Actual counts, source/CI IDs and publication/transfer receipts are recorded together
in BatchRunner evidence; this source report does not pre-claim those outcomes.

## Licensed manual QA - TESTING

1. Start a fresh Theurgist/Bogatyr run. Check0/600,1/sec increments, class isolation,
   and matching world/expanded/minimized counts. Inspect supported GUI scales, drag/
   minimize/reset, extra health/armor/air, item dragging and rich hover tooltips.
2. Carry tagged and untagged stacks, including named/component-bearing supplies.
   Recycle at595 with more than5 eligible items: exactly5 disappear and600 is shown;
   at600 nothing is consumed. Fish/cooked meat never convert to Kibble.
3. Die and respawn, then disconnect/rejoin after a delay: saved balances persist
   without offline gain. Complete/leave the run: no active resource UI or recycling.
   Enter the next run: both known resource balances reset to0.
4. Race a click with death, cursor/container changes and other transactions; no
   duplicate credit/item loss, stale action, or permanently waiting button.
5. Compare the H guide with server tags, including after an authorized tag reload.
   Request Supplies stays unavailable. Verify existing Skills/group/account controls
   and recipe-book suppression remain correct. Multiplayer acceptance requires TEST.

## Delivery and remaining work

Unique Alpha tag from reviewed task source, one CurseForge runtime/helper publisher,
then exact published runtime to stopped TEST via pinned sFTP. Save one compact
completion receipt and stop. No local client copy, post-publication hashes, restarts
or image handoff. Leave Set C PRs unmerged until26.
Six cards remain21-26: group requests; Theurgist crafting/resurrection; paid wolf
commands/run lifecycle; core wolf modes; advanced modes/performance; final integration/Beta.

## Exact changed files

- docs/ai/D1_REMAINING.md
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
- docs/ai/tasks/class-resource-ledger-20261007.md
- docs/releases/1.6.10-alpha.1.md
- docs/releases/fragments/batch-20-class-resources.md
- gradle.properties
- src/generated/resources_server/data/cosmicdungeon/tags/item/brewing_supplies.json
- src/generated/resources_server/data/cosmicdungeon/tags/item/kibble.json
- src/main/java/net/goui/cosmicdungeon/client/ModNetworkClient.java
- src/main/java/net/goui/cosmicdungeon/client/screen/ClassResourceHelp.java
- src/main/java/net/goui/cosmicdungeon/client/screen/HelpRichTextRenderer.java
- src/main/java/net/goui/cosmicdungeon/client/screen/settings/CosmicDungeonOptionsIntegration.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/ClassResourceBar.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/ClassResourceClient.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/ClassResourcePresentation.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/ClassResourceSnapshot.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/ClassResourceState.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/SharedInventoryLayout.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/SkillsPanelComponent.java
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/SkillsPanelModel.java
- src/main/java/net/goui/cosmicdungeon/datagen/ModItemTagProvider.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/main/java/net/goui/cosmicdungeon/network/ClassResourcePayloads.java
- src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/ClassResourceEvents.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/ClassResourceGameTests.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/ClassResourceKind.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/ClassResourceLedger.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/ClassResourceService.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/OnlineResourceClock.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/ResourceRecycling.java
- src/main/java/net/goui/cosmicdungeon/transaction/InventoryTransactionGuard.java
- src/main/java/net/goui/cosmicdungeon/util/ModTags.java
- src/test/java/net/goui/cosmicdungeon/client/screen/ClassResourceHelpTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/ClassResourceBarTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/ClassResourcePresentationTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/ClassResourceSnapshotTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/ClassResourceStateTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/SkillsPanelModelTest.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
- src/test/java/net/goui/cosmicdungeon/network/ClassResourcePayloadsTest.java
- src/test/java/net/goui/cosmicdungeon/playerclass/resource/ClassResourceLedgerTest.java
- src/test/java/net/goui/cosmicdungeon/playerclass/resource/OnlineResourceClockTest.java
- src/test/java/net/goui/cosmicdungeon/playerclass/resource/ResourceRecyclingTest.java
