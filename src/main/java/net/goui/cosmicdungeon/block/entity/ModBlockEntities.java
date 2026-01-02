package net.goui.cosmicdungeon.block.entity;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

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



    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }

    private ModBlockEntities() {}
}
