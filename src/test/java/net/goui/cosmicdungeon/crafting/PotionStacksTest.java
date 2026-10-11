package net.goui.cosmicdungeon.crafting;

import java.util.List;
import java.util.UUID;
import net.goui.cosmicdungeon.trade.TradeItemTransfer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** New outputs are stackable; authored inventory inputs remain immutable. */
class PotionStacksTest {
    private static ItemStack healing() {
        return PotionStacks.crafted(PotionContents.createItemStack(Items.SPLASH_POTION,Potions.HEALING));
    }
    private static NonNullList<ItemStack> inventory() {return NonNullList.withSize(41,ItemStack.EMPTY);}
    private static void same(List<ItemStack> expected,List<ItemStack> actual) {
        assertEquals(expected.size(),actual.size());
        for(int i=0;i<expected.size();i++)assertTrue(ItemStack.matches(expected.get(i),actual.get(i)),"slot "+i);
    }
    @Test void everyVanillaPotionFormReceivesBoundedNativeStackCapacityOnACopy() {
        assertEquals(64,PotionStacks.LIMIT);
        for(var item:List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION)) {
            var original=PotionContents.createItemStack(item,Potions.TURTLE_MASTER);var before=original.copy();
            var result=PotionStacks.crafted(original);
            assertEquals(1,result.getCount());assertEquals(64,result.getMaxStackSize());
            assertTrue(ItemStack.matches(before,original));assertEquals(1,original.getMaxStackSize());
            assertEquals(original.get(DataComponents.POTION_CONTENTS),result.get(DataComponents.POTION_CONTENTS));
            assertNotSame(original,result);
        }
    }
    @Test void factoryRejectsOtherItemsAndMoreThanOneProducedUnit() {
        for(var bad:List.of(ItemStack.EMPTY,new ItemStack(Items.DIAMOND),healing().copyWithCount(2)))
            assertThrows(IllegalArgumentException.class,()->PotionStacks.crafted(bad));
    }
    @Test void factoryKeepsAuthoredComponentsWithoutMutatingSource() {
        var authored=PotionContents.createItemStack(Items.POTION,Potions.TURTLE_MASTER);
        authored.set(DataComponents.CUSTOM_NAME,Component.literal("A keeper's bottle"));
        CustomData.update(DataComponents.CUSTOM_DATA,authored,t->t.putString("future_field","preserve"));
        var before=authored.copy();var output=PotionStacks.crafted(authored);
        assertTrue(ItemStack.matches(before,authored));
        assertEquals(authored.get(DataComponents.CUSTOM_NAME),output.get(DataComponents.CUSTOM_NAME));
        assertEquals(authored.get(DataComponents.CUSTOM_DATA),output.get(DataComponents.CUSTOM_DATA));
        assertEquals(authored.get(DataComponents.CONSUMABLE),output.get(DataComponents.CONSUMABLE));
        assertEquals(authored.get(DataComponents.USE_REMAINDER),output.get(DataComponents.USE_REMAINDER));
    }
    @Test void cursorAcceptsExactlyOneAndRepeatedMatchingCrafts() {
        var single=healing();var carried=PotionOutputPlacement.cursor(ItemStack.EMPTY,single);
        assertEquals(1,carried.getCount());assertNotSame(single,carried);
        for(int i=1;i<64;i++)carried=PotionOutputPlacement.cursor(carried,single);
        assertEquals(64,carried.getCount());assertEquals(1,single.getCount());
        var before=carried.copy();assertNull(PotionOutputPlacement.cursor(carried,single));
        assertTrue(ItemStack.matches(before,carried));
    }
    @Test void cursorNeverCombinesDifferentEffectsVariantsNamesOrUnknownComponents() {
        var carried=healing().copyWithCount(2);
        var strong=PotionStacks.crafted(PotionContents.createItemStack(Items.SPLASH_POTION,Potions.STRONG_HEALING));
        var named=healing();named.set(DataComponents.CUSTOM_NAME,Component.literal("Different"));
        var marked=healing();CustomData.update(DataComponents.CUSTOM_DATA,marked,t->t.putInt("future",1));
        var ordinary=PotionContents.createItemStack(Items.SPLASH_POTION,Potions.HEALING);
        for(var other:List.of(strong,named,marked,ordinary,new ItemStack(Items.DIAMOND))) {
            var before=carried.copy();assertNull(PotionOutputPlacement.cursor(carried,other));
            assertTrue(ItemStack.matches(before,carried));assertEquals(1,other.getCount());
        }
    }
    @Test void cursorRejectsNonSingleOutputWithoutChangingEitherInput() {
        var carried=healing().copyWithCount(5);var before=carried.copy();
        assertNull(PotionOutputPlacement.cursor(carried,ItemStack.EMPTY));
        assertNull(PotionOutputPlacement.cursor(carried,healing().copyWithCount(2)));
        assertTrue(ItemStack.matches(before,carried));
    }
    @Test void shiftedCraftMergesIntoHotbarBeforeUsingEmptyHotbar() {
        var before=inventory();before.set(5,healing().copyWithCount(7));
        var after=PotionOutputPlacement.inventory(before,healing());
        assertNotNull(after);assertEquals(8,after.get(5).getCount());assertTrue(after.get(0).isEmpty());
        assertEquals(7,before.get(5).getCount());after.get(5).shrink(2);assertEquals(7,before.get(5).getCount());
    }
    @Test void emptyHotbarTakesPriorityOverMatchingMainInventory() {
        var before=inventory();before.set(9,healing().copyWithCount(7));
        var after=PotionOutputPlacement.inventory(before,healing());
        assertNotNull(after);assertEquals(1,after.get(0).getCount());assertEquals(7,after.get(9).getCount());
        assertTrue(before.get(0).isEmpty());assertEquals(7,before.get(9).getCount());
    }
    @Test void fullHotbarFallsBackToMainMergeThenEmptySlot() {
        var before=inventory();for(int i=0;i<9;i++)before.set(i,new ItemStack(Items.DIAMOND,64));
        before.set(20,healing().copyWithCount(63));
        var after=PotionOutputPlacement.inventory(before,healing());
        assertNotNull(after);assertEquals(64,after.get(20).getCount());assertTrue(after.get(9).isEmpty());
        var next=PotionOutputPlacement.inventory(after,healing());
        assertNotNull(next);assertEquals(1,next.get(9).getCount());assertEquals(64,next.get(20).getCount());
        assertEquals(63,before.get(20).getCount());
    }
    @Test void inventoryKeepsIncompatiblePotionsSeparateAndDoesNotRewriteAuthoredSlots() {
        var before=inventory();before.set(0,PotionContents.createItemStack(Items.SPLASH_POTION,Potions.HEALING));
        var renamed=healing();renamed.set(DataComponents.CUSTOM_NAME,Component.literal("Authored"));
        before.set(1,renamed);
        var after=PotionOutputPlacement.inventory(before,healing());
        assertNotNull(after);assertTrue(ItemStack.matches(before.get(0),after.get(0)));
        assertTrue(ItemStack.matches(before.get(1),after.get(1)));assertEquals(1,after.get(2).getCount());
        assertEquals(1,before.get(0).getMaxStackSize());
    }
    @Test void fullInventoryNeverUsesArmorOrOffhandAndLeavesSourceUnchanged() {
        var before=inventory();for(int i=0;i<36;i++)before.set(i,new ItemStack(Items.DIAMOND,64));
        before.set(40,healing().copyWithCount(4));var copy=before.stream().map(ItemStack::copy).toList();
        assertNull(PotionOutputPlacement.inventory(before,healing()));same(copy,before);
        before.set(36,ItemStack.EMPTY);assertNull(PotionOutputPlacement.inventory(before,healing()));
    }
    @Test void shiftedCraftOnlyCreatesOneAndRejectsInvalidOutputAtomically() {
        var before=inventory();var copy=before.stream().map(ItemStack::copy).toList();
        assertNull(PotionOutputPlacement.inventory(before,ItemStack.EMPTY));
        assertNull(PotionOutputPlacement.inventory(before,healing().copyWithCount(2)));same(copy,before);
        var after=PotionOutputPlacement.inventory(before,healing());
        assertEquals(1,after.stream().mapToInt(ItemStack::getCount).sum());same(copy,before);
    }
    @Test void splitAndRecombinePreserveFullComponentsAndTotalCount() {
        var stack=healing().copyWithCount(64);var single=stack.split(1);
        assertEquals(63,stack.getCount());assertEquals(1,single.getCount());
        assertEquals(64,single.getMaxStackSize());assertTrue(ItemStack.isSameItemSameComponents(stack,single));
        assertEquals(64,PotionOutputPlacement.cursor(stack,single).getCount());
    }
    @Test void tradeProjectionKeepsCraftedCountAndAllComponents() {
        var original=healing().copyWithCount(32);var before=original.copy();
        var donor=UUID.randomUUID();var recipient=UUID.randomUUID();
        assertTrue(TradeItemTransfer.mayOffer(original,donor));
        var transferred=TradeItemTransfer.forRecipient(original,donor,recipient);
        assertTrue(ItemStack.matches(before,transferred));assertTrue(ItemStack.matches(before,original));
        assertEquals(64,transferred.getMaxStackSize());
    }
    @Test void legacyMultiItemPlacementRejectsPartialSpaceWithoutMutation() {
        var before=inventory();for(int i=0;i<36;i++)before.set(i,new ItemStack(Items.DIAMOND,64));
        before.set(0,healing().copyWithCount(63));var original=before.stream().map(ItemStack::copy).toList();
        assertNull(PotionOutputPlacement.inventoryStack(before,healing().copyWithCount(2)));same(original,before);
        before.set(9,healing().copyWithCount(63));
        var after=PotionOutputPlacement.inventoryStack(before,healing().copyWithCount(2));
        assertEquals(64,after.get(0).getCount());assertEquals(64,after.get(9).getCount());
        assertEquals(63,before.get(0).getCount());assertEquals(63,before.get(9).getCount());
        assertNull(PotionOutputPlacement.inventoryStack(before,healing().copyWithCount(65)));
    }

}
