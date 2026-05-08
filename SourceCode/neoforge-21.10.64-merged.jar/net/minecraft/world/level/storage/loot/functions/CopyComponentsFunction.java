package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyComponentsFunction extends LootItemConditionalFunction {
    private static final ExtraCodecs.LateBoundIdMapper<String, CopyComponentsFunction.Source<?>> SOURCES = new ExtraCodecs.LateBoundIdMapper<>();
    public static final MapCodec<CopyComponentsFunction> CODEC;
    private final CopyComponentsFunction.Source<?> source;
    private final Optional<List<DataComponentType<?>>> include;
    private final Optional<List<DataComponentType<?>>> exclude;
    private final Predicate<DataComponentType<?>> bakedPredicate;

    CopyComponentsFunction(
        List<LootItemCondition> conditions,
        CopyComponentsFunction.Source<?> source,
        Optional<List<DataComponentType<?>>> include,
        Optional<List<DataComponentType<?>>> exclude
    ) {
        super(conditions);
        this.source = source;
        this.include = include.map(List::copyOf);
        this.exclude = exclude.map(List::copyOf);
        List<Predicate<DataComponentType<?>>> list = new ArrayList<>(2);
        exclude.ifPresent(p_338129_ -> list.add(p_338134_ -> !p_338129_.contains(p_338134_)));
        include.ifPresent(p_338131_ -> list.add(p_338131_::contains));
        this.bakedPredicate = Util.allOf(list);
    }

    @Override
    public LootItemFunctionType<CopyComponentsFunction> getType() {
        return LootItemFunctions.COPY_COMPONENTS;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(this.source.contextParam());
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        DataComponentGetter datacomponentgetter = this.source.get(context);
        if (datacomponentgetter != null) {
            if (datacomponentgetter instanceof DataComponentMap datacomponentmap) {
                stack.applyComponents(datacomponentmap.filter(this.bakedPredicate));
            } else {
                Collection<DataComponentType<?>> collection = this.exclude.orElse(List.of());
                this.include.map(Collection::stream).orElse(BuiltInRegistries.DATA_COMPONENT_TYPE.listElements().map(Holder::value)).forEach(p_450906_ -> {
                    if (!collection.contains(p_450906_)) {
                        TypedDataComponent<?> typeddatacomponent = datacomponentgetter.getTyped(p_450906_);
                        if (typeddatacomponent != null) {
                            stack.set(typeddatacomponent);
                        }
                    }
                });
            }
        }

        return stack;
    }

    public static CopyComponentsFunction.Builder copyComponentsFromEntity(ContextKey<? extends Entity> contextParam) {
        return new CopyComponentsFunction.Builder(new CopyComponentsFunction.EntitySource(contextParam));
    }

    public static CopyComponentsFunction.Builder copyComponentsFromBlockEntity(ContextKey<? extends BlockEntity> contextParam) {
        return new CopyComponentsFunction.Builder(new CopyComponentsFunction.BlockEntitySource(contextParam));
    }

    static {
        for (LootContext.EntityTarget lootcontext$entitytarget : LootContext.EntityTarget.values()) {
            SOURCES.put(lootcontext$entitytarget.getSerializedName(), new CopyComponentsFunction.EntitySource(lootcontext$entitytarget.getParam()));
        }

        for (LootContext.BlockEntityTarget lootcontext$blockentitytarget : LootContext.BlockEntityTarget.values()) {
            SOURCES.put(
                lootcontext$blockentitytarget.getSerializedName(), new CopyComponentsFunction.BlockEntitySource(lootcontext$blockentitytarget.getParam())
            );
        }

        for (LootContext.ItemStackTarget lootcontext$itemstacktarget : LootContext.ItemStackTarget.values()) {
            SOURCES.put(lootcontext$itemstacktarget.getSerializedName(), new CopyComponentsFunction.ItemStackSource(lootcontext$itemstacktarget.getParam()));
        }

        CODEC = RecordCodecBuilder.mapCodec(
            p_450907_ -> commonFields(p_450907_)
                .and(
                    p_450907_.group(
                        SOURCES.codec(Codec.STRING).fieldOf("source").forGetter(p_331312_ -> p_331312_.source),
                        DataComponentType.CODEC.listOf().optionalFieldOf("include").forGetter(p_338132_ -> p_338132_.include),
                        DataComponentType.CODEC.listOf().optionalFieldOf("exclude").forGetter(p_338126_ -> p_338126_.exclude)
                    )
                )
                .apply(p_450907_, CopyComponentsFunction::new)
        );
    }

    record BlockEntitySource(ContextKey<? extends BlockEntity> contextParam) implements CopyComponentsFunction.Source<BlockEntity> {
        public DataComponentGetter get(BlockEntity p_451571_) {
            return p_451571_.collectComponents();
        }
    }

    public static class Builder extends LootItemConditionalFunction.Builder<CopyComponentsFunction.Builder> {
        private final CopyComponentsFunction.Source<?> source;
        private Optional<ImmutableList.Builder<DataComponentType<?>>> include = Optional.empty();
        private Optional<ImmutableList.Builder<DataComponentType<?>>> exclude = Optional.empty();

        Builder(CopyComponentsFunction.Source<?> source) {
            this.source = source;
        }

        public CopyComponentsFunction.Builder include(DataComponentType<?> include) {
            if (this.include.isEmpty()) {
                this.include = Optional.of(ImmutableList.builder());
            }

            this.include.get().add(include);
            return this;
        }

        public CopyComponentsFunction.Builder exclude(DataComponentType<?> exclude) {
            if (this.exclude.isEmpty()) {
                this.exclude = Optional.of(ImmutableList.builder());
            }

            this.exclude.get().add(exclude);
            return this;
        }

        protected CopyComponentsFunction.Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new CopyComponentsFunction(
                this.getConditions(), this.source, this.include.map(ImmutableList.Builder::build), this.exclude.map(ImmutableList.Builder::build)
            );
        }
    }

    record EntitySource(ContextKey<? extends Entity> contextParam) implements CopyComponentsFunction.Source<Entity> {
        public DataComponentGetter get(Entity p_451219_) {
            return p_451219_;
        }
    }

    record ItemStackSource(ContextKey<? extends ItemStack> contextParam) implements CopyComponentsFunction.Source<ItemStack> {
        public DataComponentGetter get(ItemStack p_451201_) {
            return p_451201_.getComponents();
        }
    }

    public interface Source<T> {
        ContextKey<? extends T> contextParam();

        DataComponentGetter get(T value);

        @Nullable
        default DataComponentGetter get(LootContext context) {
            T t = context.getOptionalParameter((ContextKey<T>)this.contextParam());
            return t != null ? this.get(t) : null;
        }
    }
}
