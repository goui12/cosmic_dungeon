package net.goui.cosmicdungeon.client.screen;

import java.util.List;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClassResourceHelpTest {
    @Test void feedPackListsExactlyResolvedServerFoodsWithoutReplacingLore() {
        var page = HelpMenuContent.BOGATYR;
        var before = page.blocks();
        var name = Component.literal("Server wolf food");
        var blocks = ClassResourceHelp.assemble(page, List.of(Component.literal("Bone"), name));
        assertSame(before, page.blocks());
        assertEquals(before, blocks.subList(0, before.size()));
        var tail = blocks.subList(before.size(), blocks.size());
        assertEquals(List.of("Bone", "Server wolf food"), tail.stream()
                .filter(b -> b.kind() == HelpMenuContent.Kind.BULLET).map(b -> b.text().getString()).toList());
        name.append(" changed");
        assertEquals("Server wolf food", blocks.getLast().text().getString());
        assertThrows(UnsupportedOperationException.class, () -> blocks.clear());
        String text = tail.stream().map(b -> b.text().getString()).reduce("", String::concat);
        assertFalse(text.contains("Request Supplies"));
        assertFalse(text.contains("1 per second"));
        assertFalse(text.contains("Summon spends"));
    }

    @Test void retiredResourcesCannotInjectTheurgistInstructions() {
        var page = HelpMenuContent.THEURGIST;
        assertSame(page.blocks(), ClassResourceHelp.assemble(page, List.of(Component.literal("Legacy herb"))));
        assertSame(page.blocks(), ClassResourceHelp.blocks(page));
        String text = page.blocks().stream().map(b -> b.text().getString()).reduce("", String::concat);
        assertTrue(text.contains("180-second cooldown"));
        assertTrue(text.contains("20 brewing steps"));
        assertFalse(text.contains("spends 120"));
        assertFalse(text.contains("one random"));
        assertFalse(text.contains("No bottles, fuel or stand"));
    }
}
