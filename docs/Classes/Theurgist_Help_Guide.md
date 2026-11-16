# Theurgist Help Guide

Class ID: `theurgist`. Enabled for Dungeon 1.

A healer-alchemist who balances care and chemistry. Use mace/bow and Theurgist-attuned gear; beneficial potion work and successful support contribute to the appropriate passive skills. Mending Sting and Verdant Jolt provide regenerative aid. The authored restorative arrows named Scintilla Vitalis and Lux Vitalis heal living targets and harm undead; these arrows are separate from the max-health potions below. Read actual item effects and server values rather than assuming every named arrow is an ordinary damage arrow.

Your positive thrown potions aid allies and their companions and do not affect hostile mobs, including healing damage against undead. Restorative class arrows have their own rules.

The **Potions** list appears automatically beside inventory. Only Theurgists may interact with brewing stands; valid brews finish rapidly under the server's instant-brewing setting while the stand is open. The stand still needs a valid recipe and fuel. Eon Penrose sells class-gated brewing equipment and advanced healing stock.

**Resurrect...** is available immediately in your inventory; its default shortcut is **Alt+4**. Opening the picker does not cast. Choose a dead teammate in the same active run. They have 60 seconds to accept or decline on the death screen. A successful accepted resurrection returns them to their latest recorded death location with five seconds of protection and the existing recoverable-inventory handling. It does not recreate destroyed or missing physical drops.

Player resurrection costs no items or currency and starts a **180-second cooldown only after success**. Declined, expired, stale, or failed offers start no cooldown. The cooldown survives death, reconnect, and run cleanup; server downtime pauses this game-time cooldown. Party hover and the death screen show each caster's readiness, cooldown, or reason for unavailability. Mercenary Theurgists retain their separate Positive Potions level-10 resurrection unlock and their displayed cooldown rules.

Hover a potion tile to see the server-selected route for one output: the one existing potion input consumed, exact remaining ingredient quantities, and own/eligible-group stock. Intermediate stages made within that route are not charged again. The required blaze powder total includes both any reagent and new fuel refill; the fuel line identifies the refill portion, while saved charges reduce it. Own affordable routes are preferred before pooled routes. Unavailable tiles quote no affordable route. Quantities reserve nothing; a pending click shows the previous snapshot until the server replies, and execution rechecks all costs.

## Max-health potions

Your **Positive Potions** level selects one named max-health potion in the inventory Potions list. It costs exactly the same ingredients and fuel as standard Splash Healing at every rank. Starting from water: nether wart, glistering melon slice, gunpowder, and three brewing charges; supported intermediate bottles skip completed steps. Only your highest unlocked rank is craftable. Existing lower-rank bottles remain usable.

| Positive Potions level | Potion | Extra max health |
|---|---|---|
| 0 | Scintilla Vitalis | 10% of base health |
| 25 | Favilla Vitas | 15% |
| 35 | Tepor Sanans | 20% |
| 42 | Flamma Vitalis | 25% |
| 50 | Lux Vitalis | 30% |
| 55 | Radius Sanans | 35% |
| 59 | Stella Sanans | 40% |
| 63 | Aurora Reficiens | 45% |
| 66 | Balsamum Solis | 50% |
| 68 | Fulgor Vitae | 55% |

A splash grants the full **45 minutes** to the caster and same-party players, mercenaries and allied wolves within **10 blocks of impact** in the active dungeon. It raises maximum health without healing. A stronger rank replaces a weaker rank; equal or weaker potions cannot stack, refresh the timer or earn support progress. Expiry removes the extra maximum and clamps current health if necessary. Buff time follows normal Minecraft effect ticking. An unlocked rank is announced to dungeoneers in that instance.

Theurgist players keep the beneficial potion catalog, including normal/strong variants and the existing mixed-effect drinkable Turtle Master. The extra class potion bonus is zero through Positive Potions24 and starts at25, using the existing configurable slope/cap. The named percentage table applies only to max health.

Theurgist mercenaries brew and throw only **Healing and max health**. Their quality and cooldown bonuses start at Positive Potions25. Below25 they use standard Healing; at25 the existing chance of stronger Healing begins. A max-health throw uses the mercenary's current rank. Old supplies remain stored, but unsupported potions cannot be used or block the useful-stock cap. Resurrection retains its existing progression and cooldown.

## Shared rules and controls

See the [current player guide](../PLAYER_GUIDE.md) for party sharing, crafting/fuel/stacks, gear ownership, trading, progression and travel. [Controls](../PLAYER_GUIDE.md#controls-and-passive-progression) lists defaults and rebinding. The inventory class-name link opens this class in the H guide.
