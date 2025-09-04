package net.goui.cosmicdungeon.component;

import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.common.color.AmethystColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.UnaryOperator;

public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CosmicDungeonMod.MOD_ID);

    /** UUID codec written as string to avoid version/mapping differences. */
    public static final Codec<UUID> UUID_STRING_CODEC =
            Codec.STRING.xmap(UUID::fromString, UUID::toString);

    /** Generic coordinate storage (already used in your project). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> COORDINATES =
            register("coordinates", builder -> builder.persistent(BlockPos.CODEC));

    /** Color data for amethyst items/blocks (already used in your project). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AmethystColor>> AMETHYST_COLOR =
            register("amethyst_color", b -> b.persistent(AmethystColor.CODEC));

    /** Door key binding (UUID of the lock). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> DOOR_LOCK_ID =
            register("door_lock_id", b -> b.persistent(UUID_STRING_CODEC));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name, UnaryOperator<DataComponentType.Builder<T>> builderOp) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOp.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}
