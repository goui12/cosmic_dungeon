package net.minecraft.client.multiplayer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientExplosionTracker {
    private static final int MAX_PARTICLES_PER_TICK = 512;
    private final List<ClientExplosionTracker.ExplosionInfo> explosions = new ArrayList<>();

    public void track(Vec3 center, float radius, int blockCount, WeightedList<ExplosionParticleInfo> blockParticles) {
        if (!blockParticles.isEmpty()) {
            this.explosions.add(new ClientExplosionTracker.ExplosionInfo(center, radius, blockCount, blockParticles));
        }
    }

    public void tick(ClientLevel level) {
        if (Minecraft.getInstance().options.particles().get() != ParticleStatus.ALL) {
            this.explosions.clear();
        } else {
            int i = WeightedRandom.getTotalWeight(this.explosions, ClientExplosionTracker.ExplosionInfo::blockCount);
            int j = Math.min(i, 512);

            for (int k = 0; k < j; k++) {
                WeightedRandom.getRandomItem(level.getRandom(), this.explosions, i, ClientExplosionTracker.ExplosionInfo::blockCount)
                    .ifPresent(p_437218_ -> this.addParticle(level, p_437218_));
            }

            this.explosions.clear();
        }
    }

    private void addParticle(ClientLevel level, ClientExplosionTracker.ExplosionInfo explosionInfo) {
        RandomSource randomsource = level.getRandom();
        Vec3 vec3 = explosionInfo.center();
        Vec3 vec31 = new Vec3(randomsource.nextFloat() * 2.0F - 1.0F, randomsource.nextFloat() * 2.0F - 1.0F, randomsource.nextFloat() * 2.0F - 1.0F)
            .normalize();
        float f = (float)Math.cbrt(randomsource.nextFloat()) * explosionInfo.radius();
        Vec3 vec32 = vec31.scale(f);
        Vec3 vec33 = vec3.add(vec32);
        if (level.getBlockState(BlockPos.containing(vec33)).isAir()) {
            float f1 = 0.5F / (f / explosionInfo.radius() + 0.1F) * randomsource.nextFloat() * randomsource.nextFloat() + 0.3F;
            ExplosionParticleInfo explosionparticleinfo = explosionInfo.blockParticles.getRandomOrThrow(randomsource);
            Vec3 vec34 = vec3.add(vec32.scale(explosionparticleinfo.scaling()));
            Vec3 vec35 = vec31.scale(f1 * explosionparticleinfo.speed());
            level.addParticle(explosionparticleinfo.particle(), vec34.x(), vec34.y(), vec34.z(), vec35.x(), vec35.y(), vec35.z());
        }
    }

    @OnlyIn(Dist.CLIENT)
    record ExplosionInfo(Vec3 center, float radius, int blockCount, WeightedList<ExplosionParticleInfo> blockParticles) {
    }
}
