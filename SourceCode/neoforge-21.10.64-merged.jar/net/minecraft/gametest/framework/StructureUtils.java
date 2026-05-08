package net.minecraft.gametest.framework;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class StructureUtils {
    public static final int DEFAULT_Y_SEARCH_RADIUS = 10;
    public static final String DEFAULT_TEST_STRUCTURES_DIR = "Minecraft.Server/src/test/convertables/data";
    public static Path testStructuresDir = Paths.get("Minecraft.Server/src/test/convertables/data");

    public static Rotation getRotationForRotationSteps(int rotationSteps) {
        switch (rotationSteps) {
            case 0:
                return Rotation.NONE;
            case 1:
                return Rotation.CLOCKWISE_90;
            case 2:
                return Rotation.CLOCKWISE_180;
            case 3:
                return Rotation.COUNTERCLOCKWISE_90;
            default:
                throw new IllegalArgumentException("rotationSteps must be a value from 0-3. Got value " + rotationSteps);
        }
    }

    public static int getRotationStepsForRotation(Rotation rotation) {
        switch (rotation) {
            case NONE:
                return 0;
            case CLOCKWISE_90:
                return 1;
            case CLOCKWISE_180:
                return 2;
            case COUNTERCLOCKWISE_90:
                return 3;
            default:
                throw new IllegalArgumentException("Unknown rotation value, don't know how many steps it represents: " + rotation);
        }
    }

    public static TestInstanceBlockEntity createNewEmptyTest(
        ResourceLocation id, BlockPos pos, Vec3i size, Rotation rotation, ServerLevel level
    ) {
        BoundingBox boundingbox = getStructureBoundingBox(TestInstanceBlockEntity.getStructurePos(pos), size, rotation);
        clearSpaceForStructure(boundingbox, level);
        level.setBlockAndUpdate(pos, Blocks.TEST_INSTANCE_BLOCK.defaultBlockState());
        TestInstanceBlockEntity testinstanceblockentity = (TestInstanceBlockEntity)level.getBlockEntity(pos);
        ResourceKey<GameTestInstance> resourcekey = ResourceKey.create(Registries.TEST_INSTANCE, id);
        testinstanceblockentity.set(
            new TestInstanceBlockEntity.Data(Optional.of(resourcekey), size, rotation, false, TestInstanceBlockEntity.Status.CLEARED, Optional.empty())
        );
        return testinstanceblockentity;
    }

    public static void clearSpaceForStructure(BoundingBox boundingBox, ServerLevel level) {
        int i = boundingBox.minY() - 1;
        BlockPos.betweenClosedStream(boundingBox).forEach(p_177748_ -> clearBlock(i, p_177748_, level));
        level.getBlockTicks().clearArea(boundingBox);
        level.clearBlockEvents(boundingBox);
        AABB aabb = AABB.of(boundingBox);
        List<Entity> list = level.getEntitiesOfClass(Entity.class, aabb, p_177750_ -> !(p_177750_ instanceof Player));
        list.forEach(Entity::discard);
    }

    public static BlockPos getTransformedFarCorner(BlockPos pos, Vec3i offset, Rotation rotation) {
        BlockPos blockpos = pos.offset(offset).offset(-1, -1, -1);
        return StructureTemplate.transform(blockpos, Mirror.NONE, rotation, pos);
    }

    public static BoundingBox getStructureBoundingBox(BlockPos pos, Vec3i offset, Rotation rotation) {
        BlockPos blockpos = getTransformedFarCorner(pos, offset, rotation);
        BoundingBox boundingbox = BoundingBox.fromCorners(pos, blockpos);
        int i = Math.min(boundingbox.minX(), boundingbox.maxX());
        int j = Math.min(boundingbox.minZ(), boundingbox.maxZ());
        return boundingbox.move(pos.getX() - i, 0, pos.getZ() - j);
    }

    public static Optional<BlockPos> findTestContainingPos(BlockPos pos, int radius, ServerLevel level) {
        return findTestBlocks(pos, radius, level).filter(p_177756_ -> doesStructureContain(p_177756_, pos, level)).findFirst();
    }

    public static Optional<BlockPos> findNearestTest(BlockPos pos, int radius, ServerLevel level) {
        Comparator<BlockPos> comparator = Comparator.comparingInt(p_177759_ -> p_177759_.distManhattan(pos));
        return findTestBlocks(pos, radius, level).min(comparator);
    }

    public static Stream<BlockPos> findTestBlocks(BlockPos pos, int radius, ServerLevel level) {
        return level.getPoiManager()
            .findAll(p_417693_ -> p_417693_.is(PoiTypes.TEST_INSTANCE), p_417694_ -> true, pos, radius, PoiManager.Occupancy.ANY)
            .map(BlockPos::immutable);
    }

    public static Stream<BlockPos> lookedAtTestPos(BlockPos pos, Entity entity, ServerLevel level) {
        int i = 250;
        Vec3 vec3 = entity.getEyePosition();
        Vec3 vec31 = vec3.add(entity.getLookAngle().scale(250.0));
        return findTestBlocks(pos, 250, level)
            .map(p_396415_ -> level.getBlockEntity(p_396415_, BlockEntityType.TEST_INSTANCE_BLOCK))
            .flatMap(Optional::stream)
            .filter(p_396413_ -> p_396413_.getStructureBounds().clip(vec3, vec31).isPresent())
            .map(BlockEntity::getBlockPos)
            .sorted(Comparator.comparing(pos::distSqr))
            .limit(1L);
    }

    private static void clearBlock(int structureBlockY, BlockPos pos, ServerLevel serverLevel) {
        BlockState blockstate;
        if (pos.getY() < structureBlockY) {
            blockstate = Blocks.STONE.defaultBlockState();
        } else {
            blockstate = Blocks.AIR.defaultBlockState();
        }

        BlockInput blockinput = new BlockInput(blockstate, Collections.emptySet(), null);
        blockinput.place(serverLevel, pos, 818);
        serverLevel.updateNeighborsAt(pos, blockstate.getBlock());
    }

    private static boolean doesStructureContain(BlockPos structureBlockPos, BlockPos posToTest, ServerLevel serverLevel) {
        return serverLevel.getBlockEntity(structureBlockPos) instanceof TestInstanceBlockEntity testinstanceblockentity
            ? testinstanceblockentity.getStructureBoundingBox().isInside(posToTest)
            : false;
    }
}
