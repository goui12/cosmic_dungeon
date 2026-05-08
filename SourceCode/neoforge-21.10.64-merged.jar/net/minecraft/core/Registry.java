package net.minecraft.core;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.Lifecycle;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

public interface Registry<T> extends Keyable, HolderLookup.RegistryLookup<T>, IdMap<T>, net.neoforged.neoforge.registries.IRegistryExtension<T> {
    @Override
    ResourceKey<? extends Registry<T>> key();

    default Codec<T> byNameCodec() {
        return this.referenceHolderWithLifecycle()
            .flatComapMap(Holder.Reference::value, p_325515_ -> this.safeCastToReference(this.wrapAsHolder((T)p_325515_)));
    }

    default Codec<Holder<T>> holderByNameCodec() {
        return this.referenceHolderWithLifecycle().flatComapMap(p_325516_ -> (Holder<T>)p_325516_, this::safeCastToReference);
    }

    private Codec<Holder.Reference<T>> referenceHolderWithLifecycle() {
        Codec<Holder.Reference<T>> codec = ResourceLocation.CODEC
            .comapFlatMap(
                p_367803_ -> this.get(p_367803_)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + this.key() + ": " + p_367803_)),
                p_325513_ -> p_325513_.key().location()
            );
        return ExtraCodecs.overrideLifecycle(
            codec, p_325514_ -> this.registrationInfo(p_325514_.key()).map(RegistrationInfo::lifecycle).orElse(Lifecycle.experimental())
        );
    }

    private DataResult<Holder.Reference<T>> safeCastToReference(Holder<T> value) {
        return value.getDelegate() instanceof Holder.Reference reference
            ? DataResult.success(reference)
            : DataResult.error(() -> "Unregistered holder in " + this.key() + ": " + value);
    }

    @Override
    default <U> Stream<U> keys(DynamicOps<U> ops) {
        return this.keySet().stream().map(p_235784_ -> ops.createString(p_235784_.toString()));
    }

    /**
     * @return the name used to identify the given object within this registry or {@code null} if the object is not within this registry
     */
    @Nullable
    ResourceLocation getKey(T value);

    Optional<ResourceKey<T>> getResourceKey(T value);

    /**
     * @return the integer ID used to identify the given object
     */
    @Override
    int getId(@Nullable T value);

    @Nullable
    T getValue(@Nullable ResourceKey<T> key);

    @Nullable
    T getValue(@Nullable ResourceLocation key);

    Optional<RegistrationInfo> registrationInfo(ResourceKey<T> key);

    default Optional<T> getOptional(@Nullable ResourceLocation name) {
        return Optional.ofNullable(this.getValue(name));
    }

    default Optional<T> getOptional(@Nullable ResourceKey<T> registryKey) {
        return Optional.ofNullable(this.getValue(registryKey));
    }

    Optional<Holder.Reference<T>> getAny();

    default T getValueOrThrow(ResourceKey<T> key) {
        T t = this.getValue(key);
        if (t == null) {
            throw new IllegalStateException("Missing key in " + this.key() + ": " + key);
        } else {
            return t;
        }
    }

    Set<ResourceLocation> keySet();

    Set<Entry<ResourceKey<T>, T>> entrySet();

    Set<ResourceKey<T>> registryKeySet();

    Optional<Holder.Reference<T>> getRandom(RandomSource random);

    default Stream<T> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    boolean containsKey(ResourceLocation name);

    boolean containsKey(ResourceKey<T> key);

    static <T> T register(Registry<? super T> registry, String name, T value) {
        return register(registry, ResourceLocation.parse(name), value);
    }

    static <V, T extends V> T register(Registry<V> registry, ResourceLocation name, T value) {
        return register(registry, ResourceKey.create(registry.key(), name), value);
    }

    static <V, T extends V> T register(Registry<V> registry, ResourceKey<V> key, T value) {
        ((WritableRegistry)registry).register(key, (V)value, RegistrationInfo.BUILT_IN);
        return value;
    }

    static <R, T extends R> Holder.Reference<T> registerForHolder(Registry<R> registry, ResourceKey<R> key, T value) {
        return ((WritableRegistry)registry).register(key, (R)value, RegistrationInfo.BUILT_IN);
    }

    static <R, T extends R> Holder.Reference<T> registerForHolder(Registry<R> registry, ResourceLocation name, T value) {
        return registerForHolder(registry, ResourceKey.create(registry.key(), name), value);
    }

    Registry<T> freeze();

    Holder.Reference<T> createIntrusiveHolder(T value);

    Optional<Holder.Reference<T>> get(int index);

    Optional<Holder.Reference<T>> get(ResourceLocation key);

    Holder<T> wrapAsHolder(T value);

    default Iterable<Holder<T>> getTagOrEmpty(TagKey<T> key) {
        return DataFixUtils.orElse(this.get(key), List.of());
    }

    Stream<HolderSet.Named<T>> getTags();

    default IdMap<Holder<T>> asHolderIdMap() {
        return new IdMap<Holder<T>>() {
            /**
             * @return the integer ID used to identify the given object
             */
            public int getId(Holder<T> value) {
                return Registry.this.getId(value.value());
            }

            @Nullable
            public Holder<T> byId(int id) {
                return (Holder<T>)Registry.this.get(id).orElse(null);
            }

            @Override
            public int size() {
                return Registry.this.size();
            }

            @Override
            public Iterator<Holder<T>> iterator() {
                return Registry.this.listElements().map(p_260061_ -> (Holder<T>)p_260061_).iterator();
            }
        };
    }

    Registry.PendingTags<T> prepareTagReload(TagLoader.LoadResult<T> loadResult);

    public interface PendingTags<T> {
        ResourceKey<? extends Registry<? extends T>> key();

        HolderLookup.RegistryLookup<T> lookup();

        void apply();

        int size();
    }
}
