package net.minecraft.data.info;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

public class DatapackStructureReport implements DataProvider {
    private final PackOutput output;
    private static final DatapackStructureReport.Entry PSEUDO_REGISTRY = new DatapackStructureReport.Entry(true, false, true);
    private static final DatapackStructureReport.Entry STABLE_DYNAMIC_REGISTRY = new DatapackStructureReport.Entry(true, true, true);
    private static final DatapackStructureReport.Entry UNSTABLE_DYNAMIC_REGISTRY = new DatapackStructureReport.Entry(true, true, false);
    private static final DatapackStructureReport.Entry BUILT_IN_REGISTRY = new DatapackStructureReport.Entry(false, true, true);
    private static final Map<ResourceKey<? extends Registry<?>>, DatapackStructureReport.Entry> MANUAL_ENTRIES = Map.of(
        Registries.RECIPE,
        PSEUDO_REGISTRY,
        Registries.ADVANCEMENT,
        PSEUDO_REGISTRY,
        Registries.LOOT_TABLE,
        STABLE_DYNAMIC_REGISTRY,
        Registries.ITEM_MODIFIER,
        STABLE_DYNAMIC_REGISTRY,
        Registries.PREDICATE,
        STABLE_DYNAMIC_REGISTRY
    );
    private static final Map<String, DatapackStructureReport.CustomPackEntry> NON_REGISTRY_ENTRIES = Map.of(
        "structure",
        new DatapackStructureReport.CustomPackEntry(DatapackStructureReport.Format.STRUCTURE, new DatapackStructureReport.Entry(true, false, true)),
        "function",
        new DatapackStructureReport.CustomPackEntry(DatapackStructureReport.Format.MCFUNCTION, new DatapackStructureReport.Entry(true, true, true))
    );
    static final Codec<ResourceKey<? extends Registry<?>>> REGISTRY_KEY_CODEC = ResourceLocation.CODEC
        .xmap(ResourceKey::createRegistryKey, ResourceKey::location);

    public DatapackStructureReport(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        DatapackStructureReport.Report datapackstructurereport$report = new DatapackStructureReport.Report(this.listRegistries(), NON_REGISTRY_ENTRIES);
        Path path = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("datapack.json");
        return DataProvider.saveStable(
            output, DatapackStructureReport.Report.CODEC.encodeStart(JsonOps.INSTANCE, datapackstructurereport$report).getOrThrow(), path
        );
    }

    @Override
    public String getName() {
        return "Datapack Structure";
    }

    private void putIfNotPresent(
        Map<ResourceKey<? extends Registry<?>>, DatapackStructureReport.Entry> map,
        ResourceKey<? extends Registry<?>> registryKey,
        DatapackStructureReport.Entry entry
    ) {
        DatapackStructureReport.Entry datapackstructurereport$entry = map.putIfAbsent(registryKey, entry);
        if (datapackstructurereport$entry != null) {
            throw new IllegalStateException("Duplicate entry for key " + registryKey.location());
        }
    }

    private Map<ResourceKey<? extends Registry<?>>, DatapackStructureReport.Entry> listRegistries() {
        Map<ResourceKey<? extends Registry<?>>, DatapackStructureReport.Entry> map = new HashMap<>();
        BuiltInRegistries.REGISTRY.forEach(p_364296_ -> this.putIfNotPresent(map, p_364296_.key(), BUILT_IN_REGISTRY));
        RegistryDataLoader.WORLDGEN_REGISTRIES.forEach(p_364100_ -> this.putIfNotPresent(map, p_364100_.key(), UNSTABLE_DYNAMIC_REGISTRY));
        RegistryDataLoader.DIMENSION_REGISTRIES.forEach(p_365494_ -> this.putIfNotPresent(map, p_365494_.key(), UNSTABLE_DYNAMIC_REGISTRY));
        MANUAL_ENTRIES.forEach((p_362144_, p_360989_) -> this.putIfNotPresent(map, (ResourceKey<? extends Registry<?>>)p_362144_, p_360989_));
        return map;
    }

    record CustomPackEntry(DatapackStructureReport.Format format, DatapackStructureReport.Entry entry) {
        public static final Codec<DatapackStructureReport.CustomPackEntry> CODEC = RecordCodecBuilder.create(
            p_364115_ -> p_364115_.group(
                    DatapackStructureReport.Format.CODEC.fieldOf("format").forGetter(DatapackStructureReport.CustomPackEntry::format),
                    DatapackStructureReport.Entry.MAP_CODEC.forGetter(DatapackStructureReport.CustomPackEntry::entry)
                )
                .apply(p_364115_, DatapackStructureReport.CustomPackEntry::new)
        );
    }

    record Entry(boolean elements, boolean tags, boolean stable) {
        public static final MapCodec<DatapackStructureReport.Entry> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_365343_ -> p_365343_.group(
                    Codec.BOOL.fieldOf("elements").forGetter(DatapackStructureReport.Entry::elements),
                    Codec.BOOL.fieldOf("tags").forGetter(DatapackStructureReport.Entry::tags),
                    Codec.BOOL.fieldOf("stable").forGetter(DatapackStructureReport.Entry::stable)
                )
                .apply(p_365343_, DatapackStructureReport.Entry::new)
        );
        public static final Codec<DatapackStructureReport.Entry> CODEC = MAP_CODEC.codec();
    }

    static enum Format implements StringRepresentable {
        STRUCTURE("structure"),
        MCFUNCTION("mcfunction");

        public static final Codec<DatapackStructureReport.Format> CODEC = StringRepresentable.fromEnum(DatapackStructureReport.Format::values);
        private final String name;

        private Format(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    record Report(
        Map<ResourceKey<? extends Registry<?>>, DatapackStructureReport.Entry> registries, Map<String, DatapackStructureReport.CustomPackEntry> others
    ) {
        public static final Codec<DatapackStructureReport.Report> CODEC = RecordCodecBuilder.create(
            p_363616_ -> p_363616_.group(
                    Codec.unboundedMap(DatapackStructureReport.REGISTRY_KEY_CODEC, DatapackStructureReport.Entry.CODEC)
                        .fieldOf("registries")
                        .forGetter(DatapackStructureReport.Report::registries),
                    Codec.unboundedMap(Codec.STRING, DatapackStructureReport.CustomPackEntry.CODEC)
                        .fieldOf("others")
                        .forGetter(DatapackStructureReport.Report::others)
                )
                .apply(p_363616_, DatapackStructureReport.Report::new)
        );
    }
}
