package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CompassAngleState extends NeedleDirectionHelper {
    public static final MapCodec<CompassAngleState> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_387422_ -> p_387422_.group(
                Codec.BOOL.optionalFieldOf("wobble", true).forGetter(NeedleDirectionHelper::wobble),
                CompassAngleState.CompassTarget.CODEC.fieldOf("target").forGetter(CompassAngleState::target)
            )
            .apply(p_387422_, CompassAngleState::new)
    );
    private final NeedleDirectionHelper.Wobbler wobbler;
    private final NeedleDirectionHelper.Wobbler noTargetWobbler;
    private final CompassAngleState.CompassTarget compassTarget;
    private final RandomSource random = RandomSource.create();

    public CompassAngleState(boolean wobble, CompassAngleState.CompassTarget compassTarget) {
        super(wobble);
        this.wobbler = this.newWobbler(0.8F);
        this.noTargetWobbler = this.newWobbler(0.8F);
        this.compassTarget = compassTarget;
    }

    @Override
    protected float calculate(ItemStack stack, ClientLevel level, int seed, @Nullable ItemOwner owner) {
        GlobalPos globalpos = this.compassTarget.get(level, stack, owner);
        long i = level.getGameTime();
        return !isValidCompassTargetPos(owner, globalpos)
            ? this.getRandomlySpinningRotation(seed, i)
            : this.getRotationTowardsCompassTarget(owner, i, globalpos.pos());
    }

    private float getRandomlySpinningRotation(int seed, long gameTime) {
        if (this.noTargetWobbler.shouldUpdate(gameTime)) {
            this.noTargetWobbler.update(gameTime, this.random.nextFloat());
        }

        float f = this.noTargetWobbler.rotation() + hash(seed) / 2.1474836E9F;
        return Mth.positiveModulo(f, 1.0F);
    }

    private float getRotationTowardsCompassTarget(ItemOwner owner, long gameTime, BlockPos pos) {
        float f = (float)getAngleFromEntityToPos(owner, pos);
        float f1 = getWrappedVisualRotationY(owner);
        float f2;
        if (owner.asLivingEntity() instanceof Player player && player.isLocalPlayer() && player.level().tickRateManager().runsNormally()) {
            if (this.wobbler.shouldUpdate(gameTime)) {
                this.wobbler.update(gameTime, 0.5F - (f1 - 0.25F));
            }

            f2 = f + this.wobbler.rotation();
        } else {
            f2 = 0.5F - (f1 - 0.25F - f);
        }

        return Mth.positiveModulo(f2, 1.0F);
    }

    private static boolean isValidCompassTargetPos(@Nullable ItemOwner owner, @Nullable GlobalPos pos) {
        return pos != null
            && owner != null
            && pos.dimension() == owner.level().dimension()
            && !(pos.pos().distToCenterSqr(owner.position()) < 1.0E-5F);
    }

    private static double getAngleFromEntityToPos(ItemOwner owner, BlockPos pos) {
        Vec3 vec3 = Vec3.atCenterOf(pos);
        Vec3 vec31 = owner.position();
        return Math.atan2(vec3.z() - vec31.z(), vec3.x() - vec31.x()) / (float) (Math.PI * 2);
    }

    private static float getWrappedVisualRotationY(ItemOwner owner) {
        return Mth.positiveModulo(owner.getVisualRotationYInDegrees() / 360.0F, 1.0F);
    }

    private static int hash(int seed) {
        return seed * 1327217883;
    }

    protected CompassAngleState.CompassTarget target() {
        return this.compassTarget;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum CompassTarget implements StringRepresentable {
        NONE("none") {
            @Nullable
            @Override
            public GlobalPos get(ClientLevel p_388090_, ItemStack p_388593_, @Nullable ItemOwner p_434343_) {
                return null;
            }
        },
        LODESTONE("lodestone") {
            @Nullable
            @Override
            public GlobalPos get(ClientLevel p_388590_, ItemStack p_387822_, @Nullable ItemOwner p_436068_) {
                LodestoneTracker lodestonetracker = p_387822_.get(DataComponents.LODESTONE_TRACKER);
                return lodestonetracker != null ? lodestonetracker.target().orElse(null) : null;
            }
        },
        SPAWN("spawn") {
            @Override
            public GlobalPos get(ClientLevel p_386460_, ItemStack p_387401_, @Nullable ItemOwner p_434467_) {
                return p_386460_.getRespawnData().globalPos();
            }
        },
        RECOVERY("recovery") {
            @Nullable
            @Override
            public GlobalPos get(ClientLevel p_390456_, ItemStack p_390419_, @Nullable ItemOwner p_435538_) {
                return (p_435538_ == null ? null : p_435538_.asLivingEntity()) instanceof Player player ? player.getLastDeathLocation().orElse(null) : null;
            }
        };

        public static final Codec<CompassAngleState.CompassTarget> CODEC = StringRepresentable.fromEnum(CompassAngleState.CompassTarget::values);
        private final String name;

        CompassTarget(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        @Nullable
        abstract GlobalPos get(ClientLevel level, ItemStack stack, @Nullable ItemOwner owner);
    }
}
