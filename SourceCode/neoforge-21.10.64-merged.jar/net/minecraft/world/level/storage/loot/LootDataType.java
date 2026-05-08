package net.minecraft.world.level.storage.loot;

import com.mojang.serialization.Codec;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record LootDataType<T>(ResourceKey<Registry<T>> registryKey, Codec<T> codec, LootDataType.Validator<T> validator, @org.jetbrains.annotations.Nullable T defaultValue, Codec<java.util.Optional<T>> conditionalCodec, java.util.function.BiConsumer<T, net.minecraft.resources.ResourceLocation> idSetter) {
    public static final LootDataType<LootItemCondition> PREDICATE = new LootDataType<>(
        Registries.PREDICATE, LootItemCondition.DIRECT_CODEC, createSimpleValidator()
    );
    public static final LootDataType<LootItemFunction> MODIFIER = new LootDataType<>(
        Registries.ITEM_MODIFIER, LootItemFunctions.ROOT_CODEC, createSimpleValidator()
    );
    public static final LootDataType<LootTable> TABLE = new LootDataType<>(Registries.LOOT_TABLE, LootTable.DIRECT_CODEC, createLootTableValidator(), LootTable.EMPTY, LootTable::setLootTableId);

    /**
     * @deprecated Neo: use the constructor {@link #LootDataType(ResourceKey, Codec, Validator, T, java.util.function.BiConsumer) with a default value and id setter} to support conditions
     */
    @Deprecated
    private LootDataType(ResourceKey<Registry<T>> registryKey, Codec<T> codec, LootDataType.Validator<T> validator) {
        this(registryKey, codec, validator, null, (it, id) -> {});
    }

    private LootDataType(ResourceKey<Registry<T>> registryKey, Codec<T> codec, LootDataType.Validator<T> validator, @org.jetbrains.annotations.Nullable T defaultValue, java.util.function.BiConsumer<T, net.minecraft.resources.ResourceLocation> idSetter) {
        this(registryKey, codec, validator, defaultValue, net.neoforged.neoforge.common.conditions.ConditionalOps.createConditionalCodec(codec), idSetter);
    }

    public void runValidation(ValidationContext context, ResourceKey<T> key, T value) {
        this.validator.run(context, key, value);
    }

    public static Stream<LootDataType<?>> values() {
        return Stream.of(PREDICATE, MODIFIER, TABLE);
    }

    private static <T extends LootContextUser> LootDataType.Validator<T> createSimpleValidator() {
        return (p_421454_, p_421455_, p_421456_) -> p_421456_.validate(p_421454_.enterElement(new ProblemReporter.RootElementPathElement(p_421455_), p_421455_));
    }

    private static LootDataType.Validator<LootTable> createLootTableValidator() {
        return (p_421457_, p_421458_, p_421459_) -> p_421459_.validate(
            p_421457_.setContextKeySet(p_421459_.getParamSet()).enterElement(new ProblemReporter.RootElementPathElement(p_421458_), p_421458_)
        );
    }

    @FunctionalInterface
    public interface Validator<T> {
        void run(ValidationContext context, ResourceKey<T> key, T value);
    }
}
