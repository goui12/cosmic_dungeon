package net.goui.cosmicdungeon.network.handler;

import net.goui.cosmicdungeon.network.ClientNetworkDispatch;
import net.goui.cosmicdungeon.network.payload.RegionLookAllPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RegionLookAllClientPayloadHandler {
    private RegionLookAllClientPayloadHandler() {}

    public static void handle(final RegionLookAllPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientNetworkDispatch.dispatch("onRegionLookAll", payload));
    }
}
