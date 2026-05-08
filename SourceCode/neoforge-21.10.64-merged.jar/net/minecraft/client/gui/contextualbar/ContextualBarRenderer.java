package net.minecraft.client.gui.contextualbar;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ContextualBarRenderer {
    int WIDTH = 182;
    int HEIGHT = 5;
    int MARGIN_BOTTOM = 24;
    ContextualBarRenderer EMPTY = new ContextualBarRenderer() {
        @Override
        public void renderBackground(GuiGraphics p_416316_, DeltaTracker p_416270_) {
        }

        @Override
        public void render(GuiGraphics p_416536_, DeltaTracker p_415672_) {
        }
    };

    default int left(Window window) {
        return (window.getGuiScaledWidth() - 182) / 2;
    }

    default int top(Window window) {
        return window.getGuiScaledHeight() - 24 - 5;
    }

    void renderBackground(GuiGraphics guiGraphics, DeltaTracker deltaTracker);

    void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker);

    static void renderExperienceLevel(GuiGraphics guiGraphics, Font font, int level) {
        Component component = Component.translatable("gui.experience.level", level);
        int i = (guiGraphics.guiWidth() - font.width(component)) / 2;
        int j = guiGraphics.guiHeight() - 24 - 9 - 2;
        guiGraphics.drawString(font, component, i + 1, j, -16777216, false);
        guiGraphics.drawString(font, component, i - 1, j, -16777216, false);
        guiGraphics.drawString(font, component, i, j + 1, -16777216, false);
        guiGraphics.drawString(font, component, i, j - 1, -16777216, false);
        guiGraphics.drawString(font, component, i, j, -8323296, false);
    }
}
