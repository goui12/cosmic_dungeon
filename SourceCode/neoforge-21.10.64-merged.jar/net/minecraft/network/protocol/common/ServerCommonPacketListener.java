package net.minecraft.network.protocol.common;

import net.minecraft.network.protocol.cookie.ServerCookiePacketListener;

public interface ServerCommonPacketListener extends ServerCookiePacketListener, net.neoforged.neoforge.common.extensions.IServerCommonPacketListenerExtension {
    void handleKeepAlive(ServerboundKeepAlivePacket packet);

    void handlePong(ServerboundPongPacket packet);

    void handleCustomPayload(ServerboundCustomPayloadPacket packet);

    void handleResourcePackResponse(ServerboundResourcePackPacket packet);

    void handleClientInformation(ServerboundClientInformationPacket packet);

    void handleCustomClickAction(ServerboundCustomClickActionPacket packet);
}
