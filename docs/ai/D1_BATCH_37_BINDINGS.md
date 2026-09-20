# Batch37 authored binding handoff

Developer-facing checklist; reviewed against the retained MASTER, Q&A and 20 unchanged linked
documents on 2026-09-20 UTC. No world was opened, edited or claimed verified.
See [Batch37](D1_BATCH_37.md), [Batch33 loadout mappings](D1_BATCH_33_MAPPINGS.md)
and [remaining plan](D1_REMAINING.md). Navigation coordinates do not define cuboid bounds,
placed block identity, live lock IDs or schematic contents.

## Required review on a complete TEST backup

| Binding | Authority and current behavior | Native acceptance still required |
| --- | --- | --- |
| Stairway uppermost chest | MASTER Achievements!B18/C18: World Spawn. Q&A D76: one ordinary Elytra. /d1 objective bind stairway X Y Z now requires Overworld. | Bind the actual uppermost chest. /d1 objective status identifies retained legacy D1 bindings needing review. Test either half of a double chest, another chest, full inventory/reopen and old/new receipts. |
| Piglin heads at Camp4 | Achievements!B20/C20: six characters simultaneously wearing Piglin Heads; navigation 630 22 68. d1_camp_4 region, template-to-instance mapping. | Inspect /region info d1_camp_4; if absent, use the existing region wand/creation workflow after reviewing camp bounds. Verify six distinct active players, head removal, different slots, no spectator/developer/dead/outside-escrow credit. No wolf equipment or new texture required. |
| Base Camp discovery | Tamsin Internal, 2026-08-19; personal discovery precedes Tax eligibility. base_camp is explicitly authored, not inferred from other camps. | Bind reviewed Base Camp position with /d1 objective bind base_camp X Y Z. Verify personal radius, map acceptance and Watson success prerequisite. |
| Three journals | Librarian1 and Nonspecific Class Chests. Source defaults 640 -60 60; 619 -1 119; 1637 98 4253, D1 template. | Inspect actual lecterns/handheld pages. Use existing canonical book preview/apply/undo only for reviewed books. A matching title alone cannot grant credit. |
| Fire Escape | Fire Escape 2026-07-05. D1 Nether source start 193 25 119, end 206 28 99. Lava clears the current attempt. | Confirm actual upper path endpoints with objective status; bind source-correct alternatives only after inspection. Test feet/below positions, lava reset, deaths and next run. |
| Synchronous Peal | Achievements!C4: six distinct bells, Camp5. d1_camp_5; configurable two-second default retained from prior D70 interpretation. | Verify six physical bell positions, actual ringing, same run/window, repeats of one bell, and two instances. |
| Sixfold Vigil | Achievements!C6 and D71: six distinct lit candle colors on the existing specified support in the Wither room. d1_wither_room. | Check authored support, region volume/candle scan caps and per-run credit. Wither-dependent variants remain code TODOs under D71. |
| Recorded Sound | Achievements!I5 and D74: seven distinct actual disc identities played during one run. | Verify real jukebox playback, repeat discs, another slot and reset. No historical catalog disc-list substitution. |
| Tired, Not Broken | Achievements!B8/C8: D1 Woodland Manor, Phantom attack after three days without rest. d1_woodland_manor. | Verify actual Phantom interaction and native rest statistic. |
| Shulker Express | Achievements!B19/C19: World Spawn, Shulker bullet. d1_spawn_area must be authored in Overworld. | Verify actual bullet damage and region boundary without requiring a dungeon run. |
| Plant Flags and Watson | Achievements!C11, Q&A D23/D72: assigned online instance members; Watson is the only six-Bloom success path. | Inspect existing quest status and Watson bindings; verify disconnect grace, no developer/foreign-instance credit, six physical inputs, success/failure save recovery. |
| Startup rooms and class chests | Six logical slots, 36 preserved paste requests across d1_start and d1_b1_chests through d1_b5_chests. Q&A D47/D79 preserves six D1 classes and authored contents/quantities. | Verify actual schematics, rotations, empty slots, frame contents, all six selected classes and startup rollback. Retain duplicates in source lists; never automatically replace chest contents. |
| Doors, keys and encounters | Exact world bindings remain unverified. Notes7.7 names Courier/Bound Dead spawn references at D1 Nether 217 57 532 and D1 Earth 648 15 69. | Inspect targeted /door info and held /door key info, actual lower-half lock IDs, passage counts and cloned instance locks. Preserve all placed spawners/presets and capture unknown mappings; coordinates alone do not authorize edits. |
| Main Village and Chops | Q&A D01/D03/D20: prior personal completion, round-trip Chop and separate inventories. Existing main_village/village/Main Village aliases are gated. | Inspect actual destination records, including custom aliases. Legacy main_village defaults to World Spawn until authored: no coordinate-based village gate is guessed. Test locked/eligible players and every used portal. |
| Reset exits and companion selection | Generic rifts cannot enter/escape around lifecycle custody. Reset exits use existing saved cleanup. Companion selection belongs to the original run and server clock. | Fault-inject reset saves before movement, ensure RESETTING recovery owns evacuation, test stale UI after reset/rejoin, outside escrow, dead/offline targets, blocked landing and cancelled teleport. No target failure clears the paid cooldown. |

## Source scope and deferral

Linked flavor text calling the Piglin achievement six wolves does not replace the explicit
six-character requirement in MASTER C20. This corrects an earlier audit inference, not Dad's sheet.
World Spawn Stairway corrects the earlier D1-only trigger; existing positive-run reward receipts
and advancement IDs remain valid.

The newer Q&A six-Bloom Watson rule overrides the older three/six-flower unlock split.
Existing lifetime totals and grandfathered village access are preserved; no guessed historical
subtractions or grants. D2-D5 physical progression and Vital Exchange remain deferred.

Notes Teleport (2026-07-07) explicitly proposes future features. This batch does not create new
waypoint/recall systems or authorize a Companionship recipe/unlock table. The legacy registered
item keeps its five-minute default with server configuration.

No new required PNG. The optional existing tamsin_d1_map.png remains 512 x 256 pixels,
showing the route to Base Camp and signed -JHW; the drawn fallback still works.
