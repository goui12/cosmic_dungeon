# Mercenary class roles and Bogatyr dogs

## Behavior
All six selectable classes already used the shared follow/recovery/combat brain and class-specific equipment permissions; they did not have six separate tactical AIs. Theurgist now exclusively acquires potion/reagent supplies, brews, uses potions and produces the existing fallback healing potion. Other classes follow and fight, continuing useful equipment/ammunition copying. Existing potion stock is preserved but unused by non-Theurgists.

Theurgists complete one native recipe from their private supplies immediately when the existing two-second collection poll finds a nearby accessible brewing stand. This removes the old 400-tick recipe delay and portable brewing. The existing loaded-block, distance, line-of-sight and class/lock checks remain. Stand slots and fuel are never consumed or rewritten. Processing is bounded to one recipe per poll, not an unbounded recipe chain.

Each Bogatyr summons a native tamed wolf after 2,400 active server ticks, then another each 2,400 active ticks, up to five per mercenary. Rest, unloaded mercenaries and absent hirers pause that clock. Full packs retain the elapsed cooldown; after an observed death, replacement is possible once the cooldown has elapsed. Failed safe placement retries at most once per second. Wolves use the hirer's native ownership for following and combat credit and the mercenary's hostile target when available.

Run rosters count unloaded wolves and survive reloads. Death frees a slot; unload does not. Breeding cannot bypass the cap. Dismissal/end-of-run removes loaded summons; unloaded summons are rejected when they rejoin. Summons do not enter the player's permanent Bogatyr pet directory. Commands and dismissal verify the wolf's exact run/mercenary/hirer bond.

The existing ally predicate protects players, owned companions and friendly NPCs. Summoned wolves cannot target or damage allies and reject allied melee/projectile damage and harmful splash/lingering effects. Beneficial effects and hostile/environmental damage remain possible. Existing instant-effect interception is reused.

## Exact changed files
Production, under src/main/java/net/goui/cosmicdungeon/:
- mercenary/MercenaryBrewing.java and MercenaryWolves.java (new helpers).
- mercenary/MercenaryBrain.java, MercenaryCollection.java, MercenaryConfig.java, MercenaryEntity.java, MercenaryInventory.java, MercenaryLifecycle.java and MercenaryPotions.java.
- playerclass/bogatyr/BogatyrWolfEvents.java.

Tests, under src/test/java/net/goui/cosmicdungeon/mercenary/:
- MercenaryRolesTest.java, MercenaryWolvesTest.java and MercenaryTestWolf.java (new).
- MercenaryFriendlyFireTest.java, MercenaryCopyLootTest.java and MercenaryEntryTest.java.

Documentation: this report; [task card](tasks/mercenary-class-roles-20261004.md); [release fragment](../releases/fragments/mercenary-class-roles-20261004.md).

## Compatibility and boundaries
Optional mercenary_wolf_ticks defaults to 2,400 for old entities. Legacy brewing timer fields/config remain readable. Existing inventories, equipment and loot memory are retained. Native wolf persistent data contains a versioned run/mercenary/hirer bond; roster and dismissal entries use the existing D1RunData codec. Native codec round trips cover these additions. No registry IDs, packets, spawner/door/rift/vendor/currency/preset formats change; no migration or manual spawner replacement is required.

All decisions run server-side; common sources add no client imports. Work is bounded: existing local 75-position collection scan, at most five wolf identity lookups per AI pass, and a local safe-placement search only when a summon is due. Native wolf pathfinding adds up to five normal pets per Bogatyr; live multiplayer performance is not measured.

Class access and locked container checks are reused. Door keys and player-authored stacks are unchanged. Vendors/currency, networking/menu state, faction/reward formulas, rift/reset formats and Cosmic Spawners are untouched. Permanent pet enrollment has only the explicit summoned-wolf exclusion. Native hirer attribution is used, but live progression/kill-credit behavior remains QA.

## Validation
Final Java 21 Gradle build passed. All 261 native JUnit tests passed (zero failures, errors or skips), all 2,001 JSON files under src parsed, and git diff --check passed. Tests exercise private brewing without stand mutation, non-Theurgist restrictions, invalid recipes/locked stands, old cooldown defaults, NBT/run-roster round trips, cap and death/unload behavior, dismissal/admission, bond isolation, breeding and friendly combat/effects.
The native wolf test fixture supplies vanilla dynamic variant registries without launching a world. Earlier fixture-only failures were fixed without weakening production assertions.
No datagen is applicable: no recipes, tags, models or generated resources change.
No dedicated/GameTest server or client launch was performed. Destructive clean is omitted to preserve the historically tracked JAR.

## Pending licensed gameplay QA
1. Hire each class: all follow and fight; only Theurgist collects/uses potion supplies. Existing non-Theurgist potion stock remains unused.
2. Give Theurgist copied bottles/reagents near an unlocked stand: each eligible recipe completes on its next collection poll; player stand slots/fuel are unchanged. Away from a stand or at a locked stand, no brewing occurs.
3. Keep Bogatyr active: one dog at two minutes, five by ten minutes, no sixth. Unload/reload and restart to check the cap/timer; have a hostile kill one and check replacement timing.
4. Try allied melee, arrows, harmful/mixed splash and lingering potions on dogs, including another player's attacks. No allied damage/debuffs or retaliation; healing works and hostile attacks still work.
5. Finish/leave the run: summoned dogs disappear, including previously unloaded dogs on reload. Permanent player pets remain. Verify native following and hirer kill credit in multiplayer.

Back up the world before testing. JAR rollback alone does not undo already saved entity changes; restoring a pre-test world backup is the complete rollback route. An older build will not enforce the new summon marker rules.
Possible future improvement: add licensed multiplayer regression coverage for native wolf navigation and mixed potion effects.
