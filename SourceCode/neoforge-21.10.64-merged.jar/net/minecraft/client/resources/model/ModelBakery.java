package net.minecraft.client.resources.model;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.thread.ParallelMapTransform;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ModelBakery {
    public static final Material FIRE_0 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("fire_0");
    public static final Material FIRE_1 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("fire_1");
    public static final Material LAVA_FLOW = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("lava_flow");
    public static final Material WATER_FLOW = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("water_flow");
    public static final Material WATER_OVERLAY = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("water_overlay");
    public static final Material BANNER_BASE = new Material(Sheets.BANNER_SHEET, ResourceLocation.withDefaultNamespace("entity/banner_base"));
    public static final Material SHIELD_BASE = new Material(Sheets.SHIELD_SHEET, ResourceLocation.withDefaultNamespace("entity/shield_base"));
    public static final Material NO_PATTERN_SHIELD = new Material(Sheets.SHIELD_SHEET, ResourceLocation.withDefaultNamespace("entity/shield_base_nopattern"));
    public static final int DESTROY_STAGE_COUNT = 10;
    public static final List<ResourceLocation> DESTROY_STAGES = IntStream.range(0, 10)
        .mapToObj(p_349912_ -> ResourceLocation.withDefaultNamespace("block/destroy_stage_" + p_349912_))
        .collect(Collectors.toList());
    public static final List<ResourceLocation> BREAKING_LOCATIONS = DESTROY_STAGES.stream()
        .map(p_349910_ -> p_349910_.withPath(p_349911_ -> "textures/" + p_349911_ + ".png"))
        .collect(Collectors.toList());
    public static final List<RenderType> DESTROY_TYPES = BREAKING_LOCATIONS.stream().map(RenderType::crumbling).collect(Collectors.toList());
    static final Logger LOGGER = LogUtils.getLogger();
    private final EntityModelSet entityModelSet;
    private final MaterialSet materials;
    private final PlayerSkinRenderCache playerSkinRenderCache;
    private final Map<BlockState, BlockStateModel.UnbakedRoot> unbakedBlockStateModels;
    private final Map<ResourceLocation, ClientItem> clientInfos;
    final Map<ResourceLocation, ResolvedModel> resolvedModels;
    final ResolvedModel missingModel;
    private final net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.LoadedModels standaloneModels;
    private final net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.PendingAnimations pendingAnimations;

    /**
     * @deprecated Neo: use {@link #ModelBakery(EntityModelSet, MaterialSet,
     *             PlayerSkinRenderCache, Map, Map, Map, ResolvedModel,
     *             net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.LoadedModels
     *             ,
     *             net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.PendingAnimations
     *             )} instead
     */
    @Deprecated
    public ModelBakery(
        EntityModelSet entityModelSet,
        MaterialSet materials,
        PlayerSkinRenderCache playerSkinRenderCache,
        Map<BlockState, BlockStateModel.UnbakedRoot> unbakedBlockStateModels,
        Map<ResourceLocation, ClientItem> clientInfos,
        Map<ResourceLocation, ResolvedModel> resolvedModels,
        ResolvedModel missingModel
    ) {
        this(entityModelSet, materials, playerSkinRenderCache, unbakedBlockStateModels, clientInfos, resolvedModels, missingModel, net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.LoadedModels.EMPTY, net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.PendingAnimations.EMPTY);
    }

    public ModelBakery(
        EntityModelSet entityModelSet,
        MaterialSet materials,
        PlayerSkinRenderCache playerSkinRenderCache,
        Map<BlockState, BlockStateModel.UnbakedRoot> unbakedBlockStateModels,
        Map<ResourceLocation, ClientItem> clientInfos,
        Map<ResourceLocation, ResolvedModel> resolvedModels,
        ResolvedModel missingModel,
        net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.LoadedModels standaloneModels,
        net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.PendingAnimations pendingAnimations
    ) {
        this.entityModelSet = entityModelSet;
        this.materials = materials;
        this.playerSkinRenderCache = playerSkinRenderCache;
        this.unbakedBlockStateModels = unbakedBlockStateModels;
        this.clientInfos = clientInfos;
        this.resolvedModels = resolvedModels;
        this.missingModel = missingModel;
        this.standaloneModels = standaloneModels;
        this.pendingAnimations = pendingAnimations;
    }

    public CompletableFuture<ModelBakery.BakingResult> bakeModels(SpriteGetter sprites, Executor executor) {
        ModelBakery.MissingModels modelbakery$missingmodels = ModelBakery.MissingModels.bake(this.missingModel, sprites);
        ModelBakery.ModelBakerImpl modelbakery$modelbakerimpl = new ModelBakery.ModelBakerImpl(sprites);
        CompletableFuture<Map<BlockState, BlockStateModel>> completablefuture = ParallelMapTransform.schedule(
            this.unbakedBlockStateModels, (p_409109_, p_409110_) -> {
                try {
                    return p_409110_.bake(p_409109_, modelbakery$modelbakerimpl);
                } catch (Exception exception) {
                    LOGGER.warn("Unable to bake model: '{}': {}", p_409109_, exception);
                    return null;
                }
            }, executor
        );
        CompletableFuture<Map<ResourceLocation, ItemModel>> completablefuture1 = ParallelMapTransform.schedule(
            this.clientInfos,
            (p_438821_, p_438822_) -> {
                try {
                    return p_438822_.model()
                        .bake(
                            new ItemModel.BakingContext(
                                modelbakery$modelbakerimpl,
                                this.entityModelSet,
                                this.materials,
                                this.playerSkinRenderCache,
                                modelbakery$missingmodels.item,
                                p_438822_.registrySwapper()
                                , this.pendingAnimations
                            )
                        );
                } catch (Exception exception) {
                    LOGGER.warn("Unable to bake item model: '{}'", p_438821_, exception);
                    return null;
                }
            },
            executor
        );
        CompletableFuture<net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.BakedModels> standaloneModelsFuture =
                net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.bake(this.standaloneModels, modelbakery$modelbakerimpl, executor);
        Map<ResourceLocation, ClientItem.Properties> map = new HashMap<>(this.clientInfos.size());
        this.clientInfos.forEach((p_404116_, p_404117_) -> {
            ClientItem.Properties clientitem$properties = p_404117_.properties();
            if (!clientitem$properties.equals(ClientItem.Properties.DEFAULT)) {
                map.put(p_404116_, clientitem$properties);
            }
        });
        completablefuture = completablefuture.thenCombine(standaloneModelsFuture, (stateModels, standaloneModels) -> stateModels);
        return completablefuture.thenCombine(
            completablefuture1,
            (p_404127_, p_404128_) -> new ModelBakery.BakingResult(
                modelbakery$missingmodels, (Map<BlockState, BlockStateModel>)p_404127_, (Map<ResourceLocation, ItemModel>)p_404128_, map, standaloneModelsFuture.join()
            )
        );
    }

    @OnlyIn(Dist.CLIENT)
    public record BakingResult(
        ModelBakery.MissingModels missingModels,
        Map<BlockState, BlockStateModel> blockStateModels,
        Map<ResourceLocation, ItemModel> itemStackModels,
        Map<ResourceLocation, ClientItem.Properties> itemProperties
        , net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.BakedModels standaloneModels
    ) {
        /**
         * @deprecated Neo: use {@link #BakingResult(ModelBakery.MissingModels, Map, Map, Map, net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.BakedModels)} instead
         */
        @Deprecated
        public BakingResult(
                ModelBakery.MissingModels missingModels,
                Map<BlockState, BlockStateModel> blockStateModels,
                Map<ResourceLocation, ItemModel> itemStackModels,
                Map<ResourceLocation, ClientItem.Properties> itemProperties
        ) {
            this(missingModels, blockStateModels, itemStackModels, itemProperties, net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.BakedModels.EMPTY);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record MissingModels(BlockStateModel block, ItemModel item) {
        public static ModelBakery.MissingModels bake(ResolvedModel model, final SpriteGetter sprites) {
            ModelBaker modelbaker = new ModelBaker() {
                @Override
                public ResolvedModel getModel(ResourceLocation p_405277_) {
                    throw new IllegalStateException("Missing model can't have dependencies, but asked for " + p_405277_);
                }

                @Override
                public <T> T compute(ModelBaker.SharedOperationKey<T> p_410289_) {
                    return p_410289_.compute(this);
                }

                @Override
                public SpriteGetter sprites() {
                    return sprites;
                }
            };
            TextureSlots textureslots = model.getTopTextureSlots();
            boolean flag = model.getTopAmbientOcclusion();
            boolean flag1 = model.getTopGuiLight().lightLikeBlock();
            ItemTransforms itemtransforms = model.getTopTransforms();
            QuadCollection quadcollection = model.bakeTopGeometry(textureslots, modelbaker, BlockModelRotation.X0_Y0);
            TextureAtlasSprite textureatlassprite = model.resolveParticleSprite(textureslots, modelbaker);
            BlockStateModel blockstatemodel = new SingleVariant(new SimpleModelWrapper(quadcollection, flag, textureatlassprite));
            ItemModel itemmodel = new MissingItemModel(quadcollection.getAll(), new ModelRenderProperties(flag1, textureatlassprite, itemtransforms));
            return new ModelBakery.MissingModels(blockstatemodel, itemmodel);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class ModelBakerImpl implements ModelBaker {
        private final SpriteGetter sprites;
        private final Map<ModelBaker.SharedOperationKey<Object>, Object> operationCache = new ConcurrentHashMap<>();
        private final Function<ModelBaker.SharedOperationKey<Object>, Object> cacheComputeFunction = p_410129_ -> p_410129_.compute(this);

        ModelBakerImpl(SpriteGetter sprites) {
            this.sprites = sprites;
        }

        @Override
        public SpriteGetter sprites() {
            return this.sprites;
        }

        @Override
        public ResolvedModel getModel(ResourceLocation modelLocation) {
            ResolvedModel resolvedmodel = ModelBakery.this.resolvedModels.get(modelLocation);
            if (resolvedmodel == null) {
                ModelBakery.LOGGER.warn("Requested a model that was not discovered previously: {}", modelLocation);
                return ModelBakery.this.missingModel;
            } else {
                return resolvedmodel;
            }
        }

        @Override
        public <T> T compute(ModelBaker.SharedOperationKey<T> key) {
            return (T)this.operationCache.computeIfAbsent((ModelBaker.SharedOperationKey<Object>)key, this.cacheComputeFunction);
        }
    }
}
