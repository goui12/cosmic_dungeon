package net.minecraft.world.level;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ClipContext {
    private final Vec3 from;
    private final Vec3 to;
    private final ClipContext.Block block;
    private final ClipContext.Fluid fluid;
    private final CollisionContext collisionContext;

    public ClipContext(Vec3 from, Vec3 to, ClipContext.Block block, ClipContext.Fluid fluid, Entity entity) {
        this(from, to, block, fluid, CollisionContext.of(entity));
    }

    public ClipContext(Vec3 from, Vec3 to, ClipContext.Block block, ClipContext.Fluid fluid, CollisionContext collisionContext) {
        this.from = from;
        this.to = to;
        this.block = block;
        this.fluid = fluid;
        this.collisionContext = collisionContext;
    }

    public Vec3 getTo() {
        return this.to;
    }

    public Vec3 getFrom() {
        return this.from;
    }

    public VoxelShape getBlockShape(BlockState blockState, BlockGetter level, BlockPos pos) {
        return this.block.get(blockState, level, pos, this.collisionContext);
    }

    public VoxelShape getFluidShape(FluidState state, BlockGetter level, BlockPos pos) {
        return this.fluid.canPick(state) ? state.getShape(level, pos) : Shapes.empty();
    }

    public static enum Block implements ClipContext.ShapeGetter {
        COLLIDER(BlockBehaviour.BlockStateBase::getCollisionShape),
        OUTLINE(BlockBehaviour.BlockStateBase::getShape),
        VISUAL(BlockBehaviour.BlockStateBase::getVisualShape),
        FALLDAMAGE_RESETTING(
            (p_450898_, p_450899_, p_450900_, p_450901_) -> {
                if (p_450898_.is(BlockTags.FALL_DAMAGE_RESETTING)) {
                    return Shapes.block();
                } else {
                    if (p_450901_ instanceof EntityCollisionContext entitycollisioncontext
                        && entitycollisioncontext.getEntity() != null
                        && entitycollisioncontext.getEntity().getType() == EntityType.PLAYER) {
                        if (p_450898_.is(Blocks.END_GATEWAY) || p_450898_.is(Blocks.END_PORTAL)) {
                            return Shapes.block();
                        }

                        if (p_450899_ instanceof ServerLevel serverlevel
                            && p_450898_.is(Blocks.NETHER_PORTAL)
                            && serverlevel.getGameRules().getInt(GameRules.RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY) == 0) {
                            return Shapes.block();
                        }
                    }

                    return Shapes.empty();
                }
            }
        );

        private final ClipContext.ShapeGetter shapeGetter;

        private Block(ClipContext.ShapeGetter shapeGetter) {
            this.shapeGetter = shapeGetter;
        }

        @Override
        public VoxelShape get(BlockState state, BlockGetter block, BlockPos pos, CollisionContext collisionContext) {
            return this.shapeGetter.get(state, block, pos, collisionContext);
        }
    }

    public static enum Fluid {
        NONE(p_45736_ -> false),
        SOURCE_ONLY(FluidState::isSource),
        ANY(p_45734_ -> !p_45734_.isEmpty()),
        WATER(p_201988_ -> p_201988_.is(FluidTags.WATER));

        private final Predicate<FluidState> canPick;

        private Fluid(Predicate<FluidState> canPick) {
            this.canPick = canPick;
        }

        public boolean canPick(FluidState state) {
            return this.canPick.test(state);
        }
    }

    public interface ShapeGetter {
        VoxelShape get(BlockState state, BlockGetter block, BlockPos pos, CollisionContext collisionContext);
    }
}
