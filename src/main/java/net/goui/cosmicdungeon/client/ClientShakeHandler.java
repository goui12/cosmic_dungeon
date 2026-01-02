// file: src/main/java/net/goui/cosmicdungeon/client/ClientShakeHandler.java
package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class ClientShakeHandler {
    private static final RandomSource RANDOM = RandomSource.create();

    private static boolean shaking = false;
    private static long startTimeMs = 0L;
    private static long endTimeMs   = 0L;
    private static float intensity  = 1.0F;

    private ClientShakeHandler() {}

    // Called from ModNetwork payload handler (client thread)
    public static void startShake(final float durationSeconds, final float intensity) {
        ClientShakeHandler.shaking = true;
        ClientShakeHandler.intensity = intensity;
        ClientShakeHandler.startTimeMs = System.currentTimeMillis();
        ClientShakeHandler.endTimeMs = startTimeMs + (long) (durationSeconds * 1000.0F);
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(final ViewportEvent.ComputeCameraAngles event) {
        if (!shaking) return;

        long now = System.currentTimeMillis();
        if (now >= endTimeMs) {
            shaking = false;
            return;
        }

        float progress = (now - startTimeMs) / (float) (endTimeMs - startTimeMs);
        float falloff = 1.0F - progress; // fade out

        float maxYaw   = 8.0F  * intensity;
        float maxPitch = 6.0F  * intensity;
        float maxRoll  = 12.0F * intensity;

        float yawOffset   = (RANDOM.nextFloat() * 2.0F - 1.0F) * maxYaw   * falloff;
        float pitchOffset = (RANDOM.nextFloat() * 2.0F - 1.0F) * maxPitch * falloff;
        float rollOffset  = (RANDOM.nextFloat() * 2.0F - 1.0F) * maxRoll  * falloff;

        event.setYaw(event.getYaw() + yawOffset);
        event.setPitch(event.getPitch() + pitchOffset);
        event.setRoll(event.getRoll() + rollOffset);
    }
}
