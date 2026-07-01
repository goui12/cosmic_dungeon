package net.goui.cosmicdungeon.achievement;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class TradeAchievementEvents {
    private TradeAchievementEvents() {}

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player instanceof FakePlayer) return;
        if (player.level().isClientSide()) return;
        TradeAchievementService.syncPromptState(player);
    }
}
