// file: src/main/java/net/goui/cosmicdungeon/network/handler/RegionLookAllServerPayloadHandler.java
package net.goui.cosmicdungeon.network.handler;

import net.goui.cosmicdungeon.network.payload.RegionLookAllRequestPayload;
import net.goui.cosmicdungeon.region.RegionLookServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RegionLookAllServerPayloadHandler {
    private RegionLookAllServerPayloadHandler() {}

    public static void handle(final RegionLookAllRequestPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            RegionLookServer.refreshAllFor(player, payload);
        });
    }
}
