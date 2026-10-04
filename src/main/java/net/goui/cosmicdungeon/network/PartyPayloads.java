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
    public record Recruitment(String groupName, boolean looking, int page, int pages, List<Member> candidates) {
        public static final Recruitment EMPTY = new Recruitment("", false, 0, 1, List.of());
        public static final StreamCodec<ByteBuf, Recruitment> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(32), Recruitment::groupName, ByteBufCodecs.BOOL, Recruitment::looking,
                ByteBufCodecs.VAR_INT, Recruitment::page, ByteBufCodecs.VAR_INT, Recruitment::pages,
                Member.CODEC.apply(ByteBufCodecs.list(4)), Recruitment::candidates, Recruitment::new);
    }
    public record Mercenary(String name,String owner,float health,float maxHealth,int respawnSeconds,String status){
        public Mercenary{
            if(name==null||owner==null||name.length()>64||owner.length()>16||!Float.isFinite(health)
                    ||!Float.isFinite(maxHealth)||health<0||maxHealth<0||health>maxHealth
                    ||respawnSeconds < -1||respawnSeconds>600
                    ||!List.of("ACTIVE","RESPAWNING","UNLOADED").contains(status))
                throw new IllegalArgumentException("Invalid mercenary HUD row");
        }
        public static final StreamCodec<ByteBuf,Mercenary> CODEC=StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64),Mercenary::name,ByteBufCodecs.stringUtf8(16),Mercenary::owner,
            ByteBufCodecs.FLOAT,Mercenary::health,ByteBufCodecs.FLOAT,Mercenary::maxHealth,
            ByteBufCodecs.VAR_INT,Mercenary::respawnSeconds,ByteBufCodecs.stringUtf8(16),Mercenary::status,Mercenary::new);
    }
    public record Options(String difficulty,Hire hire,List<Mercenary> mercenaries){
        public static final StreamCodec<ByteBuf,Options> CODEC=StreamCodec.composite(
            ByteBufCodecs.stringUtf8(16),Options::difficulty,Hire.CODEC,Options::hire,Mercenary.CODEC.apply(ByteBufCodecs.list(3)),Options::mercenaries,Options::new);
    }
    public record Hire(String selectedClass,int price){
        public static final Hire NONE=new Hire("",500);
        public static final StreamCodec<ByteBuf,Hire> CODEC=StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32),Hire::selectedClass,ByteBufCodecs.VAR_INT,Hire::price,Hire::new);
    }
    public record View(int containerId, State state, List<Member> members, Invite invitation, Recruitment recruitment, String difficulty, Hire hire, List<Mercenary> mercenaries) implements CustomPacketPayload {
        public View { mercenaries=List.copyOf(mercenaries);if(mercenaries.size()>3)throw new IllegalArgumentException("Too many mercenaries"); }
        public View(int containerId,State state,List<Member> members,Invite invitation,Recruitment recruitment,String difficulty,Hire hire){
            this(containerId,state,members,invitation,recruitment,difficulty,hire,List.of());
        }
        public View(int containerId,State state,List<Member> members,Invite invitation,Recruitment recruitment,String difficulty){
            this(containerId,state,members,invitation,recruitment,difficulty,Hire.NONE);
        }
        public View(int containerId, State state, List<Member> members, Invite invitation, Recruitment recruitment) {
            this(containerId, state, members, invitation, recruitment, "HARD");
        }
        public View(int containerId, State state, List<Member> members, Invite invitation) {
            this(containerId, state, members, invitation, Recruitment.EMPTY);
        }
        public static final Type<View> TYPE = new Type<>(id("d1_party_view"));
        public static final StreamCodec<ByteBuf, View> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, View::containerId, State.CODEC, View::state,
                Member.CODEC.apply(ByteBufCodecs.list(6)), View::members, Invite.CODEC, View::invitation,
                Recruitment.CODEC, View::recruitment, Options.CODEC, v->new Options(v.difficulty(),v.hire(),v.mercenaries()),
                (id,state,members,invite,recruitment,options)->new View(id,state,members,invite,recruitment,options.difficulty(),options.hire(),options.mercenaries()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
