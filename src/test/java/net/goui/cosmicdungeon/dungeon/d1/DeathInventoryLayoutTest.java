package net.goui.cosmicdungeon.dungeon.d1;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeathInventoryLayoutTest {
    private static List<ItemStack> empty(int size) {
        var list = new ArrayList<ItemStack>(size);
        for (int i = 0; i < size; i++) list.add(ItemStack.EMPTY);
        return list;
    }

    @Test void movesOnlyNewlyInsertedAmountAndNeverPreexistingCopies() {
        var before = empty(8);
        var after = empty(8);
        before.set(0, new ItemStack(Items.DIAMOND, 4));
        after.set(0, new ItemStack(Items.DIAMOND, 6)); // actual pickup added only two
        var plan = DeathInventoryLayout.plan(before, after, new ItemStack(Items.DIAMOND),
                5, 64, true);
        assertEquals(2, plan.addToTarget());
        assertEquals(List.of(new DeathInventoryLayout.Move(0, 2)), plan.moves());
    }

    @Test void occupiedIncompatibleTargetIsNeverOverwritten() {
        var before = empty(8);
        var after = empty(8);
        after.set(0, new ItemStack(Items.DIAMOND, 3));
        after.set(5, new ItemStack(Items.STONE, 1));
        assertTrue(DeathInventoryLayout.plan(before, after, new ItemStack(Items.DIAMOND),
                5, 64, true).empty());
    }

    @Test void compatibleTargetUsesOnlyAvailableHeadroom() {
        var before = empty(8);
        var after = empty(8);
        before.set(0, new ItemStack(Items.APPLE, 10));
        after.set(0, new ItemStack(Items.APPLE, 20)); // ten recovered
        before.set(5, new ItemStack(Items.APPLE, 60));
        after.set(5, new ItemStack(Items.APPLE, 60));
        var plan = DeathInventoryLayout.plan(before, after, new ItemStack(Items.APPLE),
                5, 64, true);
        assertEquals(4, plan.addToTarget());
        assertEquals(List.of(new DeathInventoryLayout.Move(0, 4)), plan.moves());
    }

    @Test void componentsArePartOfTheRecoveryIdentity() {
        var named = new ItemStack(Items.DIAMOND, 1);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Recovered"));
        var before = empty(8);
        var after = empty(8);
        after.set(0, new ItemStack(Items.DIAMOND, 1));
        assertTrue(DeathInventoryLayout.plan(before, after, named, 5, 64, true).empty());
    }

    @Test void slotPlacementRulesCanVetoEquipmentRecovery() {
        var before = empty(8);
        var after = empty(8);
        after.set(0, new ItemStack(Items.DIAMOND, 1));
        assertTrue(DeathInventoryLayout.plan(before, after, new ItemStack(Items.DIAMOND),
                5, 1, false).empty());
    }
}
