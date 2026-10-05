package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.block.entity.ClassLockedChestBlockEntity;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.item.identity.ClassItemOwnership;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryCopyLootTest {
    private static final ResourceLocation DIM=ResourceLocation.parse("cosmicdungeon:d1_instance_21");
    private static final RegistryAccess LOOKUP=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static PotionBrewing recipes(){var b=new PotionBrewing.Builder(FeatureFlags.DEFAULT_FLAGS);PotionBrewing.addVanillaMixes(b);return b.build();}
    private static MercenaryEntity merc(String cls){
        var m=new MercenaryEntity(ModEntities.MERCENARY.get(),null);
        m.initialize(21,new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),cls,2,500));return m;
    }
    private static BarrelBlockEntity barrel(){return new BarrelBlockEntity(new BlockPos(4,5,6),Blocks.BARREL.defaultBlockState());}
    private static boolean copy(MercenaryEntity m,BaseContainerBlockEntity chest,int slot){return MercenaryCollection.copySlot(m,chest,slot,DIM,recipes());}
    private static CompoundTag save(MercenaryLootMemory memory){
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,LOOKUP);memory.save(out);return out.buildResult();
    }
    private static MercenaryLootMemory load(CompoundTag tag){return MercenaryLootMemory.load(TagValueInput.create(ProblemReporter.DISCARDING,LOOKUP,tag));}
    @Test void barrelCopiesOnlyUsefulStacksAndLeavesEverySourceComponentForPlayers(){
        var m=merc("theurgist");var chest=barrel();var arrows=new ItemStack(Items.ARROW,24);
        arrows.set(DataComponents.CUSTOM_NAME,Component.literal("Authored arrows"));
        var key=new ItemStack(ModItems.DOOR_KEY.get());key.set(ModDataComponents.DOOR_LOCK_ID.get(),UUID.randomUUID());
        chest.setItem(0,arrows.copy());chest.setItem(1,key.copy());chest.setItem(2,new ItemStack(Items.TORCH,16));
        assertTrue(copy(m,chest,0));assertFalse(copy(m,chest,1));assertFalse(copy(m,chest,2));
        assertTrue(ItemStack.matches(arrows,chest.getItem(0)));assertTrue(ItemStack.matches(key,chest.getItem(1)));
        assertTrue(ItemStack.matches(arrows,m.supplies().getFirst()));assertNotSame(chest.getItem(0),m.supplies().getFirst());
        assertTrue(ItemStack.matches(arrows,chest.removeItem(0,24)));assertTrue(chest.getItem(0).isEmpty());
    }
    @Test void revisitsAndConsumptionCannotRefillTheSameSlotButEachMercenaryHasOwnCopy(){
        var chest=barrel();chest.setItem(0,new ItemStack(Items.ARROW,16));var a=merc("theurgist");var b=merc("bogatyr");
        assertTrue(copy(a,chest,0));a.supplies().set(0,ItemStack.EMPTY);
        assertFalse(copy(a,chest,0));assertTrue(copy(b,chest,0));assertEquals(16,chest.getItem(0).getCount());
    }
    @Test void fullBagDoesNotConsumeOrClaimTheSourceAndCanRetryLater(){
        var m=merc("theurgist");for(int i=0;i<m.supplies().size();i++)m.supplies().set(i,new ItemStack(Items.DIRT,64));
        var chest=barrel();chest.setItem(0,new ItemStack(Items.ARROW,16));
        assertFalse(copy(m,chest,0));assertTrue(m.lootMemory().available(DIM,chest.getBlockPos().asLong(),0));
        assertEquals(16,chest.getItem(0).getCount());m.supplies().set(0,ItemStack.EMPTY);assertTrue(copy(m,chest,0));
    }
    @Test void keysAndUnusableGroundItemsAreRejectedByTheSameAcquisitionPath(){
        var m=merc("theurgist");
        for(var stack:List.of(new ItemStack(ModItems.DOOR_KEY.get()),new ItemStack(Items.DIAMOND),
                new ItemStack(Items.APPLE),new ItemStack(Items.FIREWORK_ROCKET),new ItemStack(Items.TRIDENT)))
            assertFalse(MercenaryCollection.receive(m,stack,recipes()));
        var disguisedKey=new ItemStack(Items.ARROW,5);disguisedKey.set(ModDataComponents.DOOR_LOCK_ID.get(),UUID.randomUUID());
        assertFalse(MercenaryCollection.receive(m,disguisedKey,recipes()));
        assertTrue(m.supplies().stream().allMatch(ItemStack::isEmpty));
    }
    @Test void effectSplashesAndClassAmmunitionRemainAvailableWithoutReagents(){
        var hire=merc("theurgist").contract();
        for(var stack:List.of(PotionContents.createItemStack(Items.SPLASH_POTION,Potions.HEALING),new ItemStack(Items.ARROW)))
            assertTrue(MercenaryInventory.useful(stack,hire,recipes()));
        assertFalse(MercenaryInventory.useful(new ItemStack(Items.ARROW),merc("dragoon").contract(),recipes()));
    }
    @Test void foreignAndMalformedOwnershipAreNotCopied(){
        var m=merc("theurgist");var chest=barrel();var tag=new CompoundTag();
        for(String owner:List.of(UUID.randomUUID().toString(),"malformed")){
            tag.putString(ClassItemOwnership.KEY,owner);var stack=new ItemStack(Items.ARROW,12);
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));chest.setItem(0,stack.copy());
            assertFalse(copy(m,chest,0));assertTrue(ItemStack.matches(stack,chest.getItem(0)));
        }
    }
    @Test void equipmentSelectionUsesNativeUpgradeRulesWithoutChangingAuthoredStacks(){
        var m=merc("bogatyr");var old=new ItemStack(Items.LEATHER_HELMET);
        // Native transaction setter avoids world sound/events in this level-free fixture.
        m.setItemSlot(EquipmentSlot.HEAD,old.copy(),true);
        var upgrade=new ItemStack(Items.DIAMOND_HELMET);upgrade.set(DataComponents.CUSTOM_NAME,Component.literal("Authored upgrade"));
        assertTrue(m.equipmentUpgrade(upgrade,EquipmentSlot.HEAD));
        assertFalse(m.equipmentUpgrade(old,EquipmentSlot.HEAD));
        m.setItemSlot(EquipmentSlot.HEAD,upgrade.copy(),true);
        assertFalse(m.equipmentUpgrade(old,EquipmentSlot.HEAD));
        assertEquals("Authored upgrade",upgrade.getHoverName().getString());
        assertEquals(1,upgrade.getCount());
    }
    @Test void starterChestCopiesSuppliesButNeverClearsTheAuthoredChest(){
        var m=merc("theurgist");var chest=new ClassLockedChestBlockEntity(BlockPos.ZERO,ModBlocks.THEURGIST_CHEST.get().defaultBlockState());
        var brew=new ItemStack(Items.NETHER_WART,4);brew.set(DataComponents.CUSTOM_NAME,Component.literal("Starter reagent"));
        chest.setItem(0,brew.copy());chest.setItem(1,new ItemStack(Items.ARROW,32));chest.setItem(2,new ItemStack(ModItems.DOOR_KEY.get()));
        assertTrue(MercenaryEquipment.equip(m,List.of(chest),recipes(),DIM));
        assertTrue(ItemStack.matches(brew,chest.getItem(0)));assertEquals(32,chest.getItem(1).getCount());assertTrue(chest.getItem(2).is(ModItems.DOOR_KEY.get()));
        assertTrue(m.supplies().stream().noneMatch(stack->stack.is(Items.NETHER_WART)));
        assertEquals(32,m.supplies().getFirst().getCount());
        m.supplies().set(0,ItemStack.EMPTY);assertFalse(copy(m,chest,1));
        assertFalse(MercenaryEquipment.equip(m,List.of(chest),recipes(),DIM));
    }
    @Test void unopenedLootTablesRemainUnrolledAndCannotBeCopied(){
        var chest=barrel();var table=net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE,ResourceLocation.parse("minecraft:chests/simple_dungeon"));
        chest.setLootTable(table);
        assertFalse(copy(merc("theurgist"),chest,0));assertEquals(table,chest.getLootTable());
    }
    @Test void nativeContainerExtractionVetoStillBlocksCopying(){
        var chest=new BarrelBlockEntity(BlockPos.ZERO,Blocks.BARREL.defaultBlockState()){
            @Override public boolean canTakeItem(net.minecraft.world.Container destination,int slot,ItemStack stack){return false;}
        };
        chest.setItem(0,new ItemStack(Items.ARROW,12));
        assertFalse(copy(merc("theurgist"),chest,0));assertEquals(12,chest.getItem(0).getCount());
    }
    @Test void savedReceiptsSurviveReloadAndSeparatePositionsSlotsAndDimensions(){
        var memory=new MercenaryLootMemory();memory.remember(DIM,12,3);var loaded=load(save(memory));
        assertFalse(loaded.available(DIM,12,3));assertTrue(loaded.available(DIM,12,4));assertTrue(loaded.available(DIM,13,3));
        assertTrue(loaded.available(ResourceLocation.parse("minecraft:overworld"),12,3));
        assertEquals(save(memory),save(loaded));
    }
    @Test void oldSavesStartEmptyAndMalformedNewReceiptsFailClosed(){
        assertTrue(load(new CompoundTag()).available(DIM,1,0));
        var broken=new CompoundTag();broken.putInt("mercenary_loot_version",1);broken.putString("mercenary_loot_claims","bad");
        var blocked=load(broken);assertFalse(blocked.available(DIM,1,0));assertFalse(load(save(blocked)).available(DIM,1,0));
        var future=save(new MercenaryLootMemory());future.putInt("mercenary_loot_version",2);assertFalse(load(future).available(DIM,1,0));
    }
    @Test void receiptBudgetNeverEvictsOldClaimsToAllowMoreCopies(){
        var memory=new MercenaryLootMemory();for(int i=0;i<MercenaryLootMemory.MAX_CLAIMS;i++)memory.remember(DIM,i,0);
        assertFalse(memory.available(DIM,Long.MAX_VALUE,0));var loaded=load(save(memory));
        assertFalse(loaded.available(DIM,0,0));assertFalse(loaded.available(DIM,Long.MAX_VALUE,0));
    }
}
