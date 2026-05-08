package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntrySimplePerformanceImpactors implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer p_435301_, @Nullable Level p_433686_, @Nullable LevelChunk p_433754_, @Nullable LevelChunk p_434594_) {
        Minecraft minecraft = Minecraft.getInstance();
        Options options = minecraft.options;
        p_435301_.addLine(
            String.format(
                Locale.ROOT,
                "%s%s B: %d",
                options.graphicsMode().get(),
                options.cloudStatus().get() == CloudStatus.OFF ? "" : (options.cloudStatus().get() == CloudStatus.FAST ? " fast-clouds" : " fancy-clouds"),
                options.biomeBlendRadius().get()
            )
        );
    }

    @Override
    public boolean isAllowed(boolean p_434942_) {
        return true;
    }
}
