package net.minecraft.client.gui.components.debug;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryPostEffect implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer p_434035_, @Nullable Level p_435454_, @Nullable LevelChunk p_433496_, @Nullable LevelChunk p_434900_) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation resourcelocation = minecraft.gameRenderer.currentPostEffect();
        if (resourcelocation != null) {
            p_434035_.addLine("Post: " + resourcelocation);
        }
    }
}
