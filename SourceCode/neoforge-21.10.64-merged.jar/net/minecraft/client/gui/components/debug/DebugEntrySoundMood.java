package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntrySoundMood implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer p_435285_, @Nullable Level p_436001_, @Nullable LevelChunk p_432769_, @Nullable LevelChunk p_433840_) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            p_435285_.addLine(
                minecraft.getSoundManager().getDebugString()
                    + String.format(Locale.ROOT, " (Mood %d%%)", Math.round(minecraft.player.getCurrentMood() * 100.0F))
            );
        }
    }
}
