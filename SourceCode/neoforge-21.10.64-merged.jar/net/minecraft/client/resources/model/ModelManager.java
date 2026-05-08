package net.minecraft.client.resources.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SpecialBlockModelRenderer;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ModelManager implements PreparableReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter MODEL_LISTER = FileToIdConverter.json("models");
    private Map<ResourceLocation, ItemModel> bakedItemStackModels = Map.of();
    private Map<ResourceLocation, ClientItem.Properties> itemProperties = Map.of();
    private final AtlasManager atlasManager;
    private final PlayerSkinRenderCache playerSkinRenderCache;
    private final BlockModelShaper blockModelShaper;
    private final BlockColors blockColors;
    private EntityModelSet entityModelSet = EntityModelSet.EMPTY;
    private SpecialBlockModelRenderer specialBlockModelRenderer = SpecialBlockModelRenderer.EMPTY;
    private ModelBakery.MissingModels missingModels;
    private Object2IntMap<BlockState> modelGroups = Object2IntMaps.emptyMap();
    private final java.util.concurrent.atomic.AtomicReference<ModelBakery> modelBakery = new java.util.concurrent.atomic.AtomicReference<>(null);
    private net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.BakedModels bakedStandaloneModels;
    private Set<ResourceLocation> reportedMissingItemModels = new java.util.HashSet<>();

    public ModelManager(BlockColors blockColors, AtlasManager atlasManager, PlayerSkinRenderCache playerSkinRenderCache) {
        this.blockColors = blockColors;
        this.atlasManager = atlasManager;
        this.playerSkinRenderCache = playerSkinRenderCache;
        this.blockModelShaper = new BlockModelShaper(this);
    }

    public BlockStateModel getMissingBlockStateModel() {
        return this.missingModels.block();
    }

    public ItemModel getItemModel(ResourceLocation modelLocation) {
        ItemModel model = this.bakedItemStackModels.get(modelLocation);
        if (model == null) {
            if (this.reportedMissingItemModels.add(modelLocation)) {
                LOGGER.warn("Missing item model for location {}", modelLocation);
            }
            return this.missingModels.item();
        }
        return model;
    }

    public ClientItem.Properties getItemProperties(ResourceLocation itemId) {
        return this.itemProperties.getOrDefault(itemId, ClientItem.Properties.DEFAULT);
    }

    public BlockModelShaper getBlockModelShaper() {
        return this.blockModelShaper;
    }

    @Override
    public final CompletableFuture<Void> reload(
        PreparableReloadListener.SharedState sharedState, Executor exectutor, PreparableReloadListener.PreparationBarrier barrier, Executor applyExectutor
    ) {
        ResourceManager resourcemanager = sharedState.resourceManager();
        CompletableFuture<EntityModelSet> completablefuture = CompletableFuture.supplyAsync(EntityModelSet::vanilla, exectutor);
        var pendingAnimations = sharedState.get(net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.STATE_KEY);
        CompletableFuture<SpecialBlockModelRenderer> completablefuture1 = completablefuture.thenApplyAsync(
            p_438833_ -> SpecialBlockModelRenderer.vanilla(
                new SpecialModelRenderer.BakingContext.Simple(p_438833_, this.atlasManager, this.playerSkinRenderCache, pendingAnimations)
            ),
            exectutor
        );
        CompletableFuture<Map<ResourceLocation, UnbakedModel>> completablefuture2 = loadBlockModels(resourcemanager, exectutor);
        CompletableFuture<BlockStateModelLoader.LoadedModels> completablefuture3 = BlockStateModelLoader.loadBlockStates(resourcemanager, exectutor);
        CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> completablefuture4 = ClientItemInfoLoader.scheduleLoad(resourcemanager, exectutor);
        CompletableFuture<net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.LoadedModels> standaloneModelsFuture =
                net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.load(exectutor);
        CompletableFuture<ModelManager.ResolvedModels> completablefuture5 = CompletableFuture.allOf(completablefuture2, completablefuture3, completablefuture4, standaloneModelsFuture)
            .thenApplyAsync(p_404152_ -> discoverModelDependencies(completablefuture2.join(), completablefuture3.join(), completablefuture4.join(), standaloneModelsFuture.join()), exectutor);
        CompletableFuture<Object2IntMap<BlockState>> completablefuture6 = completablefuture3.thenApplyAsync(
            p_359309_ -> buildModelGroups(this.blockColors, p_359309_), exectutor
        );
        CompletableFuture<SpriteLoader.Preparations> completablefuture7 = sharedState.get(AtlasManager.PENDING_STITCH).get(AtlasIds.BLOCKS);
        return CompletableFuture.allOf(
                completablefuture7,
                completablefuture5,
                completablefuture6,
                completablefuture3,
                completablefuture4,
                completablefuture,
                completablefuture1,
                completablefuture2
                , standaloneModelsFuture
            )
            .thenComposeAsync(
                p_438832_ -> {
                    SpriteLoader.Preparations spriteloader$preparations = completablefuture7.join();
                    ModelManager.ResolvedModels modelmanager$resolvedmodels = completablefuture5.join();
                    Object2IntMap<BlockState> object2intmap = completablefuture6.join();
                    Set<ResourceLocation> set = Sets.difference(completablefuture2.join().keySet(), modelmanager$resolvedmodels.models.keySet());
                    if (!set.isEmpty()) {
                        LOGGER.debug("Unreferenced models: \n{}", set.stream().sorted().map(p_386272_ -> "\t" + p_386272_ + "\n").collect(Collectors.joining()));
                    }

                    ModelBakery modelbakery = new ModelBakery(
                        completablefuture.join(),
                        this.atlasManager,
                        this.playerSkinRenderCache,
                        completablefuture3.join().models(),
                        completablefuture4.join().contents(),
                        modelmanager$resolvedmodels.models(),
                        modelmanager$resolvedmodels.missing()
                        , standaloneModelsFuture.join(),
                        pendingAnimations
                    );
                    this.modelBakery.set(modelbakery);
                    return loadModels(spriteloader$preparations, modelbakery, object2intmap, completablefuture.join(), completablefuture1.join(), exectutor);
                },
                exectutor
            )
            .thenCompose(barrier::wait)
            .thenAcceptAsync(this::apply, applyExectutor);
    }

    private static CompletableFuture<Map<ResourceLocation, UnbakedModel>> loadBlockModels(ResourceManager resourceManager, Executor executor) {
        return CompletableFuture.<Map<ResourceLocation, Resource>>supplyAsync(() -> MODEL_LISTER.listMatchingResources(resourceManager), executor)
            .thenCompose(
                p_250597_ -> {
                    List<CompletableFuture<Pair<ResourceLocation, UnbakedModel>>> list = new ArrayList<>(p_250597_.size());

                    for (Entry<ResourceLocation, Resource> entry : p_250597_.entrySet()) {
                        list.add(CompletableFuture.supplyAsync(() -> {
                            ResourceLocation resourcelocation = MODEL_LISTER.fileToId(entry.getKey());

                            try {
                                Pair pair;
                                try (Reader reader = entry.getValue().openAsReader()) {
                                    pair = Pair.of(resourcelocation, net.neoforged.neoforge.client.model.UnbakedModelParser.parse(reader));
                                }

                                return pair;
                            } catch (Exception exception) {
                                LOGGER.error("Failed to load model {}", entry.getKey(), exception);
                                return null;
                            }
                        }, executor));
                    }

                    return Util.sequence(list)
                        .thenApply(
                            p_250813_ -> p_250813_.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond))
                        );
                }
            );
    }

    /**
     * @deprecated Neo: use {@link #discoverModelDependencies(Map,
     *             BlockStateModelLoader.LoadedModels,
     *             ClientItemInfoLoader.LoadedClientInfos,
     *             net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.LoadedModels
     *             )} instead
     */
    @Deprecated
    private static ModelManager.ResolvedModels discoverModelDependencies(
        Map<ResourceLocation, UnbakedModel> inputModels, BlockStateModelLoader.LoadedModels loadedModels, ClientItemInfoLoader.LoadedClientInfos loadedClientInfos
    ) {
        return discoverModelDependencies(inputModels, loadedModels, loadedClientInfos, net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.LoadedModels.EMPTY);
    }

    private static ModelManager.ResolvedModels discoverModelDependencies(
            Map<ResourceLocation, UnbakedModel> inputModels, BlockStateModelLoader.LoadedModels loadedModels, ClientItemInfoLoader.LoadedClientInfos loadedClientInfos, net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader.LoadedModels standaloneModels
    ) {
        ModelManager.ResolvedModels modelmanager$resolvedmodels;
        try (Zone zone = Profiler.get().zone("dependencies")) {
            ModelDiscovery modeldiscovery = new ModelDiscovery(inputModels, MissingBlockModel.missingModel());
            modeldiscovery.addSpecialModel(ItemModelGenerator.GENERATED_ITEM_MODEL_ID, new ItemModelGenerator());
            loadedModels.models().values().forEach(modeldiscovery::addRoot);
            loadedClientInfos.contents().values().forEach(p_390109_ -> modeldiscovery.addRoot(p_390109_.model()));
            standaloneModels.models().values().forEach(modeldiscovery::addRoot);
            modelmanager$resolvedmodels = new ModelManager.ResolvedModels(modeldiscovery.missingModel(), modeldiscovery.resolve());
        }

        return modelmanager$resolvedmodels;
    }

    private static CompletableFuture<ModelManager.ReloadState> loadModels(
        final SpriteLoader.Preparations preperations,
        ModelBakery modelBakery,
        Object2IntMap<BlockState> modelGroups,
        EntityModelSet entityModelSet,
        SpecialBlockModelRenderer specialBlockModelRenderer,
        Executor executor
    ) {
        final Multimap<String, Material> multimap = Multimaps.synchronizedMultimap(HashMultimap.create());
        final Multimap<String, String> multimap1 = Multimaps.synchronizedMultimap(HashMultimap.create());
        return modelBakery.bakeModels(new SpriteGetter() {
                private final TextureAtlasSprite missingSprite = preperations.missing();

                @Override
                public TextureAtlasSprite get(Material p_388183_, ModelDebugName p_388862_) {
                    if (p_388183_.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) {
                        TextureAtlasSprite textureatlassprite = preperations.getSprite(p_388183_.texture());
                        if (textureatlassprite != null) {
                            return textureatlassprite;
                        }
                    }

                    multimap.put(p_388862_.debugName(), p_388183_);
                    return this.missingSprite;
                }

                @Override
                public TextureAtlasSprite reportMissingReference(String p_387702_, ModelDebugName p_387819_) {
                    multimap1.put(p_387819_.debugName(), p_387702_);
                    return this.missingSprite;
                }
            }, executor)
            .thenApply(
                p_432333_ -> {
                    multimap.asMap()
                        .forEach(
                            (p_387727_, p_252017_) -> LOGGER.warn(
                                "Missing textures in model {}:\n{}",
                                p_387727_,
                                p_252017_.stream()
                                    .sorted(Material.COMPARATOR)
                                    .map(p_339314_ -> "    " + p_339314_.atlasLocation() + ":" + p_339314_.texture())
                                    .collect(Collectors.joining("\n"))
                            )
                        );
                    multimap1.asMap()
                        .forEach(
                            (p_386266_, p_386267_) -> LOGGER.warn(
                                "Missing texture references in model {}:\n{}",
                                p_386266_,
                                p_386267_.stream().sorted().map(p_386265_ -> "    " + p_386265_).collect(Collectors.joining("\n"))
                            )
                        );
                    try (Zone ignored = Profiler.get().zone("neoforge_modify_baking_result")) {
                        net.neoforged.neoforge.client.ClientHooks.onModifyBakingResult(p_432333_, preperations, modelBakery);
                    }
                    Map<BlockState, BlockStateModel> map = createBlockStateToModelDispatch(p_432333_.blockStateModels(), p_432333_.missingModels().block());
                    return new ModelManager.ReloadState(p_432333_, modelGroups, map, entityModelSet, specialBlockModelRenderer);
                }
            );
    }

    private static Map<BlockState, BlockStateModel> createBlockStateToModelDispatch(Map<BlockState, BlockStateModel> blockStateModels, BlockStateModel missingModel) {
        Object object;
        try (Zone zone = Profiler.get().zone("block state dispatch")) {
            Map<BlockState, BlockStateModel> map = new IdentityHashMap<>(blockStateModels);

            for (Block block : BuiltInRegistries.BLOCK) {
                block.getStateDefinition().getPossibleStates().forEach(p_404155_ -> {
                    if (blockStateModels.putIfAbsent(p_404155_, missingModel) == null) {
                        LOGGER.warn("Missing model for variant: '{}'", p_404155_);
                    }
                });
            }

            object = map;
        }

        return (Map<BlockState, BlockStateModel>)object;
    }

    private static Object2IntMap<BlockState> buildModelGroups(BlockColors blockColors, BlockStateModelLoader.LoadedModels loadedModels) {
        Object2IntMap object2intmap;
        try (Zone zone = Profiler.get().zone("block groups")) {
            object2intmap = ModelGroupCollector.build(blockColors, loadedModels);
        }

        return object2intmap;
    }

    private void apply(ModelManager.ReloadState state) {
        ModelBakery.BakingResult modelbakery$bakingresult = state.bakedModels;
        this.bakedItemStackModels = modelbakery$bakingresult.itemStackModels();
        this.itemProperties = modelbakery$bakingresult.itemProperties();
        this.modelGroups = state.modelGroups;
        this.missingModels = modelbakery$bakingresult.missingModels();
        this.bakedStandaloneModels = modelbakery$bakingresult.standaloneModels();
        net.neoforged.neoforge.client.ClientHooks.onModelBake(this, modelbakery$bakingresult, this.modelBakery.get());
        this.reportedMissingItemModels = new java.util.HashSet<>();
        for (net.minecraft.world.item.Item item : BuiltInRegistries.ITEM) {
            ResourceLocation modelId = item.components().get(net.minecraft.core.component.DataComponents.ITEM_MODEL);
            if (modelId != null && !this.bakedItemStackModels.containsKey(modelId)) {
                this.reportedMissingItemModels.add(modelId);
                LOGGER.warn("No model loaded for default item model ID {} of {}", modelId, item);
            }
        }
        this.blockModelShaper.replaceCache(state.modelCache);
        this.specialBlockModelRenderer = state.specialBlockModelRenderer;
        this.entityModelSet = state.entityModelSet;
    }

    public boolean requiresRender(BlockState oldState, BlockState newState) {
        if (oldState == newState) {
            return false;
        } else {
            int i = this.modelGroups.getInt(oldState);
            if (i != -1) {
                int j = this.modelGroups.getInt(newState);
                if (i == j) {
                    FluidState fluidstate = oldState.getFluidState();
                    FluidState fluidstate1 = newState.getFluidState();
                    return fluidstate != fluidstate1;
                }
            }

            return true;
        }
    }

    public Supplier<SpecialBlockModelRenderer> specialBlockModelRenderer() {
        return () -> this.specialBlockModelRenderer;
    }

    public Supplier<EntityModelSet> entityModels() {
        return () -> this.entityModelSet;
    }

    public ModelBakery getModelBakery() {
        return this.modelBakery.get();
    }

    @org.jetbrains.annotations.Nullable
    public <T> T getStandaloneModel(net.neoforged.neoforge.client.model.standalone.StandaloneModelKey<T> modelKey) {
        return this.bakedStandaloneModels.get(modelKey);
    }

    @OnlyIn(Dist.CLIENT)
    record ReloadState(
        ModelBakery.BakingResult bakedModels,
        Object2IntMap<BlockState> modelGroups,
        Map<BlockState, BlockStateModel> modelCache,
        EntityModelSet entityModelSet,
        SpecialBlockModelRenderer specialBlockModelRenderer
    ) {
    }

    @OnlyIn(Dist.CLIENT)
    record ResolvedModels(ResolvedModel missing, Map<ResourceLocation, ResolvedModel> models) {
    }
}
