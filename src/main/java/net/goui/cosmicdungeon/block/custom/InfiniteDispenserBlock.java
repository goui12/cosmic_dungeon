package net.goui.cosmicdungeon.block.custom;

import net.goui.cosmicdungeon.block.entity.InfiniteDispenserBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class InfiniteDispenserBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    public InfiniteDispenserBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TRIGGERED, false));
    }

    // ----- Blockstate -----
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, TRIGGERED);
    }

    // MUST be public in 1.21.9
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // ----- Block entity -----
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfiniteDispenserBlockEntity(pos, state);
    }

    @Override
    public @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof InfiniteDispenserBlockEntity disp ? disp : null;
    }

    // ----- Right-click opens GUI -----
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            MenuProvider provider = getMenuProvider(state, level, pos);
            if (provider != null) {
                player.openMenu(provider);
                // GameProfile now uses name()
                debug(level, pos, "GUI opened by " + player.getGameProfile().name());
            }
        }
        return InteractionResult.SUCCESS;
    }

    // ----- Redstone rising edge -----
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation ori, boolean movedByPiston) {
        if (level.isClientSide()) return;

        boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        boolean triggered = state.getValue(TRIGGERED);

        if (powered && !triggered) {
            level.scheduleTick(pos, this, 4);
            level.setBlock(pos, state.setValue(TRIGGERED, true), 2);
            debug(level, pos, "Energized: scheduling tick in 4t. Facing=" + state.getValue(FACING));
        } else if (!powered && triggered) {
            level.setBlock(pos, state.setValue(TRIGGERED, false), 2);
            debug(level, pos, "De-energized.");
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rng) {
        debug(level, pos, "Tick fired.");
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof InfiniteDispenserBlockEntity disp)) {
            debug(level, pos, "No block entity found (aborting).");
            return;
        }

        int slot = disp.findFirstShootableSlot(); // BE logs its search too
        if (slot != -1) {
            debug(level, pos, "Found shootable slot=" + slot + " → attempting projectile.");
            disp.shootStack(level, pos, state.getValue(FACING), disp.getItem(slot)); // no consume
            level.gameEvent(GameEvent.BLOCK_ACTIVATE, pos, GameEvent.Context.of(state));
            return;
        }

        // No shootables → mimic fail feedback only
        level.levelEvent(1001, pos, 0);
        level.gameEvent(GameEvent.BLOCK_ACTIVATE, pos, GameEvent.Context.of(state));
        debug(level, pos, "No shootables found — played fail event.");
    }

    // ----- Comparator output (signatures are public in 1.21.x) -----
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction side) {
        int sig = net.minecraft.world.inventory.AbstractContainerMenu
                .getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
        debug(level, pos, "Comparator read: " + sig);
        return sig;
    }


    // ----- Removal / drops -----
    // Keep this without @Override to avoid mapping wiggles across toolchains.
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (oldState.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof InfiniteDispenserBlockEntity disp) {
                debug(level, pos, "Block removed — dropping all contents.");
                disp.dropAllContents(level, pos);
                level.removeBlockEntity(pos);
            }
            // Handle neighbor updates explicitly
            level.updateNeighbourForOutputSignal(pos, this);
            level.updateNeighborsAt(pos, this);
        }
    }

    /* ---------- chat debug helper ---------- */
    private static void debug(Level level, BlockPos pos, String msg) {
        if (level.isClientSide()) return;
        String prefixed = "[InfiniteDispenser] " + msg + " @ " + pos.getX() + "," + pos.getY() + "," + pos.getZ();
        if (level instanceof ServerLevel sl) {
            for (ServerPlayer sp : sl.players()) {
                double dx = sp.getX() - (pos.getX() + 0.5);
                double dy = sp.getY() - (pos.getY() + 0.5);
                double dz = sp.getZ() - (pos.getZ() + 0.5);
                if ((dx * dx + dy * dy + dz * dz) <= (48 * 48)) {
                    sp.sendSystemMessage(Component.literal(prefixed).withStyle(ChatFormatting.AQUA));
                }
            }
        }
    }
}
