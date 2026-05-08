package net.minecraft.client.gui.screens.inventory.tooltip;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TooltipRenderUtil {
    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("tooltip/background");
    private static final ResourceLocation FRAME_SPRITE = ResourceLocation.withDefaultNamespace("tooltip/frame");
    public static final int MOUSE_OFFSET = 12;
    private static final int PADDING = 3;
    public static final int PADDING_LEFT = 3;
    public static final int PADDING_RIGHT = 3;
    public static final int PADDING_TOP = 3;
    public static final int PADDING_BOTTOM = 3;
    private static final int MARGIN = 9;

    public static void renderTooltipBackground(
        GuiGraphics guiGraphics, int x, int y, int width, int height, @Nullable ResourceLocation sprite
    ) {
        int i = x - 3 - 9;
        int j = y - 3 - 9;
        int k = width + 3 + 3 + 18;
        int l = height + 3 + 3 + 18;
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, getBackgroundSprite(sprite), i, j, k, l);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, getFrameSprite(sprite), i, j, k, l);
    }

    private static ResourceLocation getBackgroundSprite(@Nullable ResourceLocation name) {
        return name == null ? BACKGROUND_SPRITE : name.withPath(p_371425_ -> "tooltip/" + p_371425_ + "_background");
    }

    private static ResourceLocation getFrameSprite(@Nullable ResourceLocation name) {
        return name == null ? FRAME_SPRITE : name.withPath(p_371467_ -> "tooltip/" + p_371467_ + "_frame");
    }
}
