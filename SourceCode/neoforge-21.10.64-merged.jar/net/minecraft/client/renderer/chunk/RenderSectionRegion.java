package net.minecraft.client.renderer.chunk;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderSectionRegion implements BlockAndTintGetter {
    public static final int RADIUS = 1;
    public static final int SIZE = 3;
    private final int minSectionX;
    private final int minSectionY;
    private final int minSectionZ;
    private final SectionCopy[] sections;
    private final Level level;
    private final it.unimi.dsi.fastutil.longs.Long2ObjectFunction<net.neoforged.neoforge.model.data.ModelData> modelDataSnapshot;

    /**
 * @deprecated Neo: use {@link #RenderSectionRegion(Level, int, int, int,
 *             SectionCopy[], it.unimi.dsi.fastutil.longs.Long2ObjectFunction)}
 *             instead
 */
    @Deprecated
    RenderSectionRegion(Level level, int minSectionX, int minSectionY, int minSectionZ, SectionCopy[] sections) {
        this(level, minSectionX, minSectionY, minSectionZ, sections, net.neoforged.neoforge.model.data.ModelDataManager.EMPTY_SNAPSHOT);
    }

    RenderSectionRegion(Level level, int minSectionX, int minSectionY, int minSectionZ, SectionCopy[] sections, it.unimi.dsi.fastutil.longs.Long2ObjectFunction<net.neoforged.neoforge.model.data.ModelData> modelDataSnapshot) {
        this.level = level;
        this.minSectionX = minSectionX;
        this.minSectionY = minSectionY;
        this.minSectionZ = minSectionZ;
        this.sections = sections;
        this.modelDataSnapshot = modelDataSnapshot;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.getSection(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getY()),
                SectionPos.blockToSectionCoord(pos.getZ())
            )
            .getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getSection(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getY()),
                SectionPos.blockToSectionCoord(pos.getZ())
            )
            .getBlockState(pos)
            .getFluidState();
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return this.level.getShade(direction, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.getSection(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getY()),
                SectionPos.blockToSectionCoord(pos.getZ())
            )
            .getBlockEntity(pos);
    }

    private SectionCopy getSection(int x, int y, int z) {
        return this.sections[index(this.minSectionX, this.minSectionY, this.minSectionZ, x, y, z)];
    }

    @Override
    public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
        return this.level.getBlockTint(blockPos, colorResolver);
    }

    @Override
    public int getMinY() {
        return this.level.getMinY();
    }

    @Override
    public int getHeight() {
        return this.level.getHeight();
    }

    @Override
    public float getShade(float normalX, float normalY, float normalZ, boolean shade) {
        return this.level.getShade(normalX, normalY, normalZ, shade);
    }

    @Override
    public net.neoforged.neoforge.model.data.ModelData getModelData(BlockPos pos) {
        return modelDataSnapshot.get(pos.asLong());
    }

    @Override
    public net.neoforged.neoforge.common.world.AuxiliaryLightManager getAuxLightManager(net.minecraft.world.level.ChunkPos pos) {
        return this.getSection(pos.x, this.minSectionY, pos.z).wrapped.getAuxLightManager(pos);
    }

    public static int index(int minX, int minY, int minZ, int x, int y, int z) {
        return x - minX + (y - minY) * 3 + (z - minZ) * 3 * 3;
    }
}
