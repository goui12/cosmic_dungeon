package net.goui.cosmicdungeon.achievement.d1;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.dungeon.d1.D1Members;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Called only after BellBlock.attemptToRing returns true, never from a generic block change. */
public final class SynchronousPealTracker {
    private SynchronousPealTracker() {}
    public static void successfulRing(ServerLevel level, BlockPos pos, Entity cause) {
        if (cause instanceof ServerPlayer player && AccessPolicy.isDeveloper(player)) return;
        if (!D1AchievementRegionService.inRegion(level, pos, D1AchievementRegionService.CAMP_5)) return;
        D1Members.run(level).ifPresent(run -> {
            var data = D1RunData.get(level.getServer());
            if (!D1InstanceAchievements.hasOnlineParticipant(level, run)
                    || data.values(run.runId(), "awarded").contains(CosmicAchievementIds.SYNCHRONOUS_PEAL.toString())) return;
            String bell = level.dimension().location() + ":" + pos.asLong();
            if (data.strikeBell(run.runId(), bell, level.getGameTime(), Config.BELL_WINDOW_TICKS.get()) >= Config.BELL_COUNT.get())
                D1InstanceAchievements.grant(level, run, CosmicAchievementIds.SYNCHRONOUS_PEAL);
        });
    }
}
