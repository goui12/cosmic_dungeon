package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.CacheSlot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.ItemModelPropertyTest;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConditionalItemModel implements ItemModel {
    private final ItemModelPropertyTest property;
    private final ItemModel onTrue;
    private final ItemModel onFalse;

    public ConditionalItemModel(ItemModelPropertyTest property, ItemModel onTrue, ItemModel onFalse) {
        this.property = property;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
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
        (this.property.get(stack, level, owner == null ? null : owner.asLivingEntity(), seed, displayContext) ? this.onTrue : this.onFalse)
            .update(renderState, stack, itemModelResolver, displayContext, level, owner, seed);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(ConditionalItemModelProperty property, ItemModel.Unbaked onTrue, ItemModel.Unbaked onFalse) implements ItemModel.Unbaked {
        public static final MapCodec<ConditionalItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_388916_ -> p_388916_.group(
                    ConditionalItemModelProperties.MAP_CODEC.forGetter(ConditionalItemModel.Unbaked::property),
                    ItemModels.CODEC.fieldOf("on_true").forGetter(ConditionalItemModel.Unbaked::onTrue),
                    ItemModels.CODEC.fieldOf("on_false").forGetter(ConditionalItemModel.Unbaked::onFalse)
                )
                .apply(p_388916_, ConditionalItemModel.Unbaked::new)
        );

        @Override
        public MapCodec<ConditionalItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context) {
            return new ConditionalItemModel(
                this.adaptProperty(this.property, context.contextSwapper()), this.onTrue.bake(context), this.onFalse.bake(context)
            );
        }

        private ItemModelPropertyTest adaptProperty(ConditionalItemModelProperty property, @Nullable RegistryContextSwapper contextSwapper) {
            if (contextSwapper == null) {
                return property;
            } else {
                CacheSlot<ClientLevel, ItemModelPropertyTest> cacheslot = new CacheSlot<>(p_399329_ -> swapContext(property, contextSwapper, p_399329_));
                return (p_399322_, p_399323_, p_399324_, p_399325_, p_399326_) -> {
                    ItemModelPropertyTest itemmodelpropertytest = (ItemModelPropertyTest)(p_399323_ == null ? property : cacheslot.compute(p_399323_));
                    return itemmodelpropertytest.get(p_399322_, p_399323_, p_399324_, p_399325_, p_399326_);
                };
            }
        }

        private static <T extends ConditionalItemModelProperty> T swapContext(T property, RegistryContextSwapper contextSwapper, ClientLevel level) {
            return (T)contextSwapper.swapTo((com.mojang.serialization.Codec<T>)property.type().codec(), property, level.registryAccess()).result().orElse(property);
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            this.onTrue.resolveDependencies(resolver);
            this.onFalse.resolveDependencies(resolver);
        }
    }
}
