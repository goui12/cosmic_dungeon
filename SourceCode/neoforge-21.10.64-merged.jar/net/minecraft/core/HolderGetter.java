package net.minecraft.core;

import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;

public interface HolderGetter<T> {
    Optional<Holder.Reference<T>> get(ResourceKey<T> resourceKey);

    default Holder.Reference<T> getOrThrow(ResourceKey<T> resourceKey) {
        return this.get(resourceKey).orElseThrow(() -> new IllegalStateException("Missing element " + resourceKey));
    }

    Optional<HolderSet.Named<T>> get(TagKey<T> tagKey);

    default HolderSet.Named<T> getOrThrow(TagKey<T> tagKey) {
        return this.get(tagKey).orElseThrow(() -> new IllegalStateException("Missing tag " + tagKey));
    }

    default Optional<Holder<T>> getRandomElementOf(TagKey<T> tagKey, RandomSource random) {
        return this.get(tagKey).flatMap(p_432337_ -> p_432337_.getRandomElement(random));
    }

    public interface Provider {
        <T> Optional<? extends HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey);

        default <T> HolderGetter<T> lookupOrThrow(ResourceKey<? extends Registry<? extends T>> registryKey) {
            return (HolderGetter<T>)this.lookup(registryKey).orElseThrow(() -> new IllegalStateException("Registry " + registryKey.location() + " not found"));
        }

        default <T> Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
            return this.lookup(key.registryKey()).flatMap(p_335174_ -> p_335174_.get(key));
        }

        default <T> Holder.Reference<T> getOrThrow(ResourceKey<T> key) {
            return this.lookup(key.registryKey())
                .flatMap(p_396323_ -> p_396323_.get(key))
                .orElseThrow(() -> new IllegalStateException("Missing element " + key));
        }
    }
}
