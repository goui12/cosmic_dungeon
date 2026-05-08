package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.RandomSource;

public class MappedRegistry<T> extends net.neoforged.neoforge.registries.BaseMappedRegistry<T> implements WritableRegistry<T> {
    private final ResourceKey<? extends Registry<T>> key;
    private final ObjectList<Holder.Reference<T>> byId = new ObjectArrayList<>(256);
    private final Reference2IntMap<T> toId = Util.make(new Reference2IntOpenHashMap<>(), p_304142_ -> p_304142_.defaultReturnValue(-1));
    private final Map<ResourceLocation, Holder.Reference<T>> byLocation = new HashMap<>();
    private final Map<ResourceKey<T>, Holder.Reference<T>> byKey = new HashMap<>();
    private final Map<T, Holder.Reference<T>> byValue = new IdentityHashMap<>();
    private final Map<ResourceKey<T>, RegistrationInfo> registrationInfos = new IdentityHashMap<>();
    private Lifecycle registryLifecycle;
    private final Map<TagKey<T>, HolderSet.Named<T>> frozenTags = new IdentityHashMap<>();
    MappedRegistry.TagSet<T> allTags = MappedRegistry.TagSet.unbound();
    private boolean frozen;
    @Nullable
    private Map<T, Holder.Reference<T>> unregisteredIntrusiveHolders;

    @Override
    public Stream<HolderSet.Named<T>> listTags() {
        return this.getTags();
    }

    public MappedRegistry(ResourceKey<? extends Registry<T>> key, Lifecycle registryLifecycle) {
        this(key, registryLifecycle, false);
    }

    public MappedRegistry(ResourceKey<? extends Registry<T>> key, Lifecycle registryLifecycle, boolean hasIntrusiveHolders) {
        this.key = key;
        this.registryLifecycle = registryLifecycle;
        if (hasIntrusiveHolders) {
            this.unregisteredIntrusiveHolders = new IdentityHashMap<>();
        }
    }

    @Override
    public ResourceKey<? extends Registry<T>> key() {
        return this.key;
    }

    @Override
    public String toString() {
        return "Registry[" + this.key + " (" + this.registryLifecycle + ")]";
    }

    private void validateWrite() {
        if (this.frozen) {
            throw new IllegalStateException("Registry is already frozen");
        }
    }

    private void validateWrite(ResourceKey<T> key) {
        if (this.frozen) {
            throw new IllegalStateException("Registry is already frozen (trying to add key " + key + ")");
        }
    }

    @Override
    public Holder.Reference<T> register(ResourceKey<T> key, T value, RegistrationInfo registrationInfo) {
        return register(this.byId.size(), key, value, registrationInfo);
    }

    public Holder.Reference<T> register(int id, ResourceKey<T> p_256252_, T p_256591_, RegistrationInfo p_326235_) {
        this.validateWrite(p_256252_);
        Objects.requireNonNull(p_256252_);
        Objects.requireNonNull(p_256591_);
        int i = id;
        if (i > this.getMaxId())
            throw new IllegalStateException(String.format(java.util.Locale.ENGLISH, "Invalid id %d - maximum id range of %d exceeded.", i, this.getMaxId()));

        if (this.byLocation.containsKey(p_256252_.location())) {
            throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Adding duplicate key '" + p_256252_ + "' to registry"));
        } else if (this.byValue.containsKey(p_256591_)) {
            throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Adding duplicate value '" + p_256591_ + "' to registry"));
        } else {
            Holder.Reference<T> reference;
            if (this.unregisteredIntrusiveHolders != null) {
                reference = this.unregisteredIntrusiveHolders.remove(p_256591_);
                if (reference == null) {
                    throw new AssertionError("Missing intrusive holder for " + p_256252_ + ":" + p_256591_);
                }

                reference.bindKey(p_256252_);
            } else {
                reference = this.byKey.computeIfAbsent(p_256252_, p_367800_ -> Holder.Reference.createStandAlone(this, (ResourceKey<T>)p_367800_));
                // Forge: Bind the value immediately so it can be queried while the registry is not frozen
                reference.bindValue(p_256591_);
            }

            this.byKey.put(p_256252_, reference);
            this.byLocation.put(p_256252_.location(), reference);
            this.byValue.put(p_256591_, reference);
            this.byId.add(reference);
            this.toId.put(p_256591_, i);
            this.registrationInfos.put(p_256252_, p_326235_);
            this.registryLifecycle = this.registryLifecycle.add(p_326235_.lifecycle());
            this.addCallbacks.forEach(addCallback -> addCallback.onAdd(this, i, p_256252_, p_256591_));
            return reference;
        }
    }

    /**
     * @return the name used to identify the given object within this registry or {@code null} if the object is not within this registry
     */
    @Nullable
    @Override
    public ResourceLocation getKey(T value) {
        Holder.Reference<T> reference = this.byValue.get(value);
        return reference != null ? reference.key().location() : null;
    }

    @Override
    public Optional<ResourceKey<T>> getResourceKey(T value) {
        return Optional.ofNullable(this.byValue.get(value)).map(Holder.Reference::key);
    }

    /**
     * @return the integer ID used to identify the given object
     */
    @Override
    public int getId(@Nullable T value) {
        return this.toId.getInt(value);
    }

    @Nullable
    @Override
    public T getValue(@Nullable ResourceKey<T> key) {
        return getValueFromNullable(this.byKey.get(resolve(key)));
    }

    @Nullable
    @Override
    public T byId(int id) {
        return id >= 0 && id < this.byId.size() ? this.byId.get(id).value() : null;
    }

    @Override
    public Optional<Holder.Reference<T>> get(int index) {
        return index >= 0 && index < this.byId.size() ? Optional.ofNullable(this.byId.get(index)) : Optional.empty();
    }

    @Override
    public Optional<Holder.Reference<T>> get(ResourceLocation key) {
        return Optional.ofNullable(this.byLocation.get(resolve(key)));
    }

    @Override
    public Optional<Holder.Reference<T>> get(ResourceKey<T> resourceKey) {
        return Optional.ofNullable(this.byKey.get(resolve(resourceKey)));
    }

    @Override
    public Optional<Holder.Reference<T>> getAny() {
        return this.byId.isEmpty() ? Optional.empty() : Optional.of(this.byId.getFirst());
    }

    @Override
    public Holder<T> wrapAsHolder(T value) {
        Holder.Reference<T> reference = this.byValue.get(value);
        return (Holder<T>)(reference != null ? reference : Holder.direct(value));
    }

    Holder.Reference<T> getOrCreateHolderOrThrow(ResourceKey<T> key) {
        return this.byKey.computeIfAbsent(resolve(key), p_258169_ -> {
            if (this.unregisteredIntrusiveHolders != null) {
                throw new IllegalStateException("This registry can't create new holders without value");
            } else {
                this.validateWrite((ResourceKey<T>)p_258169_);
                return Holder.Reference.createStandAlone(this, (ResourceKey<T>)p_258169_);
            }
        });
    }

    @Override
    public int size() {
        return this.byKey.size();
    }

    @Override
    public Optional<RegistrationInfo> registrationInfo(ResourceKey<T> key) {
        return Optional.ofNullable(this.registrationInfos.get(key));
    }

    @Override
    public Lifecycle registryLifecycle() {
        return this.registryLifecycle;
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.transform(this.byId.iterator(), Holder::value);
    }

    @Nullable
    @Override
    public T getValue(@Nullable ResourceLocation key) {
        Holder.Reference<T> reference = this.byLocation.get(key != null ? resolve(key) : null);
        return getValueFromNullable(reference);
    }

    @Nullable
    private static <T> T getValueFromNullable(@Nullable Holder.Reference<T> holder) {
        return holder != null ? holder.value() : null;
    }

    @Override
    public Set<ResourceLocation> keySet() {
        return Collections.unmodifiableSet(this.byLocation.keySet());
    }

    @Override
    public Set<ResourceKey<T>> registryKeySet() {
        return Collections.unmodifiableSet(this.byKey.keySet());
    }

    @Override
    public Set<Entry<ResourceKey<T>, T>> entrySet() {
        return Collections.unmodifiableSet(Util.mapValuesLazy(this.byKey, Holder::value).entrySet());
    }

    @Override
    public Stream<Holder.Reference<T>> listElements() {
        return this.byId.stream();
    }

    @Override
    public Stream<HolderSet.Named<T>> getTags() {
        return this.allTags.getTags();
    }

    HolderSet.Named<T> getOrCreateTagForRegistration(TagKey<T> key) {
        return this.frozenTags.computeIfAbsent(key, this::createTag);
    }

    private HolderSet.Named<T> createTag(TagKey<T> key) {
        return new HolderSet.Named<>(this, key);
    }

    @Override
    public boolean isEmpty() {
        return this.byKey.isEmpty();
    }

    @Override
    public Optional<Holder.Reference<T>> getRandom(RandomSource random) {
        return Util.getRandomSafe(this.byId, random);
    }

    @Override
    public boolean containsKey(ResourceLocation name) {
        return this.byLocation.containsKey(name);
    }

    @Override
    public boolean containsKey(ResourceKey<T> key) {
        return this.byKey.containsKey(key);
    }

    /** @deprecated Forge: For internal use only. Use the Register events when registering values. */
    @Deprecated
    @Override
    public void unfreeze(boolean clearTags) {
        if (clearTags) {
            // unbind tags which were bound by the previous freeze
            this.allTags = MappedRegistry.TagSet.unbound();
        }
        this.frozen = false;
    }

    @Override
    public Registry<T> freeze() {
        if (this.frozen) {
            return this;
        } else {
            this.frozen = true;
            List<ResourceLocation> list = this.byKey
                .entrySet()
                .stream()
                .filter(p_211055_ -> !p_211055_.getValue().isBound())
                .map(p_211794_ -> p_211794_.getKey().location())
                .sorted()
                .toList();
            if (!list.isEmpty()) {
                throw new IllegalStateException("Unbound values in registry " + this.key() + ": " + list);
            } else {
                if (this.unregisteredIntrusiveHolders != null) {
                    if (!this.unregisteredIntrusiveHolders.isEmpty()) {
                        throw new IllegalStateException("Some intrusive holders were not registered: " + this.unregisteredIntrusiveHolders.values());
                    }

                    // Neo: We freeze/unfreeze vanilla registries more than once, so we need to keep the unregistered intrusive holders map around.
                    // this.unregisteredIntrusiveHolders = null;
                }
                this.bakeCallbacks.forEach(bakeCallback -> bakeCallback.onBake(this));

                if (this.allTags.isBound()) {
                    // Neo: we freeze/unfreeze the registry to apply snapshots and tags need to be preserved in those cases
                    //throw new IllegalStateException("Tags already present before freezing");
                    return this;
                } else {
                    List<ResourceLocation> list1 = this.frozenTags
                        .entrySet()
                        .stream()
                        .filter(p_359337_ -> !p_359337_.getValue().isBound())
                        .map(p_359338_ -> p_359338_.getKey().location())
                        .sorted()
                        .toList();
                    if (!list1.isEmpty()) {
                        throw new IllegalStateException("Unbound tags in registry " + this.key() + ": " + list1);
                    } else {
                        this.allTags = MappedRegistry.TagSet.fromMap(this.frozenTags);
                        this.refreshTagsInHolders();
                        return this;
                    }
                }
            }
        }
    }

    @Override
    public Holder.Reference<T> createIntrusiveHolder(T value) {
        if (this.unregisteredIntrusiveHolders == null) {
            throw new IllegalStateException("This registry can't create intrusive holders");
        } else {
            this.validateWrite();
            return this.unregisteredIntrusiveHolders.computeIfAbsent(value, p_367802_ -> Holder.Reference.createIntrusive(this, (T)p_367802_));
        }
    }

    @Override
    public Optional<HolderSet.Named<T>> get(TagKey<T> tagKey) {
        return this.allTags.get(tagKey);
    }

    private Holder.Reference<T> validateAndUnwrapTagElement(TagKey<T> key, Holder<T> value) {
        if (!value.canSerializeIn(this)) {
            throw new IllegalStateException("Can't create named set " + key + " containing value " + value + " from outside registry " + this);
        } else if (value instanceof Holder.Reference<T> reference) {
            return reference;
        } else {
            throw new IllegalStateException("Found direct holder " + value + " value in tag " + key);
        }
    }

    @Override
    public void bindTag(TagKey<T> tag, List<Holder<T>> values) {
        this.validateWrite();
        this.getOrCreateTagForRegistration(tag).bind(values);
    }

    void refreshTagsInHolders() {
        Map<Holder.Reference<T>, List<TagKey<T>>> map = new IdentityHashMap<>();
        this.byKey.values().forEach(p_211801_ -> map.put((Holder.Reference<T>)p_211801_, new ArrayList<>()));
        this.allTags.forEach((p_359335_, p_359336_) -> {
            for (Holder<T> holder : p_359336_) {
                Holder.Reference<T> reference = this.validateAndUnwrapTagElement((TagKey<T>)p_359335_, holder);
                map.get(reference).add((TagKey<T>)p_359335_);
            }
        });
        map.forEach(Holder.Reference::bindTags);
    }

    public void bindAllTagsToEmpty() {
        this.validateWrite();
        this.frozenTags.values().forEach(p_211792_ -> p_211792_.bind(List.of()));
    }

    @Override
    public HolderGetter<T> createRegistrationLookup() {
        this.validateWrite();
        return new HolderGetter<T>() {
            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> p_255624_) {
                return Optional.of(this.getOrThrow(p_255624_));
            }

            @Override
            public Holder.Reference<T> getOrThrow(ResourceKey<T> p_368503_) {
                return MappedRegistry.this.getOrCreateHolderOrThrow(p_368503_);
            }

            @Override
            public Optional<HolderSet.Named<T>> get(TagKey<T> p_256277_) {
                return Optional.of(this.getOrThrow(p_256277_));
            }

            @Override
            public HolderSet.Named<T> getOrThrow(TagKey<T> p_368701_) {
                return MappedRegistry.this.getOrCreateTagForRegistration(p_368701_);
            }
        };
    }

    @Override
    public Registry.PendingTags<T> prepareTagReload(TagLoader.LoadResult<T> loadResult) {
        if (!this.frozen) {
            throw new IllegalStateException("Invalid method used for tag loading");
        } else {
            Builder<TagKey<T>, HolderSet.Named<T>> builder = ImmutableMap.builder();
            final Map<TagKey<T>, List<Holder<T>>> map = new HashMap<>();
            loadResult.tags().forEach((p_359332_, p_359333_) -> {
                HolderSet.Named<T> named = this.frozenTags.get(p_359332_);
                if (named == null) {
                    named = this.createTag((TagKey<T>)p_359332_);
                }

                builder.put((TagKey<T>)p_359332_, named);
                map.put((TagKey<T>)p_359332_, List.copyOf(p_359333_));
            });
            final ImmutableMap<TagKey<T>, HolderSet.Named<T>> immutablemap = builder.build();
            final HolderLookup.RegistryLookup<T> registrylookup = new HolderLookup.RegistryLookup.Delegate<T>() {
                @Override
                public HolderLookup.RegistryLookup<T> parent() {
                    return MappedRegistry.this;
                }

                @Override
                public Optional<HolderSet.Named<T>> get(TagKey<T> p_259486_) {
                    return Optional.ofNullable(immutablemap.get(p_259486_));
                }

                @Override
                public Stream<HolderSet.Named<T>> listTags() {
                    return immutablemap.values().stream();
                }
            };
            return new Registry.PendingTags<T>() {
                @Override
                public ResourceKey<? extends Registry<? extends T>> key() {
                    return MappedRegistry.this.key();
                }

                @Override
                public int size() {
                    return map.size();
                }

                @Override
                public HolderLookup.RegistryLookup<T> lookup() {
                    return registrylookup;
                }

                @Override
                public void apply() {
                    immutablemap.forEach((p_368608_, p_368715_) -> {
                        List<Holder<T>> list = map.getOrDefault(p_368608_, List.of());
                        p_368715_.bind(list);
                    });
                    MappedRegistry.this.allTags = MappedRegistry.TagSet.fromMap(immutablemap);
                    MappedRegistry.this.refreshTagsInHolders();
                }
            };
        }
    }

    @Override
    protected void clear(boolean full) {
        this.validateWrite();
        this.clearCallbacks.forEach(clearCallback -> clearCallback.onClear(this, full));
        super.clear(full);
        this.byId.clear();
        this.toId.clear();
        if (full) {
            this.byLocation.clear();
            this.byKey.clear();
            this.byValue.clear();
            this.allTags = MappedRegistry.TagSet.unbound();
            this.frozenTags.entrySet().removeIf(entry -> !entry.getValue().isBound());
            if (unregisteredIntrusiveHolders != null) {
                unregisteredIntrusiveHolders.clear();
                unregisteredIntrusiveHolders = null;
            }
        }
    }

    @Override
    protected void registerIdMapping(ResourceKey<T> key, int id) {
        this.validateWrite(key);
        if (id > this.getMaxId())
            throw new IllegalStateException(String.format(java.util.Locale.ENGLISH, "Invalid id %d - maximum id range of %d exceeded.", id, this.getMaxId()));
        if (0 <= id && id < this.byId.size() && this.byId.get(id) != null) { // Don't use byId() method, it will return the default value if the entry is absent
            throw new IllegalStateException("Duplicate id " + id + " for " + key + " and " + this.getKey(this.byId.get(id).value()));
        }
        var holder = byKey.get(key);
        while (this.byId.size() < (id + 1)) this.byId.add(null);
        this.byId.set(id, holder);
        this.toId.put(holder.value(), id);
    }

    @Override
    public int getId(ResourceLocation name) {
        return getId(getValue(name));
    }

    @Override
    public int getId(ResourceKey<T> key) {
        return getId(getValue(key));
    }

    @Override
    public boolean containsValue(T value) {
        return byValue.containsKey(value);
    }

    interface TagSet<T> {
        static <T> MappedRegistry.TagSet<T> unbound() {
            return new MappedRegistry.TagSet<T>() {
                @Override
                public boolean isBound() {
                    return false;
                }

                @Override
                public Optional<HolderSet.Named<T>> get(TagKey<T> p_363319_) {
                    throw new IllegalStateException("Tags not bound, trying to access " + p_363319_);
                }

                @Override
                public void forEach(BiConsumer<? super TagKey<T>, ? super HolderSet.Named<T>> p_361880_) {
                    throw new IllegalStateException("Tags not bound");
                }

                @Override
                public Stream<HolderSet.Named<T>> getTags() {
                    throw new IllegalStateException("Tags not bound");
                }
            };
        }

        static <T> MappedRegistry.TagSet<T> fromMap(final Map<TagKey<T>, HolderSet.Named<T>> map) {
            return new MappedRegistry.TagSet<T>() {
                @Override
                public boolean isBound() {
                    return true;
                }

                @Override
                public Optional<HolderSet.Named<T>> get(TagKey<T> p_362077_) {
                    return Optional.ofNullable(map.get(p_362077_));
                }

                @Override
                public void forEach(BiConsumer<? super TagKey<T>, ? super HolderSet.Named<T>> p_363215_) {
                    map.forEach(p_363215_);
                }

                @Override
                public Stream<HolderSet.Named<T>> getTags() {
                    return map.values().stream();
                }
            };
        }

        boolean isBound();

        Optional<HolderSet.Named<T>> get(TagKey<T> key);

        void forEach(BiConsumer<? super TagKey<T>, ? super HolderSet.Named<T>> action);

        Stream<HolderSet.Named<T>> getTags();
    }
}
