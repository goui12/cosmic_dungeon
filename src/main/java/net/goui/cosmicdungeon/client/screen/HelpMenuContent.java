package net.goui.cosmicdungeon.client.screen;

import net.minecraft.network.chat.Component;

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
            HelpBlock.bullet("Elias Centvin is a Dungeon 1 weapon and tool vendor unlocked through village and NPC progression."),
            HelpBlock.tip("Purchases and sales are server-authoritative and atomic: they either complete safely or do not complete.")));

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
            HelpBlock.bullet("Dungeon 1 NPC unlock tiers derive from cumulative Lesser Blooms: tier 1 at 5, tier 2 at 10, tier 3 at 15, and tier 4 at 20."),
            HelpBlock.command("Read Progress", "/progression get <your_name>"),
            HelpBlock.tip("Progression commands listed here are read-only for your own character unless you have elevated access.")));

    public static final Page FACTIONS = page("factions", "Factions", true, List.of(
            HelpBlock.heading("Faction Standing"),
            HelpBlock.bullet("Faction tracks long-term alignment and conduct with faction groups."),
            HelpBlock.bullet("Tiers are Hostile, Suspicious, Indifferent, Cordial, Favorable, Warmly, and Ally."),
            HelpBlock.bullet("Faction may affect vendors, dialogue, quests, teleport access, NPC reactions, and settlement access where configured."),
            HelpBlock.bullet("Positive and negative actions can change faction over time."),
            HelpBlock.bullet("Hostile status can restrict access, but recovery paths may exist by faction."),
            HelpBlock.command("List", "/faction list <your_name>"),
            HelpBlock.command("Get", "/faction get <your_name> <faction_id>"),
            HelpBlock.tip("Exact value ranges can vary by faction; rely on the tier shown to you in-game.")));

    public static final Page TELEPORTATION = page("teleportation", "Teleportation", true, List.of(
            HelpBlock.heading("Potion of Companionship"),
            HelpBlock.bullet("Only works during an active dungeon run."),
            HelpBlock.bullet("Opens Teleport to a Dungeoneer with online companions in the same active run, excluding you."),
            HelpBlock.bullet("Choosing a companion teleports you to that dungeoneer."),
            HelpBlock.bullet("Successful teleport applies a visible 5-minute Teleport Cooldown."),
            HelpBlock.bullet("The potion is not consumed when the attempt is invalid or you are cooling down."),
            HelpBlock.heading("Rifts"),
            HelpBlock.bullet("Rifts are world-travel structures when presented by the dungeon."),
            HelpBlock.tip("Only use teleport options offered by the dungeon, a potion, or visible in-world travel structures.")));

    public static final Page ACHIEVEMENTS = page("achievements", "Achievements", true, List.of(
            HelpBlock.heading("Milestones"),
            HelpBlock.bullet("Achievements are server-side advancements that reward notable dungeon milestones."),
            HelpBlock.bullet("First Trace grants 5 Trace and explains currency abbreviations and value."),
            HelpBlock.bullet("Handshake Protocol is awarded after your first successful player trade."),
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
            HelpBlock.bullet("Party identity: major healing, resurrection, and potion-brewing support."),
            HelpBlock.bullet("Only Theurgists can use brewing stands."),
            HelpBlock.bullet("Valid potion brews complete instantly."),
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
            HelpBlock.bullet("Specializes in slaying undead foes and light healing support identity."),
            HelpBlock.bullet("Can use Judicator-attuned gear and class chests.")));

    public static final Page METALMANCER = page("class.metalmancer", "Metalmancer", false, List.of(
            HelpBlock.heading("Coming in Dungeon 2"),
            HelpBlock.paragraph("Metalmancer is disabled.")));

    public static final Page DEADEYE = page("class.deadeye", "Deadeye", false, List.of(
            HelpBlock.heading("Coming in Dungeon 2"),
            HelpBlock.paragraph("Deadeye is disabled.")));

    public static final List<Page> PAGES = List.of(GET_STARTED, DUNGEONS, CLASSES, THEURGIST, PYROCLAST, BOGATYR, DRAGOON, VENEFEX, JUDICATOR, METALMANCER, DEADEYE, PARTY, TRADING, VENDORS, CURRENCY, PROGRESSION, FACTIONS, TELEPORTATION, ACHIEVEMENTS, REFERENCES);

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
    }
    public enum Kind { HEADING, PARAGRAPH, BULLET, COMMAND, TIP, SPACER }
}
