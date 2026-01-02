package net.goui.cosmicdungeon.auth;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class RankEnforcementEvents {
    private RankEnforcementEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        enforce(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        enforce(sp);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
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
