package net.goui.cosmicdungeon.block.custom;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class ClassSelectorPresentationTest {
    private static final Vec3 FORWARD = new Vec3(0, 0, 1);

    @Test
    public void collisionFollowsSolidsAndLeavesAirBetweenTheProps() {
        var world = new Fixture();
        var state = world.getBlockState(BlockPos.ZERO);
        var shape = state.getShape(world, BlockPos.ZERO);
        assertSame(ClassSelectorShape.INSTANCE.shape(), shape);
        assertEquals(shape.toAabbs(), state.getCollisionShape(world, BlockPos.ZERO).toAabbs());
        assertEquals(2.0, shape.max(Direction.Axis.Y));
        assertTrue(inside(new Vec3(0.5, 0.15, 0.5)), "Pedestal remains solid");
        assertTrue(inside(new Vec3(9.0 / 16, 30.0 / 16, 8.0 / 16)), "Upper trident is solid");
        assertTrue(inside(new Vec3(-0.5 / 16, 16.0 / 16, 11.5 / 16)), "Shield's side overhang remains solid");
        assertFalse(inside(new Vec3(0.95, 0.7, 0.1)), "Empty corner must not retain the old cube collision");
        assertFalse(inside(new Vec3(0.95, 1.5, 0.5)), "No large bounding box around every prop");
        assertTrue(shape.toAabbs().size() < 512, "Shape remains bounded and cached");
        System.out.println("Selector cached collision boxes: " + shape.toAabbs().size());
    }

    @Test
    public void upperTridentIsSelectableAndUsesNativePacketCoordinateBounds() {
        var world = new Fixture();
        Vec3 eye = new Vec3(9.0 / 16, 30.0 / 16, -2);
        var hit = pick(world, eye, 5, miss(eye, 5));
        assertInstanceOf(BlockHitResult.class, hit);
        assertEquals(HitResult.Type.BLOCK, hit.getType());
        assertEquals(BlockPos.ZERO, ((BlockHitResult) hit).getBlockPos());
        Vec3 offset = hit.getLocation().subtract(Vec3.atCenterOf(BlockPos.ZERO));
        assertTrue(Math.abs(offset.x) < 1.0000001 && Math.abs(offset.y) < 1.0000001
                && Math.abs(offset.z) < 1.0000001, "Upper clicks must pass vanilla's hit-coordinate envelope");
    }

    @Test
    public void shieldOverhangTargetsItsRootFromTheAdjacentCell() {
        var world = new Fixture();
        Vec3 eye = new Vec3(-0.5 / 16, 1, -2);
        var hit = pick(world, eye, 5, miss(eye, 5));
        assertEquals(HitResult.Type.BLOCK, hit.getType());
        assertEquals(BlockPos.ZERO, ((BlockHitResult) hit).getBlockPos());
    }

    @Test
    public void gapsReachAndNativeWallOrEntityOcclusionArePreserved() {
        var world = new Fixture();
        Vec3 eye = new Vec3(9.0 / 16, 30.0 / 16, -2);
        HitResult wall = new BlockHitResult(new Vec3(eye.x, eye.y, -0.5),
                Direction.NORTH, new BlockPos(0, 1, -1), false);
        assertSame(wall, pick(world, eye, 5, wall));
        HitResult entity = new HitResult(new Vec3(eye.x, eye.y, -0.5)) {
            @Override public Type getType() { return Type.ENTITY; }
        };
        assertSame(entity, pick(world, eye, 5, entity));
        var shortRay = miss(eye, 1);
        assertSame(shortRay, pick(world, eye, 1, shortRay), "Cannot extend player reach");
        Vec3 gap = new Vec3(0.95, 1.5, -2);
        var gapMiss = miss(gap, 5);
        assertSame(gapMiss, pick(world, gap, 5, gapMiss), "Can aim through empty space");
        Vec3 reverseEye = new Vec3(eye.x, eye.y, 3);
        var reverse = ClassSelectorTargeting.INSTANCE.refine(world, reverseEye,
                new Vec3(0, 0, -1), 5, miss(reverseEye, 5));
        assertEquals(HitResult.Type.BLOCK, reverse.getType());
    }

    @Test
    public void targetingWorkIsBoundedEvenWithAnExtremeReachAttribute() {
        var world = new Fixture();
        world.blocks.clear();
        Vec3 eye = new Vec3(0.5, 1.8, -2);
        var original = miss(eye, 1_000_000);
        assertSame(original, pick(world, eye, 1_000_000, original));
        assertTrue(world.reads <= 64 * 27 + 1, "Bounded voxel traversal, no world scan");
        world.reads = 0;
        assertSame(original, pick(world, eye, Double.NaN, original));
        assertEquals(0, world.reads);
        long began = System.nanoTime();
        for (int i = 0; i < 5000; i++) pick(world, eye, 5, miss(eye, 5));
        System.out.println("Selector empty-fixture targeting average microseconds: "
                + (System.nanoTime() - began) / 5_000_000.0
                + " (offline lookup fixture, not a native frame-time measurement)");
    }

    @Test
    public void generatedItemUsesTheSeparateTransparentIcon() throws Exception {
        String assets = "/assets/cosmicdungeon/";
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                assets + "items/class_selector_block.json"), StandardCharsets.UTF_8)) {
            assertEquals("cosmicdungeon:item/class_selector_block", JsonParser.parseReader(reader)
                    .getAsJsonObject().getAsJsonObject("model").get("model").getAsString());
        }
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                assets + "models/item/class_selector_block.json"), StandardCharsets.UTF_8)) {
            var model = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals("minecraft:item/generated", model.get("parent").getAsString());
            assertEquals("cosmicdungeon:item/class_selector_block",
                    model.getAsJsonObject("textures").get("layer0").getAsString());
        }
        try (var stream = getClass().getResourceAsStream(assets + "textures/item/class_selector_block.png")) {
            var image = ImageIO.read(stream);
            assertEquals(64, image.getWidth()); assertEquals(64, image.getHeight());
            assertTrue(image.getColorModel().hasAlpha());
            assertEquals(0, image.getRGB(0, 0) >>> 24);
            assertEquals(0, image.getRGB(63, 63) >>> 24);
        }
    }

    private boolean inside(Vec3 point) {
        return ClassSelectorShape.INSTANCE.shape().toAabbs().stream().anyMatch(box -> box.contains(point));
    }

    private HitResult pick(Fixture world, Vec3 eye, double reach, HitResult original) {
        return ClassSelectorTargeting.INSTANCE.refine(world, eye, FORWARD, reach, original);
    }

    private BlockHitResult miss(Vec3 eye, double reach) {
        Vec3 end = eye.add(FORWARD.scale(reach));
        return BlockHitResult.miss(end, Direction.NORTH, BlockPos.containing(end));
    }

    private static final class Fixture implements BlockGetter {
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();
        private int reads;
        private Fixture() { blocks.put(BlockPos.ZERO, ModBlocks.CLASS_SELECTOR_BLOCK.get().defaultBlockState()); }
        @Override public BlockState getBlockState(BlockPos pos) {
            reads++; return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }
        @Override public BlockEntity getBlockEntity(BlockPos pos) { return null; }
        @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
        @Override public int getHeight() { return 384; }
        @Override public int getMinY() { return -64; }
    }
}
