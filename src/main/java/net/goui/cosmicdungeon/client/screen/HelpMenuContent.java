package net.goui.cosmicdungeon.client.screen;

import net.minecraft.network.chat.Component;

import java.util.List;

public final class HelpMenuContent {
    public static final Page GET_STARTED = page("get_started", "Get Started", true, List.of(
            HelpBlock.heading("Welcome"),
            HelpBlock.paragraph("Press H to open or close this help menu. Use the mouse wheel over the left index or right page to scroll."),
            HelpBlock.bullet("Choose a class from a class selector before entering class-locked areas."),
            HelpBlock.bullet("Trade, vendor shops, currencies, factions, achievements, rifts, and dungeon access are handled by server-authoritative systems."),
            HelpBlock.tip("Esc closes this screen without pausing the game.")));
    public static final Page DUNGEONS = page("dungeons", "Dungeons", true, List.of(
            HelpBlock.heading("Dungeon Runs"), HelpBlock.paragraph("Cosmic Dungeon content is designed around party exploration, class roles, keys, doors, rifts, and progression locks."),
            HelpBlock.bullet("Follow in-world prompts and unlocked routes."), HelpBlock.bullet("Door keys and access rules are checked by the server.")));
    public static final Page CLASSES = page("classes", "Classes", true, List.of(
            HelpBlock.heading("Class Index"), HelpBlock.paragraph("Select a class in the left index to read a short player-facing summary. Disabled classes remain visible for future Dungeon 2 content.")));
    public static final Page PARTY = page("party", "Party", true, List.of(HelpBlock.heading("Party Play"), HelpBlock.paragraph("Coordinate roles before starting a dungeon. Some systems can react to grouped players and shared dungeon progress.")));
    public static final Page TRADING = page("trading", "Trading", true, List.of(HelpBlock.heading("Player Trading"), HelpBlock.paragraph("Look at a nearby player and use the trade request keybind to request a trade when trading is available.")));
    public static final Page VENDORS = page("vendors", "Vendors", true, List.of(HelpBlock.heading("Vendors"), HelpBlock.paragraph("Vendor screens show offers, costs, locked status, and sell options. Unlocks and purchases are validated by the server.")));
    public static final Page CURRENCY = page("currency", "Currency", true, List.of(HelpBlock.heading("Currency"), HelpBlock.paragraph("Cosmic Dungeon uses server-managed currency values for rewards, vendor costs, and progression economies.")));
    public static final Page PROGRESSION = page("progression", "Progression", true, List.of(HelpBlock.heading("Progression"), HelpBlock.paragraph("Progression locks can control access to regions, vendors, dungeons, and rewards. Your client help menu never grants access by itself.")));
    public static final Page FACTIONS = page("factions", "Factions", true, List.of(HelpBlock.heading("Factions"), HelpBlock.paragraph("Faction progress can unlock content where configured by the server or world.")));
    public static final Page TELEPORTATION = page("teleportation", "Teleportation", true, List.of(HelpBlock.heading("Teleportation"), HelpBlock.paragraph("Teleportation features, including companionship travel, are handled by configured in-world mechanics and server checks.")));
    public static final Page ACHIEVEMENTS = page("achievements", "Achievements", true, List.of(HelpBlock.heading("Achievements"), HelpBlock.paragraph("Achievements and advancements record notable dungeon milestones such as trading and special discoveries.")));
    public static final Page REFERENCES = page("references", "References", true, List.of(HelpBlock.heading("Player Commands"), HelpBlock.command("Keybind", "H - Open or close Cosmic Dungeon Help"), HelpBlock.tip("Operator and world-authoring commands are intentionally not listed in this in-game player help.")));

    public static final Page THEURGIST = classPage("theurgist", "Theurgist", true, "Supportive magic user focused on helping the party survive dangerous encounters.");
    public static final Page PYROCLAST = classPage("pyroclast", "Pyroclast", true, "Fire-focused combatant built around pressure and destructive output.");
    public static final Page BOGATYR = classPage("bogatyr", "Bogatyr", true, "Stalwart front-line warrior who leans into resilience and direct combat.");
    public static final Page DRAGOON = classPage("dragoon", "Dragoon", true, "Mobile spear fighter with a distinct martial identity and dungeon role.");
    public static final Page VENEFEX = classPage("venefex", "Venefex", true, "Venomous or curse-leaning combat style for players who prefer attrition and control.");
    public static final Page JUDICATOR = classPage("judicator", "Judicator", true, "Oathbound fighter themed around judgment, shields, standards, and disciplined pressure.");
    public static final Page METALMANCER = page("class.metalmancer", "Metalmancer", false, List.of(HelpBlock.heading("Coming in Dungeon 2"), HelpBlock.paragraph("Metalmancer is visible in the class index but is not enabled in this help menu.")));
    public static final Page DEADEYE = page("class.deadeye", "Deadeye", false, List.of(HelpBlock.heading("Coming in Dungeon 2"), HelpBlock.paragraph("Deadeye is visible in the class index but is not enabled in this help menu.")));

    public static final List<Page> PAGES = List.of(GET_STARTED, DUNGEONS, CLASSES, THEURGIST, PYROCLAST, BOGATYR, DRAGOON, VENEFEX, JUDICATOR, METALMANCER, DEADEYE, PARTY, TRADING, VENDORS, CURRENCY, PROGRESSION, FACTIONS, TELEPORTATION, ACHIEVEMENTS, REFERENCES);

    private HelpMenuContent() {}
    private static Page classPage(String id, String title, boolean enabled, String summary) { return page("class." + id, title, enabled, List.of(HelpBlock.heading("Role"), HelpBlock.paragraph(summary), HelpBlock.tip("Use the class selector to choose enabled classes."))); }
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
