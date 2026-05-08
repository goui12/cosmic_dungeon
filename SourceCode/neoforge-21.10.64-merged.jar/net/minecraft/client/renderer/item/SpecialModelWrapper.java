package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class SpecialModelWrapper<T> implements ItemModel {
    private final SpecialModelRenderer<T> specialRenderer;
    private final ModelRenderProperties properties;

    public SpecialModelWrapper(SpecialModelRenderer<T> specialRenderer, ModelRenderProperties properties) {
        this.specialRenderer = specialRenderer;
        this.properties = properties;
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
        ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = renderState.newLayer();
        if (stack.hasFoil()) {
            ItemStackRenderState.FoilType itemstackrenderstate$foiltype = ItemStackRenderState.FoilType.STANDARD;
            itemstackrenderstate$layerrenderstate.setFoilType(itemstackrenderstate$foiltype);
            renderState.setAnimated();
            renderState.appendModelIdentityElement(itemstackrenderstate$foiltype);
        }

        T t = this.specialRenderer.extractArgument(stack);
        itemstackrenderstate$layerrenderstate.setExtents(() -> {
            Set<Vector3f> set = new HashSet<>();
            this.specialRenderer.getExtents(set);
            return set.toArray(new Vector3f[0]);
        });
        itemstackrenderstate$layerrenderstate.setupSpecialModel(this.specialRenderer, t);
        if (t != null) {
            renderState.appendModelIdentityElement(t);
        }

        this.properties.applyToLayer(itemstackrenderstate$layerrenderstate, displayContext);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(ResourceLocation base, SpecialModelRenderer.Unbaked specialModel) implements ItemModel.Unbaked {
        public static final MapCodec<SpecialModelWrapper.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_386693_ -> p_386693_.group(
                    ResourceLocation.CODEC.fieldOf("base").forGetter(SpecialModelWrapper.Unbaked::base),
                    SpecialModelRenderers.CODEC.fieldOf("model").forGetter(SpecialModelWrapper.Unbaked::specialModel)
                )
                .apply(p_386693_, SpecialModelWrapper.Unbaked::new)
        );

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(this.base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context) {
            SpecialModelRenderer<?> specialmodelrenderer = this.specialModel.bake(context);
            if (specialmodelrenderer == null) {
                return context.missingItemModel();
            } else {
                ModelRenderProperties modelrenderproperties = this.getProperties(context);
                return new SpecialModelWrapper<>(specialmodelrenderer, modelrenderproperties);
            }
        }

        private ModelRenderProperties getProperties(ItemModel.BakingContext context) {
            ModelBaker modelbaker = context.blockModelBaker();
            ResolvedModel resolvedmodel = modelbaker.getModel(this.base);
            TextureSlots textureslots = resolvedmodel.getTopTextureSlots();
            return ModelRenderProperties.fromResolvedModel(modelbaker, resolvedmodel, textureslots);
        }

        @Override
        public MapCodec<SpecialModelWrapper.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
