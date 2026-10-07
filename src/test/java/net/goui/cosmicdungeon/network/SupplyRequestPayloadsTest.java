package net.goui.cosmicdungeon.network;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SupplyRequestPayloadsTest {
    private static final UUID ID=new UUID(1,2),OWNER=new UUID(3,4);
    @Test void ingredientsAreImmutableAndCompareFullComponents(){
        var stack=new ItemStack(Items.SUGAR,3);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Authored"));
        var a=new SupplyRequestPayloads.Ingredient(stack);var b=new SupplyRequestPayloads.Ingredient(stack.copy());
        assertEquals(a,b);assertEquals(a.hashCode(),b.hashCode());stack.shrink(2);assertEquals(3,a.stack().getCount());
        a.stack().shrink(2);assertEquals(3,a.stack().getCount());
        var changed=b.stack();changed.set(DataComponents.CUSTOM_NAME,Component.literal("Other"));
        assertNotEquals(a,new SupplyRequestPayloads.Ingredient(changed));
    }
    @Test void exactYieldIdentityListAndConsentBoundsAreEnforced(){
        var item=new SupplyRequestPayloads.Ingredient(new ItemStack(Items.SUGAR,3));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestPayloads.Card(ID,OWNER,"Name","bogatyr","brewing_supplies",3,List.of(item),true));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestPayloads.Card(ID,OWNER,"Name","theurgist","brewing_supplies",4,List.of(item),true));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestPayloads.Action(1,0,SupplyRequestPayloads.Decision.ACCEPT,List.of(ID,ID)));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestPayloads.Action(1,0,SupplyRequestPayloads.Decision.REQUEST,List.of(ID)));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestPayloads.Action(1,0,SupplyRequestPayloads.Decision.ACCEPT_ALL,List.of()));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestPayloads.Action(0,0,SupplyRequestPayloads.Decision.REQUEST,List.of()));
        var cards=new ArrayList<SupplyRequestPayloads.Card>();
        for(int i=0;i<33;i++)cards.add(new SupplyRequestPayloads.Card(new UUID(0,i),OWNER,"Name","theurgist","brewing_supplies",3,List.of(item),true));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestPayloads.View(1,1,true,true,false,cards));
    }
}
