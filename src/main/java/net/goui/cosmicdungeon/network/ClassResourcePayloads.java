package net.goui.cosmicdungeon.network;

import io.netty.buffer.ByteBuf;
import net.goui.cosmicdungeon.playerclass.resource.ClassResourceKind;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Own-player snapshots and a bounded recycle request; no client amount, item list or recipient is trusted. */
public final class ClassResourcePayloads {
    private ClassResourcePayloads(){}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("cosmicdungeon",path);}
    public record View(long runId,String resourceId,int amount,int cap,boolean active,boolean alive,boolean recyclable,long revision)
            implements CustomPacketPayload {
        public View {
            if(cap!=600||amount<0||amount>cap||revision<0||runId<0||resourceId==null
                    ||runId==0&&(!resourceId.isEmpty()||amount!=0||active||alive||recyclable||revision!=0)
                    ||runId>0&&(ClassResourceKind.byId(resourceId).isEmpty()||!active)
                    ||recyclable&&(!alive||amount>=cap))throw new IllegalArgumentException("Invalid resource snapshot");
        }
        public static View empty(){return new View(0,"",0,600,false,false,false,0);}
        public static final Type<View> TYPE=new Type<>(id("class_resource"));
        public static final StreamCodec<ByteBuf,View> STREAM_CODEC=StreamCodec.of((buf,v)->{
            ByteBufCodecs.VAR_LONG.encode(buf,v.runId());ByteBufCodecs.stringUtf8(32).encode(buf,v.resourceId());
            ByteBufCodecs.VAR_INT.encode(buf,v.amount());ByteBufCodecs.VAR_INT.encode(buf,v.cap());
            ByteBufCodecs.BOOL.encode(buf,v.active());ByteBufCodecs.BOOL.encode(buf,v.alive());
            ByteBufCodecs.BOOL.encode(buf,v.recyclable());ByteBufCodecs.VAR_LONG.encode(buf,v.revision());
        },buf->new View(ByteBufCodecs.VAR_LONG.decode(buf),ByteBufCodecs.stringUtf8(32).decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),ByteBufCodecs.VAR_INT.decode(buf),ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),ByteBufCodecs.BOOL.decode(buf),ByteBufCodecs.VAR_LONG.decode(buf)));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Recycle(long runId,String resourceId,long revision) implements CustomPacketPayload {
        public Recycle{if(runId<=0||revision<0||ClassResourceKind.byId(resourceId).isEmpty())throw new IllegalArgumentException("Invalid recycle request");}
        public static final Type<Recycle> TYPE=new Type<>(id("class_resource_recycle"));
        public static final StreamCodec<ByteBuf,Recycle> STREAM_CODEC=StreamCodec.composite(
                ByteBufCodecs.VAR_LONG,Recycle::runId,ByteBufCodecs.stringUtf8(32),Recycle::resourceId,
                ByteBufCodecs.VAR_LONG,Recycle::revision,Recycle::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
