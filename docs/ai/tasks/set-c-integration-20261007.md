# Batch26: final Set C integration and Beta

Approved final card26 only. Version1.6.16-beta.1; branch
feature/set-c-integration-20261007; baseline Batch25 ce1090505fd2df8afe45f906bb508b55e0fc6800.
The validated PR merges the entire chained Set C (19-26) into main. No artifact-only
branch merge, direct-main push or stable promotion. Exact commit/merge/CI/release and
deployment IDs are recorded by the controller; this report does not pre-claim completion.

Root owns native cross-feature tests/registration, resulting integration fixes, release
assembly, main merge and deployment. One worker owns the new client integration test
and the corrected breeding tooltip/test, followed by independent native review.

## Changes and compatibility

Add composed panel/state regressions and three native cross-feature multiplayer
scenarios. Correct Breed guidance to allow every mode except Stand Ground.
No other gameplay rule, packet/schema/registry ID, datagen input or runtime dependency
is changed. Existing additive resource/mode records and active-run packs retain their
native save behavior; unknown fields remain intact. No authored chest, loadout,
renamed rocket, spawner/preset or world-content changes.

Set C includes new earlier additive registries/effects and normal save records; use
normal world backups for version changes and prefer forward corrections. This batch
requires no additional migration. Unloaded retired wolves stay rejected on later load.
Existing independent tests preserve legacy mode defaults, unknown future data,
relog/reset lifecycle and source/latest-death distinctions.

## Automated integration coverage

| Boundary | Evidence |
| --- | --- |
| Full class panels and viewport sizing | SetCSkillsIntegrationTest composes authoritative resource,12-action Bogatyr and maximum69-action Theurgist models across four viewports and survival/creative widths; every action remains reachable. |
| Drag, minimize, reset, scroll and consent | Combined Skills/Requests pointer tests preserve independent scrolling, exact request ID/generation, carried-item cancellation, resize/pending acknowledgement and per-class preference reload/reset. Existing layout/state/wire tests remain. |
| Crafting, supply consent and600 cap | set_c_supply_crafting_cap uses actual native players, inventory, saves and services: crafting creates headroom without expanding quoted consent; regeneration wins the cap race without consuming donor items; two donors share one remaining capacity. |
| Resource spending, resurrection and latest death | set_c_resurrection_resource_death rejects acceptance after crafting drops below120; real normal respawn/new death invalidates old player/offer; accepted native resurrection retains latest organization metadata without recreating missing items and saves the exact remaining caster balance. |
| Pack mode transitions and friendly fire | set_c_pack_transitions changes30 loaded wolves through Stand Ground and every active mode, retaining actual target response, both-direction friendly guards, another owner's mode and free-command balance; run retirement clears both packs. |
| Existing lifecycle and compatibility | Required full suite retains death_inventory_lifecycle/pickup, class_resource_lifecycle_persistence, supply_requests_save_recovery, theurgist_revival_recovery, bogatyr_command_save_recovery/run_retirement/mode_persistence and the old-shape unit cases. |
| Advanced tactics and large packs | Existing strategic/rescue/companionship/danger_close/work_scaling cases remain. Batch25 measured1/30/120-wolf native decision/path work; final CI repeats those assertions. No evidence-based need for the conditional30 cap was found. |

Native fixtures use serialized server-thread interleavings, the actual engine/save and
network services, and isolated owners/runs; these are automated co-op boundary checks,
not licensed real players or rendered screenshots. Latest-death integration seeds a
non-custodial layout and removes the physical item explicitly; existing death tests
separately exercise genuine native drops, provenance and pickup organization.
Performance instrumentation is opt-in and compares synthetic decision/path work,
not full-world TPS. Full Java21 local build without clean, all source JSON and diff
checks, complete PR CI, exact merged-main CI and release build/native tests are required.
Datagen is not applicable: no input or generated resource changes.

## Licensed Beta acceptance: TESTING

1. With two legitimate clients, open each class inventory at narrow/wide GUI scales.
   Drag/minimize/reset Skills; wheel and drag each scrollbar; confirm item dragging and
   Requests remain independent, all action/tooltips are reachable and the class guide
   opens correctly. Verify default/minimized resource updates.
2. Use mixed classes and two donors near the600 cap. Craft/recycle while consent is
   pending, accept stale and current cards, and try full inventories. Expect no extra
   item consumption, over-cap credit or failed-action charges.
3. Kill a teammate, offer/decline/accept player resurrection, spend caster supplies
   before acceptance, then die again and reconnect. Verify latest location/offer,
   one120 debit and only physically recovered items returning to their saved slots.
4. Use one and large packs around real terrain. Exercise all six modes and free
   transitions, loaded-only care/Regroup, allies' attacks/negative effects, shared
   Companionship and moving Danger Close. Check another owner's pack and mercenary
   wolves remain independent; complete/reset the run and reload old chunks.
5. Confirm both matching CurseForge projects are available and update in the legitimate
   client profile. Moderation/app visibility is separate from accepted upload.

Stable promotion waits for this licensed gameplay acceptance. No client/game/server
launch is part of this batch; Cameron controls TEST startup.

## Delivery and finish

Publish the unique runtime/helper Beta from validated merged main. Freshly verify TEST
shutdown, transfer only the published runtime using pinned sFTP and record acknowledged
completion. Do no post-publication artifact/readback hashes, local-client installation
or legacy-feed parity gate. Finish the controller at26 and leave recovery disabled.
Zero implementation batches remain afterward; the next activity is licensed Beta QA.

## Exact changed files

- docs/ai/D1_REMAINING.md
- docs/ai/PARTY_SKILLS_BATCHES_20261006.md
- docs/ai/tasks/set-c-integration-20261007.md
- docs/releases/1.6.16-beta.1.md
- docs/releases/Update_1.5.1.md
- docs/releases/fragments/batch-26-set-c-integration.md
- gradle.properties
- src/main/java/net/goui/cosmicdungeon/client/screen/skills/BogatyrClient.java
- src/main/java/net/goui/cosmicdungeon/dungeon/DungeonInstanceGameTests.java
- src/main/java/net/goui/cosmicdungeon/playerclass/bogatyr/BogatyrTacticsGameTests.java
- src/main/java/net/goui/cosmicdungeon/playerclass/resource/SupplyRequestGameTests.java
- src/main/java/net/goui/cosmicdungeon/playerclass/theurgist/TheurgistActionGameTests.java
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/BogatyrModesClientTest.java
- src/test/java/net/goui/cosmicdungeon/client/screen/skills/SetCSkillsIntegrationTest.java
- src/test/java/net/goui/cosmicdungeon/gametest/GameTestSerializationTest.java
