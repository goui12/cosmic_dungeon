package net.goui.cosmicdungeon.client;

import java.util.List;
import net.minecraft.world.inventory.Slot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class InventoryCraftingPresentationTest {
    private static Slot slot(int index) {
        // Selection uses identity only; no item/container reads are necessary.
        var slot = new Slot(null, index, 0, 0);
        slot.index = index;
        return slot;
    }

    @Test void onlyTheActualResultAndInputObjectsAreHidden() {
        var result = slot(0);
        var inputs = List.of(slot(1), slot(2), slot(3), slot(4));
        assertTrue(InventoryCraftingPresentation.craftingSlot(result, result, inputs));
        for (var input : inputs)
            assertTrue(InventoryCraftingPresentation.craftingSlot(input, result, inputs));
        assertFalse(InventoryCraftingPresentation.craftingSlot(null, result, inputs));
        for (int index = 0; index <= 4; index++)
            assertFalse(InventoryCraftingPresentation.craftingSlot(slot(index), result, inputs),
                    "An unrelated menu's coindexed slot must remain visible");
    }

    @Test void playerStorageEquipmentAndMetalmancerExtrasRemainUntouched() {
        var result = slot(0);
        var inputs = List.of(slot(1), slot(2), slot(3), slot(4));
        // Both supported native layouts put every storage/equipment slot after index4;
        // ExtraInventoryMenu also owns its extra3 at41..43 and equipment44..48.
        for (int index = 5; index <= 48; index++)
            assertFalse(InventoryCraftingPresentation.craftingSlot(slot(index), result, inputs));
    }
}
