package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryLocalDifficulty implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer p_433498_, @Nullable Level p_435699_, @Nullable LevelChunk p_435922_, @Nullable LevelChunk p_432782_) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        if (entity != null && minecraft.level != null && p_432782_ != null && p_435699_ != null) {
            BlockPos blockpos = entity.blockPosition();
            if (minecraft.level.isInsideBuildHeight(blockpos.getY())) {
                float f = p_435699_.getMoonBrightness();
                long i = p_432782_.getInhabitedTime();
                DifficultyInstance difficultyinstance = new DifficultyInstance(p_435699_.getDifficulty(), p_435699_.getDayTime(), i, f);
                p_433498_.addLine(
                    String.format(
                        Locale.ROOT,
                        "Local Difficulty: %.2f // %.2f (Day %d)",
                        difficultyinstance.getEffectiveDifficulty(),
                        difficultyinstance.getSpecialMultiplier(),
                        minecraft.level.getDayTime() / 24000L
                    )
                );
            }
        }
    }
}
