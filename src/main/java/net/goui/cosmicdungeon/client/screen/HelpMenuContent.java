package net.goui.cosmicdungeon.client.screen;

import net.minecraft.network.chat.Component;

public final class HelpMenuContent {
    public static final int CLASS_PAGE_SIZE = 4;

    public static final Page MAIN = new Page("screen.cosmicdungeon.help.main", "screen.cosmicdungeon.help.main.body");
    public static final Page GET_STARTED = new Page("screen.cosmicdungeon.help.get_started", "screen.cosmicdungeon.help.get_started.body");
    public static final Page DUNGEONS = new Page("screen.cosmicdungeon.help.dungeons", "screen.cosmicdungeon.help.dungeons.body");
    public static final Page CLASSES = new Page("screen.cosmicdungeon.help.classes", "");
    public static final Page CODEX = new Page("screen.cosmicdungeon.help.codex", "screen.cosmicdungeon.help.codex.body");

    public static final Page[] MAIN_CHILDREN = {GET_STARTED, DUNGEONS, CLASSES, CODEX};
    public static final ClassEntry[] CLASS_ENTRIES = {
            classEntry("theurgist", true),
            classEntry("pyroclast", true),
            classEntry("bogatyr", true),
            classEntry("dragoon", true),
            classEntry("venefex", true),
            classEntry("judicator", true),
            classEntry("metalmancer", false),
            classEntry("deadeye", false),
            comingSoon(),
            comingSoon(),
            comingSoon(),
            comingSoon(),
            comingSoon(),
            comingSoon(),
            comingSoon(),
            comingSoon()
    };

    private HelpMenuContent() {}

    public static boolean isClassPage(Page page) {
        for (ClassEntry entry : CLASS_ENTRIES) {
            if (entry.page() == page) {
                return true;
            }
        }
        return false;
    }

    private static ClassEntry classEntry(String id, boolean enabled) {
        return new ClassEntry(
                new Page("screen.cosmicdungeon.help.class." + id, "screen.cosmicdungeon.help.class." + id + ".body"),
                enabled
        );
    }

    private static ClassEntry comingSoon() {
        return new ClassEntry(new Page("screen.cosmicdungeon.help.class.coming_soon", ""), false);
    }

    public record Page(String titleKey, String bodyKey) {
        public String title() {
            return Component.translatable(titleKey).getString();
        }

        public String body() {
            return bodyKey.isBlank() ? "" : Component.translatable(bodyKey).getString();
        }
    }

    public record ClassEntry(Page page, boolean enabled) {}
}
