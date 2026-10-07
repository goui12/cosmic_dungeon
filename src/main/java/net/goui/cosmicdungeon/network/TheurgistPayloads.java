package net.goui.cosmicdungeon.network;

import java.util.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Bounded capabilities and one captured latest-death/offer token per action; never client authority. */
public final class TheurgistPayloads {
    public static final UUID NONE=new UUID(0,0);
    public static final int MAX_TARGETS=64;
    public enum Kind { CRAFT,EPIC,OFFER,ACCEPT,DECLINE }
    private TheurgistPayloads(){}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("cosmicdungeon",path);}
    public record Target(UUID player,String name,UUID death,boolean available){
        public Target{Objects.requireNonNull(player);Objects.requireNonNull(death);if(name==null||name.isEmpty()||name.length()>64)throw new IllegalArgumentException("Invalid target");}
    }
    public record Offer(UUID id,UUID caster,String name,UUID death){
        public Offer{Objects.requireNonNull(id);Objects.requireNonNull(caster);Objects.requireNonNull(death);if(name==null||name.isEmpty()||name.length()>64)throw new IllegalArgumentException("Invalid offer");}
    }
    public record View(long run,long revision,boolean theurgist,boolean alive,boolean normal,boolean epic,List<Target> targets,Offer offer) implements CustomPacketPayload{
        public View{
            targets=List.copyOf(targets);
            if(run<0||revision<0||targets.size()>MAX_TARGETS||targets.stream().map(Target::player).distinct().count()!=targets.size()
                    ||(!theurgist||!alive)&&(normal||epic||!targets.isEmpty())
                    ||run==0&&(theurgist||alive||normal||epic||!targets.isEmpty()||offer!=null))
                throw new IllegalArgumentException("Invalid Theurgist view");
        }
        public static View empty(long revision){return new View(0,revision,false,false,false,false,List.of(),null);}
        public static final Type<View> TYPE=new Type<>(id("theurgist_actions"));
        public static final StreamCodec<ByteBuf,View> STREAM_CODEC=new StreamCodec<>(){
            public void encode(ByteBuf raw,View v){
                var b=new FriendlyByteBuf(raw);b.writeVarLong(v.run);b.writeVarLong(v.revision);b.writeBoolean(v.theurgist);b.writeBoolean(v.alive);
                b.writeBoolean(v.normal);b.writeBoolean(v.epic);b.writeVarInt(v.targets.size());
                for(var t:v.targets){b.writeUUID(t.player);b.writeUtf(t.name,64);b.writeUUID(t.death);b.writeBoolean(t.available);}
                b.writeBoolean(v.offer!=null);if(v.offer!=null){var o=v.offer;b.writeUUID(o.id);b.writeUUID(o.caster);b.writeUtf(o.name,64);b.writeUUID(o.death);}
            }
            public View decode(ByteBuf raw){
                var b=new FriendlyByteBuf(raw);long run=b.readVarLong(),revision=b.readVarLong();boolean cls=b.readBoolean(),alive=b.readBoolean(),normal=b.readBoolean(),epic=b.readBoolean();
                int n=b.readVarInt();if(n<0||n>MAX_TARGETS)throw new IllegalArgumentException("Too many revival targets");
                var targets=new ArrayList<Target>(n);for(int i=0;i<n;i++)targets.add(new Target(b.readUUID(),b.readUtf(64),b.readUUID(),b.readBoolean()));
                Offer offer=b.readBoolean()?new Offer(b.readUUID(),b.readUUID(),b.readUtf(64),b.readUUID()):null;
                return new View(run,revision,cls,alive,normal,epic,targets,offer);
            }
        };
        public Type<View> type(){return TYPE;}
    }
    public record Action(long run,long revision,Kind kind,UUID target,UUID token) implements CustomPacketPayload{
        public Action{if(run<=0||revision<0||kind==null||target==null||token==null
                ||(kind==Kind.CRAFT||kind==Kind.EPIC)&&(!target.equals(NONE)||!token.equals(NONE)))
            throw new IllegalArgumentException("Invalid Theurgist action");}
        public static final Type<Action> TYPE=new Type<>(id("theurgist_action"));
        public static final StreamCodec<ByteBuf,Action> STREAM_CODEC=new StreamCodec<>(){
            public void encode(ByteBuf raw,Action a){var b=new FriendlyByteBuf(raw);b.writeVarLong(a.run);b.writeVarLong(a.revision);b.writeVarInt(a.kind.ordinal());b.writeUUID(a.target);b.writeUUID(a.token);}
            public Action decode(ByteBuf raw){var b=new FriendlyByteBuf(raw);long run=b.readVarLong(),revision=b.readVarLong();int k=b.readVarInt();if(k<0||k>=Kind.values().length)throw new IllegalArgumentException("Unknown action");return new Action(run,revision,Kind.values()[k],b.readUUID(),b.readUUID());}
        };
        public Type<Action> type(){return TYPE;}
    }
}
