package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DripParticle extends SingleQuadParticle {
    private final Fluid type;
    protected boolean isGlowing;

    public DripParticle(ClientLevel level, double x, double y, double z, Fluid type, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.setSize(0.01F, 0.01F);
        this.gravity = 0.06F;
        this.type = type;
    }

    protected Fluid getType() {
        return this.type;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return this.isGlowing ? 240 : super.getLightColor(partialTick);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.preMoveUpdate();
        if (!this.removed) {
            this.yd = this.yd - this.gravity;
            this.move(this.xd, this.yd, this.zd);
            this.postMoveUpdate();
            if (!this.removed) {
                this.xd *= 0.98F;
                this.yd *= 0.98F;
                this.zd *= 0.98F;
                if (this.type != Fluids.EMPTY) {
                    BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
                    FluidState fluidstate = this.level.getFluidState(blockpos);
                    if (fluidstate.getType() == this.type && this.y < blockpos.getY() + fluidstate.getHeight(this.level, blockpos)) {
                        this.remove();
                    }
                }
            }
        }
    }

    protected void preMoveUpdate() {
        if (this.lifetime-- <= 0) {
            this.remove();
        }
    }

    protected void postMoveUpdate() {
    }

    @OnlyIn(Dist.CLIENT)
    static class CoolingDripHangParticle extends DripParticle.DripHangParticle {
        public CoolingDripHangParticle(
            ClientLevel p_106068_,
            double p_106069_,
            double p_106070_,
            double p_106071_,
            Fluid p_106072_,
            ParticleOptions p_106073_,
            TextureAtlasSprite p_446665_
        ) {
            super(p_106068_, p_106069_, p_106070_, p_106071_, p_106072_, p_106073_, p_446665_);
        }

        @Override
        protected void preMoveUpdate() {
            this.rCol = 1.0F;
            this.gCol = 16.0F / (40 - this.lifetime + 16);
            this.bCol = 4.0F / (40 - this.lifetime + 8);
            super.preMoveUpdate();
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class DripHangParticle extends DripParticle {
        private final ParticleOptions fallingParticle;

        public DripHangParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            Fluid type,
            ParticleOptions fallingParticle,
            TextureAtlasSprite sprite
        ) {
            super(level, x, y, z, type, sprite);
            this.fallingParticle = fallingParticle;
            this.gravity *= 0.02F;
            this.lifetime = 40;
        }

        @Override
        protected void preMoveUpdate() {
            if (this.lifetime-- <= 0) {
                this.remove();
                this.level.addParticle(this.fallingParticle, this.x, this.y, this.z, this.xd, this.yd, this.zd);
            }
        }

        @Override
        protected void postMoveUpdate() {
            this.xd *= 0.02;
            this.yd *= 0.02;
            this.zd *= 0.02;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class DripLandParticle extends DripParticle {
        public DripLandParticle(ClientLevel p_106102_, double p_106103_, double p_106104_, double p_106105_, Fluid p_106106_, TextureAtlasSprite p_445778_) {
            super(p_106102_, p_106103_, p_106104_, p_106105_, p_106106_, p_445778_);
            this.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class DripstoneFallAndLandParticle extends DripParticle.FallAndLandParticle {
        public DripstoneFallAndLandParticle(
            ClientLevel p_171930_,
            double p_171931_,
            double p_171932_,
            double p_171933_,
            Fluid p_171934_,
            ParticleOptions p_171935_,
            TextureAtlasSprite p_445845_
        ) {
            super(p_171930_, p_171931_, p_171932_, p_171933_, p_171934_, p_171935_, p_445845_);
        }

        @Override
        protected void postMoveUpdate() {
            if (this.onGround) {
                this.remove();
                this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
                SoundEvent soundevent = this.getType() == Fluids.LAVA ? SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA : SoundEvents.POINTED_DRIPSTONE_DRIP_WATER;
                float f = Mth.randomBetween(this.random, 0.3F, 1.0F);
                this.level.playLocalSound(this.x, this.y, this.z, soundevent, SoundSource.BLOCKS, f, 1.0F, false);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DripstoneLavaFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DripstoneLavaFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            DripParticle dripparticle = new DripParticle.DripstoneFallAndLandParticle(
                level, x, y, z, Fluids.LAVA, ParticleTypes.LANDING_LAVA, this.sprite.get(random)
            );
            dripparticle.setColor(1.0F, 0.2857143F, 0.083333336F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DripstoneLavaHangProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DripstoneLavaHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            return new DripParticle.CoolingDripHangParticle(
                level, x, y, z, Fluids.LAVA, ParticleTypes.FALLING_DRIPSTONE_LAVA, this.sprite.get(random)
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DripstoneWaterFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DripstoneWaterFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            DripParticle dripparticle = new DripParticle.DripstoneFallAndLandParticle(
                level, x, y, z, Fluids.WATER, ParticleTypes.SPLASH, this.sprite.get(random)
            );
            dripparticle.setColor(0.2F, 0.3F, 1.0F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DripstoneWaterHangProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public DripstoneWaterHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            DripParticle dripparticle = new DripParticle.DripHangParticle(
                level, x, y, z, Fluids.WATER, ParticleTypes.FALLING_DRIPSTONE_WATER, this.sprite.get(random)
            );
            dripparticle.setColor(0.2F, 0.3F, 1.0F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class FallAndLandParticle extends DripParticle.FallingParticle {
        protected final ParticleOptions landParticle;

        public FallAndLandParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            Fluid type,
            ParticleOptions landParticle,
            TextureAtlasSprite sprite
        ) {
            super(level, x, y, z, type, sprite);
            this.landParticle = landParticle;
        }

        @Override
        protected void postMoveUpdate() {
            if (this.onGround) {
                this.remove();
                this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class FallingParticle extends DripParticle {
        public FallingParticle(ClientLevel level, double x, double y, double z, Fluid type, TextureAtlasSprite sprite) {
            this(level, x, y, z, type, (int)(64.0 / (Math.random() * 0.8 + 0.2)), sprite);
        }

        public FallingParticle(
            ClientLevel level, double x, double y, double z, Fluid type, int lifetime, TextureAtlasSprite sprite
        ) {
            super(level, x, y, z, type, sprite);
            this.lifetime = lifetime;
        }

        @Override
        protected void postMoveUpdate() {
            if (this.onGround) {
                this.remove();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class HoneyFallAndLandParticle extends DripParticle.FallAndLandParticle {
        public HoneyFallAndLandParticle(
            ClientLevel p_106146_,
            double p_106147_,
            double p_106148_,
            double p_106149_,
            Fluid p_106150_,
            ParticleOptions p_106151_,
            TextureAtlasSprite p_447239_
        ) {
            super(p_106146_, p_106147_, p_106148_, p_106149_, p_106150_, p_106151_, p_447239_);
        }

        @Override
        protected void postMoveUpdate() {
            if (this.onGround) {
                this.remove();
                this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
                float f = Mth.randomBetween(this.random, 0.3F, 1.0F);
                this.level.playLocalSound(this.x, this.y, this.z, SoundEvents.BEEHIVE_DRIP, SoundSource.BLOCKS, f, 1.0F, false);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class HoneyFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public HoneyFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            DripParticle dripparticle = new DripParticle.HoneyFallAndLandParticle(
                level, x, y, z, Fluids.EMPTY, ParticleTypes.LANDING_HONEY, this.sprite.get(random)
            );
            dripparticle.gravity = 0.01F;
            dripparticle.setColor(0.582F, 0.448F, 0.082F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class HoneyHangProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public HoneyHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            DripParticle.DripHangParticle dripparticle$driphangparticle = new DripParticle.DripHangParticle(
                level, x, y, z, Fluids.EMPTY, ParticleTypes.FALLING_HONEY, this.sprite.get(random)
            );
            dripparticle$driphangparticle.gravity *= 0.01F;
            dripparticle$driphangparticle.lifetime = 100;
            dripparticle$driphangparticle.setColor(0.622F, 0.508F, 0.082F);
            return dripparticle$driphangparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class HoneyLandProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public HoneyLandProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            DripParticle dripparticle = new DripParticle.DripLandParticle(level, x, y, z, Fluids.EMPTY, this.sprite.get(random));
            dripparticle.lifetime = (int)(128.0 / (Math.random() * 0.8 + 0.2));
            dripparticle.setColor(0.522F, 0.408F, 0.082F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class LavaFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public LavaFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            DripParticle dripparticle = new DripParticle.FallAndLandParticle(
                level, x, y, z, Fluids.LAVA, ParticleTypes.LANDING_LAVA, this.sprite.get(random)
            );
            dripparticle.setColor(1.0F, 0.2857143F, 0.083333336F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class LavaHangProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public LavaHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            return new DripParticle.CoolingDripHangParticle(
                level, x, y, z, Fluids.LAVA, ParticleTypes.FALLING_LAVA, this.sprite.get(random)
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class LavaLandProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public LavaLandProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            DripParticle dripparticle = new DripParticle.DripLandParticle(level, x, y, z, Fluids.LAVA, this.sprite.get(random));
            dripparticle.setColor(1.0F, 0.2857143F, 0.083333336F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class NectarFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public NectarFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            DripParticle dripparticle = new DripParticle.FallingParticle(level, x, y, z, Fluids.EMPTY, this.sprite.get(random));
            dripparticle.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
            dripparticle.gravity = 0.007F;
            dripparticle.setColor(0.92F, 0.782F, 0.72F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ObsidianTearFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public ObsidianTearFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            DripParticle dripparticle = new DripParticle.FallAndLandParticle(
                level, x, y, z, Fluids.EMPTY, ParticleTypes.LANDING_OBSIDIAN_TEAR, this.sprite.get(random)
            );
            dripparticle.isGlowing = true;
            dripparticle.gravity = 0.01F;
            dripparticle.setColor(0.51171875F, 0.03125F, 0.890625F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ObsidianTearHangProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public ObsidianTearHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            DripParticle.DripHangParticle dripparticle$driphangparticle = new DripParticle.DripHangParticle(
                level, x, y, z, Fluids.EMPTY, ParticleTypes.FALLING_OBSIDIAN_TEAR, this.sprite.get(random)
            );
            dripparticle$driphangparticle.isGlowing = true;
            dripparticle$driphangparticle.gravity *= 0.01F;
            dripparticle$driphangparticle.lifetime = 100;
            dripparticle$driphangparticle.setColor(0.51171875F, 0.03125F, 0.890625F);
            return dripparticle$driphangparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ObsidianTearLandProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public ObsidianTearLandProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            DripParticle dripparticle = new DripParticle.DripLandParticle(level, x, y, z, Fluids.EMPTY, this.sprite.get(random));
            dripparticle.isGlowing = true;
            dripparticle.lifetime = (int)(28.0 / (Math.random() * 0.8 + 0.2));
            dripparticle.setColor(0.51171875F, 0.03125F, 0.890625F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class SporeBlossomFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public SporeBlossomFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            int i = (int)(64.0F / Mth.randomBetween(level.getRandom(), 0.1F, 0.9F));
            DripParticle dripparticle = new DripParticle.FallingParticle(
                level, x, y, z, Fluids.EMPTY, i, this.sprite.get(random)
            );
            dripparticle.gravity = 0.005F;
            dripparticle.setColor(0.32F, 0.5F, 0.22F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class WaterFallProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public WaterFallProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            DripParticle dripparticle = new DripParticle.FallAndLandParticle(
                level, x, y, z, Fluids.WATER, ParticleTypes.SPLASH, this.sprite.get(random)
            );
            dripparticle.setColor(0.2F, 0.3F, 1.0F);
            return dripparticle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class WaterHangProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public WaterHangProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            SimpleParticleType particleType,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            RandomSource random
        ) {
            DripParticle dripparticle = new DripParticle.DripHangParticle(
                level, x, y, z, Fluids.WATER, ParticleTypes.FALLING_WATER, this.sprite.get(random)
            );
            dripparticle.setColor(0.2F, 0.3F, 1.0F);
            return dripparticle;
        }
    }
}
