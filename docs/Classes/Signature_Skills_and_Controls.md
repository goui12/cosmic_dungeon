# Signature skills and shared controls

Players and mercenaries share these signature identities. The current release includes max-health potion progression, charged Dragoon/Judicator combat and shared party buffs:

| Class | Signature | Abbreviation |
|---|---|---|
| Bogatyr | Wolves | WLF |
| Dragoon | Chain Lightning | CL |
| Judicator | Smite | SM |
| Pyroclast | Fireworks | FW |
| Theurgist | Positive Potions | PP |
| Venefex | Negative Potions | NP |

The inventory Skills panel shows the current player signature level and progress. Existing weapon skills, Bogatyr commands, potion palette, and Theurgist resurrection remain available under their existing rules.

| Controls entry | Default | Inventory slot |
|---|---|---|
| Primary class skill | G | Primary class skill |
| Class skill 1 | Z | Class skill 1 |
| Class skill 2 | X | Class skill 2 |
| Class skill 3 | C | Class skill 3 |

Remap these entries in Options > Controls > Key Binds > Cosmic Dungeon. Bindings stay the same when changing classes. Each button shows its current binding in smaller italic text; unbound keys are labeled Unbound. Unassigned slots remain gray. Primary class skill now arms Dragoon Chain Lightning or activates Judicator Smite. Its inventory button shows the current charge and readiness/armed state. Class skill 1 activates the four buffs below; other primary slots and Class skill 2/3 remain gray. The existing Alt+1/2/3 Bogatyr utility and Alt+4 resurrection shortcuts retain their saved bindings.

Shortcuts act once per press in the world or player inventory. Chat, other menus, carried stacks, dragging, modal controls, lost focus and conflicting bindings block execution. A rejected action gives a short on-screen reason. The server owns the active class, run, slot availability and revision.

Positive Potions begins with the player's exact existing potion XP and continues from eligible useful support. Its independent level ceiling is 100 so future ranks above the old weapon-skill ceiling can be reached; existing weapon progression is unchanged. New non-potion player signature tracks begin at level 0, except Smite at level 1. Chain Lightning and Smite now earn one proficiency success per activation that actually damages an eligible target. Negative Potions earns one success per useful harmful splash/cloud: actual instant damage, a new/stronger non-damaging harmful effect, or actual poison/wither tick damage. Multiple targets/ticks from one launch cannot multiply credit. Fireworks earns one success per rocket that damages a hostile. Wolves earns one success when a genuinely new owned wolf is admitted to the active run; repeated registration/reloading cannot repeat that credit. Misses, denied effects and inactive/wrong-class actions earn nothing.

Existing mercenary counters and their existing level curves are preserved. Smite receives a separate successful-cast counter instead of reinterpreting historic Combat kills. Mercenary progress remains run-based. Player signature records persist independently per player and class across saves and reconnects.

Positive Potions now selects the [max-health potion rank](Theurgist_Help_Guide.md#max-health-potions), from Scintilla at level0 to Fulgor at68. Useful new/stronger max-health applications can earn support XP; equal/weaker applications cannot refresh or train the skill.

## Charged primary skills

- **Dragoon:** 100 valid damaging melee/thrown trident hits fill one bar. The player presses G (or the remapped Primary class skill) to arm the next valid trident strike. Mercenaries automatically arm at 100. A chain with no effective additional target retains the full armed bar.
- **Judicator:** ten credited undead kills fill one bar. G activates an undead-only Smite around the player; mercenaries activate automatically when useful. [Smite ranks](Judicator_Help_Guide.md#smite) unlock after 0, 10, 30 and 60 successful activations. An activation that damages nothing retains all charge and displays: **Smite only works on undead! You did not damage any mobs**

Both bars have no overflow bank. Secondary ability damage cannot refill them. Charge and the armed state survive death/reconnect in the same run and reset with the run; player proficiency persists separately, while mercenary proficiency stays with its run. Historical mercenary Combat progress is retained separately from Smite.

The inventory Skills panel and party cards show charge/readiness independently from timed buffs.

## Shared party buffs

Class skill 1 (default **Z**) activates the current class buff from the world or inventory. Remap it once in Controls; the numbered button shows your current key in small italics and a **m:ss** cooldown. Each buff has a **five-minute cooldown** and includes the caster, nearby party players, mercenaries and allied wolves within **10 blocks** in the same active dungeon.

| Class | Buff | Effect | Duration |
|---|---|---|---|
| Bogatyr | Flurry of Fangs | +10% melee attack speed, including mercenaries and wolves | 30 minutes |
| Dragoon | Blackiron Sheath | 20% damage reduction through native Resistance | 15 minutes |
| Judicator, Smite 1 | Judicator’s Aegis | +3 melee attack damage (1½ hearts) | 30 minutes |
| Judicator, Smite 3 | Defender’s Bulwark | +6 melee attack damage (3 hearts), replacing Aegis | 30 minutes |
| Venefex | Banish Afflictions | Remove harmful effects only; keep beneficial and neutral effects | Instant |

Mercenaries cast automatically when useful. Equal/weaker timed buffs cannot stack or refresh the current family; a stronger effect replaces it. Strength and Resistance share their native potion effects instead of adding duplicate bonuses. A cast that helps nobody spends no cooldown. Flurry changes melee cadence, not ranged attacks or mining.

Cooldowns survive death, reconnect and class switching within the same run, and reset with a new run. X/C remain unassigned. Pyroclast has no new buff; Theurgist potion crafting and resurrection retain their existing controls. Party cards display effect attribution, remaining duration and class-buff cooldowns.

See [the six-batch plan](../ai/tasks/CLASS_SKILLS_BATCHES_20261115.md) for the approved six-batch scope.

## Mercenary participation and healing

Mercenary kills from one Cosmic Spawner share a party counter. The hirer receives a private warning at **30** and refusal at **40**. The restriction includes melee, arrows, fireworks, charged area abilities, harmful potions and mercenary wolves. Other spawners and ordinary hostiles remain eligible.

To restore help, walk at least one block and actually damage enemies from that spawner. The movement must be recent (within ten seconds); simply looking, jumping vertically or being pushed does not prove walking. A party-player death resets all spawner counters. Reconnecting or reviving a mercenary does not reset them; a new dungeon does.

The separate healing rule observes each recipient. Repeated accepted injuries no more than ten seconds apart, while remaining within one horizontal block, warn after **30 seconds** and stop mercenary healing after **40 seconds**. Move one horizontal block to restore healing immediately. Attacking from the same position does not clear this restriction, and movement alone does not clear the spawner counter. A blocked player cannot receive healing indirectly from a splash aimed at an eligible teammate. Other allies, player potions, max-health buffs and resurrection remain available.

## Party skill and effect displays

World party names show the class in yellow and the abbreviated signature level in blue, for example **Cameron / Theurgist / PP – 35**. Hover details retain the full identity when a narrow layout clips the label.

Inventory party cards and group inspection show each available charge bar and class-buff cooldown. Buff cooldown bars have five one-minute sections; charge bars show the actual count and armed/readiness state separately. The client counts down between server snapshots; reaching zero locally never authorizes an action before server confirmation.

Hover an effect for its name and amount in green, **Cast by** the winning caster in blue, and **Remaining** in yellow (for example **32m 18s**). Historical effects with no recorded caster say **Unknown**. Equal/weaker rejected applications cannot change the winner. Offline/unloaded members retain known signature identity but show no invented effect or ability readiness.
