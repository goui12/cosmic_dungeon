# Batch35: conversion ceilings and pricing references

Completed in code and offline-validated; checkpoint finalized 2026-09-20 UTC
(September19 on the PC). Cameron authorized Batch35 only; stop before36.
Verified parent: 91075b5cb86a92ab648387e40c578d364d46cc07, branch
feature/d1-canon-config-20260916.

## Changes

- B35-1: native Sweeping Edge now resolves to the existing sweeping configuration.
  This fixes rejection of otherwise valid equipment, including Cold Comfort, without
  renaming any registry ID, config section or saved item.
- B35-2: vendor quotes apply reviewed conversion ceilings to nine cooked foods,
  milk/bottle returns and13 already-configured potion variants. The calculation accounts
  for three-bottle yield, one twentieth of a blaze powder per brew, returned dragon-breath
  bottles, cheaper intermediate potions and the lowest configured faction retail floor.
  Exact decimal arithmetic keeps fractional fuel until the final whole-Trace floor.
  Cooking uses the zero-fuel campfire route, so furnace fuel cannot justify a higher payout.
- B35-3: disabled or unknown input prices reject their dependent conversions. A disabled
  potion does not disable unrelated D1 prices. Existing lower per-item caps still win.
  Vendor restrictions apply only to vendor sales; existing player-trade eligibility and
  freely negotiated prices remain separate. Quotes expose list value and the adjustment.
- B35-4:182 source-versioned price rows (141 items,41 enchantments), calculator boundary
  fixtures and native identifier checks preserve manual/category exceptions. All23 named
  final prices were separately reconciled against the newer August28 source. The candidate
  catalogue remains reference material and never creates stock or pricing eligibility.

## Configuration

All prices remain in server all_vendors_prices.config; existing faction multipliers
remain in CosmicDungeon.config. No additional configuration file or balance knob.
The sole changed numeric default is Universal.bucket.purchaseTrace:3 ->50, matching the
documented list price. Its live default ceiling remains3. Existing operator values are
retained; no active config was edited or replaced. Exported examples are reference only.
The existing Enchantments.sweeping section also prices native sweeping_edge.
Glow Berries were already correctly priced; the interrupted draft's duplicate was removed.

These are default quoted ceilings, not changes to the source list prices or retail stock:

| Item/variant | Listed purchase | Effective ceiling |
| --- | ---: | ---: |
| Bucket | 50 | 3 |
| Glass Bottle | 5 | 3 |
| Healing | 18 | 16 |
| Healing II | 20 | 18 |
| Regeneration | 18 | 16 |
| Night Vision | 30 | 30 |
| Fire Resistance | 14 | 12 |
| Splash Healing | 20 | 17 |
| Lingering Healing | 30 | 26 |
| Strength | 13 | 11 |
| Extended Water Breathing | 13 | 11 |
| Invisibility | 48 | 33 |
| Splash Poison | 16 | 14 |
| Splash Invisibility | 49 | 34 |
| Lingering Regeneration | 30 | 26 |

The final six potion rows already had purchase prices but remain excluded from D1 retail
stock by the existing profile. This pass adds no D2 feature or stock. Derived ceilings can
be more conservative than one particular recipe: they must also cover cheaper alternative
inputs and container cycles. A null retail route imposes no retail-cost constraint; an
unknown consumed-input price is never treated as an approved zero price.

## Source authority and reference checks

The debloated20-tab MASTER and66-answer Q&A retained their original SHA256 hashes.
Thirteen relevant Google Docs were refreshed read-only and unchanged. SOURCE_REVIEW.json
separates body/section review from metadata checks and earlier review; retrieval is not
reported as semantic coverage. Pricing Master2.0 modified2026-08-19 governs base values
and its explicit conversion rule. Dungeon Dropped Gear modified2026-08-28 governs final
named prices. Q&A D83 forbids the container exploit; D65/D82 keep later content deferred.

Calculator18KJWpZghx4O8-5cL9KiXn71A7HwG2sizVQn-lGeOkU0, version20, modified
2026-08-20T21:41:16.938Z, was fetched into private evidence. All1509 formulas were inventoried;
250 repeated rows match six native templates without mismatches. Formula/manual/zero/
override examples were evaluated independently. No Excel recalculation is claimed.
The original calculator, MASTER and Q&A were not edited. No runtime network dependency.

The1489-entry Non-Exhaustive Item List targets26.2 and is not an approved vendor catalogue.
Spears/Lunge require a later-version decision; unpriced rods, shovels, books and rockets
remain unpriced. Fortune remains excluded. Three old links to the deleted Master Item
Sheet remain recorded at Venefex Item Sheet!A1, Metalmancer Item Sheet!A1 and Deadeye Item
Sheet!A1. The source workbook was preserved; no deleted tab or inferred equipment was restored.

## Validation and limits

Java21: gradlew.bat d1OfflineChecks build --offline --console=plain passed.
11,933 offline checks, including1,139 new source/calculator/conversion checks, plus two
separate config round trips. All1,963 source JSON files parse. The validation receipt
records source hashes, exact file inventory, config semantic comparison, local report
links, diff checks and preservation of unrelated tracked files.

Checks cover exact-integral and fractional strict retail boundaries, returned outputs,
500 independent integer-arithmetic oracle cases, missing/disabled/zero inputs, live offer
overrides and aliases, bounded config reads, native Sweeping Edge mapping, named final
values, item caps and vendor/player-trade separation. The first run found a duplicate
draft entry/count mismatch; it was corrected before the successful checks. The initial
timeout did not create a commit.

No game/client/server/GameTest launch, deployment, push, world edit or active config edit.
Datagen was not applicable: no item, model, recipe, tag, loot or registry resource changed.
No new required PNG. Optional tamsin_d1_map.png remains512x256: route to Base Camp, signed
"-JHW"; its existing drawn fallback remains usable.

## Compatibility and cumulative licensed TEST

No save schema, registry ID, network payload, spawner, preset or item-component format
changed. No migration is required. Existing authored equipment and chest contents remain.
The server recalculates prices at quote/confirmation; the work is bounded to a fixed
conversion graph and memoized config reads, with no new tick scanner or persistent cache.
Native latency and multiplayer behavior remain unmeasured.

Before release, in a complete licensed TEST world copy:

1. Sell valid Cold Comfort/Sweeping Edge equipment; verify the retained config override.
2. Compare displayed list/adjustment/total for milk, buckets, honey, bottles and D1 potions.
   Test partial stacks, zero surrender, cheaper inputs and changed config between confirmations.
3. Brew one/two/three bottles, mixed variants and both ingredient/container orders; verify
   fuel use and returned bottles, including lower existing per-item caps and reload/restart.
4. Verify player trades retain their existing eligibility when vendor conversion is unpriced.
5. Inspect actual D1 crafting access, including ExtraInventoryMenu, automated crafters and
   data packs. General crafting is disallowed by Gear Trading2.0. Custom recipes/fuels and
   component-cap washing need explicit review before enabling any additional conversion.

M12 remains partial for legacy/world/native conversion coverage. M106 remains partial for
source-link and authored-item context. Detailed code TODOs record these release gates;
this finite vanilla schedule is not a proof covering every Minecraft recipe.
M107 reference tooling is implemented/runtime-unverified. All101 IDs remain accounted for:
45 implemented-unverified,20 partial D1,9 preserved-verification-pending,27 D2+ deferred.

[Three planned batches remain](D1_REMAINING.md):36 class effects/ammunition;
37 achievements/progression/travel;38 displays/performance and cumulative TEST handoff.
Future improvement: automate native recipe-graph comparison before approving custom conversions.

## Exact file inventory

- `docs/ai/D1_BATCH_35.md`
- `docs/ai/D1_IMPLEMENTATION_20260916.md`
- `docs/ai/D1_REMAINING.md`
- `docs/ai/tasks/d1-canon-config-20260916.md`
- `docs/config-examples/all_vendors_prices.config`
- `docs/releases/fragments/d1-batch-35-pricing-conversion-reference.md`
- `src/main/java/net/goui/cosmicdungeon/config/VendorCatalog.java`
- `src/main/java/net/goui/cosmicdungeon/config/VendorPricesConfig.java`
- `src/main/java/net/goui/cosmicdungeon/economy/pricing/VendorPricingService.java`
- `src/main/java/net/goui/cosmicdungeon/economy/pricing/VendorConversionRules.java`
- `src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java`
- `src/test/java/net/goui/cosmicdungeon/economy/pricing/VendorPricingReferenceChecks.java`
- `src/test/resources/d1/pricing-master-2026-08-19.csv`
