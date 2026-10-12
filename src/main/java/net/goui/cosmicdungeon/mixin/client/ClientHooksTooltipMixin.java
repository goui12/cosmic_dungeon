package net.goui.cosmicdungeon.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.goui.cosmicdungeon.client.screen.DeferredTooltipAccess;
import net.goui.cosmicdungeon.client.screen.D1PartyHud;
import net.goui.cosmicdungeon.client.screen.skills.SkillsPanelClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;

/** Covers native items, widgets and every Cosmic Dungeon screen without replacing tooltip drawing. */
@Mixin(value = ClientHooks.class, remap = false)
public abstract class ClientHooksTooltipMixin {
    @WrapMethod(method = "drawScreenInternal", remap = false)
    private static void cosmicdungeon$finishTooltips(Screen screen, GuiGraphics graphics, int x, int y,
                                                    float partialTick, Operation<Void> original) {
        var access = (DeferredTooltipAccess) graphics;
        access.cosmicdungeon$tooltipPass().render(
                () -> original.call(screen, graphics, x, y, partialTick),
                () -> {
                    // Background layers, modal transitions and item/panel drags cannot leak hover tips.
                    if (Minecraft.getInstance().screen != screen
                            || SkillsPanelClient.suppressesTooltips(screen)
                            || SkillsPanelClient.inventory(screen) && D1PartyHud.dragging())
                        access.cosmicdungeon$clearTooltip();
                    graphics.renderDeferredElements();
                }, access::cosmicdungeon$clearTooltip);
    }
}
