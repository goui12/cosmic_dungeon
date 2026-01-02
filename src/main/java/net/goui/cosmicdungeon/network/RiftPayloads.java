package net.goui.cosmicdungeon.network;

import io.netty.buffer.ByteBuf;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class RiftPayloads {
    private RiftPayloads() {}

    private static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS_STREAM =
            ByteBufCodecs.VAR_LONG.map(BlockPos::of, BlockPos::asLong);

    /* ---------- S2C: open rift config (server-authorized) ---------- */
    public record S2C_OpenRiftConfig(BlockPos clickedTilePos) implements CustomPacketPayload {
        public static final Type<S2C_OpenRiftConfig> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "rift_open"));

        public static final StreamCodec<ByteBuf, S2C_OpenRiftConfig> STREAM_CODEC =
                StreamCodec.composite(
                        BLOCK_POS_STREAM, S2C_OpenRiftConfig::clickedTilePos,
                        S2C_OpenRiftConfig::new
                );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /* ---------- C2S: request config for clicked tile ---------- */
    public record C2S_RequestRiftConfig(BlockPos clickedTilePos) implements CustomPacketPayload {
        public static final Type<C2S_RequestRiftConfig> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "rift_request_config"));

        public static final StreamCodec<ByteBuf, C2S_RequestRiftConfig> STREAM_CODEC =
                StreamCodec.composite(
                        BLOCK_POS_STREAM, C2S_RequestRiftConfig::clickedTilePos,
                        C2S_RequestRiftConfig::new
                );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /* ---------- S2C: config response (authoritative) ---------- */
    public record S2C_RiftConfig(
            BlockPos clickedTilePos,
            BlockPos anchorPos,
            String riftName,
            String destinationName,
            List<String> allDestinations
    ) implements CustomPacketPayload {
        public static final Type<S2C_RiftConfig> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "rift_config"));

        public static final StreamCodec<ByteBuf, S2C_RiftConfig> STREAM_CODEC =
                StreamCodec.composite(
                        BLOCK_POS_STREAM, S2C_RiftConfig::clickedTilePos,
                        BLOCK_POS_STREAM, S2C_RiftConfig::anchorPos,
                        ByteBufCodecs.STRING_UTF8, S2C_RiftConfig::riftName,
                        ByteBufCodecs.STRING_UTF8, S2C_RiftConfig::destinationName,
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), S2C_RiftConfig::allDestinations,
                        S2C_RiftConfig::new
                );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /* ---------- C2S: save config ---------- */
    public record C2S_SaveRiftConfig(
            BlockPos anchorPos,
            String riftName,
            String destinationName
    ) implements CustomPacketPayload {
        public static final Type<C2S_SaveRiftConfig> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "rift_save"));

        public static final StreamCodec<ByteBuf, C2S_SaveRiftConfig> STREAM_CODEC =
                StreamCodec.composite(
                        BLOCK_POS_STREAM, C2S_SaveRiftConfig::anchorPos,
                        ByteBufCodecs.STRING_UTF8, C2S_SaveRiftConfig::riftName,
                        ByteBufCodecs.STRING_UTF8, C2S_SaveRiftConfig::destinationName,
                        C2S_SaveRiftConfig::new
                );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /* ---------- S2C: save result ---------- */
    public record S2C_SaveResult(
            BlockPos anchorPos,
            boolean ok,
            String message
    ) implements CustomPacketPayload {
        public static final Type<S2C_SaveResult> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "rift_save_result"));

        public static final StreamCodec<ByteBuf, S2C_SaveResult> STREAM_CODEC =
                StreamCodec.composite(
                        BLOCK_POS_STREAM, S2C_SaveResult::anchorPos,
                        ByteBufCodecs.BOOL, S2C_SaveResult::ok,
                        ByteBufCodecs.STRING_UTF8, S2C_SaveResult::message,
                        S2C_SaveResult::new
                );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
