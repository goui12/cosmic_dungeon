package net.goui.cosmicdungeon.client.screen;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class HelpMenuContent {
    public static final Page GET_STARTED = page("get_started", "Get Started", true, List.of(
            HelpBlock.heading("Welcome, Dungeoneer"),
            HelpBlock.paragraph("Meet Tamsin Vane, choose an enabled class, and prepare with your party at the class selector. Dungeon 1 entry is free."),
            HelpBlock.paragraph(Component.literal("Start with ").append(link("Classes", "classes")).append(", then use the Party guide for readiness and resource sharing.")),
            HelpBlock.bullet("H or Esc closes this guide without pausing the world. Alt+Left or Back returns to the previous guide page."),
            HelpBlock.bullet("Use the left directory or underlined text links. Wheel scrolling affects the pane under your pointer; Page Up and Page Down scroll the focused pane (information pane by default). Tab / Shift+Tab moves focus; Enter opens the focused link."),
            HelpBlock.bullet("Hover example item tiles for their real tooltips. These are read-only illustrations: they cannot grant, craft, equip, or move items."),
            HelpBlock.bullet("Your inventory keeps armor, offhand, storage and hotbar controls. The old 2x2 crafting grid and recipe book are absent."),
            HelpBlock.paragraph(Component.literal("The class name in your inventory opens your class guide. See ").append(link("Quick Reference", "references")).append(" for controls and commands.")),
            HelpBlock.tip("Artwork illustrates the lore. It does not unlock a class, recipe, location, or encounter.")));

    public static final Page DUNGEONS = page("dungeons", "Dungeons", true, List.of(
            HelpBlock.heading("Dungeon Runs"),
            HelpBlock.bullet("Follow Tamsin's class and party flow. The leader starts readiness; each player confirms Ready before Start Adventure!"),
            HelpBlock.bullet("Watch route clues, doors, keys and objective messages. Keep the correct key available for its matching lock; renaming an ordinary item does not make it a key."),
            HelpBlock.examples("key"),
            HelpBlock.tip("The key above is an unbound example, not a key to any door."),
            HelpBlock.paragraph(Component.literal("Collect the six spectral Blooms, plant the group's banners and gather near Watson. Read ").append(link("Progression", "progression")).append(" for the completion and Lesser Bloom distinction.")),
            HelpBlock.bullet("Qualifying Cosmic Spawner enemies can award Group Split Trace. Stay together for rewards and survival."),
            HelpBlock.bullet("Death drops your gear for owner-only recovery. Resurrection uses the latest death and existing recoverable drops; missing items are not recreated."),
            HelpBlock.paragraph(Component.literal("A Theurgist can offer ").append(link("Resurrection", "class.theurgist")).append(". Review the warning before using Forfeit: it ends the run and its player wolfpack.")),
            HelpBlock.paragraph(Component.literal("Use ").append(link("Travel", "teleportation")).append(" for Companionship, Farrow's Chop and Inn homes."))));

    public static final Page CLASSES = page("classes", "Classes", true, List.of(
            HelpBlock.heading("Choose Your Class"),
            HelpBlock.paragraph("Your class controls access to class chests and class-attuned weapons, tools, armor and special gear."),
            HelpBlock.bullet("Enabled: Theurgist, Pyroclast, Bogatyr, Dragoon, Venefex and Judicator. Metalmancer and Deadeye remain unavailable."),
            HelpBlock.bullet("The first eligible acquisition binds attuned gear to its owner. Dungeon, tier and Trace labels do not change its class."),
            HelpBlock.bullet("Gear, including class ammunition, can be manually dropped and drops on death. Only its owner can recover player drops; dropping does not transfer ownership."),
            HelpBlock.paragraph(Component.literal("Use authorized ").append(link("Trading", "trading")).append(" to exchange eligible items. All potions trade; explicitly class-attuned non-potions do not.")),
            HelpBlock.bullet("Class flags remain placeable where the dungeon expects them. Authored items keep their actual components and restrictions."),
            HelpBlock.heading("Actions and Passive Skills"),
            HelpBlock.bullet("Bogatyr uses inventory Skills for Patrol, Stay and Feed Pack. Theurgist uses the inventory Potions list and Resurrect...; Venefex uses the same Potions list."),
            HelpBlock.bullet("Theurgist and Venefex do not use the old separate Skills window. Requests, Brewing Supplies and Kibble are retired."),
            HelpBlock.bullet("Eligible combat and useful support earn passive class skills. Progression is not an active button or a new class unlock."),
            HelpBlock.paragraph(Component.literal("Read the class pages in the left directory and ").append(link("Quick Reference", "references")).append(" for saved bindings."))));

    public static final Page PARTY = page("party", "Party", true, List.of(
            HelpBlock.heading("Ready and Start"),
            HelpBlock.bullet("The leader begins readiness; each player controls Ready / Not Ready. An available Ready action is green with a subtle pulse; Not Ready withdraws your confirmed readiness."),
            HelpBlock.bullet("Membership changes clear readiness. The leader chooses Start Adventure! when the group is ready. Mercenaries have no Ready toggle; ready controls disappear after entry."),
            HelpBlock.bullet("Starting an adventure safely cancels the party's open trades. Recruitment and mercenary pages keep forward progression available."),
            HelpBlock.heading("Share Ingredients"),
            HelpBlock.bullet("The creator and joiners explicitly choose YES or NO before starting. Choices lock when Start Adventure is accepted and remain fixed for the adventure. Change them after returning to pre-game."),
            HelpBlock.bullet("YES lets eligible recipe ingredients help party crafting. The crafter uses their own stock first, then missing materials from eligible consenting online, living teammates."),
            HelpBlock.bullet("Distance does not block ingredient sharing. Another container, held cursor stack, death, or pending transaction can make an inventory unavailable."),
            HelpBlock.bullet("Several teammates may supply different ingredients. Least-recent donors are preferred and receive item-and-recipient notifications. Consent, membership and materials are checked again at the click."),
            HelpBlock.paragraph(Component.literal("See the ").append(link("Theurgist guide", "class.theurgist")).append(" for recipe counts, fuel and shared potion crafting.")),
            HelpBlock.heading("Party Hover and Mercenaries"),
            HelpBlock.bullet("Hover a party member for class, health, skills, sharing YES/NO, shared resources used/contributed and relevant resurrection cooldowns."),
            HelpBlock.bullet("Mercenaries follow their hirer, use class-specific skills and separate progression, and drop no equipment. New hires draw from 80 short historical names across eight balanced regional pools; existing hires keep their names through saves and respawns."),
            HelpBlock.heading("Group Split and AFK"),
            HelpBlock.bullet("Qualifying mob Trace is shared with eligible active members within 60 blocks in the same instance. Before respawn, a dead member's death position is used; disconnected players receive no share."),
            HelpBlock.bullet("After 15 minutes without observed activity, a member is marked AFK and the leader receives a kick prompt."),
            HelpBlock.command("Group Leader", "/dungeoneer kick <player>")));

    public static final Page TRADING = page("trading", "Trading", true, List.of(
            HelpBlock.heading("Player Trading"),
            HelpBlock.command("Request", "/trade <player>"),
            HelpBlock.command("Accept", "/trade accept <player>"),
            HelpBlock.command("Deny", "/trade deny <player>"),
            HelpBlock.command("Cancel", "/trade cancel"),
            HelpBlock.bullet("CAPS LOCK is the default Trade Request key. Look at an eligible nearby player to use the prompt. Invites expire after 30 seconds."),
            HelpBlock.bullet("All potions are tradable, even when your class cannot craft them. Other inventory items may trade except explicitly class-attuned non-potions."),
            HelpBlock.bullet("Ordinary price, quest and ability labels are not blanket bans. Ownership and transaction checks still apply."),
            HelpBlock.bullet("Offer items and whole currency denominations. Offered currency immediately reduces the available displayed balance."),
            HelpBlock.bullet("Both players Ready, then Confirm the final offers. Offer changes clear confirmations; there is no countdown."),
            HelpBlock.bullet("Cancel, closing, disconnect or a failed trade returns items and reserved currency. Starting an adventure cancels party trades. Successful trades transfer custody."),
            HelpBlock.tip("Dropping gear on the ground is not a transfer: player drops remain owner-only."),
            HelpBlock.paragraph(Component.literal("Read ").append(link("Currency", "currency")).append(" for the Trace account and denominations."))));

    public static final Page VENDORS = page("vendors", "Vendors", true, List.of(
            HelpBlock.heading("Vendor Shops"),
            HelpBlock.bullet("Right-click assigned vendor NPCs to open their shop."),
            HelpBlock.bullet("Locked vendors show the reason instead of opening."),
            HelpBlock.bullet("The Vendor Selling pane shows item, name, cost, and Buy controls."),
            HelpBlock.bullet("The Player Inventory pane shows inventory items that vendor can buy."),
            HelpBlock.bullet("Use Sell Selected or Sell All to sell eligible items."),
            HelpBlock.bullet("Your displayed account balance refreshes while the shop is open."),
            HelpBlock.bullet("Retail prices reflect your NPC faction; personal stock refreshes each Overworld morning."),
            HelpBlock.bullet("Naton Whitlock offers general supplies: food, fire, light, and container basics."),
            HelpBlock.bullet("Elias Centvin sells weapons, tools, and repair components. Use /repair shop 1 through 4 nearby for direct repair."),
            HelpBlock.bullet("Eon Penrose runs the Brewing Store for ingredients, bottles, equipment, and potions."),
            HelpBlock.bullet("Beatrix sells food and Raw Farrow's Chop. Any lit campfire can prepare the trip to the Village."),
            HelpBlock.bullet("See Brewing Store, Travel, and Village Souls for the short field guide."),
            HelpBlock.tip("Check the server's final price and available inventory space before confirming a transaction.")));


    public static final Page BREWING_STORE = page("brewing_store", "Brewing Store", true, List.of(
            HelpBlock.heading("Eon Penrose"),
            HelpBlock.paragraph("Eon Penrose sells brewing ingredients, equipment, and potions in the restored Village."),
            HelpBlock.heading("Live Dungeon 1 Stock"),
            HelpBlock.bullet("General: brewing ingredients, Glass Bottles, Night Vision, and Fire Resistance."),
            HelpBlock.bullet("Theurgist only: Brewing Stand, Cauldron, Healing II, Regeneration, and Lingering Healing."),
            HelpBlock.bullet("Theurgist or Judicator: Healing and Splash Healing."),
            HelpBlock.bullet("Vendor stock depends on your dungeon progress."),
            HelpBlock.tip("If a row is locked, check your class and Village progression before buying.")));

    public static final Page VILLAGE_SOULS = page("village_souls", "Village NPCs", true, List.of(
            HelpBlock.heading("Village NPC Lore"),
            HelpBlock.paragraph("These pages collect the dungeoneer-facing backstories of the Village NPCs and story figures tied to Dungeon 1."),
            HelpBlock.bullet("Naton Whitlock: general supplies, food, fire, light, and container basics."),
            HelpBlock.bullet("Elias Centvin: weapons, tools, compatible Dragoon repair components, and direct shop repair."),
            HelpBlock.bullet("Eon Penrose: brewing ingredients, equipment, and potion work."),
            HelpBlock.bullet("Beatrix Farrow: food, campfire cooking, Farrow's Chop, and the memory of home."),
            HelpBlock.bullet("Beluzon: the First Heart bond, an Inn home, and fallback respawn."),
            HelpBlock.bullet("Tamsin Vane: a starting-area treasure hunter who found a JHW map to Base Camp."),
            HelpBlock.bullet("John Hamish Watson: doomed scholar tied to the abandoned Base Camp, six spectral blooms, and later guidance."),
            HelpBlock.tip("Use each NPC's page in the left index for the full backstory. Vendor prices stay out of these lore pages.")));

    public static final Page NATON_WHITLOCK = lorePage("npc.naton_whitlock", "Naton Whitlock", "General Supply Vendor",
            "Naton Whitlock provides basic travel supplies, food, fire access, light access, and container access for players in the Village.", natonBackstory(), natonNotes());

    public static final Page ELIAS_CENTVIN = lorePage("npc.elias_centvin", "Elias Centvin", "Weapon, Tool Sales and Service",
            "Elias Centvin provides weapon and tool sales, armor repair material access, and repair service rules for damaged armor, weapons, and tools.", eliasBackstory(), eliasNotes());

    public static final Page EON_PENROSE = lorePage("npc.eon_penrose", "Eon Penrose", "Brewing Store Vendor",
            "Eon Penrose provides brewing ingredients, equipment, and potion sales, for players in the Village.", eonBackstory(), eonNotes());

    public static final Page BEATRIX_FARROW = lorePage("npc.beatrix_farrow", "Beatrix Farrow", "Food and Farrow's Chop Vendor",
            "Beatrix sells food that can be cooked on a campfire, along with her special Farrow’s Chop. Beatrix Farrow grounds the campfire food system and Farrow’s Chop travel mechanic in restored-soul progression, Village recovery, and the lingering cosmic damage caused by Atlach-Nacha’s centuries-long search for an anchor on Earth.", beatrixBackstory(), beatrixNotes());

    public static final Page BELUZON = page("npc.beluzon", "Beluzon", true, List.of(
            HelpBlock.heading("Village Inn"),
            HelpBlock.paragraph("Beluzon binds travelers to the First Heart, the Pale Oak remembered by the Village Inn."),
            HelpBlock.bullet("After unlocking Village access, speak to Beluzon for the one-time bond."),
            HelpBlock.bullet("Claim an approved Inn bed to set your home. Changing approved beds needs no second bond."),
            HelpBlock.command("Return home", "/home"),
            HelpBlock.tip("Your registered Inn bed remains a fallback when no stronger valid respawn point exists.")));

    public static final Page TAMSIN_VANE = lorePage("npc.tamsin_vane", "Tamsin Vane", "Dungeon Entry Broker",
            "Tamsin Vane found a map to Base Camp. She asks for a promised share of recovered treasure; Dungeon 1 entry is free.", tamsinBackstory(), tamsinNotes());

    public static final Page JOHN_HAMISH_WATSON = lorePage("npc.john_hamish_watson", "John Hamish Watson", "Quest Giver",
            "John Hamish Watson introduces the abandoned Base Camp, the six spectral blooms, and the first major dungeon recovery objective. The six spectral blooms are unique placed dungeon bloom items tied to Watson's spiritual release; Lesser Blooms are separate restoration collectibles for side progression, vendor access, achievements, and unlocks.", watsonBackstory(), watsonNotes());

    public static final Page FOOD_VENDOR = page("vendor.food", "Food Vendor", true, List.of(
            HelpBlock.heading("Beatrix Farrow"),
            HelpBlock.paragraph("Beatrix Farrow provides food and Raw Farrow's Chop in the restored Village."),
            HelpBlock.bullet("Her food and Farrow's Chop travel behavior are documented under Travel and her NPC lore page."),
            HelpBlock.tip("Choose your D1 class at the selector, then confirm Ready with your party.")));

    public static final Page WEAPONS_SUPPLIER = page("vendor.weapons", "Weapons Supplier", true, List.of(
            HelpBlock.heading("Elias Centvin"),
            HelpBlock.paragraph("Elias Centvin sells weapons, tools, and Dragoon repair materials."),
            HelpBlock.bullet("Use /repair shop 1, 2, 3, or 4 near Elias for 25, 50, 75, or 100 percent repair."),
            HelpBlock.tip("Locked rows and access requirements are enforced server-side.")));

    public static final Page GENERAL_SUPPLY_VENDOR = page("vendor.general_supply", "General Supply Vendor", true, List.of(
            HelpBlock.heading("Naton Whitlock"),
            HelpBlock.paragraph("Naton Whitlock offers basic general supplies: food, fire, light, and container basics."),
            HelpBlock.bullet("Purchases and sales use the same server-authoritative currency balance as other vendors."),
            HelpBlock.tip("If a vendor is locked, follow the reason shown in-game.")));

    public static final Page TELEPORT_VENDOR = page("vendor.teleport", "Dungeon Entry", true, List.of(
            HelpBlock.heading("Tamsin Vane"),
            HelpBlock.paragraph("Tamsin Vane found a map pointing toward Base Camp and the initials JHW. Use her introduction, party and class flow; the information icon reopens the map/lore."),
            HelpBlock.bullet("Dungeon 1 entry is free. Meet your party at the class selector."),
            HelpBlock.bullet("A later eligible Tamsin Tax offer lets you review and confirm one recovered item. It is not a Trace entry fee."),
            HelpBlock.tip("For live travel behavior, use Travel and the prompts shown by the dungeon.")));

    public static final Page CURRENCY = page("currency", "Currency", true, List.of(
            HelpBlock.heading("Attunement Fragments"),
            HelpBlock.paragraph("Trace is stabilized attunement residue recovered from dungeon-bound beings and valued by freed NPCs as protection against future binding."),
            HelpBlock.bullet("Your Attunement Fragment currency is stored in an account balance."),
            HelpBlock.bullet("Trace = 1, Mark = 10, Seal = 100, Crown = 1,000, Anchor = 10,000."),
            HelpBlock.bullet("Your onboarding currency reward grants 5 Trace."),
            HelpBlock.bullet("Trace is used for purchases, upgrades, trades, and dungeon rewards."),
            HelpBlock.bullet("Recoverable death currency returns to your account when collected."),
            HelpBlock.bullet("Death-currency pickup waits if the entire amount would exceed your account capacity."),
            HelpBlock.bullet("Vendors and trades use the same balance; open menus refresh as it changes."),
            HelpBlock.bullet("Old physical currency items do not become account money automatically. Keep them for a developer to review."),
            HelpBlock.command("Balance", "/currency balance"),
            HelpBlock.command("Held Value", "/currency value"),
            HelpBlock.command("Inventory Value", "/currency value inventory")));

    public static final Page PROGRESSION = page("progression", "Progression", true, List.of(
            HelpBlock.heading("Dungeon 1 Progress"),
            HelpBlock.bullet("Collect all six spectral Blooms and gather the whole group near John Watson."),
            HelpBlock.bullet("Lesser Blooms support Village NPC progression and faction standing."),
            HelpBlock.bullet("Plant the group's banners and speak to Watson while everyone is gathered."),
            HelpBlock.bullet("Village access unlocks from Dungeon 1 completion."),
            HelpBlock.bullet("Watson checks the group's inventories for all six spectral Blooms; everyone receives the same outcome."),
            HelpBlock.bullet("Dungeon 1 NPC unlock tiers derive from cumulative Lesser Blooms: tier 1 at 5, tier 2 at 10, tier 3 at 15, and tier 4 at 20."),
            HelpBlock.bullet("Spectral Blooms, flags, and run objectives start fresh in each dungeon instance."),
            HelpBlock.bullet("Lifetime Bloom contributions, completion totals, personal vendor unlocks, and your Trace account persist."),
            HelpBlock.command("Lifetime totals", "/d1 stats"),
            HelpBlock.command("Stored belongings", "/d1 claim"),
            HelpBlock.command("Read Progress", "/progression get <your_name>"),
            HelpBlock.tip("Use /d1 claim to collect belongings held for you when inventory space is available.")));

    public static final Page FACTIONS = page("factions", "Factions", true, List.of(
            HelpBlock.heading("Faction Standing"),
            HelpBlock.bullet("Faction tracks long-term alignment and conduct with faction groups."),
            HelpBlock.bullet("NPC vendor standing uses Hostile, Cordial, Warmly, and Ally; other factions may use different tiers."),
            HelpBlock.bullet("Faction standing can be checked in-game when a faction is visible to you."),
            HelpBlock.bullet("Positive and negative actions may change faction over time where the dungeon has configured them."),
            HelpBlock.bullet("Hostile status can restrict access, but recovery paths may exist by faction."),
            HelpBlock.command("List", "/faction list <your_name>"),
            HelpBlock.command("Get", "/faction get <your_name> <faction_id>"),
            HelpBlock.tip("Exact value ranges can vary by faction; rely on the tier shown to you in-game.")));

    public static final Page TELEPORTATION = page("teleportation", "Travel", true, List.of(
            HelpBlock.heading("Potion of Companionship"),
            HelpBlock.bullet("Drink inside your active dungeon with no travel cooldown and no unfinished item handoff."),
            HelpBlock.bullet("Opens Teleport to a Dungeoneer with eligible online companions inside the same active run."),
            HelpBlock.bullet("Choose a companion to travel to a safe position near them."),
            HelpBlock.bullet("Drinking consumes the potion and starts Teleport Cooldown immediately; its default duration is five minutes."),
            HelpBlock.bullet("If drinking is blocked, you keep the potion. After drinking, a blocked destination lets you retry within the selection window."),
            HelpBlock.heading("Beluzon's Inn"),
            HelpBlock.bullet("Beluzon offers a permanent Inn bond, paid once. Claim an approved Inn bed to use /home."),
            HelpBlock.bullet("Your Inn bed is a fallback when no stronger valid respawn point exists."),
            HelpBlock.heading("Rifts"),
            HelpBlock.bullet("Rifts are world-travel structures when presented by the dungeon."),
            HelpBlock.heading("Farrow's Chop"),
            HelpBlock.bullet("Hold Raw Farrow's Chop at a lit campfire; the default cooking time is four seconds."),
            HelpBlock.bullet("Cook Raw Farrow's Chop inside your active dungeon to remember your exact location and travel to Main Village; eat the bound chop to return."),
            HelpBlock.bullet("You may own one Chop at a time. Its return campfire must still exist."),
            HelpBlock.bullet("A Chop trade must respect the one-Chop limit. To trade a bound return Chop, the recipient needs their own stored inventory in that same active dungeon; a trade never transfers another player's inventory."),
            HelpBlock.examples("food"),
            HelpBlock.tip("Only use teleport options offered by the dungeon, a potion, visible in-world travel structures, or Farrow's Chop.")));

    public static final Page ACHIEVEMENTS = page("achievements", "Achievements", true, List.of(
            HelpBlock.heading("Dungeon 1 Milestones"),
            HelpBlock.bullet("First Trace"),
            HelpBlock.bullet("Handshake Protocol"),
            HelpBlock.bullet("Nostalgia Bait"),
            HelpBlock.bullet("Plant Flags"),
            HelpBlock.bullet("Synchronous Peal"),
            HelpBlock.bullet("Sixfold Vigil"),
            HelpBlock.bullet("Cycle of Recorded Sound"),
            HelpBlock.bullet("Fire Escape"),
            HelpBlock.bullet("Librarian I"),
            HelpBlock.bullet("Shulker Express"),
            HelpBlock.bullet("Stairway To Heaven"),
            HelpBlock.bullet("Tired, Not Broken"),
            HelpBlock.bullet("Wolves in Piglin Clothing"),
            HelpBlock.bullet("Boom!"),
            HelpBlock.bullet("The Tamsin Tax"),
            HelpBlock.tip("Read the advancement description for its objective and locked requirements. A visible title or illustration is not an unlock.")));

    public static final Page REFERENCES = page("references", "References", true, List.of(
            HelpBlock.heading("Controls"),
            HelpBlock.paragraph(Component.literal("Hover for your saved bindings: ")
                    .append(key("Patrol", "bogatyr_patrol")).append(", ")
                    .append(key("Stay", "bogatyr_stay")).append(", ")
                    .append(key("Feed Pack", "bogatyr_feed_pack")).append(", ")
                    .append(key("Resurrect...", "theurgist_resurrect")).append(".")),
            HelpBlock.command("Guide", "H / Esc - close; H - open"),
            HelpBlock.command("Back", "Alt+Left or Back - previous guide page"),
            HelpBlock.command("Scroll", "Wheel - pane under cursor; Page Up / Page Down - focused pane (information by default)"),
            HelpBlock.command("Trade", "CAPS LOCK - default Trade Request"),
            HelpBlock.bullet("Cycle ammunition defaults to R. Hold a bow or crossbow in the world; each press selects the next legal exact variant. Rebind or unbind in Controls. Screens, typing, lost focus and conflicting bindings block this action."),
            HelpBlock.bullet("Ammo feedback shows the exact icon/name and current key. Exhaustion automatically switches and announces the next available type; no stock reports out of ammunition. Charged crossbows keep their current load."),
            HelpBlock.bullet("Active defaults: Bogatyr Alt+1 Patrol, Alt+2 Stay, Alt+3 Feed Pack; Theurgist Alt+4 opens Resurrect...."),
            HelpBlock.bullet("Rebind or unbind in Options > Controls > Key Binds > Cosmic Dungeon. Gray skill hints show your actual saved chord."),
            HelpBlock.bullet("Shortcuts act once per press in the world or inventory. Chat, other menus, dragging, carried items and modal controls block them. Resolve conflicting chords in Controls."),
            HelpBlock.bullet("Potion crafting requires the inventory and has no hotkey. Passive skills are not action buttons. Reset the movable Skills panel in Cosmic Dungeon settings if needed."),
            HelpBlock.heading("Player Commands"),
            HelpBlock.command("Trade", "/trade <player>; /trade accept <player>"),
            HelpBlock.command("End trade", "/trade deny <player>; /trade cancel"),
            HelpBlock.command("Repair", "/repair <player>; /repair accept <player>"),
            HelpBlock.command("End repair", "/repair deny <player>; /repair cancel"),
            HelpBlock.command("Balance", "/currency balance"),
            HelpBlock.command("Value", "/currency value; /currency value inventory"),
            HelpBlock.command("Run history / claims", "/d1 stats; /d1 claim"),
            HelpBlock.command("Wolf count", "/d1 wolves"),
            HelpBlock.command("Home", "/home"),
            HelpBlock.command("Progress", "/progression get <your_name>"),
            HelpBlock.command("Factions", "/faction list <your_name>"),
            HelpBlock.command("Faction", "/faction get <your_name> <faction_id>"),
            HelpBlock.command("Group Leader", "/dungeoneer kick <player>"),
            HelpBlock.tip("These are reference commands, not executable links. Developer, operator, debug and world-authoring commands do not belong in the player guide.")));

    public static final Page THEURGIST = page("class.theurgist", "Theurgist", true, List.of(
            HelpBlock.heading("Healer and Alchemist"),
            HelpBlock.paragraph("Once a battlefield medic, the Theurgist combines chemistry and measured restoration to sustain the living and recall the fallen."),
            HelpBlock.bullet("Use Theurgist-attuned gear and class chests. Mace, bow and potion progression is passive; useful healing and support can earn the appropriate skill."),
            HelpBlock.bullet("Mending Sting and Verdant Jolt give regenerative aid. Scintilla Vitalis and Lux Vitalis heal living targets and harm undead."),
            HelpBlock.heading("Potions"),
            HelpBlock.paragraph("Open inventory for the opaque Potions list on its right. Every legal Normal, Strong and Extended beneficial potion has its own labeled row. Scroll the list with the wheel or its scrollbar; crafting requires an active Dungeon 1 run."),
            HelpBlock.examples("healing_potions"),
            HelpBlock.bullet("Your positive thrown potions aid allies and their companions, and do not affect hostile mobs, including healing damage against undead. Restorative class arrows have their own rules."),
            HelpBlock.bullet("Both Theurgist and Venefex can craft mixed-effect drinkable Turtle Master. Its splash/lingering variants are not offered. Luck has no vanilla brewing recipe."),
            HelpBlock.examples("turtle_master"),
            HelpBlock.bullet("Green rows show crafts from your own inventory. A blue count uses eligible shared materials; light-red rows with red 0 are unavailable."),
            HelpBlock.bullet("Counts are alternative recipe capacities, not reservations. Hover for effects and the own/shared-material explanation."),
            HelpBlock.bullet("Click creates exactly one potion on the cursor. Repeat to stack identical crafted potions, with all components matching, up to 64. A different/full cursor spends nothing."),
            HelpBlock.bullet("Shift-click crafts exactly one into inventory, hotbar first. Drinking or throwing uses one potion at a time; drinks return a bottle. Crafting has no hotkey."),
            HelpBlock.heading("Real Ingredients and Fuel"),
            HelpBlock.bullet("Use a water bottle or supported intermediate potion plus every remaining vanilla-chain ingredient, including gunpowder for splash. Some recipes do not need nether wart."),
            HelpBlock.examples("brewing_fuel"),
            HelpBlock.bullet("Hover a row for its selected one-output route: one consumed potion input, remaining ingredients and own/eligible-group quantities. Intermediate stages are made within the route, not charged again."),
            HelpBlock.bullet("The required powder total includes reagent and new fuel; the fuel line identifies the refill part. Saved charges reduce new fuel. Own affordable routes are preferred; unavailable rows quote no affordable route. Quantities reserve nothing and pending clicks show the previous snapshot until the server replies."),
            HelpBlock.bullet("The gauge at the top of the list shows remaining fuel. One blaze powder supplies 20 brewing steps; each remaining recipe step uses one charge. Intermediate potions skip completed steps."),
            HelpBlock.bullet("Strength may need separate blaze powder for its ingredient and fuel. Unused charges survive ordinary saves, reconnects and death. Failed/full destinations spend no fuel or ingredients."),
            HelpBlock.paragraph(Component.literal("Your ingredients take priority; eligible consenting teammates can supply missing materials. Read ").append(link("Party sharing", "party")).append(".")),
            HelpBlock.bullet("Requests, random-potion skills and Brewing Supplies are retired. Old virtual balances do not become physical item refunds."),
            HelpBlock.heading("Resurrect..."),
            HelpBlock.paragraph(Component.literal("Hover ").append(key("Resurrect...", "theurgist_resurrect"))
                    .append(" for your current binding. This guide does not cast the skill.")),
            HelpBlock.bullet("Available immediately and free. Open the inventory picker or its shortcut, default Alt+4; opening does not cast."),
            HelpBlock.bullet("Choose a dead teammate in the same active run. They have 60 seconds to accept or decline. Success returns them to their latest death location with five seconds of protection and recoverable inventory handling."),
            HelpBlock.bullet("Only success starts the caster's 180-second cooldown. Declined, expired, stale and failed attempts cost nothing. Death, reconnect and run cleanup do not reset it; server downtime pauses it."),
            HelpBlock.bullet("Resurrection cannot recreate missing physical drops. Party hover and the death screen show player/mercenary Theurgist readiness and cooldowns. Mercenary resurrection retains its separate level-10 Positive Potions unlock."),
            HelpBlock.heading("Brewing Stands"),
            HelpBlock.bullet("Only Theurgists can use brewing stands. With valid ingredients and fuel, the server's instant-brewing setting shortens valid brews while the stand remains open."),
            HelpBlock.paragraph(Component.literal("Visit ").append(link("Eon Penrose", "brewing_store")).append(" for ingredients, equipment and class-gated potion stock.")),
            HelpBlock.paragraph(Component.literal("Change saved active shortcuts in ").append(link("Quick Reference", "references")).append("."))));

    public static final Page PYROCLAST = page("class.pyroclast", "Pyroclast", true, List.of(
            HelpBlock.heading("Fire and Controlled Chaos"),
            HelpBlock.paragraph("A former pyrotechnician and demolition hobbyist, the Pyroclast brings sword, bow, crossbow and explosive utility to dungeon fights."),
            HelpBlock.bullet("Right-click flint with gravel in your inventory to make gunpowder. Default cost is one flint and one gravel for one gunpowder; keep room for the output."),
            HelpBlock.bullet("The removed 2x2 inventory crafting grid is not used. A pickup reminder may appear when you have matching materials."),
            HelpBlock.bullet("Authored vanilla Cinderkiss, Cinderbite and Cindermaul rockets load from inventory or hands into bows and crossbows. Their components and existing effects are preserved. R cycles legal ammunition for the next load; the hint uses your current binding."),
            HelpBlock.bullet("Ordinary right-click near a Cosmic Spawner allows weapon use. Developer Shift + right-click deliberately copies held equipment; skeletons can use copied bows."),
            HelpBlock.bullet("Authored D1 Cinderbite and Cindermaul rockets have enhanced enemy-only explosions with obstruction checks and no terrain damage. Damage follows server settings."),
            HelpBlock.bullet("Sword, bow and crossbow progression is passive. Use Pyroclast-attuned gear and class chests; a renamed ordinary item does not gain an ability."),
            HelpBlock.paragraph(Component.literal("Read ").append(link("Gear and trading", "trading")).append(" before handing equipment to another player."))));

    public static final Page BOGATYR = page("class.bogatyr", "Bogatyr", true, List.of(
            HelpBlock.heading("Bogatyr"),
            HelpBlock.paragraph("A survivalist, cold-weather endurance racer, and hardened wolf-handler from the frozen Russian Federation reaches, the Bogatyr favors traditional weaponry and grit."),
            HelpBlock.bullet("Fight beside loyal wolves with sword, bow and shield. Sword/bow skill progression is passive."),
            HelpBlock.examples("bogatyr_wolves"),
            HelpBlock.bullet("Can use Bogatyr-attuned gear and class chests."),
            HelpBlock.bullet("Acquire companions through wolf eggs, ordinary bone taming and breeding. Player packs are uncapped and belong to the current dungeon run."),
            HelpBlock.command("Living loaded Wolfpack count in this dungeon", "/d1 wolves"),
            HelpBlock.bullet("Open inventory Skills to choose Patrol or Stay. Both are free; Kibble and the old skills are retired."),
            HelpBlock.paragraph(Component.literal("Your saved bindings: ")
                    .append(key("Patrol", "bogatyr_patrol")).append(", ")
                    .append(key("Stay", "bogatyr_stay")).append(", ")
                    .append(key("Feed Pack", "bogatyr_feed_pack")).append(". Hover to inspect; guide links do not execute actions.")),
            HelpBlock.tip("Active-skill defaults: Alt+1 Patrol, Alt+2 Stay, Alt+3 Feed Pack. Rebind or unbind in Options > Controls > Key Binds > Cosmic Dungeon; gray skill hints always show your current binding."),
            HelpBlock.tip("Shortcuts work in the world or your inventory, once per press. Chat, other menus, carried items and dragging block them. Resolve duplicate skill/native chords in Controls; different modifiers use distinct chords even if native Controls highlights the same base key."),
            HelpBlock.bullet("Patrol is the default: wolves follow, defend themselves and their master, and assist against hostile monsters their master successfully attacks."),
            HelpBlock.bullet("Stay calls wolves back to sit within 5 blocks. Each holds its settled place even when you move away and ignores threats."),
            HelpBlock.tip("Choose Patrol to stand the pack up. Stay clears earlier fights; repeated Stay clicks do not move settled wolves."),
            HelpBlock.bullet("Feed Pack spends up to 10 actual items from your inventory: bones/rotten flesh first, then raw meat, then other meat by player food value. Injured wolves are healed first; remaining food goes to healthy adult pairs within 8 blocks of each other."),
            HelpBlock.bullet("Bones only act as food through Feed Pack. Ordinary bones, taming, and meat feeding are unchanged. Pups, breeding cooldowns, individually seated Patrol dogs, and adults without a nearby mate do not spend breeding food."),
            HelpBlock.bullet("In Stay, feeding allows temporary mating movement. Wolves return to their original held places after birth or a failed attempt (30 seconds at default settings). Saves/reloads cancel unfinished attempts safely."),
            HelpBlock.bullet("Allies cannot hurt or sit your wolves, but can feed and breed them. Hired Bogatyrs admit at most 20 dogs, starting at one every 30 active seconds. Their existing skill progression reduces that interval to a 20-second minimum; player packs remain uncapped."),
            HelpBlock.tip("Active packs survive saves and reconnects. Completion, forfeit or reset ends the pack; old archives cannot be recalled.")));


    public static final Page DRAGOON = page("class.dragoon", "Dragoon", true, List.of(
            HelpBlock.heading("Trident and Repair Support"),
            HelpBlock.paragraph("A trident-wielding specialist in battlefield salvage and historical armorcraft."),
            HelpBlock.bullet("Use Dragoon-attuned gear and class chests. Successful melee or thrown trident hits can trigger nearby enemy chain lightning. Sword hits are not a replacement trigger."),
            HelpBlock.examples("dragoon_trident"),
            HelpBlock.bullet("Trident progression improves the relevant passive damage and chain benefits. Chain damage does not recursively trigger more chains."),
            HelpBlock.heading("Repair Affinity"),
            HelpBlock.bullet("Dragoons can use anvils and repair supported damaged gear for another player. The customer retains ownership."),
            HelpBlock.command("Invite", "/repair <player>"),
            HelpBlock.command("Accept", "/repair accept <player>"),
            HelpBlock.command("Deny", "/repair deny <player>"),
            HelpBlock.command("Cancel", "/repair cancel"),
            HelpBlock.bullet("The customer offers the damaged item and an optional Trace labor fee. The Dragoon supplies compatible repair components; approved ordinary unmarked materials are accepted where supported."),
            HelpBlock.bullet("Both players must remain nearby with the interface open. Review the quote and readiness controls. Materials and fees move only after successful server validation; unsupported special items cannot be repaired."),
            HelpBlock.paragraph(Component.literal("For direct shop repairs and materials, visit ").append(link("Elias Centvin", "vendor.weapons")).append("."))));

    public static final Page VENEFEX = page("class.venefex", "Venefex", true, List.of(
            HelpBlock.heading("Poison and Pressure"),
            HelpBlock.paragraph("A toxicologist shaped by invasive-species decay, the Venefex uses sword, bow, poison and weakening effects to wear enemies down."),
            HelpBlock.bullet("Use Venefex-attuned gear and class chests. Sword/bow progression and relevant arrow-duration benefits are passive."),
            HelpBlock.bullet("Tree Viper, Bushmaster and Fer-de-Lance poison; Pestis and Black Bubo weaken; Vapours, Melancholia and Deathly Stupor slow."),
            HelpBlock.bullet("Spicule Breach and Rend deal instant harm that scales with harmful effects against living targets under server settings. Spicule heals undead; native poison immunities still apply."),
            HelpBlock.heading("Inventory Crafting"),
            HelpBlock.bullet("Open inventory for the opaque Potions list on its right. Every legal Normal, Strong and Extended harmful potion has its own row. Scroll with the wheel or scrollbar. Outside a dungeon the recipes remain visible but disabled."),
            HelpBlock.examples("harming_potions"),
            HelpBlock.bullet("Both potion classes may craft drinkable Turtle Master. Its splash/lingering forms and a fictional Luck recipe are not offered."),
            HelpBlock.examples("turtle_master"),
            HelpBlock.bullet("Click crafts one onto the cursor, stacking only identical components up to 64. Shift-click crafts one into inventory, hotbar first. Full/incompatible destinations spend nothing."),
            HelpBlock.bullet("Green counts use own materials, blue counts need eligible shared materials, red 0 is unavailable. Counts reserve nothing. The top fuel gauge spends one charge per recipe step; one blaze powder supplies 20."),
            HelpBlock.paragraph(Component.literal("Read the ").append(link("Theurgist crafting reference", "class.theurgist")).append(" for full ingredient, fuel, sharing and stack rules.")),
            HelpBlock.bullet("Hover a row for its selected one-output route: one consumed potion input, remaining ingredients and own/eligible-group quantities. Intermediate stages are made within the route, not charged again."),
            HelpBlock.bullet("The required powder total includes reagent and new fuel; the fuel line identifies the refill part. Saved charges reduce new fuel. Own affordable routes are preferred; unavailable rows quote no affordable route. Quantities reserve nothing and pending clicks show the previous snapshot until the server replies."),
            HelpBlock.tip("Brewing-stand interaction remains Theurgist-only. Venefex uses the Potions list in inventory, with no potion hotkey or old separate Skills window.")));

    public static final Page JUDICATOR = page("class.judicator", "Judicator", true, List.of(
            HelpBlock.heading("Oathbound Protector"),
            HelpBlock.paragraph("A wandering arbiter sworn to uphold balance, the Judicator supports the party with mace, bow and restorative or revealing arrows."),
            HelpBlock.bullet("Scintilla Vitalis and Lux Vitalis heal living targets and harm undead. Ebonsight provides night vision; Vielpiercer applies Glowing."),
            HelpBlock.bullet("Use the actual authored item's tooltip and server values. Mace/bow progression is passive; it has no new action hotkey."),
            HelpBlock.bullet("Use Judicator-attuned gear and class chests. Eon offers healing stock shared with Theurgists."),
            HelpBlock.paragraph(Component.literal("Repair Affinity and anvil access belong to ").append(link("Dragoon", "class.dragoon")).append(".")),
            HelpBlock.paragraph(Component.literal("Visit the ").append(link("Brewing Store", "brewing_store")).append(" for its class-gated healing rows."))));

    public static final Page METALMANCER = page("class.metalmancer", "Metalmancer", false, List.of(
            HelpBlock.heading("Coming in Dungeon 2"),
            HelpBlock.paragraph("Metalmancer is disabled.")));

    public static final Page DEADEYE = page("class.deadeye", "Deadeye", false, List.of(
            HelpBlock.heading("Coming in Dungeon 2"),
            HelpBlock.paragraph("Deadeye is disabled.")));

    public static final HelpNode ROOT = node(rootPage(),
            node(GET_STARTED),
            node(DUNGEONS),
            node(PARTY,
                    node(FACTIONS),
                    node(PROGRESSION),
                    node(TELEPORTATION),
                    node(VENDORS,
                            node(VILLAGE_SOULS,
                                    node(BEATRIX_FARROW, node(FOOD_VENDOR)),
                                    node(ELIAS_CENTVIN, node(WEAPONS_SUPPLIER)),
                                    node(EON_PENROSE, node(BREWING_STORE)),
                                    node(JOHN_HAMISH_WATSON),
                                    node(BELUZON),
                                    node(NATON_WHITLOCK, node(GENERAL_SUPPLY_VENDOR)),
                                    node(TAMSIN_VANE, node(TELEPORT_VENDOR))))),
            node(CLASSES,
                    node(BOGATYR),
                    node(DRAGOON),
                    node(JUDICATOR),
                    node(PYROCLAST),
                    node(THEURGIST),
                    node(VENEFEX)),
            node(generalPage(),
                    node(ACHIEVEMENTS),
                    node(CURRENCY),
                    node(TRADING)),
            node(REFERENCES));

    public static final List<HelpNode> ROOT_ENTRIES = ROOT.children();




    private static String natonBackstory() { return """
Naton Whitlock was once a general supplier by profession and a public inconvenience by necessity.

He made his living buying excess goods from couriers, wagon drivers, pack handlers, and anyone else who might have a crate or two fall off the wagon while passing through town. He did not rob caravans. He was firm on that point. He was simply present at the right time with coin in hand, a friendly smile, and a special charisma for noticing when a delivery contained twelve buckets but only eleven were expected.

Naton called this commerce.

Several clerks called it deeply irritating.

He dealt in small necessities. Bread. Apples. Torches. Buckets. Flint and steel. Things travelers forgot until the precise moment forgetting became a problem. He had a talent for being nearby when someone said, “I wish I had brought one of those.”

He also had a talent for discovering, at that same moment, that the price had just gone up.

Naton’s career carried him through many settlements, though rarely twice in the same season. In Felbridge, he was chased out after selling defective “rain-tested torches” after a storm. In Oakhollow, a customer broke a tooth on three day old bread Naton had labeled “firm travel loaf.” In Dunmere, he was asked to leave after renting the same bucket to one farmer three times before noon.

Naton insisted every complaint was a misunderstanding, except, of course, the bucket matter, which he considered sound business.

His shop was never impressive. It was crowded, tilted, over-labeled, and arranged according to a system known only to him and possibly one clever cat. Customers often found carrots under torch bundles, apples in tool crates, and a sign reading, “All sales final. All complaints ignored.”

He was proud of that sign.

By the time death found him, Naton had survived angry customers, suspicious clerks, unpaid guards, kicked shins, thrown turnips, and one regrettable incident involving a goat, three sacks of potatoes, and a bridge toll.

When his soul was restored through the Lesser Bloom system, Naton returned with the posture of a man who had expected to wake inside a crate and was relieved to find himself only partly dead. He resumed business quickly, though no one is entirely certain where his new stock comes from.

Naton says he has suppliers.

When asked who they are, he looks around, lowers his voice, and says, “Reliable fellows. Tall. Private. Excellent delivery times.”

He will not say more.

Naton considers his work honest enough to stand behind, though preferably not in front of.
"""; }
    private static String natonNotes() { return """
Naton’s stall is crowded with useful things arranged in a manner that suggests planning, panic, and several interrupted breakfasts. Food sits beside fire-starting tools. Buckets hang from hooks at unequal heights. Torch bundles are tied neatly, though never in the same kind of cord twice.

His supplies are ordinary in use, but not always ordinary in arrival. Crates sometimes appear before anyone sees them delivered. Apples occasionally roll out from under shelves that were empty moments before.

He does not sell rare treasures. He sells the things travelers should have brought before leaving home.
"""; }
    private static String eliasBackstory() { return """
Elias Centvin was once a blacksmith of renown. When his son, Oren, enlisted as a Dragoon, Elias was always there to help.

The Dragoons patrolled the nearby towns and roadways. Whenever Oren returned to the forge, Elias would see him approach and call, “How did the armor hold?”

Oren would grin through road dust and exhaustion.

“It holds.”

Elias always answered, “Then it shall hold longer,” as he pulled the damaged pieces from his son and began to work.

At first, the work was simple. Split straps. Bent buckles. Dents hammered into breastplates. Elias learned the intricacies of Dragoon armor because Oren kept bringing it home broken, proud, and alive.

Over time, the lower roads they patrolled became crowded with walking monsters.

The Dragoons called them the Hollow Mail. They were soldiers in armor, but not human enough. They walked after wounds that should have ended them. Their shields bore narrow claw marks no animal had made. Their helmets turned toward sound, though some had no heads inside them.

Eventually, the Dragoons marched out to stop the menace once and for all. Elias packed what gear he could and followed behind.

For several days, the Dragoons fought at the breach. Armor came back scorched, torn, and bent into impossible shapes. Elias patched, hammered, reset, and tightened until his hands bled and sweat matted his hair and clothes.

On the third night of battle, Oren brought back a breastplate with deep creases across the front.

Elias set both hands on the plate.

“How did it hold?”

Oren smiled.

“It holds.”

Elias looked at the damage, then at his son.

“Then it shall hold longer.”

He repaired it before dawn, and Oren wore it once again into the breach.

After the fighting ended, bones rose inside Dragoon armor and came against the support camp. As the hungry dead approached, Elias saw familiar helmets moving in the dark. He heard Dragoon mail clattering over empty ribs, and among their ranks he spotted the breastplate he had repaired the night before.

The tradesmen, doctors, and support staff rose one at a time, each realizing the threat in their own moment. Then, together, they took up the tools of their trades and carried on the fight the Dragoons had lost. Only this time, they fought the Hollow Mail, who had been their friends, family, and defenders only a day before.

Elias rose as well. His forge hammer in hand, he paced out to unmake the armor he had strengthened only the night before and lay his son to rest. As he swung his hammer, he felt webs pulling him down. He felt them, cold and wet, winding up his legs and constricting his heart even as he charged forward.

From across the expanse, Atlach-Nacha’s webs searched among the broken souls under the earth, hunting for something strong enough to anchor the tugging of worlds.

In the dark, Elias was dragged underground. As the webs passed through him, he felt his life ending and his soul divided into pieces useful enough to keep, but scattered far and wide throughout the dungeon.

When restored through the Lesser Bloom system, Elias returns as a semi-solid remembrance of himself. He appears as a working armorer because that is how he remembers his most important contribution.

Elias does not speak about death, the Dragoons, or even his son. But every so often, you may hear him grumble about cracks, dents, and whether something will hold.

Because of his affinity with the Dragoons, Elias stocks a secret list of armor patches that some gifted in the art of weapon and armor repair may find useful.
"""; }
    private static String eliasNotes() { return """
Elias’s shop has the order of a dedicated mind and a disciplined body. Tools hang in straight lines. Repair materials are sorted by weight and use. In front of his shop is an old grindstone, and behind it an old Dragoon anvil, but only Elias is permitted to labor over the anvil within.

His Dragoon stock is arranged apart from the rest. Leather patches, chain links, ingots, diamonds, and Netherite repair fragments are ordered carefully, yet hidden from most eyes.

Some players report hearing a second hammer strike when Elias works, though no second hammer is visible. Others say the repaired item feels briefly cold or damp, as if it has passed through a place with no sun before returning to the hand.

But do not ask Elias. He knows only that broken armor should be made whole, dull weapons should be made sharp, and some battles are survived because someone mended what others had given up.
"""; }
    private static String eonBackstory() { return """
Eon was once an apothecary in the heart of town. His shop smelled of simmering sugar solutions, savory herbs, and the slightest traces of smoke from the small flames he kept beneath his bottles.

For Eon, business had never been better. Just last year, he had discovered how to make healing potions linger in the air, and now he could barely keep up with demand. Eon knew that a good theurgist could follow recipes, but the best theurgists were not afraid to take chances. And take chances he did.

Well after dark, when the streets were empty and his shop was closed, Eon gathered his supplies and experimented through the night. A little less heat. A little more powder. One drop added, two drops regretted. He kept careful notes, but his best discoveries usually began with the words, “That should not have happened.”

When the dungeon’s influence reached the town, the first signs appeared in the sickroom. Wounds closed too quickly, then opened in wrong places. Sleep tonics brought dreams of black corridors. Patients woke speaking in an ancient tongue no one could decipher.

Eon followed the symptoms until the symptoms followed him back.

At first, the signs were small. A burn on a patient’s hand appeared faintly on his own. A sleeping man whispered a phrase, and Eon later found the same words written in his notes, though he had no memory of putting them there. Slowly, terribly, he began to understand that the foreign tongue was not sickness. It was ritual.

The words spoke of vast powers beyond the ordered world, of webs drawn across hidden spaces, and of a hunger that mistook souls for thread. Eon was frightened, but fear did not stop him. It clarified the work.

He began searching for a cure, one mixture at a time. Each failure taught him something, and every failed mixture brought him closer to the cure. Whether standing at his station or lying sleepless in bed, he could feel a downward pull upon his soul.

The pull grew stronger as the sickness spread. Eon came to believe that the answer could not be found in his shop alone. If the ritual language had risen from below, then some part of its cure might be waiting below as well.

So Eon descended.

He entered the labyrinth beneath the city with bottles wrapped in cloth and notes tucked against his chest. In the passages below, where Atlach-Nacha’s web had grown thickest, the dead rose around him. Skeletons climbed from the dark in broken armor and burial cloth, closing in until there was nowhere left to run. Glass shattered around him as Eon breathed his last.

When restored through the Lesser Bloom system, Eon resumes his work because his soul remembers the need for a cure. He mixes ingredients, steadies flames beneath glass bottles, and watches percolating liquids for the smallest change in color. He sells brewing ingredients, equipment, and potions because he believes the cure still exists somewhere in the work.

Every failed mixture brings him closer to the cure.
"""; }
    private static String eonNotes() { return """
Eon’s shop feels lived in, worked in, and slightly overfilled. Bottles crowd the shelves. Notes sit beneath paperweights. Brewing stands click softly as liquid moves through narrow glass necks. The air carries sugar, herbs, smoke, and the clean sharpness of potion work.

He does not treat failure as shameful. A spoiled batch, cracked bottle, or useless potion only sends him back to his notes with renewed interest. Travelers who expect certainty may find him unsettling. Travelers who need hope often find him useful.

Some potions in Eon’s shop glow after the room has gone dark. He claims this is a promising sign, though he has not yet determined what.
"""; }
    private static String beatrixBackstory() { return """
Beatrix Farrow is a restored village soul and the only known source of Farrow’s Chop. She keeps a small pig farm near the village lanes, tending her animals with the quiet seriousness of a child repeating work she remembers from life.

Beatrix Farrow was once a child on her parents’ pig farm, long before the Village became a refuge for restored souls. Her memories of life are simple, but stubborn. Mud underfoot. Warm feed buckets. Piglets pressing close in the cold. Her parents calling her in before dusk.

Those memories ended in the dungeon.

Beatrix and her family were among the many souls drawn into the deeper working of Atlach-Nacha, the Cosmic Weaver. For centuries, Atlach-Nacha brushed against Earth from beyond the thin places, seeking a stronger anchor point. She entangled lives, places, memories, and souls in her cosmic web, but most broke under the strain. Beatrix’s family was lost in that pressure. Their souls did not hold together.

Beatrix’s soul survived, but not unchanged.

When her soul is restored through the Lesser Bloom system, she returns as the child she remembers herself to be, carrying fragments of the farm that once made her happy. She begins again in the Village, small and quiet, building a little pig farm from memory more than knowledge. She feeds the animals, keeps the pens, and prepares food for travelers who still have roads ahead of them.

Beatrix does not understand what happened to her family. She does not understand why certain cuts of meat carry more than scent after passing through her hands. She only knows that some food comforts people, and some food brings them back.

The truth is stranger. Beatrix’s brief contact with Atlach-Nacha’s web left a cosmic affinity threaded through her soul. The food she prepares, especially Farrow’s Chop, carries a faint interdimensional charge. When cooked over Beatrix’s Campfire, that charge binds to the fire, the Village, and the traveler who prepared it, opening a temporary path between them.

Beatrix believes she is only feeding people.

She may be right. But sometimes, a warm meal is enough to turn the traveler’s heart toward home.
"""; }
    private static String beatrixNotes() { return """
Beatrix’s farm is small because she is small. A few pens, a few animals, a few routines recovered from a life that ended badly. There is comfort in the shape of it, but not peace.

Travelers often notice that Beatrix seems happiest when working. She speaks more easily to pigs than to adults and becomes guarded when asked too much about her parents. She remembers them kindly, but never clearly enough to finish the story.

Farrow’s Chop carries the warm smell of a village hearth, but beneath that comfort is something that does not belong to hearths, villages, or meat. The scent lingers too long. The memory it stirs is too strong. Some travelers say it reminds them of home, even when they cannot remember having one.

Beatrix is unaware of the cosmic nature of her gift. She does not know that Atlach-Nacha’s failed attempt to anchor herself through weaker souls left traces behind. She does not know that her family broke in the same web that marked her.

She only knows the pens need cleaning, the animals need feeding, and travelers come back hungry.
"""; }
    private static String tamsinBackstory() { return """
Tamsin Vane was already a seller of recovered treasures before she became a seller of dangerous opportunities. Tomb roads, collapsed shrines, sealed wells, and abandoned military works all interested her for the same practical reason. Old places held old treasures, and old treasures sold for big money.

She did not call herself a grave robber. She preferred treasure hunter, expedition guide, or independent recovery specialist, depending on who was doing the asking.

Tamsin found a narrow cave mouth half-hidden behind stone and root. She expected a smuggler’s hollow, or a buried strongbox if fortune smiled. Instead, she found a passage sloping farther down.

Beyond the first chamber, she found the remains of an old makeshift camp.

Bedrolls had rotted into dark cloth. Crates had split. A lantern hung from a bent hook with no oil left inside it. Nothing in the chamber looked recently used, but nothing looked entirely abandoned either.

Beneath a fallen pack, she found the map.

It showed a route through the tunnels below, twisting in loops and spirals until the lines nearly crossed themselves. At the end was an X beside the words Base Camp. In the bottom right corner, written in a careful hand, were three initials.

JHW.

Tamsin did not know the initials, but she knew value when she saw it.

She followed the map farther down. The lower passages carried sound strangely. Footsteps arrived before she took them. Once, she saw torchlight ahead and hurried toward it, only to find her own lantern sitting on a stone ledge she had not yet reached.

That was enough for Tamsin.

She returned to the surface with the map, three broken fingernails, and a new respect for good judgment. She decided there was treasure below, almost certainly. There was also death below, absolutely. She could not spend the treasure if she was dead, so she chose to sell the opportunity to someone more adventurous.

When players meet her, Tamsin presents the map with confidence, charm, and very little desire to personally test the route again. She speaks of lost camps, buried wealth, forgotten gear, and the kind of treasure people only leave behind when something has gone impressively wrong.

Tamsin considers this a warning.

She also considers it excellent marketing.
"""; }
    private static String tamsinNotes() { return """
Tamsin stands just off the main path, watching for newcomers to the area. The map is tied with string and hangs from her belt. A compass hangs beside it, though the needle has been known to spin from time to time.

She does not understand the full importance of the initials JHW. To her, they are proof that someone educated, funded, or desperate once reached the tunnels before her. That is enough to make the map valuable.

Tamsin is not cruel. She is simply better at measuring risk for herself than for others. She tells players the route is dangerous, then immediately mentions what might be found at the bottom.
"""; }
    private static String watsonBackstory() { return """
To Whom It May Concern, Should They Descend

Written from the lower depths.

Anno Domini 1897

In life, I had always sensed the oppressive shadows draped upon our world, pressing down from rooftops and lurking beneath streets paved in whispers. My scholarly pursuits into antiquities, once governed by reason, gradually surrendered to obsession with the hidden pathways beneath.

These pathways were labyrinthine corridors spiraling endlessly downward, carved from stone by hands far older and stranger than our own. My predecessors left scattered warnings, journals whose pages grew frantic, cryptic, and stained with despair.

In time, and in defiance of their scribbled pleas for caution, I too fell under the downward sway. Though a fear pleaded with me to remain aboveground, curiosity compelled me onward, guiding my steps through crumbling archways into forgotten subterranean realms.

There, beyond the reach of sunlight, after months of careful exploration, I established a modest camp amid ruins shaped by eldritch geometries. Lanterns were lit, supplies gathered, maps painstakingly drawn. Foolishly, I believed myself secure within that silent abyss.

Yet something found me there in the gloom beneath our city.

A presence older than history stirred within those tunnels, whispering secrets never meant for mortal ears. Its terrible voice drove me frantically back toward the surface, shattered and hollow. I abandoned my camp with lanterns extinguished, provisions untouched, notes scattered across the ground.

Even then, I knew others would eventually follow my footsteps into the darkness.

During my time beneath the surface, I discovered faded murals hinting at six rare subterranean flowers whose blossoms held the power to shield travelers from madness and ward off horrors lurking in the shadows.

In my travels, I found the flowers, but as fate would have it, I never truly understood their enigmatic power until it was too late.

In my desperate flight upward, I carefully placed one flower blossom at each of the six camps where I paused to rest. Influenced by the murals, perhaps I believed they emitted a cleansing aura. Or perhaps they merely marked my fleeting defiance against that citadel of evil.

Upon reflection, I now suspect these flowers did not cleanse, but instead absorbed my life while I slept. Fragments of myself may still cling to them, bound in the petals and blooms that once drew breath from me.

Should you come upon them, I implore you, gather all six and return them to me. If any remnant of my spirit endures, their presence may grant me release, and I may at last be permitted to rest.

But if you do nothing else, do not leave them scattered, or even in death, I remain tethered to what lies beneath.

After days of trekking ever upward, I remember vividly the desperate moment when I emerged beneath the moonless sky, gasping and stumbling through shadows. I glimpsed above me a strange winged silhouette gliding silently among clouds stained silver by starlight.

Madness clawed at my thoughts while shrieks of pursuing horrors echoed beneath my feet. Knowing my fate was sealed, I hurriedly completed this letter by the dwindling flame of my final candle.

Raising a trembling hand toward the sky, I waved my message frantically, shouting for the creature above to carry my warning onward. Behind me, the earth rumbled as darkness whispered my name, promising oblivion.

If you receive this correspondence, understand it was written long before your eyes fell upon these words. It was composed feverishly in dreadful certainty of the fate awaiting me.

But now, I fear my bones lie scattered quietly in the darkness, my voice forever silenced by truths beyond comprehension. Yet perhaps my spirit lingers still, eternally bound to those tunnels where reason faltered and madness took root.

Below awaits that abandoned camp, perhaps untouched, perhaps disturbed by the shadows that once whispered my name.

Enter cautiously, for if you venture downward into the city’s forgotten depths, you may discover more than ruin. You may learn that even death cannot sever a soul’s bond with what lies buried beneath.

May Providence shield your reason where mine failed.

J.H.W.
"""; }
    private static String watsonNotes() { return """
Watson’s first known presence is not his ghost, but his warning. The letter establishes the lower depths, the abandoned Base Camp, and the six spectral blooms as remnants of his failed escape.

Watson believed the blooms offered protection, but later understood that they had drawn fragments of his life into themselves while he slept. His spirit remained divided among them, leaving him bound to the tunnels after death.

The initials JHW on Tamsin Vane’s map connect her discovery to Watson’s original expedition. Tamsin sees the initials as proof of value. Watson’s later restoration reveals them as evidence of a doomed scholar’s descent.

Once all six blooms are rescued from the dungeon, Watson’s soul is released. He remains dead, but no longer scattered. His restored ghost becomes a guide for later dungeon progression. Though Atlach-Nacha’s thread still keeps him from true rest, his affinity for her webs and his keen mind allow him to track dangers nearing our cosmos.
"""; }

    private static Page lorePage(String id, String title, String role, String purpose, String backstory, String notes) {
        List<HelpBlock> blocks = new ArrayList<>();
        blocks.add(HelpBlock.heading(title));
        blocks.add(HelpBlock.heading("Role"));
        blocks.add(HelpBlock.paragraph(role));
        blocks.add(HelpBlock.heading("Purpose"));
        blocks.add(HelpBlock.paragraph(purpose));
        blocks.add(HelpBlock.heading("Backstory"));
        blocks.addAll(HelpBlock.longText(backstory));
        blocks.add(HelpBlock.heading("Lore Notes"));
        blocks.addAll(HelpBlock.longText(notes));
        blocks.add(HelpBlock.paragraph(Component.literal("Read ").append(link("Village NPCs", "village_souls"))
                .append(" for related characters, or ").append(link("Progression", "progression"))
                .append(" for current dungeon objectives.")));
        return page(id, title, true, List.copyOf(blocks));
    }

    private HelpMenuContent() {}

    /** Internal guide navigation metadata only; never a command or external URL. */
    static Component link(String label, String pageId) {
        return Component.literal(label).withStyle(style -> style.withColor(0x73BBFF)
                .withUnderlined(true).withInsertion("cosmic_help:" + pageId));
    }

    /** Hover/focus reads the saved binding only; this never invokes a skill. */
    static Component key(String label, String actionId) {
        return Component.literal(label).withStyle(style -> style.withColor(0x73BBFF)
                .withUnderlined(true).withInsertion("cosmic_key:" + actionId));
    }

    private static HelpNode node(Page page, HelpNode... children) { return new HelpNode(page, List.of(children)); }
    private static Page rootPage() { return page("root", "Guide", true, List.of(
            HelpBlock.heading("Cosmic Dungeon Guide"),
            HelpBlock.paragraph("Choose a directory entry on the left to get started."))); }
    private static Page generalPage() { return page("general", "General", true, List.of(
            HelpBlock.heading("General Reference"),
            HelpBlock.paragraph("General topics cover achievements, currency, and trading. Follow the topic links and current in-game requirements."),
            HelpBlock.tip("Choose a child topic from the left directory."))); }
    private static Page validationPage() { return page("validation", "Validation", true, List.of(
            HelpBlock.heading("Server Validation"),
            HelpBlock.paragraph("Cosmic Dungeon validates gameplay-sensitive behavior on the server."),
            HelpBlock.bullet("Class access, vendors, trades, currency, progression, teleports, achievements, and dungeon systems remain server-authoritative."),
            HelpBlock.tip("The H menu is only a guide; it never grants access or changes saved data."))); }

    /** Exact existing H-guide pages; unknown/unselected classes open the class index. */
    public static Page classGuide(String classId) {
        if (classId == null) return CLASSES;
        return switch (classId) {
            case "bogatyr" -> BOGATYR;
            case "dragoon" -> DRAGOON;
            case "judicator" -> JUDICATOR;
            case "pyroclast" -> PYROCLAST;
            case "theurgist" -> THEURGIST;
            case "venefex" -> VENEFEX;
            default -> CLASSES;
        };
    }

    public static HelpNode classDirectory() {
        return ROOT.children().stream().filter(node -> node.page() == CLASSES).findFirst().orElseThrow();
    }

    private static Page page(String id, String title, boolean enabled, List<HelpBlock> blocks) { return new Page(id, Component.literal(title), enabled, blocks); }
    public record Page(String id, Component title, boolean enabled, List<HelpBlock> blocks) {}
    public record HelpNode(Page page, List<HelpNode> children) {
        public boolean isDirectory() { return !children.isEmpty(); }
    }
    public record HelpBlock(Kind kind, Component label, Component text) {
        static HelpBlock heading(String text) { return new HelpBlock(Kind.HEADING, Component.empty(), Component.literal(text)); }
        static HelpBlock paragraph(String text) { return paragraph(Component.literal(text)); }
        static HelpBlock paragraph(Component text) { return new HelpBlock(Kind.PARAGRAPH, Component.empty(), text); }
        static HelpBlock examples(String topic) { return new HelpBlock(Kind.ITEMS, Component.empty(), Component.literal(topic)); }
        static HelpBlock bullet(String text) { return new HelpBlock(Kind.BULLET, Component.empty(), Component.literal(text)); }
        static HelpBlock command(String label, String text) { return new HelpBlock(Kind.COMMAND, Component.literal(label), Component.literal(text)); }
        static HelpBlock tip(String text) { return new HelpBlock(Kind.TIP, Component.empty(), Component.literal(text)); }
        static HelpBlock spacer() { return new HelpBlock(Kind.SPACER, Component.empty(), Component.empty()); }
        static List<HelpBlock> longText(String text) {
            List<HelpBlock> blocks = new ArrayList<>();
            for (String paragraph : text.split("\\R\\s*\\R")) {
                String trimmed = paragraph.strip();
                if (!trimmed.isEmpty()) blocks.add(paragraph(trimmed.replaceAll("\\R", " ")));
            }
            return List.copyOf(blocks);
        }
    }
    public enum Kind { HEADING, PARAGRAPH, BULLET, COMMAND, TIP, SPACER, ITEMS }
}
