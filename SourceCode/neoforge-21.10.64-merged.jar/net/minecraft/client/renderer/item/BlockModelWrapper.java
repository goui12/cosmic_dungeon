package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class BlockModelWrapper implements ItemModel {
    private final List<ItemTintSource> tints;
    private final List<BakedQuad> quads;
    private final Supplier<Vector3f[]> extents;
    private final ModelRenderProperties properties;
    private final boolean animated;
    @Nullable
    private final net.minecraft.client.renderer.RenderType renderType;

    public BlockModelWrapper(List<ItemTintSource> tints, List<BakedQuad> quads, ModelRenderProperties properties) {
        this(tints, quads, properties, null);
    }

    public BlockModelWrapper(List<ItemTintSource> tints, List<BakedQuad> quads, ModelRenderProperties properties, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        this.tints = tints;
        this.quads = quads;
        this.properties = properties;
        this.extents = Suppliers.memoize(() -> computeExtents(this.quads));
        boolean flag = false;

        for (BakedQuad bakedquad : quads) {
            if (bakedquad.sprite().contents().isAnimated()) {
                flag = true;
                break;
            }
        }

        this.animated = flag;
        this.renderType = renderType;
    }

    public static Vector3f[] computeExtents(List<BakedQuad> quads) {
        Set<Vector3f> set = new HashSet<>();

        for (BakedQuad bakedquad : quads) {
            FaceBakery.extractPositions(bakedquad.vertices(), set::add);
        }

        return set.toArray(Vector3f[]::new);
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
            ItemStackRenderState.FoilType itemstackrenderstate$foiltype = hasSpecialAnimatedTexture(stack)
                ? ItemStackRenderState.FoilType.SPECIAL
                : ItemStackRenderState.FoilType.STANDARD;
            itemstackrenderstate$layerrenderstate.setFoilType(itemstackrenderstate$foiltype);
            renderState.setAnimated();
            renderState.appendModelIdentityElement(itemstackrenderstate$foiltype);
        }

        int k = this.tints.size();
        int[] aint = itemstackrenderstate$layerrenderstate.prepareTintLayers(k);

        for (int i = 0; i < k; i++) {
            int j = this.tints.get(i).calculate(stack, level, owner == null ? null : owner.asLivingEntity());
            aint[i] = j;
            renderState.appendModelIdentityElement(j);
        }

        itemstackrenderstate$layerrenderstate.setExtents(this.extents);
        itemstackrenderstate$layerrenderstate.setRenderType(this.renderType != null ? this.renderType : ItemBlockRenderTypes.getRenderType(stack));
        this.properties.applyToLayer(itemstackrenderstate$layerrenderstate, displayContext);
        itemstackrenderstate$layerrenderstate.prepareQuadList().addAll(this.quads);
        if (this.animated) {
            renderState.setAnimated();
        }
    }

    private static boolean hasSpecialAnimatedTexture(ItemStack stack) {
        return stack.is(ItemTags.COMPASSES) || stack.is(Items.CLOCK);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(ResourceLocation model, List<ItemTintSource> tints) implements ItemModel.Unbaked {
        public static final MapCodec<BlockModelWrapper.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_387684_ -> p_387684_.group(
                    ResourceLocation.CODEC.fieldOf("model").forGetter(BlockModelWrapper.Unbaked::model),
                    ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(BlockModelWrapper.Unbaked::tints)
                )
                .apply(p_387684_, BlockModelWrapper.Unbaked::new)
        );

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(this.model);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context) {
            ModelBaker modelbaker = context.blockModelBaker();
            ResolvedModel resolvedmodel = modelbaker.getModel(this.model);
            TextureSlots textureslots = resolvedmodel.getTopTextureSlots();
            List<BakedQuad> list = resolvedmodel.bakeTopGeometry(textureslots, modelbaker, BlockModelRotation.X0_Y0).getAll();
            ModelRenderProperties modelrenderproperties = ModelRenderProperties.fromResolvedModel(modelbaker, resolvedmodel, textureslots);
            var renderTypeGroup = resolvedmodel.getTopAdditionalProperties().getOptional(
                    net.neoforged.neoforge.client.model.NeoForgeModelProperties.RENDER_TYPE);
            var renderType = renderTypeGroup == null ? null : renderTypeGroup.entity();
            return new BlockModelWrapper(this.tints, list, modelrenderproperties, renderType);
        }

        @Override
        public MapCodec<BlockModelWrapper.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
