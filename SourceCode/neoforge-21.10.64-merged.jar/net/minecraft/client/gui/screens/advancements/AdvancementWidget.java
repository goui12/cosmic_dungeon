package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AdvancementWidget {
    private static final ResourceLocation TITLE_BOX_SPRITE = ResourceLocation.withDefaultNamespace("advancements/title_box");
    private static final int HEIGHT = 26;
    private static final int BOX_X = 0;
    private static final int BOX_WIDTH = 200;
    private static final int FRAME_WIDTH = 26;
    private static final int ICON_X = 8;
    private static final int ICON_Y = 5;
    private static final int ICON_WIDTH = 26;
    private static final int TITLE_PADDING_LEFT = 3;
    private static final int TITLE_PADDING_RIGHT = 5;
    private static final int TITLE_X = 32;
    private static final int TITLE_PADDING_TOP = 9;
    private static final int TITLE_PADDING_BOTTOM = 8;
    private static final int TITLE_MAX_WIDTH = 163;
    private static final int TITLE_MIN_WIDTH = 80;
    private static final int[] TEST_SPLIT_OFFSETS = new int[]{0, 10, -10, 25, -25};
    private final AdvancementTab tab;
    private final AdvancementNode advancementNode;
    private final DisplayInfo display;
    private final List<FormattedCharSequence> titleLines;
    private final int width;
    private final List<FormattedCharSequence> description;
    private final Minecraft minecraft;
    @Nullable
    private AdvancementWidget parent;
    private final List<AdvancementWidget> children = Lists.newArrayList();
    @Nullable
    private AdvancementProgress progress;
    private final int x;
    private final int y;

    public AdvancementWidget(AdvancementTab tab, Minecraft minecraft, AdvancementNode advancementNode, DisplayInfo display) {
        this.tab = tab;
        this.advancementNode = advancementNode;
        this.display = display;
        this.minecraft = minecraft;
        this.titleLines = minecraft.font.split(display.getTitle(), 163);
        this.x = Mth.floor(display.getX() * 28.0F);
        this.y = Mth.floor(display.getY() * 27.0F);
        int i = Math.max(this.titleLines.stream().mapToInt(minecraft.font::width).max().orElse(0), 80);
        int j = this.getMaxProgressWidth();
        int k = 29 + i + j;
        this.description = Language.getInstance()
            .getVisualOrder(
                this.findOptimalLines(ComponentUtils.mergeStyles(display.getDescription().copy(), Style.EMPTY.withColor(display.getType().getChatColor())), k)
            );

        for (FormattedCharSequence formattedcharsequence : this.description) {
            k = Math.max(k, minecraft.font.width(formattedcharsequence));
        }

        this.width = k + 3 + 5;
    }

    private int getMaxProgressWidth() {
        int i = this.advancementNode.advancement().requirements().size();
        if (i <= 1) {
            return 0;
        } else {
            int j = 8;
            Component component = Component.translatable("advancements.progress", i, i);
            return this.minecraft.font.width(component) + 8;
        }
    }

    private static float getMaxWidth(StringSplitter manager, List<FormattedText> text) {
        return (float)text.stream().mapToDouble(manager::stringWidth).max().orElse(0.0);
    }

    private List<FormattedText> findOptimalLines(Component component, int maxWidth) {
        StringSplitter stringsplitter = this.minecraft.font.getSplitter();
        List<FormattedText> list = null;
        float f = Float.MAX_VALUE;

        for (int i : TEST_SPLIT_OFFSETS) {
            List<FormattedText> list1 = stringsplitter.splitLines(component, maxWidth - i, Style.EMPTY);
            float f1 = Math.abs(getMaxWidth(stringsplitter, list1) - maxWidth);
            if (f1 <= 10.0F) {
                return list1;
            }

            if (f1 < f) {
                f = f1;
                list = list1;
            }
        }

        return list;
    }

    @Nullable
    private AdvancementWidget getFirstVisibleParent(AdvancementNode advancement) {
        do {
            advancement = advancement.parent();
        } while (advancement != null && advancement.advancement().display().isEmpty());

        return advancement != null && !advancement.advancement().display().isEmpty() ? this.tab.getWidget(advancement.holder()) : null;
    }

    public void drawConnectivity(GuiGraphics guiGraphics, int x, int y, boolean dropShadow) {
        if (this.parent != null) {
            int i = x + this.parent.x + 13;
            int j = x + this.parent.x + 26 + 4;
            int k = y + this.parent.y + 13;
            int l = x + this.x + 13;
            int i1 = y + this.y + 13;
            int j1 = dropShadow ? -16777216 : -1;
            if (dropShadow) {
                guiGraphics.hLine(j, i, k - 1, j1);
                guiGraphics.hLine(j + 1, i, k, j1);
                guiGraphics.hLine(j, i, k + 1, j1);
                guiGraphics.hLine(l, j - 1, i1 - 1, j1);
                guiGraphics.hLine(l, j - 1, i1, j1);
                guiGraphics.hLine(l, j - 1, i1 + 1, j1);
                guiGraphics.vLine(j - 1, i1, k, j1);
                guiGraphics.vLine(j + 1, i1, k, j1);
            } else {
                guiGraphics.hLine(j, i, k, j1);
                guiGraphics.hLine(l, j, i1, j1);
                guiGraphics.vLine(j, i1, k, j1);
            }
        }

        for (AdvancementWidget advancementwidget : this.children) {
            advancementwidget.drawConnectivity(guiGraphics, x, y, dropShadow);
        }
    }

    public void draw(GuiGraphics guiGraphics, int x, int y) {
        if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
            float f = this.progress == null ? 0.0F : this.progress.getPercent();
            AdvancementWidgetType advancementwidgettype;
            if (f >= 1.0F) {
                advancementwidgettype = AdvancementWidgetType.OBTAINED;
            } else {
                advancementwidgettype = AdvancementWidgetType.UNOBTAINED;
            }

            guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, advancementwidgettype.frameSprite(this.display.getType()), x + this.x + 3, y + this.y, 26, 26
            );
            guiGraphics.renderFakeItem(this.display.getIcon(), x + this.x + 8, y + this.y + 5);
        }

        for (AdvancementWidget advancementwidget : this.children) {
            advancementwidget.draw(guiGraphics, x, y);
        }
    }

    public int getWidth() {
        return this.width;
    }

    public void setProgress(AdvancementProgress progress) {
        this.progress = progress;
    }

    public void addChild(AdvancementWidget advancementWidget) {
        this.children.add(advancementWidget);
    }

    public void drawHover(GuiGraphics guiGraphics, int x, int y, float fade, int width, int height) {
        Font font = this.minecraft.font;
        int i = 9 * this.titleLines.size() + 9 + 8;
        int j = y + this.y + (26 - i) / 2;
        int k = j + i;
        int l = this.description.size() * 9;
        int i1 = 6 + l;
        boolean flag = width + x + this.x + this.width + 26 >= this.tab.getScreen().width;
        Component component = this.progress == null ? null : this.progress.getProgressText();
        int j1 = component == null ? 0 : font.width(component);
        boolean flag1 = k + i1 >= 113;
        float f = this.progress == null ? 0.0F : this.progress.getPercent();
        int k1 = Mth.floor(f * this.width);
        AdvancementWidgetType advancementwidgettype;
        AdvancementWidgetType advancementwidgettype1;
        AdvancementWidgetType advancementwidgettype2;
        if (f >= 1.0F) {
            k1 = this.width / 2;
            advancementwidgettype = AdvancementWidgetType.OBTAINED;
            advancementwidgettype1 = AdvancementWidgetType.OBTAINED;
            advancementwidgettype2 = AdvancementWidgetType.OBTAINED;
        } else if (k1 < 2) {
            k1 = this.width / 2;
            advancementwidgettype = AdvancementWidgetType.UNOBTAINED;
            advancementwidgettype1 = AdvancementWidgetType.UNOBTAINED;
            advancementwidgettype2 = AdvancementWidgetType.UNOBTAINED;
        } else if (k1 > this.width - 2) {
            k1 = this.width / 2;
            advancementwidgettype = AdvancementWidgetType.OBTAINED;
            advancementwidgettype1 = AdvancementWidgetType.OBTAINED;
            advancementwidgettype2 = AdvancementWidgetType.UNOBTAINED;
        } else {
            advancementwidgettype = AdvancementWidgetType.OBTAINED;
            advancementwidgettype1 = AdvancementWidgetType.UNOBTAINED;
            advancementwidgettype2 = AdvancementWidgetType.UNOBTAINED;
        }

        int l1 = this.width - k1;
        int i2;
        if (flag) {
            i2 = x + this.x - this.width + 26 + 6;
        } else {
            i2 = x + this.x;
        }

        int j2 = i + i1;
        if (!this.description.isEmpty()) {
            if (flag1) {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TITLE_BOX_SPRITE, i2, k - j2, this.width, j2);
            } else {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TITLE_BOX_SPRITE, i2, j, this.width, j2);
            }
        }

        if (advancementwidgettype != advancementwidgettype1) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, advancementwidgettype.boxSprite(), 200, i, 0, 0, i2, j, k1, i);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, advancementwidgettype1.boxSprite(), 200, i, 200 - l1, 0, i2 + k1, j, l1, i);
        } else {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, advancementwidgettype.boxSprite(), i2, j, this.width, i);
        }

        guiGraphics.blitSprite(
            RenderPipelines.GUI_TEXTURED, advancementwidgettype2.frameSprite(this.display.getType()), x + this.x + 3, y + this.y, 26, 26
        );
        int k2 = i2 + 5;
        if (flag) {
            this.drawMultilineText(guiGraphics, this.titleLines, k2, j + 9, -1);
            if (component != null) {
                guiGraphics.drawString(font, component, x + this.x - j1, j + 9, -1);
            }
        } else {
            this.drawMultilineText(guiGraphics, this.titleLines, x + this.x + 32, j + 9, -1);
            if (component != null) {
                guiGraphics.drawString(font, component, x + this.x + this.width - j1 - 5, j + 9, -1);
            }
        }

        if (flag1) {
            this.drawMultilineText(guiGraphics, this.description, k2, j - l + 1, -16711936);
        } else {
            this.drawMultilineText(guiGraphics, this.description, k2, k, -16711936);
        }

        guiGraphics.renderFakeItem(this.display.getIcon(), x + this.x + 8, y + this.y + 5);
    }

    private void drawMultilineText(GuiGraphics guiGraphics, List<FormattedCharSequence> text, int x, int y, int color) {
        Font font = this.minecraft.font;

        for (int i = 0; i < text.size(); i++) {
            guiGraphics.drawString(font, text.get(i), x, y + i * 9, color);
        }
    }

    public boolean isMouseOver(int x, int y, int mouseX, int mouseY) {
        if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
            int i = x + this.x;
            int j = i + 26;
            int k = y + this.y;
            int l = k + 26;
            return mouseX >= i && mouseX <= j && mouseY >= k && mouseY <= l;
        } else {
            return false;
        }
    }

    public void attachToParent() {
        if (this.parent == null && this.advancementNode.parent() != null) {
            this.parent = this.getFirstVisibleParent(this.advancementNode);
            if (this.parent != null) {
                this.parent.addChild(this);
            }
        }
    }

    public int getY() {
        return this.y;
    }

    public int getX() {
        return this.x;
    }
}
