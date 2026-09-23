package net.goui.cosmicdungeon.dungeon.d1;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** Run counters and lifetime counters have different names and different reset paths. */
public final class D1Scoreboards {
    private D1Scoreboards() {}
    private static void score(ServerPlayer player, String key, String title, long value) {
        var board = player.level().getServer().getScoreboard();
        Objective objective = board.getObjective(key);
        if (objective == null) objective = board.addObjective(key, ObjectiveCriteria.DUMMY,
                Component.literal(title), ObjectiveCriteria.RenderType.INTEGER, false, null);
        board.getOrCreatePlayerScore(player, objective).set((int) Math.min(Integer.MAX_VALUE, Math.max(0, value)));
    }
    public static void update(ServerPlayer player, int blooms, int lesser) {
        score(player, "d1_blooms", "D1 Blooms this run", blooms);
        score(player, "d1_lesser", "D1 Lesser Blooms this run", lesser);
        int kills = D1Members.run(player.level()).map(run -> D1RunData.get(player.level().getServer())
                .count(run.runId(), "kills:" + player.getUUID())).orElse(0);
        runKills(player, kills);
        lifetime(player);
    }
    public static void runKills(ServerPlayer player, int kills) {
        score(player, "d1_kills", "D1 hostile kills this run", kills);
    }
    public static void lifetime(ServerPlayer player) {
        var totals = D1LifetimeData.get(player.level().getServer()).totals(player.getUUID());
        score(player, "d1_blooms_total", "Lifetime recovered Blooms", totals.spectralBlooms());
        score(player, "d1_lesser_total", "Lifetime Lesser Blooms", totals.lesserBlooms());
        score(player, "d1_completions", "D1 completions", totals.completions());
        score(player, "d1_kills_total", "D1 kills in successful runs", totals.successfulKills());
    }
    public static void resetRun(ServerPlayer player) { update(player, 0, 0); runKills(player, 0); }
}
