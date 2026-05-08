package net.minecraft.client.multiplayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagNetworkSerialization;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RegistryDataCollector {
    @Nullable
    private RegistryDataCollector.ContentsCollector contentsCollector;
    @Nullable
    private RegistryDataCollector.TagCollector tagCollector;

    public void appendContents(ResourceKey<? extends Registry<?>> registryKey, List<RegistrySynchronization.PackedRegistryEntry> registryEntries) {
        if (this.contentsCollector == null) {
            this.contentsCollector = new RegistryDataCollector.ContentsCollector();
        }

        this.contentsCollector.append(registryKey, registryEntries);
    }

    public void appendTags(Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags) {
        if (this.tagCollector == null) {
            this.tagCollector = new RegistryDataCollector.TagCollector();
        }

        tags.forEach(this.tagCollector::append);
    }

    private static <T> Registry.PendingTags<T> resolveRegistryTags(
        RegistryAccess.Frozen registryAccess, ResourceKey<? extends Registry<? extends T>> registryKey, TagNetworkSerialization.NetworkPayload payload
    ) {
        Registry<T> registry = registryAccess.lookupOrThrow(registryKey);
        return registry.prepareTagReload(payload.resolve(registry));
    }

    private RegistryAccess loadNewElementsAndTags(ResourceProvider resourceProvider, RegistryDataCollector.ContentsCollector contentCollector, boolean isMemoryConnection) {
        LayeredRegistryAccess<ClientRegistryLayer> layeredregistryaccess = ClientRegistryLayer.createRegistryAccess();
        RegistryAccess.Frozen registryaccess$frozen = layeredregistryaccess.getAccessForLoading(ClientRegistryLayer.REMOTE);
        Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> map = new HashMap<>();
        contentCollector.elements
            .forEach(
                (p_360955_, p_361011_) -> map.put(
                    (ResourceKey<? extends Registry<?>>)p_360955_,
                    new RegistryDataLoader.NetworkedRegistryData(
                        (List<RegistrySynchronization.PackedRegistryEntry>)p_361011_, TagNetworkSerialization.NetworkPayload.EMPTY
                    )
                )
            );
        List<Registry.PendingTags<?>> list = new ArrayList<>();
        if (this.tagCollector != null) {
            this.tagCollector.forEach((p_364406_, p_365188_) -> {
                if (!p_365188_.isEmpty()) {
                    if (RegistrySynchronization.isNetworkable((ResourceKey<? extends Registry<?>>)p_364406_)) {
                        map.compute((ResourceKey<? extends Registry<?>>)p_364406_, (p_363401_, p_364225_) -> {
                            List<RegistrySynchronization.PackedRegistryEntry> list2 = p_364225_ != null ? p_364225_.elements() : List.of();
                            return new RegistryDataLoader.NetworkedRegistryData(list2, p_365188_);
                        });
                    } else if (!isMemoryConnection) {
                        list.add(resolveRegistryTags(registryaccess$frozen, (ResourceKey<? extends Registry<?>>)p_364406_, p_365188_));
                    }
                }
            });
        }

        List<HolderLookup.RegistryLookup<?>> list1 = TagLoader.buildUpdatedLookups(registryaccess$frozen, list);

        RegistryAccess.Frozen registryaccess$frozen1;
        try {
            registryaccess$frozen1 = RegistryDataLoader.load(map, resourceProvider, list1, RegistryDataLoader.SYNCHRONIZED_REGISTRIES).freeze();
        } catch (Exception exception) {
            CrashReport crashreport = CrashReport.forThrowable(exception, "Network Registry Load");
            addCrashDetails(crashreport, map, list);
            throw new ReportedException(crashreport);
        }

        RegistryAccess registryaccess = layeredregistryaccess.replaceFrom(ClientRegistryLayer.REMOTE, registryaccess$frozen1).compositeAccess();
        list.forEach(Registry.PendingTags::apply);
        return registryaccess;
    }

    private static void addCrashDetails(
        CrashReport crashReport,
        Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> dynamicRegistries,
        List<Registry.PendingTags<?>> staticRegistries
    ) {
        CrashReportCategory crashreportcategory = crashReport.addCategory("Received Elements and Tags");
        crashreportcategory.setDetail(
            "Dynamic Registries",
            () -> dynamicRegistries.entrySet()
                .stream()
                .sorted(Comparator.comparing(p_378804_ -> p_378804_.getKey().location()))
                .map(
                    p_378806_ -> String.format(
                        Locale.ROOT,
                        "\n\t\t%s: elements=%d tags=%d",
                        p_378806_.getKey().location(),
                        p_378806_.getValue().elements().size(),
                        p_378806_.getValue().tags().size()
                    )
                )
                .collect(Collectors.joining())
        );
        crashreportcategory.setDetail(
            "Static Registries",
            () -> staticRegistries.stream()
                .sorted(Comparator.comparing(p_378808_ -> p_378808_.key().location()))
                .map(p_378803_ -> String.format(Locale.ROOT, "\n\t\t%s: tags=%d", p_378803_.key().location(), p_378803_.size()))
                .collect(Collectors.joining())
        );
    }

    private void loadOnlyTags(RegistryDataCollector.TagCollector tagCollector, RegistryAccess.Frozen registryAccess, boolean isMemoryConnection) {
        tagCollector.forEach((p_360314_, p_361795_) -> {
            if (isMemoryConnection || RegistrySynchronization.isNetworkable((ResourceKey<? extends Registry<?>>)p_360314_)) {
                resolveRegistryTags(registryAccess, (ResourceKey<? extends Registry<?>>)p_360314_, p_361795_).apply();
            }
        });
    }

    public RegistryAccess.Frozen collectGameRegistries(ResourceProvider resourceProvider, RegistryAccess.Frozen registryAccess, boolean isMemoryConnection) {
        RegistryAccess registryaccess;
        if (this.contentsCollector != null) {
            registryaccess = this.loadNewElementsAndTags(resourceProvider, this.contentsCollector, isMemoryConnection);
        } else {
            if (this.tagCollector != null) {
                this.loadOnlyTags(this.tagCollector, registryAccess, !isMemoryConnection);
            }

            registryaccess = registryAccess;
        }

        return registryaccess.freeze();
    }

    @OnlyIn(Dist.CLIENT)
    static class ContentsCollector {
        final Map<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> elements = new HashMap<>();

        public void append(ResourceKey<? extends Registry<?>> registryKey, List<RegistrySynchronization.PackedRegistryEntry> entries) {
            this.elements.computeIfAbsent(registryKey, p_321745_ -> new ArrayList<>()).addAll(entries);
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class TagCollector {
        private final Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags = new HashMap<>();

        public void append(ResourceKey<? extends Registry<?>> registryKey, TagNetworkSerialization.NetworkPayload payload) {
            this.tags.put(registryKey, payload);
        }

        public void forEach(BiConsumer<? super ResourceKey<? extends Registry<?>>, ? super TagNetworkSerialization.NetworkPayload> action) {
            this.tags.forEach(action);
        }
    }
}
