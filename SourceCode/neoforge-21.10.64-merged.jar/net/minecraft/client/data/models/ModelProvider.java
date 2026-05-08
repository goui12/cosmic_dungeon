package net.minecraft.client.data.models;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelProvider implements DataProvider, net.neoforged.neoforge.client.extensions.IModelProviderExtension {
    private final PackOutput.PathProvider blockStatePathProvider;
    private final PackOutput.PathProvider itemInfoPathProvider;
    private final PackOutput.PathProvider modelPathProvider;
    public final String modId;

    // Neo: Use the constructor which accepts a mod ID.
    @Deprecated
    public ModelProvider(PackOutput output) {
        this(output, ResourceLocation.DEFAULT_NAMESPACE);
    }

    public ModelProvider(PackOutput output, String modId) {
        this.blockStatePathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.itemInfoPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
        this.modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        this.modId = modId;
    }

    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockModels.run();
        itemModels.run();
    }

    /**
     * Returns a {@link Stream stream} containing all {@link Block blocks} which must have their models/block states generated or {@link Stream#empty() empty} if none are desired.
     * <p>
     * When using providers for specific {@link Block block} usages, it is best to override this method returning the exact {@link Block blocks} which must be generated,
     * or {@link Stream#empty() empty} if generating only {@link Item item} models.
     * <p>
     * Default implementation generates models for {@link Block blocks} matching the given {@code modId}.
     * @see #getKnownItems()
     */
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return BuiltInRegistries.BLOCK.listElements().filter(holder -> holder.getKey().location().getNamespace().equals(modId));
    }

    /**
     * Returns a {@link Stream stream} containing all {@link Item items} which must have their models/client items generated or {@link Stream#empty() empty} if none are desired.
     * <p>
     * When using providers for specific {@link Item item} usages, it is best to override this method returning the exact {@link Item items} which must be generated,
     * or {@link Stream#empty() empty} if generating only {@link Block block} models (which have no respective {@link Item item}).
     * <p>
     * Default implementation generates models for {@link Item items} matching the given {@code modId}.
     * @see #getKnownBlocks()
     */
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return BuiltInRegistries.ITEM.listElements().filter(holder -> holder.getKey().location().getNamespace().equals(modId));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        ModelProvider.ItemInfoCollector modelprovider$iteminfocollector = new ModelProvider.ItemInfoCollector(this::getKnownItems);
        ModelProvider.BlockStateGeneratorCollector modelprovider$blockstategeneratorcollector = new ModelProvider.BlockStateGeneratorCollector(this::getKnownBlocks);
        ModelProvider.SimpleModelCollector modelprovider$simplemodelcollector = new ModelProvider.SimpleModelCollector();
        registerModels(new BlockModelGenerators(modelprovider$blockstategeneratorcollector, modelprovider$iteminfocollector, modelprovider$simplemodelcollector), new ItemModelGenerators(modelprovider$iteminfocollector, modelprovider$simplemodelcollector));
        modelprovider$blockstategeneratorcollector.validate();
        modelprovider$iteminfocollector.finalizeAndValidate();
        return CompletableFuture.allOf(
            modelprovider$blockstategeneratorcollector.save(output, this.blockStatePathProvider),
            modelprovider$simplemodelcollector.save(output, this.modelPathProvider),
            modelprovider$iteminfocollector.save(output, this.itemInfoPathProvider)
        );
    }

    @Override
    public String getName() {
        return "Model Definitions - " + modId;
    }

    @OnlyIn(Dist.CLIENT)
    static class BlockStateGeneratorCollector implements Consumer<BlockModelDefinitionGenerator> {
        private final Map<Block, BlockModelDefinitionGenerator> generators = new HashMap<>();
        private final Supplier<Stream<? extends Holder<Block>>> knownBlocks;

        public BlockStateGeneratorCollector(Supplier<Stream<? extends Holder<Block>>> knownBlocks) {
            this.knownBlocks = knownBlocks;
        }

        @Deprecated // Neo: Provided for vanilla/multi-loader compatibility. Use constructor with Supplier parameter.
        public BlockStateGeneratorCollector() {
            this(BuiltInRegistries.BLOCK::listElements);
        }

        public void accept(BlockModelDefinitionGenerator generator) {
            Block block = generator.block();
            BlockModelDefinitionGenerator blockmodeldefinitiongenerator = this.generators.put(block, generator);
            if (blockmodeldefinitiongenerator != null) {
                throw new IllegalStateException("Duplicate blockstate definition for " + block);
            }
        }

        public void validate() {
            Stream<? extends Holder<Block>> stream = knownBlocks.get();
            List<ResourceLocation> list = stream.filter(p_386843_ -> !this.generators.containsKey(p_386843_.value()))
                .map(p_386823_ -> p_386823_.unwrapKey().orElseThrow().location())
                .toList();
            if (!list.isEmpty()) {
                throw new IllegalStateException("Missing blockstate definitions for: " + list);
            }
        }

        public CompletableFuture<?> save(CachedOutput output, PackOutput.PathProvider pathProvider) {
            Map<Block, BlockModelDefinition> map = Maps.transformValues(this.generators, BlockModelDefinitionGenerator::create);
            Function<Block, Path> function = p_387598_ -> pathProvider.json(p_387598_.builtInRegistryHolder().key().location());
            return DataProvider.saveAll(output, BlockModelDefinition.CODEC, function, map);
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class ItemInfoCollector implements ItemModelOutput {
        private final Map<Item, ClientItem> itemInfos = new HashMap<>();
        private final Map<Item, Item> copies = new HashMap<>();
        private final Supplier<Stream<? extends Holder<Item>>> knownItems;

        public ItemInfoCollector(Supplier<Stream<? extends Holder<Item>>> knownItems) {
            this.knownItems = knownItems;
        }

        @Deprecated // Neo: Provided for vanilla/multi-loader compatibility. Use constructor with Supplier parameter.
        public ItemInfoCollector() {
            this(BuiltInRegistries.ITEM::listElements);
        }

        @Override
        public void accept(Item item, ItemModel.Unbaked model) {
            this.register(item, new ClientItem(model, ClientItem.Properties.DEFAULT));
        }

        public void register(Item item, ClientItem clientItem) {
            ClientItem clientitem = this.itemInfos.put(item, clientItem);
            if (clientitem != null) {
                throw new IllegalStateException("Duplicate item model definition for " + item);
            }
        }

        @Override
        public void copy(Item item1, Item item2) {
            this.copies.put(item2, item1);
        }

        public void finalizeAndValidate() {
            knownItems.get().map(Holder::value).forEach(p_388426_ -> {
                if (!this.copies.containsKey(p_388426_)) {
                    if (p_388426_ instanceof BlockItem blockitem && !this.itemInfos.containsKey(blockitem)) {
                        ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(blockitem.getBlock());
                        this.accept(blockitem, ItemModelUtils.plainModel(resourcelocation));
                    }
                }
            });
            this.copies.forEach((p_386494_, p_386575_) -> {
                ClientItem clientitem = this.itemInfos.get(p_386575_);
                if (clientitem == null) {
                    throw new IllegalStateException("Missing donor: " + p_386575_ + " -> " + p_386494_);
                } else {
                    this.register(p_386494_, clientitem);
                }
            });
            List<ResourceLocation> list = knownItems.get()
                .filter(p_388636_ -> !this.itemInfos.containsKey(p_388636_.value()))
                .map(p_388278_ -> p_388278_.unwrapKey().orElseThrow().location())
                .toList();
            if (!list.isEmpty()) {
                throw new IllegalStateException("Missing item model definitions for: " + list);
            }
        }

        public CompletableFuture<?> save(CachedOutput output, PackOutput.PathProvider pathProvider) {
            return DataProvider.saveAll(
                output, ClientItem.CODEC, p_388594_ -> pathProvider.json(p_388594_.builtInRegistryHolder().key().location()), this.itemInfos
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class SimpleModelCollector implements BiConsumer<ResourceLocation, ModelInstance> {
        private final Map<ResourceLocation, ModelInstance> models = new HashMap<>();

        public void accept(ResourceLocation location, ModelInstance model) {
            Supplier<JsonElement> supplier = this.models.put(location, model);
            if (supplier != null) {
                throw new IllegalStateException("Duplicate model definition for " + location);
            }
        }

        public CompletableFuture<?> save(CachedOutput output, PackOutput.PathProvider pathProvider) {
            return DataProvider.saveAll(output, Supplier::get, pathProvider::json, this.models);
        }
    }
}
