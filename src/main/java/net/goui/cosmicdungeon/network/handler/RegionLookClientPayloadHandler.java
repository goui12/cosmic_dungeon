package net.goui.cosmicdungeon.network.handler;

import net.goui.cosmicdungeon.network.ClientNetworkDispatch;
import net.goui.cosmicdungeon.network.payload.RegionLookPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RegionLookClientPayloadHandler {
    private RegionLookClientPayloadHandler() {}

    public static void handle(final RegionLookPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientNetworkDispatch.dispatch("onRegionLook", payload));
    }
}
