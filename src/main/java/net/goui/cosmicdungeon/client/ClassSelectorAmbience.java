package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.custom.D1_Class_Selector_Block;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** At most two native particles per eight client ticks, for one focused selector only. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class ClassSelectorAmbience {
    private static final ClassSelectorAmbience INSTANCE = new ClassSelectorAmbience();
    private int cooldown;

    private ClassSelectorAmbience() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        INSTANCE.update(Minecraft.getInstance());
    }

    private void update(Minecraft client) {
        if (cooldown > 0) cooldown--;
        if (client.level == null || client.player == null || client.screen != null || client.isPaused()
                || client.options.particles().get() == ParticleStatus.MINIMAL
                || !(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || !(client.level.getBlockState(hit.getBlockPos()).getBlock() instanceof D1_Class_Selector_Block)) {
            return;
        }
        if (cooldown > 0) return;
        cooldown = client.options.particles().get() == ParticleStatus.DECREASED ? 16 : 8;
        var pos = hit.getBlockPos();
        var random = client.level.random;
        double angle = random.nextDouble() * Math.PI * 2;
        double x = pos.getX() + 0.5 + Math.cos(angle) * 0.38;
        double z = pos.getZ() + 0.5 + Math.sin(angle) * 0.38;
        client.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, pos.getY() + 0.4, z,
                0, 0.018, 0);
        client.level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.0,
                pos.getZ() + 0.5, Math.cos(angle) * 0.65, 0.6 + random.nextDouble() * 0.3,
                Math.sin(angle) * 0.65);
    }
}
