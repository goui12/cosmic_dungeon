package net.minecraft.client.gui.components;

import java.time.Duration;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.BelowOrAboveWidgetTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.MenuTooltipPositioner;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WidgetTooltipHolder {
    @Nullable
    private Tooltip tooltip;
    private Duration delay = Duration.ZERO;
    private long displayStartTime;
    private boolean wasDisplayed;

    public void setDelay(Duration delay) {
        this.delay = delay;
    }

    public void set(@Nullable Tooltip tooltip) {
        this.tooltip = tooltip;
    }

    @Nullable
    public Tooltip get() {
        return this.tooltip;
    }

    public void refreshTooltipForNextRenderPass(
        GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovering, boolean focused, ScreenRectangle screenRectangle
    ) {
        if (this.tooltip == null) {
            this.wasDisplayed = false;
        } else {
            Minecraft minecraft = Minecraft.getInstance();
            boolean flag = hovering || focused && minecraft.getLastInputType().isKeyboard();
            if (flag != this.wasDisplayed) {
                if (flag) {
                    this.displayStartTime = Util.getMillis();
                }

                this.wasDisplayed = flag;
            }

            if (flag && Util.getMillis() - this.displayStartTime > this.delay.toMillis()) {
                guiGraphics.setTooltipForNextFrame(
                    minecraft.font,
                    this.tooltip.toCharSequence(minecraft),
                    this.createTooltipPositioner(screenRectangle, hovering, focused),
                    mouseX,
                    mouseY,
                    focused
                );
            }
        }
    }

    private ClientTooltipPositioner createTooltipPositioner(ScreenRectangle screenRectangle, boolean hovering, boolean focused) {
        return (ClientTooltipPositioner)(!hovering && focused && Minecraft.getInstance().getLastInputType().isKeyboard()
            ? new BelowOrAboveWidgetTooltipPositioner(screenRectangle)
            : new MenuTooltipPositioner(screenRectangle));
    }

    public void updateNarration(NarrationElementOutput output) {
        if (this.tooltip != null) {
            this.tooltip.updateNarration(output);
        }
    }
}
