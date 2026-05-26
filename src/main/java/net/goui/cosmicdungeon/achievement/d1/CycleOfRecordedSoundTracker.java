package net.goui.cosmicdungeon.achievement.d1;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.achievement.AchievementCounterData;
import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.achievement.CosmicAdvancementUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class CycleOfRecordedSoundTracker {
    private CycleOfRecordedSoundTracker() {}

    @SubscribeEvent
    public static void onJukeboxUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;
        if (!D1AchievementRegionService.inRegion(sp.serverLevel(), event.getPos(), D1AchievementRegionService.WOODLAND_MANOR)) return;

        BlockState state = sp.serverLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof JukeboxBlock)) return;
        var held = sp.getItemInHand(event.getHand());
        int bit = discBit(held.getItem());
        if (bit < 0) return;

        AchievementCounterData data = AchievementCounterData.get(sp.level().getServer());
        var rec = data.get(sp.getUUID());
        int mask = rec.d1MusicDiscMask() | (1 << bit);
        data.set(new AchievementCounterData.CounterRecord(rec.playerId(), rec.bindingIdolReturns(), rec.bindingIdolProvided(), rec.vitalExchangeMask(), mask, rec.genericCounter1(), rec.genericCounter2()));
        if (mask == 0b1111111) CosmicAdvancementUtil.grant(sp, CosmicAchievementIds.CYCLE_OF_RECORDED_SOUND);
    }

    private static int discBit(net.minecraft.world.item.Item item) {
        if (item == Items.MUSIC_DISC_13) return 0;
        if (item == Items.MUSIC_DISC_CAT) return 1;
        if (item == Items.MUSIC_DISC_BLOCKS) return 2;
        if (item == Items.MUSIC_DISC_CHIRP) return 3;
        if (item == Items.MUSIC_DISC_FAR) return 4;
        if (item == Items.MUSIC_DISC_MALL) return 5;
        if (item == Items.MUSIC_DISC_MELLOHI) return 6;
        return -1;
    }
}
