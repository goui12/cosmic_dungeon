package net.minecraft.client.gui.components.debug;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntrySpawnCounts implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer p_433443_, @Nullable Level p_435954_, @Nullable LevelChunk p_436014_, @Nullable LevelChunk p_434093_) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        ServerLevel serverlevel = p_435954_ instanceof ServerLevel ? (ServerLevel)p_435954_ : null;
        if (entity != null && serverlevel != null) {
            ServerChunkCache serverchunkcache = serverlevel.getChunkSource();
            NaturalSpawner.SpawnState naturalspawner$spawnstate = serverchunkcache.getLastSpawnState();
            if (naturalspawner$spawnstate != null) {
                Object2IntMap<MobCategory> object2intmap = naturalspawner$spawnstate.getMobCategoryCounts();
                int i = naturalspawner$spawnstate.getSpawnableChunkCount();
                p_433443_.addLine(
                    "SC: "
                        + i
                        + ", "
                        + Stream.of(MobCategory.values())
                            .map(p_435910_ -> Character.toUpperCase(p_435910_.getName().charAt(0)) + ": " + object2intmap.getInt(p_435910_))
                            .collect(Collectors.joining(", "))
                );
            }
        }
    }
}
