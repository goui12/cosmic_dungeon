package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.coppergolem.CopperGolem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class CarvedPumpkinBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CarvedPumpkinBlock> CODEC = simpleCodec(CarvedPumpkinBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    @Nullable
    private BlockPattern snowGolemBase;
    @Nullable
    private BlockPattern snowGolemFull;
    @Nullable
    private BlockPattern ironGolemBase;
    @Nullable
    private BlockPattern ironGolemFull;
    @Nullable
    private BlockPattern copperGolemBase;
    @Nullable
    private BlockPattern copperGolemFull;
    private static final Predicate<BlockState> PUMPKINS_PREDICATE = p_51396_ -> p_51396_ != null
        && (p_51396_.is(Blocks.CARVED_PUMPKIN) || p_51396_.is(Blocks.JACK_O_LANTERN));

    @Override
    public MapCodec<? extends CarvedPumpkinBlock> codec() {
        return CODEC;
    }

    public CarvedPumpkinBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            this.trySpawnGolem(level, pos);
        }
    }

    public boolean canSpawnGolem(LevelReader level, BlockPos pos) {
        return this.getOrCreateSnowGolemBase().find(level, pos) != null
            || this.getOrCreateIronGolemBase().find(level, pos) != null
            || this.getOrCreateCopperGolemBase().find(level, pos) != null;
    }

    private void trySpawnGolem(Level level, BlockPos pos) {
        BlockPattern.BlockPatternMatch blockpattern$blockpatternmatch = this.getOrCreateSnowGolemFull().find(level, pos);
        if (blockpattern$blockpatternmatch != null) {
            SnowGolem snowgolem = EntityType.SNOW_GOLEM.create(level, EntitySpawnReason.TRIGGERED);
            if (snowgolem != null) {
                spawnGolemInWorld(level, blockpattern$blockpatternmatch, snowgolem, blockpattern$blockpatternmatch.getBlock(0, 2, 0).getPos());
                return;
            }
        }

        BlockPattern.BlockPatternMatch blockpattern$blockpatternmatch1 = this.getOrCreateIronGolemFull().find(level, pos);
        if (blockpattern$blockpatternmatch1 != null) {
            IronGolem irongolem = EntityType.IRON_GOLEM.create(level, EntitySpawnReason.TRIGGERED);
            if (irongolem != null) {
                irongolem.setPlayerCreated(true);
                spawnGolemInWorld(level, blockpattern$blockpatternmatch1, irongolem, blockpattern$blockpatternmatch1.getBlock(1, 2, 0).getPos());
                return;
            }
        }

        BlockPattern.BlockPatternMatch blockpattern$blockpatternmatch2 = this.getOrCreateCopperGolemFull().find(level, pos);
        if (blockpattern$blockpatternmatch2 != null) {
            CopperGolem coppergolem = EntityType.COPPER_GOLEM.create(level, EntitySpawnReason.TRIGGERED);
            if (coppergolem != null) {
                spawnGolemInWorld(level, blockpattern$blockpatternmatch2, coppergolem, blockpattern$blockpatternmatch2.getBlock(0, 0, 0).getPos());
                this.replaceCopperBlockWithChest(level, blockpattern$blockpatternmatch2);
                coppergolem.spawn(this.getWeatherStateFromPattern(blockpattern$blockpatternmatch2));
            }
        }
    }

    private WeatheringCopper.WeatherState getWeatherStateFromPattern(BlockPattern.BlockPatternMatch pattern) {
        BlockState blockstate = pattern.getBlock(0, 1, 0).getState();
        return blockstate.getBlock() instanceof WeatheringCopper weatheringcopper
            ? weatheringcopper.getAge()
            : Optional.ofNullable(HoneycombItem.WAX_OFF_BY_BLOCK.get().get(blockstate.getBlock()))
                .filter(p_432643_ -> p_432643_ instanceof WeatheringCopper)
                .map(p_432642_ -> (WeatheringCopper)p_432642_)
                .orElse((WeatheringCopper)Blocks.COPPER_BLOCK)
                .getAge();
    }

    private static void spawnGolemInWorld(Level level, BlockPattern.BlockPatternMatch patternMatch, Entity golem, BlockPos pos) {
        clearPatternBlocks(level, patternMatch);
        golem.snapTo(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(golem);

        for (ServerPlayer serverplayer : level.getEntitiesOfClass(ServerPlayer.class, golem.getBoundingBox().inflate(5.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(serverplayer, golem);
        }

        updatePatternBlocks(level, patternMatch);
    }

    public static void clearPatternBlocks(Level level, BlockPattern.BlockPatternMatch patternMatch) {
        for (int i = 0; i < patternMatch.getWidth(); i++) {
            for (int j = 0; j < patternMatch.getHeight(); j++) {
                BlockInWorld blockinworld = patternMatch.getBlock(i, j, 0);
                level.setBlock(blockinworld.getPos(), Blocks.AIR.defaultBlockState(), 2);
                level.levelEvent(2001, blockinworld.getPos(), Block.getId(blockinworld.getState()));
            }
        }
    }

    public static void updatePatternBlocks(Level level, BlockPattern.BlockPatternMatch patternMatch) {
        for (int i = 0; i < patternMatch.getWidth(); i++) {
            for (int j = 0; j < patternMatch.getHeight(); j++) {
                BlockInWorld blockinworld = patternMatch.getBlock(i, j, 0);
                level.updateNeighborsAt(blockinworld.getPos(), Blocks.AIR);
            }
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private BlockPattern getOrCreateSnowGolemBase() {
        if (this.snowGolemBase == null) {
            this.snowGolemBase = BlockPatternBuilder.start()
                .aisle(" ", "#", "#")
                .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK)))
                .build();
        }

        return this.snowGolemBase;
    }

    private BlockPattern getOrCreateSnowGolemFull() {
        if (this.snowGolemFull == null) {
            this.snowGolemFull = BlockPatternBuilder.start()
                .aisle("^", "#", "#")
                .where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE))
                .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK)))
                .build();
        }

        return this.snowGolemFull;
    }

    private BlockPattern getOrCreateIronGolemBase() {
        if (this.ironGolemBase == null) {
            this.ironGolemBase = BlockPatternBuilder.start()
                .aisle("~ ~", "###", "~#~")
                .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK)))
                .where('~', p_360136_ -> p_360136_.getState().isAir())
                .build();
        }

        return this.ironGolemBase;
    }

    private BlockPattern getOrCreateIronGolemFull() {
        if (this.ironGolemFull == null) {
            this.ironGolemFull = BlockPatternBuilder.start()
                .aisle("~^~", "###", "~#~")
                .where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE))
                .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK)))
                .where('~', p_360137_ -> p_360137_.getState().isAir())
                .build();
        }

        return this.ironGolemFull;
    }

    private BlockPattern getOrCreateCopperGolemBase() {
        if (this.copperGolemBase == null) {
            this.copperGolemBase = BlockPatternBuilder.start()
                .aisle(" ", "#")
                .where('#', BlockInWorld.hasState(p_432644_ -> p_432644_.is(BlockTags.COPPER)))
                .build();
        }

        return this.copperGolemBase;
    }

    private BlockPattern getOrCreateCopperGolemFull() {
        if (this.copperGolemFull == null) {
            this.copperGolemFull = BlockPatternBuilder.start()
                .aisle("^", "#")
                .where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE))
                .where('#', BlockInWorld.hasState(p_432641_ -> p_432641_.is(BlockTags.COPPER)))
                .build();
        }

        return this.copperGolemFull;
    }

    public void replaceCopperBlockWithChest(Level level, BlockPattern.BlockPatternMatch pattern) {
        BlockInWorld blockinworld = pattern.getBlock(0, 1, 0);
        BlockInWorld blockinworld1 = pattern.getBlock(0, 0, 0);
        Direction direction = blockinworld1.getState().getValue(FACING);
        BlockState blockstate = CopperChestBlock.getFromCopperBlock(blockinworld.getState().getBlock(), direction, level, blockinworld.getPos());
        level.setBlock(blockinworld.getPos(), blockstate, 2);
    }
}
