package net.minecraft.client.gui.components.debug;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryChunkRenderStats implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer p_435752_, @Nullable Level p_434336_, @Nullable LevelChunk p_435074_, @Nullable LevelChunk p_435647_) {
        String s = Minecraft.getInstance().levelRenderer.getSectionStatistics();
        if (s != null) {
            p_435752_.addLine(s);
        }
    }

    @Override
    public boolean isAllowed(boolean p_435265_) {
        return true;
    }
}
