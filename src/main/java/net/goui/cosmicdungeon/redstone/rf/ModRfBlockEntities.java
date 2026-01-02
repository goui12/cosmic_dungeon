package net.goui.cosmicdungeon.redstone.rf;

import java.util.function.Supplier;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRfBlockEntities {
    private ModRfBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CosmicDungeonMod.MOD_ID);

    public static final Supplier<BlockEntityType<RedstoneTransmitterBE>> REDSTONE_TRANSMITTER_BE =
            BLOCK_ENTITY_TYPES.register("redstone_transmitter",
                    () -> new BlockEntityType<>(
                            RedstoneTransmitterBE::new,
                            false,
                            ModBlocks.REDSTONE_TRANSMITTER.get()
                    )
            );

    public static final Supplier<BlockEntityType<RedstoneReceiverBE>> REDSTONE_RECEIVER_BE =
            BLOCK_ENTITY_TYPES.register("redstone_receiver",
                    () -> new BlockEntityType<>(
                            RedstoneReceiverBE::new,
                            false,
                            ModBlocks.REDSTONE_RECEIVER.get()
                    )
            );

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}
