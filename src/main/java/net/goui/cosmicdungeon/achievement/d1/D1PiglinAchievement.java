package net.goui.cosmicdungeon.achievement.d1;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.dungeon.DungeonTravelRouter;
import net.goui.cosmicdungeon.dungeon.d1.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class D1PiglinAchievement {
    private static long sampledTick = Long.MIN_VALUE;
    private static final Set<Long> SAMPLED_RUNS = new HashSet<>();
    private D1PiglinAchievement() {}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getServer().overworld().getGameTime();
        if (now % Config.PIGLIN_POLL_TICKS.get() != 0) return;
        if (sampledTick != now) { sampledTick = now; SAMPLED_RUNS.clear(); }
        var run = D1Members.run(player.level()).orElse(null);
        if (run == null || !DungeonTravelRouter.canTravelInside(player, run) || !SAMPLED_RUNS.add(run.runId())) return;
        var id = CosmicAchievementIds.WOLVES_IN_PIGLIN_CLOTHING;
        if (D1RunData.get(player.level().getServer()).values(run.runId(), "awarded").contains(id.toString())) return;
        var sample = new ArrayList<PiglinDisguiseRules.Character>();
        for (var owner : run.orderedPlayers()) {
            var member = player.level().getServer().getPlayerList().getPlayer(owner);
            if (member == null) continue;
            sample.add(new PiglinDisguiseRules.Character(owner, DungeonTravelRouter.canTravelInside(member, run),
                    D1AchievementRegionService.inRegion(member.level(), member.blockPosition(), D1AchievementRegionService.CAMP_4),
                    member.getItemBySlot(EquipmentSlot.HEAD).is(Items.PIGLIN_HEAD)));
        }
        if (PiglinDisguiseRules.qualifies(sample, Config.PIGLIN_HEAD_COUNT.get()))
            D1InstanceAchievements.grant(player.level(), run, id);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { sampledTick = Long.MIN_VALUE; SAMPLED_RUNS.clear(); }
    // TODO(M79/M81, authored TEST binding): MASTER Achievements!B20/C20 locates Camp 4
    // at navigation coordinate 630 22 68 and explicitly requires six characters in Piglin Heads.
    // Bind region d1_camp_4 to the actual camp bounds in the D1 template; the coordinate is
    // not a cuboid/radius specification. No guessed bounds or placed block edits.
    // Verify six simultaneous players, two instances, head removal, absent/oversized region,
    // developer/spectator/dead/outside escrow exclusions, reset and earned-credit replay.
}
