package net.minecraft.server.packs.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult.Error;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public abstract class SimpleJsonResourceReloadListener<T> extends SimplePreparableReloadListener<Map<ResourceLocation, T>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final DynamicOps<JsonElement> ops;
    private final Codec<T> codec;
    private final FileToIdConverter lister;

    protected SimpleJsonResourceReloadListener(HolderLookup.Provider provider, Codec<T> codec, ResourceKey<? extends Registry<T>> registryKey) {
        this(provider.createSerializationContext(JsonOps.INSTANCE), codec, FileToIdConverter.registry(registryKey));
    }

    protected SimpleJsonResourceReloadListener(Codec<T> codec, FileToIdConverter lister) {
        this(JsonOps.INSTANCE, codec, lister);
    }

    private SimpleJsonResourceReloadListener(DynamicOps<JsonElement> ops, Codec<T> codec, FileToIdConverter lister) {
        this.ops = ops;
        this.codec = codec;
        this.lister = lister;
    }

    /**
     * Performs any reloading that can be done off-thread, such as file IO
     */
    protected Map<ResourceLocation, T> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, T> map = new HashMap<>();
        // Neo: add condition context
        scanDirectory(resourceManager, this.lister, this.makeConditionalOps(this.ops), this.codec, map);
        return map;
    }

    public static <T> void scanDirectoryWithOptionalValues(
            ResourceManager p_386974_,
            ResourceKey<? extends Registry<T>> p_388878_,
            DynamicOps<JsonElement> p_388402_,
            Codec<java.util.Optional<T>> p_387608_,
            Map<ResourceLocation, java.util.Optional<T>> p_386495_
    ) {
        scanDirectory(p_386974_, FileToIdConverter.registry(p_388878_), p_388402_, p_387608_, p_386495_);
    }

    public static <T> void scanDirectory(
        ResourceManager resourceManager,
        ResourceKey<? extends Registry<T>> registryKey,
        DynamicOps<JsonElement> ops,
        Codec<T> codec,
        Map<ResourceLocation, T> output
    ) {
        scanDirectory(resourceManager, FileToIdConverter.registry(registryKey), ops, codec, output);
    }

    public static <T> void scanDirectory(
        ResourceManager resourceManager, FileToIdConverter lister, DynamicOps<JsonElement> ops, Codec<T> codec, Map<ResourceLocation, T> output
    ) {
        var conditionalCodec = net.neoforged.neoforge.common.conditions.ConditionalOps.createConditionalCodec(codec);
        for (Entry<ResourceLocation, Resource> entry : lister.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            ResourceLocation resourcelocation1 = lister.fileToId(resourcelocation);

            try (Reader reader = entry.getValue().openAsReader()) {
                conditionalCodec.parse(ops, com.google.gson.JsonParser.parseReader(reader)).ifSuccess(p_371454_ -> {
                    if (p_371454_.isEmpty()) {
                        LOGGER.debug("Skipping loading data file '{}' from '{}' as its conditions were not met", resourcelocation1, resourcelocation);
                    } else if (output.putIfAbsent(resourcelocation1, p_371454_.get()) != null) {
                        throw new IllegalStateException("Duplicate data file ignored with ID " + resourcelocation1);
                    }
                }).ifError(p_371566_ -> LOGGER.error("Couldn't parse data file '{}' from '{}': {}", resourcelocation1, resourcelocation, p_371566_));
            } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
                LOGGER.error("Couldn't parse data file '{}' from '{}'", resourcelocation1, resourcelocation, jsonparseexception);
            }
        }
    }

    protected ResourceLocation getPreparedPath(ResourceLocation rl) {
        return this.lister.idToFile(rl);
    }
}
