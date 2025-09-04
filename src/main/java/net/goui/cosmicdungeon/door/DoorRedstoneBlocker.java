package net.goui.cosmicdungeon.door;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DoorRedstoneBlocker {
    private DoorRedstoneBlocker() {}

    /**
     * When any block notifies its neighbors (how redstone propagates),
     * keep any *locked* vanilla door neighbor CLOSED and UNPOWERED.
     */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide()) return;

        final BlockPos src = event.getPos();

        for (Direction dir : event.getNotifiedSides()) {
            BlockPos maybeDoorPos = src.relative(dir);
            BlockState state = level.getBlockState(maybeDoorPos);
            if (!(state.getBlock() instanceof DoorBlock)) continue;

            // Normalize to LOWER half
            if (state.getOptionalValue(DoorBlock.HALF).orElse(DoubleBlockHalf.LOWER) == DoubleBlockHalf.UPPER) {
                maybeDoorPos = maybeDoorPos.below();
                state = level.getBlockState(maybeDoorPos);
                if (!(state.getBlock() instanceof DoorBlock)) continue;
            }

            // Is this door locked?
            DoorLockData data = DoorLockData.get(level);
            if (data.getLock(level, maybeDoorPos) == null) continue;

            // If locked, force CLOSED and UNPOWERED on both halves.
            forceClosedAndUnpowered(level, maybeDoorPos, state);
        }
    }

    private static void forceClosedAndUnpowered(Level level, BlockPos lowerPos, BlockState lowerState) {
        if (!(lowerState.getBlock() instanceof DoorBlock door)) return;

        // Door has OPEN and POWERED boolean properties
        BooleanProperty OPEN = DoorBlock.OPEN;
        BooleanProperty POWERED = DoorBlock.POWERED;

        BlockState newLower = lowerState;
        if (newLower.hasProperty(OPEN) && newLower.getValue(OPEN)) {
            newLower = newLower.setValue(OPEN, false);
        }
        if (newLower.hasProperty(POWERED) && newLower.getValue(POWERED)) {
            newLower = newLower.setValue(POWERED, false);
        }

        // Upper half must mirror OPEN/POWERED
        BlockPos upperPos = lowerPos.above();
        BlockState upperState = level.getBlockState(upperPos);
        if (upperState.getBlock() instanceof DoorBlock) {
            BlockState newUpper = upperState;
            if (newUpper.hasProperty(OPEN) && newUpper.getValue(OPEN)) {
                newUpper = newUpper.setValue(OPEN, false);
            }
            if (newUpper.hasProperty(POWERED) && newUpper.getValue(POWERED)) {
                newUpper = newUpper.setValue(POWERED, false);
            }

            if (newUpper != upperState) {
                level.setBlock(upperPos, newUpper, 2); // send neighbors & clients minimally
            }
        }

        if (newLower != lowerState) {
            level.setBlock(lowerPos, newLower, 2);
        }
    }
}
