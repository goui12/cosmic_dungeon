# September22 registered-ammunition update

All18 identities in the historical mapping below now have dedicated custom item IDs.
Four IDs were retained and14 added; vanilla signature compatibility remains unchanged.
See [the current implementation and texture handoff](D1_CUSTOM_AMMUNITION_2026-09-22.md).
The earlier four-only registry statement is historical. Creating custom items does not
authorize inspecting, reconciling or converting Cameron's authored chest stacks.

---

# Batch33: D1 item adoption and source mappings

This is a reviewed identity/authoring reference, not proof that live templates, chests or
spawners have been migrated. Q&A D79 preserves developer-authored chest contents. No
authored stack, world, schematic or preset was edited during this pass.

## Authority and revision choices

The debloated MASTER defines retained scope; Cameron's direct instructions and Q&A
take precedence. Use the newest applicable linked document. The private Batch33 evidence
records native workbook rows, document revisions/hashes and all 687 nonblank source lines
within the six chest lists' seven camp sections, including headers and repeated rows.

- Theurgist overview: `1l9ox2pQUSPy0_J3h7ljPOaVOFtMFkoHq_rFSGK4iqeM`, 2026-04-05.
  Its Verdant Jolt values remain in configuration; the older individual arrow document
  is not used to restore the old duration. D42 conduits and D48 backpack remain deferred.
- Judicator overview: `1cY_czWEYbUEg_EQmaSANhOFe326gTDVKfL9XaEFGmQo`, 2026-04-04.
- Venefex overview: `1JXqPdwWxateRMGpAuoV1ub8asNL7iMeyrBtzqTuUwm8`, 2026-04-04.
- Pyroclast overview: `16FD3wxi-Uen_DRzItDHdSrSZvkGNwa_r-ZeUYiswxoE`, 2026-03-26.
  The newer April4 chest names are retained as written; they do not define equivalence
  between Cinderkiss/Cinderbight and an approved rocket payload.
- Named drops: `1fX1UbC6cG_cnN2auDo_1ascDY24yjFy9pIfL4qe3-_g`, 2026-08-28.
- Trade/no-drop authority: `1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc`, 2026-08-18.

## Ammunition identities

The canonical component is the existing `cosmicdungeon:d1_ability`. These signatures
identify authored ammunition; power, duration, radius and other modifiers remain in
`CosmicDungeon.config`. Existing matching base/long/strong vanilla potion-family variants
remain compatible. This is a compatibility choice, not a recipe or a new balancing table.
Foreign namespaces and custom effects cannot satisfy vanilla signatures.

| Identity | Canonical name | Vanilla signature | Ability class |
| --- | --- | --- | --- |
| mending_sting | Mending Sting | Tipped arrow, regeneration family | Theurgist |
| verdant_jolt | Verdant Jolt | Tipped arrow, regeneration family | Theurgist |
| scintilla_vitalis | Scintilla Vitalis | Tipped arrow, healing family | Theurgist, Judicator |
| lux_vitalis | Lux Vitalis | Tipped arrow, healing family | Theurgist, Judicator |
| ebonsight | Ebonsight | Tipped arrow, night vision family | Judicator |
| vielpiercer | Vielpiercer | Spectral arrow | Judicator |
| tree_viper | Venom of the Tree Viper | Tipped arrow, poison family | Venefex |
| pestis | Arrow of Pestis | Tipped arrow, weakness family | Venefex |
| vapours | Arrow of Vapours | Tipped arrow, slowness family | Venefex |
| spicule_breach | Spicule Breach | Tipped arrow, harming family | Venefex |
| bushmaster | Venom of the Bushmaster | Tipped arrow, poison family | Venefex |
| fer_de_lance | Venom of the Fer-de-Lance | Tipped arrow, poison family | Venefex |
| black_bubo | Arrow of the Black Bubo | Tipped arrow, weakness family | Venefex |
| melancholia | Scytel of Melancholia | Tipped arrow, slowness family | Venefex |
| deathly_stupor | Bodkin of Deathly Stupor | Tipped arrow, slowness family | Venefex |
| spicule_rend | Spicule Rend | Tipped arrow, harming family | Venefex |
| cinderbite | Cinderbite | Firework rocket with four explosions | Pyroclast |
| cindermaul | Cindermaul | Firework rocket with five explosions | Pyroclast |

Historical aliases retained: Arrow of the Vapours, Arrow of Black Bubo, Arrow of
Melancholia, Arrow of Deathly Stupor, Spicule of Breach/Rend, Arrow of Mending Sting,
Arrow of Verdant Jolt and Veilpiercer. ID words themselves remain recognized as before.

The four existing custom registries `cosmicdungeon:vielpiercer`,
`cosmicdungeon:scintilla_vitalis`, `cosmicdungeon:lux_vitalis` and
`cosmicdungeon:ebonsight` retain intrinsic identity, including their default stacks
without potion components. An explicit marker must agree with that registry.
Other mod items never inherit abilities through namespace/name alone.
Unknown, empty or conflicting explicit markers remain untouched and give no ability.

`/d1 item ability <identity>` previews one held **vanilla** stack. It rejects an existing
marker, contradictory recognized identity, loot provenance, repair marker, incomplete
attunement, another class or non-D1 tier metadata. Valid D1 tier3/tier4 metadata is
preserved, including value. Unattuned authored stacks can be reviewed explicitly.
Names need not match when a developer deliberately chooses an identity.
It never changes class metadata or strips other restrictions.

`/d1 item apply <token>` and `/d1 item undo <token>` share the existing one-shot,
exact-stack, selected-slot, dimension/run, live-session, cursor, recovery and expiry checks.
Only one marker is appended; count, name, lore, damage, potion, rocket colors, ownership,
enchantments and unknown components are copied intact. Undo requires the exact adopted
stack and expires with the preview/session; it is **not crash-persistent recovery**.
There is no ammunition container/bulk command. No authoring command was executed this pass.

The existing native anvil event runs after vanilla result construction. Its
before/after-identity check remains intact. Verify actual anvil/menu behavior in TEST;
offline policy checks are not a native anvil test.

## Six retained class loadouts

| Class | Chest document ID | Edited | Code template suffix |
| --- | --- | --- | --- |
| Bogatyr | 1lUy03lqDbeB4s_o5Nyvft40JMRj60pKMrY2jQ8Q0ncU | 2026-04-04 | bogatyr |
| Dragoon | 1E6YgHK0CEpirhbvAmy9Ihg9oQpr-p_YUymbUbDEqhVI | 2026-04-04 | dragoon |
| Judicator | 1GY8_zURMNKZvxkV-PCkG82Rh1q7tErvFBWoSi3wvNWU | 2026-04-04 | judicator |
| Pyroclast | 1CQTFJrQyW8YNvU9pIaJZcFEHGSTrVjA7jQS0aTTYMNg | 2026-04-04 | pyroclast |
| Theurgist | 12VsVNQCmCmFy65ROmubdzaD4Vg1UTeLuNyBe9HhDyKo | 2026-04-05 | theurgist |
| Venefex | 1yc3TyFu0HmD_P6TDO7I_z0SIYkiZwq7SVprj_zc8WcA | 2026-04-04 | venefex |

`DungeonStartupSchematicPlan` selects `d1_<class>.schem` and
`d1_b1_<class>.schem` through `d1_b5_<class>.schem`. Six logical slots across six
groups produce 36 paste operations; unused slots use existing blankslot files.
`DungeonStartupSchematicPipeline` reads the original WorldEdit clipboard and pastes it;
Batch33 adds no chest transform, item manufacture, attunement pass or drop assignment.

| Source camp | Source reference coordinate | Code group |
| --- | --- | --- |
| 0 | 690 -59 69 | d1_start |
| 1 | 640 -60 60 | d1_b1_chests |
| 2 | 628 -19 106 | d1_b2_chests |
| 3 | 619 -1 119 | d1_b3_chests |
| 4 | 630 22 68 | d1_b4_chests |
| 5 | 1643 80 4210 | d1_b5_chests |
| 6 | 1637 98 4253 | No seventh startup paste group |

Source coordinates are room references, **not exact chest positions or adoption authority**.
Actual per-slot paste origins/rotations differ and remain unchanged.
The source-to-group association is a navigation aid; verify actual container ownership,
full serialized stacks, each slot, player class and all 36 pastes in TEST.

Preserve repeated Arrow rows in Bogatyr/Venefex lists and all authored quantities.
Do not normalize Pyroclast Cinderkiss/Cinderbight or launcher names into new items.
September20 readiness fix: Cameron explicitly requested the newest-document resolution.
Judicator Camp3's later chest list now grants Lux access with an independent Judicator
config entry, default8HP (four hearts), while preserving every authored chest stack.
Theurgist's Tide's Turn and the named vanilla totems remain as authored, without
constructing deferred conduits. Metalmancer and Deadeye loadouts stay D2+.

## Named drops

All 23 existing catalog identities retain their base item and configured vendor purchase
price. New named adoption additionally requires the exact documented applied enchantment
map, including no extra curses or foreign-namespace enchantments. Generic approved loot
and vendor retail classification stay separate. Existing saved named provenance is not
rewritten or retroactively invalidated by this authoring-only signature gate.

The canonical table follows; these are identity signatures, not runtime stat overrides.

| Identity | Enchantments | Default purchase Trace |
| --- | --- | --- |
| ranseur_of_the_fallen_dragoon | channeling 1, impaling 4, loyalty 3, unbreaking 3 | 779 |
| recovered_spyglass | None | 900 |
| perforated_chestplate | fire_protection 1 | 976 |
| salvaged_leggings | blast_protection 1 | 951 |
| discarded_helm | thorns 1 | 930 |
| resoled_boots | feather_falling 1 | 910 |
| last_resort | sharpness 2, unbreaking 2 | 676 |
| squared_mallet | wind_burst 1, smite 3, fire_aspect 1 | 717 |
| brutes_key | efficiency 4, unbreaking 1 | 848 |
| lash_of_the_crumbling_front | power 4, punch 2, infinity 1 | 348 |
| fibril | power 4, infinity 1 | 332 |
| triptych | multishot 1, quick_charge 2 | 406 |
| web_cautery | bane_of_arthropods 4, fire_aspect 1 | 651 |
| can_opener | wind_burst 1, breach 2, fire_aspect 1 | 666 |
| skeleton_key | efficiency 3 | 695 |
| dead_reckoning | power 3, flame 1 | 300 |
| loophole | power 2, punch 1, infinity 1 | 294 |
| traitors_enfilade | piercing 2, quick_charge 1 | 415 |
| cold_comfort | smite 3, sweeping_edge 1, knockback 1 | 267 |
| the_adjuster | efficiency 2 | 230 |
| limb_lopper | sharpness 1, efficiency 2, unbreaking 1 | 283 |
| severance_pay | sharpness 2, efficiency 2 | 280 |
| second_thought | fire_aspect 2, knockback 1, unbreaking 1 | 234 |

The source specifies six bosses/twelve entries, two entries per boss sharing a 10% budget;
ten standard spawners/eleven entries use 1% per mob with four to five mobs each. It supplies
no placed spawner UUID-to-item table. Expected 1.04-1.15 named items per run is not a cap,
minimum or guarantee. No item placement, probability or spawner preset was changed here.

Detailed code TODOs retain unknown source mappings, Cinderkiss/Cinderbight, deferred
D2+ ammunition and unloaded/nested storage. A future improvement is a read-only, native
TEST export comparing exact template stacks with this approved reference.
