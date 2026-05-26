package net.goui.cosmicdungeon.achievement.d1;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.achievement.CosmicAdvancementUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Phantom;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class TiredNotBrokenTracker {
    private static final int THREE_NIGHTS_WITHOUT_REST_TICKS = 72000;

    private TiredNotBrokenTracker() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof Phantom)) return;
        if (sp.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.TIME_SINCE_REST)) < THREE_NIGHTS_WITHOUT_REST_TICKS) return;
        if (!D1AchievementRegionService.inRegion(sp.serverLevel(), sp.blockPosition(), D1AchievementRegionService.WOODLAND_MANOR)) return;
        CosmicAdvancementUtil.grant(sp, CosmicAchievementIds.TIRED_NOT_BROKEN);
    }
}
