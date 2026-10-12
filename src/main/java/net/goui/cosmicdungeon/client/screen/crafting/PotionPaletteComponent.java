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
    public static List<Component> details(Entry entry, PotionPaletteModel.Variant selected, boolean pending) {
        var lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), entry.output()));
        add(lines, PotionPaletteModel.source(entry), ChatFormatting.GRAY);
        add(lines, "Craftable: own " + entry.own() + " | with eligible group " + entry.shared(), ChatFormatting.GRAY);
        if (PotionPaletteModel.variant(entry.targetId()) != selected)
            add(lines, "This effect has no " + selected.description.toLowerCase(Locale.ROOT) + " variant; showing its normal recipe.", ChatFormatting.GRAY);
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
            g.setTooltipForNextFrame(font, Component.literal("No eligible quote. Crafting requires a living Theurgist or Venefex in an active dungeon with available custody. Waiting for the server; nothing is reserved."), mouseX, mouseY);
    }
}
