# Testing batch 9 — mercenary field behavior

Base: Batch 8 / aa54ebabe5380f8d686a184d8e2ee22b8bff9c27 / draft PR207.
Branch: feature/testing-fixes-batch9-20261004. Three approved action items implemented.
Two implementation batches (10–11) remain; cumulative licensed gameplay acceptance is separate.
[Approved rollout](TESTING_ROLLOUT_20261003.md) | [Task card](tasks/testing-fixes-batch9-20261004.md).

## Behavior and boundaries

Each active mercenary follows its living, non-spectator, non-developer hirer in the exact
registered D1 instance. No chasing into a lobby, template, unrelated run or another dimension.
Missing/offline/dead/departed hirers pause decisions and production. Recovery tries 32
nearby loaded positions after separation beyond 16 blocks or five seconds stalled more
than four blocks away. Existing SafeTeleportUtil hazards and collision checks reject
unsafe destinations; no chunk tickets, forced loading or duplicate entity creation.

Ten-tick decisions reuse native navigation, melee attacks, equipped weapon attributes
and compatible bow/crossbow arrows. Targets must be hostile or attacking a party ally,
visible, and within eight blocks of the hirer. The shared companion-friendly policy excludes
players, NPCs, villagers, owned pets and mercenaries. Arrows revalidate at impact; marked
orphan projectiles fail closed. Existing D1 arrow identity/class/run rules also accept the
valid mercenary class; human behavior remains unchanged. Supportive D1 arrows and authored
rockets stay in supplies; this pass uses potions for support and arrow ammunition for ranged
attacks, with melee fallback when no compatible attack arrow is available.

OwnableEntity integrates existing companion kill-credit and friendly policies. Mercenaries
remain outside the human run roster, completion recipients and class-skill XP attribution.
No currency, progression or player inventory transaction service was duplicated.

Collection checks 24 nearby item entities and 75 loaded block positions every two seconds,
transferring at most four whole stacks. Public chests, barrels, brewing stands and class
chests with matching class/personal owner are eligible. Locked containers, machines,
foreign personal slots, pickup delays/target owners and foreign/malformed bound stacks are
rejected. Line of sight and short interaction distance apply. Names, payloads, quantities
and components move intact; there is no ownership binding or authored loadout rewrite.

The 54-slot supplies inventory feeds the server PotionBrewing registry, including native
and registered custom recipes. Each cycle consumes one input and one reagent, with the
native resulting stack and ingredient crafting remainder. Inventory planning is atomic:
if the full result/remainder cannot fit, nothing is consumed. This is the approved portable
mercenary auto-brew, not a player brewing stand or a new crafting recipe/recipe allowlist.
The ingredient itself is the production cost; there is no separate stand fuel charge.
Native brewing transformations intentionally produce the recipe output; unrelated spare
stacks are unchanged. No player brewing/class access rule is relaxed.

Potions are selected only for useful effects. Ordinary bottles are self-drunk with a glass
bottle remainder; splash/lingering bottles use native projectile/cloud physics. Mixed effects
are checked individually at application: beneficial effects only reach current party allies,
harmful/neutral effects only valid enemies. Healing/harming inversion respects undead.
Potions with no useful target, overhealing and repeated equal buffs are not selected.
A single action may splash several allies. Leaving the run, owner loss and stale sources
cannot turn a mercenary cloud into an unrestricted player potion.

Server configuration under mercenaries adds brewTicks=400, fallbackHealingTicks=3600,
potionCooldownTicks=200 and potionCooldowns (individual ID=ticks overrides). Healing
defaults to 200 ticks, strong healing 300 and regeneration 400; every interval is bounded
20–1,728,000 ticks. Drink/splash/lingering variants share the same base potion timer.
Custom-only contents use the custom key. Fallback generates a splash healing potion every
three minutes of active simulation when space permits; a full bag defers until the next
production interval, without dropping output or retrying each tick. Timers are remaining
active ticks, so logout/unload/restart cannot accumulate offline production.

## Compatibility, performance and verification

The optional mercenary_timers entity field preserves independent brewing, fallback and
potion cooldowns. Missing fields initialize with delayed defaults; native equipment,
mercenary_contract, mercenary_run and mercenary_supplies remain unchanged. Codec
round-trip and old-shape coverage included. Registry IDs, protocol 11, spawner/preset
formats, doors, keys, rifts and account schemas are unchanged; no spawner migration.
Common server code contains no client-only dependencies. Existing vanilla entity tracking
and effect synchronization apply; no new packets, dependencies or heap changes.

Work is bounded per admitted mercenary (maximum three per six-occupant run): two decision
polls/second, 48 living candidates per poll, collection limits above, at most 54×54 recipe
pairs per configured brew cycle and 32 recovery candidates. Default brewing is once per
20 seconds. No disk/network polling, full-world scans or persistent global AI queues.
These bounds and existing navigation are reviewed; live multiplayer frame/tick cost has
not been profiled and remains part of licensed acceptance.

Java 21 build and 182 native NeoForge JUnit tests passed, zero failures/errors/skips.
Eighteen new tests cover transfer conservation/full bags/components, foreign ownership,
native potion chains and splash/lingering conversion, custom recipe remainders, invalid
recipes, saved/independent timers, cooldown identity, undead effect polarity and non-beneficial attack arrows, recovery
thresholds, bounded configuration and class/personal container permission.
The native custom-recipe single-input requirement was fixed after its regression test
caught the stacked-input case. The invalid-recipe fixture now uses dirt because stone is
a valid vanilla brewing ingredient. d1OfflineChecks passed, including 129 config checks
and two config round trips. All source JSON and changed-document links are checked.

No generated resources changed; datagen does not apply. No destructive clean, GameTest/
dedicated-server launch, world entry or licensed gameplay acceptance was performed.
Build and client-startup checks do not establish live pathfinding, projectile accuracy,
visual rendering or multiplayer acceptance. Matching world backups remain the rollback
boundary from Batch 8; downgrading loses new timers and must not cross pending hire fees.
Full no-drop death/retention, ten-minute respawn and stacked health HUD remain Batch 10.

## Pending licensed TEST checks

1. Start with two hirers/mercenaries. Walk, obstruct the path and separate beyond 16 blocks.
   Expect safe recovery near the correct hirer. Check hazards, loaded-chunk boundaries,
   logout/death, completion/forfeit and parallel instances; no duplicate or cross-run hire.
2. Fight with melee and compatible arrows beside players, wolves, NPCs and the other hire.
   Expect no friendly damage, existing companion payout and no human weapon XP from hire
   attacks. Supply mixed splash/lingering potions: only beneficial effects reach allies,
   harmful effects reach enemies; verify undead inversion, useful support and stale clouds.
3. Place permitted supplies beside foreign personal/locked chests and bound drops. Only
   eligible stacks transfer intact. Brew water→awkward→healing→splash→lingering, including
   full-bag and remainder cases. With no ingredients, check three-minute fallback healing;
   reload midway and verify remaining timers, configurable individual cooldowns and no
   offline production burst. Keep cumulative earlier QA pending.

## Exact files

- [docs/ai/D1_REMAINING.md](D1_REMAINING.md)
- [docs/ai/TESTING_BATCH_9_20261004.md](TESTING_BATCH_9_20261004.md)
- [docs/ai/TESTING_ROLLOUT_20261003.md](TESTING_ROLLOUT_20261003.md)
- [docs/ai/tasks/testing-fixes-batch9-20261004.md](tasks/testing-fixes-batch9-20261004.md)
- [docs/config-examples/CosmicDungeon.config](../config-examples/CosmicDungeon.config)
- [docs/releases/fragments/testing-fixes-batch9-20261004.md](../releases/fragments/testing-fixes-batch9-20261004.md)
- [src/main/java/net/goui/cosmicdungeon/block/entity/ClassLockedChestBlockEntity.java](../../src/main/java/net/goui/cosmicdungeon/block/entity/ClassLockedChestBlockEntity.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryBrain.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryBrain.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryCollection.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryCollection.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryConfig.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryConfig.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEntity.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryEntity.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryInventory.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryInventory.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryPotions.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryPotions.java)
- [src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryTimers.java](../../src/main/java/net/goui/cosmicdungeon/mercenary/MercenaryTimers.java)
- [src/main/java/net/goui/cosmicdungeon/mixin/SkillCloudMixin.java](../../src/main/java/net/goui/cosmicdungeon/mixin/SkillCloudMixin.java)
- [src/main/java/net/goui/cosmicdungeon/mixin/SkillSplashPotionMixin.java](../../src/main/java/net/goui/cosmicdungeon/mixin/SkillSplashPotionMixin.java)
- [src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1ArrowAbilities.java](../../src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1ArrowAbilities.java)
- [src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1ProjectileAccess.java](../../src/main/java/net/goui/cosmicdungeon/playerclass/d1/D1ProjectileAccess.java)
- [src/main/java/net/goui/cosmicdungeon/playerclass/skill/SkillPotions.java](../../src/main/java/net/goui/cosmicdungeon/playerclass/skill/SkillPotions.java)
- [src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryFieldTest.java](../../src/test/java/net/goui/cosmicdungeon/mercenary/MercenaryFieldTest.java)
