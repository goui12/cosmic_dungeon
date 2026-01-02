// file: src/main/java/net/goui/cosmicdungeon/network/ShakeScreenPayload.java
package net.goui.cosmicdungeon.network;

import io.netty.buffer.ByteBuf;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ShakeScreenPayload() implements CustomPacketPayload {
    public static final Type<ShakeScreenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "shake_screen")
    );

    // No data -> unit codec
    public static final StreamCodec<ByteBuf, ShakeScreenPayload> STREAM_CODEC =
            StreamCodec.unit(new ShakeScreenPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
