package net.goui.cosmicdungeon.datagen;

import java.util.concurrent.CompletableFuture;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;

/** Extends native arrow/impact classification without replacing vanilla tag entries. */
public final class D1EntityTagProvider extends EntityTypeTagsProvider {
    public D1EntityTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, CosmicDungeonMod.MOD_ID);
    }
    @Override protected void addTags(HolderLookup.Provider provider) {
        tag(EntityTypeTags.ARROWS).add(ModEntities.D1_ARROW.get());
    }
}
