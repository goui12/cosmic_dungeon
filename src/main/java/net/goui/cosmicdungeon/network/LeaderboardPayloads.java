package net.goui.cosmicdungeon.network;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
public final class LeaderboardPayloads{
    private LeaderboardPayloads(){}
    public static final int PAGE_SIZE=12;
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("cosmicdungeon",path);}
    public record Request(int request,String metric,String search,int page,long afterValue,String afterId) implements CustomPacketPayload{
        public Request{
            if(request<0||metric.length()>160||search.length()>48||page<0||page>100000||afterValue< -1
                    ||afterId.length()>36||((afterValue==-1)!=afterId.isEmpty()))
                throw new IllegalArgumentException("Invalid leaderboard request");
            if(!afterId.isEmpty())UUID.fromString(afterId);
        }
        public static final Type<Request> TYPE=new Type<>(id("leaderboard_request"));
        public static final StreamCodec<ByteBuf,Request> CODEC=StreamCodec.composite(
            ByteBufCodecs.VAR_INT,Request::request,ByteBufCodecs.stringUtf8(160),Request::metric,
            ByteBufCodecs.stringUtf8(48),Request::search,ByteBufCodecs.VAR_INT,Request::page,
            ByteBufCodecs.VAR_LONG,Request::afterValue,ByteBufCodecs.stringUtf8(36),Request::afterId,Request::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Metric(String key,String label){
        public static final StreamCodec<ByteBuf,Metric> CODEC=StreamCodec.composite(
            ByteBufCodecs.stringUtf8(160),Metric::key,ByteBufCodecs.stringUtf8(160),Metric::label,Metric::new);
    }
    public record Row(String id,String name,long value){
        public Row{UUID.fromString(id);if(name.length()>16||value<0)throw new IllegalArgumentException("Invalid row");}
        public static final StreamCodec<ByteBuf,Row> CODEC=StreamCodec.composite(
            ByteBufCodecs.stringUtf8(36),Row::id,ByteBufCodecs.stringUtf8(16),Row::name,ByteBufCodecs.VAR_LONG,Row::value,Row::new);
    }
    public record Page(String metric,int page,int pages,long firstRank,boolean more,String status){
        public static final StreamCodec<ByteBuf,Page> CODEC=StreamCodec.composite(
            ByteBufCodecs.stringUtf8(160),Page::metric,ByteBufCodecs.VAR_INT,Page::page,
            ByteBufCodecs.VAR_INT,Page::pages,ByteBufCodecs.VAR_LONG,Page::firstRank,
            ByteBufCodecs.BOOL,Page::more,ByteBufCodecs.stringUtf8(240),Page::status,Page::new);
    }
    public record View(int request,List<Metric> metrics,List<Row> rows,Page page) implements CustomPacketPayload{
        public View{
            metrics=List.copyOf(metrics);rows=List.copyOf(rows);
            if(metrics.size()>PAGE_SIZE||rows.size()>PAGE_SIZE)throw new IllegalArgumentException("Oversized leaderboard");
        }
        public static final Type<View> TYPE=new Type<>(id("leaderboard_view"));
        public static final StreamCodec<ByteBuf,View> CODEC=StreamCodec.composite(
            ByteBufCodecs.VAR_INT,View::request,Metric.CODEC.apply(ByteBufCodecs.list(PAGE_SIZE)),View::metrics,
            Row.CODEC.apply(ByteBufCodecs.list(PAGE_SIZE)),View::rows,Page.CODEC,View::page,View::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
