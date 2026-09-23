package net.goui.cosmicdungeon.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public final class TamsinTaxPayloads {
    private TamsinTaxPayloads() {}
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("cosmicdungeon", path); }
    public record Action(int containerId, String action, int slot, String token) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(id("tamsin_tax_action"));
        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::containerId, ByteBufCodecs.stringUtf8(12), Action::action,
                ByteBufCodecs.VAR_INT, Action::slot, ByteBufCodecs.stringUtf8(36), Action::token, Action::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Choice(int slot, String name, int count) {
        public static final StreamCodec<ByteBuf, Choice> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Choice::slot, ByteBufCodecs.stringUtf8(80), Choice::name,
                ByteBufCodecs.VAR_INT, Choice::count, Choice::new);
    }
    public record View(int containerId, List<Choice> choices, String token, String selected) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(id("tamsin_tax_view"));
        public static final StreamCodec<ByteBuf, View> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, View::containerId, Choice.CODEC.apply(ByteBufCodecs.list(64)), View::choices,
                ByteBufCodecs.stringUtf8(36), View::token, ByteBufCodecs.stringUtf8(80), View::selected, View::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
