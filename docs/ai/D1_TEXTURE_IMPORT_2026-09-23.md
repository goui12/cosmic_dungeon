# D1 edited ammunition texture import, 2026-09-23

All 44 edited PNGs are imported byte-for-byte from Cameron's desktop kit. This completes the previously pending asset import for 18 D1 ammunition identities. The original editable desktop files remain available. No item or effect values were changed.

Parent: 698c14e09e8e8797cf6185ce3c8cc4c648211469
Branch: feature/d1-canon-config-20260916
Final local commit follows this report and staged review. No push or deployment.

## Exact asset location and integrity

From: C:/Users/Cameron/Desktop/CameronsNewPNGs9_22_26
To: src/main/resources/assets/cosmicdungeon/textures

| Subfolder | Count | Dimensions |
| --- | --- | --- |
| item | 18 | 16 x 16 |
| entity/projectiles | 16 | 32 x 32 |
| mob_effect | 10 | 18 x 18 |

All 44 files contain visible recoloring (1,739 visible pixels changed versus the original vanilla copies). PNG signatures, chunk CRCs, decompression, dimensions, alpha channels and transparent-background pixel values were verified. Every alpha mask and transparent-background pixel matches the original Minecraft 1.21.10 template. Contact sheets were visually checked; this is asset inspection, not in-game rendering acceptance.

All 18 item-definition/model pairs, 16 renderer texture paths and ten atlas sprite references resolve to the imported PNGs. Rocket flight uses the item model; no separate rocket entity texture or blockstate is needed. The four existing registered arrows now have their previously missing D1 textures.

Private IMPORT_MANIFEST.json records all original template and edited asset hashes, source/destination paths, dimensions and pixel counts under Google Docs and Sheet/Audit/D1_Texture_Import_2026-09-23.

## Attribute and gameplay wiring review

The items already have intrinsic registry identities. D1ArrowItem creates D1ArrowEntity; the impact event and post-hit callback route into D1ArrowAbilities. D1FireworkItem and the native crossbow hooks create firework entities; D1RocketMixin invokes D1RocketAbilities. The imported textures do not need extra potion components or renamed stack markers to select these custom abilities.

All 18 registrations map to the existing catalog and 20 class-specific entries in D1AbilityConfig. Config.define includes these sections in the server-registered CosmicDungeon.config. Shared Scintilla/Lux retain independent Theurgist and Judicator settings. Healing and poison use the configured timed effects; weakness modifies ATTACK_DAMAGE and slowdown modifies MOVEMENT_SPEED, with removal on effect expiry. Instant magic and rocket damage read the same server configuration.

| Ammunition | Class | Default power | Duration ticks | Meaning |
| --- | --- | ---: | ---: | --- |
| Mending Sting | Theurgist | 2 | 100 | Total health points restored over five seconds. |
| Verdant Jolt | Theurgist | 0.8 | 40 | Newest overview: 0.4 hearts total over two seconds; supersedes older 11-second effect. |
| Scintilla Vitalis | Theurgist, Judicator | 4 | 1 | Health restored to living targets, or magic damage to undead. |
| Lux Vitalis | Theurgist, Judicator | 8 | 1 | Health restored to living targets, or magic damage to undead. |
| Ebonsight | Judicator | 0 | 200 | Night vision duration; vanilla night vision also provides underwater visibility. |
| Vielpiercer | Judicator | 0 | 200 | Glowing duration; Arrow Vielpiercer Internal. |
| Venom of the Tree Viper | Venefex | 4 | 100 | Total poison health points over five seconds; poison cannot kill. |
| Arrow of Pestis | Venefex | 4 | 220 | Melee attack damage reduction in health points. |
| Arrow of Vapours | Venefex | 0.15 | 220 | Movement reduction as a fraction, 0.15 = fifteen percent. |
| Spicule Breach | Venefex | 6 | 1 | Base instant magic damage to living targets. |
| Venom of the Bushmaster | Venefex | 8 | 220 | Total poison health points over eleven seconds. |
| Venom of the Fer-de-Lance | Venefex | 3 | 40 | Total poison health points over two seconds. |
| Arrow of the Black Bubo | Venefex | 4 | 600 | Melee attack damage reduction in health points. |
| Scytel of Melancholia | Venefex | 0.15 | 600 | Movement reduction as a fraction. |
| Bodkin of Deathly Stupor | Venefex | 0.6 | 40 | Movement reduction as a fraction. |
| Spicule Rend | Venefex | 12 | 1 | Base instant magic damage to living targets. |
| Cinderbite | Pyroclast | 12 | 1 | Maximum explosion damage at the center; four-star authored vanilla rocket. |
| Cindermaul | Pyroclast | 15 | 1 | Maximum explosion damage at the center; five-star authored vanilla rocket. |

Twenty ticks equal one second. Instant actions use the power immediately; their duration field is not a damage-over-time interval. One heart equals two health points. Spicule damage retains the configurable debuff scale/cap; rocket radius defaults to five blocks with obstruction and falloff checks. Existing server overrides remain authoritative.

Effects require the permitted class, active D1 participation, valid owner and compatible metadata. Denied custom ammunition cannot fall back to native damage. Legacy matching named vanilla ammunition remains compatible and keeps native visuals. No authored stack or class-chest contents were inspected, converted or rewritten. Existing saved items do not acquire the new appearance unless they already use the custom registry IDs; this pass performs no migration.

This is code-reviewed wiring plus existing offline regression coverage. Live firing, final damage, stacking, visual appearance and native save/reload acceptance are still NOT RUN.

## Validation

Java 21 command:

    gradlew.bat --offline d1OfflineChecks --args=build/d1-texture-import-config build

Result: exit 0; build passed; 17,165 offline checks across 46 groups, including 600 custom-ammunition checks and two config round trips. All 1,994 src JSON files parsed. git diff --check passed.

The rebuilt jar contains all 44 edited PNGs byte-identical to the desktop source, all 31 prior generated resources byte-identical to their source files, and the ammunition/effect classes. No duplicate jar entries.

Candidate: build/libs/cosmicdungeon-1.5.1.jar
SHA-256: b9384c8608c195aa5ec3b292565ebdb5157a7c46c0198e4c7ec8b53c61427333

Datagen was not rerun: no model, item definition, tag, recipe, blockstate or atlas definition changed. The September 22 generated references were validated against the new files and final jar. The renderer change is a comment updating the completed handoff and retaining native TEST TODOs.

No clean task was run because the checkout tracks the older 1.5.0 jar. That jar and pre-existing cache edits are preserved. Local GameTest/client/server launch was not performed, following AGENTS.md's licensed TEST workflow. Existing tooling/deprecation notices remain distinct from passed source/build checks.

## Compatibility, security and performance

No new registry IDs, network messages, schemas, saved-data keys, recipes or config settings were introduced in this pass. No migration is required. The previously added ammunition registrations still require the same updated CosmicDungeon jar on client/server after separately authorized deployment.

The import only fills existing texture references at native dimensions; it adds no polling, scans, packets or runtime dependencies. Gameplay remains server-authoritative. Access, classes, currency, vendors, trade, repair, teleportation, spawners, keys/doors, progression and world-instance logic were not altered.

## Remaining acceptance

1. Deploy a matched jar to the licensed TEST client/server only when requested, preserving the full world/config/jar backup and Cameron's stop/start control.
2. Create separate test stacks through Creative Dungeon Items or /give; preserve authored class-chest contents.
3. Check all inventory/held icons, bow/crossbow flight skins and ten effect icons at multiple GUI scales.
4. Verify each allowed class in an active D1 run, including denied-class/outside-run behavior and actual configured damage/healing/durations.
5. Check rocket loading, multishot, walls/shields, embedded-arrow pickup and saved charged crossbows/projectiles after reload.

See [next implementation passes](D1_READINESS_NEXT_STEPS_2026-09-23.md) and [cumulative acceptance](D1_CUMULATIVE_TEST_HANDOFF.md). R07 asset import is complete; its in-game visual acceptance is folded into R08. Three implementation areas remain plus one cumulative TEST area. No additional numbered batch is started by this import.

## Exact changed files

- docs/ai/D1_CUMULATIVE_TEST_HANDOFF.md
- docs/ai/D1_READINESS_NEXT_STEPS_2026-09-23.md
- docs/ai/D1_REMAINING.md
- docs/ai/D1_TEXTURE_IMPORT_2026-09-23.md
- docs/releases/fragments/d1-edited-ammunition-textures.md
- src/main/java/net/goui/cosmicdungeon/client/render/D1ArrowRenderer.java
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/black_bubo.png
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/bushmaster.png
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/deathly_stupor.png
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/ebonsight.png
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/fer_de_lance.png
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/lux_vitalis.png
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/melancholia.png
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/mending_sting.png
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/pestis.png
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/scintilla_vitalis.png
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/spicule_breach.png
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/spicule_rend.png
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/tree_viper.png
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/vapours.png
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/verdant_jolt.png
- src/main/resources/assets/cosmicdungeon/textures/entity/projectiles/vielpiercer.png
- src/main/resources/assets/cosmicdungeon/textures/item/black_bubo.png
- src/main/resources/assets/cosmicdungeon/textures/item/bushmaster.png
- src/main/resources/assets/cosmicdungeon/textures/item/cinderbite.png
- src/main/resources/assets/cosmicdungeon/textures/item/cindermaul.png
- src/main/resources/assets/cosmicdungeon/textures/item/deathly_stupor.png
- src/main/resources/assets/cosmicdungeon/textures/item/ebonsight.png
- src/main/resources/assets/cosmicdungeon/textures/item/fer_de_lance.png
- src/main/resources/assets/cosmicdungeon/textures/item/lux_vitalis.png
- src/main/resources/assets/cosmicdungeon/textures/item/melancholia.png
- src/main/resources/assets/cosmicdungeon/textures/item/mending_sting.png
- src/main/resources/assets/cosmicdungeon/textures/item/pestis.png
- src/main/resources/assets/cosmicdungeon/textures/item/scintilla_vitalis.png
- src/main/resources/assets/cosmicdungeon/textures/item/spicule_breach.png
- src/main/resources/assets/cosmicdungeon/textures/item/spicule_rend.png
- src/main/resources/assets/cosmicdungeon/textures/item/tree_viper.png
- src/main/resources/assets/cosmicdungeon/textures/item/vapours.png
- src/main/resources/assets/cosmicdungeon/textures/item/verdant_jolt.png
- src/main/resources/assets/cosmicdungeon/textures/item/vielpiercer.png
- src/main/resources/assets/cosmicdungeon/textures/mob_effect/black_bubo.png
- src/main/resources/assets/cosmicdungeon/textures/mob_effect/bushmaster.png
- src/main/resources/assets/cosmicdungeon/textures/mob_effect/deathly_stupor.png
- src/main/resources/assets/cosmicdungeon/textures/mob_effect/fer_de_lance.png
- src/main/resources/assets/cosmicdungeon/textures/mob_effect/melancholia.png
- src/main/resources/assets/cosmicdungeon/textures/mob_effect/mending_sting.png
- src/main/resources/assets/cosmicdungeon/textures/mob_effect/pestis.png
- src/main/resources/assets/cosmicdungeon/textures/mob_effect/tree_viper.png
- src/main/resources/assets/cosmicdungeon/textures/mob_effect/vapours.png
- src/main/resources/assets/cosmicdungeon/textures/mob_effect/verdant_jolt.png
