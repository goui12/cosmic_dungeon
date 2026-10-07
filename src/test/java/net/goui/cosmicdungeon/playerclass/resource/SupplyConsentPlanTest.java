package net.goui.cosmicdungeon.playerclass.resource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SupplyConsentPlanTest {
    @Test void orderedBulkOnlyUsesRemainingQuotedStacksAndCounts(){
        var inventory=new SimpleContainer(new ItemStack(Items.SUGAR,8),new ItemStack(Items.SUGAR,30));
        var first=ResourceRecycling.plan(inventory,595,s->s.is(Items.SUGAR));
        var second=ResourceRecycling.plan(inventory,594,s->s.is(Items.SUGAR));
        assertTrue(ResourceRecycling.apply(inventory,first));
        var remainder=SupplyRequests.remaining(inventory,second,600,s->s.is(Items.SUGAR));
        assertEquals(3,remainder.credit());assertEquals(1,remainder.changes().size());
        assertTrue(ResourceRecycling.apply(inventory,remainder));
        assertTrue(inventory.getItem(0).isEmpty());assertEquals(30,inventory.getItem(1).getCount());
    }
    @Test void shrinkingCapCannotWasteOrExpandConsent(){
        var inventory=new SimpleContainer(new ItemStack(Items.SUGAR,20));
        var quote=ResourceRecycling.plan(inventory,590,s->true);
        assertEquals(2,SupplyRequests.remaining(inventory,quote,2,s->true).credit());
        assertEquals(10,SupplyRequests.remaining(inventory,quote,600,s->true).credit());
        assertEquals(0,SupplyRequests.remaining(inventory,quote,0,s->true).credit());
    }
    @Test void changedComponentsAndNewlyIneligibleItemsAreNeverCharged(){
        var inventory=new SimpleContainer(new ItemStack(Items.SUGAR,8));
        var quote=ResourceRecycling.plan(inventory,0,s->true);
        inventory.getItem(0).set(DataComponents.CUSTOM_NAME,Component.literal("New authored name"));
        assertEquals(0,SupplyRequests.remaining(inventory,quote,600,s->true).credit());
        inventory.setItem(0,new ItemStack(Items.SUGAR,8));
        assertEquals(0,SupplyRequests.remaining(inventory,quote,600,s->false).credit());
    }
}
