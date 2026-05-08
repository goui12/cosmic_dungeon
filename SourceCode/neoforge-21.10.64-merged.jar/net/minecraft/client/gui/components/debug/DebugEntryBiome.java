package net.minecraft.client.gui.components.debug;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryBiome implements DebugScreenEntry {
    public static final ResourceLocation GROUP = ResourceLocation.withDefaultNamespace("biome");

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level level, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        if (entity != null && minecraft.level != null) {
            BlockPos blockpos = entity.blockPosition();
            if (minecraft.level.isInsideBuildHeight(blockpos.getY())) {
                if (SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES && level instanceof ServerLevel) {
                    displayer.addToGroup(
                        GROUP, List.of("Biome: " + printBiome(minecraft.level.getBiome(blockpos)), "Server Biome: " + printBiome(level.getBiome(blockpos)))
                    );
                } else {
                    displayer.addLine("Biome: " + printBiome(minecraft.level.getBiome(blockpos)));
                }
            }
        }
    }

    private static String printBiome(Holder<Biome> biome) {
        return biome.unwrap().map(p_434771_ -> p_434771_.location().toString(), p_434936_ -> "[unregistered " + p_434936_ + "]");
    }
}
