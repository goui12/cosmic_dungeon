package net.minecraft.client.gui.components.debug;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryEntityRenderStats implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer p_435311_, @Nullable Level p_435099_, @Nullable LevelChunk p_435108_, @Nullable LevelChunk p_434070_) {
        String s = Minecraft.getInstance().levelRenderer.getEntityStatistics();
        if (s != null) {
            p_435311_.addLine(s);
        }
    }

    @Override
    public boolean isAllowed(boolean p_434353_) {
        return true;
    }
}
