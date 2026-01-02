package net.goui.cosmicdungeon.redstone.rf;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Receiver block entity — listens to the RF bus and updates its powered state.
 * Now also broadcasts Hz changes to clients so GUIs stay in sync for all players.
 */
public class RedstoneReceiverBE extends BlockEntity {
    private int hz = 1;

    public RedstoneReceiverBE(BlockPos pos, BlockState state) {
        super(ModRfBlockEntities.REDSTONE_RECEIVER_BE.get(), pos, state);
    }

    public int getHz() {
        return hz;
    }

    /** Authoritative setter (server-only): re-index, persist, update powered, vanilla update, broadcast. */
    public void setHz(ServerLevel level, int newHz) {
        int clamped = Mth.clamp(newHz, 1, 999);
        if (clamped == this.hz) return;

        int old = this.hz;
        this.hz = clamped;
        setChanged();

        // Maintain receiver index
        RfBusManager.ReceiverIndex idx = RfBusManager.ReceiverIndex.get(level);
        idx.remove(old, worldPosition);
        idx.add(this.hz, worldPosition);

        // Update powered state from the bus (if server still running)
        if (!level.getServer().isStopped()) {
            boolean active = RfBusManager.get(level).isActive(this.hz);
            BlockState st = level.getBlockState(worldPosition);
            if (st.getBlock() instanceof RedstoneReceiverBlock block) {
                block.setPowered(level, worldPosition, st, active);
            }
            // Nudge vanilla sync
            level.sendBlockUpdated(worldPosition, st, st, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }

        // Broadcast authoritative Hz to all tracking players
        RfNet.broadcastHz(level, this.worldPosition, this.hz);
    }

    void register(ServerLevel level) {
        RfBusManager.ReceiverIndex.get(level).add(hz, worldPosition);
        if (!level.getServer().isStopped()) {
            boolean active = RfBusManager.get(level).isActive(hz);
            BlockState st = level.getBlockState(worldPosition);
            if (st.getBlock() instanceof RedstoneReceiverBlock block) {
                block.setPowered(level, worldPosition, st, active);
            }
        }
    }

    void unregister(ServerLevel level) {
        RfBusManager.ReceiverIndex.get(level).remove(hz, worldPosition);
    }

    @Override
    public void onLoad() {
        if (this.level instanceof ServerLevel sl) {
            register(sl);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level instanceof ServerLevel sl && !sl.getServer().isStopped()) {
            unregister(sl);
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("Hz", hz);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        int loaded = in.read("Hz", Codec.INT).orElse(hz);
        this.hz = Mth.clamp(loaded, 1, 999);
    }
}
