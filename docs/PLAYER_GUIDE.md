# Cosmic Dungeon player guide

Current guide for the 1.7.19 Alpha series. The in-game **H** guide contains the same current player rules, class pages, and full Village NPC backstories. Artwork illustrates the lore; it does not unlock a class, recipe, location, or encounter.

## Getting started and finding help

Meet Tamsin Vane, follow her introduction, choose an enabled class, and assemble your party at the class selector. Dungeon 1 entry is free. The six available classes are **Bogatyr, Dragoon, Judicator, Pyroclast, Theurgist, and Venefex**. Metalmancer and Deadeye remain unavailable.

Press **H** to read the guide. Use its left directory or underlined links to move between topics. **Alt+Left** or **Back** returns to the previous guide page. The wheel scrolls the pane under your pointer; **Page Up / Page Down** scroll the focused pane (information pane by default). **Tab / Shift+Tab** moves focus; **Enter** opens the focused link. The class name in your inventory opens your class guide. Demonstration item tiles are read-only: hover for the actual item tooltip; they cannot give, take, craft, equip, or move items. **Esc** closes the guide without pausing the world.

Your inventory keeps armor, offhand, storage, and hotbar controls. The old 2x2 crafting grid and recipe book are absent. Class actions occupy that space where applicable. Normal crafting remains subject to the server's recipe rules; a guide example is not a recipe unlock. Loot can arrive cooked when its native drop conditions allow it. Crafting, furnaces and automatic crafters still require approved recipes.

## Party, readiness, and sharing

The default party size is three to six. The leader starts readiness; each player confirms their own **Ready / Not Ready** state. Membership changes clear readiness. The leader uses **Start Adventure!** once the group is ready. Mercenaries do not have a Ready toggle. Ready controls disappear after entry. Starting an adventure cancels any party member's open trade safely.

The enabled **Ready** action is green with a restrained two-second pulse. It stops pulsing while waiting for the server or once the action becomes **Not Ready**; each player still controls only their own readiness.

The creator receives the resource-sharing choice on group creation; joiners receive it in Tamsin's flow. Choose **YES** or **NO** before starting. Choices lock as soon as **Start Adventure!** is accepted, including the queue and countdown, and stay fixed for the run. Change them after completion/forfeit returns you to pre-game; canceling a submitted start also returns to pre-game. YES permits eligible recipe ingredients in your inventory to help party crafting. It does not hand over your entire inventory. Hover a party member to see their class, health, skill progress, sharing choice, resources used/contributed, and relevant resurrection availability.

A crafter uses their own materials first. If they cannot afford the recipe alone, consenting online, living members of the same party can supply the missing ingredients regardless of distance. A donor must have a free inventory: another container, carried cursor stack, or pending transaction can make them unavailable. The server checks the current membership, consent, ingredients, and output space on every craft. Missing materials may come from several inventories. Least-recent donors are preferred; each donor receives an item-and-recipient notification. Preview counts reserve nothing.

Stay near the party for combat rewards: qualifying Cosmic Spawner mob rewards are shared with eligible active members within 60 blocks in the same instance. A dead member's death position is used before respawn; disconnected players receive no share. After 15 minutes without observed activity, a member is marked AFK and the leader receives a kick prompt. The leader may use `/dungeoneer kick <player>`.

Mercenaries follow their hirer and use their own class-specific combat/skill rules. They have separate run-based progression, do not drop their equipment on death, and may catch up when stuck or separated. Player class controls do not command every mercenary skill. Check party hover for the actual skill level and availability. New hires draw from 80 distinct historical names: English, French, German-speaking, Italian, Asia, Africa, Russia and Mediterranean, with five male and five female names per pool. Names stay at most nine letters; existing hires keep their displayed names, and saved names stay stable through respawn and reload.

## Gear, ownership, and trading

Class-attuned gear and class chests require the matching class. The first eligible acquisition binds attuned gear to its owner; picking up another player's discarded gear does not transfer ownership. You may manually drop gear, including class ammunition, and gear drops on death. Only its owner can collect those player drops. Ground drops remain vulnerable to normal environmental loss; dropping an item is not a way to give it to a friend.

Use `/trade <player>` or the **CAPS LOCK** trade-request default while looking at an eligible nearby player. Requests expire after 30 seconds. Accept with `/trade accept <player>`, decline with `/trade deny <player>`, or end the exchange with `/trade cancel`.

All potions may be traded, including types your class cannot craft. Other inventory items may be traded unless they are explicitly class-attuned non-potions; ordinary price, quest, or ability labels are not blanket trade bans. Ownership and transaction checks still apply. Put items and whole currency denominations into your offer. Offered currency is immediately reserved from the available balance. Both players must **Ready**, then **Confirm** the current offers. Any offer edit clears confirmation. There is no countdown to wait out. Cancel, closing, disconnect, or an invalid trade returns the held items and reserved currency safely. A successful authorized trade transfers item custody.

## Potion crafting, fuel, and stacks

Inside an active Dungeon 1 run, a **Theurgist** or **Venefex** opens inventory to the always-visible **Potions** list on its right. Theurgists craft beneficial vanilla-brewable splash potions; Venefex craft harmful ones. Both may craft mixed-effect **drinkable Turtle Master**. Its splash and lingering forms are not offered. Luck has no vanilla brewing recipe and is not a craft tile. Existing potions remain usable/tradable according to their item rules even when absent from this palette.

Each legal Normal, Strong, and Extended variant has its own labeled row. Scroll with the mouse wheel or drag the scrollbar. Recipes stay visible as disabled previews outside a dungeon. Hover a row for its potion effects and availability explanation:

| Tile | Meaning |
|---|---|
| Green background/count | Your own inventory can supply this many crafts. |
| Blue count, normal background | Eligible shared inventories can supply the recipe. |
| Red 0, light-red background | Unavailable; clicking spends nothing. |

The count is an alternative capacity for that recipe, not a reservation or a promise that all displayed tiles can be crafted together.

**Click** crafts exactly one potion onto your cursor. Repeated clicks can stack identical newly crafted potions, with all components matching, up to **64**. A different potion or full cursor spends nothing. **Shift-click** crafts exactly one directly into inventory, preferring the hotbar. Potion crafting requires the open inventory and has no hotkey. Throwing or drinking consumes one potion at a time; drinkable potions return their bottle. Ordinary split, drag, drop, trade, and inventory movement apply within the stack's actual component rules. Existing authored potions are not automatically rewritten into crafted stacks.

Hover a potion tile to see the server-selected route for one output: the one existing potion input consumed, exact remaining ingredient quantities, and own/eligible-group stock. Intermediate stages made within that route are not charged again. The required blaze powder total includes both any reagent and new fuel refill; the fuel line identifies the refill portion, while saved charges reduce it. Own affordable routes are preferred before pooled routes. Unavailable tiles quote no affordable route. Quantities reserve nothing; a pending click shows the previous snapshot until the server replies, and execution rechecks all costs.

Recipes use the complete applicable vanilla chain: a water bottle or supported intermediate potion, all remaining ingredients, and gunpowder for a splash output. Not every route needs nether wart. Intermediate potions skip already completed steps. The fuel gauge at the top of the list shows your remaining **brewing charges**. One blaze powder supplies **20 steps**; each remaining brewing operation consumes one charge. Strength may need blaze powder both as an ingredient and as fuel when the gauge is empty. Unused charges survive normal saves, reconnects, and death. Invalid/full destinations or changed eligibility spend neither ingredients nor fuel.

Requests, random-potion skills, and the virtual Brewing Supplies resource are retired. They are not required to craft or resurrect, and obsolete balances do not become physical item refunds. Theurgist and Venefex do not use the old separate Skills window.

## Theurgist

A healer-alchemist who balances care and chemistry. Use mace/bow and Theurgist-attuned gear; beneficial potion work and successful support contribute to the appropriate passive skills. Mending Sting and Verdant Jolt provide regenerative aid. Scintilla Vitalis and Lux Vitalis heal living targets and harm undead. Read actual item effects and server values rather than assuming every named arrow is an ordinary damage arrow.

Your positive thrown potions aid allies and their companions and do not affect hostile mobs, including healing damage against undead. Restorative class arrows have their own rules.

The **Potions** list appears automatically beside inventory. Only Theurgists may interact with brewing stands; valid brews finish rapidly under the server's instant-brewing setting while the stand is open. The stand still needs a valid recipe and fuel. Eon Penrose sells class-gated brewing equipment and advanced healing stock.

**Resurrect...** is available immediately in your inventory; its default shortcut is **Alt+4**. Opening the picker does not cast. Choose a dead teammate in the same active run. They have 60 seconds to accept or decline on the death screen. A successful accepted resurrection returns them to their latest recorded death location with five seconds of protection and the existing recoverable-inventory handling. It does not recreate destroyed or missing physical drops.

Player resurrection costs no items or currency and starts a **180-second cooldown only after success**. Declined, expired, stale, or failed offers start no cooldown. The cooldown survives death, reconnect, and run cleanup; server downtime pauses this game-time cooldown. Party hover and the death screen show each caster's readiness, cooldown, or reason for unavailability. Mercenary Theurgists retain their separate Positive Potions level-10 resurrection unlock and their displayed cooldown rules.

## Venefex

A sword-and-bow toxicologist who weakens enemies and exploits their ailments. The **Potions** list beside inventory shows harmful splash recipes and drinkable Turtle Master. The same shared ingredients, fuel, cursor/Shift-click, and stack rules apply; brewing-stand interaction remains Theurgist-only.

The D1 arsenal includes the Tree Viper, Bushmaster, and Fer-de-Lance poison family; Pestis and Black Bubo weakness; Vapours, Melancholia, and Deathly Stupor slowing; and Spicule Breach/Rend instant harm. Spicule damage against living enemies grows with harmful effects under the server's balance settings. Spicule instead heals undead, and native poison immunities still matter. Sword/bow progression and the appropriate arrow-duration benefits are passive, not new hotkey abilities.

## Bogatyr

A northern survivalist who fights with sword, bow, shield, and a loyal wolfpack. Acquire companions through existing wolf eggs, ordinary bone taming, and breeding. Player packs are uncapped, survive ordinary saves/reconnects within the run, and end on completion, forfeit, or reset. Old archives cannot summon a previous run's pack. Hired Bogatyrs admit at most 20 dogs, starting at one summon every 30 active seconds. Their existing skill progression improves cadence down to 20 seconds. Saved packs already above 20 are retained, but receive no new dogs until below the limit.

Open inventory **Skills** for three actions:

| Action | Default | Behavior |
|---|---|---|
| Patrol | Alt+1 | Free default mode. Wolves follow, defend themselves and their master, and assist against hostile monsters their master successfully attacks. |
| Stay | Alt+2 | Free mode. Wolves return within five blocks, sit, and hold their individual settled positions even when you move. They ignore threats. |
| Feed Pack | Alt+3 | Uses up to ten actual food items from your own inventory, healing before breeding. |

Patrol and Stay are mutually exclusive. Selecting Stay again does not move settled wolves. Selecting Patrol stands them up without resuming old fights.

Feed Pack prefers bones/rotten flesh, then raw meat, then remaining meat by player food value. At default settings raw food/bones heal two hearts and cooked meat heals four. Injured wolves receive care first; remaining food goes to healthy eligible adult pairs within eight blocks. No mate, breeding cooldown, insufficient pair food, or an individually seated Patrol adult creates no breeding charge. Healthy pups do not consume breeding food. Bones act as food only through Feed Pack; normal bones and taming are unchanged.

In Stay, eligible pairs move temporarily for native mating, then return to their original held spots after birth or a failed attempt (30 seconds by default). Changing modes cancels the attempt. Reloads end unfinished attempts without invented offspring or refunds. Allies cannot harm your wolves, apply hostile effects, or sit them; they can feed and breed them. Kibble, paid Summon/Breed/Regroup/Heal, and the former combat modes are retired.

## Dragoon

A trident specialist and repair supporter. Successful melee or thrown **trident** hits can trigger short-range chain lightning against nearby enemies; it is passive and does not chain recursively. Trident progression improves the relevant damage/chain benefits. Sword hits are not a replacement trigger.

Dragoons may use anvils. For **Repair Affinity**, invite a customer with `/repair <player>`; accept with `/repair accept <player>`, decline with `/repair deny <player>`, or cancel with `/repair cancel`. The customer supplies damaged supported gear and may offer an optional account-currency labor fee. The Dragoon supplies compatible repair components, including approved ordinary unmarked materials where accepted. The customer retains ownership. Both participants must remain nearby with the interface open and review the quote/readiness controls. Items, materials, and fees move only after successful validation; not every special item is supported. See [Repair Affinity](Classes/Dragoon_Repair_Affinity.md).

## Pyroclast

A sword, bow, and crossbow fighter with explosive utility. **Right-click flint** while carrying gravel to make gunpowder. Default cost is one flint plus one gravel for one gunpowder; keep output space free. The old shapeless inventory-grid instructions no longer apply.

The authored vanilla Cinderkiss, Cinderbite and Cindermaul rockets load from your inventory or hands into bows and crossbows with their existing names, components and effects. Hold a bow or crossbow and press **R** to cycle legal ammunition in a stable order; exact tipped-arrow and other component variants remain separate. The icon/name and message show the selected type and your current binding. When that type runs out, selection automatically switches to available legal stock and announces it; no stock shows an out-of-ammo notice. A charged crossbow keeps its loaded ammunition: cycling chooses the next load. Native consumption, enchantments and class/ownership/run restrictions still apply.

Authored D1 Cinderbite and Cindermaul retain their supported abilities. Their enhanced explosions affect eligible enemies, respect obstruction, and do not damage terrain. Actual damage follows the server configuration. Sword, bow, and crossbow skills progress passively. Renaming an ordinary item does not grant a class ability.

## Judicator

An oathbound mace-and-bow protector. Scintilla Vitalis and Lux Vitalis provide healing to living targets and harm undead; Ebonsight provides night vision, and Vielpiercer reveals targets with Glowing. Follow each authored item's tooltip and class restrictions. Mace/bow progression is passive. Eon sells the shared Theurgist/Judicator healing offers. Repair Affinity and anvil access belong to Dragoon.

## Controls and passive progression

**Cycle ammunition** defaults to **R** and can be rebound or unbound in Controls. It works once per physical press while holding a bow or crossbow in the world; screens, chat, modals, lost focus and conflicting bindings block it. Ammo feedback uses your actual current binding. The following inventory shortcuts are class skills.

Change bindings under **Options > Controls > Key Binds > Cosmic Dungeon**. Gray hints beside active skills show your saved binding, including an unbound state. Active shortcuts work once per press in the world or your inventory when the action is eligible. Chat, other menus, dragging, held cursor items, and modal controls block them. Resolve duplicate chords with other actions in Controls. Different modifier chords are distinct even if Controls highlights the same base key.

The movable Bogatyr Skills panel retains its position; use the Cosmic Dungeon settings reset control if needed. Theurgist/Venefex use their inventory actions. Passive weapon/potion progression has no activation key: eligible combat and useful support earn the relevant class skill, with server-configured limits and bonuses. A class's passive skill is not an unlock for an inactive class or later dungeon.

## Dungeon objectives, doors, and progression

Follow the dungeon's current messages, route clues, keys, and assigned objectives. Keep keys available for their matching locks; a renamed ordinary item is not a key. Class flags remain usable where the dungeon expects them.

For Dungeon 1, collect **all six spectral Blooms**, plant the group's banners, and gather the group near **John Hamish Watson**. Watson checks the group's inventories and gives the group one shared completion outcome. Spectral Blooms are the main objective; **Lesser Blooms** are separate restoration/progression collectibles. Cumulative Lesser Bloom tiers are 5, 10, 15, and 20. Village access unlocks through Dungeon 1 completion.

Spectral Blooms, flags, and run objectives restart in each dungeon instance. Lifetime contributions, completion totals, personal vendor unlocks, and the Trace account persist. Use `/d1 stats` for lifetime totals and `/d1 claim` for stored belongings when you have inventory space. Forfeit ends the run and its player wolfpack; review the death-screen/exit warning before choosing it.

## Trace, factions, and shops

Trace is stabilized attunement residue kept in one persistent account shared by your classes, runs, shops, and trades. **Trace = 1; Mark = 10; Seal = 100; Crown = 1,000; Anchor = 10,000.** Onboarding grants five Trace. `/currency balance` shows your balance; `/currency value` and `/currency value inventory` inspect held/inventory value. Old physical currency is not converted automatically. Death-currency recovery waits if the entire amount cannot fit in the account.

NPC standing uses Hostile, Cordial, Warmly, and Ally; other factions may use other tiers. Standing affects shop prices and access. Use `/faction list <your_name>` and `/faction get <your_name> <faction_id>` for visible factions. Follow the reason shown by a locked vendor. Stock depends on class, dungeon progression, and personal stock; personal stock refreshes each Overworld morning. Shop balances update while open. Use **Buy**, **Sell Selected**, or **Sell All** for eligible rows after reviewing prices and capacity.

| NPC | Current role |
|---|---|
| Naton Whitlock | General supplies: food, fire, light, and containers. |
| Elias Centvin | Weapons, tools, repair components, and direct repairs. Nearby `/repair shop 1` through `4` request 25%, 50%, 75%, or 100% repair. |
| Eon Penrose | Brewing ingredients, bottles, equipment, and potions. Some healing/equipment rows require Theurgist or Judicator. |
| Beatrix Farrow | Food and Raw Farrow's Chop. |
| Beluzon | One-time First Heart bond and approved Inn-bed home. |
| Tamsin Vane | Introduction, party/class flow, JHW map, and the later eligible Tamsin Tax offer. D1 entry is free. |
| John Hamish Watson | Six spectral Blooms, the abandoned Base Camp, and Dungeon 1 progression. |

The H guide keeps the full NPC backstories. Tamsin's optional later payment flow presents an eligible recovered item for review and confirmation; it is not a Trace entry fee.

## Travel

Drink **Potion of Companionship** inside your active dungeon when eligible. Drinking consumes it and immediately starts Teleport Cooldown (five minutes by default). Choose an eligible online companion in the same run for safe arrival nearby. A blocked drink keeps the potion; after drinking, a blocked destination permits another choice during the selection window.

Hold **Raw Farrow's Chop** at a lit campfire. Cooking inside your active dungeon (four seconds by default) remembers your location and takes you to Main Village. Eat the owner-bound cooked chop to return. You may own one chop at a time, and the return campfire must still exist.

After Village access, Beluzon's one-time bond lets you claim an approved Inn bed and use `/home`. Changing approved beds does not require a second bond. The Inn bed is a fallback when no stronger valid respawn point exists. Use rifts only where the world presents them.

## Achievements and references

Read the in-game advancement descriptions for objectives and locked requirements. D1 milestones include First Trace, Handshake Protocol, Nostalgia Bait, Plant Flags, Synchronous Peal, Sixfold Vigil, Cycle of Recorded Sound, Fire Escape, Librarian I, Shulker Express, Stairway To Heaven, Tired, Not Broken, Wolves in Piglin Clothing, Boom!, and The Tamsin Tax. Artwork and a visible title alone do not grant an achievement.

Useful commands: `/progression get <your_name>`, `/d1 stats`, `/d1 claim`, `/currency balance`, `/trade <player>`, `/repair <player>`, `/home`, and `/d1 wolves` for a Bogatyr's living loaded run-pack count. Commands shown in the guide are reference text; clicking a guide link does not execute a command.

## Developer-only reference â€” outside the H guide

The spawner editor is an authoring tool, not a Dungeoneer feature. The server's actual **Developer rank** is required; operator level alone does not supply that rank. A Developer looks at a Cosmic Spawner within five blocks and uses `/spawner edit`. Detailed spawner hover information is intrinsic to that rank; there is no player showlabels opt-in.

Ordinary right-clicks at a Cosmic Spawner allow bow drawing and crossbow loading/firing. Developers can deliberately **Shift + right-click** with held equipment to copy its exact components into the preset; ordinary combat never copies equipment. A copied bow enables a skeleton's native ranged attack. Only uninterrupted actual wrong-tool mining warns after one second; holding/using a bow or crossbow does not spam the mining title.

The editor keeps changes in a draft until Apply. Review the target and unsaved-change prompt; Cancel abandons the draft. Entity/name/captain selection, six equipment slots, searchable item selection, copying real held-item components, enchantments, flags, drop chances, intrinsic/custom loot, boss/cap/delay controls, and named preset operations belong here. Normal enchantments retain compatibility and normal limits; Advanced/Unsafe permits deliberate bounded exceptions. The preview is visual only and does not spawn mobs. Existing five preset shortcuts remain available. Do not confuse a conditional native loot table with guaranteed custom loot.

Use [Spawner Systems](Spawners/Spawner_Systems.md) and [Spawner Commands and Features](Spawner_Commands_Features.md) for authoritative authoring controls and permission boundaries. Dungeoneers see only ordinary gameplay feedback, including a wrong-tool warning after sustained breaking of the same spawner.

## Documentation maintenance

Mechanics above were checked against the shipped class, crafting, sharing, trade, ownership, resurrection, wolf, travel, and spawner services for Batch 19. The H content source is `src/main/java/net/goui/cosmicdungeon/client/screen/HelpMenuContent.java`; class summaries link from [Help Menu Classes](Classes/Help_Menu_Classes.md). Update these together when player behavior changes.

Class atmosphere retains the local canon mirror reviewed for Batch 18 (six active class sources and full NPC lore). Later-dungeon equipment in that mirror remains deferred; artwork is not evidence of availability. No online canon refresh or gameplay balance change is implied by this guide.

Chop trades transfer the personal ownership record with the item. Each recipient must still satisfy the one-Chop limit; offer an existing Chop in the same exchange if necessary. A bound return Chop requires the recipient's own stored inventory in that active run. Trades never transfer another player's stored inventory, grant run membership, or bypass the return destination checks. If these conditions fail, the trade cancels and returns its original offers.
