package net.minecraft.client.gui.components.debug;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryParticleRenderStats implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer p_434362_, @Nullable Level p_433470_, @Nullable LevelChunk p_433698_, @Nullable LevelChunk p_433846_) {
        p_434362_.addLine("P: " + Minecraft.getInstance().particleEngine.countParticles());
    }
}
