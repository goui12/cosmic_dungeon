package net.goui.cosmicdungeon.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class TradePayloads {
    private TradePayloads() {}

    private static final StreamCodec<ByteBuf, UUID> UUID_STREAM_CODEC = StreamCodec.of(
            (buf, uuid) -> {
                buf.writeLong(uuid.getMostSignificantBits());
                buf.writeLong(uuid.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong())
    );
    public record C2S_RequestTrade(String targetName) implements CustomPacketPayload {
        public static final Type<C2S_RequestTrade> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "trade_request"));
        public static final StreamCodec<ByteBuf, C2S_RequestTrade> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, C2S_RequestTrade::targetName, C2S_RequestTrade::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record C2S_RequestLookTrade(UUID targetPlayerId) implements CustomPacketPayload {
        public static final Type<C2S_RequestLookTrade> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "trade_request_look"));
        public static final StreamCodec<ByteBuf, C2S_RequestLookTrade> STREAM_CODEC = StreamCodec.composite(UUID_STREAM_CODEC, C2S_RequestLookTrade::targetPlayerId, C2S_RequestLookTrade::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record C2S_AcceptTrade(String inviterName) implements CustomPacketPayload {
        public static final Type<C2S_AcceptTrade> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "trade_accept"));
        public static final StreamCodec<ByteBuf, C2S_AcceptTrade> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, C2S_AcceptTrade::inviterName, C2S_AcceptTrade::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record C2S_UpdateCurrencyOffer(long traceAmount) implements CustomPacketPayload {
        public static final Type<C2S_UpdateCurrencyOffer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "trade_currency_offer"));
        public static final StreamCodec<ByteBuf, C2S_UpdateCurrencyOffer> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, C2S_UpdateCurrencyOffer::traceAmount, C2S_UpdateCurrencyOffer::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record C2S_AdjustCurrencyOffer(String denominationId, int deltaCount) implements CustomPacketPayload {
        public static final Type<C2S_AdjustCurrencyOffer> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "trade_currency_adjust"));
        public static final StreamCodec<ByteBuf, C2S_AdjustCurrencyOffer> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, C2S_AdjustCurrencyOffer::denominationId,
                ByteBufCodecs.VAR_INT, C2S_AdjustCurrencyOffer::deltaCount,
                C2S_AdjustCurrencyOffer::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record C2S_Ready(boolean ready) implements CustomPacketPayload { public static final Type<C2S_Ready> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "trade_ready")); public static final StreamCodec<ByteBuf, C2S_Ready> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, C2S_Ready::ready, C2S_Ready::new); @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}}
    public record C2S_Confirm(boolean confirm) implements CustomPacketPayload { public static final Type<C2S_Confirm> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "trade_confirm")); public static final StreamCodec<ByteBuf, C2S_Confirm> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, C2S_Confirm::confirm, C2S_Confirm::new); @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}}
    public record C2S_Cancel() implements CustomPacketPayload { public static final Type<C2S_Cancel> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "trade_cancel")); public static final StreamCodec<ByteBuf, C2S_Cancel> STREAM_CODEC = StreamCodec.unit(new C2S_Cancel()); @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}}
    public record S2C_TradePromptState(boolean hidePrompt) implements CustomPacketPayload { public static final Type<S2C_TradePromptState> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "trade_prompt_state")); public static final StreamCodec<ByteBuf, S2C_TradePromptState> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, S2C_TradePromptState::hidePrompt, S2C_TradePromptState::new); @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}}
    public record S2C_TradeState(
            int containerId,
            UUID sessionId,
            String selfName,
            String otherName,
            long selfBalanceTrace,
            long otherBalanceTrace,
            long selfOfferedTrace,
            long otherOfferedTrace,
            boolean selfReady,
            boolean otherReady,
            boolean selfConfirmed,
            boolean otherConfirmed,
            String statusMessage
    ) implements CustomPacketPayload {
        public static final Type<S2C_TradeState> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "trade_state"));
        public static final StreamCodec<ByteBuf, S2C_TradeState> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    ByteBufCodecs.VAR_INT.encode(buf, payload.containerId());
                    UUID_STREAM_CODEC.encode(buf, payload.sessionId());
                    ByteBufCodecs.STRING_UTF8.encode(buf, payload.selfName());
                    ByteBufCodecs.STRING_UTF8.encode(buf, payload.otherName());
                    ByteBufCodecs.VAR_LONG.encode(buf, payload.selfBalanceTrace());
                    ByteBufCodecs.VAR_LONG.encode(buf, payload.otherBalanceTrace());
                    ByteBufCodecs.VAR_LONG.encode(buf, payload.selfOfferedTrace());
                    ByteBufCodecs.VAR_LONG.encode(buf, payload.otherOfferedTrace());
                    ByteBufCodecs.BOOL.encode(buf, payload.selfReady());
                    ByteBufCodecs.BOOL.encode(buf, payload.otherReady());
                    ByteBufCodecs.BOOL.encode(buf, payload.selfConfirmed());
                    ByteBufCodecs.BOOL.encode(buf, payload.otherConfirmed());
                    ByteBufCodecs.STRING_UTF8.encode(buf, payload.statusMessage());
                },
                buf -> new S2C_TradeState(
                        ByteBufCodecs.VAR_INT.decode(buf),
                        UUID_STREAM_CODEC.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.VAR_LONG.decode(buf),
                        ByteBufCodecs.VAR_LONG.decode(buf),
                        ByteBufCodecs.VAR_LONG.decode(buf),
                        ByteBufCodecs.VAR_LONG.decode(buf),
                        ByteBufCodecs.BOOL.decode(buf),
                        ByteBufCodecs.BOOL.decode(buf),
                        ByteBufCodecs.BOOL.decode(buf),
                        ByteBufCodecs.BOOL.decode(buf),
                        ByteBufCodecs.STRING_UTF8.decode(buf)
                )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

}
