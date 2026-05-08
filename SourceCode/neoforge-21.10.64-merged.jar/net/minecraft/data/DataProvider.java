package net.minecraft.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public interface DataProvider {
    /**
     * Neo: Allows changing the indentation width used by {@link #saveStable}.
     */
    java.util.concurrent.atomic.AtomicInteger INDENT_WIDTH = new java.util.concurrent.atomic.AtomicInteger(2);

    ToIntFunction<String> FIXED_ORDER_FIELDS = Util.make(new Object2IntOpenHashMap<>(), p_236070_ -> {
        // Neo: conditions go first
        p_236070_.put("neoforge:conditions", -1);
        p_236070_.put("neoforge:definition_type", 0);
        p_236070_.put("neoforge:ingredient_type", 0);
        p_236070_.put("type", 0);
        p_236070_.put("parent", 1);
        p_236070_.defaultReturnValue(2);
    });
    Comparator<String> KEY_COMPARATOR = Comparator.comparingInt(FIXED_ORDER_FIELDS).thenComparing(p_236077_ -> (String)p_236077_);
    Logger LOGGER = LogUtils.getLogger();

    CompletableFuture<?> run(CachedOutput output);

    String getName();

    static <T> CompletableFuture<?> saveAll(CachedOutput output, Codec<T> codec, PackOutput.PathProvider pathProvider, Map<ResourceLocation, T> entries) {
        return saveAll(output, codec, pathProvider::json, entries);
    }

    static <T, E> CompletableFuture<?> saveAll(CachedOutput output, Codec<E> codec, Function<T, Path> pathGetter, Map<T, E> entries) {
        return saveAll(output, p_386289_ -> codec.encodeStart(JsonOps.INSTANCE, (E)p_386289_).getOrThrow(), pathGetter, entries);
    }

    static <T, E> CompletableFuture<?> saveAll(CachedOutput output, Function<E, JsonElement> serializer, Function<T, Path> pathGetter, Map<T, E> entries) {
        return CompletableFuture.allOf(entries.entrySet().stream().map(p_386293_ -> {
            Path path = pathGetter.apply(p_386293_.getKey());
            JsonElement jsonelement = serializer.apply(p_386293_.getValue());
            return saveStable(output, jsonelement, path);
        }).toArray(CompletableFuture[]::new));
    }

    static <T> CompletableFuture<?> saveStable(CachedOutput output, HolderLookup.Provider registries, Codec<T> codec, T value, Path path) {
        RegistryOps<JsonElement> registryops = registries.createSerializationContext(JsonOps.INSTANCE);
        return saveStable(output, registryops, codec, value, path);
    }

    static <T> CompletableFuture<?> saveStable(CachedOutput output, Codec<T> codec, T value, Path path) {
        return saveStable(output, JsonOps.INSTANCE, codec, value, path);
    }

    private static <T> CompletableFuture<?> saveStable(
        CachedOutput output, DynamicOps<JsonElement> ops, Codec<T> codec, T value, Path path
    ) {
        JsonElement jsonelement = codec.encodeStart(ops, value).getOrThrow();
        return saveStable(output, jsonelement, path);
    }

    static CompletableFuture<?> saveStable(CachedOutput output, JsonElement json, Path path) {
        return CompletableFuture.runAsync(() -> {
            try {
                ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
                HashingOutputStream hashingoutputstream = new HashingOutputStream(Hashing.sha1(), bytearrayoutputstream);

                try (JsonWriter jsonwriter = new JsonWriter(new OutputStreamWriter(hashingoutputstream, StandardCharsets.UTF_8))) {
                    jsonwriter.setSerializeNulls(false);
                    jsonwriter.setIndent(" ".repeat(java.lang.Math.max(0, INDENT_WIDTH.get()))); // Neo: Allow changing the indent width without needing to mixin this lambda.
                    GsonHelper.writeValue(jsonwriter, json, KEY_COMPARATOR);
                }

                output.writeIfNeeded(path, bytearrayoutputstream.toByteArray(), hashingoutputstream.hash());
            } catch (IOException ioexception) {
                LOGGER.error("Failed to save file to {}", path, ioexception);
            }
        }, Util.backgroundExecutor().forName("saveStable"));
    }

    @FunctionalInterface
    public interface Factory<T extends DataProvider> {
        T create(PackOutput output);
    }
}
