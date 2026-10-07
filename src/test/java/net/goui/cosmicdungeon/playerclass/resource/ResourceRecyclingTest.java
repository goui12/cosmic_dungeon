package net.goui.cosmicdungeon.playerclass.resource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ResourceRecyclingTest {
    @Test void onlyEligibleItemsUpToHeadroomAndComponentsSurvive(){
        var inventory=new SimpleContainer(4);
        var sugar=new ItemStack(Items.SUGAR,8);sugar.set(DataComponents.CUSTOM_NAME,Component.literal("Authored sugar"));
        inventory.setItem(0,sugar);inventory.setItem(1,new ItemStack(Items.FIREWORK_ROCKET,4));
        inventory.setItem(2,new ItemStack(Items.SUGAR,12));
        var plan=ResourceRecycling.plan(inventory,595,stack->stack.is(Items.SUGAR));
        assertEquals(5,plan.credit());assertTrue(ResourceRecycling.apply(inventory,plan));
        assertEquals(3,inventory.getItem(0).getCount());assertEquals(sugar.get(DataComponents.CUSTOM_NAME),inventory.getItem(0).get(DataComponents.CUSTOM_NAME));
        assertEquals(4,inventory.getItem(1).getCount());assertEquals(12,inventory.getItem(2).getCount());
        ResourceRecycling.rollbackBeforeSave(inventory,plan);assertEquals(8,inventory.getItem(0).getCount());
    }
    @Test void capAndNoEligibleItemsConsumeNothing(){
        var inventory=new SimpleContainer(new ItemStack(Items.BEEF,64));
        assertEquals(0,ResourceRecycling.plan(inventory,600,stack->true).credit());
        assertEquals(0,ResourceRecycling.plan(inventory,0,stack->false).credit());
        assertEquals(64,inventory.getItem(0).getCount());
    }
    @Test void aStaleLaterSlotRejectsWholePlanBeforeAnyMutation(){
        var inventory=new SimpleContainer(new ItemStack(Items.BEEF,3),new ItemStack(Items.BEEF,4));
        var plan=ResourceRecycling.plan(inventory,0,stack->true);
        inventory.setItem(1,new ItemStack(Items.COOKED_BEEF,4));
        assertFalse(ResourceRecycling.apply(inventory,plan));assertEquals(3,inventory.getItem(0).getCount());
        assertTrue(inventory.getItem(1).is(Items.COOKED_BEEF));
    }
}
