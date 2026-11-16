# Signature skills and shared controls

Players and mercenaries share these signature identities. The current beta includes max-health potion progression and charged Dragoon/Judicator combat:

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

Remap these entries in Options > Controls > Key Binds > Cosmic Dungeon. Bindings stay the same when changing classes. Each button shows its current binding in smaller italic text; unbound keys are labeled Unbound. Unassigned slots remain gray. Primary class skill now arms Dragoon Chain Lightning or activates Judicator Smite. Its inventory button shows the current charge and readiness/armed state. Other primary slots and Class skill 1/2/3 remain unassigned; later batches add their abilities. The existing Alt+1/2/3 Bogatyr utility and Alt+4 resurrection shortcuts retain their saved bindings.

Shortcuts act once per press in the world or player inventory. Chat, other menus, carried stacks, dragging, modal controls, lost focus and conflicting bindings block execution. A rejected action gives a short on-screen reason. The server owns the active class, run, slot availability and revision.

Positive Potions begins with the player's exact existing potion XP and continues from eligible useful support. Its independent level ceiling is 100 so future ranks above the old weapon-skill ceiling can be reached; existing weapon progression is unchanged. New non-potion player signature tracks begin at level 0, except Smite at level 1. Chain Lightning and Smite now earn one proficiency success per activation that actually damages an eligible target. Other new player signature earning hooks remain for later integration; their display alone does not activate an ability.

Existing mercenary counters and their existing level curves are preserved. Smite receives a separate successful-cast counter instead of reinterpreting historic Combat kills. Mercenary progress remains run-based. Player signature records persist independently per player and class across saves and reconnects.

Positive Potions now selects the [max-health potion rank](Theurgist_Help_Guide.md#max-health-potions), from Scintilla at level0 to Fulgor at68. Useful new/stronger max-health applications can earn support XP; equal/weaker applications cannot refresh or train the skill.

## Charged primary skills

- **Dragoon:** 100 valid damaging melee/thrown trident hits fill one bar. The player presses G (or the remapped Primary class skill) to arm the next valid trident strike. Mercenaries automatically arm at 100. A chain with no effective additional target retains the full armed bar.
- **Judicator:** ten credited undead kills fill one bar. G activates an undead-only Smite around the player; mercenaries activate automatically when useful. [Smite ranks](Judicator_Help_Guide.md#smite) unlock after 0, 10, 30 and 60 successful activations. An activation that damages nothing retains all charge and displays: **Smite only works on undead! You did not damage any mobs**

Both bars have no overflow bank. Secondary ability damage cannot refill them. Charge and the armed state survive death/reconnect in the same run and reset with the run; player proficiency persists separately, while mercenary proficiency stays with its run. Historical mercenary Combat progress is retained separately from Smite.

This beta shows charge/readiness in the inventory Skills panel. The final combat HUD is reserved for Batch 6.

See [the six-batch plan](../ai/tasks/CLASS_SKILLS_BATCHES_20261115.md) for the approved future behavior.
