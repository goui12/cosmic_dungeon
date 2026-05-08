package net.minecraft.client.gui.components.debug;

import javax.annotation.Nullable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryNoop implements DebugScreenEntry {
    private final boolean isAllowedWithReducedDebugInfo;

    public DebugEntryNoop() {
        this(false);
    }

    public DebugEntryNoop(boolean isAllowedWithReducedDebugInfo) {
        this.isAllowedWithReducedDebugInfo = isAllowedWithReducedDebugInfo;
    }

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level level, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
    }

    @Override
    public boolean isAllowed(boolean reducedDebugInfo) {
        return this.isAllowedWithReducedDebugInfo || !reducedDebugInfo;
    }

    @Override
    public DebugEntryCategory category() {
        return DebugEntryCategory.RENDERER;
    }
}
