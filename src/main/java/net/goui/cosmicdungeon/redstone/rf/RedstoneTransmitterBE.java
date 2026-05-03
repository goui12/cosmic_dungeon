package net.goui.cosmicdungeon.redstone.rf;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class RedstoneTransmitterBE extends BlockEntity {
    public static final int MAX_HZ = 1_000_000;
    private int hz = 1;

    public RedstoneTransmitterBE(BlockPos pos, BlockState state) {
        super(ModRfBlockEntities.REDSTONE_TRANSMITTER_BE.get(), pos, state);
    }

    public int getHz() {
        return hz;
    }

    /** Authoritative setter (server-only): persist, update bus, vanilla update, broadcast to clients. */
    public void setHz(ServerLevel level, int newHz) {
        int clamped = Mth.clamp(newHz, 1, MAX_HZ);
        if (clamped == this.hz) return;

        int old = this.hz;
        this.hz = clamped;
        setChanged();

        // Update RF bus if currently powered
        BlockState st = level.getBlockState(worldPosition);
        boolean powered = st.hasProperty(RedstoneTransmitterBlock.POWERED)
                && st.getValue(RedstoneTransmitterBlock.POWERED);

        if (powered && !level.getServer().isStopped()) {
            RfBusManager bus = RfBusManager.get(level);
            bus.removeActive(level, this.worldPosition, old, getSignalStrength(level));
            bus.addActive(level, this.worldPosition, this.hz, getSignalStrength(level));
        }

        // Nudge vanilla client sync (comparators/models watching this BE)
        level.sendBlockUpdated(worldPosition, st, st, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);

        // Broadcast authoritative Hz to all tracking players
        RfNet.broadcastHz(level, this.worldPosition, this.hz);
    }

    void onPowerChanged(ServerLevel level, boolean nowPowered) {
        if (level.getServer().isStopped()) return;
        RfBusManager bus = RfBusManager.get(level);
        int strength = nowPowered ? getSignalStrength(level) : 0;
        bus.updateActive(level, worldPosition, hz, strength);
    }

    @Override
    public void onLoad() {
        if (this.level instanceof ServerLevel sl && !sl.getServer().isStopped()) {
            BlockState st = sl.getBlockState(worldPosition);
            boolean powered = st.hasProperty(RedstoneTransmitterBlock.POWERED)
                    && st.getValue(RedstoneTransmitterBlock.POWERED);
            if (powered) {
                RfBusManager.get(sl).addActive(sl, worldPosition, hz, getSignalStrength(sl));
            }
        }
    }

    @Override
    public void setRemoved() {
        if (this.level instanceof ServerLevel sl && !sl.getServer().isStopped()) {
            BlockState st = sl.getBlockState(worldPosition);
            boolean powered = st.hasProperty(RedstoneTransmitterBlock.POWERED)
                    && st.getValue(RedstoneTransmitterBlock.POWERED);
            if (powered) {
                RfBusManager.get(sl).removeActive(sl, worldPosition, hz, getSignalStrength(sl));
            }
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
        this.hz = Mth.clamp(loaded, 1, MAX_HZ);
    }

    private int getSignalStrength(ServerLevel level) {
        return Mth.clamp(level.getBestNeighborSignal(worldPosition), 0, 15);
    }
}
