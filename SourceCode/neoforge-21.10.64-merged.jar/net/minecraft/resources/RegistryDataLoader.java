package net.minecraft.resources;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.ChickenVariant;
import net.minecraft.world.entity.animal.CowVariant;
import net.minecraft.world.entity.animal.PigVariant;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.slf4j.Logger;

public class RegistryDataLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Comparator<ResourceKey<?>> ERROR_KEY_COMPARATOR = Comparator.<ResourceKey<?>, ResourceLocation>comparing(ResourceKey::registry).thenComparing(ResourceKey::location);
    private static final RegistrationInfo NETWORK_REGISTRATION_INFO = new RegistrationInfo(Optional.empty(), Lifecycle.experimental());
    private static final Function<Optional<KnownPack>, RegistrationInfo> REGISTRATION_INFO_CACHE = Util.memoize(p_325559_ -> {
        Lifecycle lifecycle = p_325559_.map(KnownPack::isVanilla).map(p_325560_ -> Lifecycle.stable()).orElse(Lifecycle.experimental());
        return new RegistrationInfo(p_325559_, lifecycle);
    });
    public static final List<RegistryDataLoader.RegistryData<?>> WORLDGEN_REGISTRIES = List.of(
        new RegistryDataLoader.RegistryData<>(Registries.DIMENSION_TYPE, DimensionType.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.BIOME, Biome.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.CHAT_TYPE, ChatType.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.CONFIGURED_CARVER, ConfiguredWorldCarver.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.CONFIGURED_FEATURE, ConfiguredFeature.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.PLACED_FEATURE, PlacedFeature.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.STRUCTURE, Structure.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.STRUCTURE_SET, StructureSet.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.PROCESSOR_LIST, StructureProcessorType.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.TEMPLATE_POOL, StructureTemplatePool.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.NOISE_SETTINGS, NoiseGeneratorSettings.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.NOISE, NormalNoise.NoiseParameters.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.DENSITY_FUNCTION, DensityFunction.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.WORLD_PRESET, WorldPreset.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.FLAT_LEVEL_GENERATOR_PRESET, FlatLevelGeneratorPreset.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.TRIM_PATTERN, TrimPattern.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.TRIM_MATERIAL, TrimMaterial.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.TRIAL_SPAWNER_CONFIG, TrialSpawnerConfig.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.WOLF_VARIANT, WolfVariant.DIRECT_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.WOLF_SOUND_VARIANT, WolfSoundVariant.DIRECT_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.PIG_VARIANT, PigVariant.DIRECT_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.FROG_VARIANT, FrogVariant.DIRECT_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.CAT_VARIANT, CatVariant.DIRECT_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.COW_VARIANT, CowVariant.DIRECT_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.CHICKEN_VARIANT, ChickenVariant.DIRECT_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.PAINTING_VARIANT, PaintingVariant.DIRECT_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.DAMAGE_TYPE, DamageType.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, MultiNoiseBiomeSourceParameterList.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.BANNER_PATTERN, BannerPattern.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.ENCHANTMENT_PROVIDER, EnchantmentProvider.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.JUKEBOX_SONG, JukeboxSong.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.INSTRUMENT, Instrument.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.TEST_ENVIRONMENT, TestEnvironmentDefinition.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.TEST_INSTANCE, GameTestInstance.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.DIALOG, Dialog.DIRECT_CODEC)
    );
    public static final List<RegistryDataLoader.RegistryData<?>> DIMENSION_REGISTRIES = List.of(
        new RegistryDataLoader.RegistryData<>(Registries.LEVEL_STEM, LevelStem.CODEC)
    );
    public static final List<RegistryDataLoader.RegistryData<?>> SYNCHRONIZED_REGISTRIES = net.neoforged.neoforge.registries.DataPackRegistriesHooks.grabNetworkableRegistries(List.of(
        new RegistryDataLoader.RegistryData<>(Registries.BIOME, Biome.NETWORK_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.CHAT_TYPE, ChatType.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.TRIM_PATTERN, TrimPattern.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.TRIM_MATERIAL, TrimMaterial.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.WOLF_VARIANT, WolfVariant.NETWORK_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.WOLF_SOUND_VARIANT, WolfSoundVariant.NETWORK_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.PIG_VARIANT, PigVariant.NETWORK_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.FROG_VARIANT, FrogVariant.NETWORK_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.CAT_VARIANT, CatVariant.NETWORK_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.COW_VARIANT, CowVariant.NETWORK_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.CHICKEN_VARIANT, ChickenVariant.NETWORK_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.PAINTING_VARIANT, PaintingVariant.DIRECT_CODEC, true),
        new RegistryDataLoader.RegistryData<>(Registries.DIMENSION_TYPE, DimensionType.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.DAMAGE_TYPE, DamageType.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.BANNER_PATTERN, BannerPattern.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.JUKEBOX_SONG, JukeboxSong.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.INSTRUMENT, Instrument.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.TEST_ENVIRONMENT, TestEnvironmentDefinition.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.TEST_INSTANCE, GameTestInstance.DIRECT_CODEC),
        new RegistryDataLoader.RegistryData<>(Registries.DIALOG, Dialog.DIRECT_CODEC)
    )); // Neo: Keep the list so custom registries can be added later

    public static RegistryAccess.Frozen load(
        ResourceManager resourceManager, List<HolderLookup.RegistryLookup<?>> registryLookups, List<RegistryDataLoader.RegistryData<?>> registryData
    ) {
        return load((p_321412_, p_321413_) -> p_321412_.loadFromResources(resourceManager, p_321413_), registryLookups, registryData, true);
    }

    public static RegistryAccess.Frozen load(
        Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> elements,
        ResourceProvider resourceProvider,
        List<HolderLookup.RegistryLookup<?>> registryLookups,
        List<RegistryDataLoader.RegistryData<?>> registryData
    ) {
        return load((p_325557_, p_325558_) -> p_325557_.loadFromNetwork(elements, resourceProvider, p_325558_), registryLookups, registryData, false);
    }

    /**
     * @deprecated Neo: use {@link #load(LoadingFunction, List, List, boolean) the
     *             source-aware variant}
     */
    @Deprecated
    private static RegistryAccess.Frozen load(
        RegistryDataLoader.LoadingFunction loadingFunction, List<HolderLookup.RegistryLookup<?>> registryLookups, List<RegistryDataLoader.RegistryData<?>> registryData
    ) {
        return load(loadingFunction, registryLookups, registryData, false);
    }
    private static RegistryAccess.Frozen load(
        RegistryDataLoader.LoadingFunction loadingFunction, List<HolderLookup.RegistryLookup<?>> registryLookups, List<RegistryDataLoader.RegistryData<?>> registryData, boolean fromResources
    ) {
        Map<ResourceKey<?>, Exception> map = new HashMap<>();
        List<RegistryDataLoader.Loader<?>> list = registryData.stream()
            .map(p_321410_ -> p_321410_.create(Lifecycle.stable(), map))
            .collect(Collectors.toUnmodifiableList());
        RegistryOps.RegistryInfoLookup registryops$registryinfolookup = createContext(registryLookups, list);
        list.forEach(p_321416_ -> loadingFunction.apply((RegistryDataLoader.Loader<?>)p_321416_, registryops$registryinfolookup));
        if (fromResources && net.neoforged.neoforge.gametest.GameTestHooks.isGametestEnabled()) {
            var byKeyMap = list.stream().collect(java.util.stream.Collectors.toMap(d -> d.registry().key(), d -> d.registry));
            var envRegistry = (WritableRegistry) byKeyMap.get(Registries.TEST_ENVIRONMENT);
            var testsRegistry = (WritableRegistry) byKeyMap.get(Registries.TEST_INSTANCE);
            if (envRegistry != null && testsRegistry != null) {
                net.neoforged.fml.ModLoader.postEvent(new net.neoforged.neoforge.event.RegisterGameTestsEvent(envRegistry, testsRegistry));
            }
        }
        list.forEach(p_396495_ -> {
            Registry<?> registry = p_396495_.registry();

            try {
                registry.freeze();
            } catch (Exception exception) {
                map.put(registry.key(), exception);
            }

            if (p_396495_.data.requiredNonEmpty && registry.size() == 0) {
                map.put(registry.key(), new IllegalStateException("Registry must be non-empty: " + registry.key().location()));
            }
        });
        if (!map.isEmpty()) {
            throw logErrors(map);
        } else {
            return new RegistryAccess.ImmutableRegistryAccess(list.stream().map(RegistryDataLoader.Loader::registry).toList()).freeze();
        }
    }

    private static RegistryOps.RegistryInfoLookup createContext(List<HolderLookup.RegistryLookup<?>> registryLookups, List<RegistryDataLoader.Loader<?>> loaders) {
        final Map<ResourceKey<? extends Registry<?>>, RegistryOps.RegistryInfo<?>> map = new HashMap<>();
        registryLookups.forEach(p_359481_ -> map.put(p_359481_.key(), createInfoForContextRegistry((HolderLookup.RegistryLookup<?>)p_359481_)));
        loaders.forEach(p_367838_ -> map.put(p_367838_.registry.key(), createInfoForNewRegistry(p_367838_.registry)));
        return new RegistryOps.RegistryInfoLookup() {
            @Override
            public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_256014_) {
                return Optional.ofNullable((RegistryOps.RegistryInfo<T>)map.get(p_256014_));
            }
        };
    }

    private static <T> RegistryOps.RegistryInfo<T> createInfoForNewRegistry(WritableRegistry<T> registry) {
        return new RegistryOps.RegistryInfo<>(registry, registry.createRegistrationLookup(), registry.registryLifecycle());
    }

    private static <T> RegistryOps.RegistryInfo<T> createInfoForContextRegistry(HolderLookup.RegistryLookup<T> registryLookup) {
        return new RegistryOps.RegistryInfo<>(registryLookup, registryLookup, registryLookup.registryLifecycle());
    }

    private static ReportedException logErrors(Map<ResourceKey<?>, Exception> errors) {
        printFullDetailsToLog(errors);
        return createReportWithBriefInfo(errors);
    }

    private static void printFullDetailsToLog(Map<ResourceKey<?>, Exception> errors) {
        StringWriter stringwriter = new StringWriter();
        PrintWriter printwriter = new PrintWriter(stringwriter);
        Map<ResourceLocation, Map<ResourceLocation, Exception>> map = errors.entrySet()
            .stream()
            .collect(
                Collectors.groupingBy(p_249353_ -> p_249353_.getKey().registry(), Collectors.toMap(p_251444_ -> p_251444_.getKey().location(), Entry::getValue))
            );
        map.entrySet().stream().sorted(Entry.comparingByKey()).forEach(p_249838_ -> {
            printwriter.printf("> Errors in registry %s:%n", p_249838_.getKey());
            p_249838_.getValue().entrySet().stream().sorted(Entry.comparingByKey()).forEach(p_250688_ -> {
                printwriter.printf(">> Errors in element %s:%n", p_250688_.getKey());
                p_250688_.getValue().printStackTrace(printwriter);
            });
        });
        printwriter.flush();
        LOGGER.error("Registry loading errors:\n{}", stringwriter);
    }

    private static ReportedException createReportWithBriefInfo(Map<ResourceKey<?>, Exception> errors) {
        CrashReport crashreport = CrashReport.forThrowable(new IllegalStateException("Failed to load registries due to errors"), "Registry Loading");
        CrashReportCategory crashreportcategory = crashreport.addCategory("Loading info");
        crashreportcategory.setDetail(
            "Errors",
            () -> {
                StringBuilder stringbuilder = new StringBuilder();
                errors.entrySet()
                    .stream()
                    .sorted(Entry.comparingByKey(ERROR_KEY_COMPARATOR))
                    .forEach(
                        p_366373_ -> stringbuilder.append("\n\t\t")
                            .append(p_366373_.getKey().registry())
                            .append("/")
                            .append(p_366373_.getKey().location())
                            .append(": ")
                            .append(p_366373_.getValue().getMessage())
                    );
                return stringbuilder.toString();
            }
        );
        return new ReportedException(crashreport);
    }

    private static <E> void loadElementFromResource(
        WritableRegistry<E> registry,
        Decoder<E> codec,
        RegistryOps<JsonElement> ops,
        ResourceKey<E> resourceKey,
        Resource resource,
        RegistrationInfo registrationInfo
    ) throws IOException {
        Decoder<Optional<E>> decoder = net.neoforged.neoforge.common.conditions.ConditionalOps.createConditionalCodec(net.neoforged.neoforge.common.util.NeoForgeExtraCodecs.decodeOnly(codec));
        try (Reader reader = resource.openAsReader()) {
            JsonElement jsonelement = StrictJsonParser.parse(reader);
            DataResult<Optional<E>> dataresult = decoder.parse(ops, jsonelement);
            Optional<E> candidate = dataresult.getOrThrow();
            candidate.ifPresentOrElse(e -> {
            registry.register(resourceKey, e, registrationInfo);
            }, () -> {
                LOGGER.debug("Skipping loading registry entry {} as its conditions were not met", resourceKey);
            });
        }
    }

    static <E> void loadContentsFromManager(
        ResourceManager resourceManager,
        RegistryOps.RegistryInfoLookup registryInfoLookup,
        WritableRegistry<E> registry,
        Decoder<E> codec,
        Map<ResourceKey<?>, Exception> loadingErrors
    ) {
        FileToIdConverter filetoidconverter = FileToIdConverter.registry(registry.key());
        RegistryOps<JsonElement> registryops = new net.neoforged.neoforge.common.conditions.ConditionalOps<>(RegistryOps.create(JsonOps.INSTANCE, registryInfoLookup), net.neoforged.neoforge.common.conditions.ICondition.IContext.TAGS_INVALID);

        for (Entry<ResourceLocation, Resource> entry : filetoidconverter.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            ResourceKey<E> resourcekey = ResourceKey.create(registry.key(), filetoidconverter.fileToId(resourcelocation));
            Resource resource = entry.getValue();
            RegistrationInfo registrationinfo = REGISTRATION_INFO_CACHE.apply(resource.knownPackInfo());

            try {
                loadElementFromResource(registry, codec, registryops, resourcekey, resource, registrationinfo);
            } catch (Exception exception) {
                loadingErrors.put(
                    resourcekey,
                    new IllegalStateException(
                        String.format(Locale.ROOT, "Failed to parse %s from pack %s", resourcelocation, resource.sourcePackId()), exception
                    )
                );
            }
        }

        TagLoader.loadTagsForRegistry(resourceManager, registry);
    }

    static <E> void loadContentsFromNetwork(
        Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> elements,
        ResourceProvider resourceProvider,
        RegistryOps.RegistryInfoLookup registryInfoLookup,
        WritableRegistry<E> registry,
        Decoder<E> codec,
        Map<ResourceKey<?>, Exception> loadingErrors
    ) {
        RegistryDataLoader.NetworkedRegistryData registrydataloader$networkedregistrydata = elements.get(registry.key());
        if (registrydataloader$networkedregistrydata != null) {
            RegistryOps<Tag> registryops = RegistryOps.create(NbtOps.INSTANCE, registryInfoLookup);
            RegistryOps<JsonElement> registryops1 = RegistryOps.create(JsonOps.INSTANCE, registryInfoLookup);
            FileToIdConverter filetoidconverter = FileToIdConverter.registry(registry.key());

            for (RegistrySynchronization.PackedRegistryEntry registrysynchronization$packedregistryentry : registrydataloader$networkedregistrydata.elements) {
                ResourceKey<E> resourcekey = ResourceKey.create(registry.key(), registrysynchronization$packedregistryentry.id());
                Optional<Tag> optional = registrysynchronization$packedregistryentry.data();
                if (optional.isPresent()) {
                    try {
                        DataResult<E> dataresult = codec.parse(registryops, optional.get());
                        E e = dataresult.getOrThrow();
                        registry.register(resourcekey, e, NETWORK_REGISTRATION_INFO);
                    } catch (Exception exception) {
                        loadingErrors.put(
                            resourcekey,
                            new IllegalStateException(String.format(Locale.ROOT, "Failed to parse value %s from server", optional.get()), exception)
                        );
                    }
                } else {
                    ResourceLocation resourcelocation = filetoidconverter.idToFile(registrysynchronization$packedregistryentry.id());

                    try {
                        Resource resource = resourceProvider.getResourceOrThrow(resourcelocation);
                        loadElementFromResource(registry, codec, registryops1, resourcekey, resource, NETWORK_REGISTRATION_INFO);
                    } catch (Exception exception1) {
                        loadingErrors.put(resourcekey, new IllegalStateException("Failed to parse local data", exception1));
                    }
                }
            }

            TagLoader.loadTagsFromNetwork(registrydataloader$networkedregistrydata.tags, registry);
        }
    }

    record Loader<T>(RegistryDataLoader.RegistryData<T> data, WritableRegistry<T> registry, Map<ResourceKey<?>, Exception> loadingErrors) {
        public void loadFromResources(ResourceManager resourceManager, RegistryOps.RegistryInfoLookup registryInfoLookup) {
            RegistryDataLoader.loadContentsFromManager(resourceManager, registryInfoLookup, this.registry, this.data.elementCodec, this.loadingErrors);
        }

        public void loadFromNetwork(
            Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> elements,
            ResourceProvider resourceProvider,
            RegistryOps.RegistryInfoLookup registryInfoLookup
        ) {
            RegistryDataLoader.loadContentsFromNetwork(elements, resourceProvider, registryInfoLookup, this.registry, this.data.elementCodec, this.loadingErrors);
        }
    }

    @FunctionalInterface
    interface LoadingFunction {
        void apply(RegistryDataLoader.Loader<?> loader, RegistryOps.RegistryInfoLookup registryInfoLookup);
    }

    public record NetworkedRegistryData(List<RegistrySynchronization.PackedRegistryEntry> elements, TagNetworkSerialization.NetworkPayload tags) {
    }

    public record RegistryData<T>(ResourceKey<? extends Registry<T>> key, Codec<T> elementCodec, boolean requiredNonEmpty, java.util.function.Consumer<net.neoforged.neoforge.registries.RegistryBuilder<T>> registryBuilderConsumer) {
        public RegistryData(ResourceKey<? extends Registry<T>> key, Codec<T> elementCodec, boolean requiredNonEmpty) {
            this(key, elementCodec, requiredNonEmpty, registryBuilder -> {});
        }

        RegistryData(ResourceKey<? extends Registry<T>> p_251360_, Codec<T> p_248976_) {
            this(p_251360_, p_248976_, false);
        }

        RegistryDataLoader.Loader<T> create(Lifecycle registryLifecycle, Map<ResourceKey<?>, Exception> loadingErrors) {
            net.neoforged.neoforge.registries.RegistryBuilder<T> registryBuilder = new net.neoforged.neoforge.registries.RegistryBuilder<>(key);
            registryBuilderConsumer.accept(registryBuilder);

            WritableRegistry<T> writableregistry = (WritableRegistry<T>) registryBuilder.disableRegistrationCheck().create();
            return new RegistryDataLoader.Loader<>(this, writableregistry, loadingErrors);
        }

        public void runWithArguments(BiConsumer<ResourceKey<? extends Registry<T>>, Codec<T>> runner) {
            runner.accept(this.key, this.elementCodec);
        }
    }
}
