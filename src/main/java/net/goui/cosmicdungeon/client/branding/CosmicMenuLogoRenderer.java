package net.goui.cosmicdungeon.client.branding;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;

/** Draws the complete authored images, including pixels outside vanilla's cropped logo UVs. */
public final class CosmicMenuLogoRenderer {
    private static final ResourceLocation TITLE = ResourceLocation.fromNamespaceAndPath(
            "cosmicdungeon", "textures/gui/title/cd_minecraft.png");
    private static final ResourceLocation EDITION = ResourceLocation.fromNamespaceAndPath(
            "cosmicdungeon", "textures/gui/title/cd_edition.png");

    public void render(GuiGraphics graphics, int screenWidth, float alpha) {
        int firstButtonY = graphics.guiHeight() / 4 + 32;
        float scale = Math.max(0.1F, Math.min(1.0F,
                Math.min((screenWidth - 24) / 256.0F, (firstButtonY - 12) / 82.0F)));
        int titleWidth = Math.round(256 * scale);
        int titleHeight = Math.round(64 * scale);
        int editionWidth = Math.round(128 * scale);
        int editionHeight = Math.round(16 * scale);
        int top = Math.max(4, Math.min(30, firstButtonY - titleHeight - editionHeight - 10));
        int color = ARGB.white(alpha);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TITLE,
                (screenWidth - titleWidth) / 2, top, 0, 0,
                titleWidth, titleHeight, 1024, 256, 1024, 256, color);
        graphics.blit(RenderPipelines.GUI_TEXTURED, EDITION,
                (screenWidth - editionWidth) / 2, top + titleHeight + 2, 0, 0,
                editionWidth, editionHeight, 512, 64, 512, 64, color);
    }
}
