package net.minecraft.world.level.material;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMapper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class Fluid implements net.neoforged.neoforge.common.extensions.IFluidExtension {
    public static final IdMapper<FluidState> FLUID_STATE_REGISTRY = new IdMapper<>();
    protected final StateDefinition<Fluid, FluidState> stateDefinition;
    private FluidState defaultFluidState;
    private final Holder.Reference<Fluid> builtInRegistryHolder = BuiltInRegistries.FLUID.createIntrusiveHolder(this);
    @Nullable
    private net.neoforged.neoforge.transfer.fluid.FluidResource defaultResource;

    protected Fluid() {
        StateDefinition.Builder<Fluid, FluidState> builder = new StateDefinition.Builder<>(this);
        this.createFluidStateDefinition(builder);
        this.stateDefinition = builder.create(Fluid::defaultFluidState, FluidState::new);
        this.registerDefaultState(this.stateDefinition.any());
    }

    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
    }

    public StateDefinition<Fluid, FluidState> getStateDefinition() {
        return this.stateDefinition;
    }

    protected final void registerDefaultState(FluidState state) {
        this.defaultFluidState = state;
    }

    public final FluidState defaultFluidState() {
        return this.defaultFluidState;
    }

    public abstract Item getBucket();

    protected void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
    }

    protected void tick(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
    }

    protected void randomTick(ServerLevel level, BlockPos pos, FluidState state, RandomSource random) {
    }

    protected void entityInside(Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier) {
        if (entity.canFluidExtinguish(getFluidType())) {
            effectApplier.apply(net.minecraft.world.entity.InsideBlockEffectType.EXTINGUISH);
        }
    }

    @Nullable
    protected ParticleOptions getDripParticle() {
        return null;
    }

    protected abstract boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction);

    protected abstract Vec3 getFlow(BlockGetter blockReader, BlockPos pos, FluidState fluidState);

    public abstract int getTickDelay(LevelReader level);

    protected boolean isRandomlyTicking() {
        return false;
    }

    protected boolean isEmpty() {
        return false;
    }

    protected abstract float getExplosionResistance();

    public abstract float getHeight(FluidState state, BlockGetter level, BlockPos pos);

    public abstract float getOwnHeight(FluidState state);

    protected abstract BlockState createLegacyBlock(FluidState state);

    public abstract boolean isSource(FluidState state);

    public abstract int getAmount(FluidState state);

    public boolean isSame(Fluid fluid) {
        return fluid == this;
    }

    @Deprecated
    public boolean is(TagKey<Fluid> tag) {
        return this.builtInRegistryHolder.is(tag);
    }

    public abstract VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos);

    @Override
    public String toString() {// Neo: Patch in a toString similar to Item#toString so that FluidStack#toStringis more readable
        return BuiltInRegistries.FLUID.wrapAsHolder(this).getRegisteredName();
    }

    @Nullable
    public AABB getAABB(FluidState state, BlockGetter level, BlockPos pos) {
        if (this.isEmpty()) {
            return null;
        } else {
            float f = state.getHeight(level, pos);
            return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + f, pos.getZ() + 1.0);
        }
    }

    public Optional<SoundEvent> getPickupSound() {
        return Optional.empty();
    }

    @Deprecated
    public Holder.Reference<Fluid> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    private net.neoforged.neoforge.fluids.FluidType forgeFluidType;

    @Override
    public net.neoforged.neoforge.fluids.FluidType getFluidType() {
        if (forgeFluidType == null) forgeFluidType = net.neoforged.neoforge.common.CommonHooks.getVanillaFluidType(this);
        return forgeFluidType;
    }

    /** @deprecated Neo: do not use, use {@link net.neoforged.neoforge.transfer.fluid.FluidResource#of} instead. */
    @org.jetbrains.annotations.ApiStatus.Internal @Deprecated
    public net.neoforged.neoforge.transfer.fluid.FluidResource computeDefaultResource(java.util.function.Function<Fluid, net.neoforged.neoforge.transfer.fluid.FluidResource> resourceConstructor) {
        if (this.defaultResource == null) {
            this.defaultResource = resourceConstructor.apply(this);
        }
        return this.defaultResource;
    }
}
