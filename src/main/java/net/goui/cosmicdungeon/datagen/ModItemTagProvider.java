// file: src/main/java/net/goui/cosmicdungeon/datagen/ModItemTagProvider.java
package net.goui.cosmicdungeon.datagen;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagCopyingItemTagProvider;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends BlockTagCopyingItemTagProvider {

    public ModItemTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTags
    ) {
        super(output, lookupProvider, blockTags, CosmicDungeonMod.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(ModTags.Items.TRANSFORMABLE_ITEMS)
                .add(Items.COAL)
                .add(Items.STICK)
                .add(Items.COMPASS);

        this.tag(ModTags.Items.SHIELDS)
                // .add(ModItems.VOWKEEPER.get())
                .add(ModItems.AEGIS_OF_TRUTH.get())
                .add(ModItems.WORKED_PLANK.get())
                .add(ModItems.REINFORCED_IRON_SLAB.get())
                .add(ModItems.SHIELD_OF_TIDAL_FORCE.get())
                .add(ModItems.SHIELD_OF_THE_DEEP.get());

        this.tag(ModTags.Items.INFINITE_SHOOTABLES)
                .add(Items.ARROW)
                .add(Items.SPECTRAL_ARROW)
                .add(Items.SNOWBALL)
                .add(Items.FIRE_CHARGE)
                .add(Items.WIND_CHARGE);

        // ===== Class restriction tags =====
        // Intentionally emitted even if empty right now, so the structure exists and is repeatable.
        this.tag(ModTags.Items.CLASS_RESTRICTED_METALMANCER);
        this.tag(ModTags.Items.CLASS_RESTRICTED_JUDICATOR);
        this.tag(ModTags.Items.CLASS_RESTRICTED_DRAGOON);
        this.tag(ModTags.Items.CLASS_RESTRICTED_DEADEYE);
        this.tag(ModTags.Items.CLASS_RESTRICTED_PYROCLAST);
        this.tag(ModTags.Items.CLASS_RESTRICTED_THEURGIST);
        this.tag(ModTags.Items.CLASS_RESTRICTED_VENEFEX);
        this.tag(ModTags.Items.CLASS_RESTRICTED_BOGATYR);

        // If you ever want to mirror block tags into item tags, you can use copy(...) here.
        // Example (ONLY if you actually need it):
        // this.copy(ModTags.Blocks.AMETHYST_BLOCKS, ModTags.Items.AMETHYST_BLOCKS_AS_ITEMS);
    }

    @Override
    public String getName() {
        return "CosmicDungeon Item Tags";
    }
}
