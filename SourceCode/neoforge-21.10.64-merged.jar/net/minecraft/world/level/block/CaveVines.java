package net.minecraft.world.level.block;

import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface CaveVines {
    VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);
    BooleanProperty BERRIES = BlockStateProperties.BERRIES;

    static InteractionResult use(Entity entity, BlockState state, Level level, BlockPos pos) {
        if (state.getValue(BERRIES)) {
            if (level instanceof ServerLevel serverlevel) {
                Block.dropFromBlockInteractLootTable(
                    serverlevel,
                    BuiltInLootTables.HARVEST_CAVE_VINE,
                    state,
                    level.getBlockEntity(pos),
                    null,
                    entity,
                    (p_432646_, p_432647_) -> Block.popResource(p_432646_, pos, p_432647_)
                );
                float f = Mth.randomBetween(serverlevel.random, 0.8F, 1.2F);
                serverlevel.playSound(null, pos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, f);
                BlockState blockstate = state.setValue(BERRIES, false);
                serverlevel.setBlock(pos, blockstate, 2);
                serverlevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, blockstate));
            }

            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    static boolean hasGlowBerries(BlockState state) {
        return state.hasProperty(BERRIES) && state.getValue(BERRIES);
    }

    static ToIntFunction<BlockState> emission(int berries) {
        return p_181216_ -> p_181216_.getValue(BlockStateProperties.BERRIES) ? berries : 0;
    }
}
