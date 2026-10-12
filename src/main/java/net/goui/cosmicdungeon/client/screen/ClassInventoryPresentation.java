package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.client.InventoryCraftingPresentation;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.api.ExtraInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

/** One native-slot-aligned layout: avatar/equipment left, storage below, reclaimed class area right. */
public final class ClassInventoryPresentation {
    public static final int LINK_X = 98, LINK_Y = 6, LINK_WIDTH = 72, LINK_HEIGHT = 12;
    private ClassInventoryPresentation() {}

    public static boolean supports(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof ExtraInventoryScreen;
    }
    public static InventoryTheme theme() {
        var player = Minecraft.getInstance().player;
        return InventoryTheme.forClass(player == null ? null : ClassData.getClassId(player));
    }
    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath("cosmicdungeon", path);
    }
    public static void background(GuiGraphics g, AbstractContainerScreen<?> screen) {
        var theme = theme();
        int x = screen.getGuiLeft(), y = screen.getGuiTop(), w = screen.getXSize(), h = screen.getYSize();
        g.fill(x, y, x + w, y + h, 0xFF18202A);
        if (theme.illustrated())
            g.blit(RenderPipelines.GUI_TEXTURED, texture(theme.artPath()), x, y, 0, 0, w, h, 256, 256, 256, 256);
        // Native labels and item counts always sit on quiet, solid surfaces.
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x28000000);
        border(g, x, y, w, h, theme.accent());
        g.fill(x + 25, y + 7, x + 76, y + 79, 0xCE101820);
        border(g, x + 25, y + 7, 51, 72, 0xFF56606A);
        for (var slot : screen.getMenu().slots) {
            if (InventoryCraftingPresentation.hidden(screen, slot) || !slot.isActive()) continue;
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF151C24);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF35404B);
            g.fill(sx, sy + 17, sx + 18, sy + 18, 0xFF859099);
            g.fill(sx + 17, sy, sx + 18, sy + 18, 0xFF859099);
        }
        g.fill(x + 96, y + 3, x + 173, y + 20, 0xF0141C26);
        if (theme.hasClass())
            g.blit(RenderPipelines.GUI_TEXTURED, texture(theme.emblemPath()), x + 143, y + 52,
                    0, 0, 24, 24, 32, 32, 32, 32);
    }
    public static void labels(GuiGraphics g) {
        if (!theme().hasClass())
            g.drawString(Minecraft.getInstance().font, net.minecraft.network.chat.Component.translatable("container.inventory"),
                    LINK_X, LINK_Y + 1, 0xFFFFFFFF, false);
    }
    private static void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
