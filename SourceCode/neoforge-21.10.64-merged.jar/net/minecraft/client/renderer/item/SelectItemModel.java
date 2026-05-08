package net.minecraft.client.renderer.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.CacheSlot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SelectItemModel<T> implements ItemModel {
    private final SelectItemModelProperty<T> property;
    private final SelectItemModel.ModelSelector<T> models;

    public SelectItemModel(SelectItemModelProperty<T> property, SelectItemModel.ModelSelector<T> models) {
        this.property = property;
        this.models = models;
    }

    @Override
    public void update(
        ItemStackRenderState renderState,
        ItemStack stack,
        ItemModelResolver itemModelResolver,
        ItemDisplayContext displayContext,
        @Nullable ClientLevel level,
        @Nullable ItemOwner owner,
        int seed
    ) {
        renderState.appendModelIdentityElement(this);
        T t = this.property.get(stack, level, owner == null ? null : owner.asLivingEntity(), seed, displayContext);
        ItemModel itemmodel = this.models.get(t, level);
        if (itemmodel != null) {
            itemmodel.update(renderState, stack, itemModelResolver, displayContext, level, owner, seed);
        }
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface ModelSelector<T> {
        @Nullable
        ItemModel get(@Nullable T propertyValue, @Nullable ClientLevel level);
    }

    @OnlyIn(Dist.CLIENT)
    public record SwitchCase<T>(List<T> values, ItemModel.Unbaked model) {
        public static <T> Codec<SelectItemModel.SwitchCase<T>> codec(Codec<T> codec) {
            return RecordCodecBuilder.create(
                p_387815_ -> p_387815_.group(
                        ExtraCodecs.nonEmptyList(ExtraCodecs.compactListCodec(codec)).fieldOf("when").forGetter(SelectItemModel.SwitchCase::values),
                        ItemModels.CODEC.fieldOf("model").forGetter(SelectItemModel.SwitchCase::model)
                    )
                    .apply(p_387815_, SelectItemModel.SwitchCase::new)
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(SelectItemModel.UnbakedSwitch<?, ?> unbakedSwitch, Optional<ItemModel.Unbaked> fallback) implements ItemModel.Unbaked {
        public static final MapCodec<SelectItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_386872_ -> p_386872_.group(
                    SelectItemModel.UnbakedSwitch.MAP_CODEC.forGetter(SelectItemModel.Unbaked::unbakedSwitch),
                    ItemModels.CODEC.optionalFieldOf("fallback").forGetter(SelectItemModel.Unbaked::fallback)
                )
                .apply(p_386872_, SelectItemModel.Unbaked::new)
        );

        @Override
        public MapCodec<SelectItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context) {
            ItemModel itemmodel = this.fallback.<ItemModel>map(p_387332_ -> p_387332_.bake(context)).orElse(context.missingItemModel());
            return this.unbakedSwitch.bake(context, itemmodel);
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            this.unbakedSwitch.resolveDependencies(resolver);
            this.fallback.ifPresent(p_388441_ -> p_388441_.resolveDependencies(resolver));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record UnbakedSwitch<P extends SelectItemModelProperty<T>, T>(P property, List<SelectItemModel.SwitchCase<T>> cases) {
        public static final MapCodec<SelectItemModel.UnbakedSwitch<?, ?>> MAP_CODEC = SelectItemModelProperties.CODEC
            .dispatchMap("property", p_387573_ -> p_387573_.property().type(), SelectItemModelProperty.Type::switchCodec);

        public ItemModel bake(ItemModel.BakingContext bakingContext, ItemModel model) {
            Object2ObjectMap<T, ItemModel> object2objectmap = new Object2ObjectOpenHashMap<>();

            for (SelectItemModel.SwitchCase<T> switchcase : this.cases) {
                ItemModel.Unbaked itemmodel$unbaked = switchcase.model;
                ItemModel itemmodel = itemmodel$unbaked.bake(bakingContext);

                for (T t : switchcase.values) {
                    object2objectmap.put(t, itemmodel);
                }
            }

            object2objectmap.defaultReturnValue(model);
            return new SelectItemModel<>(this.property, this.createModelGetter(object2objectmap, bakingContext.contextSwapper()));
        }

        private SelectItemModel.ModelSelector<T> createModelGetter(Object2ObjectMap<T, ItemModel> models, @Nullable RegistryContextSwapper contextSwapper) {
            if (contextSwapper == null) {
                return (p_399341_, p_399342_) -> models.get(p_399341_);
            } else {
                ItemModel itemmodel = models.defaultReturnValue();
                CacheSlot<ClientLevel, Object2ObjectMap<T, ItemModel>> cacheslot = new CacheSlot<>(
                    p_399349_ -> {
                        Object2ObjectMap<T, ItemModel> object2objectmap = new Object2ObjectOpenHashMap<>(models.size());
                        object2objectmap.defaultReturnValue(itemmodel);
                        models.forEach(
                            (p_432304_, p_432305_) -> contextSwapper.swapTo(this.property.valueCodec(), (T)p_432304_, p_399349_.registryAccess())
                                .ifSuccess(p_399345_ -> object2objectmap.put((T)p_399345_, p_432305_))
                        );
                        return object2objectmap;
                    }
                );
                return (p_399333_, p_399334_) -> {
                    if (p_399334_ == null) {
                        return models.get(p_399333_);
                    } else {
                        return p_399333_ == null ? itemmodel : cacheslot.compute(p_399334_).get(p_399333_);
                    }
                };
            }
        }

        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            for (SelectItemModel.SwitchCase<?> switchcase : this.cases) {
                switchcase.model.resolveDependencies(resolver);
            }
        }
    }
}
