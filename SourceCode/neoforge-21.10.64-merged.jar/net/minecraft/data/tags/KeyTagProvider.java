package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;

public abstract class KeyTagProvider<T> extends TagsProvider<T> {
    /**
     * @deprecated Neo: Use {@link #KeyTagProvider(PackOutput,ResourceKey,
     *             CompletableFuture,String)}
     */
    @Deprecated
    protected KeyTagProvider(PackOutput output, ResourceKey<? extends Registry<T>> registryKey, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        this(output, registryKey, lookupProvider, "vanilla");
    }

    protected KeyTagProvider(PackOutput output, ResourceKey<? extends Registry<T>> registryKey, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId) {
        super(output, registryKey, lookupProvider, modId);
    }

    protected TagAppender<ResourceKey<T>, T> tag(TagKey<T> key) {
        TagBuilder tagbuilder = this.getOrCreateRawBuilder(key);
        return TagAppender.forBuilder(tagbuilder);
    }
}
