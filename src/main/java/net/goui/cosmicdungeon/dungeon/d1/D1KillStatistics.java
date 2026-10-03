package net.goui.cosmicdungeon.dungeon.d1;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Q&A D20: successful-run statistics persist; failed/exited run statistics do not.
 * Counts attributed hostile deaths, including a member's companions and subsequent environmental damage.
 * Lifetime leaderboard retention is a separate pending testing batch.
 * No entity scan, currency payout, or lifetime update occurs on this event.
 */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class D1KillStatistics {
    private static final String RECORDED_RUN = "cosmicdungeon.d1_kill_recorded_run";
    private D1KillStatistics() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        var victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || !(victim instanceof Enemy)) return;
        var credited = net.goui.cosmicdungeon.dungeon.DungeonKillCredit.resolve(victim, event.getSource());
        if (credited == null) return;
        ServerPlayer killer = level.getServer().getPlayerList().getPlayer(credited);
        var run = D1Members.run(level).filter(r -> r.containsPlayer(credited)).orElse(null);
        if (run == null || victim.getPersistentData().getLongOr(RECORDED_RUN, 0) == run.runId()) return;
        victim.getPersistentData().putLong(RECORDED_RUN, run.runId());
        var data = D1RunData.get(level.getServer());
        int count = data.recordKill(run.runId(), credited);
        if (killer != null && killer.level() == level) D1Scoreboards.runKills(killer, count);
    }
}
