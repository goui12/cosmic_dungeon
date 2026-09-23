# D1 repair compatibility, crafting policy and balance displays

Completed implementation on 2026-09-23. Native gameplay acceptance is **NOT RUN**.
Cameron explicitly authorized all three changes and a final local commit.
Parent commit: 6feca3b38ab0d4e3b78610408db5a6b93a443557.
Branch: feature/d1-canon-config-20260916. This report is part of the resulting checkpoint;
use Git history for its commit hash. No push, deployment or game launch.

## Source authority

Current direct instructions override older marker requirements and Q&A conflicts.
Scope remains the local 20-tab Debloat workbook and its retained linked documents.
The full original Google Sheet and deleted tabs did not become implementation authority.
Both local workbooks and all selected source artifacts retain their original hashes.

Google metadata refresh was attempted but failed because the local OAuth authorization
expired. This pass used verified cached retained bodies and their recorded modified times;
it does not claim that newer Drive edits were checked. The exact metadata, body hashes,
task baseline and logs are in the ignored Audit/D1_Readiness_Implementation_2026-09-23 folder.

- Debloat SHA-256: 976fa26058f7ed624c8aa5aecc57e02caee008ff005591f05da40912e750dcff.
- Q&A SHA-256: c59b81018151350e4df4ae686efd59a06cfdb6887167a1be2d61fc31f815ef8d.
- Economy: 17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28, modified 2026-08-18.
- Trading restrictions: 1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc.
- Repair 2.0: 1Gbcq7Piqg2uHO1smx5oHOyeGxoT9g93cH-_G8WvhOoo, modified 2026-08-18.
- Economy References: 1EqueNwTQafNVfqAxaEW7JZRKena6YqVEx0jpyev5rYQ, modified 2026-08-19.
- Theurgist: 1l9ox2pQUSPy0_J3h7ljPOaVOFtMFkoHq_rFSGK4iqeM, modified 2026-04-05.

## R01: repair compatibility

The shared repair-service validator now accepts unmarked, pristine vanilla oak planks,
spruce planks, cobblestone, leather, gold/copper/iron ingots, diamonds and netherite ingots.
This directly addresses Cameron's instruction and D08 without changing any chest stack.
Named raw materials remain acceptable; display names do not authorize special weapon kits.

Quotation counts, reservation and final custody validation all use that same validator.
Existing marked supplies remain supported. Bow/crossbow/trident/mace kits still require
their valid existing marker and correct pristine underlying item. Ordinary weapons,
malformed markers, damage/enchantments and conflicting attunement/provenance/ability
identity are rejected. Vendor identity and price lookup continue using strict key().

No payment, timing, custody, cancellation, logout return, interrupted-save recovery or
item-delivery algorithm changed. No chest-open, pickup, migration or marking hook was added.

## R04: crafting policy

The generated server CosmicDungeon.config now includes [Crafting]:

- enabled = true.
- playerRecipes contains explicit recipe_id|class_id entries.
- Repeat an ID to permit additional classes; * permits every player for that exact ID.
- automatedRecipes is an independent list of recipe IDs, empty by default.
- Empty enabled lists deny production; no implicit creative/developer bypass.
- enabled = false is an explicit administrator opt-out from this allowlist. Existing repair
  prohibitions and protected-input safeguards still apply.
- Unknown IDs remain denied until a current datapack actually supplies them.
- Each list is limited to 4096 entries; malformed policy parsing fails closed.
- Config changes publish an immutable snapshot on the server thread, so a reload cannot
  interrupt a craft between output transfer and native remainder processing.

Default recipe selection uses these explicit retained reagent instructions:

| Recipe ID | Player class | Retained document | Cached modification |
| --- | --- | --- | --- |
| minecraft:fermented_spider_eye | theurgist | 16BgvpitWMnMv6eivZM59wFxBCTjwguJLbu3MCsusbak | 2026-04-05T14:08:50.330Z |
| minecraft:magma_cream | theurgist | 18AvKUBhfOjJEUaFfsU4fqjpOroyBQOWPariYhQr0dtY | 2026-04-05T14:04:01.586Z |
| minecraft:glistering_melon_slice | theurgist | 1CJTqY839mMPVfEFlm4yU1CyR8yGezFMTy6WWt6jBsVk | 2026-04-05T14:07:29.087Z |

These resolve to the installed vanilla 1.21.10 recipes: spider eye/sugar/brown mushroom;
slimeball/blaze powder; and one melon slice surrounded by eight gold nuggets.
Assigning these reagent recipes to Theurgists and denying unattributed machine production
are conservative implementation choices under Cameron's best-judgment authorization.
Sugar's ingredient use does not approve sugar-cane or honey-bottle conversion recipes.
D2 wolf-armor recipes and D82 custom brewing conversions remain source-backed code TODOs.

Server coverage includes 2x2 inventory, 3x3 table, extra-inventory crafting, recipe-book
autofill, ordinary clicks, quick-move loops, Crafters, furnace/blast furnace/smoker/campfire,
smithing and stonecutting. Player-owned and automated permissions do not authorize each other.
Existing furnace output can still be collected. Campfire's native invalid-recipe path
returns its original input. No recipe-book suggestion is treated as permission.

Current recipe definitions are resolved again before taking an output, including stale
results after class changes or datapack/config reload. Crafters bypass the stale recipe
cache and resolve current recipes before producing anything. Protected or marked repair
inputs cannot be converted. Denied recipes never gain an output from these hooks.
Accepted recipes keep native consumption and bucket/bottle/container remainders;
the policy does not manually decrement, duplicate, discard or replace those items.
Approving a new recipe also approves its native result/remainders, so review conversions
before extending the allowlist. Existing Theurgist brewing, Dragoon repair, Pyroclast
transmutation, anvil naming/enchantment and their guards remain separate services.

This is enforcement for the listed installed vanilla paths. It does not assert that an
unreviewed third-party machine using its own recipe engine is covered.

## R05: account displays

HUD, ordinary survival inventory and class-chest headers reuse the same existing five
registered Anchor/Crown/Seal/Mark/Trace icons used by the trade window. No PNG was added.
The header sits above the inventory/chest slots; its background is drawn in code.

Values come only from CurrencyService and the existing UUID account. They are normalized
without truncating large long-valued account totals. Hover tooltips in screens show exact
denominations, total Trace and available Trace after reservations. The panel is read-only:
no click-to-credit, physical-currency redemption or second persisted balance was introduced.

The server identifies a class chest by its container type, without reading its inventory.
Snapshots go only to the owner and bind chest display to the current container ID.
A revision rejects duplicate/older packets. Disconnect/login clears presentation state;
respawn and dimension travel receive a fresh snapshot on the same account.
The normal refresh uses Performance.menuBalancePollTicks (20 ticks by default).
Unchanged periodic values send nothing. Login, travel, respawn and menu transitions force
a fresh snapshot. Work is bounded per online player; there is no container/world scan.

The existing payload registry/dispatch handles one new S2C packet. Network protocol is
now 7; both client and server require this matching build. Common code has no client imports.
No item, entity, block or SavedData schema changed; no migration is required.

## Validation and limits

- Java 21: gradlew.bat build d1OfflineChecks --offline --no-daemon passed.
- 22,387 checks across 50 printed groups, including 129 config checks and two config round trips.
- New checks: 73 repair-material cases, 53 crafting-policy cases, 76 installed native hook
  signature checks, and 5,016 balance polling/identity/denomination/codec checks.
- Existing repair reservation/custody/interrupted-save checks, trade, commerce, lifetime
  account, dungeon handoff, NPC, spawner and ammunition checks continue to pass.
- All 1,994 JSON files under src parsed; git diff --check passed.
- Datagen was not applicable: no models, definitions, recipe JSON, tags, loot or advancements
  changed. The allowlist references existing recipes. Existing authored language JSON changed.
- No clean task: preserve the historically tracked 1.5.0 jar per AGENTS.md.
- No local GameTest/client/server launch: the licensed TEST workflow is still required.
- Native Mixin application, interaction, remainder delivery, UI positioning, full inventory,
  multiplayer, reconnect/restart and active config watcher behavior remain NOT RUN.
- Active worlds, server.properties, source workbooks, authored chest stacks and deployed
  client/server files were untouched. Pre-existing cache/build/log work stays outside this commit.

Candidate: build/libs/cosmicdungeon-1.5.1.jar
SHA-256: 5cf38cc9989f1800613ab705ce93ce008be7cfe53d217b747828674862ba9275

[Exact manual acceptance steps](D1_CUMULATIVE_TEST_HANDOFF.md).
One readiness area remains: R08 cumulative licensed TEST. Zero planned implementation
batches remain from these three requested areas; this is not a claim that every broad
audit item is runtime-accepted. The historical101 disposition ledger remains45/20/1/8/27
until a separate evidence-backed reclassification.
Future improvement: refine panel placement from licensed TEST feedback.

## Exact files in this checkpoint

- docs/ai/D1_CUMULATIVE_TEST_HANDOFF.md
- docs/ai/D1_READINESS_IMPLEMENTATION_2026-09-23.md
- docs/ai/D1_READINESS_NEXT_STEPS_2026-09-23.md
- docs/ai/D1_REMAINING.md
- docs/config-examples/CosmicDungeon.config
- docs/releases/fragments/2026-09-23-d1-repair-crafting-balance.md
- src/main/java/net/goui/cosmicdungeon/Config.java
- src/main/java/net/goui/cosmicdungeon/client/ModNetworkClient.java
- src/main/java/net/goui/cosmicdungeon/client/economy/CurrencyBalanceClient.java
- src/main/java/net/goui/cosmicdungeon/client/economy/CurrencyBalanceOverlay.java
- src/main/java/net/goui/cosmicdungeon/crafting/CraftingConfig.java
- src/main/java/net/goui/cosmicdungeon/crafting/CraftingEvents.java
- src/main/java/net/goui/cosmicdungeon/crafting/CraftingPolicy.java
- src/main/java/net/goui/cosmicdungeon/crafting/CraftingRules.java
- src/main/java/net/goui/cosmicdungeon/economy/BalanceDisplayPoll.java
- src/main/java/net/goui/cosmicdungeon/economy/BalanceDisplaySync.java
- src/main/java/net/goui/cosmicdungeon/economy/BalanceDisplayView.java
- src/main/java/net/goui/cosmicdungeon/mixin/CraftingAutomationMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/CraftingBookMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/CraftingClickMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/CraftingLookupMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/CraftingPreviewMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/CraftingSmithingMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/CraftingStonecutterMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/CraftingTakeMixin.java
- src/main/java/net/goui/cosmicdungeon/network/CurrencyBalancePayload.java
- src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java
- src/main/java/net/goui/cosmicdungeon/playerclass/api/ExtraInventoryMenu.java
- src/main/java/net/goui/cosmicdungeon/playerclass/dragoon/repair/RepairComponents.java
- src/main/java/net/goui/cosmicdungeon/playerclass/dragoon/repair/RepairMaterialRules.java
- src/main/resources/assets/cosmicdungeon/lang/en_us.json
- src/main/resources/cosmicdungeon.mixins.json
- src/test/java/net/goui/cosmicdungeon/crafting/CraftingHookChecks.java
- src/test/java/net/goui/cosmicdungeon/crafting/CraftingPolicyChecks.java
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java
- src/test/java/net/goui/cosmicdungeon/economy/BalanceDisplayChecks.java
- src/test/java/net/goui/cosmicdungeon/playerclass/dragoon/repair/RepairMaterialChecks.java
