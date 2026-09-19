package net.goui.cosmicdungeon.achievement.d1;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.dungeon.d1.D1Members;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;

/** Q&A D74: any seven distinct discs, actual playback, across this D1 instance. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class CycleOfRecordedSoundTracker {
    private CycleOfRecordedSoundTracker() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayback(VanillaGameEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getVanillaEvent().equals(GameEvent.JUKEBOX_PLAY)) return;
        BlockPos pos = BlockPos.containing(event.getEventPosition());
        if (!(level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox)
                || !jukebox.getSongPlayer().isPlaying() || jukebox.getTheItem().isEmpty()) return;
        D1Members.run(level).ifPresent(run -> {
            if (!D1InstanceAchievements.hasOnlineParticipant(level, run)) return;
            var data = D1RunData.get(level.getServer());
            if (data.values(run.runId(), "awarded").contains(CosmicAchievementIds.CYCLE_OF_RECORDED_SOUND.toString())) return;
            String disc = BuiltInRegistries.ITEM.getKey(jukebox.getTheItem().getItem()).toString();
            data.recordUnique(run.runId(), "music_discs", disc);
            if (data.values(run.runId(), "music_discs").size() >= Config.MUSIC_DISC_COUNT.get())
                D1InstanceAchievements.grant(level, run, CosmicAchievementIds.CYCLE_OF_RECORDED_SOUND);
        });
    }
}
