package net.goui.cosmicdungeon.client.screen.requests;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SupplyRequestsSnapshotTest {
    static SupplyRequestsSnapshot.Card card(UUID id,int amount,boolean accept){
        return new SupplyRequestsSnapshot.Card(id,UUID.randomUUID(),"Requester","theurgist","brewing_supplies",amount,
                amount==0?List.of():List.of(new SupplyRequestsSnapshot.Ingredient(new ItemStack(Items.SUGAR,amount))),accept);
    }
    @Test void nativeComponentsCountsAndQuoteEqualitySurviveDefensiveCopies(){
        var stack=new ItemStack(Items.SUGAR,7);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Authored supplies"));
        var ingredient=new SupplyRequestsSnapshot.Ingredient(stack);
        var identical=new SupplyRequestsSnapshot.Ingredient(stack.copy());
        assertEquals(ingredient,identical);assertEquals(ingredient.hashCode(),identical.hashCode());
        stack.shrink(3);assertEquals(7,ingredient.stack().getCount());
        var exported=ingredient.stack();exported.setCount(2);exported.remove(DataComponents.CUSTOM_NAME);
        assertEquals(7,ingredient.count());assertEquals("Authored supplies",ingredient.stack().getHoverName().getString());
        assertNotEquals(ingredient,new SupplyRequestsSnapshot.Ingredient(stack));
        var renamed=ingredient.stack();renamed.set(DataComponents.CUSTOM_NAME,Component.literal("Different"));
        assertNotEquals(ingredient,new SupplyRequestsSnapshot.Ingredient(renamed));
        var id=UUID.randomUUID();var owner=UUID.randomUUID();
        var a=new SupplyRequestsSnapshot.Card(id,owner,"Player","theurgist","brewing_supplies",7,List.of(ingredient),true);
        var b=new SupplyRequestsSnapshot.Card(id,owner,"Player","theurgist","brewing_supplies",7,List.of(identical),true);
        assertEquals(new SupplyRequestsSnapshot(8,3,true,true,false,List.of(a)),new SupplyRequestsSnapshot(8,3,true,true,false,List.of(b)));
    }
    @Test void cardsAndPayloadsRejectUnboundedOrInconsistentConsent(){
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestsSnapshot.Ingredient(ItemStack.EMPTY));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestsSnapshot.Ingredient(new ItemStack(Items.SUGAR,601)));
        var id=UUID.randomUUID();var zero=card(id,0,false);
        assertThrows(IllegalArgumentException.class,()->card(id,0,true));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestsSnapshot.Card(id,UUID.randomUUID(),"Player","bogatyr",
                "brewing_supplies",0,List.of(),false));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestsSnapshot.Card(id,UUID.randomUUID(),"x".repeat(65),"theurgist",
                "brewing_supplies",0,List.of(),false));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestsSnapshot.Card(id,UUID.randomUUID(),"Player","theurgist",
                "brewing_supplies",2,List.of(new SupplyRequestsSnapshot.Ingredient(new ItemStack(Items.SUGAR))),true));
        var many=new ArrayList<SupplyRequestsSnapshot.Card>();
        for(int i=0;i<33;i++)many.add(card(UUID.randomUUID(),1,true));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestsSnapshot(8,3,true,true,false,many));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestsSnapshot(8,3,true,true,false,List.of(zero,zero)));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestsSnapshot(0,0,false,false,false,List.of(zero)));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestsSnapshot(8,0,true,false,true,List.of()));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestsSnapshot(8,0,true,false,false,List.of(card(id,1,true))));
        var source=new ArrayList<>(List.of(zero));var snapshot=new SupplyRequestsSnapshot(8,0,true,true,false,source);
        source.clear();assertEquals(1,snapshot.cards().size());assertThrows(UnsupportedOperationException.class,()->snapshot.cards().clear());
    }
}
