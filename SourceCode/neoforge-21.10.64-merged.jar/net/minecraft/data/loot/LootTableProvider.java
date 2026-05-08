package net.minecraft.data.loot;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class LootTableProvider implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackOutput.PathProvider pathProvider;
    private final Set<ResourceKey<LootTable>> requiredTables;
    private final List<LootTableProvider.SubProviderEntry> subProviders;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public LootTableProvider(
        PackOutput output,
        Set<ResourceKey<LootTable>> requiredTables,
        List<LootTableProvider.SubProviderEntry> subProviders,
        CompletableFuture<HolderLookup.Provider> registries
    ) {
        this.pathProvider = output.createRegistryElementsPathProvider(Registries.LOOT_TABLE);
        this.subProviders = subProviders;
        this.requiredTables = requiredTables;
        this.registries = registries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return this.registries.thenCompose(p_323117_ -> this.run(output, p_323117_));
    }

    private CompletableFuture<?> run(CachedOutput output, HolderLookup.Provider provider) {
        WritableRegistry<LootTable> writableregistry = new MappedRegistry<>(Registries.LOOT_TABLE, Lifecycle.experimental());
        Map<RandomSupport.Seed128bit, ResourceLocation> map = new Object2ObjectOpenHashMap<>();
        Map<LootTable, List<net.neoforged.neoforge.common.conditions.ICondition>> conditionsPerTable = new java.util.IdentityHashMap<>();
        getTables().forEach(p_344197_ -> p_344197_.provider().apply(provider).generate((p_380827_, p_380828_) -> {
            ResourceLocation resourcelocation = sequenceIdForLootTable(p_380827_);
            ResourceLocation resourcelocation1 = map.put(RandomSequence.seedForKey(resourcelocation), resourcelocation);
            if (resourcelocation1 != null) {
                Util.logAndPauseIfInIde("Loot table random sequence seed collision on " + resourcelocation1 + " and " + p_380827_.location());
            }

            p_380828_.setRandomSequence(resourcelocation);
            LootTable loottable = p_380828_.setParamSet(p_344197_.paramSet).build();
            writableregistry.register(p_380827_, loottable, RegistrationInfo.BUILT_IN);
            var conditions = p_380828_.buildConditions();
            if (!conditions.isEmpty()) {
                conditionsPerTable.put(loottable, conditions);
            }
        }));
        writableregistry.freeze();
        ProblemReporter.Collector problemreporter$collector = new ProblemReporter.Collector();
        HolderGetter.Provider holdergetter$provider = new RegistryAccess.ImmutableRegistryAccess(List.of(writableregistry)).freeze();
        ValidationContext validationcontext = new ValidationContext(problemreporter$collector, LootContextParamSets.ALL_PARAMS, holdergetter$provider);

        validate(writableregistry, validationcontext, problemreporter$collector);

        if (!problemreporter$collector.isEmpty()) {
            problemreporter$collector.forEach((p_421299_, p_421300_) -> LOGGER.warn("Found validation problem in {}: {}", p_421299_, p_421300_.description()));
            throw new IllegalStateException("Failed to validate loot tables, see logs");
        } else {
            return CompletableFuture.allOf(writableregistry.entrySet().stream().map(p_335193_ -> {
                ResourceKey<LootTable> resourcekey1 = p_335193_.getKey();
                LootTable loottable = p_335193_.getValue();
                Path path = this.pathProvider.json(resourcekey1.location());
                var conditional = new net.neoforged.neoforge.common.conditions.WithConditions<>(conditionsPerTable.getOrDefault(loottable, List.of()), loottable);
                return DataProvider.saveStable(output, provider, LootTable.CONDITIONAL_DIRECT_CODEC, java.util.Optional.of(conditional), path);
            }).toArray(CompletableFuture[]::new));
        }
    }

    public List<LootTableProvider.SubProviderEntry> getTables() {
        return this.subProviders;
    }

    protected void validate(WritableRegistry<LootTable> writableregistry, ValidationContext validationcontext, ProblemReporter.Collector problemreporter$collector) {
        for (ResourceKey<LootTable> resourcekey : Sets.difference(this.requiredTables, writableregistry.registryKeySet())) {
            problemreporter$collector.report(new LootTableProvider.MissingTableProblem(resourcekey));
        }

        writableregistry.listElements()
                .forEach(
                        p_380823_ -> p_380823_.value()
                                .validate(
                                        validationcontext.setContextKeySet(p_380823_.value().getParamSet())
                                                .enterElement(new ProblemReporter.RootElementPathElement(p_380823_.key()), p_380823_.key())
                                )
                );
    }

    private static ResourceLocation sequenceIdForLootTable(ResourceKey<LootTable> lootTable) {
        return lootTable.location();
    }

    @Override
    public final String getName() {
        return "Loot Tables";
    }

    public record MissingTableProblem(ResourceKey<LootTable> id) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Missing built-in table: " + this.id.location();
        }
    }

    public record SubProviderEntry(Function<HolderLookup.Provider, LootTableSubProvider> provider, ContextKeySet paramSet) {
    }
}
