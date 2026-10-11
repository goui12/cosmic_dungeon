package net.goui.cosmicdungeon.trade;

import java.util.*;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.economy.CurrencyDenomination;
import net.goui.cosmicdungeon.economy.pricing.ItemTransferRules;
import net.goui.cosmicdungeon.item.identity.ClassItemOwnership;
import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TradeOwnershipTest {
    @Test void ordinaryEquipmentAndLegacyMarkersDoNotRequireVendorPricing() {
        for(var item : List.of(Items.DIAMOND_SWORD, Items.DIAMOND_CHESTPLATE, Items.ARROW, Items.STICK)) {
            var stack=new ItemStack(item);
            CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{
                t.putBoolean("no_trade",true);t.putBoolean("class_issued",true);t.putBoolean("no_drop",true);
                t.putString(ClassItemOwnership.KEY,UUID.randomUUID().toString());
            });
            assertTrue(ItemTransferRules.tradeEligible(stack),item.toString());
            ClassItemUtil.attune(stack,"theurgist",1,1,0);
            assertFalse(ItemTransferRules.tradeEligible(stack));
            ClassItemUtil.clearAttunement(stack);
            assertTrue(ItemTransferRules.tradeEligible(stack));
        }
        assertFalse(ItemTransferRules.tradeEligible(ItemStack.EMPTY));
    }
    @Test void allPotionFormsTradeEvenWithExplicitAttunement() {
        for(var item : List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION)) {
            var stack=new ItemStack(item);
            ClassItemUtil.attune(stack,"theurgist",1,1,100);
            CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putBoolean("no_trade",true));
            assertTrue(ItemTransferRules.tradeEligible(stack));
        }
    }
    @Test void portableCannotSmuggleAttunedGearAndTransfersNestedOwners() {
        var owner=UUID.randomUUID();var recipient=UUID.randomUUID();
        var item=new ItemStack(Items.ARROW,7);
        CustomData.update(DataComponents.CUSTOM_DATA,item,t->{
            t.putString(ClassItemOwnership.KEY,owner.toString());t.putByteArray("future",new byte[]{1,-1,127});
        });
        var bundle=new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS,new BundleContents(List.of(item)));
        assertTrue(ItemTransferRules.tradeEligible(bundle));
        var result=TradeItemTransfer.forRecipient(bundle,owner,recipient);
        var nested=result.get(DataComponents.BUNDLE_CONTENTS).getItemUnsafe(0);
        assertEquals(recipient,ClassItemOwnership.owner(nested));
        assertEquals(owner,ClassItemOwnership.owner(item));
        assertEquals(7,nested.getCount());
        assertArrayEquals(new byte[]{1,-1,127},nested.get(DataComponents.CUSTOM_DATA).copyTag().getByteArray("future").orElseThrow());
        ClassItemUtil.attune(item,"theurgist",1,1,0);
        bundle.set(DataComponents.BUNDLE_CONTENTS,new BundleContents(List.of(item)));
        assertFalse(ItemTransferRules.tradeEligible(bundle));
        var shulker=new ItemStack(Items.SHULKER_BOX);
        shulker.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(ItemStack.EMPTY,item)));
        assertFalse(ItemTransferRules.tradeEligible(shulker));
    }
    @Test void transferChangesOnlyKnownCustodyAndKeepsOriginalImmutable() {
        var owner=UUID.randomUUID();var recipient=UUID.randomUUID();
        var stack=new ItemStack(Items.DIAMOND_SWORD);
        stack.setDamageValue(12);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Authored name"));
        stack.set(ModDataComponents.CHOP_OWNER.get(),owner);
        CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{
            t.putString(ClassItemOwnership.KEY,owner.toString());t.putString("future_key","preserve");
        });
        var before=stack.copy();var transferred=TradeItemTransfer.forRecipient(stack,owner,recipient);
        assertTrue(ItemStack.matches(before,stack));
        assertEquals(recipient,ClassItemOwnership.owner(transferred));
        assertEquals(recipient,transferred.get(ModDataComponents.CHOP_OWNER.get()));
        assertEquals(12,transferred.getDamageValue());
        assertEquals("Authored name",transferred.getHoverName().getString());
        assertEquals("preserve",transferred.get(DataComponents.CUSTOM_DATA).copyTag().getStringOr("future_key",""));
        assertTrue(ItemStack.matches(transferred,TradeItemTransfer.forRecipient(transferred,recipient,recipient)));
    }
    @Test void remainingBalanceUsesAllDenominationsAndDoesNotUnderflow() {
        for(var denomination:CurrencyDenomination.values()) {
            long amount=denomination.traceValue()*10;
            assertEquals(200_000-amount,TradeOfferBalance.remaining(200_000,amount),denomination.id());
        }
        assertEquals(95,TradeOfferBalance.remaining(100,5));
        assertEquals(0,TradeOfferBalance.remaining(100,Long.MAX_VALUE));
        assertEquals(0,TradeOfferBalance.remaining(-1,0));
        assertEquals(Long.MAX_VALUE-5,TradeOfferBalance.remaining(Long.MAX_VALUE,5));
    }
    @Test void nestedForeignMalformedAndCurrencyCannotBeOfferedOrLaundered() {
        var donor=UUID.randomUUID();var foreign=UUID.randomUUID();var recipient=UUID.randomUUID();
        for(var marker:List.of(foreign.toString(),"unknown-future-owner")) {
            var child=new ItemStack(Items.ARROW,5);
            CustomData.update(DataComponents.CUSTOM_DATA,child,t->t.putString(ClassItemOwnership.KEY,marker));
            var container=new ItemStack(Items.SHULKER_BOX);
            container.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(child)));
            var before=container.copy();
            assertFalse(TradeItemTransfer.mayOffer(container,donor));
            assertThrows(IllegalArgumentException.class,()->TradeItemTransfer.forRecipient(container,donor,recipient));
            assertTrue(ItemStack.matches(before,container));
        }
        var token=new ItemStack(Items.PAPER);
        CustomData.update(DataComponents.CUSTOM_DATA,token,t->t.putString(
                net.goui.cosmicdungeon.economy.DeathCurrencyRecord.MARKER,"receipt"));
        assertFalse(ItemTransferRules.tradeEligible(token));
        var bundle=new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS,new BundleContents(List.of(token)));
        assertFalse(TradeItemTransfer.mayOffer(bundle,donor));
    }
}
