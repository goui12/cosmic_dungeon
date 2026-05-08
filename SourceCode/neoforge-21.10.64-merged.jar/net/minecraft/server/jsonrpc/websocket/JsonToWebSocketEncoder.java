package net.minecraft.server.jsonrpc.websocket;

import com.google.gson.JsonElement;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.List;

public class JsonToWebSocketEncoder extends MessageToMessageEncoder<JsonElement> {
    protected void encode(ChannelHandlerContext p_442993_, JsonElement p_443520_, List<Object> p_443581_) {
        p_443581_.add(new TextWebSocketFrame(p_443520_.toString()));
    }
}
