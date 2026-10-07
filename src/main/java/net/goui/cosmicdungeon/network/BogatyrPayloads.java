package net.goui.cosmicdungeon.network;

import java.util.*;
import net.goui.cosmicdungeon.playerclass.bogatyr.WolfMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Four bounded care quotes and six mode IDs; clients can only request the captured generation. */
public final class BogatyrPayloads {
    public enum Kind { BREED,SUMMON,REGROUP,HEAL }
    public record Quote(int count,int cost,boolean enabled){
        public Quote{if(count<0||cost<0||cost>600||enabled!=(count>0&&cost>0))throw new IllegalArgumentException("Invalid wolf quote");}
        public static Quote empty(){return new Quote(0,0,false);}
    }
    private BogatyrPayloads(){}
    private static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath("cosmicdungeon",name);}
    public record View(long run,long revision,int loaded,List<Quote> quotes,WolfMode mode,boolean modesEnabled) implements CustomPacketPayload {
        public View(long run,long revision,int loaded,List<Quote> quotes){this(run,revision,loaded,quotes,WolfMode.DEFENSIVE,false);}
        public View{quotes=List.copyOf(quotes);if(mode==null||run==0&&modesEnabled||run<0||revision<0||loaded<0||quotes.size()!=4
                ||run==0&&(loaded!=0||quotes.stream().anyMatch(Quote::enabled)))throw new IllegalArgumentException("Invalid Wolfpack view");}
        public static View empty(long revision){return new View(0,revision,0,Collections.nCopies(4,Quote.empty()));}
        public static final Type<View> TYPE=new Type<>(id("bogatyr_actions"));
        public static final StreamCodec<ByteBuf,View> STREAM_CODEC=new StreamCodec<>(){
            public void encode(ByteBuf raw,View v){var b=new FriendlyByteBuf(raw);b.writeVarLong(v.run);b.writeVarLong(v.revision);b.writeVarInt(v.loaded);
                for(var q:v.quotes){b.writeVarInt(q.count);b.writeVarInt(q.cost);b.writeBoolean(q.enabled);}b.writeVarInt(v.mode.ordinal());b.writeBoolean(v.modesEnabled);}
            public View decode(ByteBuf raw){var b=new FriendlyByteBuf(raw);long run=b.readVarLong(),revision=b.readVarLong();int loaded=b.readVarInt();
                var quotes=new ArrayList<Quote>(4);for(int i=0;i<4;i++)quotes.add(new Quote(b.readVarInt(),b.readVarInt(),b.readBoolean()));
                return new View(run,revision,loaded,quotes,readMode(b),b.readBoolean());}
        };
        public Type<View> type(){return TYPE;}
    }
    public record Action(long run,long revision,Kind kind) implements CustomPacketPayload {
        public Action{if(run<=0||revision<0||kind==null)throw new IllegalArgumentException("Invalid wolf action");}
        public static final Type<Action> TYPE=new Type<>(id("bogatyr_action"));
        public static final StreamCodec<ByteBuf,Action> STREAM_CODEC=new StreamCodec<>(){
            public void encode(ByteBuf raw,Action a){var b=new FriendlyByteBuf(raw);b.writeVarLong(a.run);b.writeVarLong(a.revision);b.writeVarInt(a.kind.ordinal());}
            public Action decode(ByteBuf raw){var b=new FriendlyByteBuf(raw);long run=b.readVarLong(),revision=b.readVarLong();int k=b.readVarInt();
                if(k<0||k>=Kind.values().length)throw new IllegalArgumentException("Unknown wolf action");return new Action(run,revision,Kind.values()[k]);}
        };
        public Type<Action> type(){return TYPE;}
    }
    private static WolfMode readMode(FriendlyByteBuf b){
        int mode=b.readVarInt();if(mode<0||mode>=WolfMode.values().length)throw new IllegalArgumentException("Unknown wolf mode");
        return WolfMode.values()[mode];
    }
    public record ModeAction(long run,long revision,WolfMode mode) implements CustomPacketPayload {
        public ModeAction{if(run<=0||revision<0||mode==null)throw new IllegalArgumentException("Invalid wolf mode action");}
        public static final Type<ModeAction> TYPE=new Type<>(id("bogatyr_mode"));
        public static final StreamCodec<ByteBuf,ModeAction> STREAM_CODEC=new StreamCodec<>(){
            public void encode(ByteBuf raw,ModeAction a){var b=new FriendlyByteBuf(raw);b.writeVarLong(a.run);b.writeVarLong(a.revision);b.writeVarInt(a.mode.ordinal());}
            public ModeAction decode(ByteBuf raw){var b=new FriendlyByteBuf(raw);return new ModeAction(b.readVarLong(),b.readVarLong(),readMode(b));}
        };
        public Type<ModeAction> type(){return TYPE;}
    }
}
