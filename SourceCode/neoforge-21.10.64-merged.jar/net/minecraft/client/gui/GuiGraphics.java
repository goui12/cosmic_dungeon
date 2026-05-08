package net.minecraft.client.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.ColoredRectangleRenderState;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.client.gui.render.state.TiledBlitRenderState;
import net.minecraft.client.gui.render.state.pip.GuiBannerResultRenderState;
import net.minecraft.client.gui.render.state.pip.GuiBookModelRenderState;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.gui.render.state.pip.GuiProfilerChartRenderState;
import net.minecraft.client.gui.render.state.pip.GuiSignRenderState;
import net.minecraft.client.gui.render.state.pip.GuiSkinRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.model.BannerFlagModel;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.AtlasIds;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ResultField;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector2ic;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class GuiGraphics implements net.neoforged.neoforge.client.extensions.IGuiGraphicsExtension {
    private static final int EXTRA_SPACE_AFTER_FIRST_TOOLTIP_LINE = 2;
    private final Minecraft minecraft;
    private final Matrix3x2fStack pose;
    private final GuiGraphics.ScissorStack scissorStack = new GuiGraphics.ScissorStack();
    private final MaterialSet materials;
    private final TextureAtlas guiSprites;
    private final GuiRenderState guiRenderState;
    private CursorType pendingCursor = CursorType.DEFAULT;
    @Nullable
    private Runnable deferredTooltip;
    private final List<GuiGraphics.OutlineBox> deferredOutlines = new ArrayList<>();
    private ItemStack tooltipStack = ItemStack.EMPTY;

    private GuiGraphics(Minecraft minecraft, Matrix3x2fStack pose, GuiRenderState guiRenderState) {
        this.minecraft = minecraft;
        this.pose = pose;
        AtlasManager atlasmanager = minecraft.getAtlasManager();
        this.materials = atlasmanager;
        this.guiSprites = atlasmanager.getAtlasOrThrow(AtlasIds.GUI);
        this.guiRenderState = guiRenderState;
    }

    public GuiGraphics(Minecraft minecraft, GuiRenderState guiRenderState) {
        this(minecraft, new Matrix3x2fStack(16), guiRenderState);
    }

    public void requestCursor(CursorType cursor) {
        this.pendingCursor = cursor;
    }

    public void applyCursor(Window window) {
        window.selectCursor(this.pendingCursor);
    }

    public int guiWidth() {
        return this.minecraft.getWindow().getGuiScaledWidth();
    }

    public int guiHeight() {
        return this.minecraft.getWindow().getGuiScaledHeight();
    }

    public void nextStratum() {
        this.guiRenderState.nextStratum();
    }

    public void blurBeforeThisStratum() {
        this.guiRenderState.blurBeforeThisStratum();
    }

    public Matrix3x2fStack pose() {
        return this.pose;
    }

    /**
     * Draws a horizontal line from minX to maxX at the specified y-coordinate with the given color.
     *
     * @param minX  the x-coordinate of the start point.
     * @param maxX  the x-coordinate of the end point.
     * @param y     the y-coordinate of the line.
     * @param color the color of the line.
     */
    public void hLine(int minX, int maxX, int y, int color) {
        if (maxX < minX) {
            int i = minX;
            minX = maxX;
            maxX = i;
        }

        this.fill(minX, y, maxX + 1, y + 1, color);
    }

    /**
     * Draws a vertical line from minY to maxY at the specified x-coordinate with the given color.
     *
     * @param x     the x-coordinate of the line.
     * @param minY  the y-coordinate of the start point.
     * @param maxY  the y-coordinate of the end point.
     * @param color the color of the line.
     */
    public void vLine(int x, int minY, int maxY, int color) {
        if (maxY < minY) {
            int i = minY;
            minY = maxY;
            maxY = i;
        }

        this.fill(x, minY + 1, x + 1, maxY, color);
    }

    /**
     * Enables scissoring with the specified screen coordinates.
     *
     * @param minX the minimum x-coordinate of the scissor region.
     * @param minY the minimum y-coordinate of the scissor region.
     * @param maxX the maximum x-coordinate of the scissor region.
     * @param maxY the maximum y-coordinate of the scissor region.
     */
    public void enableScissor(int minX, int minY, int maxX, int maxY) {
        ScreenRectangle screenrectangle = new ScreenRectangle(minX, minY, maxX - minX, maxY - minY)
            .transformAxisAligned(this.pose);
        this.scissorStack.push(screenrectangle);
    }

    public void disableScissor() {
        this.scissorStack.pop();
    }

    public boolean containsPointInScissor(int x, int y) {
        return this.scissorStack.containsPoint(x, y);
    }

    /**
     * Fills a rectangle with the specified color using the given coordinates as the boundaries.
     *
     * @param minX  the minimum x-coordinate of the rectangle.
     * @param minY  the minimum y-coordinate of the rectangle.
     * @param maxX  the maximum x-coordinate of the rectangle.
     * @param maxY  the maximum y-coordinate of the rectangle.
     * @param color the color to fill the rectangle with.
     */
    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        this.fill(RenderPipelines.GUI, minX, minY, maxX, maxY, color);
    }

    public void fill(RenderPipeline pipeline, int minX, int minY, int maxX, int maxY, int color) {
        if (minX < maxX) {
            int i = minX;
            minX = maxX;
            maxX = i;
        }

        if (minY < maxY) {
            int j = minY;
            minY = maxY;
            maxY = j;
        }

        this.submitColoredRectangle(pipeline, TextureSetup.noTexture(), minX, minY, maxX, maxY, color, null);
    }

    /**
     * Fills a rectangle with a gradient color from colorFrom to colorTo using the given coordinates as the boundaries.
     *
     * @param minX      the x-coordinate of the first corner of the rectangle.
     * @param minY      the y-coordinate of the first corner of the rectangle.
     * @param maxX      the x-coordinate of the second corner of the rectangle.
     * @param maxY      the y-coordinate of the second corner of the rectangle.
     * @param colorFrom the starting color of the gradient.
     * @param colorTo   the ending color of the gradient.
     */
    public void fillGradient(int minX, int minY, int maxX, int maxY, int colorFrom, int colorTo) {
        this.submitColoredRectangle(RenderPipelines.GUI, TextureSetup.noTexture(), minX, minY, maxX, maxY, colorFrom, colorTo);
    }

    public void fill(RenderPipeline pipeline, TextureSetup textureSetup, int minX, int minY, int maxX, int maxY) {
        this.submitColoredRectangle(pipeline, textureSetup, minX, minY, maxX, maxY, -1, null);
    }

    private void submitColoredRectangle(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        int minX,
        int minY,
        int maxX,
        int maxY,
        int colorFrom,
        @Nullable Integer colorTo
    ) {
        this.guiRenderState
            .submitGuiElement(
                new ColoredRectangleRenderState(
                    pipeline,
                    textureSetup,
                    new Matrix3x2f(this.pose),
                    minX,
                    minY,
                    maxX,
                    maxY,
                    colorFrom,
                    colorTo != null ? colorTo : colorFrom,
                    this.scissorStack.peek()
                )
            );
    }

    public void textHighlight(int minX, int minY, int maxX, int maxY) {
        this.fill(RenderPipelines.GUI_INVERT, minX, minY, maxX, maxY, -1);
        this.fill(RenderPipelines.GUI_TEXT_HIGHLIGHT, minX, minY, maxX, maxY, -16776961);
    }

    /**
     * Draws a centered string at the specified coordinates using the given font, text, and color.
     *
     * @param font  the font to use for rendering.
     * @param text  the text to draw.
     * @param x     the x-coordinate of the center of the string.
     * @param y     the y-coordinate of the string.
     * @param color the color of the string.
     */
    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        this.drawString(font, text, x - font.width(text) / 2, y, color);
    }

    /**
     * Draws a centered string at the specified coordinates using the given font, text component, and color.
     *
     * @param font  the font to use for rendering.
     * @param text  the text component to draw.
     * @param x     the x-coordinate of the center of the string.
     * @param y     the y-coordinate of the string.
     * @param color the color of the string.
     */
    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        FormattedCharSequence formattedcharsequence = text.getVisualOrderText();
        this.drawString(font, formattedcharsequence, x - font.width(formattedcharsequence) / 2, y, color);
    }

    /**
     * Draws a centered string at the specified coordinates using the given font, formatted character sequence, and color.
     *
     * @param font  the font to use for rendering.
     * @param text  the formatted character sequence to draw.
     * @param x     the x-coordinate of the center of the string.
     * @param y     the y-coordinate of the string.
     * @param color the color of the string.
     */
    public void drawCenteredString(Font font, FormattedCharSequence text, int x, int y, int color) {
        this.drawString(font, text, x - font.width(text) / 2, y, color);
    }

    public void drawString(Font font, @Nullable String text, int x, int y, int color) {
        this.drawString(font, text, x, y, color, true);
    }

    public void drawString(Font font, @Nullable String text, int x, int y, int color, boolean drawShadow) {
        if (text != null) {
            this.drawString(font, Language.getInstance().getVisualOrder(FormattedText.of(text)), x, y, color, drawShadow);
        }
    }

    public void drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
        this.drawString(font, text, x, y, color, true);
    }

    public void drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean drawShadow) {
        if (ARGB.alpha(color) != 0) {
            this.guiRenderState
                .submitText(
                    new GuiTextRenderState(
                        font, text, new Matrix3x2f(this.pose), x, y, color, 0, drawShadow, this.scissorStack.peek()
                    )
                );
        }
    }

    public void drawString(Font font, Component text, int x, int y, int color) {
        this.drawString(font, text, x, y, color, true);
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean drawShadow) {
        this.drawString(font, text.getVisualOrderText(), x, y, color, drawShadow);
    }

    /**
     * Draws a formatted text with word wrapping at the specified coordinates using the given font, text, line width, and color.
     *
     * @param font      the font to use for rendering.
     * @param text      the formatted text to draw.
     * @param x         the x-coordinate of the starting position.
     * @param y         the y-coordinate of the starting position.
     * @param lineWidth the maximum width of each line before wrapping.
     * @param color     the color of the text.
     */
    public void drawWordWrap(Font font, FormattedText text, int x, int y, int lineWidth, int color) {
        this.drawWordWrap(font, text, x, y, lineWidth, color, true);
    }

    public void drawWordWrap(Font font, FormattedText text, int x, int y, int lineWidth, int color, boolean dropShadow) {
        for (FormattedCharSequence formattedcharsequence : font.split(text, lineWidth)) {
            this.drawString(font, formattedcharsequence, x, y, color, dropShadow);
            y += 9;
        }
    }

    public void drawStringWithBackdrop(Font font, Component text, int x, int y, int width, int color) {
        int i = this.minecraft.options.getBackgroundColor(0.0F);
        if (i != 0) {
            int j = 2;
            this.fill(x - 2, y - 2, x + width + 2, y + 9 + 2, ARGB.multiply(i, color));
        }

        this.drawString(font, text, x, y, color, true);
    }

    public void submitOutline(int x, int y, int width, int height, int color) {
        this.deferredOutlines.add(new GuiGraphics.OutlineBox(x, y, width, height, color));
    }

    public void blitSprite(RenderPipeline pipeline, ResourceLocation sprite, int x, int y, int width, int height) {
        this.blitSprite(pipeline, sprite, x, y, width, height, -1);
    }

    public void blitSprite(RenderPipeline pipeline, ResourceLocation sprite, int x, int y, int width, int height, float fade) {
        this.blitSprite(pipeline, sprite, x, y, width, height, ARGB.color(fade, -1));
    }

    private static GuiSpriteScaling getSpriteScaling(TextureAtlasSprite sprite) {
        return sprite.contents().getAdditionalMetadata(GuiMetadataSection.TYPE).orElse(GuiMetadataSection.DEFAULT).scaling();
    }

    public void blitSprite(RenderPipeline pipeline, ResourceLocation sprite, int x, int y, int width, int height, int color) {
        TextureAtlasSprite textureatlassprite = this.guiSprites.getSprite(sprite);
        GuiSpriteScaling guispritescaling = getSpriteScaling(textureatlassprite);
        switch (guispritescaling) {
            case GuiSpriteScaling.Stretch guispritescaling$stretch:
                this.blitSprite(pipeline, textureatlassprite, x, y, width, height, color);
                break;
            case GuiSpriteScaling.Tile guispritescaling$tile:
                this.blitTiledSprite(
                    pipeline,
                    textureatlassprite,
                    x,
                    y,
                    width,
                    height,
                    0,
                    0,
                    guispritescaling$tile.width(),
                    guispritescaling$tile.height(),
                    guispritescaling$tile.width(),
                    guispritescaling$tile.height(),
                    color
                );
                break;
            case GuiSpriteScaling.NineSlice guispritescaling$nineslice:
                this.blitNineSlicedSprite(pipeline, textureatlassprite, guispritescaling$nineslice, x, y, width, height, color);
                break;
            default:
        }
    }

    public void blitSprite(
        RenderPipeline pipeline,
        ResourceLocation sprite,
        int textureWidth,
        int textureHeight,
        int u,
        int v,
        int x,
        int y,
        int width,
        int height
    ) {
        this.blitSprite(pipeline, sprite, textureWidth, textureHeight, u, v, x, y, width, height, -1);
    }

    public void blitSprite(
        RenderPipeline pipeline,
        ResourceLocation sprite,
        int textureWidth,
        int textureHeight,
        int u,
        int v,
        int x,
        int y,
        int width,
        int height,
        int color
    ) {
        TextureAtlasSprite textureatlassprite = this.guiSprites.getSprite(sprite);
        GuiSpriteScaling guispritescaling = getSpriteScaling(textureatlassprite);
        if (guispritescaling instanceof GuiSpriteScaling.Stretch) {
            this.blitSprite(pipeline, textureatlassprite, textureWidth, textureHeight, u, v, x, y, width, height, color);
        } else {
            this.enableScissor(x, y, x + width, y + height);
            this.blitSprite(pipeline, sprite, x - u, y - v, textureWidth, textureHeight, color);
            this.disableScissor();
        }
    }

    public void blitSprite(RenderPipeline pipeline, TextureAtlasSprite sprite, int x, int width, int y, int height) {
        this.blitSprite(pipeline, sprite, x, width, y, height, -1);
    }

    public void blitSprite(RenderPipeline pipeline, TextureAtlasSprite sprite, int x, int y, int width, int height, int color) {
        if (width != 0 && height != 0) {
            this.innerBlit(
                pipeline,
                sprite.atlasLocation(),
                x,
                x + width,
                y,
                y + height,
                sprite.getU0(),
                sprite.getU1(),
                sprite.getV0(),
                sprite.getV1(),
                color
            );
        }
    }

    private void blitSprite(
        RenderPipeline pipeline,
        TextureAtlasSprite sprite,
        int textureWidth,
        int textureHeight,
        int u,
        int v,
        int x,
        int y,
        int width,
        int height,
        int color
    ) {
        if (width != 0 && height != 0) {
            this.innerBlit(
                pipeline,
                sprite.atlasLocation(),
                x,
                x + width,
                y,
                y + height,
                sprite.getU((float)u / textureWidth),
                sprite.getU((float)(u + width) / textureWidth),
                sprite.getV((float)v / textureHeight),
                sprite.getV((float)(v + height) / textureHeight),
                color
            );
        }
    }

    private void blitNineSlicedSprite(
        RenderPipeline pipeline,
        TextureAtlasSprite sprite,
        GuiSpriteScaling.NineSlice nineSlice,
        int x,
        int y,
        int width,
        int height,
        int color
    ) {
        GuiSpriteScaling.NineSlice.Border guispritescaling$nineslice$border = nineSlice.border();
        int i = Math.min(guispritescaling$nineslice$border.left(), width / 2);
        int j = Math.min(guispritescaling$nineslice$border.right(), width / 2);
        int k = Math.min(guispritescaling$nineslice$border.top(), height / 2);
        int l = Math.min(guispritescaling$nineslice$border.bottom(), height / 2);
        if (width == nineSlice.width() && height == nineSlice.height()) {
            this.blitSprite(pipeline, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, width, height, color);
        } else if (height == nineSlice.height()) {
            this.blitSprite(pipeline, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, i, height, color);
            this.blitNineSliceInnerSegment(
                pipeline,
                nineSlice,
                sprite,
                x + i,
                y,
                width - j - i,
                height,
                i,
                0,
                nineSlice.width() - j - i,
                nineSlice.height(),
                nineSlice.width(),
                nineSlice.height(),
                color
            );
            this.blitSprite(
                pipeline,
                sprite,
                nineSlice.width(),
                nineSlice.height(),
                nineSlice.width() - j,
                0,
                x + width - j,
                y,
                j,
                height,
                color
            );
        } else if (width == nineSlice.width()) {
            this.blitSprite(pipeline, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, width, k, color);
            this.blitNineSliceInnerSegment(
                pipeline,
                nineSlice,
                sprite,
                x,
                y + k,
                width,
                height - l - k,
                0,
                k,
                nineSlice.width(),
                nineSlice.height() - l - k,
                nineSlice.width(),
                nineSlice.height(),
                color
            );
            this.blitSprite(
                pipeline,
                sprite,
                nineSlice.width(),
                nineSlice.height(),
                0,
                nineSlice.height() - l,
                x,
                y + height - l,
                width,
                l,
                color
            );
        } else {
            this.blitSprite(pipeline, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, i, k, color);
            this.blitNineSliceInnerSegment(
                pipeline,
                nineSlice,
                sprite,
                x + i,
                y,
                width - j - i,
                k,
                i,
                0,
                nineSlice.width() - j - i,
                k,
                nineSlice.width(),
                nineSlice.height(),
                color
            );
            this.blitSprite(
                pipeline, sprite, nineSlice.width(), nineSlice.height(), nineSlice.width() - j, 0, x + width - j, y, j, k, color
            );
            this.blitSprite(
                pipeline, sprite, nineSlice.width(), nineSlice.height(), 0, nineSlice.height() - l, x, y + height - l, i, l, color
            );
            this.blitNineSliceInnerSegment(
                pipeline,
                nineSlice,
                sprite,
                x + i,
                y + height - l,
                width - j - i,
                l,
                i,
                nineSlice.height() - l,
                nineSlice.width() - j - i,
                l,
                nineSlice.width(),
                nineSlice.height(),
                color
            );
            this.blitSprite(
                pipeline,
                sprite,
                nineSlice.width(),
                nineSlice.height(),
                nineSlice.width() - j,
                nineSlice.height() - l,
                x + width - j,
                y + height - l,
                j,
                l,
                color
            );
            this.blitNineSliceInnerSegment(
                pipeline,
                nineSlice,
                sprite,
                x,
                y + k,
                i,
                height - l - k,
                0,
                k,
                i,
                nineSlice.height() - l - k,
                nineSlice.width(),
                nineSlice.height(),
                color
            );
            this.blitNineSliceInnerSegment(
                pipeline,
                nineSlice,
                sprite,
                x + i,
                y + k,
                width - j - i,
                height - l - k,
                i,
                k,
                nineSlice.width() - j - i,
                nineSlice.height() - l - k,
                nineSlice.width(),
                nineSlice.height(),
                color
            );
            this.blitNineSliceInnerSegment(
                pipeline,
                nineSlice,
                sprite,
                x + width - j,
                y + k,
                j,
                height - l - k,
                nineSlice.width() - j,
                k,
                j,
                nineSlice.height() - l - k,
                nineSlice.width(),
                nineSlice.height(),
                color
            );
        }
    }

    private void blitNineSliceInnerSegment(
        RenderPipeline pipeline,
        GuiSpriteScaling.NineSlice nineSlice,
        TextureAtlasSprite sprite,
        int borderMinX,
        int borderMinY,
        int borderMaxX,
        int borderMaxY,
        int u,
        int v,
        int spriteWidth,
        int spriteHeight,
        int textureWidth,
        int textureHeight,
        int color
    ) {
        if (borderMaxX > 0 && borderMaxY > 0) {
            if (nineSlice.stretchInner()) {
                this.innerBlit(
                    pipeline,
                    sprite.atlasLocation(),
                    borderMinX,
                    borderMinX + borderMaxX,
                    borderMinY,
                    borderMinY + borderMaxY,
                    sprite.getU((float)u / textureWidth),
                    sprite.getU((float)(u + spriteWidth) / textureWidth),
                    sprite.getV((float)v / textureHeight),
                    sprite.getV((float)(v + spriteHeight) / textureHeight),
                    color
                );
            } else {
                this.blitTiledSprite(
                    pipeline,
                    sprite,
                    borderMinX,
                    borderMinY,
                    borderMaxX,
                    borderMaxY,
                    u,
                    v,
                    spriteWidth,
                    spriteHeight,
                    textureWidth,
                    textureHeight,
                    color
                );
            }
        }
    }

    private void blitTiledSprite(
        RenderPipeline pipeline,
        TextureAtlasSprite sprite,
        int x,
        int y,
        int width,
        int height,
        int u,
        int v,
        int spriteWidth,
        int spriteHeight,
        int textureWidth,
        int textureHeight,
        int color
    ) {
        if (width > 0 && height > 0) {
            if (spriteWidth > 0 && spriteHeight > 0) {
                GpuTextureView gputextureview = this.minecraft.getTextureManager().getTexture(sprite.atlasLocation()).getTextureView();
                this.submitTiledBlit(
                    pipeline,
                    gputextureview,
                    spriteWidth,
                    spriteHeight,
                    x,
                    y,
                    x + width,
                    y + height,
                    sprite.getU((float)u / textureWidth),
                    sprite.getU((float)(u + spriteWidth) / textureWidth),
                    sprite.getV((float)v / textureHeight),
                    sprite.getV((float)(v + spriteHeight) / textureHeight),
                    color
                );
            } else {
                throw new IllegalArgumentException("Tile size must be positive, got " + spriteWidth + "x" + spriteHeight);
            }
        }
    }

    public void blit(
        RenderPipeline pipeline,
        ResourceLocation atlas,
        int x,
        int y,
        float u,
        float v,
        int width,
        int height,
        int textureWidth,
        int textureHeight,
        int color
    ) {
        this.blit(pipeline, atlas, x, y, u, v, width, height, width, height, textureWidth, textureHeight, color);
    }

    public void blit(
        RenderPipeline pipeline,
        ResourceLocation atlas,
        int x,
        int y,
        float u,
        float v,
        int width,
        int height,
        int textureWidth,
        int textureHeight
    ) {
        this.blit(pipeline, atlas, x, y, u, v, width, height, width, height, textureWidth, textureHeight);
    }

    public void blit(
        RenderPipeline pipeline,
        ResourceLocation atlas,
        int x,
        int y,
        float u,
        float v,
        int width,
        int height,
        int uWidth,
        int vHeight,
        int textureWidth,
        int textureHeight
    ) {
        this.blit(pipeline, atlas, x, y, u, v, width, height, uWidth, vHeight, textureWidth, textureHeight, -1);
    }

    public void blit(
        RenderPipeline pipeline,
        ResourceLocation atlas,
        int x,
        int y,
        float u,
        float v,
        int width,
        int height,
        int uWidth,
        int vHeight,
        int textureWidth,
        int textureHeight,
        int color
    ) {
        this.innerBlit(
            pipeline,
            atlas,
            x,
            x + width,
            y,
            y + height,
            (u + 0.0F) / textureWidth,
            (u + uWidth) / textureWidth,
            (v + 0.0F) / textureHeight,
            (v + vHeight) / textureHeight,
            color
        );
    }

    public void blit(
        ResourceLocation atlas,
        int x0,
        int y0,
        int x1,
        int y1,
        float u0,
        float u1,
        float v0,
        float v1
    ) {
        this.innerBlit(RenderPipelines.GUI_TEXTURED, atlas, x0, x1, y0, y1, u0, u1, v0, v1, -1);
    }

    private void innerBlit(
        RenderPipeline pipeline,
        ResourceLocation atlas,
        int x0,
        int x1,
        int y0,
        int y1,
        float u0,
        float u1,
        float v0,
        float v1,
        int color
    ) {
        GpuTextureView gputextureview = this.minecraft.getTextureManager().getTexture(atlas).getTextureView();
        this.submitBlit(pipeline, gputextureview, x0, y0, x1, y1, u0, u1, v0, v1, color);
    }

    private void submitBlit(
        RenderPipeline pipeline,
        GpuTextureView atlasTexture,
        int x0,
        int y0,
        int x1,
        int y1,
        float u0,
        float u1,
        float v0,
        float v1,
        int color
    ) {
        this.guiRenderState
            .submitGuiElement(
                new BlitRenderState(
                    pipeline,
                    TextureSetup.singleTexture(atlasTexture),
                    new Matrix3x2f(this.pose),
                    x0,
                    y0,
                    x1,
                    y1,
                    u0,
                    u1,
                    v0,
                    v1,
                    color,
                    this.scissorStack.peek()
                )
            );
    }

    private void submitTiledBlit(
        RenderPipeline pipeline,
        GpuTextureView atlasTexture,
        int tileWidth,
        int tileHeight,
        int x0,
        int x1,
        int y0,
        int y1,
        float u0,
        float u1,
        float v0,
        float v1,
        int color
    ) {
        this.guiRenderState
            .submitGuiElement(
                new TiledBlitRenderState(
                    pipeline,
                    TextureSetup.singleTexture(atlasTexture),
                    new Matrix3x2f(this.pose),
                    tileWidth,
                    tileHeight,
                    x0,
                    x1,
                    y0,
                    y1,
                    u0,
                    u1,
                    v0,
                    v1,
                    color,
                    this.scissorStack.peek()
                )
            );
    }

    /**
     * Renders an item stack at the specified coordinates.
     *
     * @param stack the item stack to render.
     * @param x     the x-coordinate of the rendering position.
     * @param y     the y-coordinate of the rendering position.
     */
    public void renderItem(ItemStack stack, int x, int y) {
        this.renderItem(this.minecraft.player, this.minecraft.level, stack, x, y, 0);
    }

    /**
     * Renders an item stack at the specified coordinates with a random seed.
     *
     * @param stack the item stack to render.
     * @param x     the x-coordinate of the rendering position.
     * @param y     the y-coordinate of the rendering position.
     * @param seed  the random seed.
     */
    public void renderItem(ItemStack stack, int x, int y, int seed) {
        this.renderItem(this.minecraft.player, this.minecraft.level, stack, x, y, seed);
    }

    /**
     * Renders a fake item stack at the specified coordinates.
     *
     * @param stack the fake item stack to render.
     * @param x     the x-coordinate of the rendering position.
     * @param y     the y-coordinate of the rendering position.
     */
    public void renderFakeItem(ItemStack stack, int x, int y) {
        this.renderFakeItem(stack, x, y, 0);
    }

    public void renderFakeItem(ItemStack stack, int x, int y, int seed) {
        this.renderItem(null, this.minecraft.level, stack, x, y, seed);
    }

    /**
     * Renders an item stack for a living entity at the specified coordinates with a random seed.
     *
     * @param entity the living entity.
     * @param stack  the item stack to render.
     * @param x      the x-coordinate of the rendering position.
     * @param y      the y-coordinate of the rendering position.
     * @param seed   the random seed.
     */
    public void renderItem(LivingEntity entity, ItemStack stack, int x, int y, int seed) {
        this.renderItem(entity, entity.level(), stack, x, y, seed);
    }

    /**
     * Renders an item stack for a living entity in a specific level at the specified coordinates with a random seed.
     *
     * @param entity the living entity. Can be null.
     * @param level  the level in which the rendering occurs. Can be null.
     * @param stack  the item stack to render.
     * @param x      the x-coordinate of the rendering position.
     * @param y      the y-coordinate of the rendering position.
     * @param seed   the random seed.
     */
    private void renderItem(@Nullable LivingEntity entity, @Nullable Level level, ItemStack stack, int x, int y, int seed) {
        if (!stack.isEmpty()) {
            TrackingItemStackRenderState trackingitemstackrenderstate = new TrackingItemStackRenderState();
            this.minecraft
                .getItemModelResolver()
                .updateForTopItem(trackingitemstackrenderstate, stack, ItemDisplayContext.GUI, level, entity, seed);

            try {
                this.guiRenderState
                    .submitItem(
                        new GuiItemRenderState(
                            stack.getItem().getName().toString(),
                            new Matrix3x2f(this.pose),
                            trackingitemstackrenderstate,
                            x,
                            y,
                            this.scissorStack.peek()
                        )
                    );
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering item");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Item being rendered");
                crashreportcategory.setDetail("Item Type", () -> String.valueOf(stack.getItem()));
                crashreportcategory.setDetail("Item Components", () -> String.valueOf(stack.getComponents()));
                crashreportcategory.setDetail("Item Foil", () -> String.valueOf(stack.hasFoil()));
                throw new ReportedException(crashreport);
            }
        }
    }

    /**
     * Renders additional decorations for an item stack at the specified coordinates.
     *
     * @param font  the font used for rendering text.
     * @param stack the item stack to decorate.
     * @param x     the x-coordinate of the rendering position.
     * @param y     the y-coordinate of the rendering position.
     */
    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        this.renderItemDecorations(font, stack, x, y, null);
    }

    /**
     * Renders additional decorations for an item stack at the specified coordinates with optional custom text.
     *
     * @param font  the font used for rendering text.
     * @param stack the item stack to decorate.
     * @param x     the x-coordinate of the rendering position.
     * @param y     the y-coordinate of the rendering position.
     * @param text  the custom text to display. Can be null.
     */
    public void renderItemDecorations(Font font, ItemStack stack, int x, int y, @Nullable String text) {
        if (!stack.isEmpty()) {
            this.pose.pushMatrix();
            this.renderItemBar(stack, x, y);
            this.renderItemCooldown(stack, x, y);
            this.renderItemCount(font, stack, x, y, text);
            this.pose.popMatrix();
            // TODO 1.21.2: This probably belongs in one of the sub-methods.
            net.neoforged.neoforge.client.ItemDecoratorHandler.of(stack).render(this, font, stack, x, y);
        }
    }

    public void setTooltipForNextFrame(Component text, int x, int y) {
        this.setTooltipForNextFrame(List.of(text.getVisualOrderText()), x, y);
    }

    public void setTooltipForNextFrame(List<FormattedCharSequence> lines, int x, int y) {
        this.setTooltipForNextFrame(this.minecraft.font, lines, DefaultTooltipPositioner.INSTANCE, x, y, false);
    }

    public void setTooltipForNextFrame(Font font, ItemStack stack, int x, int y) {
        this.tooltipStack = stack;
        this.setTooltipForNextFrame(
            font,
            Screen.getTooltipFromItem(this.minecraft, stack),
            stack.getTooltipImage(),
            x,
            y,
            stack.get(DataComponents.TOOLTIP_STYLE)
        );
        this.tooltipStack = ItemStack.EMPTY;
    }

    public void setTooltipForNextFrame(Font font, List<Component> textComponents, Optional<TooltipComponent> tooltipComponent, ItemStack stack, int mouseX, int mouseY) {
        setTooltipForNextFrame(font, textComponents, tooltipComponent, stack, mouseX, mouseY, null);
    }

    public void setTooltipForNextFrame(Font font, List<Component> textComponents, Optional<TooltipComponent> tooltipComponent, ItemStack stack, int mouseX, int mouseY, @Nullable ResourceLocation backgroundTexture) {
        this.tooltipStack = stack;
        this.setTooltipForNextFrame(font, textComponents, tooltipComponent, mouseX, mouseY, backgroundTexture);
        this.tooltipStack = ItemStack.EMPTY;
    }

    public void setTooltipForNextFrame(Font font, List<Component> lines, Optional<TooltipComponent> tooltipImage, int x, int y) {
        this.setTooltipForNextFrame(font, lines, tooltipImage, x, y, null);
    }

    public void setTooltipForNextFrame(
        Font font, List<Component> lines, Optional<TooltipComponent> tooltipImage, int x, int y, @Nullable ResourceLocation background
    ) {
        List<ClientTooltipComponent> list = net.neoforged.neoforge.client.ClientHooks.gatherTooltipComponents(this.tooltipStack, lines, tooltipImage, x, guiWidth(), guiHeight(), font);
        this.setTooltipForNextFrameInternal(font, list, x, y, DefaultTooltipPositioner.INSTANCE, background, false);
    }

    public void setTooltipForNextFrame(Font font, Component text, int x, int y) {
        this.setTooltipForNextFrame(font, text, x, y, null);
    }

    public void setTooltipForNextFrame(Font font, Component text, int x, int y, @Nullable ResourceLocation background) {
        this.setTooltipForNextFrame(font, List.of(text.getVisualOrderText()), x, y, background);
    }

    public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int x, int y) {
        this.setComponentTooltipForNextFrame(font, lines, x, y, (ResourceLocation) null);
    }

    public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int x, int y, @Nullable ResourceLocation background) {
        List<ClientTooltipComponent> components = net.neoforged.neoforge.client.ClientHooks.gatherTooltipComponents(this.tooltipStack, lines, x, guiWidth(), guiHeight(), font);
        this.setTooltipForNextFrameInternal(
            font,
            components,
            x,
            y,
            DefaultTooltipPositioner.INSTANCE,
            background,
            false
        );
    }

    public void setComponentTooltipForNextFrame(Font p_font, List<? extends net.minecraft.network.chat.FormattedText> lines, int x, int y, ItemStack stack) {
        setComponentTooltipForNextFrame(p_font, lines, x, y, stack, null);
    }

    public void setComponentTooltipForNextFrame(Font p_font, List<? extends net.minecraft.network.chat.FormattedText> lines, int x, int y, ItemStack stack, @Nullable ResourceLocation backgroundTexture) {
        this.tooltipStack = stack;
        List<ClientTooltipComponent> components = net.neoforged.neoforge.client.ClientHooks.gatherTooltipComponents(stack, lines, x, guiWidth(), guiHeight(), p_font);
        this.setTooltipForNextFrameInternal(p_font, components, x, y, DefaultTooltipPositioner.INSTANCE, backgroundTexture, false);
        this.tooltipStack = ItemStack.EMPTY;
    }

    public void setComponentTooltipFromElementsForNextFrame(Font font, List<com.mojang.datafixers.util.Either<FormattedText, TooltipComponent>> elements, int mouseX, int mouseY, ItemStack stack) {
        setComponentTooltipFromElementsForNextFrame(font, elements, mouseX, mouseY, stack, null);
    }

    public void setComponentTooltipFromElementsForNextFrame(Font font, List<com.mojang.datafixers.util.Either<FormattedText, TooltipComponent>> elements, int mouseX, int mouseY, ItemStack stack, @Nullable ResourceLocation backgroundTexture) {
        this.tooltipStack = stack;
        List<ClientTooltipComponent> components = net.neoforged.neoforge.client.ClientHooks.gatherTooltipComponentsFromElements(stack, elements, mouseX, guiWidth(), guiHeight(), font);
        this.setTooltipForNextFrameInternal(font, components, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, backgroundTexture, false);
        this.tooltipStack = ItemStack.EMPTY;
    }

    public void setTooltipForNextFrame(Font font, List<? extends FormattedCharSequence> lines, int x, int y) {
        this.setTooltipForNextFrame(font, lines, x, y, null);
    }

    public void setTooltipForNextFrame(
        Font font, List<? extends FormattedCharSequence> lines, int x, int y, @Nullable ResourceLocation background
    ) {
        this.setTooltipForNextFrameInternal(
            font,
            lines.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()),
            x,
            y,
            DefaultTooltipPositioner.INSTANCE,
            background,
            false
        );
    }

    public void setTooltipForNextFrame(
        Font font, List<FormattedCharSequence> lines, ClientTooltipPositioner positioner, int x, int y, boolean focused
    ) {
        this.setTooltipForNextFrameInternal(
            font, lines.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()), x, y, positioner, null, focused
        );
    }

    private void setTooltipForNextFrameInternal(
        Font font,
        List<ClientTooltipComponent> components,
        int x,
        int y,
        ClientTooltipPositioner positioner,
        @Nullable ResourceLocation background,
        boolean focused
    ) {
        if (!components.isEmpty()) {
            if (this.deferredTooltip == null || focused) {
                ItemStack capturedTooltipStack = this.tooltipStack;
                this.deferredTooltip = () -> this.renderTooltip(font, components, x, y, positioner, background, capturedTooltipStack);
            }
        }
    }

    public void renderTooltip(
        Font font,
        List<ClientTooltipComponent> components,
        int x,
        int y,
        ClientTooltipPositioner positioner,
        @Nullable ResourceLocation background
    ) {
        this.renderTooltip(font, components, x, y, positioner, background, ItemStack.EMPTY);
    }

    public void renderTooltip(
            Font font,
            List<ClientTooltipComponent> components,
            int x,
            int y,
            ClientTooltipPositioner positioner,
            @Nullable ResourceLocation background,
            ItemStack tooltipStack
    ) {
        var preEvent = net.neoforged.neoforge.client.ClientHooks.onRenderTooltipPre(tooltipStack, this, x, y, guiWidth(), guiHeight(), components, font, positioner);
        if (preEvent.isCanceled()) return;

        font = preEvent.getFont();
        x = preEvent.getX();
        y = preEvent.getY();

        int i = 0;
        int j = components.size() == 1 ? -2 : 0;

        for (ClientTooltipComponent clienttooltipcomponent : components) {
            int k = clienttooltipcomponent.getWidth(font);
            if (k > i) {
                i = k;
            }

            j += clienttooltipcomponent.getHeight(font);
        }

        int l1 = i;
        int i2 = j;
        Vector2ic vector2ic = positioner.positionTooltip(this.guiWidth(), this.guiHeight(), x, y, i, j);
        int l = vector2ic.x();
        int i1 = vector2ic.y();
        this.pose.pushMatrix();
        var textureEvent = net.neoforged.neoforge.client.ClientHooks.onRenderTooltipTexture(this.tooltipStack, this, l, i1, preEvent.getFont(), components, background);
        TooltipRenderUtil.renderTooltipBackground(this, l, i1, i, j, textureEvent.getTexture());
        int j1 = i1;

        for (int k1 = 0; k1 < components.size(); k1++) {
            ClientTooltipComponent clienttooltipcomponent1 = components.get(k1);
            clienttooltipcomponent1.renderText(this, font, l, j1);
            j1 += clienttooltipcomponent1.getHeight(font) + (k1 == 0 ? 2 : 0);
        }

        j1 = i1;

        for (int j2 = 0; j2 < components.size(); j2++) {
            ClientTooltipComponent clienttooltipcomponent2 = components.get(j2);
            clienttooltipcomponent2.renderImage(font, l, j1, l1, i2, this);
            j1 += clienttooltipcomponent2.getHeight(font) + (j2 == 0 ? 2 : 0);
        }

        this.pose.popMatrix();
    }

    public void renderDeferredElements() {
        if (!this.deferredOutlines.isEmpty()) {
            this.nextStratum();

            for (GuiGraphics.OutlineBox guigraphics$outlinebox : this.deferredOutlines) {
                guigraphics$outlinebox.render(this);
            }

            this.deferredOutlines.clear();
        }

        if (this.deferredTooltip != null) {
            this.nextStratum();
            this.deferredTooltip.run();
            this.deferredTooltip = null;
        }
    }

    private void renderItemBar(ItemStack stack, int x, int y) {
        if (stack.isBarVisible()) {
            int i = x + 2;
            int j = y + 13;
            this.fill(RenderPipelines.GUI, i, j, i + 13, j + 2, -16777216);
            this.fill(RenderPipelines.GUI, i, j, i + stack.getBarWidth(), j + 1, ARGB.opaque(stack.getBarColor()));
        }
    }

    private void renderItemCount(Font font, ItemStack stack, int x, int y, @Nullable String text) {
        if (stack.getCount() != 1 || text != null) {
            String s = text == null ? String.valueOf(stack.getCount()) : text;
            this.drawString(font, s, x + 19 - 2 - font.width(s), y + 6 + 3, -1, true);
        }
    }

    private void renderItemCooldown(ItemStack stack, int x, int y) {
        LocalPlayer localplayer = this.minecraft.player;
        float f = localplayer == null
            ? 0.0F
            : localplayer.getCooldowns().getCooldownPercent(stack, this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        if (f > 0.0F) {
            int i = y + Mth.floor(16.0F * (1.0F - f));
            int j = i + Mth.ceil(16.0F * f);
            this.fill(RenderPipelines.GUI, x, i, x + 16, j, Integer.MAX_VALUE);
        }
    }

    /**
     * Renders a hover effect for a text component at the specified mouse coordinates.
     *
     * @param font   the font used for rendering text.
     * @param style  the style of the text component. Can be null.
     * @param mouseX the x-coordinate of the mouse position.
     * @param mouseY the y-coordinate of the mouse position.
     */
    public void renderComponentHoverEffect(Font font, @Nullable Style style, int mouseX, int mouseY) {
        if (style != null) {
            if (style.getClickEvent() != null) {
                this.requestCursor(CursorTypes.POINTING_HAND);
            }

            if (style.getHoverEvent() != null) {
                switch (style.getHoverEvent()) {
                    case HoverEvent.ShowItem(ItemStack itemstack):
                        this.setTooltipForNextFrame(font, itemstack, mouseX, mouseY);
                        break;
                    case HoverEvent.ShowEntity(HoverEvent.EntityTooltipInfo hoverevent$entitytooltipinfo1):
                        HoverEvent.EntityTooltipInfo hoverevent$entitytooltipinfo = hoverevent$entitytooltipinfo1;
                        if (this.minecraft.options.advancedItemTooltips) {
                            this.setComponentTooltipForNextFrame(font, hoverevent$entitytooltipinfo.getTooltipLines(), mouseX, mouseY);
                        }
                        break;
                    case HoverEvent.ShowText(Component component):
                        this.setTooltipForNextFrame(font, font.split(component, Math.max(this.guiWidth() / 2, 200)), mouseX, mouseY);
                        break;
                    default:
                }
            }
        }
    }

    public void submitMapRenderState(MapRenderState renderState) {
        Minecraft minecraft = Minecraft.getInstance();
        TextureManager texturemanager = minecraft.getTextureManager();
        GpuTextureView gputextureview = texturemanager.getTexture(renderState.texture).getTextureView();
        this.submitBlit(RenderPipelines.GUI_TEXTURED, gputextureview, 0, 0, 128, 128, 0.0F, 1.0F, 0.0F, 1.0F, -1);

        for (MapRenderState.MapDecorationRenderState maprenderstate$mapdecorationrenderstate : renderState.decorations) {
            if (maprenderstate$mapdecorationrenderstate.renderOnFrame) {
                this.pose.pushMatrix();
                this.pose.translate(maprenderstate$mapdecorationrenderstate.x / 2.0F + 64.0F, maprenderstate$mapdecorationrenderstate.y / 2.0F + 64.0F);
                this.pose.rotate((float) (Math.PI / 180.0) * maprenderstate$mapdecorationrenderstate.rot * 360.0F / 16.0F);
                this.pose.scale(4.0F, 4.0F);
                this.pose.translate(-0.125F, 0.125F);
                TextureAtlasSprite textureatlassprite = maprenderstate$mapdecorationrenderstate.atlasSprite;
                if (textureatlassprite != null) {
                    GpuTextureView gputextureview1 = texturemanager.getTexture(textureatlassprite.atlasLocation()).getTextureView();
                    this.submitBlit(
                        RenderPipelines.GUI_TEXTURED,
                        gputextureview1,
                        -1,
                        -1,
                        1,
                        1,
                        textureatlassprite.getU0(),
                        textureatlassprite.getU1(),
                        textureatlassprite.getV1(),
                        textureatlassprite.getV0(),
                        -1
                    );
                }

                this.pose.popMatrix();
                if (maprenderstate$mapdecorationrenderstate.name != null) {
                    Font font = minecraft.font;
                    float f = font.width(maprenderstate$mapdecorationrenderstate.name);
                    float f1 = Mth.clamp(25.0F / f, 0.0F, 6.0F / 9.0F);
                    this.pose.pushMatrix();
                    this.pose
                        .translate(
                            maprenderstate$mapdecorationrenderstate.x / 2.0F + 64.0F - f * f1 / 2.0F,
                            maprenderstate$mapdecorationrenderstate.y / 2.0F + 64.0F + 4.0F
                        );
                    this.pose.scale(f1, f1);
                    this.guiRenderState
                        .submitText(
                            new GuiTextRenderState(
                                font,
                                maprenderstate$mapdecorationrenderstate.name.getVisualOrderText(),
                                new Matrix3x2f(this.pose),
                                0,
                                0,
                                -1,
                                Integer.MIN_VALUE,
                                false,
                                this.scissorStack.peek()
                            )
                        );
                    this.pose.popMatrix();
                }
            }
        }
    }

    public void submitEntityRenderState(
        EntityRenderState renderState,
        float scale,
        Vector3f translation,
        Quaternionf rotation,
        @Nullable Quaternionf overrideCameraAngle,
        int x0,
        int y0,
        int x1,
        int y1
    ) {
        this.guiRenderState
            .submitPicturesInPictureState(
                new GuiEntityRenderState(
                    renderState, translation, rotation, overrideCameraAngle, x0, y0, x1, y1, scale, this.scissorStack.peek()
                )
            );
    }

    public void submitSkinRenderState(
        PlayerModel playerModel,
        ResourceLocation texture,
        float scale,
        float rotationX,
        float rotationY,
        float pivotY,
        int x0,
        int y0,
        int x1,
        int y1
    ) {
        this.guiRenderState
            .submitPicturesInPictureState(
                new GuiSkinRenderState(
                    playerModel, texture, rotationX, rotationY, pivotY, x0, y0, x1, y1, scale, this.scissorStack.peek()
                )
            );
    }

    public void submitBookModelRenderState(
        BookModel bookModel,
        ResourceLocation texture,
        float scale,
        float open,
        float flip,
        int x0,
        int y0,
        int x1,
        int y1
    ) {
        this.guiRenderState
            .submitPicturesInPictureState(
                new GuiBookModelRenderState(
                    bookModel, texture, open, flip, x0, y0, x1, y1, scale, this.scissorStack.peek()
                )
            );
    }

    public void submitBannerPatternRenderState(
        BannerFlagModel flag, DyeColor baseColor, BannerPatternLayers resultBannerPatterns, int x0, int y0, int x1, int y1
    ) {
        this.guiRenderState
            .submitPicturesInPictureState(
                new GuiBannerResultRenderState(flag, baseColor, resultBannerPatterns, x0, y0, x1, y1, this.scissorStack.peek())
            );
    }

    public void submitSignRenderState(Model.Simple signModel, float scale, WoodType woodType, int x0, int y0, int x1, int y1) {
        this.guiRenderState
            .submitPicturesInPictureState(
                new GuiSignRenderState(signModel, woodType, x0, y0, x1, y1, scale, this.scissorStack.peek())
            );
    }

    public void submitProfilerChartRenderState(List<ResultField> chartData, int x0, int y0, int x1, int y1) {
        this.guiRenderState
            .submitPicturesInPictureState(new GuiProfilerChartRenderState(chartData, x0, y0, x1, y1, this.scissorStack.peek()));
    }

    /**
     * Neo: Submit a custom {@link net.minecraft.client.gui.render.state.GuiElementRenderState} for rendering
     */
    public void submitGuiElementRenderState(net.minecraft.client.gui.render.state.GuiElementRenderState renderState) {
        this.guiRenderState.submitGuiElement(renderState);
    }

    /**
     * Neo: Submit a custom {@link net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState} for rendering
     *
     * @see net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent
     */
    public void submitPictureInPictureRenderState(net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState renderState) {
        this.guiRenderState.submitPicturesInPictureState(renderState);
    }

    /**
     * Neo: Returns the top-most scissor rectangle, if present, for use with custom {@link net.minecraft.client.gui.render.state.GuiElementRenderState}s
     * and {@link net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState}s
     */
    @Nullable
    public ScreenRectangle peekScissorStack() {
        return this.scissorStack.peek();
    }

    public TextureAtlasSprite getSprite(Material material) {
        return this.materials.get(material);
    }

    @OnlyIn(Dist.CLIENT)
    record OutlineBox(int x, int y, int width, int height, int color) {
        public void render(GuiGraphics guiGraphics) {
            guiGraphics.fill(this.x, this.y, this.x + this.width, this.y + 1, this.color);
            guiGraphics.fill(this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, this.color);
            guiGraphics.fill(this.x, this.y + 1, this.x + 1, this.y + this.height - 1, this.color);
            guiGraphics.fill(this.x + this.width - 1, this.y + 1, this.x + this.width, this.y + this.height - 1, this.color);
        }
    }

    /**
     * A utility class for managing a stack of screen rectangles for scissoring.
     */
    @OnlyIn(Dist.CLIENT)
    static class ScissorStack {
        private final Deque<ScreenRectangle> stack = new ArrayDeque<>();

        /**
         * Pushes a screen rectangle onto the scissor stack.
         * <p>
         * @return The resulting intersection of the pushed rectangle with the previous top rectangle on the stack, or the pushed rectangle if the stack is empty.
         *
         * @param scissor the screen rectangle to push.
         */
        public ScreenRectangle push(ScreenRectangle scissor) {
            ScreenRectangle screenrectangle = this.stack.peekLast();
            if (screenrectangle != null) {
                ScreenRectangle screenrectangle1 = Objects.requireNonNullElse(scissor.intersection(screenrectangle), ScreenRectangle.empty());
                this.stack.addLast(screenrectangle1);
                return screenrectangle1;
            } else {
                this.stack.addLast(scissor);
                return scissor;
            }
        }

        @Nullable
        public ScreenRectangle pop() {
            if (this.stack.isEmpty()) {
                throw new IllegalStateException("Scissor stack underflow");
            } else {
                this.stack.removeLast();
                return this.stack.peekLast();
            }
        }

        @Nullable
        public ScreenRectangle peek() {
            return this.stack.peekLast();
        }

        public boolean containsPoint(int x, int y) {
            return this.stack.isEmpty() ? true : this.stack.peek().containsPoint(x, y);
        }
    }
}
