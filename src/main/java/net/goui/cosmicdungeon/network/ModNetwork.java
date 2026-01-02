package net.goui.cosmicdungeon.network;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Centralized payload registration + small helpers.
 * Server-first style: screens request authoritative state from server.
 */
public final class ModNetwork {
    private ModNetwork() {}

    // wired from CosmicDungeonMod constructor with modEventBus.addListener(ModNetwork::registerPayloadHandlers)
    public static void registerPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // existing
        registrar.playToClient(
                ShakeScreenPayload.TYPE,
                ShakeScreenPayload.STREAM_CODEC
        );

        /* ===================== RIFT (SERVER-AUTHORITATIVE) ===================== */

        registrar.playToServer(
                RiftPayloads.C2S_RequestRiftConfig.TYPE,
                RiftPayloads.C2S_RequestRiftConfig.STREAM_CODEC,
                (payload, ctx) -> {
                    if (!(ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
                    if (!(sp.level() instanceof net.minecraft.server.level.ServerLevel level)) return;

                    var data = net.goui.cosmicdungeon.rift.RiftRegistryData.get(level);

                    var clicked = payload.clickedTilePos();
                    var anchorOpt = data.getAnchorForTile(clicked);

                    if (anchorOpt.isEmpty()) {
                        // Not registered / not part of a known portal
                        ctx.reply(new RiftPayloads.S2C_RiftConfig(
                                clicked,
                                clicked,
                                "",
                                "",
                                data.listDestinationNamesSorted()
                        ));
                        return;
                    }

                    var anchor = net.minecraft.core.BlockPos.of(anchorOpt.getAsLong());
                    var portal = data.getPortal(anchor.asLong()).orElse(null);

                    String name = portal == null ? "" : portal.portalName();
                    String dest = portal == null ? "" : portal.destinationName();

                    ctx.reply(new RiftPayloads.S2C_RiftConfig(
                            clicked,
                            anchor,
                            name == null ? "" : name,
                            dest == null ? "" : dest,
                            data.listDestinationNamesSorted()
                    ));
                }
        );

        registrar.playToServer(
                RiftPayloads.C2S_SaveRiftConfig.TYPE,
                RiftPayloads.C2S_SaveRiftConfig.STREAM_CODEC,
                (payload, ctx) -> {
                    if (!(ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
                    if (!(sp.level() instanceof net.minecraft.server.level.ServerLevel level)) return;

                    var data = net.goui.cosmicdungeon.rift.RiftRegistryData.get(level);

                    // basic sanity / proximity check (professional default)
                    var anchor = payload.anchorPos();
                    if (sp.blockPosition().distManhattan(anchor) > 16) {
                        ctx.reply(new RiftPayloads.S2C_SaveResult(anchor, false, "Too far from rift to configure."));
                        return;
                    }

                    var result = data.setPortalConfig(anchor, payload.riftName(), payload.destinationName());

                    if (result instanceof net.goui.cosmicdungeon.rift.RiftRegistryData.SaveResult.Ok) {
                        ctx.reply(new RiftPayloads.S2C_SaveResult(anchor, true, "Rift saved."));
                    } else if (result instanceof net.goui.cosmicdungeon.rift.RiftRegistryData.SaveResult.BadDestination bd) {
                        ctx.reply(new RiftPayloads.S2C_SaveResult(anchor, false, "Unknown destination: " + bd.name()));
                    } else {
                        ctx.reply(new RiftPayloads.S2C_SaveResult(anchor, false, "Rift not found."));
                    }
                }
        );

        registrar.playToClient(
                RiftPayloads.S2C_RiftConfig.TYPE,
                RiftPayloads.S2C_RiftConfig.STREAM_CODEC,
                (payload, ctx) -> net.minecraft.client.Minecraft.getInstance().execute(() ->
                        net.goui.cosmicdungeon.client.rift.RiftConfigScreen.onServerConfig(payload)
                )
        );

        registrar.playToClient(
                RiftPayloads.S2C_SaveResult.TYPE,
                RiftPayloads.S2C_SaveResult.STREAM_CODEC,
                (payload, ctx) -> net.minecraft.client.Minecraft.getInstance().execute(() ->
                        net.goui.cosmicdungeon.client.rift.RiftConfigScreen.onServerSaveResult(payload)
                )
        );
    }

    /* ===================== CLIENT SEND HELPERS ===================== */

    /**
     * Client-only convenience: send one payload to server.
     * Uses NeoForge's ClientPacketDistributor so you don't need to wrap packets manually.
     */
    public static void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    /**
     * Overload for batching (optional).
     */
    public static void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload,
                                    net.minecraft.network.protocol.common.custom.CustomPacketPayload... more) {
        ClientPacketDistributor.sendToServer(payload, more);
    }
}
