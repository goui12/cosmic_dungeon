package net.minecraft.server.jsonrpc.websocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.List;

public class WebSocketToJsonCodec extends MessageToMessageDecoder<TextWebSocketFrame> {
    protected void decode(ChannelHandlerContext p_442809_, TextWebSocketFrame p_442817_, List<Object> p_442912_) {
        JsonElement jsonelement = JsonParser.parseString(p_442817_.text());
        p_442912_.add(jsonelement);
    }
}
