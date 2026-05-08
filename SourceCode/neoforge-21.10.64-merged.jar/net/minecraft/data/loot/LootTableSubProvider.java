package net.minecraft.data.loot;

import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

@FunctionalInterface
public interface LootTableSubProvider extends net.neoforged.neoforge.common.extensions.LootTableSubProviderExtension {
    void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output);
}
