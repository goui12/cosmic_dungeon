package net.minecraft.client.gui.components.debug;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryLight implements DebugScreenEntry {
    public static final ResourceLocation GROUP = ResourceLocation.withDefaultNamespace("light");

    @Override
    public void display(DebugScreenDisplayer p_436067_, @Nullable Level p_435429_, @Nullable LevelChunk p_434364_, @Nullable LevelChunk p_435633_) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        if (entity != null && minecraft.level != null) {
            BlockPos blockpos = entity.blockPosition();
            int i = minecraft.level.getChunkSource().getLightEngine().getRawBrightness(blockpos, 0);
            int j = minecraft.level.getBrightness(LightLayer.SKY, blockpos);
            int k = minecraft.level.getBrightness(LightLayer.BLOCK, blockpos);
            String s = "Client Light: " + i + " (" + j + " sky, " + k + " block)";
            if (SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES) {
                String s1;
                if (p_435633_ != null) {
                    LevelLightEngine levellightengine = p_435633_.getLevel().getLightEngine();
                    s1 = "Server Light: ("
                        + levellightengine.getLayerListener(LightLayer.SKY).getLightValue(blockpos)
                        + " sky, "
                        + levellightengine.getLayerListener(LightLayer.BLOCK).getLightValue(blockpos)
                        + " block)";
                } else {
                    s1 = "Server Light: (?? sky, ?? block)";
                }

                p_436067_.addToGroup(GROUP, List.of(s, s1));
            } else {
                p_436067_.addToGroup(GROUP, s);
            }
        }
    }
}
