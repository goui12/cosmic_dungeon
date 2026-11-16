# Class skills Batch 4 — Shared party buffs

Cameron authorized Batch 4, then pause. Baseline a3e9e6e81ffd98988bbafaa2e26f342aa27a8783 is completed Batch 3. Branch feature/class-skills-b4-20261116 continues the unmerged series in C:\\Users\\Cameron\\Documents\\mod_development\\cosmic_dungeon_skills_20261115.

## Delivery

1.8.12-beta.1 is an unpublished source beta. Update Goggins development source and GitHub only. No TEST, local client, CurseForge, release tags or distribution before Batch 6 finishes. Two cards remain: independent anti-farming, then party UI/integration/official release. [Approved plan](CLASS_SKILLS_BATCHES_20261115.md).

## Behavior

| Class | Class skill 1 (default Z) | Effect | Duration |
|---|---|---|---|
| Bogatyr | Flurry of Fangs | +10% melee attack speed | 30 minutes |
| Dragoon | Blackiron Sheath | 20% native Resistance damage reduction | 15 minutes |
| Judicator, Smite 1 | Judicator’s Aegis | +3 melee attack damage (1½ hearts) | 30 minutes |
| Judicator, Smite 3 | Defender’s Bulwark | +6 melee attack damage (3 hearts), replaces Aegis | 30 minutes |
| Venefex | Banish Afflictions | Remove harmful effects; retain beneficial and neutral effects | Instant |

All use a five-minute cooldown and an inclusive spherical 10-block radius. Recipients are the caster, same-run party players, admitted mercenaries and allied wolves. Foreign/retired companions, outsiders and enemies are excluded. Mercenaries automatically cast when the ability is ready and at least one eligible target benefits. No milk bucket or crafting materials are spent.

Player melee uses ATTACK_SPEED; mercenary and wolf native melee clocks receive the matching sustained 10% increase. This does not change bow draw, crossbow loading, projectile cadence, movement or mining. Mercenary AI keeps its existing ten-tick decisions, preserving fractional timing instead of rounding away the buff. Idle time cannot bank extra attacks. Wolf pathfinding cadence is unchanged.

Strength and Resistance reuse native effect families, so equivalent potions cannot double their modifiers. Stronger effects replace weaker ones; equal/weaker class-family applications neither refresh nor stack, and overwritten weaker effects cannot return through a hidden native chain. A wholly ineffective or canceled cast consumes no cooldown. Reservation before native callbacks prevents reentrant duplicate casts; compare-before-refund preserves newer state and never recreates cleared run data.

Z and its inventory button share server authority. Buttons show the numbered slot, current remapped italic key and a readable m:ss cooldown. Successful player casts show their skill name. Strength broadcasts exactly "<caster> shares renewed strength for battle. <skill>" with the skill blue and italic. X/C remain gray; Pyroclast has no new buff. Existing Theurgist potion and resurrection behavior is preserved.

## Compatibility and cost

Cooldowns use additive version1 keys in existing D1 SavedData, keyed by player/mercenary contract and class. Native game time determines expiry. Save/reconnect/death/class switching retain the same-run cooldown; Judicator rank changes share it; normal run cleanup removes it. Invalid or unknown deadline shapes fail closed without overwriting stored data. No synchronous save or data-version change.

Timed effects retain caster UUID/name, skill, title and amplifier in additive entity metadata for Batch6 party inspection. Unknown metadata fields survive. Native replacement clears stale attribution. Existing native effect serialization retains duration and strength. A new Flurry effect registration and generated native haste-icon alias require matching protocol38 binaries; payload schemas are unchanged. Downgrade-saving newly registered effects is not supported. This source series is not installed.

Queries are bounded to nearby entities, without a global entity scan or arbitrary player-wolf cap. Mercenary useful-target decisions run at most once per second within existing AI. Existing delta snapshots carry cooldowns. No dependency, thread, authored chest, equipment, preset, spawner, loot or resource transaction changes.

## Validation and review

Java21 local Gradle build, unit tests, JSON/link/diff checks, effect-atlas datagen and exact-head full Integration Gate clean build/native GameTests are required. Exact counts, source, PR and CI IDs live in operational batch04 evidence. No local client/GameTest/server launch.

Unit checks cover the buff catalog/ranks/durations, malformed and expired deadlines, timer formatting, independent cooldown keys and fractional haste/idle/ranged behavior. Native tests cover actual generic actions, party/radius boundaries, real player attributes and damage, mercenary auto-use, wolf/mercenary sustained melee, harmful-only cleanse, canceled/no-op refunds, callback lifecycle/replacement races, strongest-only native families, metadata and SavedData/entity round trips, class/death/run boundaries and malformed state.

One cooperative worker implemented/reviewed the disjoint melee cadence paths and independently reviewed shared effect/cooldown authority. Review found and corrected the callback refund lifecycle race with native regression coverage. Relevant offline canon was read with source provenance and hidden-sheet checks; explicit approved class-skill mechanics supersede older item/ability descriptions. No class-chest or full semantic-canon audit is claimed.

After Batch6 delivery, verify all four buffs at two GUI scales with remapped Z and inventory activation, 10-block party/wolf boundaries, m:ss cooldowns, actual attack cadence, cleanse retention and strength upgrade/broadcast. Repeat with mercenaries, death/reconnect/class switching and a fresh dungeon. Licensed native rendering/co-op acceptance remains TESTING, alongside all16 active P2 entries in the [active ledger](BATCH_1_7_20_REVIEW_NOTES_20261012.md).

## Changed file groups

Shared buff catalog, authority, cooldowns and effect provenance; Flurry effect/icon registration and native family mixin; player actions/inventory timers; mercenary auto-use and melee clock; wolf melee hook; native and unit fixtures/registrations; protocol38; current class/player/help guidance, beta version, release notes, plan and active ledger. Exact paths are in evidence/batch04/changed-files.json.
