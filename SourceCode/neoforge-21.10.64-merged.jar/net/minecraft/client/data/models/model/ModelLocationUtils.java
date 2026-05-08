package net.minecraft.client.data.models.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelLocationUtils {
    @Deprecated
    public static ResourceLocation decorateBlockModelLocation(String name) {
        // Neo: Use ResourceLocation.parse to support modded paths
        return ResourceLocation.parse(name).withPrefix("block/");
    }

    public static ResourceLocation decorateItemModelLocation(String name) {
        // Neo: Use ResourceLocation.parse to support modded paths
        return ResourceLocation.parse(name).withPrefix("item/");
    }

    public static ResourceLocation getModelLocation(Block block, String suffix) {
        ResourceLocation resourcelocation = BuiltInRegistries.BLOCK.getKey(block);
        return resourcelocation.withPath(p_388420_ -> "block/" + p_388420_ + suffix);
    }

    public static ResourceLocation getModelLocation(Block block) {
        ResourceLocation resourcelocation = BuiltInRegistries.BLOCK.getKey(block);
        return resourcelocation.withPrefix("block/");
    }

    public static ResourceLocation getModelLocation(Item item) {
        ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(item);
        return resourcelocation.withPrefix("item/");
    }

    public static ResourceLocation getModelLocation(Item item, String suffix) {
        ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(item);
        return resourcelocation.withPath(p_386751_ -> "item/" + p_386751_ + suffix);
    }
}
