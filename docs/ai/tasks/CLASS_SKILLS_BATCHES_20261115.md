# Approved class-skills series — 2026-11-15

Cameron's latest authorization: Batches 1–3 completed; execute Batch 4, then pause. Batches 1–5 are unpublished source betas; update development files and GitHub each time. Do not update TEST, local client, or CurseForge until Batch 6 is finished; release that completed set as an official stable build. Never launch or stop a game/server automatically.

The six cards below retain the settled design. Abilities and effects apply consistently to players and their class mercenaries; the latter use applicable abilities automatically. Preserve existing resurrection and utility behavior.

1. Shared signature skills and generic input foundation
Create shared Positive Potions, Negative Potions, Wolves, Fireworks, Chain Lightning and Smite skill identities for players/mercenaries. Preserve existing weapon progression and mercenary counters; add compatible progression storage/readers and truthful initial states. Add generic Primary class skill G and Class skill 1/2/3 Z/X/C in native Controls, class-independent remaps, inventory slots showing number/name/current key in small italics. Unimplemented slots remain disabled; no premature new combat/buffs. Preserve current Bogatyr utility and Theurgist resurrection. Server authority, stale/class/run rejection, bounded delta sync. Tests for old saves/unknown-field preservation, independent progression, codecs, inputs, class switching, native server compatibility.

2. Theurgist max-health progression
Theurgist mercenary potion use restricted to healing/max health only; player retains positive potion crafting. Scintilla Vitalis L0 +10%, Favilla Vitas L25 +15%, Tepor Sanans L35 +20%, Flamma Vitalis L42 +25%, Lux Vitalis L50 +30%, Radius Sanans L55 +35%, Stella Sanans L59 +40%, Aurora Reficiens L63 +45%, Balsamum Solis L66 +50%, Fulgor Vitae L68 +55%. Same standard Healing recipe cost every rank, highest rank only craftable; old physical bottles preserved, strongest family wins. Base health percentage, no healing, fixed45min. Potion cooldown first reductionL25: max(40,ceil(base/(1+.06*max(0,L-24)))). Preserve resurrection progression. Instance unlock broadcast and helpful tooltip. No invented scaling table for other positive potions.

3. Charged primary combat abilities
Dragoon100 valid direct hostile trident hits (melee or thrown); no self-charging lightning secondary damage. G arms next valid trident strike, auto-arm mercenaries, replace random player trigger. Judicator Smite replaces merc random Combat burst;10 undead kills charge, only damage undead. Smite startsL1, advance after10/20/30 additional successful casts (cumulative10/30/60): Gravehammer radius3 damage2.5HP; Bonegrinder radius4 damage2.5; Deathpurge radius5 damage5; Last Rites radius6 damage5. No-damage activation preserves charge and gives exact screen text: Smite only works on undead! You did not damage any mobs. Valid use counts once regardless number targets. Preserve run/lifecycle and friendly protections.

4. Shared class buffs and mercenary automatic use
All area buffs include caster, same-party players/mercenaries/allied wolves, radius10 unless specified. Bogatyr Flurry of Fangs +10% actual attack speed30min; Dragoon Blackiron Sheath20% damage reduction15min; Judicator Aegis+3damage atSmite1, Bulwark+6damage atSmite3 replace previous strength tier30min; Venefex Banish Afflictions removes harmful only. All5min cooldown, Z firstbuff/X/C reserved, no Pyroclast newbuff. Shared strongest-wins families/caster/duration authority; merc automatic useful use. Small activation text, actual-key italic button labels. Judicator broadcast exact approved wording.

5. Independent mercenary anti-farming protections
Spawner kills keyed run/dimension/spawner identity; warn privately at30, refuse at40 absent group player death or meaningful player participation (movement AND attacking mobs). Count owned secondary attacks/wolves once per real death; repeat refusal for blocked encounter with bounded message throttle. Separate healing detector: repeated damage with <1block X/Z displacement30sec warning,40sec stophealing, resume after1block horizontal movement. Attacks alone do not clear healing detector;1block alone does not prove spawner combat. Preserve counters through relog/mercrespawn, newrun reset. Retain exact user dialogue (fine typo corrected). No placed-spawner rebuild or authored preset changes.

6. Party UI, integration and official release
Worldparty name/yellow class/blue abbreviated signature-level. Inventory effects show name/amount/caster/readable remaining duration with colors; cooldown timers visible in group management,5minute displays for buffs; charge displays per provided sketches. Complete all-class/co-op/lifecycle/migration/server/client regression and current guides; merge completed validated series. Official stable build only after Batch6 finished; then CurseForge/runtime+companion and eligible requested targets per fresh state checks. No publication/deployment before this milestone; never auto-launch/stop games/server.

## Completion and integration requirements

Batch 1 establishes storage, identities and inputs. Later batches must connect real successful-action earning for Negative Potions, Wolves and Fireworks as well as the charged abilities, reusing actual existing useful-action events without double credit. Do not treat an initial level or an exposed slot as a functional ability.

Five-minute buff cooldown displays are distinct from charge counts and effect duration. Preserve charge on a Smite cast that damages no undead. Charge and proficiency are independent; one successful Smite activation earns one progression count even if it hits multiple undead. G arms lightning rather than firing immediately.

Anti-farming dialogue: at 30 spawner kills, "I grow weary of fighting these monsters alone. You look well rested, get over here and help me!" At 40, "That’s it. I’m putting my sword away. You fight, I’ll watch. You’ve shown me how that part works." Healing warning: "Your sword arm looks fine to me. I prescribe using it!" Refusal: "I'm done healing you. If you get injured, rub some dirt on it or walk it off!"

Potion unlock broadcast: <player/merc_name> has a new understanding of potions. <italic><new potion name> unlocked</italic>. Judicator buff broadcast: <player/merc name> shares renewed strength for battle. <blue><italic><skill name></italic></blue>.

Batches 2–5 each need their own beta version and exact source/CI receipts; distribution stays held. Batch 6 requires all six-class regression, completed-set review and an official stable release. Preserve all existing saved identities and counters.
