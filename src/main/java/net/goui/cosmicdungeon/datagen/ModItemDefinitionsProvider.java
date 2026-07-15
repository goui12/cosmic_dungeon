// file: src/main/java/net/goui/cosmicdungeon/datagen/ModItemDefinitionsProvider.java
package net.goui.cosmicdungeon.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.item.ModItems;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 1.21+ Item Definitions provider.
 *
 * Generates:
 *   assets/<modid>/items/<item>.json
 *
 * For your class-locked chest *items*, we want the item-definition to point at our normal
 * item model (models/item/<id>.json) WITHOUT triggering any chest-special pipeline.
 *
 * Working format (what you manually fixed):
 * {
 *   "model": {
 *     "type": "minecraft:model",
 *     "model": "<modid>:item/<id>"
 *   }
 * }
 */
public final class ModItemDefinitionsProvider implements DataProvider {

    @SuppressWarnings("unused")
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final PackOutput packOutput;

    public ModItemDefinitionsProvider(PackOutput packOutput) {
        this.packOutput = packOutput;
    }

    @Override
    public String getName() {
        return "Cosmic Dungeon Item Definitions (<modid>/items/*.json)";
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        // RESOURCE_PACK target root -> .../generated/resources
        // We must write under: <modid>/items/
        Path outRoot = this.packOutput.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(CosmicDungeonMod.MOD_ID)
                .resolve("items");

        // These must be item registry ids, not block ids. Block-backed entries use their BlockItem ids.
        List<ResourceLocation> simpleItemDefinitionIds = List.of(
                ModItems.LEATHER_PATCH.getId(),
                ModItems.CHAIN_LINK.getId(),
                ModItems.NETHERITE_REPAIR_FRAGMENT.getId(),
                ModBlocks.BOGATYR_CHEST_ITEM.getId(),
                ModBlocks.DEADEYE_CHEST_ITEM.getId(),
                ModBlocks.DRAGOON_CHEST_ITEM.getId(),
                ModBlocks.JUDICATOR_CHEST_ITEM.getId(),
                ModBlocks.METALMANCER_CHEST_ITEM.getId(),
                ModBlocks.PYROCLAST_CHEST_ITEM.getId(),
                ModBlocks.THEURGIST_CHEST_ITEM.getId(),
                ModBlocks.VENEFEX_CHEST_ITEM.getId()
        );

        CompletableFuture<?>[] futures = new CompletableFuture<?>[simpleItemDefinitionIds.size()];

        for (int i = 0; i < simpleItemDefinitionIds.size(); i++) {
            ResourceLocation id = simpleItemDefinitionIds.get(i);

            ResourceLocation itemModel = ResourceLocation.fromNamespaceAndPath(
                    CosmicDungeonMod.MOD_ID,
                    "item/" + id.getPath()
            );

            // Exact working structure:
            // {
            //   "model": { "type": "minecraft:model", "model": "<modid>:item/<id>" }
            // }
            JsonObject root = new JsonObject();

            JsonObject modelObj = new JsonObject();
            modelObj.addProperty("type", "minecraft:model");
            modelObj.addProperty("model", itemModel.toString());

            root.add("model", modelObj);

            Path file = outRoot.resolve(id.getPath() + ".json");
            futures[i] = DataProvider.saveStable(cache, root, file);
        }

        return CompletableFuture.allOf(futures);
    }
}
