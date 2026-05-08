package net.minecraft.client.data.models;

import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ItemModelOutput {
    void accept(Item item, ItemModel.Unbaked model);

    void copy(Item item1, Item item2);

    /**
     * Neo: Pulled up from {@link ModelProvider.ItemInfoCollector} to give modders full control over the {@link net.minecraft.client.renderer.item.ClientItem} instead of just the {@link ItemModel.Unbaked} in {@link #accept(Item, ItemModel.Unbaked)
     */
    default void register(Item item, net.minecraft.client.renderer.item.ClientItem clientItem) {
    }
}
