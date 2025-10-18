// ModBlockEntities.java
package net.goui.cosmicdungeon.block.entity;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CosmicDungeonMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfiniteDispenserBlockEntity>>
            INFINITE_DISPENSER = BLOCK_ENTITY_TYPES.register(
            "infinite_dispenser",
            () -> new BlockEntityType<>(
                    InfiniteDispenserBlockEntity::new,
                    Set.of(ModBlocks.INFINITE_DISPENSER.get())
            )
    );

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }

    private ModBlockEntities() {}
}
