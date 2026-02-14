package net.goui.cosmicdungeon.redstone.rf;

import java.util.function.Supplier;
import net.goui.cosmicdungeon.block.entity.ModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;

/**
 * Compatibility wrapper: RF BlockEntities now live in {@link ModBlockEntities}.
 * Keep this class so existing code doesn't break; it no longer owns a DeferredRegister.
 */
public final class ModRfBlockEntities {
    private ModRfBlockEntities() {}

    public static final Supplier<BlockEntityType<RedstoneTransmitterBE>> REDSTONE_TRANSMITTER_BE =
            () -> ModBlockEntities.REDSTONE_TRANSMITTER_BE.get();

    public static final Supplier<BlockEntityType<RedstoneReceiverBE>> REDSTONE_RECEIVER_BE =
            () -> ModBlockEntities.REDSTONE_RECEIVER_BE.get();

    /** No-op: registrations are centralized in ModBlockEntities now. */
    public static void register(IEventBus bus) {
        // intentionally empty
    }
}
