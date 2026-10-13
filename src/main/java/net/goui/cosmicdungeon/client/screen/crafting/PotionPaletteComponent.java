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
    private static void add(List<Component> lines, String text, ChatFormatting color) {
        var minecraft = Minecraft.getInstance();
        int width = Math.max(1, Math.min(300, minecraft.getWindow().getGuiScaledWidth() - 16));
        for (String line : PotionPaletteModel.wrap(text, width, minecraft.font::width))
            lines.add(Component.literal(line).withStyle(color));
    }
    private static String name(String key) {
        var parts = key.split("/");
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.parse(parts[1]));
        if (item == null) return key;
        if (parts[0].equals("item")) return new net.minecraft.world.item.ItemStack(item).getHoverName().getString();
        var potion = net.minecraft.core.registries.BuiltInRegistries.POTION.get(net.minecraft.resources.ResourceLocation.parse(parts[2]));
        return potion.isEmpty() ? key : net.minecraft.world.item.alchemy.PotionContents.createItemStack(item, potion.orElseThrow()).getHoverName().getString();
    }
    public static List<Component> details(Entry entry, boolean pending) {
        var lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), entry.output()));
        add(lines, PotionPaletteModel.source(entry), ChatFormatting.GRAY);
        add(lines, "Craftable: own " + entry.own() + " | with eligible group " + entry.shared(), ChatFormatting.GRAY);
        add(lines, PotionPaletteModel.variantLabel(entry), ChatFormatting.AQUA);
        if (entry.materials().isEmpty()) {
            add(lines, "No affordable route: missing input, ingredients or fuel. Costs are unavailable.", ChatFormatting.GRAY);
        } else {
            add(lines, entry.own() > 0 ? "Selected route: your inventory." : "Selected route: your inventory plus eligible shared stock.", ChatFormatting.GRAY);
            for (int i = 0; i < entry.materials().size(); i++) {
                var material = entry.materials().get(i);
                add(lines, (i == 0 ? "Consumes input: " : "Requires: ") + material.needed() + " x " + name(material.key())
                        + " | own " + material.own() + ", with group " + material.shared(), ChatFormatting.GRAY);
            }
            add(lines, "Intermediate stages are made within this route; they are not extra inputs.", ChatFormatting.GRAY);
            add(lines, "Fuel: " + entry.steps() + " steps; " + entry.powders()
                    + " new fuel powder (included above). Saved charges are used first.", ChatFormatting.GRAY);
        }
        add(lines, PotionPaletteModel.snapshotStatus(pending), ChatFormatting.GRAY);
        add(lines, pending ? "Waiting for the server." : "Click: craft one onto the cursor. Shift-click: one into inventory, hotbar first.", ChatFormatting.YELLOW);
        return lines;
    }
    public static void draw(GuiGraphics g, PotionPaletteLayout layout, PotionPaletteState state, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font; var panel = layout.panel();
        g.nextStratum(); fill(g, panel, 0xFF17202B);
        g.fill(panel.x(), panel.y(), panel.right(), panel.y() + 1, 0xFF8095A9);
        g.drawString(font, font.plainSubstrByWidth("Potions", Math.max(0, panel.width() - 6)), panel.x() + 3, panel.y() + 4, 0xFFF3F5F8, false);
        var fuel = layout.fuel(); fill(g, fuel, 0xFF10151C);
        int charges = state.view().charges(), capacity = Math.max(1, state.view().perPowder());
        int length = Math.clamp(charges * Math.max(0, fuel.width() - 2) / capacity, 0, Math.max(0, fuel.width() - 2));
        g.fill(fuel.x() + 1, fuel.y() + 1, fuel.x() + 1 + length, fuel.bottom() - 1, 0xFFFFB238);
        if (fuel.contains(mouseX, mouseY))
            g.setTooltipForNextFrame(font, Component.literal("Brewing fuel: " + charges + " / " + capacity + " remaining charges. One blaze powder supplies 20 brewing steps; unused charges are preserved."), mouseX, mouseY);
        var entries = state.entries();
        for (int i = 0; i < layout.tiles().size(); i++) {
            var entry = entries.get(layout.first() + i); var box = layout.tiles().get(i);
            var output = entry.output();
            fill(g, box, PotionPaletteModel.background(entry));
            g.enableScissor(box.x(), box.y(), box.right(), box.bottom());
            boolean narrow = box.width() < 110;
            g.renderItem(output, box.x() + 2, box.y() + 2);
            int count = PotionPaletteModel.count(entry); String text = count > 99 ? "99+" : Integer.toString(count);
            g.drawString(font, text, box.right() - font.width(text) - 2, box.y() + 4, PotionPaletteModel.color(entry), true);
            int textX = box.x() + (narrow ? 2 : 22), textY = box.y() + (narrow ? 21 : 3);
            int textWidth = Math.max(1, box.right() - textX - (narrow ? 2 : 24));
            var lines = font.split(output.getHoverName(), textWidth);
            int limit = Math.max(1, (box.bottom() - 13 - textY) / font.lineHeight);
            for (int line = 0; line < Math.min(limit, lines.size()); line++)
                g.drawString(font, lines.get(line), textX, textY + line * font.lineHeight, 0xFFFFFFFF, true);
            g.drawString(font, font.plainSubstrByWidth(PotionPaletteModel.variantLabel(entry), Math.max(0, box.width() - 4)),
                    box.x() + 2, box.bottom() - 11, 0xFFFFFFFF, true);
            g.disableScissor();
            if (box.contains(mouseX, mouseY)) {
                var tooltip = details(entry, state.pending());
                if (state.view().run() == 0) tooltip.add(Component.literal("Recipes are shown for reference. Crafting becomes available in an active dungeon.").withStyle(ChatFormatting.YELLOW));
                g.setTooltipForNextFrame(font, tooltip, output.getTooltipImage(), output, mouseX, mouseY);
            }
        }
        if (layout.maxFirst() > 0) { fill(g, layout.track(), 0xFF10151C); fill(g, layout.thumb(), 0xFF92A8BC); }
        if (entries.isEmpty() && panel.contains(mouseX, mouseY))
            g.setTooltipForNextFrame(font, Component.literal("Waiting for class recipes. Nothing is reserved."), mouseX, mouseY);
    }
}
