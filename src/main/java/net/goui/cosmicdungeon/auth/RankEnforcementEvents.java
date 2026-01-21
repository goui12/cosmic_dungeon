package net.goui.cosmicdungeon.auth;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.concurrent.atomic.AtomicBoolean;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class RankEnforcementEvents {
    private RankEnforcementEvents() {}

    /**
     * Per-server-run flag: first real player to join gets DEVELOPER (dev environment only).
     * Resets on each server start.
     */
    private static final AtomicBoolean FIRST_JOIN_GRANTED = new AtomicBoolean(false);

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent e) {
        FIRST_JOIN_GRANTED.set(false);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (sp instanceof FakePlayer) return;

        final MinecraftServer server = sp.level().getServer();
        if (server == null) return;

        // DEV/IDE convenience: first joiner since restart becomes developer automatically.
        if (!FMLEnvironment.isProduction() && FIRST_JOIN_GRANTED.compareAndSet(false, true)) {
            RankStore store = RankStore.get(server);

            if (!store.getRank(sp.getUUID()).isDeveloper()) {
                store.setRank(sp.getUUID(), Rank.DEVELOPER);
            }

            // Apply OP immediately and tell them once (dev env only)
            OpUtil.setOperator(server, sp, true);
            sp.sendSystemMessage(Component.literal("[CosmicDungeon] Auto-promoted first joiner to DEVELOPER (dev environment).")
                    .withStyle(ChatFormatting.GOLD));
        }

        enforce(sp);
    }

    private static void enforce(ServerPlayer sp) {
        if (sp instanceof FakePlayer) return;

        final MinecraftServer server = sp.level().getServer();
        if (server == null) return;

        final boolean shouldBeOp = Authority.isDeveloper(sp);
        OpUtil.setOperator(server, sp, shouldBeOp);
    }
}
