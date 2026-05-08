package net.minecraft.client.gui.components.debug;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryChunkGeneration implements DebugScreenEntry {
    public static final ResourceLocation GROUP = ResourceLocation.withDefaultNamespace("chunk_generation");

    @Override
    public void display(DebugScreenDisplayer p_434944_, @Nullable Level p_434973_, @Nullable LevelChunk p_434305_, @Nullable LevelChunk p_435595_) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        ServerLevel serverlevel = p_434973_ instanceof ServerLevel ? (ServerLevel)p_434973_ : null;
        if (entity != null && serverlevel != null) {
            BlockPos blockpos = entity.blockPosition();
            ServerChunkCache serverchunkcache = serverlevel.getChunkSource();
            List<String> list = new ArrayList<>();
            ChunkGenerator chunkgenerator = serverchunkcache.getGenerator();
            RandomState randomstate = serverchunkcache.randomState();
            chunkgenerator.addDebugScreenInfo(list, randomstate, blockpos);
            Climate.Sampler climate$sampler = randomstate.sampler();
            BiomeSource biomesource = chunkgenerator.getBiomeSource();
            biomesource.addDebugInfo(list, blockpos, climate$sampler);
            if (p_435595_ != null && p_435595_.isOldNoiseGeneration()) {
                list.add("Blending: Old");
            }

            p_434944_.addToGroup(GROUP, list);
        }
    }
}
