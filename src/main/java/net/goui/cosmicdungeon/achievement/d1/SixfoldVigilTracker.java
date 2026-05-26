package net.goui.cosmicdungeon.achievement.d1;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.achievement.CosmicAdvancementUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class SixfoldVigilTracker {
    private SixfoldVigilTracker() {}

    @SubscribeEvent
    public static void onBlockChanged(BlockEvent.BlockToolModificationEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;
        check(sp.serverLevel(), sp, event.getPos());
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        check(sp.serverLevel(), sp, event.getPos());
    }

    private static void check(ServerLevel level, ServerPlayer trigger, BlockPos origin) {
        if (!D1AchievementRegionService.inRegion(level, origin, D1AchievementRegionService.WITHER_ROOM)) return;
        int litCandlesOnChiseledTuff = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-16, -8, -16), origin.offset(16, 8, 16))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.CANDLES)) continue;
            if (!(state.getBlock() instanceof AbstractCandleBlock) || !state.getValue(AbstractCandleBlock.LIT)) continue;
            if (!level.getBlockState(pos.below()).is(Blocks.CHISELED_TUFF)) continue;
            litCandlesOnChiseledTuff++;
        }
        if (litCandlesOnChiseledTuff < 6) return;

        int withers = level.getEntitiesOfClass(WitherBoss.class, trigger.getBoundingBox().inflate(64.0), w -> D1AchievementRegionService.inRegion(level, w.blockPosition(), D1AchievementRegionService.WITHER_ROOM)).size();
        CosmicAdvancementUtil.grant(trigger, CosmicAchievementIds.SIXFOLD_VIGIL);
        if (withers <= 0) CosmicAdvancementUtil.grant(trigger, CosmicAchievementIds.SIXFOLD_VIGIL_AFTER_DISSOLUTION);
        else if (withers == 1) CosmicAdvancementUtil.grant(trigger, CosmicAchievementIds.SIXFOLD_VIGIL_LONE_ADVERSARY);
        else CosmicAdvancementUtil.grant(trigger, CosmicAchievementIds.SIXFOLD_VIGIL_TWIN_MANIFESTATION);
    }
}
