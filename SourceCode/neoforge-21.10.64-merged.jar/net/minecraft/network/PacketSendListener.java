package net.minecraft.network;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.util.function.Supplier;
import net.minecraft.network.protocol.Packet;
import org.slf4j.Logger;

public class PacketSendListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static ChannelFutureListener thenRun(Runnable action) {
        return p_428417_ -> {
            action.run();
            if (!p_428417_.isSuccess()) {
                p_428417_.channel().pipeline().fireExceptionCaught(p_428417_.cause());
            }
        };
    }

    public static ChannelFutureListener exceptionallySend(Supplier<Packet<?>> packetSupplier) {
        return p_428313_ -> {
            if (!p_428313_.isSuccess()) {
                Packet<?> packet = packetSupplier.get();
                if (packet != null) {
                    LOGGER.warn("Failed to deliver packet, sending fallback {}", packet.type(), p_428313_.cause());
                    p_428313_.channel().writeAndFlush(packet, p_428313_.channel().voidPromise());
                } else {
                    p_428313_.channel().pipeline().fireExceptionCaught(p_428313_.cause());
                }
            }
        };
    }
}
