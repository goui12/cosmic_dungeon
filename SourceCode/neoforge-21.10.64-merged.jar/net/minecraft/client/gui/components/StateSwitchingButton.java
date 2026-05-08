package net.minecraft.client.gui.components;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StateSwitchingButton extends AbstractWidget {
    @Nullable
    protected WidgetSprites sprites;
    protected boolean isStateTriggered;

    public StateSwitchingButton(int x, int y, int width, int height, boolean initialState) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.isStateTriggered = initialState;
    }

    public void initTextureValues(WidgetSprites sprites) {
        this.sprites = sprites;
    }

    public void setStateTriggered(boolean triggered) {
        this.isStateTriggered = triggered;
    }

    public boolean isStateTriggered() {
        return this.isStateTriggered;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.sprites != null) {
            guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                this.sprites.get(this.isStateTriggered, this.isHoveredOrFocused()),
                this.getX(),
                this.getY(),
                this.width,
                this.height
            );
        }
    }
}
