# Batch33: trusted D1 item adoption

Completed in code and offline-validated on 2026-09-19. Cameron initially requested33, then
explicitly included32. Both batches share the final local commit; STOP before34.
Verified parent: `fcccd96a20c55d162db0f07cf7a1318fa7c689d5` (Batch31),
branch `feature/d1-canon-config-20260916`. No Batch32 completion was assumed from numbering.

## Changes

- B33-1: central catalog for18 D1 ammunition identities; corrected Venefex aliases.
  Invalid explicit markers no longer fall back to names. Vanilla base/potion namespaces,
  custom effects and rocket star counts are checked. Four existing custom registry
  identities remain compatible; unrelated mod items cannot impersonate D1 ammunition.
- B33-2: developer-only `/d1 item ability <identity>` previews one existing held vanilla
  stack. Apply/undo reuse exact-stack, selected-slot, session, dimension/run, cursor,
  recovery, expiry and replay checks. Only the stable ability marker is added.
  Legacy recognized ammunition now receives the shared no-drop/private-storage rules.
- B33-3: new named-loot adoption requires exact approved enchantment signatures for23
  catalog entries. Existing saved provenance, configured purchase prices and retail
  classification remain intact. `/d1 item inspect` reports identity and enchantment data.
- B33-4: [source mappings](D1_BATCH_33_MAPPINGS.md) cover ammunition, six retained class
  chest lists, startup template naming,23 drops and explicit unknown world bindings.
  Detailed code TODOs preserve Cinderkiss/Cinderbight ambiguity and deferred D2+ work.

## Sources and scope

Both native workbooks retain their sealed hashes:
MASTER `976fa26058f7ed624c8aa5aecc57e02caee008ff005591f05da40912e750dcff`;
Q&A `c59b81018151350e4df4ae686efd59a06cfdb6887167a1be2d61fc31f815ef8d`.
Retained20-tab scope and Q&A D19/D37-D39/D42-D44/D48/D63/D79 govern.
D79 explicitly preserves developer-authored chest contents and duplicate quantities.

33 relevant linked Docs were refreshed:32 unchanged, one Theurgist overview downloaded.
18 document bodies were read for this batch;15 received metadata checks only.
The private SOURCE_REVIEW.json distinguishes those statuses and records revisions/hashes.
Six chest lists contribute687 nonblank lines within42 source camp sections, including
headers/quantities/repeats; LOADOUT_SOURCE_ROWS.json/CSV retain exact source lines.
This is not a claim that every item document or actual world stack has been verified.

Newest Theurgist overview keeps existing Verdant configuration; no balancing values,
vendor prices or operator configuration changed. Source room coordinates are reference
locations, not approved container ownership or spawner UUID assignments.

## Validation

Standalone33:9,217 offline checks,620 new adoption checks, two config round trips and
Java21 offline build passed. Combined32+33:10,617 checks, two round trips and build passed.
All1,963 source JSON files parse; source hashes and focused Markdown/diff checks pass.
New checks exercise production identity policies, malformed/future markers, wrong
bases/namespaces/potions, aliases,23 loot signatures, class binding and native-NBT
preview snapshots. They do not construct live menus, players or native ItemStack registries.
Existing native anvil result timing and before/after guard were code-reviewed.
Gradle/native gameplay, GameTests, client rendering and multiplayer were not launched.

Command: `gradlew.bat d1OfflineChecks build --offline --console=plain`, Java21.
Datagen not applicable: no new item, registry, recipe, tag, model, texture or resource.
Configuration exports were semantically checked and restored byte-for-byte to their
unchanged baseline, including operator examples. No active configuration was touched.

## Compatibility and acceptance

No new save field, network payload, registry ID or migration is introduced by33.
The existing string ability component is appended only by explicit authoring commands;
none were executed during this pass. A previously invalid vanilla marker now gives no
ability but stays on its original item. Exact authored count/components remain intact.
Existing registered custom ammo keeps intrinsic identity without requiring potion data.

No chest, world, schematic, preset, spawner or equipment was rewritten.
Temporary authoring undo is session/expiry-bound and is not crash-persistent.
Before future native authoring, back up the complete world and preserve exact original
stacks. Roll back the complete backup when reverting a tested migration; never replace
a player file independently from account/run/world evidence.

Licensed TEST steps:
1. Compare all six classes and36 startup pastes with the source mapping; preserve quantities.
2. Exercise all18 vanilla signatures, aliases and four old custom registries in their classes.
3. Try invalid/future markers, wrong potion namespace and renamed ordinary ammunition.
4. Verify the native anvil cannot create/change a legacy ability by renaming.
5. Preview a held stack with lore, damage, potion/rocket colors and unknown data; apply/undo.
6. Change count, slot, components, class metadata, run, permission, cursor or recovery state;
   confirm the old token fails. Check expiry, replay and logout.
7. Verify legacy and marked ammo drop, hopper, bundle, death/clone and private-storage guards.
8. Preview all23 named loot signatures and reject missing/extra/wrong enchantments.
9. Inspect actual spawner UUID/drop assignments separately; no migration is certified here.

Audit counts remain44 implemented/runtime-unverified,21 partial D1,9 preserved-content
verification,27 deferred D2+ =101. M10/M72/M55 and loadout IDs retain native/world follow-ups.
No new required PNG. Optional `tamsin_d1_map.png`,512x256, remains unchanged with a fallback.
A future improvement is a read-only native TEST export of exact template stack evidence.

Exact changed files are listed in [the combined checkpoint](D1_BATCHES_32-33.md).
Private evidence: `Google Docs and Sheet/Audit/D1_Batch_33_2026-09-19/`.
Remaining plan: [five batches34-38](D1_REMAINING.md), then cumulative licensed testing.
