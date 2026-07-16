package net.goui.cosmicdungeon.block.entity;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.redstone.rf.RedstoneReceiverBE;
import net.goui.cosmicdungeon.redstone.rf.RedstoneTransmitterBE;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CosmicDungeonMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfiniteDispenserBlockEntity>>
            INFINITE_DISPENSER = BLOCK_ENTITY_TYPES.register(
            "infinite_dispenser",
            () -> new BlockEntityType<>(
                    InfiniteDispenserBlockEntity::new,
                    false,
                    ModBlocks.INFINITE_DISPENSER.get()
            )
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CosmicSpawnerBlockEntity>>
            COSMIC_SPAWNER = BLOCK_ENTITY_TYPES.register(
            "cosmic_mob_spawner",
            () -> new BlockEntityType<>(
                    CosmicSpawnerBlockEntity::new,
                    false,
                    ModBlocks.COSMIC_MOB_SPAWNER.get()
            )
    );

    /** One BE type used by all 8 class-locked chest block variants. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ClassLockedChestBlockEntity>>
            CLASS_LOCKED_CHEST = BLOCK_ENTITY_TYPES.register(
            "class_locked_chest",
            () -> new BlockEntityType<>(
                    ClassLockedChestBlockEntity::new,
                    false,
                    ModBlocks.BOGATYR_CHEST.get(),
                    ModBlocks.DEADEYE_CHEST.get(),
                    ModBlocks.DRAGOON_CHEST.get(),
                    ModBlocks.JUDICATOR_CHEST.get(),
                    ModBlocks.METALMANCER_CHEST.get(),
                    ModBlocks.PYROCLAST_CHEST.get(),
                    ModBlocks.THEURGIST_CHEST.get(),
                    ModBlocks.VENEFEX_CHEST.get()
            )
    );

    /** Class selector destination storage. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ClassSelectorBlockEntity>>
            CLASS_SELECTOR = BLOCK_ENTITY_TYPES.register(
            "class_selector",
            () -> new BlockEntityType<>(
                    ClassSelectorBlockEntity::new,
                    false,
                    ModBlocks.CLASS_SELECTOR_BLOCK.get()
            )
    );

    /* =========================================================================================
     * RF / Redstone Frequency (moved here to keep ONE BlockEntityType registry)
     * ========================================================================================= */

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneTransmitterBE>>
            REDSTONE_TRANSMITTER_BE = BLOCK_ENTITY_TYPES.register(
            "redstone_transmitter",
            () -> new BlockEntityType<>(
                    RedstoneTransmitterBE::new,
                    false,
                    ModBlocks.REDSTONE_TRANSMITTER.get()
            )
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneReceiverBE>>
            REDSTONE_RECEIVER_BE = BLOCK_ENTITY_TYPES.register(
            "redstone_receiver",
            () -> new BlockEntityType<>(
                    RedstoneReceiverBE::new,
                    false,
                    ModBlocks.REDSTONE_RECEIVER.get()
            )
    );

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
        bus.addListener(ModBlockEntities::addVanillaCompatibleBlocks);
    }

    private static void addVanillaCompatibleBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(
                BlockEntityType.CAMPFIRE,
                ModBlocks.BEATRIX_CAMPFIRE.get()
        );
    }

    private ModBlockEntities() {}
}
