package net.goui.cosmicdungeon.client.screen;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class HelpMenuContent {
    public static final Page GET_STARTED = page("get_started", "Get Started", true, List.of(
            HelpBlock.heading("Welcome, Dungeoneer"),
            HelpBlock.paragraph("Cosmic Dungeon is a dungeon-focused adventure pack with classes, party flow, currency, trading, vendors, achievements, and progression."),
            HelpBlock.bullet("Press H to reopen this help menu at any time."),
            HelpBlock.bullet("Pick an enabled class at the class selector before entering class-locked content."),
            HelpBlock.bullet("Stay with your party and watch chat, UI prompts, and in-world objectives."),
            HelpBlock.bullet("Currency is stored in your account balance, not carried as loose coins after pickup."),
            HelpBlock.bullet("Use dungeon rewards, vendors, and player trading to build toward the next unlock."),
            HelpBlock.tip("Esc closes this screen without pausing the game.")));

    public static final Page DUNGEONS = page("dungeons", "Dungeons", true, List.of(
            HelpBlock.heading("Dungeon Runs"),
            HelpBlock.bullet("Dungeon runs begin through the class selector flow."),
            HelpBlock.bullet("The first player to ready up becomes Group Leader for that run."),
            HelpBlock.bullet("The active run party is used for rewards, teleportation, AFK handling, and completion checks."),
            HelpBlock.bullet("Follow objectives, collect key progression items, and avoid leaving party members behind."),
            HelpBlock.bullet("Hostile mobs spawned by Cosmic Mob Spawners can feed Group Split Trace rewards."),
            HelpBlock.tip("If a route, door, or objective gives feedback in chat, treat that message as the current dungeon instruction.")));

    public static final Page CLASSES = page("classes", "Classes", true, List.of(
            HelpBlock.heading("Class Index"),
            HelpBlock.paragraph("Your class controls class-locked chests and class-attuned weapons, tools, armor, and special gear."),
            HelpBlock.bullet("Class-attuned weapons, tools, and armor are restricted by the server to the matching class."),
            HelpBlock.bullet("Dungeon, tier, and Trace details on gear do not change which class can use it."),
            HelpBlock.bullet("Plant Flags and class-attuned banners are special dungeon items and should remain placeable where the dungeon expects them."),
            HelpBlock.spacer(),
            HelpBlock.heading("Enabled Classes"),
            HelpBlock.bullet("Theurgist, Pyroclast, Bogatyr, Dragoon, Venefex, and Judicator."),
            HelpBlock.heading("Dungeon 2 Classes"),
            HelpBlock.bullet("Metalmancer and Deadeye are disabled for now and arrive in Dungeon 2."),
            HelpBlock.tip("Select a class name in the left index for a quick role page.")));

    public static final Page PARTY = page("party", "Party", true, List.of(
            HelpBlock.heading("Party Flow"),
            HelpBlock.bullet("The first dungeoneer to ready up in the class selector becomes Group Leader."),
            HelpBlock.bullet("Group Leader can remove another active run member when needed."),
            HelpBlock.command("Group Leader", "/dungeoneer kick <player>"),
            HelpBlock.heading("AFK"),
            HelpBlock.bullet("After 15 minutes without observed activity, a run member is marked AFK."),
            HelpBlock.bullet("AFK members stop receiving Group Split until activity resumes."),
            HelpBlock.bullet("The Group Leader receives a clickable kick prompt for AFK members."),
            HelpBlock.heading("Group Split"),
            HelpBlock.bullet("Eligible nearby active party members share Trace from qualifying dungeon mobs."),
            HelpBlock.bullet("Eligibility requires active run membership, same world, within 100 blocks, online, non-spectator, and not AFK.")));

    public static final Page TRADING = page("trading", "Trading", true, List.of(
            HelpBlock.heading("Player Trading"),
            HelpBlock.command("Request", "/trade <player>"),
            HelpBlock.command("Accept", "/trade accept <player>"),
            HelpBlock.command("Deny", "/trade deny <player>"),
            HelpBlock.command("Cancel", "/trade cancel"),
            HelpBlock.bullet("The Trade Request keybind defaults to CAPS LOCK."),
            HelpBlock.bullet("Looking at a nearby player can show the trade prompt and send an invite."),
            HelpBlock.bullet("Trade invites expire after 30 seconds."),
            HelpBlock.bullet("The first accept locks that player's offer; both players readying or accepting finalizes the trade."),
            HelpBlock.bullet("Denying or canceling returns items and cancels currency offers."),
            HelpBlock.tip("The server validates items, balances, inventory capacity, and trade state before anything changes hands.")));

    public static final Page VENDORS = page("vendors", "Vendors", true, List.of(
            HelpBlock.heading("Vendor Shops"),
            HelpBlock.bullet("Right-click assigned vendor NPCs to open their shop."),
            HelpBlock.bullet("Locked vendors show the reason instead of opening."),
            HelpBlock.bullet("The Vendor Selling pane shows item, name, cost, and Buy controls."),
            HelpBlock.bullet("The Player Inventory pane shows inventory items that vendor can buy."),
            HelpBlock.bullet("Use Sell Selected or Sell All to sell eligible items."),
            HelpBlock.bullet("Your currency balance updates after completed transactions."),
            HelpBlock.bullet("Costs display in the vendor's configured denominations."),
            HelpBlock.bullet("Naton Whitlock offers general supplies: food, fire, light, and container basics."),
            HelpBlock.bullet("Elias Centvin sells weapons, tools, and Dragoon repair materials; direct shop repair service is future-only."),
            HelpBlock.bullet("Eon Penrose runs the Brewing Store for ingredients, bottles, equipment, and potions."),
            HelpBlock.bullet("Beatrix Farrow provides food, Beatrix's Campfire, and Farrow's Chop for a safe return home."),
            HelpBlock.bullet("See Brewing Store, Travel, and Village Souls for the short field guide."),
            HelpBlock.tip("Purchases and sales are server-authoritative and atomic: they either complete safely or do not complete.")));


    public static final Page BREWING_STORE = page("brewing_store", "Brewing Store", true, List.of(
            HelpBlock.heading("Eon Penrose"),
            HelpBlock.paragraph("Eon Penrose sells brewing ingredients, equipment, and potions in the restored Village."),
            HelpBlock.heading("Live Dungeon 1 Stock"),
            HelpBlock.bullet("General: brewing ingredients, Glass Bottles, Night Vision, and Fire Resistance."),
            HelpBlock.bullet("Theurgist only: Brewing Stand, Cauldron, Healing II, Regeneration, and Lingering Healing."),
            HelpBlock.bullet("Theurgist or Judicator: Healing and Splash Healing."),
            HelpBlock.bullet("Dungeon 2 potion stock is planned for later and is not live in Dungeon 1."),
            HelpBlock.tip("If a row is locked, check your class and Village progression before buying.")));

    public static final Page VILLAGE_SOULS = page("village_souls", "Village NPCs", true, List.of(
            HelpBlock.heading("Village NPC Lore"),
            HelpBlock.paragraph("These pages collect the dungeoneer-facing backstories of the Village NPCs and story figures tied to Dungeon 1."),
            HelpBlock.bullet("Naton Whitlock: general supplies, food, fire, light, and container basics."),
            HelpBlock.bullet("Elias Centvin: weapons, tools, Dragoon repair materials, and salvage craft; direct shop repair service is future-only."),
            HelpBlock.bullet("Eon Penrose: brewing ingredients, equipment, and potion work."),
            HelpBlock.bullet("Beatrix Farrow: food, campfire cooking, Farrow's Chop, and the memory of home."),
            HelpBlock.bullet("Tamsin Vane: starting-area treasure hunter who found a JHW map to Base Camp; her deeper broker/payment systems are future-only until presented in-game."),
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

    public static final Page TAMSIN_VANE = lorePage("npc.tamsin_vane", "Tamsin Vane", "Dungeon Entry Broker",
            "Tamsin Vane is a living surface-side treasure hunter who discovered a map to the Base Camp and now sells access to nearby adventurers. Her map lore can frame entry, but map interfaces, dungeon broker UI, payment flows, and queue systems remain future-only unless you see them implemented in-game.", tamsinBackstory(), tamsinNotes());

    public static final Page JOHN_HAMISH_WATSON = lorePage("npc.john_hamish_watson", "John Hamish Watson", "Quest Giver",
            "John Hamish Watson introduces the abandoned Base Camp, the six spectral blooms, and the first major dungeon recovery objective. The six spectral blooms are unique placed dungeon bloom items tied to Watson's spiritual release; Lesser Blooms are separate restoration collectibles for side progression, vendor access, achievements, and unlocks.", watsonBackstory(), watsonNotes());

    public static final Page CURRENCY = page("currency", "Currency", true, List.of(
            HelpBlock.heading("Attunement Fragments"),
            HelpBlock.paragraph("Trace is stabilized attunement residue recovered from dungeon-bound beings and valued by freed NPCs as protection against future binding."),
            HelpBlock.bullet("Your Attunement Fragment currency is stored in an account balance."),
            HelpBlock.bullet("Trace = 1, Mark = 10, Seal = 100, Crown = 1,000, Anchor = 10,000."),
            HelpBlock.bullet("Your First Trace onboarding reward grants 5 Trace."),
            HelpBlock.bullet("Trace is used for purchases, upgrades, trades, and dungeon rewards."),
            HelpBlock.bullet("Picking up currency deposits it into your account balance."),
            HelpBlock.bullet("Pickup is all-or-nothing if the deposit would exceed account capacity."),
            HelpBlock.bullet("Vendors and trades use the same balance."),
            HelpBlock.command("Balance", "/currency balance"),
            HelpBlock.command("Held Value", "/currency value"),
            HelpBlock.command("Inventory Value", "/currency value inventory")));

    public static final Page PROGRESSION = page("progression", "Progression", true, List.of(
            HelpBlock.heading("Dungeon 1 Progress"),
            HelpBlock.bullet("Lesser Blooms are the main Dungeon 1 flower collectible."),
            HelpBlock.bullet("Cavern Residue is a progression material used by current and future systems."),
            HelpBlock.bullet("Dungeon 1 completion currently expects at least 3 Lesser Blooms."),
            HelpBlock.bullet("Village access unlocks from Dungeon 1 completion."),
            HelpBlock.bullet("Watson's Base Camp story points toward spectral blooms, but your active Dungeon 1 instructions currently follow Lesser Bloom completion checks."),
            HelpBlock.bullet("Dungeon 1 NPC unlock tiers derive from cumulative Lesser Blooms: tier 1 at 5, tier 2 at 10, tier 3 at 15, and tier 4 at 20."),
            HelpBlock.command("Read Progress", "/progression get <your_name>"),
            HelpBlock.tip("Progression commands listed here are read-only for your own character unless you have elevated access.")));

    public static final Page FACTIONS = page("factions", "Factions", true, List.of(
            HelpBlock.heading("Faction Standing"),
            HelpBlock.bullet("Faction tracks long-term alignment and conduct with faction groups."),
            HelpBlock.bullet("Tiers are Hostile, Suspicious, Indifferent, Cordial, Favorable, Warmly, and Ally."),
            HelpBlock.bullet("Faction standing can be checked in-game when a faction is visible to you."),
            HelpBlock.bullet("Positive and negative actions may change faction over time where the dungeon has configured them."),
            HelpBlock.bullet("Hostile status can restrict access, but recovery paths may exist by faction."),
            HelpBlock.command("List", "/faction list <your_name>"),
            HelpBlock.command("Get", "/faction get <your_name> <faction_id>"),
            HelpBlock.tip("Exact value ranges can vary by faction; rely on the tier shown to you in-game.")));

    public static final Page TELEPORTATION = page("teleportation", "Travel", true, List.of(
            HelpBlock.heading("Potion of Companionship"),
            HelpBlock.bullet("Only works during an active dungeon run."),
            HelpBlock.bullet("Opens Teleport to a Dungeoneer with online companions in the same active run, excluding you."),
            HelpBlock.bullet("Choosing a companion teleports you to that dungeoneer."),
            HelpBlock.bullet("Successful teleport applies a visible 5-minute Teleport Cooldown."),
            HelpBlock.bullet("The potion is not consumed when the attempt is invalid or you are cooling down."),
            HelpBlock.heading("Rifts"),
            HelpBlock.bullet("Rifts are world-travel structures when presented by the dungeon."),
            HelpBlock.heading("Farrow's Chop"),
            HelpBlock.bullet("Beatrix's Campfire cooks Raw Farrow's Chop into Farrow's Chop."),
            HelpBlock.bullet("Eating Farrow's Chop returns you to the Main Village when the route is available and safe."),
            HelpBlock.bullet("The chop is consumed only after a successful return, unless creative-mode rules preserve it."),
            HelpBlock.bullet("A successful return grants the Nostalgia Bait achievement."),
            HelpBlock.tip("Only use teleport options offered by the dungeon, a potion, visible in-world travel structures, or Farrow's Chop.")));

    public static final Page ACHIEVEMENTS = page("achievements", "Achievements", true, List.of(
            HelpBlock.heading("Milestones"),
            HelpBlock.bullet("Achievements are server-side advancements that reward notable dungeon milestones."),
            HelpBlock.bullet("First Trace grants 5 Trace and explains currency abbreviations and value."),
            HelpBlock.bullet("Handshake Protocol is awarded after your first successful player trade."),
            HelpBlock.bullet("Nostalgia Bait is live: return to the Main Village with Farrow's Chop."),
            HelpBlock.bullet("The Tamsin Tax is coming soon: repay Tamsin after your first dungeon once a payment interface exists."),
            HelpBlock.bullet("Plant Flags tracks class-attuned banner planting during Dungeon 1 where configured."),
            HelpBlock.bullet("Binding Idol and Vital Exchange support dungeon progression and support-role milestone hooks where relevant."),
            HelpBlock.tip("Achievement popups are reminders of progress; server checks decide when they are earned.")));

    public static final Page REFERENCES = page("references", "References", true, List.of(
            HelpBlock.heading("Quick Reference"),
            HelpBlock.command("Help", "H - Open Cosmic Dungeon Help"),
            HelpBlock.command("Trade Key", "CAPS LOCK - Trade Request keybind"),
            HelpBlock.command("Trade", "/trade <player>"),
            HelpBlock.command("Accept", "/trade accept <player>"),
            HelpBlock.command("Deny", "/trade deny <player>"),
            HelpBlock.command("Cancel", "/trade cancel"),
            HelpBlock.command("Balance", "/currency balance"),
            HelpBlock.command("Held Value", "/currency value"),
            HelpBlock.command("Inventory Value", "/currency value inventory"),
            HelpBlock.command("Progress", "/progression get <your_name>"),
            HelpBlock.command("Factions", "/faction list <your_name>"),
            HelpBlock.command("Faction", "/faction get <your_name> <faction_id>"),
            HelpBlock.command("Group Leader", "/dungeoneer kick <player>"),
            HelpBlock.tip("Developer, operator, debug, and world-authoring commands are intentionally not listed here.")));

    public static final Page THEURGIST = page("class.theurgist", "Theurgist", true, List.of(
            HelpBlock.heading("Theurgist"),
            HelpBlock.paragraph("Once a battlefield medic turned biochemist, the Theurgist draws on care, chemistry, and the otherworldly to mend what others consider beyond saving."),
            HelpBlock.bullet("Role theme: healing and potion-brewing support."),
            HelpBlock.bullet("Only Theurgists can use brewing stands."),
            HelpBlock.bullet("Valid potion brews complete instantly."),
            HelpBlock.bullet("Eon Penrose sells Theurgist-only brewing equipment and advanced healing/regeneration potions."),
            HelpBlock.bullet("Can use Theurgist-attuned gear and class chests.")));

    public static final Page PYROCLAST = page("class.pyroclast", "Pyroclast", true, List.of(
            HelpBlock.heading("Pyroclast"),
            HelpBlock.paragraph("A former pyrotechnician and demolition hobbyist, the Pyroclast brings fire, fireworks, and controlled chaos to dungeon fights."),
            HelpBlock.bullet("Craft 1 gravel and 1 flint into 1 gunpowder in any crafting shape."),
            HelpBlock.bullet("A pickup reminder can appear while carrying matching materials."),
            HelpBlock.bullet("Crafting this gunpowder grants the Boom! achievement."),
            HelpBlock.bullet("Can use Pyroclast-attuned gear and class chests.")));

    public static final Page BOGATYR = page("class.bogatyr", "Bogatyr", true, List.of(
            HelpBlock.heading("Bogatyr"),
            HelpBlock.paragraph("A survivalist, cold-weather endurance racer, and hardened wolf-handler from the frozen Russian Federation reaches, the Bogatyr favors traditional weaponry and grit."),
            HelpBlock.bullet("Damage identity: fights alongside loyal wolf companions as a class role theme."),
            HelpBlock.bullet("Can use Bogatyr-attuned gear and class chests.")));

    public static final Page DRAGOON = page("class.dragoon", "Dragoon", true, List.of(
            HelpBlock.heading("Dragoon"),
            HelpBlock.paragraph("A trident-wielding specialist in battlefield salvage and historical armorcraft."),
            HelpBlock.bullet("Dragoon is the anvil and repair-support class."),
            HelpBlock.bullet("Dragoons can use vanilla anvils."),
            HelpBlock.bullet("Repair Affinity lets a Dragoon repair another player's supported damaged gear while the customer keeps item ownership."),
            HelpBlock.bullet("The customer may offer an optional account-currency labor fee; the Dragoon supplies Elias repair materials."),
            HelpBlock.bullet("Both players must stay nearby with the interface open. Commands: /repair <player>, /repair accept <player>, /repair deny <player>, /repair cancel."),
            HelpBlock.bullet("Dragoon passive: 3% chance to trigger chain lightning."),
            HelpBlock.bullet("Can use Dragoon-attuned gear and class chests.")));

    public static final Page VENEFEX = page("class.venefex", "Venefex", true, List.of(
            HelpBlock.heading("Venefex"),
            HelpBlock.paragraph("A toxicologist shaped by invasive-species decay, the Venefex leans into poison, debuffs, and attrition."),
            HelpBlock.bullet("Damage identity: pressure enemies through toxic and weakening themes."),
            HelpBlock.bullet("Can use Venefex-attuned gear and class chests.")));

    public static final Page JUDICATOR = page("class.judicator", "Judicator", true, List.of(
            HelpBlock.heading("Judicator"),
            HelpBlock.paragraph("A wandering arbiter sworn to uphold balance."),
            HelpBlock.bullet("Role theme: slaying undead foes and protecting the party."),
            HelpBlock.bullet("Can use Judicator-attuned gear and class chests.")));

    public static final Page METALMANCER = page("class.metalmancer", "Metalmancer", false, List.of(
            HelpBlock.heading("Coming in Dungeon 2"),
            HelpBlock.paragraph("Metalmancer is disabled.")));

    public static final Page DEADEYE = page("class.deadeye", "Deadeye", false, List.of(
            HelpBlock.heading("Coming in Dungeon 2"),
            HelpBlock.paragraph("Deadeye is disabled.")));

    public static final List<Page> PAGES = List.of(GET_STARTED, DUNGEONS, CLASSES, THEURGIST, PYROCLAST, BOGATYR, DRAGOON, VENEFEX, JUDICATOR, METALMANCER, DEADEYE, PARTY, TRADING, VENDORS, BREWING_STORE, VILLAGE_SOULS, NATON_WHITLOCK, ELIAS_CENTVIN, EON_PENROSE, BEATRIX_FARROW, TAMSIN_VANE, JOHN_HAMISH_WATSON, CURRENCY, PROGRESSION, FACTIONS, TELEPORTATION, ACHIEVEMENTS, REFERENCES);



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
        return page(id, title, true, List.copyOf(blocks));
    }

    private HelpMenuContent() {}

    private static Page page(String id, String title, boolean enabled, List<HelpBlock> blocks) { return new Page(id, Component.literal(title), enabled, blocks); }
    public record Page(String id, Component title, boolean enabled, List<HelpBlock> blocks) {}
    public record HelpBlock(Kind kind, Component label, Component text) {
        static HelpBlock heading(String text) { return new HelpBlock(Kind.HEADING, Component.empty(), Component.literal(text)); }
        static HelpBlock paragraph(String text) { return new HelpBlock(Kind.PARAGRAPH, Component.empty(), Component.literal(text)); }
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
    public enum Kind { HEADING, PARAGRAPH, BULLET, COMMAND, TIP, SPACER }
}
