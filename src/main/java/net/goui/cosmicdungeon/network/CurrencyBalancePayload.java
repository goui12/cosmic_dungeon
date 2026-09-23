package net.goui.cosmicdungeon.network;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
/** Read-only account projection, sent only to its owner. Never accepted from clients. */
public record CurrencyBalancePayload(UUID owner,long revision,long balance,long available,int classChestId)
        implements CustomPacketPayload {
    public static final Type<CurrencyBalancePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon","currency_balance"));
    public static final StreamCodec<ByteBuf,CurrencyBalancePayload> STREAM_CODEC = StreamCodec.of(
        (buf,p) -> {buf.writeLong(p.owner.getMostSignificantBits());buf.writeLong(p.owner.getLeastSignificantBits());
            buf.writeLong(p.revision);buf.writeLong(p.balance);buf.writeLong(p.available);buf.writeInt(p.classChestId);},
        buf -> new CurrencyBalancePayload(new UUID(buf.readLong(),buf.readLong()),buf.readLong(),buf.readLong(),buf.readLong(),buf.readInt()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
