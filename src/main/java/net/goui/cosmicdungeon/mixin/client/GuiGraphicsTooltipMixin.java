package net.goui.cosmicdungeon.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.goui.cosmicdungeon.client.screen.DeferredTooltipAccess;
import net.goui.cosmicdungeon.client.screen.TooltipRenderPass;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.*;

/** Keep native outlines at their original pass; only hold the tooltip until Post overlays finish. */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsTooltipMixin implements DeferredTooltipAccess {
    @Shadow private Runnable deferredTooltip;
    @Unique private final TooltipRenderPass cosmicdungeon$tooltips = new TooltipRenderPass();
    @Override public TooltipRenderPass cosmicdungeon$tooltipPass() { return cosmicdungeon$tooltips; }
    @Override public void cosmicdungeon$clearTooltip() { deferredTooltip = null; }

    @WrapMethod(method = "renderDeferredElements")
    private void cosmicdungeon$holdTooltip(Operation<Void> original) {
        if (!cosmicdungeon$tooltips.deferred()) { original.call(); return; }
        Runnable held = deferredTooltip;
        deferredTooltip = null;
        try { original.call(); }
        finally { deferredTooltip = held; }
    }
}
