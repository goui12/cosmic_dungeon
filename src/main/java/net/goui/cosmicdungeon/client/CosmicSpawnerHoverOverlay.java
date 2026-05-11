// file: src/main/java/net/goui/cosmicdungeon/client/CosmicSpawnerHoverOverlay.java
package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.block.entity.CosmicSpawnerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class CosmicSpawnerHoverOverlay {

    private CosmicSpawnerHoverOverlay() {}

    /**
     * Client-only toggle for whether spawner labels are rendered.
     * Default: HIDE.
     *
     * Server controls this via SpawnerLabelPayload (developer-only command).
     *
     * NOTE:
     * We store the actual value in SpawnerLabelState so common/network code can set it
     * without relying on reflective dispatch.
     */
    public static boolean isEnabled() {
        return SpawnerLabelState.isEnabled();
    }

    public static void setEnabled(boolean enabled) {
        SpawnerLabelState.setEnabled(enabled);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post e) {
        if (!SpawnerLabelState.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr)) return;

        if (!(mc.level.getBlockEntity(bhr.getBlockPos()) instanceof CosmicSpawnerBlockEntity be)) return;

        Component text = Component.translatable("hud.cosmicdungeon.spawner", be.getSpawnerDisplayEntityId());

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        // Centered, slightly above crosshair
        int x = (w / 2) - (mc.font.width(text) / 2);
        int y = (h / 2) - 20;

        e.getGuiGraphics().drawString(mc.font, text, x, y, 0xFFFFFF, true);
    }
}