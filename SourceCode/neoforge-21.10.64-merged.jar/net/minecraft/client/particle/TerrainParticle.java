package net.minecraft.client.particle;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TerrainParticle extends SingleQuadParticle {
    private final BlockPos pos;
    private final float uo;
    private final float vo;

    public TerrainParticle(
        ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, BlockState state
    ) {
        this(level, x, y, z, xSpeed, ySpeed, zSpeed, state, BlockPos.containing(x, y, z));
    }

    public TerrainParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        BlockState state,
        BlockPos pos
    ) {
        super(
            level,
            x,
            y,
            z,
            xSpeed,
            ySpeed,
            zSpeed,
            Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state)
        );
        this.pos = pos;
        this.gravity = 1.0F;
        this.rCol = 0.6F;
        this.gCol = 0.6F;
        this.bCol = 0.6F;
        if (net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions.of(state).areBreakingParticlesTinted(state, level, pos)) {
            int i = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
            this.rCol *= (i >> 16 & 0xFF) / 255.0F;
            this.gCol *= (i >> 8 & 0xFF) / 255.0F;
            this.bCol *= (i & 0xFF) / 255.0F;
        }

        this.quadSize /= 2.0F;
        this.uo = this.random.nextFloat() * 3.0F;
        this.vo = this.random.nextFloat() * 3.0F;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TERRAIN;
    }

    @Override
    protected float getU0() {
        return this.sprite.getU((this.uo + 1.0F) / 4.0F);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU(this.uo / 4.0F);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(this.vo / 4.0F);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV((this.vo + 1.0F) / 4.0F);
    }

    @Override
    public int getLightColor(float partialTick) {
        int i = super.getLightColor(partialTick);
        return i == 0 && this.level.hasChunkAt(this.pos) ? LevelRenderer.getLightColor(this.level, this.pos) : i;
    }

    @Nullable
    static TerrainParticle createTerrainParticle(
        BlockParticleOption type,
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed
    ) {
        BlockState blockstate = type.getState();
        return !blockstate.isAir() && !blockstate.is(Blocks.MOVING_PISTON) && blockstate.shouldSpawnTerrainParticles()
            ? new TerrainParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, blockstate).updateSprite(blockstate, type.getPos())
            : null;
    }

    public TerrainParticle updateSprite(BlockState state, BlockPos pos) { //FORGE: we cannot assume that the x y z of the particles match the block pos of the block.
        if (pos != null) // There are cases where we are not able to obtain the correct source pos, and need to fallback to the non-model data version
            this.setSprite(Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state, level, pos));
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    public static class CrumblingProvider implements ParticleProvider<BlockParticleOption> {
        @Nullable
        public Particle createParticle(
            BlockParticleOption p_379844_,
            ClientLevel p_379918_,
            double p_380066_,
            double p_379966_,
            double p_379761_,
            double p_380172_,
            double p_379630_,
            double p_379393_,
            RandomSource p_446990_
        ) {
            Particle particle = TerrainParticle.createTerrainParticle(p_379844_, p_379918_, p_380066_, p_379966_, p_379761_, p_380172_, p_379630_, p_379393_);
            if (particle != null) {
                particle.setParticleSpeed(0.0, 0.0, 0.0);
                particle.setLifetime(p_446990_.nextInt(10) + 1);
            }

            return particle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DustPillarProvider implements ParticleProvider<BlockParticleOption> {
        @Nullable
        public Particle createParticle(
            BlockParticleOption p_338199_,
            ClientLevel p_338462_,
            double p_338552_,
            double p_338714_,
            double p_338211_,
            double p_338881_,
            double p_338238_,
            double p_338376_,
            RandomSource p_446719_
        ) {
            Particle particle = TerrainParticle.createTerrainParticle(p_338199_, p_338462_, p_338552_, p_338714_, p_338211_, p_338881_, p_338238_, p_338376_);
            if (particle != null) {
                particle.setParticleSpeed(p_446719_.nextGaussian() / 30.0, p_338238_ + p_446719_.nextGaussian() / 2.0, p_446719_.nextGaussian() / 30.0);
                particle.setLifetime(p_446719_.nextInt(20) + 20);
            }

            return particle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<BlockParticleOption> {
        @Nullable
        public Particle createParticle(
            BlockParticleOption p_108304_,
            ClientLevel p_108305_,
            double p_108306_,
            double p_108307_,
            double p_108308_,
            double p_108309_,
            double p_108310_,
            double p_108311_,
            RandomSource p_447039_
        ) {
            return TerrainParticle.createTerrainParticle(p_108304_, p_108305_, p_108306_, p_108307_, p_108308_, p_108309_, p_108310_, p_108311_);
        }
    }
}
