// file: src/main/java/net/goui/cosmicdungeon/client/CosmicSpawnerHoverOverlay.java
package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.block.entity.CosmicSpawnerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

public final class CosmicSpawnerHoverOverlay {

    private static final int PADDING = 6;
    private static final int MARGIN = 12;
    private static final int LINE_HEIGHT = 10;

    private CosmicSpawnerHoverOverlay() {}

    /**
     * Client-only toggle for whether the developer spawner summary HUD is rendered.
     * Default: HIDE. Server controls this via SpawnerLabelPayload after developer checks.
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
        if (mc.screen != null || mc.getDebugOverlay().showDebugScreen()) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr)) return;
        if (!(mc.level.getBlockEntity(bhr.getBlockPos()) instanceof CosmicSpawnerBlockEntity be)) return;

        List<Component> lines = new ArrayList<>();
        if (SpawnerHudClientConfig.MOB_TYPE.get()) lines.add(Component.literal("Mob Type: " + be.getSpawnerDisplayEntityId()));
        if (SpawnerHudClientConfig.MOB_NAME.get()) lines.add(Component.literal("Mob Name: " + be.getSpawnerDisplayMobName()));
        if (SpawnerHudClientConfig.COORDINATES.get()) lines.add(Component.literal("Coordinates: " + bhr.getBlockPos().toShortString()));
        if (SpawnerHudClientConfig.CAP.get()) lines.add(Component.literal("Cap: " + formatCap(be.getSpawnerMobCap())));
        if (SpawnerHudClientConfig.DELAY.get()) lines.add(Component.literal("Delay: " + be.getSpawnerMinSpawnDelay() + "-" + be.getSpawnerMaxSpawnDelay() + " ticks"));
        if (lines.isEmpty()) return;

        int textWidth = lines.stream().mapToInt(mc.font::width).max().orElse(0);
        int boxWidth = Math.min(SpawnerHudClientConfig.MAX_WIDTH.get(), textWidth + (PADDING * 2));
        int boxHeight = (lines.size() * LINE_HEIGHT) + (PADDING * 2) - 2;
        SpawnerHudClientConfig.Anchor anchor = SpawnerHudClientConfig.anchor();
        int edgeOffsetX = MARGIN + SpawnerHudClientConfig.HORIZONTAL_OFFSET.get();
        int edgeOffsetY = MARGIN + SpawnerHudClientConfig.VERTICAL_OFFSET.get();
        int x = anchor.left()
                ? edgeOffsetX
                : mc.getWindow().getGuiScaledWidth() - boxWidth - edgeOffsetX;
        int y = anchor.top()
                ? edgeOffsetY
                : mc.getWindow().getGuiScaledHeight() - boxHeight - edgeOffsetY;

        e.getGuiGraphics().fill(x, y, x + boxWidth, y + boxHeight, SpawnerHudClientConfig.backgroundColor());
        e.getGuiGraphics().fill(x, y, x + boxWidth, y + 1, SpawnerHudClientConfig.borderColor());
        e.getGuiGraphics().fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, SpawnerHudClientConfig.borderColor());

        int lineY = y + PADDING;
        for (Component line : lines) {
            e.getGuiGraphics().drawString(mc.font, line, x + PADDING, lineY, 0xFFFFFFFF, true);
            lineY += LINE_HEIGHT;
        }
    }

    private static String formatCap(int cap) {
        return cap <= 0 ? "uncapped" : Integer.toString(cap);
    }
}
