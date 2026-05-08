package net.minecraft.client.gui.components.debug;

import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
class DebugEntryVersion implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer p_433743_, @Nullable Level p_435374_, @Nullable LevelChunk p_433899_, @Nullable LevelChunk p_434851_) {
        p_433743_.addPriorityLine(
            "Minecraft "
                + SharedConstants.getCurrentVersion().name()
                + " ("
                + Minecraft.getInstance().getLaunchedVersion()
                + "/"
                + ClientBrandRetriever.getClientModName()
                + ")"
        );
    }

    @Override
    public boolean isAllowed(boolean p_435274_) {
        return true;
    }
}
