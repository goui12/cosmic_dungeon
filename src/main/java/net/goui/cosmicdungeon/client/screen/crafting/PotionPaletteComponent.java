package net.goui.cosmicdungeon.client.screen.crafting;

import java.util.*;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;
import net.goui.cosmicdungeon.network.PotionCraftPayloads.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Native potion names/effects/advanced tooltips, with availability appended rather than simulated items. */
public final class PotionPaletteComponent {
    private PotionPaletteComponent() {}
    private static void fill(GuiGraphics g, Rect box, int color) { g.fill(box.x(), box.y(), box.right(), box.bottom(), color); }
    public static List<Component> details(Entry entry, PotionPaletteModel.Variant selected, boolean pending) {
        var lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), entry.output()));
        lines.add(Component.literal(PotionPaletteModel.source(entry)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Own materials: " + entry.own() + " | With group: " + entry.shared()).withStyle(ChatFormatting.GRAY));
        if (PotionPaletteModel.variant(entry.targetId()) != selected)
            lines.add(Component.literal("This effect has no " + selected.description.toLowerCase(Locale.ROOT) + " variant; showing its normal recipe.").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Fuel: " + entry.steps() + " steps; " + entry.powders() + " new blaze powder.").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal(pending ? "Waiting for the server." : "Click: craft one onto the cursor. Shift-click: one into inventory, hotbar first.").withStyle(ChatFormatting.YELLOW));
        return lines;
    }
    public static void draw(GuiGraphics g, PotionPaletteLayout layout, PotionPaletteState state, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font; var panel = layout.panel();
        g.nextStratum(); fill(g, panel, 0xF017202B);
        g.fill(panel.x(), panel.y(), panel.right(), panel.y() + 1, 0xFF8095A9);
        g.drawString(font, "Craft", panel.x() + 3, panel.y() + 4, 0xFFF3F5F8, false);
        for (int i = 0; i < 3; i++) {
            var variant = PotionPaletteModel.Variant.values()[i]; var box = layout.variants().get(i);
            fill(g, box, state.variant() == variant ? 0xFF426A86 : 0xFF303B48);
            g.drawCenteredString(font, variant.label, box.x() + box.width() / 2, box.y() + 4, 0xFFFFFFFF);
            if (box.contains(mouseX, mouseY)) g.setTooltipForNextFrame(font, Component.literal(variant.description), mouseX, mouseY);
        }
        var fuel = layout.fuel(); fill(g, fuel, 0xFF10151C);
        int charges = state.view().charges(), capacity = Math.max(1, state.view().perPowder());
        int height = Math.clamp(charges * Math.max(0, fuel.height() - 2) / capacity, 0, Math.max(0, fuel.height() - 2));
        g.fill(fuel.x() + 1, fuel.bottom() - 1 - height, fuel.right() - 1, fuel.bottom() - 1, 0xFFFFB238);
        if (fuel.contains(mouseX, mouseY))
            g.setTooltipForNextFrame(font, Component.literal("Brewing fuel: " + charges + " / " + capacity + " remaining charges. One blaze powder supplies 20 brewing steps; unused charges are preserved."), mouseX, mouseY);
        var entries = state.entries();
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i); var box = layout.tiles().get(i);
            fill(g, box, PotionPaletteModel.background(entry)); g.renderItem(entry.output(), box.x() + 1, box.y() + 1);
            int count = PotionPaletteModel.count(entry); String text = count > 99 ? "99+" : Integer.toString(count);
            g.drawString(font, text, box.right() - font.width(text), box.bottom() - 8, PotionPaletteModel.color(entry), true);
            if (box.contains(mouseX, mouseY))
                g.setTooltipForNextFrame(font, details(entry, state.variant(), state.pending()), entry.output().getTooltipImage(), entry.output(), mouseX, mouseY);
        }
        if (entries.isEmpty() && panel.contains(mouseX, mouseY))
            g.setTooltipForNextFrame(font, Component.literal("Crafting is available to living Theurgists and Venefex in an active dungeon. Waiting for eligible recipes."), mouseX, mouseY);
    }
}
