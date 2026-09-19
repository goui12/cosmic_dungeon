package net.goui.cosmicdungeon.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public final class PartyPayloads {
    private PartyPayloads() {}
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("cosmicdungeon", path); }
    public record Action(int containerId, long revision, String action, String target) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(id("d1_party_action"));
        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::containerId, ByteBufCodecs.VAR_LONG, Action::revision,
                ByteBufCodecs.stringUtf8(16), Action::action, ByteBufCodecs.stringUtf8(36), Action::target, Action::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Member(String name, String classId, boolean ready, boolean leader) {
        public static final StreamCodec<ByteBuf, Member> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(16), Member::name, ByteBufCodecs.stringUtf8(32), Member::classId,
                ByteBufCodecs.BOOL, Member::ready, ByteBufCodecs.BOOL, Member::leader, Member::new);
    }
    public record State(long revision, String phase, boolean leader, int capacity, int queuePosition, int countdownSeconds) {
        public static final StreamCodec<ByteBuf, State> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, State::revision, ByteBufCodecs.stringUtf8(16), State::phase,
                ByteBufCodecs.BOOL, State::leader, ByteBufCodecs.VAR_INT, State::capacity,
                ByteBufCodecs.VAR_INT, State::queuePosition, ByteBufCodecs.VAR_INT, State::countdownSeconds, State::new);
    }
    public record Invite(String token, String inviter, boolean accepted, boolean canInvite) {
        public static final StreamCodec<ByteBuf, Invite> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(36), Invite::token, ByteBufCodecs.stringUtf8(16), Invite::inviter,
                ByteBufCodecs.BOOL, Invite::accepted, ByteBufCodecs.BOOL, Invite::canInvite, Invite::new);
    }
    public record View(int containerId, State state, List<Member> members, Invite invitation) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(id("d1_party_view"));
        public static final StreamCodec<ByteBuf, View> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, View::containerId, State.CODEC, View::state,
                Member.CODEC.apply(ByteBufCodecs.list(6)), View::members, Invite.CODEC, View::invitation, View::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
