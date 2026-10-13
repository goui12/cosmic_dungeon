package net.goui.cosmicdungeon.block.entity;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Context is established after chunk admission. Never upgrades instance/snapshot files or forces chunks. */
public final class SpawnerDevelopmentMigration {
    private static final Map<MinecraftServer,Long> NEXT_NOTICE=new WeakHashMap<>();
    private SpawnerDevelopmentMigration(){}
    public static boolean developmentWorld(ServerLevel level){
        if(!level.dimension().equals(Level.OVERWORLD)&&DungeonDefinitions.byDimension(level.dimension()).isEmpty())return false;
        // PREPARING and PAUSED are instance ownership too; the gameplay active-run query omits them.
        return DungeonRunRegistryData.get(level.getServer()).listAllRuns().stream()
                .noneMatch(run->run.containsDimension(level.dimension()));
    }
    public static void loaded(CosmicSpawnerBlockEntity spawner){
        if(!(spawner.getLevel() instanceof ServerLevel level)||!spawner.needsDevelopmentUpgrade())return;
        if(developmentWorld(level)){
            long start=System.nanoTime();
            spawner.upgradeDevelopmentData();
            DungeonStartupTiming.record(DungeonStartupTiming.Phase.MIGRATION,start,1);
            return;
        }
        long now=System.nanoTime();
        if(now<NEXT_NOTICE.getOrDefault(level.getServer(),Long.MIN_VALUE))return;
        NEXT_NOTICE.put(level.getServer(),now+600_000_000_000L);
        com.mojang.logging.LogUtils.getLogger().warn(
                "[SpawnerMigration] Legacy spawner at {} in {} is read compatibly, without instance migration. "
                +"Developer: visit the corresponding development-main-world chunks, let their authentic upgrades finish, "
                +"then explicitly /world save dungeon_1 (or the matching dungeon). Existing snapshots are never saved automatically.",
                spawner.getBlockPos(),level.dimension().location());
    }
}
