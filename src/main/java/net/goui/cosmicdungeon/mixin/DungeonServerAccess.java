package net.goui.cosmicdungeon.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.concurrent.Executor;

/** Native server dependencies needed to construct and retire isolated dungeon levels. */
@Mixin(MinecraftServer.class)
public interface DungeonServerAccess {
    @Accessor("storageSource") LevelStorageSource.LevelStorageAccess cosmicdungeon$storage();
    @Accessor("executor") Executor cosmicdungeon$executor();
    @Accessor("perWorldTickTimes") Map<ResourceKey<Level>, long[]> cosmicdungeon$tickTimes();
}
