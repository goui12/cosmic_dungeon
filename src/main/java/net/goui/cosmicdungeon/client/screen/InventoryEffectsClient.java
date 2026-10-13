package net.goui.cosmicdungeon.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;

/** Keep native effect information visible when the permanent potion list occupies the right column. */
@EventBusSubscriber(modid = "cosmicdungeon", value = Dist.CLIENT)
public final class InventoryEffectsClient {
    private record Frame(AbstractContainerScreen<?> screen, List<MobEffectInstance> effects, InventoryEffectsLayout layout) {}
    private static Frame frame;
    private InventoryEffectsClient() {}

    private static boolean supported(Screen candidate) {
        var mc = Minecraft.getInstance();
        if (!ClassInventoryPresentation.supports(candidate) || mc.screen != candidate || mc.player == null
                || mc.level == null || !mc.player.isAlive()) return false;
        var container = (AbstractContainerScreen<?>)candidate;
        var role = ClassInventoryPresentation.theme().classId();
        return mc.player.containerMenu == container.getMenu() && ("theurgist".equals(role) || "venefex".equals(role));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void prepare(ScreenEvent.Render.Pre event) {
        frame = null;
        if (!supported(event.getScreen())) return;
        var screen = (AbstractContainerScreen<?>)event.getScreen();
        var effects = Minecraft.getInstance().player.getActiveEffects().stream().filter(ClientHooks::shouldRenderEffect).sorted().toList();
        var layout = InventoryEffectsLayout.of(screen.getGuiLeft(), screen.getGuiTop(), effects.size());
        if (!effects.isEmpty() && layout.fits(screen.width, screen.height)) frame = new Frame(screen, effects, layout);
    }

    @SubscribeEvent
    public static void nativeColumn(ScreenEvent.RenderInventoryMobEffects event) {
        // This event has no GuiGraphics. Cancel only when the current render pass has a replacement row.
        if (frame != null && frame.screen() == event.getScreen()) event.setCanceled(true);
    }

    static Component effectName(MobEffectInstance effect) {
        var name = effect.getEffect().value().getDisplayName().copy();
        if (effect.getAmplifier() >= 1 && effect.getAmplifier() <= 9)
            name.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + (effect.getAmplifier() + 1)));
        return name;
    }

    private static List<Component> details(AbstractContainerScreen<?> screen, MobEffectInstance effect) {
        return ClientHooks.getEffectTooltip(screen, effect, List.of(effectName(effect),
                MobEffectUtil.formatDuration(effect, 1.0F, Minecraft.getInstance().level.tickRateManager().tickrate())));
    }

    private static void fill(net.minecraft.client.gui.GuiGraphics g, Rect box, int color) {
        g.fill(box.x(), box.y(), box.right(), box.bottom(), color);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void draw(ScreenEvent.Render.Post event) {
        var current = frame; frame = null;
        if (current == null || current.screen() != event.getScreen() || !supported(event.getScreen())) return;
        var g = event.getGuiGraphics(); var mc = Minecraft.getInstance(); var screen = current.screen();
        var layout = current.layout(); int mx = event.getMouseX(), my = event.getMouseY();
        boolean hovering = layout.row().contains(mx, my);
        boolean carrying = !screen.getMenu().getCarried().isEmpty()
                || !((net.goui.cosmicdungeon.mixin.client.InventoryDragAccess)screen).cosmicdungeon$draggingItem().isEmpty();
        if (hovering) ((DeferredTooltipAccess)g).cosmicdungeon$clearTooltip();
        g.nextStratum(); fill(g, layout.row(), 0xFF18232E);
        for (int i = 0; i < layout.icons().size(); i++) {
            var cell = layout.icons().get(i); var effect = current.effects().get(i);
            fill(g, cell, cell.contains(mx, my) && !carrying ? 0xFF45576A : 0xFF293744);
            if (!IClientMobEffectExtensions.of(effect).renderInventoryIcon(effect, screen, g, cell.x(), cell.y() + 1, 0))
                g.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(effect.getEffect()),
                        cell.x() + 1, cell.y() + 2, 16, 16);
            if (!carrying && cell.contains(mx, my)) g.setComponentTooltipForNextFrame(mc.font, details(screen, effect), mx, my);
        }
        if (layout.hidden() > 0) {
            var overflow = layout.overflow();
            fill(g, overflow, overflow.contains(mx, my) && !carrying ? 0xFF45576A : 0xFF293744);
            String label = layout.hidden() > 99 ? "+" : "+" + layout.hidden();
            g.drawCenteredString(mc.font, label, overflow.x() + overflow.width() / 2, overflow.y() + 6, 0xFFFFFFFF);
            if (!carrying && overflow.contains(mx, my)) {
                var lines = new ArrayList<Component>();
                lines.add(Component.literal("All active effects (" + current.effects().size() + ")"));
                for (var effect : current.effects()) {
                    var tooltip = details(screen, effect);
                    // One compact native name/duration line per effect; preserve extension-provided detail.
                    if (tooltip.size() >= 2) {
                        lines.add(tooltip.get(0).copy().append(": ").append(tooltip.get(1)));
                        lines.addAll(tooltip.subList(2, tooltip.size()));
                    } else lines.addAll(tooltip);
                }
                g.setComponentTooltipForNextFrame(mc.font, lines, mx, my);
            }
        }
        screen.renderCarriedItem(g, mx, my);
    }

    @SubscribeEvent public static void close(ScreenEvent.Closing event) { if (frame != null && frame.screen() == event.getScreen()) frame = null; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { frame = null; }
}
