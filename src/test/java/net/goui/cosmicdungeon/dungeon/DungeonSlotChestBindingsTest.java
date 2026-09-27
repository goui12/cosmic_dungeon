package net.goui.cosmicdungeon.dungeon;

import com.sk89q.worldedit.math.BlockVector3;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.goui.cosmicdungeon.block.entity.ClassChestOwnership;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DungeonSlotChestBindingsTest {
    @Test void everyGroupUsesTheOriginalSlotOwnerEvenWithDuplicateClasses() {
        var first = new ClassChestOwnership(UUID.randomUUID(), "PlayerOne");
        var second = new ClassChestOwnership(UUID.randomUUID(), "PlayerTwo");
        var roster = new ArrayList<>(List.of(first, second));
        var bindings = new DungeonSlotChestBindings(roster);
        roster.removeFirst();
        var plan = DungeonStartupSchematicPlan.buildPlan(List.of("pyroclast", "pyroclast"));
        for (var request : plan.requests()) {
            if (request.logicalSlot() == 1) assertEquals(first, bindings.ownerFor(1));
            else if (request.logicalSlot() == 2) assertEquals(second, bindings.ownerFor(2));
            else assertFalse(bindings.ownerFor(request.logicalSlot()).permits(first.playerId()));
        }
        assertThrows(IllegalArgumentException.class, () -> bindings.ownerFor(0));
        assertThrows(IllegalArgumentException.class, () -> bindings.ownerFor(7));
        assertThrows(IllegalArgumentException.class, () -> new DungeonSlotChestBindings(List.of(first, first)));
        assertThrows(IllegalArgumentException.class, () -> new DungeonSlotChestBindings(List.of(ClassChestOwnership.UNASSIGNED)));
    }

    @Test void clipboardOriginAndAllFourRotationsMatchPastedChestCoordinates() {
        var source = BlockVector3.at(25, 8, -16);
        var origin = BlockVector3.at(23, 5, -19);
        var target = new BlockPos(100, -60, 300);
        assertEquals(new BlockPos(102, -57, 303), DungeonSlotChestBindings.pastedPosition(source, origin, target, 0));
        assertEquals(new BlockPos(103, -57, 298), DungeonSlotChestBindings.pastedPosition(source, origin, target, 90));
        assertEquals(new BlockPos(98, -57, 297), DungeonSlotChestBindings.pastedPosition(source, origin, target, 180));
        assertEquals(new BlockPos(97, -57, 302), DungeonSlotChestBindings.pastedPosition(source, origin, target, 270));
        for (int rotation : new int[]{0,90,180,270})
            assertEquals(target, DungeonSlotChestBindings.pastedPosition(origin, origin, target, rotation));
        assertThrows(IllegalArgumentException.class, () -> DungeonSlotChestBindings.pastedPosition(source, origin, target, 45));
    }
}
