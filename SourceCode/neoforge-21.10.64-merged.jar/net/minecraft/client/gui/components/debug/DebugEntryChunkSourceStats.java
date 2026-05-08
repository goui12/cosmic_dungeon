package net.minecraft.client.gui.components.debug;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryChunkSourceStats implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer p_434133_, @Nullable Level p_433954_, @Nullable LevelChunk p_435295_, @Nullable LevelChunk p_432927_) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            p_434133_.addLine(minecraft.level.gatherChunkSourceStats());
        }

        if (p_433954_ != null && p_433954_ != minecraft.level) {
            p_434133_.addLine(p_433954_.gatherChunkSourceStats());
        }
    }

    @Override
    public boolean isAllowed(boolean p_433582_) {
        return true;
    }
}
