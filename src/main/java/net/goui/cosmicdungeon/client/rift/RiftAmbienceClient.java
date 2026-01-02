package net.goui.cosmicdungeon.client.rift;

import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public class RiftAmbienceClient {

    private static final double MAX_DISTANCE = 16.0;
    private static final int SCAN_INTERVAL_TICKS = 10;

    // You export ~9s, we trigger every 8s -> ~1s overlap tail
    private static final int HUM_INTERVAL_TICKS = 8 * 20;

    private static final int SCREAM_MIN_TICKS = 12 * 20;
    private static final int SCREAM_MAX_TICKS = 35 * 20;

    private BlockPos currentAnchorPos;

    private int scanCooldown = 0;
    private int humCooldown = 0;
    private int screamCooldown = 0;

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        LocalPlayer player = mc.player;
        Level level = mc.level;

        if (player == null || level == null) {
            currentAnchorPos = null;
            humCooldown = 0;
            screamCooldown = 0;
            return;
        }

        if (scanCooldown-- <= 0) {
            scanCooldown = SCAN_INTERVAL_TICKS;

            BlockPos best = findClosestAnchorTile(level, player.position(), MAX_DISTANCE);

            if (best == null) {
                currentAnchorPos = null;
                humCooldown = 0;     // reset so hum starts immediately next time you enter range
                screamCooldown = 0;
            } else {
                currentAnchorPos = best;
            }
        }

        if (currentAnchorPos != null) {
            tickHum(mc, player);
            tickScream(level, player);
        } else {
            humCooldown = 0;
            screamCooldown = 0;
        }
    }

    private void tickHum(Minecraft mc, LocalPlayer player) {
        if (humCooldown-- > 0) return;

        Vec3 anchor = Vec3.atCenterOf(currentAnchorPos);
        double d = player.position().distanceTo(anchor);

        // If somehow we have an anchor but player is out of range, wait a bit and re-check
        if (d > MAX_DISTANCE) {
            humCooldown = 10;
            return;
        }

        humCooldown = HUM_INTERVAL_TICKS;

        // Volume curve (feel free to tweak)
        float t = (float) Mth.clamp(1.0 - (d / MAX_DISTANCE), 0.0, 1.0);
        float vol = 0.10f + 0.90f * (t * t);  // quiet far, strong close

        // Subtle pitch drift helps hide repetition if desired
        float pitch = 0.985f + (player.level().random.nextFloat() * 0.03f);

        mc.getSoundManager().play(new RiftHumOneShotSoundInstance(anchor, vol, pitch));
    }

    private void tickScream(Level level, LocalPlayer player) {
        if (screamCooldown-- > 0) return;

        Vec3 anchor = Vec3.atCenterOf(currentAnchorPos);
        double d = player.position().distanceTo(anchor);

        if (d > 10.0) {
            screamCooldown = Mth.nextInt(level.random, 40, 80);
            return;
        }

        screamCooldown = Mth.nextInt(level.random, SCREAM_MIN_TICKS, SCREAM_MAX_TICKS);

        float distanceT = (float) Mth.clamp(1.0 - (d / 10.0), 0.0, 1.0);
        float vol = 0.25f + 0.75f * distanceT;
        float pitch = 0.93f + (level.random.nextFloat() * 0.14f);

        level.playLocalSound(
                anchor.x, anchor.y, anchor.z,
                ModSounds.RIFT_SCREAM.get(),
                SoundSource.AMBIENT,
                vol,
                pitch,
                false
        );
    }

    private static BlockPos findClosestAnchorTile(Level level, Vec3 playerPos, double radius) {
        int r = (int) Math.ceil(radius);

        BlockPos playerBlock = BlockPos.containing(playerPos);
        BlockPos bestPos = null;
        double bestD2 = radius * radius;

        for (int dy = -2; dy <= 2; dy++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    BlockPos p = playerBlock.offset(dx, dy, dz);
                    double d2 = playerPos.distanceToSqr(Vec3.atCenterOf(p));
                    if (d2 > bestD2) continue;

                    BlockState state = level.getBlockState(p);

                    // Only react to the ONE anchor tile: RIFT_MID_TOP2
                    if (state.getBlock() != ModBlocks.COSMIC_RIFT_TILE.get()) continue;


                    bestD2 = d2;
                    bestPos = p.immutable();
                }
            }
        }

        return bestPos;
    }
}
