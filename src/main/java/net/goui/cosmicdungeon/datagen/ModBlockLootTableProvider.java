package net.goui.cosmicdungeon.datagen;

import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.data.loot.BlockLootSubProvider;

import java.util.HashSet;
import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {

    // Track what we explicitly defined so we can safely default the rest to dropSelf(...)
    private final Set<Block> explicitlyHandled = new HashSet<>();

    public ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        // ===== Explicit loot rules =====

        dropSelfHandled(ModBlocks.PILE_OF_BOOKS.get());


        dropSelfHandled(ModBlocks.INFINITE_DISPENSER.get());


        // --- Cosmic mob spawner: drop 3–5 XP bottles
        addHandled(ModBlocks.COSMIC_MOB_SPAWNER.get(), this::cosmicSpawnerLoot);

        // --- Blooms: drop themselves
        dropSelfHandled(ModBlocks.BLOOM_OF_QUIET_ASSURANCE.get());
        dropSelfHandled(ModBlocks.BLOOM_OF_GENTLE_LIES.get());
        dropSelfHandled(ModBlocks.BLOOM_OF_WANING_MERCY.get());
        dropSelfHandled(ModBlocks.BLOOM_OF_CONSTRICTING_BONDS.get());
        dropSelfHandled(ModBlocks.BLOOM_OF_UNSPOKEN_RESIGNATION.get());
        dropSelfHandled(ModBlocks.BLOOM_OF_ELEGY.get());

        // --- Potted blooms: drop flower pot + unpotted bloom item
        addHandled(ModBlocks.POTTED_BLOOM_OF_QUIET_ASSURANCE.get(),
                b -> pottedBloomLoot(ModBlocks.BLOOM_OF_QUIET_ASSURANCE.get()));
        addHandled(ModBlocks.POTTED_BLOOM_OF_GENTLE_LIES.get(),
                b -> pottedBloomLoot(ModBlocks.BLOOM_OF_GENTLE_LIES.get()));
        addHandled(ModBlocks.POTTED_BLOOM_OF_WANING_MERCY.get(),
                b -> pottedBloomLoot(ModBlocks.BLOOM_OF_WANING_MERCY.get()));
        addHandled(ModBlocks.POTTED_BLOOM_OF_CONSTRICTING_BONDS.get(),
                b -> pottedBloomLoot(ModBlocks.BLOOM_OF_CONSTRICTING_BONDS.get()));
        addHandled(ModBlocks.POTTED_BLOOM_OF_UNSPOKEN_RESIGNATION.get(),
                b -> pottedBloomLoot(ModBlocks.BLOOM_OF_UNSPOKEN_RESIGNATION.get()));
        addHandled(ModBlocks.POTTED_BLOOM_OF_ELEGY.get(),
                b -> pottedBloomLoot(ModBlocks.BLOOM_OF_ELEGY.get()));

        // --- Utility / helper blocks that should never drop:
        noDropHandled(ModBlocks.REGION_GHOST_GLASS.get());
        noDropHandled(ModBlocks.BARRIER_BLOCK.get());
        noDropHandled(ModBlocks.COSMIC_RIFT.get());
        noDropHandled(ModBlocks.COSMIC_RIFT_TILE.get());
        noDropHandled(ModBlocks.REDSTONE_TRANSMITTER.get());
        noDropHandled(ModBlocks.REDSTONE_RECEIVER.get());

        // ===== Default: everything else drops itself if not defined above =====
        for (Block b : getKnownBlocks()) {
            if (!explicitlyHandled.contains(b)) {
                // Only safe if the block actually has an item.
                // If a block has no item, this still generates a loot table, but it will drop "air" (bad).
                // So we guard for that.
                if (b.asItem() == Items.AIR) {
                    this.add(b, noDrop());
                } else {
                    this.dropSelf(b);
                }
            }
        }
    }

    // --- Helpers to record explicit handling
    private void dropSelfHandled(Block b) {
        explicitlyHandled.add(b);
        this.dropSelf(b);
    }

    private void noDropHandled(Block b) {
        explicitlyHandled.add(b);
        this.add(b, noDrop());
    }

    private void addHandled(Block b, java.util.function.Function<Block, LootTable.Builder> table) {
        explicitlyHandled.add(b);
        this.add(b, table);
    }

    // --- Your spawner loot: 3–5 XP bottles
    private LootTable.Builder cosmicSpawnerLoot(Block block) {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(this.applyExplosionDecay(block,
                                LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 5.0F)))
                        )));
    }

    // --- Potted bloom loot: pot + the unpotted bloom item
    private LootTable.Builder pottedBloomLoot(Block unpottedBloomBlock) {
        Item bloomItem = unpottedBloomBlock.asItem(); // should be your BlockItem
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(Items.FLOWER_POT)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(bloomItem)));
    }

    // --- Your existing helper for multiple ore drops
    protected LootTable.Builder createMultipleOreDrops(Block block, Item item, float minDrops, float maxDrops) {
        HolderLookup.RegistryLookup<Enchantment> enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);

        return this.createSilkTouchDispatchTable(block,
                this.applyExplosionDecay(block, LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(minDrops, maxDrops)))
                        .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream()
                .map(Holder::value)
                .toList();
    }
}
