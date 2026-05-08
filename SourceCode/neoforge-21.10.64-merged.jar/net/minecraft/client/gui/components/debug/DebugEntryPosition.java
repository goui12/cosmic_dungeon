package net.minecraft.client.gui.components.debug;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryPosition implements DebugScreenEntry {
    public static final ResourceLocation GROUP = ResourceLocation.withDefaultNamespace("position");

    @Override
    public void display(DebugScreenDisplayer p_433233_, @Nullable Level p_433124_, @Nullable LevelChunk p_435598_, @Nullable LevelChunk p_433371_) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        if (entity != null) {
            BlockPos blockpos = minecraft.getCameraEntity().blockPosition();
            ChunkPos chunkpos = new ChunkPos(blockpos);
            Direction direction = entity.getDirection();

            String s = switch (direction) {
                case NORTH -> "Towards negative Z";
                case SOUTH -> "Towards positive Z";
                case WEST -> "Towards negative X";
                case EAST -> "Towards positive X";
                default -> "Invalid";
            };
            LongSet longset = (LongSet)(p_433124_ instanceof ServerLevel ? ((ServerLevel)p_433124_).getForceLoadedChunks() : LongSets.EMPTY_SET);
            p_433233_.addToGroup(
                GROUP,
                List.of(
                    String.format(
                        Locale.ROOT,
                        "XYZ: %.3f / %.5f / %.3f",
                        minecraft.getCameraEntity().getX(),
                        minecraft.getCameraEntity().getY(),
                        minecraft.getCameraEntity().getZ()
                    ),
                    String.format(Locale.ROOT, "Block: %d %d %d", blockpos.getX(), blockpos.getY(), blockpos.getZ()),
                    String.format(
                        Locale.ROOT,
                        "Chunk: %d %d %d [%d %d in r.%d.%d.mca]",
                        chunkpos.x,
                        SectionPos.blockToSectionCoord(blockpos.getY()),
                        chunkpos.z,
                        chunkpos.getRegionLocalX(),
                        chunkpos.getRegionLocalZ(),
                        chunkpos.getRegionX(),
                        chunkpos.getRegionZ()
                    ),
                    String.format(
                        Locale.ROOT, "Facing: %s (%s) (%.1f / %.1f)", direction, s, Mth.wrapDegrees(entity.getYRot()), Mth.wrapDegrees(entity.getXRot())
                    ),
                    minecraft.level.dimension().location() + " FC: " + longset.size()
                )
            );
        }
    }
}
