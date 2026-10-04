package net.goui.cosmicdungeon.npc.tamsin;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.goui.cosmicdungeon.npc.NpcIdentityChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class TamsinPlacementTest {
    private final TamsinPlacement placement = TamsinPlacement.INSTANCE;

    @Test
    public void placementPrefersThePlayersSideAndSkipsBlockedSpots() {
        BlockPos root = new BlockPos(10, 64, 10);
        Vec3 player = new Vec3(10.5, 64, 5);
        assertEquals(root.north(2), placement.findSpot(root, player, pos -> true).orElseThrow());
        assertEquals(root.south(), placement.findSpot(root, player, root.south()::equals).orElseThrow());
        assertEquals(root.east().above(),
                placement.findSpot(root, player, root.east().above()::equals).orElseThrow());
    }

    @Test
    public void fullyBlockedPlacementIsBoundedAndNeverChoosesTheSelector() {
        BlockPos root = new BlockPos(-17, -20, 15);
        var visited = new HashSet<BlockPos>();
        var result = placement.findSpot(root, Vec3.ZERO, pos -> {
            assertTrue(visited.add(pos), "Every candidate is checked only once");
            assertNotEquals(root.getX() + ":" + root.getZ(), pos.getX() + ":" + pos.getZ());
            assertTrue(Math.abs(pos.getX() - root.getX()) <= 2);
            assertTrue(Math.abs(pos.getY() - root.getY()) <= 1);
            assertTrue(Math.abs(pos.getZ() - root.getZ()) <= 2);
            return false;
        });
        assertTrue(result.isEmpty());
        assertEquals(36, visited.size());
        AtomicInteger checks = new AtomicInteger();
        assertTrue(placement.findSpot(root, Vec3.ZERO, pos -> checks.incrementAndGet() == 1).isPresent());
        assertEquals(1, checks.get(), "Stop at the first valid space");
    }

    @Test
    public void bindingSurvivesOnlySuccessfulInsertionAndDoesNotResetAgreements() throws Exception {
        var data = emptyData();
        UUID player = UUID.randomUUID(), oldNpc = UUID.randomUUID(), newNpc = UUID.randomUUID();
        var oldBinding = new TamsinData.Binding("minecraft:overworld", BlockPos.ZERO.asLong());
        var newBinding = new TamsinData.Binding("minecraft:overworld", new BlockPos(4, 64, 4).asLong());
        data.accept(player);
        data.bind(oldNpc, oldBinding);
        assertFalse(placement.bindAndSpawn(data, newNpc, newBinding, () -> {
            assertEquals(newBinding, data.binding(newNpc), "Join validation sees the pending binding");
            return false;
        }));
        assertNull(data.binding(newNpc));
        assertEquals(oldBinding, data.binding(oldNpc));
        assertThrows(IllegalStateException.class, () -> placement.bindAndSpawn(data, newNpc, newBinding, () -> {
            throw new IllegalStateException("Rejected entity join");
        }));
        assertNull(data.binding(newNpc));
        assertFalse(placement.bindAndSpawn(data, oldNpc, newBinding, () -> false));
        assertEquals(oldBinding, data.binding(oldNpc), "An existing binding is restored on refusal");
        assertTrue(placement.bindAndSpawn(data, newNpc, newBinding, () -> true));
        assertEquals(newBinding, data.binding(newNpc));
        assertEquals(oldBinding, data.binding(oldNpc), "Keep stale records for unloaded identity rejection");
        assertTrue(data.accepted(player), "Developer placement does not reset player agreement");
    }

    @Test
    public void existingIdentityReplacementAndRollbackContractStillPasses() throws Exception {
        NpcIdentityChecks.main(new String[0]);
    }

    private TamsinData emptyData() throws Exception {
        var constructor = TamsinData.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
