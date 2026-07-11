package net.goui.cosmicdungeon.achievement;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.economy.CurrencyService;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class WelcomeCurrencyAchievementEvents {
    private static final long WELCOME_REWARD_TRACE = 5L;

    private WelcomeCurrencyAchievementEvents() {}

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player instanceof FakePlayer) return;
        if (player.level().isClientSide()) return;

        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        AdvancementHolder holder = server.getAdvancements().get(CosmicAchievementIds.FIRST_TRACE);
        if (holder == null) return;
        if (player.getAdvancements().getOrStartProgress(holder).isDone()) return;

        CurrencyService.tryDeposit(player, WELCOME_REWARD_TRACE);
        CosmicAdvancementUtil.grant(player, CosmicAchievementIds.FIRST_TRACE);
        player.sendSystemMessage(Component.literal("First Trace reward: ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("5 Trace").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal(" received. Check achievements to learn how Trace is used.")
                        .withStyle(ChatFormatting.AQUA)));
    }
}
