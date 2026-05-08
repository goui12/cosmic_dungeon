package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntrySectionPosition implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer p_434471_, @Nullable Level p_434018_, @Nullable LevelChunk p_432887_, @Nullable LevelChunk p_433613_) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        if (entity != null) {
            BlockPos blockpos = minecraft.getCameraEntity().blockPosition();
            p_434471_.addToGroup(
                DebugEntryPosition.GROUP,
                String.format(Locale.ROOT, "Section-relative: %02d %02d %02d", blockpos.getX() & 15, blockpos.getY() & 15, blockpos.getZ() & 15)
            );
        }
    }

    @Override
    public boolean isAllowed(boolean p_435675_) {
        return true;
    }
}
