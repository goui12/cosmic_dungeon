package net.goui.cosmicdungeon.datagen;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, CosmicDungeonMod.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.Items.TRANSFORMABLE_ITEMS)
                .add(ModItems.BISMUTH.get())
                .add(ModItems.RAW_BISMUTH.get())
                .add(net.minecraft.world.item.Items.COAL)
                .add(net.minecraft.world.item.Items.STICK)
                .add(net.minecraft.world.item.Items.COMPASS);
        tag(ModTags.Items.SHIELDS)
                //.add(ModItems.VOWKEEPER.get())
                .add(ModItems.AEGIS_OF_TRUTH.get())
                .add(ModItems.WORKED_PLANK.get())
                .add(ModItems.REINFORCED_IRON_SLAB.get())
                .add(ModItems.SHIELD_OF_TIDAL_FORCE.get())
                .add(ModItems.SHIELD_OF_THE_DEEP.get());
    }
}
