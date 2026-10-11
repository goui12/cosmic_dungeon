package net.goui.cosmicdungeon.client.screen.skills;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SkillBindingLabelsTest {
    private static SkillBindingLabels.Layout layout(String title, String hint, int width) {
        return SkillBindingLabels.layout(title, hint, width, String::length,
                (text, limit) -> text.substring(0, Math.min(text.length(), Math.max(0, limit))));
    }
    @Test void nameAndLiveBindingStayAdjacentWhenThereIsRoom() {
        var row = layout("Feed Pack", "<Alt + 3>", 30);
        assertFalse(row.stacked());
        assertEquals("Feed Pack", row.title()); assertEquals("<Alt + 3>", row.hint());
        assertEquals(row.titleX() + row.title().length() + 4, row.hintX());
        assertTrue(row.hintX() + row.hint().length() <= 30);
        var rebound = layout("Feed Pack", "<Enter>", 30);
        assertEquals("<Enter>", rebound.hint());
        assertEquals("<Unbound>", layout("Feed Pack", "<Unbound>", 30).hint());
    }
    @Test void smallRowsPreserveSeparateClippedNameAndBindingLines() {
        for (int width : List.of(0, 1, 4, 8, 10, 17)) {
            var row = layout("Feed Pack", "<Alt + 3>", width);
            assertTrue(row.stacked());
            assertTrue(row.titleX() >= 0 && row.hintX() >= 0);
            assertTrue(row.titleX() + row.title().length() <= width);
            assertTrue(row.hintX() + row.hint().length() <= width);
            if (width > 0) { assertFalse(row.title().isEmpty()); assertFalse(row.hint().isEmpty()); }
        }
        var narrowButton = layout("Resurrect...", "<Alt + 4>", "Resurrect...".length());
        assertTrue(narrowButton.stacked());
        assertEquals("Resurrect...", narrowButton.title());
        assertEquals("<Alt + 4>", narrowButton.hint());
        var clippedButton = layout("Resurrect...", "<Alt + 4>", 11);
        assertTrue(clippedButton.stacked());
        assertEquals("Resurrect..", clippedButton.title());
        assertEquals("<Alt + 4>", clippedButton.hint());
        assertTrue(clippedButton.titleX() + clippedButton.title().length() <= 11);
        assertEquals("", layout("Feed Pack", "<Alt + 3>", -8).title());
    }
    @Test void passiveGuideDoesNotAcquireAnInventedBindingOrSecondLine() {
        var row = layout("Class guide", "", 18);
        assertFalse(row.stacked()); assertEquals("", row.hint());
        assertEquals(3, row.titleX());
        var tip = SkillBindingLabels.tooltip("Class guide", Component.literal("Learn the class."), Component.empty());
        assertEquals("Class guide\nLearn the class.", tip.getString());
        assertFalse(tip.getString().contains("Hotkey"));
    }
    @Test void fullDescriptionAndCurrentBindingSurviveClippingAndRebindingWithoutMutatingModel() {
        var description = Component.literal("Heal injured wolves, then breed healthy adults.").withStyle(ChatFormatting.AQUA);
        var sourceHint = Component.literal("<Alt + 3>").withStyle(ChatFormatting.RED);
        var tip = SkillBindingLabels.tooltip("Feed Pack", description, sourceHint);
        assertEquals("Feed Pack\nHeal injured wolves, then breed healthy adults.\nHotkey: <Alt + 3>", tip.getString());
        assertEquals(ChatFormatting.AQUA.getColor(), description.getStyle().getColor().getValue());
        assertEquals(ChatFormatting.RED.getColor(), sourceHint.getStyle().getColor().getValue());
        assertEquals(ChatFormatting.GRAY.getColor(), tip.getSiblings().getLast().getStyle().getColor().getValue());
        assertEquals("Heal injured wolves, then breed healthy adults.", description.getString());
        var rebound = SkillBindingLabels.tooltip("Feed Pack", description, Component.literal("<Mouse 4>"));
        assertTrue(rebound.getString().endsWith("Hotkey: <Mouse 4>"));
        assertFalse(rebound.getString().contains("Alt + 3"));
        var unbound = SkillBindingLabels.tooltip("Feed Pack", description, Component.literal("<Unbound>"));
        assertTrue(unbound.getString().endsWith("Hotkey: <Unbound>"));
    }
    @Test void existingActionTitleIsNotRepeatedByTooltipDecoration() {
        var description = Component.literal("Stay\nRecall and sit.").withStyle(ChatFormatting.WHITE);
        var tip = SkillBindingLabels.tooltip("Stay", description, Component.literal("<Alt + 2>"));
        assertEquals("Stay\nRecall and sit.\nHotkey: <Alt + 2>", tip.getString());
        assertEquals("Stay\nRecall and sit.", description.getString());
    }
}
