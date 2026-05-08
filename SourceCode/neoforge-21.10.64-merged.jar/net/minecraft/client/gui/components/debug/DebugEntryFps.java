package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryFps implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer p_434057_, @Nullable Level p_433966_, @Nullable LevelChunk p_435484_, @Nullable LevelChunk p_435613_) {
        Minecraft minecraft = Minecraft.getInstance();
        int i = minecraft.getFramerateLimitTracker().getFramerateLimit();
        Options options = minecraft.options;
        p_434057_.addPriorityLine(
            String.format(Locale.ROOT, "%d fps T: %s%s", minecraft.getFps(), i == 260 ? "inf" : i, options.enableVsync().get() ? " vsync" : "")
        );
    }

    @Override
    public boolean isAllowed(boolean p_434350_) {
        return true;
    }
}
