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

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr)) return;

        if (!(mc.level.getBlockEntity(bhr.getBlockPos()) instanceof CosmicSpawnerBlockEntity be)) return;

        Component text = Component.translatable("hud.cosmicdungeon.spawner", be.getSpawnerEntityId());

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        // Centered, slightly above crosshair
        int x = (w / 2) - (mc.font.width(text) / 2);
        int y = (h / 2) - 20;

        e.getGuiGraphics().drawString(mc.font, text, x, y, 0xFFFFFF, true);
    }
}
