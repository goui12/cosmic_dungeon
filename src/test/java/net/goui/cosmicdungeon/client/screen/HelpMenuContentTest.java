package net.goui.cosmicdungeon.client.screen;

import java.util.*;
import net.goui.cosmicdungeon.client.SkillInputPolicy;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HelpMenuContentTest {
    private static List<HelpMenuContent.Page> pages() {
        var result = new ArrayList<HelpMenuContent.Page>();
        collect(HelpMenuContent.ROOT, result);
        return result;
    }
    private static void collect(HelpMenuContent.HelpNode node, List<HelpMenuContent.Page> result) {
        result.add(node.page());
        node.children().forEach(child -> collect(child, result));
    }
    private static List<Component> components(Component text) {
        var result = new ArrayList<Component>();
        result.add(text);
        text.getSiblings().forEach(child -> result.addAll(components(child)));
        return result;
    }
    @Test void internalLinksAndBindingsResolveWithoutCommandsOrExternalActions() {
        var pages = pages();
        var ids = new HashSet<String>();
        pages.forEach(page -> assertTrue(ids.add(page.id()), "Duplicate page " + page.id()));
        assertEquals(33, pages.size());
        assertFalse(ids.contains("class.metalmancer"));
        assertFalse(ids.contains("class.deadeye"));
        int links = 0, keys = 0;
        for (var page : pages) for (var block : page.blocks()) {
            for (var text : components(block.text())) {
                var style = text.getStyle();
                assertNull(style.getClickEvent(), "Guide text must never dispatch commands or URLs");
                String insertion = style.getInsertion();
                if (insertion == null) continue;
                assertTrue(style.isUnderlined());
                if (insertion.startsWith("cosmic_help:")) {
                    assertTrue(ids.contains(insertion.substring("cosmic_help:".length())), insertion);
                    links++;
                } else if (insertion.startsWith("cosmic_key:")) {
                    assertNotNull(SkillInputPolicy.Action.forId(insertion.substring("cosmic_key:".length())), insertion);
                    keys++;
                } else fail("Unknown guide metadata: " + insertion);
            }
        }
        assertTrue(links >= 20);
        assertEquals(8, keys);
    }
    @Test void itemTopicsAreBoundedAndLoreRemainsComplete() {
        Set<String> supported = Set.of("healing_potions", "harming_potions", "turtle_master",
                "brewing_fuel", "bogatyr_wolves", "dragoon_trident", "food", "key");
        Set<String> used = new HashSet<>();
        for (var page : pages()) for (var block : page.blocks())
            if (block.kind() == HelpMenuContent.Kind.ITEMS) used.add(block.text().getString());
        assertEquals(supported, used);
        for (var page : List.of(HelpMenuContent.NATON_WHITLOCK, HelpMenuContent.ELIAS_CENTVIN,
                HelpMenuContent.EON_PENROSE, HelpMenuContent.BEATRIX_FARROW,
                HelpMenuContent.TAMSIN_VANE, HelpMenuContent.JOHN_HAMISH_WATSON)) {
            String text = page.blocks().stream().map(b -> b.text().getString()).reduce("", String::concat);
            assertTrue(text.contains("Backstory"));
            assertTrue(text.contains("Lore Notes"));
            assertTrue(text.length() > 2000, page.id());
        }
    }
}
