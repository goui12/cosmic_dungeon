package net.minecraft.client.gui.components.debug;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryGpuUtilization implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer p_435517_, @Nullable Level p_435988_, @Nullable LevelChunk p_434838_, @Nullable LevelChunk p_435176_) {
        Minecraft minecraft = Minecraft.getInstance();
        String s = "GPU: " + (minecraft.getGpuUtilization() > 100.0 ? ChatFormatting.RED + "100%" : Math.round(minecraft.getGpuUtilization()) + "%");
        p_435517_.addLine(s);
    }

    @Override
    public boolean isAllowed(boolean p_432925_) {
        return true;
    }
}
