# Batch36: D1 class effects and ammunition

Completed in code and offline-validated on 2026-09-20 UTC. Cameron authorized Batch36
only, followed by a final local commit and a stop before37. Verified parent:
c5678b7d169c738b11f51b889a4491db969d6646 on feature/d1-canon-config-20260916.

## Changes

- B36-1: recognized D1 ammunition now revalidates active run membership, owner state,
  class and existing attunement at impact. A departed/dead/disconnected owner, foreign
  class, malformed binding or non-player owner gets no class effect or vanilla fallback.
  Denied arrows are consumed on entity impact; denied rockets retain their visual
  explosion without damage. Unknown identities and ordinary ammo retain native behavior.
  No automatic inventory, chest or saved-projectile rewrite.
- B36-2: Mending Sting, Verdant Jolt and Ebonsight apply aid without an ordinary arrow
  wound, matching the existing instant restorative approach. A narrow common mixin
  permits these and instant healing arrows to contact active same-run teammates when
  team friendly fire is disabled. Native projectile hit/vehicle/piercing filters remain;
  damaging arrows do not gain that exception. Canceled impact events remain untouched.
- B36-3: custom poison/regeneration consult native family applicability, retaining
  undead/spider immunity and applicability vetoes. Family selection compares strength
  (rate for periodic effects), then remaining duration. A weaker effect cannot replace
  a stronger one; equal power cannot shorten an existing effect. The whole family is
  checked before any removal, and a removal veto prevents the new application.
  Native same-ID refresh handling remains. Periodic totals, poison floor and Spicule
  scaling now use production rules exercised by the offline checks.
- B36-4: chain lightning requires a genuine successful trident melee/thrown hit from
  an active D1 Dragoon. Holding a trident during other damage no longer qualifies.
  Existing chance, radius, damage multiplier, hostile selection and original-victim
  exclusion remain. The recursion guard still prevents chaining and double class scaling.
- B36-5: rocket and lightning queries cap locally inspected candidates before expensive
  obstruction work. Rockets perform at most two rays per candidate; lightning sorts the
  bounded candidate list, tests LOS and selects up to the existing target limit.
  At saturation, some targets may be omitted; this is not nearest-all selection.

## Configuration and source choices

Two additive server CosmicDungeon.config keys, both default256 and bounded1..1024:

| Section | Key | Purpose |
| --- | --- | --- |
| Dragoon | chainLightningCandidateLimit | Bound candidate storage and LOS checks per trigger |
| Pyroclast | rocketCandidateLimit | Bound candidate storage and obstruction checks per explosion |

Existing numeric balance defaults and operator values remain. The generated config
example contains only these two semantic additions; unrelated export ordering was
restored. No active config was edited. Vendor prices are unchanged.

The debloated MASTER and original Q&A hashes match the baseline. All20 relevant Google
Doc metadata checks returned unchanged, and all saved source artifact hashes match.
Eighteen applicable bodies/sections were reviewed for D1 or exclusion boundaries.
The unchanged enchantment-price review from Batch35 and Venefex chest review from
Batch33 were reused explicitly. This is not a new complete D2+ semantic audit.
SOURCE_REVIEW.json records IDs, revisions, scope, Q&A coordinates and choices.

- Q&A D17/D21/D43/D44 delegate newest-document defaults to server configuration.
- Theurgist overview 1l9ox2pQUSPy0_J3h7ljPOaVOFtMFkoHq_rFSGK4iqeM, April5:
  Verdant Jolt remains0.8 HP over40 ticks, superseding the older individual11-second entry.
- Venefex overview 1JXqPdwWxateRMGpAuoV1ub8asNL7iMeyrBtzqTuUwm8, April4:
  documented poison/weakness/slowness and instant amounts remain. Spicule's10% per
  harmful effect, capped10, remains Cameron-authorized judgment rather than an exact
  documented curve. Periodic effects retain the existing once-per-second pulse approach.
- Judicator overview 1cY_czWEYbUEg_EQmaSANhOFe326gTDVKfL9XaEFGmQo, April4:
  Scintilla, Ebonsight and Vielpiercer remain assigned. A Lux chest entry does not define
  a new Judicator ability; the authored chest remains unchanged.
- Pyroclast overview 16FD3wxi-Uen_DRzItDHdSrSZvkGNwa_r-ZeUYiswxoE, March26 14:08:
  D1 Cinderbite four-star12 HP and Cindermaul five-star15 HP remain. The latter supersedes
  the older14-HP individual entry. No inferred Cinderkiss/Cinderbight aliases or D2 rockets.
- MASTER Classes!S4 retains3% chain chance.32-block server LOS,64 additional hostiles and
  1x triggering final damage remain explicit configurable approximations of "on screen".
  Classes!T4 riptide/channeling requires reconciliation with actual authored tridents,
  including the newer August28 Dragoon overview. Q&A D79 prohibits changing chest items.
- Q&A D42 leaves conduits deferred; D63 requires custom effects for non-vanilla amounts.

## Validation

Java21 gradlew.bat d1OfflineChecks build --offline --console=plain passed:
12,804 offline checks, including866 new combat checks and5 new config checks, plus
two separate config round trips. All1,963 source JSON files parse.
The first build found a comparator generic-inference error; it was corrected before
the successful58-second build. No tests were removed or weakened.

New checks cover18 ammunition identities across9 classes, all run/binding combinations,
restorative-only classification, strength/duration precedence, periodic totals including
partial seconds, poison floor, Spicule scaling/cap, rocket center/falloff/boundaries,
successful trident trigger conditions and20 configured source power/duration entries.
Native applicability/event behavior is code-reviewed, not simulated by these pure checks.

The exact native AbstractArrow bytecode confirms the new redirect target and preserves
the superclass and piercing filters. Native rocket damage and effect application/removal
code were inspected. This verifies method compatibility, not mixin execution in Minecraft.
The candidate JAR includes the new mixin and existing generated effect atlas.
Source/workbook hashes, scoped file hashes, JSON, local report links and diff checks pass.

No datagen was needed: no model, item definition, recipe, tag, loot or generated asset
changed. The hand-maintained common mixin declaration is the only resource JSON edit.

## Compatibility and remaining TEST acceptance

No registry ID, save field/schema, packet/protocol, spawner, preset, world, equipment,
chest or source workbook changed. No migration is required. Existing1.5.0 JAR and
pre-existing generated-resource cache change were preserved. No client-only imports
were added to common code. Gameplay decisions remain on the server.

AccessPolicy and D1Members determine active ownership. Class metadata is read, never
rewritten. The changes affect combat/effects only: trade/vendor/currency custody,
teleport services, door/key data, progression saves and spawner restoration are intact.
Native damage can still feed the existing reward/achievement listeners; integration
with those listeners needs gameplay acceptance. No added tick scanner, persistent
cache, packet family or runtime dependency. Engine query cost/latency is not measured.

Before release, in the authorized licensed TEST world copy:

1. Fire each authored arrow with its allowed and foreign classes. Try missing/conflicting
   attunement, class change, departure, disconnect and dispenser ownership. Check ordinary
   unnamed arrows remain native and denied special arrows do not apply vanilla effects.
2. With team friendly fire disabled and server PvP both off/on, fire restorative arrows
   at same-run teammates. Verify exact healing/vision and no ordinary wound; harmful
   arrows retain native restrictions. Check canceled impacts, shields and piercing.
3. Test poison on spiders/undead and regeneration on immune types. Compare Mending/Verdant,
   Pestis/Black Bubo and slow tiers in both orders. Check near-expiry, stronger-shorter,
   death/reload, armor, invulnerability frames, absorption and poison's nonlethal result.
4. Test native effect cancellation and legacy overlaps. A stateful mod can change
   applicability after preflight; partial removals in old overlapping saves are not a
   transaction across other mods. Do not bypass native vetoes or claim migration coverage.
5. Trigger trident melee, thrown and riptide hits. Confirm positive damage/chance,
   no chain from unrelated damage, no original-victim repeat, LOS, allies and saturation.
   Verify authored enchants without replacing or modifying any chest stack.
6. Fire both D1 rockets with actual launchers, including multishot. Check payload visuals,
   distance falloff, walls, shield/armor, self/team damage, lifecycle and candidate limits.
   Verify no terrain damage and preservation of all authored components.

M55/M58/M60/M63 remain partial_D1 for the native/world acceptance above; implementation
review is complete. All101 audit IDs remain accounted for:45 implemented-unverified,
20 partial D1,9 preserved-verification-pending and27 deferred D2+ entries.

## Graphics and remaining batches

No new PNG is required. The ten D1 custom effect icons already use generated aliases
to vanilla regeneration/poison/weakness/slowness sprites. Existing items and rocket
payload visuals remain authored vanilla resources.

Optional tamsin_d1_map.png remains512x256: route to Base Camp, signed "-JHW".
Its existing drawn fallback remains usable.

[Two planned batches remain](D1_REMAINING.md):
37 achievements, progression bindings and travel gates;
38 help/balance displays, performance bounds and cumulative licensed TEST handoff.
Batch38 also carries Batch35 custom-recipe/crafting release gates. No game, GameTest,
client/server launch, deployment or push occurred.

Future improvement: use licensed TEST measurements to tune the new candidate budgets.

## Exact commit file inventory

1. docs/ai/D1_BATCH_36.md
2. docs/ai/D1_REMAINING.md
3. docs/ai/tasks/d1-canon-config-20260916.md
4. docs/config-examples/CosmicDungeon.config
5. docs/releases/fragments/d1-batch-36-class-ammunition.md
6. src/main/java/net/goui/cosmicdungeon/Config.java
7. src/main/java/net/goui/cosmicdungeon/effect/D1TunedMobEffect.java
8. src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1AbilityConfig.java
9. src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1ArrowAbilities.java
10. src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1CombatRules.java
11. src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1ProjectileAccess.java
12. src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1RocketAbilities.java
13. src/main/java/net/goui/cosmicdungeon/playerclass/dragoon/DragoonPassiveEvents.java
14. src/main/java/net/goui/cosmicdungeon/mixin/D1SupportArrowMixin.java
15. src/main/resources/cosmicdungeon.mixins.json
16. src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java
17. src/test/java/net/goui/cosmicdungeon/playerclass/d1/D1CombatChecks.java

Private evidence: Google Docs and Sheet/Audit/D1_Batch_36_2026-09-20.
Final local commit title: Complete D1 batch 36: enforce class ammunition and bound combat effects.
Resolve its exact hash from local Git history; commit is the final repository-changing step.
