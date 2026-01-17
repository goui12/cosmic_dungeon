// file: src/main/java/net/goui/cosmicdungeon/datagen/DataGenerators.java
package net.goui.cosmicdungeon.datagen;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DataGenerators {
    private DataGenerators() {}

    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();

        // Client datagen (models, lang, etc.)
        generator.addProvider(true, new ModModelProvider(packOutput));

        // NEW: 1.21+ item definition JSONs (assets/<modid>/items/*.json)
        generator.addProvider(true, new ModItemDefinitionsProvider(packOutput));
    }

    @SubscribeEvent
    public static void gatherServerData(GatherDataEvent.Server event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Loot tables
        generator.addProvider(true, new LootTableProvider(
                packOutput,
                Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new, LootContextParamSets.BLOCK)),
                lookupProvider
        ));

        // Recipes
        generator.addProvider(true, new ModRecipeProvider.Runner(packOutput, lookupProvider));

        // ✅ FIX: register block+item tags together so item tags receive blockTags.contentsGetter()
        event.createBlockAndItemTags(ModBlockTagProvider::new, ModItemTagProvider::new);

        // Data maps
        generator.addProvider(true, new ModDataMapProvider(packOutput, lookupProvider));
    }
}
