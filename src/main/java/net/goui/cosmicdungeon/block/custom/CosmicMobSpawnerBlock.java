// file: src/main/java/net/goui/cosmicdungeon/block/custom/CosmicMobSpawnerBlock.java
package net.goui.cosmicdungeon.block.custom;

import com.mojang.logging.LogUtils;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerBlockEntity;
import net.goui.cosmicdungeon.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import javax.annotation.Nullable;

public class CosmicMobSpawnerBlock extends Block implements EntityBlock {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Toggle extra spawner debug logging with:
     * -Dcosmicdungeon.debugSpawner=true
     */
    private static final boolean DEBUG = Boolean.getBoolean("cosmicdungeon.debugSpawner");

    public CosmicMobSpawnerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (DEBUG) {
            LOGGER.debug("Creating CosmicSpawnerBlockEntity at {}", pos);
        }
        return new CosmicSpawnerBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        // Only tick our own BE type
        if (type != ModBlockEntities.COSMIC_SPAWNER.get()) return null;

        if (level.isClientSide()) {
            return (lvl, pos, st, be) -> {
                if (be instanceof CosmicSpawnerBlockEntity cosmic) {
                    CosmicSpawnerBlockEntity.clientTick(lvl, pos, st, cosmic);
                }
            };
        }

        return (lvl, pos, st, be) -> {
            if (be instanceof CosmicSpawnerBlockEntity cosmic && lvl instanceof ServerLevel) {
                CosmicSpawnerBlockEntity.serverTick(lvl, pos, st, cosmic);
            }
        };
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CosmicSpawnerBlockEntity cosmic) {
            return cosmic.triggerEvent(id, param);
        }
        return super.triggerEvent(state, level, pos, id, param);
    }
}