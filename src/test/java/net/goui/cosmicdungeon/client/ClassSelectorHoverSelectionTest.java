package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class ClassSelectorHoverSelectionTest {
    private final ClassSelectorHoverSelection selection = new ClassSelectorHoverSelection();
    private final Vec3 forward = new Vec3(0, 0, 1);
    private final Vec3 pedestal = new Vec3(0.5, 0.2, 0);

    @Test
    public void requiresAnActualMatchingBlockHitAndSolidUnderTheCrosshair() {
        Vec3 eye = new Vec3(0.5, 0.2, -3);
        assertTrue(selection.canShow(BlockPos.ZERO, hit(pedestal, BlockPos.ZERO), eye, forward, 4.5));
        assertFalse(selection.canShow(BlockPos.ZERO, null, eye, forward, 4.5));
        assertFalse(selection.canShow(BlockPos.ZERO,
                BlockHitResult.miss(pedestal, Direction.NORTH, BlockPos.ZERO), eye, forward, 4.5));
        assertFalse(selection.canShow(BlockPos.ZERO, hit(pedestal, BlockPos.ZERO.north()), eye, forward, 4.5),
                "A nearer wall/native target must suppress the selector label");
        assertFalse(selection.canShow(BlockPos.ZERO, hit(pedestal, BlockPos.ZERO), eye, forward.reverse(), 4.5));
        assertFalse(selection.canShow(BlockPos.ZERO, hit(new Vec3(0.95, 1.9, 0), BlockPos.ZERO),
                new Vec3(0.95, 1.9, -1), forward, 4.5), "Empty space above the pedestal is not a solid");
    }

    @Test
    public void respectsPlayerReachAndHardCapsExtendedPointerVectors() {
        var hit = hit(pedestal, BlockPos.ZERO);
        assertTrue(selection.canShow(BlockPos.ZERO, hit, new Vec3(0.5, 0.2, -2.9), forward, 3));
        assertFalse(selection.canShow(BlockPos.ZERO, hit, new Vec3(0.5, 0.2, -3.1), forward, 3));
        assertTrue(selection.canShow(BlockPos.ZERO, hit, new Vec3(0.5, 0.2, -4.9), forward, 1000));
        assertFalse(selection.canShow(BlockPos.ZERO, hit, new Vec3(0.5, 0.2, -5.1), forward, 1000));
        assertFalse(selection.canShow(BlockPos.ZERO, hit, new Vec3(0.5, 0.2, -1000),
                new Vec3(0, 0, 10000), 10000));
        assertFalse(selection.canShow(BlockPos.ZERO, hit, new Vec3(0.5, 0.2, -3), Vec3.ZERO, 5));
        assertFalse(selection.canShow(BlockPos.ZERO, hit, new Vec3(0.5, 0.2, -3), forward, Double.NaN));
        assertFalse(selection.canShow(BlockPos.ZERO, hit, new Vec3(Double.NaN, 0, 0), forward, 5));
    }

    @Test
    public void upperTipUsesItsRealSurfaceRatherThanOnlyTheNormalizedPacketHit() {
        Vec3 eye = new Vec3(9.0 / 16, 30.0 / 16, -2);
        var normalized = hit(new Vec3(9.0 / 16, 1.499, 7.0 / 16), BlockPos.ZERO);
        assertTrue(selection.canShow(BlockPos.ZERO, normalized, eye, forward, 4.5));
        assertFalse(selection.canShow(BlockPos.ZERO, normalized, eye, forward.reverse(), 4.5));
        var shiftedRoot = new BlockPos(-20, 70, 35);
        var offset = Vec3.atLowerCornerOf(shiftedRoot);
        assertTrue(selection.canShow(shiftedRoot, hit(normalized.getLocation().add(offset), shiftedRoot),
                eye.add(offset), forward, 4.5));
    }

    @Test
    public void usesExistingClassTranslationsAndClampsUnknownOrUnselectedClasses() {
        for (String id : ClassKeys.ORDERED) {
            var component = selection.label(id);
            assertEquals("Class: ", assertInstanceOf(PlainTextContents.class, component.getContents()).text());
            var name = assertInstanceOf(TranslatableContents.class, component.getSiblings().getFirst().getContents());
            assertEquals("playerclass.cosmicdungeon." + id, name.getKey());
        }
        for (String id : new String[] {null, "", "unknown"}) {
            var name = assertInstanceOf(TranslatableContents.class,
                    selection.label(id).getSiblings().getFirst().getContents());
            assertEquals("playerclass.cosmicdungeon.none", name.getKey());
        }
    }

    private BlockHitResult hit(Vec3 point, BlockPos root) {
        return new BlockHitResult(point, Direction.NORTH, root, false);
    }
}
