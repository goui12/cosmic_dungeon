# D1 custom ammunition: pre-texture checkpoint, 2026-09-22

Cameron authorized dedicated D1 ammunition and exact editable vanilla PNG copies. This adds14 item IDs, preserves four existing IDs and adds one shared D1 arrow entity. All18 identities reuse the existing server ability/configuration rules. Class-chest contents, authored stacks, equipment, world data and active configs were not edited. D2-D5 remain outside this pass.

Parent: a09f8837e45bec490314e1599fb8ae0640729557
Branch: feature/d1-canon-config-20260916
Final local commit is the last repository mutation after this report and staged review. No push, deployment or gameplay launch.

## Item list

All registry names below have the cosmicdungeon namespace.

| Item | ID / PNG stem | Class | Purpose | Registry |
| --- | --- | --- | --- | --- |
| Mending Sting | mending_sting | Theurgist | Gradual healing | New |
| Verdant Jolt | verdant_jolt | Theurgist | Fast regeneration | New |
| Scintilla Vitalis | scintilla_vitalis | Theurgist / Judicator | Healing or undead damage | Retained |
| Lux Vitalis | lux_vitalis | Theurgist / Judicator | Stronger healing or undead damage | Retained |
| Ebonsight | ebonsight | Judicator | Night vision | Retained |
| Vielpiercer | vielpiercer | Judicator | Glowing target outline | Retained |
| Venom of the Tree Viper | tree_viper | Venefex | Short poison | New |
| Arrow of Pestis | pestis | Venefex | Melee weakness | New |
| Arrow of Vapours | vapours | Venefex | Short movement slowdown | New |
| Spicule Breach | spicule_breach | Venefex | Magic damage scaling with debuffs | New |
| Venom of the Bushmaster | bushmaster | Venefex | Longer poison | New |
| Venom of the Fer-de-Lance | fer_de_lance | Venefex | Fast poison | New |
| Arrow of the Black Bubo | black_bubo | Venefex | Longer melee weakness | New |
| Scytel of Melancholia | melancholia | Venefex | Longer movement slowdown | New |
| Bodkin of Deathly Stupor | deathly_stupor | Venefex | Strong brief movement slowdown | New |
| Spicule Rend | spicule_rend | Venefex | Stronger magic damage scaling with debuffs | New |
| Cinderbite | cinderbite | Pyroclast | Four-star explosion, configurable damage | New |
| Cindermaul | cindermaul | Pyroclast | Five-star explosion, configurable damage | New |

Cinderkiss and Cinderbight remain named vanilla equipment because the retained reference establishes names/counts, not an approved custom payload. No equivalence to Cinderbite/Cindermaul was invented. Later-dungeon ammunition, conduits and vanilla trident equipment were not recreated.

## Texture handoff

Folder: C:/Users/Cameron/Desktop/CameronsNewPNGs9_22_26

All44 PNGs are exact, SHA-256-verified copies from the local Minecraft1.21.10 client archive. README.md and TEXTURE_MANIFEST.json explain every source, renamed file, dimension, hash and final destination. Do not rename the supplied files.

| Subfolder | Count | Dimensions | Use |
| --- | --- | --- | --- |
| item | 18 | 16x16 | Inventory/held icons; rockets also use this in flight |
| entity/projectiles | 16 | 32x32 | Arrow flight/embedded skins, native UV layout |
| mob_effect | 10 | 18x18 | Custom healing, poison, weakness and slow icons |

Native inventory arrow/spectral-arrow and firework-rocket PNGs are untinted single layers in the generated item models. Flight skins copy tipped-arrow or spectral-arrow PNGs. Effect icons copy the appropriate native effect. All copies remain editable independently.

No blocks or blockstates are needed. Native firework particles, burst shapes/colors, Night Vision and Glowing keep their native resources. No separate flying-rocket PNG is required.

The44 destination PNGs intentionally do not exist in project assets or the candidate jar yet. Models, renderer paths and effect atlas references are prepared for Cameron's later import. Missing textures are expected in this requested pre-texture checkpoint.

## Code and behavior

- D1ArrowItem creates D1ArrowEntity, using native arrow physics, weapon enchantments and exact saved/picked-up stacks. A bounded synchronized identity selects16 flight skins; server stack identity controls gameplay.
- Existing impact and class/run/attunement checks remain authoritative. Custom registry identity is intrinsic; malformed/conflicting custom markers are denied rather than receiving native damage fallback. Legacy matching vanilla stacks retain compatibility without automatic adoption.
- Cinderbite/Cindermaul use D1FireworkItem and native firework entities. Narrow crossbow hooks recognize custom rockets for held-ammo selection, launch speed, durability, projectile creation and loaded crossbow display.
- Cinderbite defaults to four small red bursts without trail/twinkle; Cindermaul uses five large orange/red bursts with trail/twinkle. One gunpowder is the default within the source's1-3 range; native stack components can author flight/visuals.
- The existing CosmicDungeon.config supplies damage, duration, power, radius and class modifiers. No balancing defaults, vendor prices or active server configs were changed.
- Creative Dungeon Items exposes all18. Datagen adds14 item/model pairs,16 entries to the native item arrows tag, the shared entity to the entity arrows tag and ten namespaced effect sprites.
- Native dispenser behavior is registered; existing protected-item/ownerless ability rules remain. No new recipes, inventory scans or per-tick world scans.
- New code TODOs track later texture import and native projectile/crossbow acceptance. No later-dungeon mechanics were implemented.

## Authority and source choices

The local20-tab Debloat workbook defines scope; direct instructions and Q&A take precedence, then the newest applicable linked document. The original full sheet did not restore deleted content. A read-only freshness check found all20 selected linked Docs unchanged; all40 local Markdown/native artifacts still matched their recorded hashes.

- Debloat workbook: Sheets/Cameron Modifications/Dungeon Crawl Master Sheet - Debloat.xlsx
- SHA-256: 976fa26058f7ed624c8aa5aecc57e02caee008ff005591f05da40912e750dcff
- Q&A workbook: Sheets/Cameron Modifications/Questions and answers.xlsx
- SHA-256: c59b81018151350e4df4ae686efd59a06cfdb6887167a1be2d61fc31f815ef8d
- Theurgist April5 overview controls Verdant Jolt timing over the older individual internal note.
- Pyroclast March26 14:08 overview controls Cindermaul15HP over the earlier internal14HP value; Cinderbite remains12HP. Internal records supply compatible visual burst details.
- Existing Judicator Lux permission remains; no chest content audit was performed.
- Native1.21.10 source/bytecode supplied API and texture evidence; generated pointers do not depend on guessed legacy model paths.

| Selected document | ID | Modified UTC |
| --- | --- | --- |
| Arrow (Ebonsight) | 1Nt20YvuaTY7zl2AbJd8w2Xd2X8z9-26njxBwFXFaI_I | 2026-03-14T21:37:13.804Z |
| Arrow (Ebonsight) (Internal) | 1HnwmguP_4MjQyAdUlSeJuEPd3s1b33iFUR7TT_sjCrU | 2026-03-14T21:35:01.908Z |
| Arrow (Lux Vitalis) | 11i6S00FqQ4vOuaLit7CLtFepxTpXW_682WcDiMenc6E | 2026-04-04T17:07:50.966Z |
| Arrow (Lux Vitalis)(Internal) | 11KIBXd995g2qjZhC65doSiK38g1d3N7V6ilBxkb93OM | 2026-04-04T17:08:51.912Z |
| Arrow (Mending Sting) | 1FHpPdnRMTu5D-x-hXarl1KFghmBRgN5sLM60lS1E6oQ | 2026-04-04T15:36:23.099Z |
| Arrow (Mending Sting) (Internal) | 1bPkBSpDNZf8lr6ocdn0UGj2ZUJ9fzXX8xuVGRwcZYSg | 2026-04-04T16:38:55.948Z |
| Arrow (Scintilla Vitalis) | 1fn4KhCx06080Gn2p3M9GnWlzGPJmaqVrfNdYI2YAJsg | 2026-04-04T15:31:03.779Z |
| Arrow (Scintilla Vitalis) (Internal) | 1MufAd5Ow8tBj04_NIoENDOZLc4IzzxofxZPHFVOAVgo | 2026-03-14T21:31:47.537Z |
| Arrow (Verdant Jolt) | 1-B5krWHhtwEOKmjnhRFW8sXYrn4pc12NHWTKZte5hMk | 2026-04-04T16:43:58.764Z |
| Arrow (Verdant Jolt) (Internal) | 1aX2vZ5tlPQh1-crhQBmMhDnVQfJpNiQ2EWcASkmgZFQ | 2026-04-04T16:42:20.247Z |
| Arrow (Vielpiercer) | 1qr8JAQAqEkO5EUEZ8oCMaW5HDrlwV_okebUugncRslk | 2026-03-14T21:06:38.038Z |
| Arrow (Vielpiercer) (Internal) | 1u1Ou0Jmt2cacvF4mwdUvUriChBC_RYoOS8RwkL9nNpU | 2026-03-14T21:38:40.664Z |
| Firework Rocket (Cinderbite) | 1cOFJ1U5a8878ZdLZuJmTjsoTJyIx_y_v3qG5HFNhDq8 | 2026-03-26T11:00:33.629Z |
| Firework Rocket (Cinderbite) (Internal) | 1vyElnGqiq5ZumdIu1cF51hA6HZn_eSB4z-l61dsZEss | 2026-03-26T13:25:47.056Z |
| Firework Rocket (Cindermaul) | 1eB7h9kyj93bfzWj3i4YGepEmCA-Kwj2RjdIwhASC67k | 2026-03-26T11:01:32.502Z |
| Firework Rocket (Cindermaul) (Internal) | 1A-TIYSaInjFJOm80KrgxSFErXow4JSHz2it-cvkY6xU | 2026-03-26T13:27:20.195Z |
| Judicator Items and Armor | 1cY_czWEYbUEg_EQmaSANhOFe326gTDVKfL9XaEFGmQo | 2026-04-04T14:39:30.851Z |
| Pyroclast Items and Armor | 16FD3wxi-Uen_DRzItDHdSrSZvkGNwa_r-ZeUYiswxoE | 2026-03-26T14:08:42.520Z |
| Theurgist Items and Armor | 1l9ox2pQUSPy0_J3h7ljPOaVOFtMFkoHq_rFSGK4iqeM | 2026-04-05T14:27:08.267Z |
| Venefex Items and Armor | 1JXqPdwWxateRMGpAuoV1ub8asNL7iMeyrBtzqTuUwm8 | 2026-04-04T14:36:04.408Z |

Private detailed source/evidence receipt: Google Docs and Sheet/Audit/D1_Custom_Ammunition_2026-09-22.

## Validation and limits

Final commands ran sequentially with Java21, Minecraft1.21.10 and NeoForge21.10.64:

    gradlew.bat --offline runClientData
    gradlew.bat --offline runServerData
    gradlew.bat --offline d1OfflineChecks --args=build/d1-custom-ammunition-config build

All three returned0. The final build passed17,165 offline assertions across46 printed groups, including600 custom-ammunition assertions and two config round trips. All1,994 source JSON files parsed. Native crossbow descriptors and datagen mixin-loading logs were checked; this does not prove live firing/rendering.

All31 changed/generated resources are byte-identical in the rebuilt jar. All18 item/model pairs, tags, ten effect sprites and44 pending PNG paths were checked. Desktop copies match the source archive byte-for-byte. git diff --check passed. Source workbooks/artifacts, unrelated tracked files and the tracked1.5.0 jar were preserved. Datagen caches were restored to their exact pre-task state and are excluded from this commit.

An initial combined datagen/build invocation succeeded but shared run-data logging contended and task order could package resources before generation. It is superseded by the explicit sequential run above. The initial log remains for traceability. The Bogatyr malformed-input log entry is an intentional negative-test fixture. Existing Gradle deprecation notices remain.

Candidate: build/libs/cosmicdungeon-1.5.1.jar
SHA-256: 914c77c8f8b3166f633c043b6d2919f58a209ab328def18c51661177eee33f4e
A documentation-only renderer TODO and these handoff notes were added after that build; executable behavior is unchanged.

No native gameplay, client render, GameTest, server launch or performance acceptance was performed. Missing PNGs are deliberate. This is a local pre-texture code checkpoint.

## Persistence, client/server and rollback

Four existing item IDs remain stable. Fourteen new item IDs and cosmicdungeon:d1_arrow require matching client/server jars. The arrow entity uses native saved item/weapon/owner data and recomputes its visual identity on load; no existing stacks or save records are migrated. No protocol-number change.

Before future TEST deployment, preserve the complete matched world/config/jar backup. Rolling back after new items/entities enter a world requires that backup; an older jar cannot interpret new registry IDs. New registry/save behavior still needs the native acceptance cases below.

## Later acceptance

1. After Cameron edits the PNGs and authorizes import, copy only manifest-mapped paths into src/main/resources/assets/cosmicdungeon/textures. Preserve original dimensions, transparency and arrow UV layout. Build and check every resource before the texture commit.
2. On separately authorized licensed TEST, use identical client/server jars and a consistent full world/config/jar backup. Cameron controls Akliz stop/start. This pre-texture candidate is not a visual acceptance build.
3. Create separate Creative Dungeon Items or /give test stacks for all18 IDs. Do not inspect, reconcile, replace or convert authored class-chest contents.
4. Load all16 custom arrows in bows and crossbows. Check inventory icons, flight skins, native velocity/enchantments, Infinity consumption and missed-arrow pickup. Save/reload embedded arrows and compare exact item/components and owner permissions.
5. In an active D1 run, exercise each effect with its allowed class against living/undead targets, native immunity/effect veto and friendly-fire rules. Confirm Judicator/Theurgist Scintilla/Lux use their separate configured power.
6. Check denied class, inactive/foreign run, missing owner, conflicting marker and invalid attunement. No unintended native damage or custom effect may bypass the server gate.
7. Load Cinderbite and Cindermaul from the offhand. Check the loaded-rocket crossbow icon, native1.6 launch speed and three-durability cost; test multishot, walls, shields, saved charged crossbows and one custom explosion per projectile. Defaults remain12/15HP with the existing radius/config rules.
8. Verify ten custom status-effect icons. Compare separately created legacy named-vanilla test stacks for unchanged compatibility and visuals. This does not authorize any real chest/loadout inspection or changes.

## Remaining work

Five readiness areas remain: R01 repair-service compatibility, R04 approved recipes, R05 balance displays/treasure-credit decision, R07 edited D1 ammunition texture import and visual acceptance, R08 world/NPC/instance acceptance. Zero additional numbered batches are scheduled. Other later-dungeon legacy texture references remain deferred.

The101 audit dispositions remain45 implemented-unverified /20 partial D1 /1 preserved-verification-pending /8 author-owned/no AI work /27 deferred D2+. Build checks do not close gameplay acceptance.

## Exact commit file set

- docs/ai/D1_BATCH_33_MAPPINGS.md
- docs/ai/D1_CUMULATIVE_TEST_HANDOFF.md
- docs/ai/D1_CUSTOM_AMMUNITION_2026-09-22.md
- docs/ai/D1_REMAINING.md
- docs/releases/fragments/d1-custom-ammunition-pre-texture.md
- src/generated/resources_client/assets/cosmicdungeon/items/black_bubo.json
- src/generated/resources_client/assets/cosmicdungeon/items/bushmaster.json
- src/generated/resources_client/assets/cosmicdungeon/items/cinderbite.json
- src/generated/resources_client/assets/cosmicdungeon/items/cindermaul.json
- src/generated/resources_client/assets/cosmicdungeon/items/deathly_stupor.json
- src/generated/resources_client/assets/cosmicdungeon/items/fer_de_lance.json
- src/generated/resources_client/assets/cosmicdungeon/items/melancholia.json
- src/generated/resources_client/assets/cosmicdungeon/items/mending_sting.json
- src/generated/resources_client/assets/cosmicdungeon/items/pestis.json
- src/generated/resources_client/assets/cosmicdungeon/items/spicule_breach.json
- src/generated/resources_client/assets/cosmicdungeon/items/spicule_rend.json
- src/generated/resources_client/assets/cosmicdungeon/items/tree_viper.json
- src/generated/resources_client/assets/cosmicdungeon/items/vapours.json
- src/generated/resources_client/assets/cosmicdungeon/items/verdant_jolt.json
- src/generated/resources_client/assets/cosmicdungeon/models/item/black_bubo.json
- src/generated/resources_client/assets/cosmicdungeon/models/item/bushmaster.json
- src/generated/resources_client/assets/cosmicdungeon/models/item/cinderbite.json
- src/generated/resources_client/assets/cosmicdungeon/models/item/cindermaul.json
- src/generated/resources_client/assets/cosmicdungeon/models/item/deathly_stupor.json
- src/generated/resources_client/assets/cosmicdungeon/models/item/fer_de_lance.json
- src/generated/resources_client/assets/cosmicdungeon/models/item/melancholia.json
- src/generated/resources_client/assets/cosmicdungeon/models/item/mending_sting.json
- src/generated/resources_client/assets/cosmicdungeon/models/item/pestis.json
- src/generated/resources_client/assets/cosmicdungeon/models/item/spicule_breach.json
- src/generated/resources_client/assets/cosmicdungeon/models/item/spicule_rend.json
- src/generated/resources_client/assets/cosmicdungeon/models/item/tree_viper.json
- src/generated/resources_client/assets/cosmicdungeon/models/item/vapours.json
- src/generated/resources_client/assets/cosmicdungeon/models/item/verdant_jolt.json
- src/generated/resources_client/assets/minecraft/atlases/mob_effects.json
- src/generated/resources_server/data/minecraft/tags/entity_type/arrows.json
- src/generated/resources_server/data/minecraft/tags/item/arrows.json
- src/main/java/net/goui/cosmicdungeon/client/CosmicDungeonClient.java
- src/main/java/net/goui/cosmicdungeon/client/render/D1ArrowRenderer.java
- src/main/java/net/goui/cosmicdungeon/datagen/D1EffectAtlasProvider.java
- src/main/java/net/goui/cosmicdungeon/datagen/D1EntityTagProvider.java
- src/main/java/net/goui/cosmicdungeon/datagen/DataGenerators.java
- src/main/java/net/goui/cosmicdungeon/datagen/ModItemTagProvider.java
- src/main/java/net/goui/cosmicdungeon/datagen/ModModelProvider.java
- src/main/java/net/goui/cosmicdungeon/entity/D1ArrowEntity.java
- src/main/java/net/goui/cosmicdungeon/entity/ModEntities.java
- src/main/java/net/goui/cosmicdungeon/item/ModCreativeModeTabs.java
- src/main/java/net/goui/cosmicdungeon/item/ModItems.java
- src/main/java/net/goui/cosmicdungeon/item/custom/D1AmmunitionSetup.java
- src/main/java/net/goui/cosmicdungeon/item/custom/D1ArrowItem.java
- src/main/java/net/goui/cosmicdungeon/item/custom/D1FireworkItem.java
- src/main/java/net/goui/cosmicdungeon/item/custom/D1RocketPayload.java
- src/main/java/net/goui/cosmicdungeon/mixin/D1CrossbowMixin.java
- src/main/java/net/goui/cosmicdungeon/mixin/client/D1CrossbowChargeMixin.java
- src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1AmmunitionCatalog.java
- src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1ProjectileAccess.java
- src/main/resources/assets/cosmicdungeon/lang/en_us.json
- src/main/resources/cosmicdungeon.mixins.json
- src/test/java/net/goui/cosmicdungeon/dungeon/d1/D1OfflineChecks.java
- src/test/java/net/goui/cosmicdungeon/playerclass/d1/D1CustomAmmunitionChecks.java
