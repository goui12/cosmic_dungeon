package net.goui.cosmicdungeon.achievement.d1;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.achievement.AchievementCounterData;
import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.achievement.CosmicAdvancementUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class SynchronousPealTracker {
    private static final int WINDOW_TICKS = 60;

    private SynchronousPealTracker() {}

    @SubscribeEvent
    public static void onBellUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;
        if (!D1AchievementRegionService.inRegion(sp.level(), event.getPos(), D1AchievementRegionService.CAMP_5)) return;
        BlockState state = sp.level().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BellBlock)) return;

        AchievementCounterData data = AchievementCounterData.get(sp.level().getServer());
        var rec = data.get(sp.getUUID());
        int now = (int) sp.level().getGameTime();
        int start = rec.genericCounter2();
        int hits = rec.genericCounter1();
        if (start <= 0 || now - start > WINDOW_TICKS) {
            start = now;
            hits = 0;
        }
        hits++;
        data.set(new AchievementCounterData.CounterRecord(rec.playerId(), rec.bindingIdolReturns(), rec.bindingIdolProvided(), rec.vitalExchangeMask(), rec.d1MusicDiscMask(), hits, start));
        if (hits >= 6) CosmicAdvancementUtil.grant(sp, CosmicAchievementIds.SYNCHRONOUS_PEAL);
    }
}
