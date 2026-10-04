package net.goui.cosmicdungeon.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Retain native delay events and weighted SpawnPotentials selection. */
@Mixin(BaseSpawner.class)
public interface BaseSpawnerAccess {
    @Invoker("delay") void cosmicdungeon$delay(Level level, BlockPos pos);
}
