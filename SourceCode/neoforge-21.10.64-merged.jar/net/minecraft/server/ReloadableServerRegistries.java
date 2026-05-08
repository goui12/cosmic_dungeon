package net.minecraft.server;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class ReloadableServerRegistries {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final RegistrationInfo DEFAULT_REGISTRATION_INFO = new RegistrationInfo(Optional.empty(), Lifecycle.experimental());

    public static CompletableFuture<ReloadableServerRegistries.LoadResult> reload(
        LayeredRegistryAccess<RegistryLayer> registryAccess, List<Registry.PendingTags<?>> postponedTags, ResourceManager resourceManager, Executor backgroundExecutor
    ) {
        List<HolderLookup.RegistryLookup<?>> list = TagLoader.buildUpdatedLookups(registryAccess.getAccessForLoading(RegistryLayer.RELOADABLE), postponedTags);
        HolderLookup.Provider holderlookup$provider = HolderLookup.Provider.create(list.stream());
        RegistryOps<JsonElement> registryops = holderlookup$provider.createSerializationContext(JsonOps.INSTANCE);
        List<CompletableFuture<WritableRegistry<?>>> list1 = LootDataType.values()
            .map(p_359506_ -> scheduleRegistryLoad((LootDataType<?>)p_359506_, registryops, resourceManager, backgroundExecutor))
            .toList();
        CompletableFuture<List<WritableRegistry<?>>> completablefuture = Util.sequence(list1);
        return completablefuture.thenApplyAsync(
            p_359496_ -> createAndValidateFullContext(registryAccess, holderlookup$provider, (List<WritableRegistry<?>>)p_359496_), backgroundExecutor
        );
    }

    private static <T> CompletableFuture<WritableRegistry<?>> scheduleRegistryLoad(
        LootDataType<T> lootDataType, RegistryOps<JsonElement> ops, ResourceManager resourceManager, Executor backgroundExecutor
    ) {
        return CompletableFuture.supplyAsync(
            () -> {
                WritableRegistry<T> writableregistry = new MappedRegistry<>(lootDataType.registryKey(), Lifecycle.experimental());
                Map<ResourceLocation, T> map = new HashMap<>();
                var provider = net.neoforged.neoforge.common.CommonHooks.extractLookupProvider(ops);
                Map<ResourceLocation, Optional<T>> optionalMap = new HashMap<>();
                SimpleJsonResourceReloadListener.scanDirectoryWithOptionalValues(resourceManager, lootDataType.registryKey(), ops, lootDataType.conditionalCodec(), optionalMap);
                optionalMap.forEach((rl, optionalEntry) -> {
                    optionalEntry.ifPresent(entry -> lootDataType.idSetter().accept(entry, rl));
                    T value = optionalEntry.orElse(lootDataType.defaultValue());
                    if (value instanceof LootTable lootTable) value = (T) net.neoforged.neoforge.event.EventHooks.loadLootTable(provider, rl, lootTable);
                    if (value != null)
                        map.put(rl, value);
                });
                map.forEach(
                    (p_335721_, p_335683_) -> writableregistry.register(
                        ResourceKey.create(lootDataType.registryKey(), p_335721_), (T)p_335683_, DEFAULT_REGISTRATION_INFO
                    )
                );
                TagLoader.loadTagsForRegistry(resourceManager, writableregistry);
                return writableregistry;
            },
            backgroundExecutor
        );
    }

    private static ReloadableServerRegistries.LoadResult createAndValidateFullContext(
        LayeredRegistryAccess<RegistryLayer> registryAccess, HolderLookup.Provider provider, List<WritableRegistry<?>> registries
    ) {
        LayeredRegistryAccess<RegistryLayer> layeredregistryaccess = createUpdatedRegistries(registryAccess, registries);
        HolderLookup.Provider holderlookup$provider = concatenateLookups(provider, layeredregistryaccess.getLayer(RegistryLayer.RELOADABLE));
        validateLootRegistries(holderlookup$provider);
        return new ReloadableServerRegistries.LoadResult(layeredregistryaccess, holderlookup$provider);
    }

    private static HolderLookup.Provider concatenateLookups(HolderLookup.Provider lookup1, HolderLookup.Provider lookup2) {
        return HolderLookup.Provider.create(Stream.concat(lookup1.listRegistries(), lookup2.listRegistries()));
    }

    private static void validateLootRegistries(HolderLookup.Provider registries) {
        ProblemReporter.Collector problemreporter$collector = new ProblemReporter.Collector();
        ValidationContext validationcontext = new ValidationContext(problemreporter$collector, LootContextParamSets.ALL_PARAMS, registries);
        LootDataType.values().forEach(p_359499_ -> validateRegistry(validationcontext, (LootDataType<?>)p_359499_, registries));
        problemreporter$collector.forEach(
            (p_421316_, p_421317_) -> LOGGER.warn("Found loot table element validation problem in {}: {}", p_421316_, p_421317_.description())
        );
    }

    private static LayeredRegistryAccess<RegistryLayer> createUpdatedRegistries(
        LayeredRegistryAccess<RegistryLayer> registryAccess, List<WritableRegistry<?>> registries
    ) {
        return registryAccess.replaceFrom(RegistryLayer.RELOADABLE, new RegistryAccess.ImmutableRegistryAccess(registries).freeze());
    }

    private static <T> void validateRegistry(ValidationContext context, LootDataType<T> lootDataType, HolderLookup.Provider registries) {
        HolderLookup<T> holderlookup = registries.lookupOrThrow(lootDataType.registryKey());
        holderlookup.listElements().forEach(p_335842_ -> lootDataType.runValidation(context, p_335842_.key(), p_335842_.value()));
    }

    public static class Holder {
        private final HolderLookup.Provider registries;

        public Holder(HolderLookup.Provider registries) {
            this.registries = registries;
        }

        public HolderLookup.Provider lookup() {
            return this.registries;
        }

        public LootTable getLootTable(ResourceKey<LootTable> lootTableKey) {
            return this.registries
                .lookup(Registries.LOOT_TABLE)
                .flatMap(p_335799_ -> p_335799_.get(lootTableKey))
                .map(net.minecraft.core.Holder::value)
                .orElse(LootTable.EMPTY);
        }
    }

    public record LoadResult(LayeredRegistryAccess<RegistryLayer> layers, HolderLookup.Provider lookupWithUpdatedTags) {
    }
}
