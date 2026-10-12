package net.goui.cosmicdungeon.client.screen;

/** Client-only bridge implemented by GuiGraphics; native tooltip rendering stays authoritative. */
public interface DeferredTooltipAccess {
    TooltipRenderPass cosmicdungeon$tooltipPass();
    void cosmicdungeon$clearTooltip();
}
