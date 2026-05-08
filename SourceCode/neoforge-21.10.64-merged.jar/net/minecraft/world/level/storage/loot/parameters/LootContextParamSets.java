package net.minecraft.world.level.storage.loot.parameters;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.context.ContextKeySet;

/**
 * Registry for {@link LootContextParamSet}s.
 */
public class LootContextParamSets {
    private static final BiMap<ResourceLocation, ContextKeySet> REGISTRY = HashBiMap.create();
    public static final Codec<ContextKeySet> CODEC = ResourceLocation.CODEC
        .comapFlatMap(
            p_380918_ -> Optional.ofNullable(REGISTRY.get(p_380918_))
                .map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "No parameter set exists with id: '" + p_380918_ + "'")),
            REGISTRY.inverse()::get
        );
    public static final ContextKeySet EMPTY = register("empty", p_381149_ -> {});
    public static final ContextKeySet CHEST = register(
        "chest", p_380909_ -> p_380909_.required(LootContextParams.ORIGIN).optional(LootContextParams.THIS_ENTITY).optional(LootContextParams.ATTACKING_ENTITY) //Forge: Chest minecarts can have killer entities
    );
    public static final ContextKeySet COMMAND = register(
        "command", p_380924_ -> p_380924_.required(LootContextParams.ORIGIN).optional(LootContextParams.THIS_ENTITY)
    );
    public static final ContextKeySet SELECTOR = register(
        "selector", p_380913_ -> p_380913_.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY)
    );
    public static final ContextKeySet FISHING = register(
        "fishing", p_81446_ -> p_81446_.required(LootContextParams.ORIGIN).required(LootContextParams.TOOL).optional(LootContextParams.THIS_ENTITY).optional(LootContextParams.ATTACKING_ENTITY) //Forge: Add the fisher as a killer.
    );
    public static final ContextKeySet ENTITY = register(
        "entity",
        p_380922_ -> p_380922_.required(LootContextParams.THIS_ENTITY)
            .required(LootContextParams.ORIGIN)
            .required(LootContextParams.DAMAGE_SOURCE)
            .optional(LootContextParams.ATTACKING_ENTITY)
            .optional(LootContextParams.DIRECT_ATTACKING_ENTITY)
            .optional(LootContextParams.LAST_DAMAGE_PLAYER)
    );
    public static final ContextKeySet EQUIPMENT = register(
        "equipment", p_380914_ -> p_380914_.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY)
    );
    public static final ContextKeySet ARCHAEOLOGY = register(
        "archaeology", p_380923_ -> p_380923_.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY).required(LootContextParams.TOOL)
    );
    public static final ContextKeySet GIFT = register("gift", p_380921_ -> p_380921_.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY));
    public static final ContextKeySet PIGLIN_BARTER = register("barter", p_380915_ -> p_380915_.required(LootContextParams.THIS_ENTITY));
    public static final ContextKeySet VAULT = register(
        "vault", p_380917_ -> p_380917_.required(LootContextParams.ORIGIN).optional(LootContextParams.THIS_ENTITY).optional(LootContextParams.TOOL)
    );
    public static final ContextKeySet ADVANCEMENT_REWARD = register(
        "advancement_reward", p_380910_ -> p_380910_.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ORIGIN)
    );
    public static final ContextKeySet ADVANCEMENT_ENTITY = register(
        "advancement_entity", p_380920_ -> p_380920_.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ORIGIN)
    );
    public static final ContextKeySet ADVANCEMENT_LOCATION = register(
        "advancement_location",
        p_380927_ -> p_380927_.required(LootContextParams.THIS_ENTITY)
            .required(LootContextParams.ORIGIN)
            .required(LootContextParams.TOOL)
            .required(LootContextParams.BLOCK_STATE)
    );
    public static final ContextKeySet BLOCK_USE = register(
        "block_use", p_380931_ -> p_380931_.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ORIGIN).required(LootContextParams.BLOCK_STATE)
    );
    public static final ContextKeySet ALL_PARAMS = register(
        "generic",
        p_380912_ -> p_380912_.required(LootContextParams.THIS_ENTITY)
            .required(LootContextParams.LAST_DAMAGE_PLAYER)
            .required(LootContextParams.DAMAGE_SOURCE)
            .required(LootContextParams.ATTACKING_ENTITY)
            .required(LootContextParams.DIRECT_ATTACKING_ENTITY)
            .required(LootContextParams.ORIGIN)
            .required(LootContextParams.BLOCK_STATE)
            .required(LootContextParams.BLOCK_ENTITY)
            .required(LootContextParams.TOOL)
            .required(LootContextParams.EXPLOSION_RADIUS)
    );
    public static final ContextKeySet BLOCK = register(
        "block",
        p_380919_ -> p_380919_.required(LootContextParams.BLOCK_STATE)
            .required(LootContextParams.ORIGIN)
            .required(LootContextParams.TOOL)
            .optional(LootContextParams.THIS_ENTITY)
            .optional(LootContextParams.BLOCK_ENTITY)
            .optional(LootContextParams.EXPLOSION_RADIUS)
    );
    public static final ContextKeySet SHEARING = register(
        "shearing", p_380929_ -> p_380929_.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY).required(LootContextParams.TOOL)
    );
    public static final ContextKeySet ENTITY_INTERACT = register(
        "entity_interact",
        p_432738_ -> p_432738_.required(LootContextParams.TARGET_ENTITY).optional(LootContextParams.INTERACTING_ENTITY).required(LootContextParams.TOOL)
    );
    public static final ContextKeySet BLOCK_INTERACT = register(
        "block_interact",
        p_432739_ -> p_432739_.required(LootContextParams.BLOCK_STATE)
            .optional(LootContextParams.BLOCK_ENTITY)
            .optional(LootContextParams.INTERACTING_ENTITY)
            .optional(LootContextParams.TOOL)
    );
    public static final ContextKeySet ENCHANTED_DAMAGE = register(
        "enchanted_damage",
        p_380925_ -> p_380925_.required(LootContextParams.THIS_ENTITY)
            .required(LootContextParams.ENCHANTMENT_LEVEL)
            .required(LootContextParams.ORIGIN)
            .required(LootContextParams.DAMAGE_SOURCE)
            .optional(LootContextParams.DIRECT_ATTACKING_ENTITY)
            .optional(LootContextParams.ATTACKING_ENTITY)
    );
    public static final ContextKeySet ENCHANTED_ITEM = register(
        "enchanted_item", p_380911_ -> p_380911_.required(LootContextParams.TOOL).required(LootContextParams.ENCHANTMENT_LEVEL)
    );
    public static final ContextKeySet ENCHANTED_LOCATION = register(
        "enchanted_location",
        p_380930_ -> p_380930_.required(LootContextParams.THIS_ENTITY)
            .required(LootContextParams.ENCHANTMENT_LEVEL)
            .required(LootContextParams.ORIGIN)
            .required(LootContextParams.ENCHANTMENT_ACTIVE)
    );
    public static final ContextKeySet ENCHANTED_ENTITY = register(
        "enchanted_entity",
        p_380926_ -> p_380926_.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ENCHANTMENT_LEVEL).required(LootContextParams.ORIGIN)
    );
    public static final ContextKeySet HIT_BLOCK = register(
        "hit_block",
        p_380916_ -> p_380916_.required(LootContextParams.THIS_ENTITY)
            .required(LootContextParams.ENCHANTMENT_LEVEL)
            .required(LootContextParams.ORIGIN)
            .required(LootContextParams.BLOCK_STATE)
    );

    private static ContextKeySet register(String name, Consumer<ContextKeySet.Builder> constructor) {
        ContextKeySet.Builder contextkeyset$builder = new ContextKeySet.Builder();
        constructor.accept(contextkeyset$builder);
        ContextKeySet contextkeyset = contextkeyset$builder.build();
        ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace(name);
        ContextKeySet contextkeyset1 = REGISTRY.put(resourcelocation, contextkeyset);
        if (contextkeyset1 != null) {
            throw new IllegalStateException("Loot table parameter set " + resourcelocation + " is already registered");
        } else {
            return contextkeyset;
        }
    }
}
