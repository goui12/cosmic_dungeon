package net.goui.cosmicdungeon.network;

import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Bounded read-only details. Run and sequence bind responses to the currently open inspection. */
public final class PartyInspectionPayloads {
    private PartyInspectionPayloads() {}
    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath("cosmicdungeon", name); }
    public record Request(long run, long sequence, UUID subject) implements CustomPacketPayload {
        public Request { if(run<=0 || sequence<=0 || subject==null) throw new IllegalArgumentException("Invalid inspection request"); }
        public static final Type<Request> TYPE=new Type<>(id("party_inspect"));
        public static final StreamCodec<ByteBuf,Request> STREAM_CODEC=StreamCodec.composite(
                ByteBufCodecs.VAR_LONG,Request::run,ByteBufCodecs.VAR_LONG,Request::sequence,
                net.minecraft.core.UUIDUtil.STREAM_CODEC,Request::subject,Request::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Reading(String label,String value) {
        public Reading { if(label==null || value==null || label.length()>48 || value.length()>96)
            throw new IllegalArgumentException("Invalid inspection reading"); }
        public static final StreamCodec<ByteBuf,Reading> CODEC=StreamCodec.composite(
                ByteBufCodecs.stringUtf8(48),Reading::label,ByteBufCodecs.stringUtf8(96),Reading::value,Reading::new);
    }
    public record View(Request request,boolean allowed,String name,String classId,PartyVitals vitals,List<Reading> readings)
            implements CustomPacketPayload {
        public View {
            readings=List.copyOf(readings);
            if(request==null || vitals==null || name==null || name.length()>16 || classId==null || classId.length()>32
                    || readings.size()>24 || !allowed && (!name.isEmpty() || !classId.isEmpty()
                    || !readings.isEmpty() || !vitals.equals(PartyVitals.UNLOADED)))
                throw new IllegalArgumentException("Invalid inspection view");
        }
        public static View denied(Request request){return new View(request,false,"","",PartyVitals.UNLOADED,List.of());}
        public static final Type<View> TYPE=new Type<>(id("party_inspection"));
        public static final StreamCodec<ByteBuf,View> STREAM_CODEC=StreamCodec.composite(
                Request.STREAM_CODEC,View::request,ByteBufCodecs.BOOL,View::allowed,
                ByteBufCodecs.stringUtf8(16),View::name,ByteBufCodecs.stringUtf8(32),View::classId,
                PartyVitals.CODEC,View::vitals,Reading.CODEC.apply(ByteBufCodecs.list(24)),View::readings,View::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
