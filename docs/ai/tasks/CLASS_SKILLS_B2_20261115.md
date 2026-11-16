# Class skills Batch 2 — Theurgist max health

Cameron authorized Batch2, then pause. Baseline is Batch1 commit0b8df5b09474514f173371b561695f5c6de3caad. Task branch: feature/class-skills-b2-20261115. Development checkout: C:\Users\Cameron\Documents\mod_development\cosmic_dungeon_skills_20261115.

## Delivery contract

1.8.10-beta.1 is an unpublished source beta. Local development and GitHub are updated. No TEST server, local-client, CurseForge, release tag or distribution update before Batch6 finishes. Four cards remain: charged combat, shared buffs, independent anti-farming, and party UI/integration/official release. See the [approved plan](CLASS_SKILLS_BATCHES_20261115.md).

## Implemented behavior

- One shared ten-rank table, precisely the approved shifted thresholds: 0/25/35/42/50/55/59/63/66/68 with 10/15/20/25/30/35/40/45/50/55 percent base-health increases.
- Native beneficial effect and registered named splash potions. Fixed54000-tick duration, no healing, stronger-only replacement. Equal or weaker doses cannot refresh duration or earn skill credit; hidden lower ranks do not return after expiry.
- Same-party caster/players/mercenaries/allied wolves within10 blocks of impact receive the full duration. Hostile, unrelated and outside-radius recipients are excluded. Existing ordinary potion protections are retained.
- An attribute modifier supplies the exact percentage of base MAX_HEALTH, with compensation for other finite positive attribute multipliers. Native attribute min/max still apply; degenerate nonpositive multipliers fail safely. The effect updates its own modifier once per second when attributes change, never scans the world, and does not repeatedly modify unchanged attributes. Native expiry/clear removes the modifier and clamps health.
- Durable caster identity/name and rank metadata augment the native effect save; remaining time stays native. Unknown metadata fields survive. Party display of source/time is Batch6.
- The highest unlocked max-health potion is the only craftable rank; server quote and transaction execution enforce this independently of the UI. Rank changes invalidate cached quotes without inventory changes. All ten ranks copy the exact standard Splash Healing routes, ingredients, intermediate inputs and brewing-step fuel cost. Existing recipes and transactional ownership/consent/cursor recovery are reused.
- Player normal/strong/extended positive recipes and mixed drinkable Turtle Master remain available. The existing configurable extra player potion bonus begins at Positive Potions25 with its existing slope/cap; the named potency table applies only to max health.
- Mercenary Theurgists produce/use only Healing and max health. Old unsupported stock stays intact and does not fill the usable-stock cap. Standard Healing below25; at25 the previous quality chance starts at its former first-level value. Positive cadence is max(40,ceil(base/(1+.06*max(0,level-24)))); negative/Venefex quality and cadence are unchanged.
- Merc max-health throws use current rank without rewriting stored bottles. Useful new/stronger effects earn once per cast. Existing resurrection remains intact.
- Instance-local rank unlock notices italicize the new name. English item/effect tooltips show percent/base-health/no-heal/45-minute rules; native health-boost icon is reused through client datagen. Wire protocol37 includes the added registry entries.

Theurgist restorative arrows with overlapping authored names remain separate and untouched. No class-chest stack, spawner, preset, loot or weapon rebalance is included.

## Verification and review

Local Java21 Gradle build and client datagen; source JSON and diff validation. GitHub full Integration Gate must pass on the final exact head, including clean build and native GameTests. Actual totals, logs, commit/PR and CI IDs are recorded in the operational Batch2 evidence and completion receipt.

Unit regressions cover every rank boundary, exactly one craftable rank, all inherited Healing routes/costs, delayed positive bonus/cadence/quality, preserved old stock, and current-rank casts. Native regressions extend existing registered tests: exact base-health arithmetic with another flat and total multiplier, no heal, full duration/radius/recipient boundaries, equal/weaker denial, save/load attribution and unknown fields, native expiry/clamp/no hidden rank, stale craft rejection without debits, live rank-cache invalidation, exact new output and unchanged old bottles.

Independent cooperative review found one multiplier interaction; compensation and the native mixed-modifier regression address it. No additional unresolved source findings remained in that review. No local game, development client, GameTest server or TEST launch occurred.

Performance: one bounded ten-rank selection and one saved player-level read per palette quote; existing quote cache/stock/fuel snapshots retained. One spatial query per max-health splash, only bounded current recipients. One small attribute-modifier scan per second while the effect is active; no new global polling, dependency, thread or network feed.

## Compatibility and manual acceptance

No destructive migration: new potion/effect registry IDs are additive, existing player/mercenary keys and stores remain. Old saves without these effects use normal defaults; ordinary bottles and unsupported merc stock are not rewritten. Source metadata version1 is additive and preserves unknown fields. An older executable cannot load/preserve newly registered potion/effect content reliably: do not save an upgraded world using older binaries. This series is not installed yet.

After Batch6 delivery: inspect the Theurgist potion list at0/24/25/35/68, tooltip wrapping and item names at two GUI scales; craft from water and intermediate bottles, verify shared/fuel/cursor behavior; splash a mixed party at inner/outer radius, check hearts do not fill, compare equal/stronger doses, reconnect and wait for expiry. Verify merc Healing/max-health selection and first cadence bonus at25, unlock announcement, unchanged resurrection and Venefex behavior. Real co-op/GUI/45-minute play acceptance remains TESTING in the [active ledger](BATCH_1_7_20_REVIEW_NOTES_20261012.md).

## File groups

Shared effect/ranks/registry: VitalityPotions, VitalityMobEffect, VitalitySplash, VitalityEffectEvents, ModPotions, ModMobEffects and CosmicDungeonMod wiring. Player support/mixins: SkillPotions and SkillSplashPotionMixin. Crafting/client: PotionRecipeCatalog, PotionCrafting, PotionCraftClient, PotionPaletteModel, ClassAbilityClient and VitalityTooltipClient. Mercenary: brewing, balance, casting, role protection, effective-skill credit and unlock broadcasts. Resources: client effect atlas, language, network protocol and beta version. Existing native potion/crafting fixtures and unit tests provide regressions. Help, class/player guides, release notes, remaining plan and active ledger describe this exact boundary.
