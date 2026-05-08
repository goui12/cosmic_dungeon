package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerFaceRenderer {
    public static final int SKIN_HEAD_U = 8;
    public static final int SKIN_HEAD_V = 8;
    public static final int SKIN_HEAD_WIDTH = 8;
    public static final int SKIN_HEAD_HEIGHT = 8;
    public static final int SKIN_HAT_U = 40;
    public static final int SKIN_HAT_V = 8;
    public static final int SKIN_HAT_WIDTH = 8;
    public static final int SKIN_HAT_HEIGHT = 8;
    public static final int SKIN_TEX_WIDTH = 64;
    public static final int SKIN_TEX_HEIGHT = 64;

    public static void draw(GuiGraphics guiGraphics, PlayerSkin skin, int x, int y, int size) {
        draw(guiGraphics, skin, x, y, size, -1);
    }

    public static void draw(GuiGraphics guiGraphics, PlayerSkin skin, int x, int y, int size, int color) {
        draw(guiGraphics, skin.body().texturePath(), x, y, size, true, false, color);
    }

    public static void draw(
        GuiGraphics guiGraphics, ResourceLocation skinTexture, int x, int y, int size, boolean p_drawHat, boolean upsideDown, int color
    ) {
        int i = 8 + (upsideDown ? 8 : 0);
        int j = 8 * (upsideDown ? -1 : 1);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, skinTexture, x, y, 8.0F, i, size, size, 8, j, 64, 64, color);
        if (p_drawHat) {
            drawHat(guiGraphics, skinTexture, x, y, size, upsideDown, color);
        }
    }

    private static void drawHat(
        GuiGraphics guiGraphics, ResourceLocation skinTexture, int x, int y, int size, boolean upsideDown, int color
    ) {
        int i = 8 + (upsideDown ? 8 : 0);
        int j = 8 * (upsideDown ? -1 : 1);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, skinTexture, x, y, 40.0F, i, size, size, 8, j, 64, 64, color);
    }
}
